(function() {
    'use strict';

    angular
        .module('chat.admin.user')
        .factory('UserService', UserService);

    UserService.$inject = ['$http'];

    function UserService($http) {
        return {
            getUser: getUser
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
    }
})();
