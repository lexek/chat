var usersModule = angular.module("chat.users", ["chat.services"]);

extendedOnlineElems = false;

/**
 * @param {string} title
 * @param {number} value
 * @constructor
 */
Level = function(title, value) {
    this.title = title;
    this.value = value;
};

Level.prototype.valueOf = function() {
    return this.value;
};

/**
 * @enum
 */
levels = {
    'GUEST': new Level("GUEST", 1),
    'USER': new Level("USER", 2),
    'MOD': new Level("MOD", 3),
    'ADMIN': new Level("ADMIN", 4)
};

globalLevels = {
    'UNAUTHENTICATED': new Level("UNAUTHENTICATED", 0),
    'GUEST': new Level("GUEST", 1),
    'USER_UNCONFIRMED': new Level("USER_UNCONFIRMED", 2),
    'USER': new Level("USER", 2),
    "SUPPORTER": new Level("SUPPORTER", 3),
    'MOD': new Level("MOD", 4),
    'ADMIN': new Level("ADMIN", 5),
    'SUPERADMIN': new Level("SUPERADMIN", 6)
};

usersModule.filter("users", function() {
    return function(users) {
        return users.filter(function (user) {
            return (user.role < levels.MOD) && (user.globalRole < globalLevels.MOD)
        });
    }
});

usersModule.filter("mods", function() {
    return function(users) {
        return users.filter(function (user) {
            return (user.role >= levels.MOD) || (user.globalRole >= globalLevels.MOD)
        });
    }
});

usersModule.controller("UsersController", ["$scope", "$http", "chatService", function($scope, $http, chat) {
    $scope.users = [];

    $http({method: "GET", url: "/api/chatters?room=" + encodeURIComponent(chat.activeRoom)})
        .success(function (d, status, headers, config) {
            angular.forEach(d, function(e) {
                e.role = levels[e.role];
                e.globalRole = globalLevels[e.globalRole];
            });
            $scope.users = d;
        })
        .error(function (data, status, headers, config) {
            //alert.alert("danger", data);
        });

    $scope.$on('$viewContentLoaded', function() {
        $('#onlineList').TrackpadScrollEmulator({ wrapContent: false, autoHide: false });
        $scope.$watchCollection("users", function() {
            $('#onlineList').TrackpadScrollEmulator('recalculate');
        });
    });
}]);

usersModule.controller("UserController", ["$scope", "chatService", function($scope, chat) {
    $scope.user.showDescription = false;

    $scope.toggleDescription = function() {
        if (!$scope.user.showDescription) {
            angular.forEach($scope.$parent.users, function(user) {
                if (user.showDescription) {
                    user.showDescription = false;
                }
            });
        }
        $scope.user.showDescription = !$scope.user.showDescription;
        $('#onlineList').TrackpadScrollEmulator('recalculate');
    };

    $scope.showModButtons = function() {
        var localRole = chat.localRole[chat.activeRoom];
        return chat.self &&
            (chat.self != $scope.user) && ($scope.user.role != levels.ADMIN) &&
            (
                (
                    (localRole >= levels.MOD) &&
                    (localRole > $scope.user.role)
                ) || (
                    (chat.self.role >= globalLevels.MOD) &&
                    (chat.self.role > $scope.user.globalRole)
                )
            );
    };

    $scope.canTimeOut = function() {
        return !$scope.user.timedOut && $scope.showModButtons();
    };

    $scope.canBan = function() {
        return !$scope.user.banned && $scope.showModButtons() && ($scope.user.role < levels.SUPPORTER);
    };

    $scope.canUnban = function() {
        return ($scope.user.banned || $scope.user.timedOut) && $scope.showModButtons();
    };

    $scope.clear = function() {
        chat.sendMessage({"type": "CLEAR", args: [chat.activeRoom, $scope.user.name]});
    };
    $scope.timeout = function() {
        chat.sendMessage({"type": "TIMEOUT", args: [chat.activeRoom, $scope.user.name]});
        $scope.user.timedOut = true;
    };
    $scope.unban = function() {
        chat.sendMessage({"type": "UNBAN", args: [chat.activeRoom, $scope.user.name]});
        $scope.user.banned = false;
        $scope.user.timedOut = false;
    };
    $scope.ban = function() {
        chat.sendMessage({"type": "BAN", args: [chat.activeRoom, $scope.user.name]});
        $scope.user.banned = true;
    };
}]);
