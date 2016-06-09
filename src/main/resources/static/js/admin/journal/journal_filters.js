angular
    .module("chat.admin.journal")
    .component("journalFilters", {
        restrict: "E",
        bindings: {
            filterState: "<",
            filterChange: "&",
            global: "@"
        },
        controller: function (JournalService, UserService) {
            'use strict';

            var vm = this;
            vm.filter = jQuery.extend({}, vm.filterState);
            vm.inputCategories = {};
            vm.filterChanged = filterChanged;
            vm.updateCategories = updateCategories;

            angular.forEach(vm.filter.categories, function(e) {
                vm.inputCategories[e] = true;
            });

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

            function load() {
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

            load();
        },
        templateUrl: "/js/admin/journal/journal_filters.html"
    });
