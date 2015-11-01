var module = angular.module("chat.services.settings", []);

module.service("chatSettings", [function() {
    var SettingsService = function() {
        $.cookie.json = true;
    };

    SettingsService.prototype.setS = function (name, value) {
        $.cookie(name, value, {expires : 365});
    };

    SettingsService.prototype.getS = function(name) {
        return $.cookie(name);
    };

    SettingsService.prototype.getRooms = function() {
        var s = this.getS("rooms");
        var result;
        if (s) {
            result =  $.parseJSON(s);
        } else {
            result = [DEFAULT_ROOM];
        }
        return result;
    };

    SettingsService.prototype.addRoom = function(name) {
        var rooms = this.getRooms();
        if (rooms) {
            rooms.push(name);
        }
        this.setS("rooms", JSON.stringify(rooms));
    };

    SettingsService.prototype.deleteRoom = function(name) {
        var rooms = this.getRooms();
        if (rooms) {
            rooms.remove(name);
        }
        this.setS("rooms", JSON.stringify(rooms));
    };

    return new SettingsService();
}]);
