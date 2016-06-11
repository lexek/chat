(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .factory('JournalService', JournalService);

    /* @ngInject */
    function JournalService($http) {
        return {
            getJournalPage: getJournalPage,
            getCategories: getCategories
        };

        function getJournalPage(filter, global, page, room) {
            var resource = global ? 'global' : 'room';

            if (room) {
                resource += '/' + room.id;
            }

            return $http({
                method: 'GET',
                url: '/rest/journal/' + resource,
                params: {
                    page: page,
                    user: filter.user ? filter.user.id : null,
                    admin: filter.admin ? filter.admin.id : null,
                    category: filter.categories
                }
            })
                .then(requestComplete)
                .catch(requestFailed);

            function requestComplete(response) {
                return response.data;
            }

            function requestFailed(error) {
                console.error('XHR Failed for getJournalPage.' + error.data);
            }
        }

        function getCategories(global) {
            return $http({
                method: 'GET',
                url: '/rest/journal/categories/' + (global ? 'global' : 'room')
            })
                .then(requestComplete)
                .catch(requestFailed);

            function requestComplete(response) {
                return response.data;
            }

            function requestFailed(error) {
                console.error('XHR Failed for getCategories.' + error.data);
            }
        }
    }
})();
