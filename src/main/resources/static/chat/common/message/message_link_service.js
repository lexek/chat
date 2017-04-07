(function() {
    'use strict';

    var SUPPORTED_CATEGORIES = ['video', 'image'];

    angular.module('chat.common.message').service('messageLinkService', MessageLinkService);

    function composeMessageModel(type, link, text, secure, youTubeTime) {
        return {
            'secure': secure,
            'type': type,
            'link': link,
            'text': text,
            'youTubeTime': youTubeTime
        };
    }

    /* @ngInject */
    function MessageLinkService($q, $http) {
        var STEAM_APP_REGEXP = /\/app\/([0-9]+).*/;

        return {
            'resolve': resolve
        };

        function genLink(prefix, link, text, type) {
            var secure = prefix === 'https://';
            if (type) {
                return composeMessageModel(type, prefix + link, text, secure);
            } else {
                return composeMessageModel('link', prefix + link, text, secure);
            }
        }

        function fetchYouTubeTitle(videoId, ytKey, prefix, link, linkText, second, deferred) {
            $http({
                method: 'GET',
                url: 'https://www.googleapis.com/youtube/v3/videos',
                params: {
                    'part': 'snippet',
                    'id': videoId,
                    'key': ytKey
                }
            }).success(function(data) {
                if (data.items && data.items[0] && data.items[0].snippet) {
                    var text = data.items[0].snippet.title;
                    var time = null;
                    if (second) {
                        var hours = Math.floor(second / 3600);
                        var minutes = Math.floor(((second % 86400) % 3600) / 60);
                        var seconds = ((second % 86400) % 3600) % 60;
                        var durations = [];
                        if (hours) {
                            durations.push(hours + 'h');
                        }
                        if (minutes) {
                            durations.push(minutes + 'm');
                        }
                        if (seconds) {
                            durations.push(seconds + 's');
                        }
                        time = durations.join(' ');
                    }
                    deferred.resolve(composeMessageModel('youtube', link, text, true, time));
                } else {
                    deferred.resolve(genLink(prefix, link, linkText));
                }
            }).error(function() {
                deferred.resolve(genLink(prefix, link, linkText));
            });
        }

        function tryFetchOg(deferred, prefix, link, linkText) {
            $http({
                method: 'POST',
                url: 'https://monitoring.atplay.ch/og/fetch',
                data: {
                    'url': prefix + link
                }
            }).then(
                function (response) {
                    var data = response.data;
                    if (data) {
                        var title = data['og:title'] || data['title'];
                        if (title) {
                            deferred.resolve(genLink(prefix, link, data.hostname + ': ' + title));
                            return;
                        }
                        var mime = data.mime;
                        if (mime && mime.indexOf('/') !== -1) {
                            var category = mime.split('/')[0];
                            if (SUPPORTED_CATEGORIES.indexOf(category) !== -1) {
                                deferred.resolve(genLink(prefix, link, linkText, category));
                                return;
                            }
                        }
                    }
                    deferred.resolve(genLink(prefix, link, linkText));
                },
                function () {
                    deferred.resolve(genLink(prefix, link, linkText));
                }
            );
        }

        function resolve(completeLink, prefix, link) {
            var deferred = $q.defer();
            var linkText = '';
            try {
                linkText = $.trim(decodeURIComponent(link));
                if (linkText.length === 0) {
                    linkText = link;
                }
            } catch (e) {
                linkText = link;
            }
            if (linkText.length > 37) {
                linkText = linkText.substr(0, 32) + '[...]';
            }
            try {
                var parsedUrl = new Uri(link);
                var host = parsedUrl.host();
                if (host === 'youtube.com' || host === 'www.youtube.com') {
                    var videoId = null;
                    var second = parsedUrl.getQueryParamValue('t');
                    if (parsedUrl.getQueryParamValue('v')) {
                        videoId = parsedUrl.getQueryParamValue('v');
                    }
                    if (parsedUrl.getQueryParamValue('watch')) {
                        videoId = parsedUrl.getQueryParamValue('watch');
                    }
                    if (videoId) {
                        fetchYouTubeTitle(videoId, ytKey, prefix, link, linkText, second, deferred);
                    } else {
                        tryFetchOg(deferred, prefix, link, linkText);
                    }
                } else if (host === 'youtu.be') {
                    var videoId = parsedUrl.uriParts.path;
                    var second = parsedUrl.getQueryParamValue('t');
                    if (videoId[0] === '/') {
                        videoId = videoId.substr(1);
                    }
                    if (videoId) {
                        fetchYouTubeTitle(videoId, ytKey, prefix, link, linkText, second, deferred);
                    } else {
                        tryFetchOg(deferred, prefix, link, linkText);
                    }
                } else if (host === 'store.steampowered.com') {
                    var r = STEAM_APP_REGEXP.exec(parsedUrl.uriParts.path);
                    if (r && r[1]) {
                        var id = r[1];
                        $http({
                            method: 'GET',
                            url: '/rest/steamGames/' + id
                        }).success(function(data) {
                            var text = data['name'];
                            deferred.resolve(composeMessageModel('steam', link, text));
                        }).error(function() {
                            deferred.resolve(genLink(prefix, link, linkText));
                        });
                    } else {
                        tryFetchOg(deferred, prefix, link, linkText);
                    }
                } else {
                    tryFetchOg(deferred, prefix, link, linkText);
                }
            } catch (URIError) {
                deferred.resolve(genLink(prefix, link, linkText));
            }
            return deferred.promise;
        }
    }
})();
