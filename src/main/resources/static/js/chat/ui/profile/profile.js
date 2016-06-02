angular.module("chat.ui.profile", ["chat.ui.profile.email", "chat.ui.profile.password", "chat.services.chat"])
.controller(
    "ProfileController",
    [
        "$scope", "$modalInstance", "$modal", "$http", "chatService",
        function ($scope, $modalInstance, $modal, $http, chat) {
            makeClosable($scope, $modalInstance);

            $scope.self = chat.self;
            $scope.profile = null;

            $scope.showEmailSettings = function() {
                var user = $scope.profile.user;
                $modal.open({
                    templateUrl: 'chat/ui/profile/email.html',
                    controller: "EmailSettingsController",
                    size: "sm",
                    resolve: {
                        hasPendingVerification: function() {
                            return user["email"] && !user["emailVerified"];
                        }
                    }
                }).result.then(function(result) {
                    alert(result);
                    loadProfile();
                });
            };

            var loadProfile = function() {
                if ($scope.self.role >= globalLevels.USER_UNCONFIRMED) {
                    $http({
                        method: 'get',
                        url: '/rest/profile'
                    }).success(function(data) {
                        $scope.profile = data;
                    });
                }
            };

            $scope.toggleCheckIp = function() {
                var newValue = !$scope.profile.user.checkIp;
                $http({
                    "method": "PUT",
                    "url": "/rest/auth/self/checkIp",
                    "data": {
                        "value": newValue
                    }
                }).success(function() {
                    $scope.profile.user.checkIp = newValue;
                }).error(function(data) {
                    alert(data.message);
                });
            };

            $scope.update = loadProfile;

            loadProfile();
        }
    ]
).directive("socialConnect", ["$modal", "$http", function($modal, $http) {
    return {
        restrict: "E",
        scope: {
            "connected": "=",
            "change": "&"
        },
        controller: function($scope) {
            makeProgressable($scope);

            $scope.services = [
                "twitch",
                "twitter",
                "vk",
                "google",
                "goodgame"
            ];

            var iconMap = {
                "twitch": "fa fa-fw fa-twitch",
                "google": "fa fa-fw fa-google",
                "twitter": "fa fa-fw fa-twitter",
                "vk": "fa fa-fw fa-vk",
                "token": "fa fa-fw fa-globe",
                "password": "fa fa-fw fa-key",
                "goodgame": "gg-icon"
            };

            $scope.getIcon = function(service) {
                var value = iconMap[service];
                if (!value) {
                    value = "fa fa-fw fa-question";
                }
                return value;
            };

            $scope.isConnected = function(service) {
                return $scope.connected.hasOwnProperty(service);
            };

            $scope.addAuth = function(service) {
                window.open("https://" + HOST_NAME + ":1337/rest/auth/social/" + service);
            };

            $scope.removeAuth = function(service) {
                if (service === "token") {
                    $scope.apiToken = "";
                }
                $http({
                    "method": "DELETE",
                    "url": "/rest/auth/" + service
                }).success(function() {
                    $scope.change();
                }).error(function(data) {
                    alert(data.message);
                });
            };

            $scope.isSingleAuth = function() {
                var count = Object.keys($scope.connected).length;
                if ($scope.isConnected("token")) {
                    count--;
                }
                return count === 1;
            };

            $scope.setPassword = function() {
                $modal.open({
                    templateUrl: 'chat/ui/profile/password.html',
                    controller: "PasswordSettingsController",
                    size: "sm",
                    resolve: {
                        hasPassword: function() {
                            return $scope.isConnected("password");
                        }
                    }
                }).result.then(function() {
                    $scope.change();
                });
            };

            $scope.newToken = function() {
                $scope.startProgress();
                $http.post("/token").success(function(data) {
                    var token = data["token"];
                    if (token) {
                        $scope.apiToken = token;
                        $scope.connected["token"] = "";
                    }
                    $scope.stopProgress();
                }).error(function(data) {
                    $scope.stopProgressWithError(data);
                });
            };

            $scope.$on("auth-updated", function() {
                $scope.change();
            });
        },
        templateUrl: "/templates/profile_auth.html"
    }
}]);
