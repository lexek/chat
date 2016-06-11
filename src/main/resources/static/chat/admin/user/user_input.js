(function() {
    'use strict';

    angular.module('chat.admin.user')
        .directive('userInput', function () {
            return {
                restrict: 'E',
                scope: {
                    ngModel: '=',
                    ngChange: '=',
                    class: '@?'
                },
                controller: UserInputController,
                template: '<input ' +
                    'title="{{ngModel}}" ' +
                    'type="text" ' +
                    'ng-model="ngModel" ' +
                    'ng-change="ngChange" ' +
                    'placeholder="User name" ' +
                    'typeahead="user as user.name for user in searchUsers($viewValue)" ' +
                    'typeahead-editable="false" ' +
                    'class="form-control {{class}}">'
            };
        });

    /* @ngInject */
    function UserInputController($scope, UserService) {
        $scope.searchUsers = UserService.searchUsers;
    }
})();
