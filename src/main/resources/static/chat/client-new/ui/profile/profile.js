(function () {
    'use strict';

    angular
        .module('chat.ui.profile')
        .controller('ProfileController', Controller);

    /* @ngInject */
    function Controller($scope, $modalInstance, $modal, $http, chatService) {
        makeClosable($scope, $modalInstance);

        $scope.self = chatService.self;
        $scope.profile = null;

        $scope.showEmailSettings = function () {
            var user = $scope.profile.user;
            $modal.open({
                templateUrl: '/chat/client-new/ui/profile/email.html',
                controller: 'EmailSettingsController',
                size: 'sm',
                resolve: {
                    hasPendingVerification: function () {
                        return user['email'] && !user['emailVerified'];
                    }
                }
            }).result.then(function (result) {
                alert(result);
                loadProfile();
            });
        };

        var loadProfile = function () {
            if ($scope.self.role >= globalLevels.USER_UNCONFIRMED) {
                $http({
                    method: 'get',
                    url: '/rest/profile'
                }).success(function (data) {
                    $scope.profile = data;
                });
            }
        };

        $scope.toggleCheckIp = function () {
            var newValue = !$scope.profile.user.checkIp;
            $http({
                'method': 'PUT',
                'url': '/rest/auth/self/checkIp',
                'data': {
                    'value': newValue
                }
            }).success(function () {
                $scope.profile.user.checkIp = newValue;
            }).error(function (data) {
                alert(data.message);
            });
        };

        $scope.update = loadProfile;

        loadProfile();
    }
})();
