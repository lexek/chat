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

    //todo: move http to user service
    /* @ngInject */
    function UserModalController($scope, $route, $http, $modalInstance, id) {
        $scope.fullUser = null;

        activate();

        function activate() {
            $http({
                method: 'GET',
                url: StringFormatter.format('/rest/users/{number}', id)
            }).success(function (d) {
                $scope.fullUser = d;
            });
        }

        $scope.requestDelete = function() {
            if (confirm('You sure that you want to delete user "' + $scope.fullUser.user.name + '"?')) {
                $http({
                    method: 'DELETE',
                    url: '/rest/users/' + $scope.fullUser.user.id
                }).success(function() {
                    $route.reload();
                });
            }
        };

        $scope.closeModal = function() {
            $modalInstance.dismiss('cancel');
        };

        $scope.userUpdated = function(updatedUser) {
            console.log(updatedUser);
            $scope.fullUser = updatedUser;
        };
    }
})();
