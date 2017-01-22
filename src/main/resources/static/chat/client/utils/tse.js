(function() {
    'use strict';

    angular
        .module('chat.client.utils')
        .directive('tseOnLoad', tseOnLoad)
        .directive('tse', tse)
        .directive('tseRecalculateOnLoad', tseRecalculateOnLoad);

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

    function tse() {
        return {
            link: link,
            restrict: 'E',
            template:
                '<div class="tse-scrollable wrapper" style="width: 100%; height: 100%">' +
                    '<div class="tse-scroll-content">' +
                        '<div class="tse-content" ng-transclude>' +
                        '</div>' +
                    '</div>' +
                '</div>',
            transclude: true
        };

        function link(scope, element) {
            console.warn(['tse init', element]);
            element.find('.tse-scrollable').TrackpadScrollEmulator({
                'wrapContent': false,
                'autoHide': false
            });
        }
    }

    function tseRecalculateOnLoad() {
        return {
            link: link,
            restrict: 'A'
        };

        function link(scope, element, attrs) {
            element.one('load', function () {
                scope.$apply(function() {
                    $('.tse-scrollable').TrackpadScrollEmulator('recalculate');
                });
            });
        }
    }
})();
