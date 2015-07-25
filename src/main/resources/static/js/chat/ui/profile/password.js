{
    var module = angular.module("chat.ui.profile.password", []);
    var PasswordSettingsController = function ($scope, $modalInstance, $http) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.changePassword = function(password) {
            $.post("/password", $.param({"password": password}), function(data) {
                if (data["success"]) {
                    $scope.error = null;
                    $scope.info = "You have successfuly changed password.";
                    $scope.$apply();
                } else {
                    $scope.info = null;
                    $scope.error = data["error"];
                    $scope.$apply();
                }
            });
        };
    };
    module.controller("PasswordSettingsController", ["$scope", "$modalInstance", "$http", PasswordSettingsController])
}