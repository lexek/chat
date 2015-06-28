var usersModule = angular.module("chat.users", ["chat.services"]);

extendedOnlineElems = false;

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
        chat.clear($scope.user);
    };
    $scope.timeout = function() {
        chat.timeout($scope.user);
        $scope.user.timedOut = true;
    };
    $scope.unban = function() {
        chat.unban($scope.user);
        $scope.user.banned = false;
        $scope.user.timedOut = false;
    };
    $scope.ban = function() {
        chat.ban($scope.user);
        $scope.user.banned = true;
    };
}]);
