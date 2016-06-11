var module = angular.module("chat.services.settings", []);

var getCookieBackend = function() {
    $.cookie.json = true;

    var CookieBackend = function() {
    }

    CookieBackend.prototype.storeString = function(key, value) {
        $.cookie(key, value, {expires : 365});
    }

    CookieBackend.prototype.getString = function(key) {
        return $.cookie(key);
    }

    return new CookieBackend();
}

var getLocalStorageBackend = function() {
    var migrateKeys = [
        "compact",
        "dark",
        "hideExt",
        "hideMB",
        "notifications",
        "showIgnored",
        "showTS",
        "NG_TRANSLATE_LANG_KEY"
    ]

    var LocalStorageBackend = function() {
    }

    LocalStorageBackend.prototype.storeString = function(key, value) {
        window.localStorage.setItem(key, JSON.stringify(value));
    }

    LocalStorageBackend.prototype.getString = function(key) {
        var s = window.localStorage.getItem(key);
        if (s !== "undefined") {
            return JSON.parse(s);
        } else {
            return null;
        }
    }

    var backend = new LocalStorageBackend();

    if (!backend.getString("migrated")) {
        backend.storeString("migrated", true);
        var cookieBackend = getCookieBackend();
        angular.forEach(migrateKeys, function(key) {
            var value = cookieBackend.getString(key);
            if (value) {
                backend.storeString(key, value);
            }
        });
    }

    return backend;
}

module.service("chatSettings", [function() {
    var SettingsService = function() {
        if (window.localStorage) {
            this.backend = getLocalStorageBackend();
        } else {
            this.backend = getCookieBackend();
        }
    };

    SettingsService.prototype.setS = function(key, value) {
        this.backend.storeString(key, value);
    };

    SettingsService.prototype.getS = function(key) {
        return this.backend.getString(key);
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
