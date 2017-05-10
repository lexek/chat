(function() {
    'use strict';

    var module = angular.module('chat.services.windowState', []);

    module.service('windowState', function () {
        var WindowStateService = function () {
            this.onFocus = [];
            this.onBlur = [];
            this.active = true;

            var ref = this;

            if (window === window.top) {
                $(window).focus(function () {
                    ref.active = true;
                    angular.forEach(ref.onFocus, function (callback) {
                        callback();
                    });
                });
                $(window).blur(function () {
                    ref.active = false;
                    angular.forEach(ref.onBlur, function (callback) {
                        callback();
                    });
                });
            }
        };

        WindowStateService.prototype.isActive = function () {
            return this.active;
        };

        WindowStateService.prototype.blur = function (callback) {
            this.onBlur.push(callback);
        };

        WindowStateService.prototype.focus = function (callback) {
            this.onFocus.push(callback);
        };

        return new WindowStateService();
    });
})();
