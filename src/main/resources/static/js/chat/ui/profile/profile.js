{
    var module = angular.module("chat.ui.profile", ["chat.ui.profile.email", "chat.ui.profile.password", "chat.services.chat"]);
    var ProfileController = function ($scope, $modalInstance, $modal, $http, chat) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

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

        $scope.showPasswordSettings = function() {
            $modal.open({
                templateUrl: 'chat/ui/profile/password.html',
                controller: "PasswordSettingsController",
                size: "sm"
            }).result.then(function() {
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

        $scope.newToken = function () {
            $scope.startProgress();
            $http.post("/token").success(function(data) {
                var token = data["token"];
                if (token) {
                    $scope.apiToken = token;
                }
                $scope.stopProgress();
            }).error(function(data) {
                $scope.stopProgressWithError(data);
            });
        };

        loadProfile();
    };

    module.controller("ProfileController", ["$scope", "$modalInstance", "$modal", "$http", "chatService", ProfileController])
}