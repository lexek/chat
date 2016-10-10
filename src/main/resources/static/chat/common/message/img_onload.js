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
            element.one('load', function () {
                scope.$apply(function() {
                    (document.onScroll || angular.noop)();
                    $('.messagesContainer').TrackpadScrollEmulator('recalculate');
                });
            });
        }
    }
})();
