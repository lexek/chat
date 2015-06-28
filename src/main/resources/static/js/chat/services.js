var services = angular.module('chat.services', []);

var ytKey = "AIzaSyCTxR8OCdIZN5dUxINg6-pwmaDWQiH8KgY";

var now = function() {
    return Math.round(+new Date()/1000)
};

services.directive("faCheckbox", function() {
    return {
        restrict: "E",
        scope: {
            model: "=",
            label: "@"
        },
        template:
            "<span class=\"checkboxWrapper\" ng-click=\"toggle()\" style=\"cursor: pointer\">" +
            "<span class=\"btn-link btn-checkbox fa pull-left\" ng-class=\"{'fa-square-o': !model, 'fa-check-square-o': model}\"></span> {{label}}" +
            "</span>",
        link: function(scope, element, attrs) {
            scope.model = scope.model || false;

            scope.toggle = function() {
                scope.model = !scope.model;
            }
        }
    }
});
