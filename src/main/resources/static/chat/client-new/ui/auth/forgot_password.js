(function () {
    'use strict';

    angular
        .module('chat.ui.auth')
        .controller('ForgotPasswordController', Controller);

    /* @ngInject */
    function Controller($scope, $modalInstance, $http) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.error = null;
        $scope.input = {
            'name': ''
        };

        $scope.submit = function () {
            $http({
                method: 'POST',
                url: '/rest/auth/requestPasswordReset',
                data: $scope.input
            }).success(function () {
                $modalInstance.close();
            }).error(function (data) {
                $scope.info = null;
                $scope.error = data;
                if (data[0]) {
                    $scope.error = data[0].message;
                }
            });
        };
    }
})();
