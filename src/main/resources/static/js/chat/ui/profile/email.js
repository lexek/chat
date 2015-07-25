{
    var module = angular.module("chat.ui.profile.email", []);
    var EmailSettingsController = function ($scope, $modalInstance, $http) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.setEmail = function(email) {
            $scope.startProgress();
            $http({
                method: 'PUT',
                url: '/rest/email',
                data: {
                    "email": email
                }
            }).success(function() {
                $scope.stopProgress("You should now receive verification email.");
            }).error(function(data) {
                $scope.stopProgressWithError(data["message"]);
            });
        };

        $scope.resendVerification = function() {
            $scope.startProgress();
            $http({
                method: 'POST',
                url: '/rest/email/resendVerification'
            }).success(function() {
                $scope.stopProgress("You should now receive verification email.");
            }).error(function(data) {
                $scope.stopProgressWithError(data["message"]);
            });
        };

        var loadVerification = function() {
            $http({
                method: 'GET',
                url: '/rest/email/hasPendingVerification'
            }).success(function(d) {
                $scope.hasPendingVerification = d["value"];
            });
        };

        loadVerification()
    };

    module.run(function($templateCache) {
        $templateCache.put("chat/ui/profile/email.html",
            "<div class='modal-header'>" +
                "<h3><i class='fa fa-envelope'></i> email settings</h3>" +
            "</div>" +
            "<div class='modal-body'>" +
                '<div ng-if="hasPendingVerification">' +
                    '<div class="btn btn-default" ng-click="resendVerification()">Resend verification email</div>' +
                '</div>' +
                '<form class="panel-body" name="form" ng-submit="setEmail(email)">' +
                    "<div class='alert alert-danger' ng-if='error' ng-bind='error'></div>" +
                    "<div class='alert alert-info' ng-if='info' ng-bind='info'></div>" +
                    '<div class="form-group" ng-class="{\'has-error\': form.email.$invalid && form.email.$dirty, \'has-success\': !form.email.$invalid}">' +
                        '<label for="email" class="control-label" translate="AUTH_EMAIL"></label>' +
                        '<input ' +
                            'ng-model="email" ' +
                            'id="email" ' +
                            'type="email" ' +
                            'class="form-control" ' +
                            'name="email" ' +
                            'ng-placeholder="\'AUTH_EMAIL\' | translate" ' +
                            'required' +
                            '/>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<input ng-disabled="form.$invalid" type="submit" class="btn btn-primary" value="set email"/>' +
                    '</div>' +
                '</form>' +
            "</div>" +
            "<div class='modal-footer'>" +
                "<div class='btn btn-warning' ng-click='close()' translate='CONTROLS_CLOSE'></div>" +
            "</div>"
        );
    });
    module.controller("EmailSettingsController", ["$scope", "$modalInstance", "$http", EmailSettingsController])
}