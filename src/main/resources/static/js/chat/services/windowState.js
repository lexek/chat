var module = angular.module("chat.services.windowState", []);

module.service("windowState", function() {
    var WindowStateService = function() {
        this.active = true;
        var ref = this;
        if (window === window.top) {
            $(window).focus(function () {
                ref.active = true;
                console.log("active");
            });
            $(window).blur(function () {
                ref.active = false;
                console.log("blur");
            });
        }
    };

    WindowStateService.prototype.isActive = function() {
        return this.active;
    };

    return new WindowStateService();
});
