(function() {
    'use strict';

    angular
        .module('chat.admin.utils')
        .filter('message', MessageFilter);

    /* @ngInject */
    function MessageFilter($http, $sce) {
        var emoticons = {};
        var emoticonRegExp = null;
        $http({
            method: 'GET',
            url: '/rest/emoticons/all'
        }).success(function(data) {
            emoticons = {};
            var emoticonCodeList = [];
            angular.forEach(data, function (e) {
                emoticons[e.code] = e;
                emoticonCodeList.push(
                    e.code
                        .replace('\\', '\\\\')
                        .replace(')', '\\)')
                        .replace('(', '\\(')
                        .replace('.', '\\.')
                        .replace('*', '\\*')
                );
            });
            emoticonRegExp = new RegExp(emoticonCodeList.join('|'), 'g');
        });

        return function(input) {
            var text = input.replace(/</gi, '&lt;');
            text = twemoji.parse(text, {
                base: '/img/',
                folder: 'twemoji',
                ext: '.png',
                callback: function(icon, options) {
                    switch (icon) {
                        case 'a9':
                        case 'ae':
                        case '2122':
                            return false;
                    }
                    return ''.concat(options.base, options.size, '/', icon, options.ext);
                }
            });
            if (emoticonRegExp) {
                text = text.replace(emoticonRegExp, function (match) {
                    var emoticon = emoticons[match];
                    if (emoticon) {
                        return '<img class=\'faceCode\' src=\'/emoticons/' + emoticon.fileName + '\' title=\'' + emoticon.code + '\'></img>';
                    } else {
                        return null;
                    }
                });
            }
            return $sce.trustAsHtml(text);
        };
    }
})();
