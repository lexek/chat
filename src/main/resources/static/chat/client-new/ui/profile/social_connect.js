(function () {
    'use strict';

    angular
        .module('chat.ui.profile')
        .directive('socialConnect', SocialConnectDirective);

    /* @ngInject */
    function SocialConnectDirective($modal, $http) {
        return {
            restrict: 'E',
            scope: {
                'connected': '=',
                'change': '&'
            },
            controller: function ($scope) {
                makeProgressable($scope);

                $scope.services = [
                    'twitch',
                    'twitter',
                    'vk',
                    'google',
                    'goodgame'
                ];

                var iconMap = {
                    'twitch': 'fa fa-fw fa-twitch',
                    'google': 'fa fa-fw fa-google',
                    'twitter': 'fa fa-fw fa-twitter',
                    'vk': 'fa fa-fw fa-vk',
                    'token': 'fa fa-fw fa-globe',
                    'password': 'fa fa-fw fa-key',
                    'goodgame': 'gg-icon'
                };

                $scope.getIcon = function (service) {
                    var value = iconMap[service];
                    if (!value) {
                        value = 'fa fa-fw fa-question';
                    }
                    return value;
                };

                $scope.isConnected = function (service) {
                    return $scope.connected.hasOwnProperty(service);
                };

                $scope.addAuth = function (service) {
                    window.open('https://' + HOST_NAME + ':1337/rest/auth/social/' + service);
                };

                $scope.removeAuth = function (service) {
                    if (service === 'token') {
                        $scope.apiToken = '';
                    }
                    $http({
                        'method': 'DELETE',
                        'url': '/rest/auth/' + service
                    }).success(function () {
                        $scope.change();
                    }).error(function (data, status) {
                        if (status === 418) {
                            $modal.open({
                                templateUrl: '/chat/client-new/ui/profile/password.html',
                                controller: 'PasswordController',
                                size: 'sm'
                            }).result.then(function (password) {
                                $scope.removePassword(password);
                            });
                            return;
                        }
                        alert(data.message);
                    });
                };

                $scope.removePassword = function (password) {
                    $http({
                        'method': 'DELETE',
                        'url': '/rest/auth/password',
                        'data': {
                            'password': password
                        },
                        headers: {'Content-Type': 'application/json'}
                    }).success(function () {
                        $scope.change();
                    }).error(function (data) {
                        alert(data.message);
                    });
                };

                $scope.isSingleAuth = function () {
                    var count = Object.keys($scope.connected).length;
                    if ($scope.isConnected('token')) {
                        count--;
                    }
                    return count === 1;
                };

                $scope.setPassword = function () {
                    $modal.open({
                        templateUrl: 'chat/ui/profile/password.html',
                        controller: 'PasswordSettingsController',
                        size: 'sm',
                        resolve: {
                            hasPassword: function () {
                                return $scope.isConnected('password');
                            }
                        }
                    }).result.then(function () {
                        $scope.change();
                    });
                };

                $scope.newToken = function () {
                    $scope.startProgress();
                    $http.post('/token').success(function (data) {
                        var token = data['token'];
                        if (token) {
                            $scope.apiToken = token;
                            $scope.connected['token'] = '';
                        }
                        $scope.stopProgress();
                    }).error(function (data) {
                        $scope.stopProgressWithError(data);
                    });
                };

                $scope.$on('auth-updated', function () {
                    $scope.change();
                });
            },
            templateUrl: '/templates/profile_auth.html'
        };
    }
})();
