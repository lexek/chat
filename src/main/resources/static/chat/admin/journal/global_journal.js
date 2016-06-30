(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .component('globalJournal', {
            controller: JournalController,
            templateUrl: '/chat/admin/journal/global_journal.html'
        });

    /* @ngInject */
    function JournalController(JournalService, $location, title) {
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
        vm.onPageChange = onPageChange;
        activate();

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
                $location.search(angular.extend(newSimple, {
                    page: 1
                }));
                vm.filter = angular.copy(newFilter);
                vm.page = 1;
                fetchData();
            }
        }

        function onPageChange() {
            $location.search('page', vm.page.toString());
            fetchData();
        }

        function fetchData() {
            vm.waitingFor = JournalService
                .getJournalPage(vm.filter, vm.page - 1)
                .then(function (data) {
                    vm.items = data.data;
                    vm.totalPages = data.pageCount;
                    title.secondary = 'page ' + (vm.page) + '/' + (vm.totalPages);
                });
        }

        function activate() {
            var locationSearch = $location.search(),
                page = parseInt(locationSearch.page, 10),
                userId = locationSearch.userId,
                adminId = locationSearch.adminId,
                categories = locationSearch.category;
            if (userId) {
                vm.filter.user = {
                    id: parseInt(userId, 10)
                };
            }
            if (adminId) {
                vm.filter.admin = {
                    id: parseInt(adminId, 10)
                };
            }
            if (categories) {
                if (!Array.isArray(categories)) {
                    categories = [categories];
                }
                if (categories.length > 0) {
                    vm.filter.categories = categories;
                }
            }
            if (isNaN(page) || page < 1) {
                $location.search('page', '1');
            } else {
                vm.page = page;
                fetchData();
            }
        }
    }
})();
