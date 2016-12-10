(function () {
    'use strict';

    angular
        .module('chat.admin.proxy')
        .factory('ProxyService', ProxyService);

    /* @ngInject */
    function ProxyService($http) {
        return {
            getServices: getServices
        };

        function getServices() {
            //todo
        }
    }
})();
