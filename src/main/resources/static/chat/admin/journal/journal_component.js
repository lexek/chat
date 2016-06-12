(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .component('journal', {
            bindings: {
                'global': '<',
                'room': '<',
                'useLocation': '<',
                'onPageChange': '&'
            },
            controller: JournalController,
            templateUrl: '/chat/admin/journal/journal_component.html'
        });

    /* @ngInject */
    function JournalController(JournalService, $location, title) {
        var vm = this;

        vm.items = [];
        vm.page = 0;
        vm.totalPages = 0;
        vm.filter = {
            admin: undefined,
            user: undefined,
            categories: []
        };
        vm.previousPage = previousPage;
        vm.nextPage = nextPage;
        vm.hasNextPage = hasNextPage;
        vm.onFilterChange = onFilterChange;
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
                if (vm.useLocation) {
                    $location.search(angular.extend(newSimple, {
                        page: 0
                    }));
                } else {
                    vm.filter = angular.copy(newFilter);
                    fetchData();
                }
            }
        }

        function goToPage(page) {
            if (vm.useLocation) {
                $location.search('page', page.toString());
            } else {
                vm.page = page;
                fetchData();
            }
        }

        function previousPage() {
            if (vm.page !== 0) {
                goToPage(vm.page - 1);
            }
        }

        function nextPage() {
            if ((vm.page + 1) < vm.totalPages) {
                goToPage(vm.page + 1);
            }
        }

        function hasNextPage() {
            return (vm.page + 1) < vm.totalPages;
        }

        function fetchData() {
            JournalService.getJournalPage(vm.filter, vm.global, vm.page, vm.room)
                .then(function (data) {
                    vm.items = data.data;
                    vm.totalPages = data.pageCount;
                    if (vm.useLocation) {
                        title.secondary = 'page ' + (vm.page + 1) + '/' + (vm.totalPages);
                    }
                });
        }

        //todo: simplify?
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
            if (isNaN(page) || page < 0) {
                if (vm.useLocation) {
                    $location.search('page', '0');
                } else {
                    vm.page = 0;
                    fetchData();
                }
            } else {
                vm.page = page;
                fetchData();
            }
        }
    }
})();
