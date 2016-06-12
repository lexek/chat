(function() {
    'use strict';

    angular
        .module('chat.admin.user')
        .directive('userLink', UserLinkDirective);

    /* @njInject */
    function UserLinkDirective($modal) {
        return {
            restrict: 'A',
            scope: {
                userLink: '='
            },
            link: function(scope, element) {
                element.on('click', function(event) {
                    if (event) {
                        event.preventDefault();
                    }
                    $modal.open({
                        templateUrl: '/chat/admin/user/user_modal.html',
                        controller: UserModalController,
                        size: 'sm',
                        resolve: {
                            id: function () {
                                return scope.userLink;
                            }
                        }
                    });
                });
            }
        };
    }

    /* @ngInject */
    function UserModalController($scope, $route, $http, $modal, $modalInstance, roles, id) {
        var editing = '';
        $scope.input = {};
        $scope.auth = [];
        $scope.availableRoles = [
            'USER_UNCONFIRMED',
            'USER',
            'MOD'
        ];

        if (document.SELF_ROLE === 'SUPERADMIN') {
            $scope.availableRoles.push('ADMIN');
        }

        var loadPage = function() {
            $http({
                method: 'GET',
                url: StringFormatter.format('/rest/users/{number}', id)
            }).success(function (d) {
                $scope.user = d.user;
                $scope.input.name = $scope.user.name;
                $scope.input.role = $scope.user.role;
                $scope.input.banned = $scope.user.banned;
                $scope.input.renameAvailable = $scope.user.renameAvailable;
                $scope.auth = d.authServices;
            });
        };

        $scope.isUser = function() {
            return ($scope.user.role === 'USER') || ($scope.user.role === 'USER_UNCONFIRMED');
        };

        $scope.saveRenameAvailable = function() {
            $http({
                method: 'PUT',
                data: {
                    rename: $scope.input.renameAvailable
                },
                url: '/rest/users/' + $scope.user.id
            }).success(function() {
                $scope.user.renameAvailable = $scope.input.renameAvailable;
            });
        };

        $scope.saveBanned = function() {
            $http({
                method: 'PUT',
                data: {
                    banned: $scope.input.banned
                },
                url: '/rest/users/' + $scope.user.id
            }).success(function() {
                $scope.user.banned = $scope.input.banned;
            });
        };

        $scope.saveRole = function() {
            $http({
                method: 'PUT',
                data: {
                    role: $scope.input.role
                },
                url: '/rest/users/' + $scope.user.id
            }).success(function () {
                $scope.user.role = $scope.input.role;
                $scope.edit('');
            });
        };

        $scope.saveName = function() {
            $http({
                method: 'PUT',
                data: {
                    name: $scope.input.name
                },
                url: '/rest/users/' + $scope.user.id
            }).success(function () {
                $scope.user.name = $scope.input.name;
                $scope.edit('');
            });
        };

        $scope.editing = function(variable) {
            return variable === editing;
        };

        $scope.edit = function(variable) {
            editing = variable;
            $scope.input[variable] = $scope.user[variable];
        };

        $scope.canEdit = function(variable) {
            if (variable === 'name') {
                return (($scope.user.role === 'USER') || ($scope.user.role === 'USER_UNCONFIRMED')) &&
                    (document.SELF_ROLE === 'SUPERADMIN');
            }
            if (variable === 'role') {
                return roles[$scope.user.role] < roles[document.SELF_ROLE];
            }
        };

        $scope.reset = function(variable) {
            $scope.edit('');
            $scope.input[variable] = $scope.user[variable];
        };

        $scope.requestDelete = function() {
            if (confirm('You sure that you want to delete user "' + $scope.user.name + '"?')) {
                $http({
                    method: 'DELETE',
                    url: '/rest/users/' + $scope.user.id
                }).success(function() {
                    $route.reload();
                });
            }
        };

        $scope.hasAuth = function(auth) {
            return auth in $scope.auth;
        };

        $scope.showActivity = function() {
            $modal.open({
                templateUrl: 'user_activity.html',
                controller: 'UserActivityController',
                resolve: {
                    user: function () {
                        return $scope.user;
                    }
                }
            });
        };

        $scope.showEmoticons = function() {
            $modal.open({
                templateUrl: '/templates/user_emoticons.html',
                controller: 'UserEmoticonsController',
                resolve: {
                    user: function () {
                        return $scope.user;
                    }
                }
            });
        };

        $scope.changePassword = function() {
            $modal.open({
                templateUrl: '/templates/password_modal.html',
                controller: 'UserPasswordController',
                size: 'sm',
                resolve: {
                    user: function () {
                        return $scope.user;
                    }
                }
            });
        };

        $scope.closeModal = function() {
            $modalInstance.dismiss('cancel');
        };

        loadPage();
    }
})();
