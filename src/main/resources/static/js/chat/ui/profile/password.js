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

    module.run(function($templateCache) {
        $templateCache.put("chat/ui/profile/password.html",
            "<div class='modal-header'>" +
                "<h3><i class='fa fa-key'></i> set password</h3>" +
            "</div>" +
            "<div class='modal-body'>" +
                "<form class='panel-body' name='pwForm' ng-submit='changePassword(password)'>" +
                    "<div class='alert alert-info' ng-if='info'>" +
                        "{{info}}" +
                    "</div>" +
                    "<div class='alert alert-danger' ng-if='error'>" +
                        "{{error}}" +
                    "</div>" +
                    "<div class='form-group' ng-class='{\"has-error\": pwForm.password.$invalid && pwForm.password.$dirty, \"has-success\": !pwForm.password.$invalid}'>" +
                        "<label for='password' class='control-label' translate='AUTH_PASSWORD'></label>" +
                        "<input " +
                                "ng-model='password' " +
                                "id='password' " +
                                "type='password' " +
                                "class='form-control' " +
                                "name='password' " +
                                "placeholder='{{\"AUTH_PASSWORD\" | translate}}' " +
                                "pattern='.{6,30}' " +
                                "title='{{\"AUTH_PASSWORD_FORMAT\" | translate}}' " +
                                "required" +
                                "/>" +
                    "</div>" +
                    "<div class='form-group'>" +
                        "<input ng-disabled='pwForm.$invalid' type='submit' class='btn btn-primary btn-modal' value='{{\"CONTROLS_CHANGE_PASSWORD\" | translate}}'/>" +
                    "</div>" +
                "</form>" +
            "</div>" +
            "<div class='modal-footer'>" +
                "<div class='btn btn-warning' ng-click='close()' translate='CONTROLS_CLOSE'></div>" +
            "</div>"
        );
    });
    module.controller("PasswordSettingsController", ["$scope", "$modalInstance", "$http", PasswordSettingsController])
}