(function() {
    'use strict';

    angular
        .module('chat.common.message')
        .directive('tseOnLoad', tseOnLoad);

    function tseOnLoad() {
        return {
            link: link,
            restrict: 'A'
        };

        function link(scope, element) {
            element.on('load', function () {
                scope.$apply(function() {
                    $('.messagesContainer').TrackpadScrollEmulator('recalculate');
                });
            });
        }
    }
})();
