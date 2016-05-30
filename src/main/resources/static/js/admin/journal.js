angular.module("chat.admin.journal", ["chat.admin.utils"])
    .directive("journal", function() {
        return {
            restrict: "E",
            scope: {
                "global": "=",
                "useLocation": "=?",
                "room": "=?",
                "onPageChange": "=?"
            },
            controller: function($scope, $http, $location) {
                $scope.items = [];
                $scope.page = 0;
                $scope.totalPages = 0;
                $scope.filter = {};

                //todo: load only once

                var load = function() {
                    var resource = $scope.global ? "global" : "room";

                    if ($scope.room) {
                        resource += "/" + $scope.room.id;
                    }

                    $http({
                        method: "GET",
                        url: "/rest/journal/" + resource,
                        params: {
                            page: $scope.page,
                            user: $scope.filter.user ? $scope.filter.user.id : null,
                            admin: $scope.filter.admin ? $scope.filter.admin.id : null,
                            category: $scope.filter.categories
                        }
                    }).success(function (data) {
                        $scope.items = data["data"];
                        $scope.totalPages = data["pageCount"];
                        if ($scope.onPageChange) {
                            $scope.onPageChange(data.page, data.pageCount);
                        }
                    }).error(function (data) {
                        alert(data);
                    });
                };

                $scope.$watch("filter", function(newFilter) {
                    if (newFilter.ready) {
                        console.log(newFilter);
                        load();
                    }
                }, true);

                $scope.previousPage = function() {
                    if ($scope.page !== 0) {
                        if ($scope.useLocation) {
                            $location.search("page", ($scope.page - 1).toString());
                        } else {
                            $scope.page--;
                            load();
                        }
                    }
                };

                $scope.nextPage = function() {
                    if (($scope.page+1) < $scope.totalPages) {
                        if ($scope.useLocation) {
                            $location.search("page", ($scope.page + 1).toString());
                        } else {
                            $scope.page++;
                            load();
                        }
                    }
                };

                $scope.hasNextPage = function() {
                    return ($scope.page+1) < $scope.totalPages
                };

                var classMap = {
                    "DELETED_EMOTICON": "warning",
                    "ROOM_BAN": "warning",
                    "DELETED_PROXY": "warning",
                    "ROOM_UNBAN": "success",
                    "ROOM_ROLE": "success"
                };

                var actionMap = {
                    "USER_UPDATE": "User changed",
                    "NAME_CHANGE": "User name changed",
                    "NEW_EMOTICON": "New emoticon",
                    "IMAGE_EMOTICON": "Updated emoticon image",
                    "DELETED_EMOTICON": "Deleted emoticon",
                    "NEW_ROOM": "Created room",
                    "DELETED_ROOM": "Deleted room",
                    "PASSWORD": "Changed password",
                    "NEW_PROXY": "Proxy added",
                    "DELETED_PROXY": "Proxy removed",
                    "NEW_POLL": "Poll created",
                    "CLOSE_POLL": "Poll closed",
                    "ROOM_BAN": "User banned",
                    "ROOM_UNBAN": "User unbanned",
                    "ROOM_ROLE": "Role changed",
                    "NEW_ANNOUNCEMENT": "Announcement created",
                    "INACTIVE_ANNOUNCEMENT": "Announcement archived"
                };

                $scope.getClassForJournalAction = function(action) {
                    return 'list-group-item-' + classMap[action];
                };

                $scope.translateAction = function(action) {
                    return actionMap[action];
                };

                {
                    var locationSearch = $location.search();
                    var page = parseInt(locationSearch["page"]);
                    if (isNaN(page) || page < 0) {
                        if ($scope.useLocation) {
                            $location.search("page", "0");
                        } else {
                            $scope.page = 0;
                        }
                    } else {
                        $scope.page = page;
                    }
                }
            },
            templateUrl: "/templates/journal.html"
        }
    })
    .directive("journalFilters", function($http) {
        return {
            restrict: "E",
            scope: {
                filter: "=",
                global: "="
            },
            controller: function($scope) {
                $scope.filter = jQuery.extend({
                    admin: undefined,
                    user: undefined,
                    categories: [],
                    ready: true
                }, $scope.filter);
                $scope.inputCategories = {};

                $scope.$watchCollection("inputCategories", function() {
                    $scope.filter.categories = [];
                    angular.forEach($scope.inputCategories, function(value, key) {
                        if (value) {
                            $scope.filter.categories.push(key);
                        }
                    });
                });

                var load = function() {
                    var resource = $scope.global ? "global" : "room";

                    $http({
                        method: "GET",
                        url: "/rest/journal/categories/" + resource,
                        params: {page: $scope.page}
                    }).success(function (data) {
                        $scope.categories = data;
                        jQuery.each(data, function(i, e) {
                            $scope.inputCategories[e] = false;
                        });
                    }).error(function (data) {
                        alert(data);
                    });
                };

                load();
            },
            templateUrl: "/templates/journal_filters.html"
        }
    });
