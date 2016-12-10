(function () {
    'use strict';

    angular
        .module('chat.admin.proxy')
        .component('proxyAdmin', {
            controller: ProxyAdminController,
            templateUrl: '/chat/admin/proxy/proxy_admin.html'
        });

    /* @ngInject */
    function ProxyAdminController(ProxyService) {
        const vm = this;
    }
})();
