(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .component('journalFilters', {
            bindings: {
                filterState: '<',
                filterChange: '&',
                global: '<'
            },
            controller: JournalFiltersController,
            templateUrl: '/chat/admin/journal/journal_filters.html'
        });

    /* @ngInject */
    function JournalFiltersController(JournalService, UserService) {
        var vm = this;

        vm.filter = angular.copy(vm.filterState);
        vm.inputCategories = {};
        vm.filterChanged = filterChanged;
        vm.updateCategories = updateCategories;
        activate();

        function filterChanged() {
            vm.filterChange({
                filter: vm.filter
            });
        }

        function updateCategories() {
            vm.filter.categories = [];
            angular.forEach(vm.inputCategories, function(value, key) {
                if (value) {
                    vm.filter.categories.push(key);
                }
            });
            filterChanged();
        }

        function activate() {
            angular.forEach(vm.filter.categories, function(e) {
                vm.inputCategories[e] = true;
            });

            JournalService
                .getCategories(vm.global)
                .then(function (data) {
                    vm.categories = data;
                    jQuery.each(data, function(i, e) {
                        vm.inputCategories[e] = vm.inputCategories[e] || false;
                    });
                });

            var user = vm.filter.user;
            if (user && !user.name) {
                UserService
                    .getUser(user.id)
                    .then(function (data) {
                        if (vm.filter.user.id === data.user.id) {
                            vm.filter.user = {
                                id: data.user.id,
                                name: data.user.name
                            };
                        }
                    });
            }
            var admin = vm.filter.admin;
            if (admin && !admin.name) {
                UserService
                    .getUser(user.id)
                    .then(function (data) {
                        if (vm.filter.admin.id === data.user.id) {
                            vm.filter.admin = {
                                id: data.user.id,
                                name: data.user.name
                            };
                        }
                    });
            }
        }
    }
})();
