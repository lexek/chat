var messagesModule = angular.module("chat.messages", ["chat.services.chat"]);

//TODO: fix moderation (???)
messagesModule.controller("MessageController", ["$scope", "chatService", "chatSettings", "$element", function($scope, chat, settings) {
    $scope.showTimestamps = function() {
        return $scope.message.showTS || settings.getS("showTS");
    };

    $scope.like = function(id_) {
        chat.sendMessage({
            "type":"LIKE",
            "room": chat.activeRoom,
            "messageId": parseInt(id_)
        });
    };

    $scope.isSupporter = function() {
        return false;
    };

    $scope.isMod = function() {
        return ($scope.message.user.role == levels.MOD) || ($scope.message.user.globalRole == globalLevels.MOD);
    };

    $scope.isAdmin = function() {
        return ($scope.message.user.role == levels.ADMIN) || ($scope.message.user.globalRole == globalLevels.ADMIN);
    };

    $scope.getHighestRole = function() {
        var localRole = $scope.message.user.role;
        var globalRole = $scope.message.user.globalRole;
        if ((localRole === levels.ADMIN) || (globalRole < globalLevels.MOD)) {
            return {global: false, role: localRole}
        } else {
            return {global: true, role: globalRole}
        }
    };

    $scope.message.messageUpdatedCallbacks.push(function() {$scope.$apply();});

    $scope.showModButtons = function() {
        return $scope.message.showModButtons && !settings.getS("hideMB");
    };

    $scope.addToInput = function(evt) {
        evt.preventDefault();
        chat.addToInputCallback("@" + $scope.message.user.name);
    };

    $scope.isMult = function() {
        return $scope.message.mult;
    };

    $scope.extUrl = function() {
        if ($scope.message.user.service !== null) {
            if ($scope.message.user.service === "twitch") {
                return "http://www.twitch.tv/" + $scope.message.user.name + "/profile";
            }
        }
    };

    $scope.clear = function() {
        console.log($scope.message);
        if ($scope.message.user.service) {
            chat.sendMessage({
                "type": "PROXY_MOD",
                "text": "CLEAR",
                "room": chat.activeRoom,
                "name": $scope.message.user.name,
                "service": $scope.message.user.service
            })
        } else {
            chat.clear($scope.message.user);
        }
    };
    $scope.timeout = function() {
        if ($scope.message.user.service) {
            chat.sendMessage({
                "type": "PROXY_MOD",
                "text": "TIMEOUT",
                "room": chat.activeRoom,
                "name": $scope.message.user.name,
                "service": $scope.message.user.service
            })
        } else {
            chat.timeout($scope.message.user);
        }
    };
    $scope.ban = function() {
        if ($scope.message.user.service) {
            chat.sendMessage({
                "type": "PROXY_MOD",
                "text": "BAN",
                "room": chat.activeRoom,
                "name": $scope.message.user.name,
                "service": $scope.message.user.service
            })
        } else {
            chat.ban($scope.message.user);
        }
    };
}]);

messagesModule.controller("MessagesController", ["$scope", "chatService", "chatSettings", function($scope, chat, settings) {
    $scope.messages = chat.messages;

    $scope.compact = function() {
        return settings.getS("compact");
    };

    $scope.getActiveRoom = function() {
        return chat.activeRoom;
    };

    $scope.$on('$viewContentLoaded', function() {
        $('.messagesContainer').TrackpadScrollEmulator({ wrapContent: false });
        $scope.$watchCollection("messages", function() {
            $('.messagesContainer').TrackpadScrollEmulator('recalculate');
        });
    });

    chat.messagesUpdatedCallbacks.push(function() {$scope.$apply();});
    chat.init();
}]);
