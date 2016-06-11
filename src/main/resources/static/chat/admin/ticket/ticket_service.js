(function() {
    'use strict';

    angular
        .module('chat.admin.ticket')
        .factory('TicketService', TicketService);

    /* @ngInject */
    function TicketService($http) {
        return {
            getOpenCount: getOpenCount
        };

        function getOpenCount() {
            return $http({
                method: 'GET',
                url: '/rest/tickets/open/count'
            })
                .then(requestComplete)
                .catch(requestFailed);

            function requestComplete(response) {
                return response.data.count;
            }

            function requestFailed(error) {
                console.error('XHR Failed for getOpenCount.' + error.data);
            }
        }
    }
})();
