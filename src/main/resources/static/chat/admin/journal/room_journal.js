(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .component('journalModal', {
            bindings: {
                'room': '<'
            },
            controller: JournalController,
            templateUrl: '/chat/admin/journal/room_journal.html'
        });

    /* @ngInject */
    function JournalController(JournalService) {
        var vm = this;

        vm.items = [];
        vm.page = 1;
        vm.totalPages = 1;
        vm.filter = {
            admin: undefined,
            user: undefined,
            categories: []
        };
        vm.waitingFor = null;
        vm.onFilterChange = onFilterChange;
        vm.onPageChange = fetchData;
        fetchData();

        function simplifyFilter(filter) {
            return {
                userId: filter.user ? filter.user.id : null,
                adminId: filter.admin ? filter.admin.id : null,
                category: filter.categories && filter.categories.length ? filter.categories : null
            };
        }

        function onFilterChange(newFilter) {
            var newSimple = simplifyFilter(newFilter);
            var oldSimple = simplifyFilter(vm.filter);
            if (!angular.equals(newSimple, oldSimple)) {
                vm.filter = angular.copy(newFilter);
                vm.page = 1;
                fetchData();
            }
        }

        function fetchData() {
            console.log(vm.room);
            vm.waitingFor = JournalService
                .getJournalPage(vm.filter, vm.page - 1, vm.room)
                .then(function (data) {
                    vm.items = data.data;
                    vm.totalPages = data.pageCount;
                });
        }
    }
})();
