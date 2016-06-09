angular
    .module("chat.admin.journal")
    .directive("journalFilters", function($http) {
        return {
            restrict: "E",
            scope: {
                filterState: "<",
                filterChange: "=",
                global: "="
            },
            controller: function($scope) {
                $scope.filter = jQuery.extend({}, $scope.filterState);
                $scope.inputCategories = {};

                angular.forEach($scope.filter.categories, function(e) {
                    $scope.inputCategories[e] = true;
                });

                $scope.$watchCollection("inputCategories", function() {
                    $scope.filter.categories = [];
                    angular.forEach($scope.inputCategories, function(value, key) {
                        if (value) {
                            $scope.filter.categories.push(key);
                        }
                    });
                });

                $scope.$watch("filter", function () {
                    $scope.filterChange($scope.filter);
                }, true);

                var load = function() {
                    var resource = $scope.global ? "global" : "room";

                    $http({
                        method: "GET",
                        url: "/rest/journal/categories/" + resource,
                        params: {page: $scope.page}
                    }).success(function (data) {
                        $scope.categories = data;
                        jQuery.each(data, function(i, e) {
                            $scope.inputCategories[e] = $scope.inputCategories[e] || false;
                        });
                    }).error(function (data) {
                        alert(data);
                    });

                    var user = $scope.filter.user;
                    if (user && !user.name) {
                        $http({
                            method: "GET",
                            url: "/rest/users/" + user.id
                        }).success(function (data) {
                            if ($scope.filter.user.id === data.user.id) {
                                $scope.filter.user = {
                                    id: data.user.id,
                                    name: data.user.name
                                };
                            }
                        }).error(function (data) {
                            alert(data);
                        });
                    }
                    var admin = $scope.filter.admin;
                    if (admin && !admin.name) {
                        $http({
                            method: "GET",
                            url: "/rest/users/" + admin.id
                        }).success(function (data) {
                            if ($scope.filter.admin.id === data.user.id) {
                                $scope.filter.admin = {
                                    id: data.user.id,
                                    name: data.user.name
                                };
                            }
                        }).error(function (data) {
                            alert(data);
                        });
                    }
                };

                load();
            },
            templateUrl: "/js/admin/journal/journal_filters.html"
        }
    });
