(function() {
    'use strict';

    angular
        .module('chat.admin.user')
        .component('user', {
            bindings: {
                fullUser: '<user',
                onChange: '&'
            },
            controller: UserController,
            templateUrl: '/chat/admin/user/user.html'
        });

    //todo: move http to user service
    /* @ngInject */
    function UserController($http, $modal, ROLES) {
        var editing = '';

        var vm = this;

        vm.auth = [];
        vm.availableRoles = [
            'USER_UNCONFIRMED',
            'USER',
            'MOD'
        ];
        if (document.SELF_ROLE === 'SUPERADMIN') {
            vm.availableRoles.push('ADMIN');
        }

        updateInternalState(vm.fullUser);

        vm.$onChanges = handleChanges;

        function handleChanges(changes) {
            if (changes['fullUser']) {
                updateInternalState(changes['fullUser'].currentValue);
            }
        }

        function updateInternalState(newObject) {
            vm.fullUser = newObject;
            vm.user = vm.fullUser ? vm.fullUser.user : null;
            vm.auth = vm.fullUser ? vm.fullUser.authServices : null;
            vm.input = angular.copy(vm.user);
        }

        function handleUpdate(newObject) {
            console.log(newObject);
            console.log(vm.onChange);
            if (vm.onChange) {
                vm.onChange({
                    user: newObject
                });
            }
        }

        vm.isUser = function() {
            return (vm.fullUser.user.role === 'USER') || (vm.fullUser.user.role === 'USER_UNCONFIRMED');
        };

        vm.saveRenameAvailable = function() {
            $http({
                method: 'PUT',
                data: {
                    rename: vm.input.renameAvailable
                },
                url: '/rest/users/' + vm.user.id
            }).success(function(user) {
                handleUpdate(user);
            });
        };

        vm.saveBanned = function() {
            $http({
                method: 'PUT',
                data: {
                    banned: vm.input.banned
                },
                url: '/rest/users/' + vm.user.id
            }).success(function(user) {
                handleUpdate(user);
            });
        };

        vm.saveRole = function() {
            $http({
                method: 'PUT',
                data: {
                    role: vm.input.role
                },
                url: '/rest/users/' + vm.user.id
            }).success(function (user) {
                handleUpdate(user);
                vm.edit('');
            });
        };

        vm.saveName = function() {
            $http({
                method: 'PUT',
                data: {
                    name: vm.input.name
                },
                url: '/rest/users/' + vm.user.id
            }).success(function (user) {
                handleUpdate(user);
                vm.edit('');
            });
        };

        vm.editing = function(variable) {
            return variable === editing;
        };

        vm.edit = function(variable) {
            editing = variable;
            vm.input[variable] = vm.user[variable];
        };

        vm.canEdit = function(variable) {
            if (variable === 'name') {
                return ((vm.fullUser.user.role === 'USER') || (vm.fullUser.user.role === 'USER_UNCONFIRMED')) &&
                    (document.SELF_ROLE === 'SUPERADMIN');
            }
            if (variable === 'role') {
                return ROLES[vm.fullUser.user.role] < ROLES[document.SELF_ROLE];
            }
        };

        vm.reset = function(variable) {
            vm.edit('');
            vm.input[variable] = vm.user[variable];
        };

        vm.hasAuth = function(auth) {
            return auth in vm.auth;
        };

        vm.showActivity = function() {
            $modal.open({
                templateUrl: 'user_activity.html',
                controller: 'UserActivityController',
                resolve: {
                    user: function () {
                        return vm.user;
                    }
                }
            });
        };

        vm.showEmoticons = function() {
            $modal.open({
                templateUrl: '/templates/user_emoticons.html',
                controller: 'UserEmoticonsController',
                resolve: {
                    user: function () {
                        return vm.user;
                    }
                }
            });
        };

        vm.changePassword = function() {
            $modal.open({
                templateUrl: '/templates/password_modal.html',
                controller: 'UserPasswordController',
                size: 'sm',
                resolve: {
                    user: function () {
                        return vm.user;
                    }
                }
            });
        };
    }
})();
