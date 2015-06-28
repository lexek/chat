var module = angular.module("chat.services.notifications",
    ["chat.services.windowState", "chat.services.settings"]);

module.service("notificationService", ["chatSettings", "windowState", function(settings, windowState) {
    var NotificationService = function() {
    };

    NotificationService.prototype.isAvailable = function() {
        return window.Notification ? true : false;
    };

    NotificationService.prototype.hasPermission = function() {
        return Notification.permission == "granted";
    };

    NotificationService.prototype.requestPermissionAndEnable = function() {
        Notification.requestPermission(function(result) {
            console.log(result);
            if (result == "granted") {
                settings.setS("notifications", true);
            }
        });
    };

    NotificationService.prototype.notify = function(title, body) {
        if (this.isAvailable() && this.hasPermission() && !windowState.isActive()) {
            new Notification(title, {"body": body});
        }
    };

    return new NotificationService();
}]);
