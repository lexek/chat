angular.module('ngColorPicker', [])
    .directive('ngColorPicker', function() {
        var defaultColors =  [
            '#7bd148',
            '#5484ed',
            '#a4bdfc',
            '#46d6db',
            '#7ae7bf',
            '#51b749',
            '#fbd75b',
            '#ffb878',
            '#ff887c',
            '#dc2127',
            '#dbadff',
            '#e1e1e1'
        ];
        return {
            scope: {
                selected: '=',
                customizedColors: '=colors'
            },
            restrict: 'AE',
            template:
            '<div class="colorpicker-container>"' +
                '<div class="colorpicker-box">' +
                    '<div class="colorpicker-color-box" ng-repeat="color in colors" ng-class="{selected: (color===selected)}" ng-click="pick(color)" style="background-color:{{color}};"></div>' +
                '</div>' +
                '<div>Selected color: <input type="text" ng-model="selected"/></div>' +
            '</div>',
            link: function (scope, element, attr) {
                scope.colors = scope.customizedColors || defaultColors;
                scope.selected = scope.selected || scope.colors[0];

                scope.pick = function (color) {
                    scope.selected = color;
                };

            }
        };

    });