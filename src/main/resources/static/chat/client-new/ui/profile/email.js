(function () {
    'use strict';

    angular
        .module('chat.ui.profile')
        .controller('EmailSettingsController', EmailSettingsController);

    /* @ngInject */
    function EmailSettingsController($scope, $modalInstance, $http, hasPendingVerification) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.hasPendingVerification = hasPendingVerification;
        $scope.setEmail = function (email) {
            $scope.startProgress();
            $http({
                method: 'PUT',
                url: '/rest/email',
                data: {
                    'email': email
                }
            }).success(function () {
                $scope.stopProgress();
                $modalInstance.close('You should now receive verification email.')
            }).error(function (data) {
                $scope.stopProgressWithError(data['message']);
            });
        };

        $scope.resendVerification = function () {
            $scope.startProgress();
            $http({
                method: 'POST',
                url: '/rest/email/resendVerification'
            }).success(function () {
                $scope.stopProgress('You should now receive verification email.');
            }).error(function (data) {
                $scope.stopProgressWithError(data['message']);
            });
        };
    }
})();
