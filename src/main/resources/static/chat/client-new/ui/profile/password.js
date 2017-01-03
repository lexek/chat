(function () {
    'use strict';

    angular
        .module('chat.ui.profile')
        .controller('PasswordController', Controller);

    function Controller($scope, $modalInstance) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.errors = {};
        $scope.input = {
            'password': ''
        };

        $scope.submit = function () {
            $modalInstance.close($scope.input.password);
        };
    }
})();
