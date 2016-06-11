(function() {
    'use strict';

    angular
        .module('chat.admin.ticket')
        .component('ticketCount', {
            controller: TicketCountController,
            template: '{{$ctrl.count}}'
        });

    /* @ngInject */
    function TicketCountController($interval, TicketService) {
        var vm = this;

        vm.count = 0;

        activate();

        function activate() {
            vm.$onDestroy = $interval(updateTime, 30000);
            updateTime();
        }

        function updateTime() {
            TicketService
                .getOpenCount()
                .then(function(count) {
                    vm.count = count;
                });
        }
    }
})();
