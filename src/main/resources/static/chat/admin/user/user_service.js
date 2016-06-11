(function() {
    'use strict';

    angular
        .module('chat.admin.user')
        .factory('UserService', UserService);

    /* @ngInject */
    function UserService($http) {
        return {
            getUser: getUser,
            searchUsers: searchUsers
        };

        function getUser(id) {
            return $http({
                method: 'GET',
                url: '/rest/users/' + id
            })
                .then(requestComplete)
                .catch(requestFailed);

            function requestComplete(response) {
                return response.data;
            }

            function requestFailed(error) {
                console.error('XHR Failed for getUser.' + error.data);
            }
        }

        function searchUsers(partialName) {
            return $http({
                method: 'GET',
                url: '/rest/users/search',
                params: {
                    search: partialName
                }
            })
                .then(requestComplete)
                .catch(requestFailed);

            function requestComplete(response) {
                return response.data;
            }

            function requestFailed(error) {
                console.error('XHR Failed for searchUser.' + error.data);
            }
        }
    }
})();
