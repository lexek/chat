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
        vm.getClassForJournalAction = getClassForJournalAction;
        vm.translateAction = translateAction;
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

        var classMap = {
            'DELETED_EMOTICON': 'warning',
            'ROOM_BAN': 'warning',
            'DELETED_PROXY': 'warning',
            'ROOM_UNBAN': 'success',
            'ROOM_ROLE': 'success'
        };
        var actionMap = {
            'USER_UPDATE': 'User changed',
            'NAME_CHANGE': 'User name changed',
            'NEW_EMOTICON': 'New emoticon',
            'IMAGE_EMOTICON': 'Updated emoticon image',
            'DELETED_EMOTICON': 'Deleted emoticon',
            'NEW_ROOM': 'Created room',
            'DELETED_ROOM': 'Deleted room',
            'PASSWORD': 'Changed password',
            'NEW_PROXY': 'Proxy added',
            'DELETED_PROXY': 'Proxy removed',
            'NEW_POLL': 'Poll created',
            'CLOSE_POLL': 'Poll closed',
            'ROOM_BAN': 'User banned',
            'ROOM_UNBAN': 'User unbanned',
            'ROOM_ROLE': 'Role changed',
            'NEW_ANNOUNCEMENT': 'Announcement created',
            'INACTIVE_ANNOUNCEMENT': 'Announcement archived'
        };

        function getClassForJournalAction(action) {
            return 'list-group-item-' + classMap[action];
        }

        function translateAction(action) {
            return actionMap[action];
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
