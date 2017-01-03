(function () {
    'use strict';

    angular
        .module('chat.ui.profile')
        .controller('PasswordSettingsController', PasswordSettingsController);

    /* @ngInject */
    function PasswordSettingsController($scope, $modalInstance, $http, hasPassword) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.errors = {};
        $scope.hasPassword = hasPassword;
        $scope.input = {
            'oldPassword': null,
            'password': ''
        };
        $scope.password2 = '';

        $scope.submit = function () {
            $http({
                method: 'PUT',
                url: '/rest/password',
                data: $scope.input
            }).success(function () {
                $modalInstance.close();
            }).error(function (data) {
                $scope.info = null;
                $scope.errors = data;
            });
        };
    };
})();
