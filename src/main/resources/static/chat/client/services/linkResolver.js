var module = angular.module("chat.services.linkResolver", []);

module.service("linkResolver", ["$q", "$http", function($q, $http) {
    var STEAM_APP_REGEXP = /\/app\/([0-9]+).*/;

    var genLinkSimple = function(prefix, link, linkText) {
        return "<a href=\"" + prefix + htmlEscape(link) + "\" rel=\"noopener\" target=\"_blank\" title=\"" + htmlEscape(link) + "\">" + linkText + "</a>";
    }

    var genLink = function(prefix, link, linkText) {
        console.log(prefix);
        if (prefix === "https://") {
            linkText = "<i class='fa fa-lock fa-fw'></i>" + linkText;
        } else {
            linkText = "<i class='fa fa-external-link link-icon fa-fw'></i>" + linkText;
        }
        return genLinkSimple(prefix, link, linkText);
    };

    var fetchYoutubeTitle = function(videoId, ytKey, prefix, link, linkText, second, deferred) {
        $http({
            method: 'GET',
            url: 'https://www.googleapis.com/youtube/v3/videos',
            params: {
                "part": "snippet",
                "id": videoId,
                "key": ytKey
            }
        }).success(function(data){
            if (data.items && data.items[0] && data.items[0].snippet) {
                var text = "<span style=\"color:#cd201f;\" class=\"fa fa-youtube-play\"></span> "
                    + htmlEscape(data.items[0].snippet.title);
                if (second) {
                    var hours = Math.floor(second / 3600);
                    var minutes = Math.floor(((second % 86400) % 3600) / 60);
                    var seconds = ((second % 86400) % 3600) % 60;
                    var durations = [];
                    if (hours) {
                        durations.push(hours + "h");
                    }
                    if (minutes) {
                        durations.push(minutes + "m");
                    }
                    if (seconds) {
                        durations.push(seconds + "s");
                    }
                    text += " <strong>[<i class=\"fa fa-clock-o\"></i>" + durations.join(" ") + "]</strong>";
                }
                deferred.resolve(genLinkSimple(prefix, link, text));
            } else {
                deferred.resolve(genLink(prefix, link, linkText))
            }
        }).error(function(){
            deferred.resolve(genLink(prefix, link, linkText));
        });
    };

    var LinkResolverService = function() {};

    LinkResolverService.prototype.resolve = function (completeLink, prefix, link) {
        var deferred = $q.defer();
        var linkText = "";
        try {
            linkText = $.trim(decodeURIComponent(link));
            if (linkText.length === 0) {
                linkText = link;
            }
        } catch (e) {
            linkText = link;
        }
        if (linkText.length > 37) {
            linkText = linkText.substr(0, 32) + "[...]";
        }
        linkText = htmlEscape(linkText);
        try {
            var parsedUrl = new Uri(link);
            var host = parsedUrl.host();
            if (host === "youtube.com" || host === "www.youtube.com") {
                var videoId = null;
                var second = parsedUrl.getQueryParamValue("t");
                if (parsedUrl.getQueryParamValue("v")) {
                    videoId = parsedUrl.getQueryParamValue("v");
                }
                if (parsedUrl.getQueryParamValue("watch")) {
                    videoId = parsedUrl.getQueryParamValue("watch");
                }
                if (videoId) {
                    fetchYoutubeTitle(videoId, ytKey, prefix, link, linkText, second, deferred);
                } else {
                    deferred.resolve(genLink(prefix, link, linkText));
                }
            } else if (host === "youtu.be") {
                var videoId = parsedUrl.uriParts.path;
                var second = parsedUrl.getQueryParamValue("t");
                if (videoId[0] === "/") {
                    videoId = videoId.substr(1);
                }
                if (videoId) {
                    fetchYoutubeTitle(videoId, ytKey, prefix, link, linkText, second, deferred);
                } else {
                    deferred.resolve(genLink(prefix, link, linkText));
                }
            } else if (host === "store.steampowered.com") {
                var r = STEAM_APP_REGEXP.exec(parsedUrl.uriParts.path);
                if (r && r[1]) {
                    var id = r[1];
                    $http({
                        method: 'GET',
                        url: '/rest/steamGames/'+id
                    }).success(function(data){
                        var text = "<span style=\"color: #156291;\" class=\"fa fa-steam-square\"></span> "
                            + htmlEscape(data["name"]);
                        deferred.resolve(genLinkSimple(prefix, link, text));
                    }).error(function(){
                        deferred.resolve(genLink(prefix, link, linkText));
                    });
                } else {
                    deferred.resolve(genLink(prefix, link, linkText));
                }
            } else {
                deferred.resolve(genLink(prefix, link, linkText));
            }
        } catch (URIError) {
            deferred.resolve(genLink(prefix, link, linkText));
        }
        return deferred.promise;
    };

    return new LinkResolverService();
}]);
