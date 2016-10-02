//from google closure
Array.prototype.remove = function(obj) {
    var i = this.indexOf(obj);
    var rv;
    if ((rv = i >= 0)) {
        this.splice(i, 1);
    }
    return rv;
};

var AdminServices = angular.module("AdminServices", []);

var AlertServiceFactory = function() {
    var AlertService = function () {
        this.alerts = [];
        this.newAlertCallback = angular.noop;
    };

    AlertService.prototype.alert = function(type, message) {
        var a = {
            "type": type,
            "message": message
        };
        this.alerts.push(a);
        this.newAlertCallback(a);
    };

    return new AlertService();
};

var TitleServiceFactory = function() {
    var TitleService = function () {
        this.title = null;
        this.secondary = null;
    };

    return new TitleService();
};

AdminServices.filter('slice', function() {
  return function(arr, start, end) {
    return (arr || []).slice(start, end);
  };
});

AdminServices.factory("alert", AlertServiceFactory);
AdminServices.factory("title", TitleServiceFactory);

var AdminApplication = angular.module(
    "AdminApplication",
    [
        "ngRoute",
        "ngAnimate",
        "AdminServices",
        "relativeDate",
        "ui.inflector",
        "ui.bootstrap",
        "ui.bootstrap.datetimepicker",
        "highcharts-ng",
        "ngSanitize",
        "rgkevin.datetimeRangePicker",
        "cgBusy",
        "chat.admin.auth",
        "chat.admin.journal",
        "chat.admin.utils",
        "chat.admin.ticket",
        'chat.admin.history',
        "templates"
    ]
);

AdminApplication.value('cgBusyDefaults',{
    message: 'LOADING',
    templateUrl: '/chat/admin/utils/busy.html'
});

//todo: remove later
Role = function(title, value) {
    this.title = title;
    this.value = value;
};

Role.prototype.valueOf = function() {
    return this.value;
};

var ROLES = {
    'UNAUTHENTICATED': new Role("unauthenticated", 0),
    'GUEST': new Role("guest", 1),
    'USER_UNCONFIRMED': new Role("unconfirmed user", 2),
    'USER': new Role("user", 2),
    "SUPPORTER": new Role("supporter", 3),
    'MOD': new Role("moderator", 4),
    'ADMIN': new Role("administrator", 5),
    'SUPERADMIN': new Role("administrator", 6)
};

/* @ngInject */
var AlertController = function($scope, $timeout, alert) {
    alert.newAlertCallback = function(a) {
        $timeout(function(){alert.alerts.remove(a)}, 1000);
    };

    $scope.list = function() {
        return alert.alerts;
    }
};

AdminApplication.controller("AlertController", AlertController);

/* @ngInject */
var TimeRangePickerController = function($scope, $modalInstance, date) {
    $scope.hours = {
        from: date.from.getHours(),
        to: date.to.getHours()
    };

    $scope.range = {
        date: {
            from: date.from,
            to: date.to,
            max: new Date()
        },
        "hasDatePickers": true,
        "hasTimeSliders": false
    };

    $scope.labels = {
        date: {
            from: 'Start date',
            to: 'End date'
        }
    };

    $scope.ok = function() {
        var f = new Date($scope.range.date.from);
        f.setHours(0,0,0,0);
        var t = new Date($scope.range.date.to);
        t.setHours(0,0,0,0);
        var result = {
            from: f.getTime() + $scope.hours.from * 3600000,
            to: t.getTime() + $scope.hours.to * 3600000
        };
        $modalInstance.close(result);
    }
};

AdminApplication.controller("TimeRangePickerController", TimeRangePickerController);

/* @ngInject */
var SteamController = function($scope, $http) {
    $scope.inProgress = false;

    $scope.updateDatabase = function() {
        $scope.inProgress = true;
        $http({
            method: "POST",
            url: "/rest/steamGames/syncDb"
        }).success(function () {
            $scope.inProgress = false;
        }).error(function() {
            $scope.inProgress = false;
        });
    }
};

AdminApplication.controller("SteamController", ["$scope", "$http", SteamController]);

/* @ngInject */
var TitleController = function($scope, title) {
    $scope.title = function() {
        return title.title;
    };

    $scope.secondary = function() {
        return title.secondary;
    };
};

AdminApplication.controller("TitleController", TitleController);

/* @ngInject */
var DashboardController = function($scope, $http, alert) {
    var loadMetrics = function () {
        $http({method: "GET", url: "/rest/stats/global/metrics"})
            .success(function (d, status, headers, config) {
                var data = d["metrics"];
                var streams = d["streams"];

                var result = {
                    "online": [],
                    "online.authenticated": [],
                    "online.active": [],
                    "activity": [],
                    "viewers": []
                };

                angular.forEach(data, function (metric) {
                    var values = result[metric.name];
                    values.push([metric.time, "value" in metric ? metric.value : null]);
                });

                $('#onlineConnectionsChart').highcharts({
                    chart: {
                        type: 'areaspline',
                        zoomType: 'x'
                    },
                    title: {
                        text: null
                    },
                    xAxis: {
                        type: 'datetime',
                        title: {
                            enabled: false
                        }
                    },
                    yAxis: [
                        {
                            title: {
                                text: "online",
                                enabled: false
                            },
                            min: 0,
                            "allowDecimals": false
                        },
                        {
                            title: {
                                text: "chatters",
                                enabled: false
                            },
                            min: 0,
                            "allowDecimals": true,
                            opposite: true
                        }
                    ],
                    legend: {
                        enabled: true
                    },
                    series: [
                        {
                            "name": "total online",
                            "data": result["online"]
                        },
                        {
                            "name": "authenticated",
                            "data": result["online.authenticated"]
                        },
                        {
                            "name": "active users",
                            "data": result["online.active"]
                        },
                        {
                            type: "spline",
                            "name": "viewers",
                            "data": result["viewers"]
                        },
                        {
                            type: "spline",
                            yAxis: 1,
                            "name": "activity",
                            "data": result["activity"],
                            visible: false
                        }
                    ],
                    plotOptions: {
                        areaspline: {
                            lineWidth: 0,
                            marker: {
                                enabled: false,
                                symbol: 'circle',
                                radius: 2,
                                states: {
                                    hover: {
                                        enabled: true
                                    }
                                }
                            },
                            states: {
                                hover: {
                                    lineWidth: 0
                                }
                            }
                        },
                        spline: {
                            lineWidth: 1.5,
                            marker: {
                                enabled: false,
                                radius: 2
                            },
                            states: {
                                hover: {
                                    lineWidth: 1.5
                                }
                            }
                        }
                    },
                    tooltip: {
                        shared: true,
                        crosshairs: true
                    }
                });
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    loadMetrics();
};

/* @ngInject */
var RoomsController = function($scope, $http) {
    $scope.showForm = false;
    $scope.entries = [];
    $scope.filter = "";
    $scope.inProgress = false;
    $scope.input = {
        name: ""
    };

    var loadRooms = function() {
        $http({
            method: "GET",
            url: "/rest/rooms/all"
        }).success(function (d) {
            $scope.entries = d;
        });
    };

    $scope.toggleForm = function(value) {
        $scope.showForm = value;
    };

    $scope.submitForm = function() {
        var data = {
            "topic": "",
            "name": $scope.input.name
        };
        $scope.inProgress = true;
        $http({
            method: "POST",
            data: data,
            url: "/rest/rooms/new"
        }).success(function () {
            loadRooms();
            $scope.input.name = "";
            $scope.showForm = false;
            $scope.inProgress = false;
        }).error(function(data) {
            alert(data);
            $scope.inProgress = false;
        });
    };

    loadRooms();
};

AdminApplication.controller("RoomsController", RoomsController);

AdminApplication.controller('UserEmoticonsController', UserEmoticonsController);

/* @ngInject */
function UserEmoticonsController($scope, $http, $modal, user) {
    $scope.user = user;
    $scope.users = [];
    $scope.total = 0;

    var load = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/stats/user/{number}/emoticons", user.id)
        }).success(function(data) {
            $scope.emoticons = data;
            $scope.total = data.reduce(function(prev, e) {
                return prev + e.count;
            }, 0);
        }).error(function(data) {
            alert.alert("danger", data);
        });
    };

    load();
}

/* @ngInject */
var EmoticonUsersController = function($scope, $http, emoticon) {
    $scope.emoticon = emoticon;
    $scope.users = [];

    var load = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/stats/global/emoticons/{number}", emoticon.id)
        }).success(function(data) {
            $scope.users = data;
            $scope.total = data.reduce(function(prev, e) {
                return prev + e.count;
            }, 0);
        }).error(function(data) {
            alert.alert("danger", data);
        });
    };

    load();
};

/* @ngInject */
var EmoticonsController = function($scope, $http, $modal, alert) {
    $scope.emoticons = [];
    $scope.formData = {};
    $scope.order = "code";
    $scope.orderDesc = false;
    $scope.popularity = {};

    var loadEmoticons = function() {
        $scope.emoticons.length = 0;
        $http({
            method: "GET",
            url: "/rest/emoticons/all"
        }).success(function(d) {
            $scope.emoticons = d;
        }).error(function(data) {
            alert.alert("danger", data);
        });
        $http({
            method: "GET",
            url: "/rest/stats/global/emoticons"
        }).success(function(data) {
            angular.forEach(data, function(e) {
                $scope.popularity[e.emoticon.id] = e.count
            });
        }).error(function(data) {
            alert.alert("danger", data);
        });
    };

    $scope.requestDelete = function(id) {
        $http({
            method: "DELETE",
            url: "/rest/emoticons/"+id
        }).success(function() {
            loadEmoticons();
        }).error(function(data) {
            alert.alert("danger", data);
        });
    };

    $scope.submitForm = function() {
        $http({
            method  : "POST",
            url     : "/rest/emoticons/add",
            data    : $scope.formData,
            headers : { "Content-Type": "multipart/form-data" }
        }).success(function() {
            loadEmoticons();
        });
    };

    $scope.pop = function(emoticon) {
        return $scope.popularity[emoticon.id] || 0;
    };

    $scope.orderBy = function(orderVar) {
        console.log($scope.order + " -> " + orderVar)
        if (orderVar === $scope.order) {
            $scope.orderDesc = !$scope.orderDesc;
        } else {
            $scope.order = orderVar;
            $scope.orderDesc = false;
        }
    };

    $scope.getSortIconClass = function(orderVar) {
        if (orderVar === $scope.order) {
            if ($scope.orderDesc) {
                return "fa-sort-up";
            } else {
                return "fa-sort-down";
            }
        } else {
            return "fa-sort";
        }
    };

    $scope.showEmoticonUsers = function(emoticon) {
        $modal.open({
            templateUrl: "/templates/emoticon_users.html",
            controller: EmoticonUsersController,
            resolve: {
                emoticon: function () {
                    return emoticon;
                }
            }
        });
    };

    loadEmoticons();
};

/* @ngInject */
var UsersController = function($scope, $location, $http, alert, title) {
    $scope.users = [];
    $scope.search = null;
    $scope.totalPages = 0;
    $scope.user = null;

    var loadPage = function() {
        $http({
            method: "GET",
            url: "/rest/users/all",
            params: {
                search: $scope.search,
                page: $scope.page
            }
        }).success(function (d) {
            $scope.users = d.data;
            $scope.totalPages = d.pageCount;
            title.secondary = "page " + ($scope.page+1) + "/" + ($scope.totalPages);
            $scope.user = null;
        });
    };

    $scope.selectUser = function(u) {
        if ($scope.user && u && ($scope.user.user.id === u.user.id)) {
            $scope.user = null;
        } else {
            $scope.user = u;
        }
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $location.search("page", ($scope.page-1).toString());
        }
    };

    $scope.nextPage = function() {
        if ((page+1) < $scope.totalPages) {
            $location.search("page", ($scope.page+1).toString());
        }
    };

    $scope.hasNextPage = function() {
        return (page+1) < $scope.totalPages
    };

    $scope.orderBy = function(orderVar) {
        $location.search("page", "0");
        if (orderVar === $scope.order) {
            $scope.orderDesc = !$scope.orderDesc;
            $location.search("orderDesc", $scope.orderDesc ? "true" : null);
        } else {
            $location.search("orderBy", orderVar);
            $location.search("orderDesc", null);
            $scope.orderDesc = false;
        }
    };

    $scope.getSortIconClass = function(orderVar) {
        if (orderVar === $scope.order) {
            if ($scope.orderDesc) {
                return "fa-sort-up";
            } else {
                return "fa-sort-down";
            }
        } else {
            return "fa-sort";
        }
    };

    $scope.doSearch = function() {
        if ($scope.searchInput) {
            $location.search("page", "0");
            $location.search("search", $scope.searchInput);
        } else {
            $scope.resetSearch();
        }
    };

    $scope.resetSearch = function() {
        $location.search("page", "0");
        $location.search("search", null);
    };

    $scope.userUpdated = function(updatedUser) {
        $scope.user = updatedUser;
        $scope.users = $scope.users.map(function(user) {
            if (user.user.id === updatedUser.user.id) {
                return updatedUser;
            } else {
                return user;
            }
        });
    };

    {
        var locationSearch = $location.search();
        var page = parseInt(locationSearch["page"]);
        if (isNaN(page) || page < 0) {
            $location.search("page", "0");
        } else {
            $scope.page = page;
            var order = locationSearch["orderBy"];
            if (!order) {
                order = "id";
            }
            $scope.order = order;
            $scope.orderDesc = locationSearch["orderDesc"] === "true";
            $scope.search = locationSearch["search"];
            if ($scope.search) {
                $scope.searchInput = $scope.search;
            }
            loadPage();
        }
    }
};

/* @ngInject */
var OnlineController = function($scope, $http, $modal, alert, title) {
    $scope.connections = [];
    $scope.orderVar = "user.name";
    $scope.orderDesc = false;
    $scope.blockedIps = [];

    var loadOnline = function() {
        $scope.connections.length = 0;
        $http({method: "GET", url: "/rest/users/online"})
            .success(function (data, status, headers, config) {
                $scope.connections = data;
                title.secondary = $scope.connections.length;
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    var loadBlockedIps = function() {
        $scope.connections.length = 0;
        $http({method: "GET", url: "/rest/security/ip-block"})
            .success(function (data, status, headers, config) {
                $scope.blockedIps = data;
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.orderBy = function(orderVar) {
        if (orderVar === $scope.orderVar) {
            $scope.orderDesc = !$scope.orderDesc;
        } else {
            $scope.orderVar = orderVar;
            $scope.orderDesc = false;
        }
    };

    $scope.getSortIconClass = function(orderVar) {
        if (orderVar === $scope.orderVar) {
            if ($scope.orderDesc) {
                return "fa-sort-up";
            } else {
                return "fa-sort-down";
            }
        } else {
            return "fa-sort";
        }
    };

    $scope.blockIp = function(ip) {
        $http({method: "POST", url: "/rest/security/ip-block", params: {ip: ip}})
            .success(function (data, status, headers, config) {
                $scope.blockedIps = data;
            })
            .error(function (data, status, headers, config) {
            });
    };

    $scope.unblockIp = function(ip) {
        $http({method: "DELETE", url: "/rest/security/ip-block", params: {ip: ip}})
            .success(function (data, status, headers, config) {
                $scope.blockedIps = data;
            })
            .error(function (data, status, headers, config) {
            });
    };

    $scope.isBlocked = function(ip) {
        return $scope.blockedIps.indexOf(ip) !== -1;
    };

    {
        loadOnline();
        loadBlockedIps();
    }
};

AdminApplication.controller("UserActivityController", UserActivityController);

/* @ngInject */
function UserActivityController($scope, $http, $modal, user) {
    $scope.user = user;
    $http({method: "GET", url: "/rest/stats/user/" + user.id + "/activity"})
        .success(function(data, status, headers, config) {
            var activity = {};
            angular.forEach(data, function(v, k) {
                activity[k/1000] = v;
            });
            var startDate = new Date();
            startDate.setDate(startDate.getDate() - 7);
            var cfg = {
                label: {
                    position: "left",
                    align: "left",
                    width: 50
                },
                itemSelector: "#userActivity",
                domain: 'day',
                subDomain: 'hour',
                range: 8,
                cellSize: 20,
                domainGutter: 10,
                data: activity,
                start: startDate,
                colLimit: 24,
                verticalOrientation: true,
                itemName: ["message", "messages"]
            };
            var cal = new CalHeatMap();
            cal.init(cfg);
        })
}

/* @ngInject */
var UserPasswordController = function($scope, $http, $modalInstance, user) {
    $scope.user = user;
    $scope.password = "";

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };

    $scope.submit = function() {
        $scope.inProgress = true;
        $http({
            method: "PUT",
            data: {
                password: $scope.password
            },
            url: "/rest/users/" + user.id + "/password"
        }).success(function() {
            $scope.inProgress = false;
            $modalInstance.dismiss("ok");
        }).error(function(data) {
            if (data) {
                $scope.error = data.message;
            } else {
                $scope.error = "error";
            }
            $scope.inProgress = false;
        })
    }
};

AdminApplication.controller("UserPasswordController", ["$scope", "$http", "$modalInstance", "userId", UserPasswordController]);

/* @ngInject */
var JournalController = function($scope, title) {
    $scope.onPageChange = function(current, total) {
        title.secondary = "page " + (current + 1) + "/" + (total);
    }
};

/* @ngInject */
var TicketController = function($scope) {
    $scope.showReply = false;
    $scope.text = "";

    $scope.toggleReply = function() {
        $scope.showReply = !$scope.showReply;
    };
};

AdminApplication.controller("TicketController", ["$scope", TicketController]);

/* @ngInject */
var TicketsController = function($scope, $location, $http, $modal, alert, title) {
    $scope.entries = [];
    $scope.totalPages = 0;
    $scope.secondaryTitle = $scope.page;
    $scope.opened = true;

    var loadPage = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/tickets/{string}/all", $scope.opened ? "open" : "closed"),
            params: {
                page: $scope.page
            }
        }).success(function (d) {
            $scope.entries = d["data"];
            $scope.totalPages = d["pageCount"];
            title.secondary = ($scope.opened ? "opened" : "closed") + ", page " + ($scope.page+1) + "/" + ($scope.totalPages+1);
        });
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $location.search("page", ($scope.page-1).toString());
        }
    };

    $scope.nextPage = function() {
        if (page < $scope.totalPages) {
            $location.search("page", ($scope.page+1).toString());
        }
    };

    $scope.hasNextPage = function() {
        return page < $scope.totalPages
    };

    $scope.setOpened = function(opened) {
        if ($scope.opened !== opened) {
            $location.search("opened", opened ? "true" : "false");
        }
    };

    $scope.close = function(ticketId, text) {
        var data = {
            "comment": text
        };
        $http({
            method: "POST",
            url: "/rest/tickets/ticket/"+ticketId+"/close",
            data: data
        }).success(function() {
            loadPage();
        });
    };

    {
        var locationSearch = $location.search();
        var page = parseInt(locationSearch["page"]);
        $scope.opened = !(locationSearch["opened"] === "false");
        if (isNaN(page) || page < 0) {
            $location.search("page", "0");
        } else {
            $scope.page = page;
            loadPage();
        }
    }
};

AdminApplication.controller('HistoryController', HistoryController);

/* @ngInject */
function HistoryController($scope, $http, $modal, title, options) {
    $scope.entries = [];
    $scope.page = 0;
    $scope.totalPages = 0;
    $scope.room = options.room;
    $scope.input = {};
    $scope.since = options.since;
    $scope.until = options.until;
    $scope.users = [];

    var loadPage = function() {
        var params = {
            "page": $scope.page
        };
        if ($scope.users) {
            params["user"] = $scope.users.map(mapToId);
        }
        if ($scope.since) {
            params["since"] = $scope.since;
        }
        if ($scope.until) {
            params["until"] = $scope.until;
        }
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/rooms/{number}/history/all", $scope.room.id),
            params: params
        }).success(function (d) {
            $scope.entries = d["data"];
            $scope.totalPages = d["pageCount"];
        });
    };

    $scope.removeUserFilter = function(user) {
        $scope.users.remove(user);
        $scope.page = 0;
        loadPage();
    };

    $scope.addUserFilter = function(user) {
        function userNotEq(u) {
            return u.id !== user.id;
        }

        if (user && $scope.users.every(userNotEq)) {
            $scope.input.user = null;
            $scope.users.push(user);
            $scope.page = 0;
            loadPage();
        }
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $scope.page = $scope.page - 1;
            loadPage();
        }
    };

    $scope.nextPage = function() {
        if (($scope.page+1) < $scope.totalPages) {
            $scope.page = $scope.page + 1;
            loadPage();
        }
    };

    $scope.hasNextPage = function() {
        return ($scope.page+1) < $scope.totalPages
    };

    $scope.pickRange = function() {
        var modalInstance = $modal.open({
            templateUrl: "range_pick.html",
            controller: "TimeRangePickerController",
            resolve: {
                date: function () {
                    return {
                        from: $scope.since ? new Date($scope.since) : new Date(),
                        to: $scope.since ? new Date($scope.until) : new Date()
                    };
                }
            }
        });
        modalInstance.result.then(function (data) {
            if (data) {
                $scope.since = data.from;
                $scope.until = data.to;
                $scope.page = 0;
                loadPage();
            }
        });
    };

    $scope.goToPage = function() {
        var newPage = parseInt(prompt("Page number", ($scope.page+1).toString()));
        if ((newPage > 0) && (newPage <= $scope.totalPages)) {
            $scope.page = newPage-1;
            loadPage();
        }
    };

    function mapToId(user) {
        return user.id;
    }

    loadPage();
}

/* @ngInject */
var PollsController = function($scope, $http, room) {
    $scope.polls = [];
    $scope.page = 0;

    var loadPage = function() {
        $scope.polls.length = 0;
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/rooms/{number}/polls/all", room.id),
            params: {
                page: $scope.page
            }
        }).success(function (d) {
            $scope.polls = d["data"];
            $scope.totalPages = d["pageCount"];
            angular.forEach($scope.polls, function(e) {
                e.maxPollVotes = Math.max.apply(null, e.votes);
            });
        });
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $scope.page = $scope.page - 1;
            loadPage();
        }
    };

    $scope.nextPage = function() {
        if (($scope.page+1) < $scope.totalPages) {
            $scope.page = $scope.page + 1;
            loadPage();
        }
    };

    $scope.hasNextPage = function() {
        return ($scope.page+1) < $scope.totalPages
    };

    loadPage();
};

/* @ngInject */
AdminApplication.controller("PollController", ["$scope", function($scope) {
    var series = [];
    {
        var max = $scope.poll.maxPollVotes;
        angular.forEach($scope.poll.poll.options, function(option) {
            series.push({
                "name": option.text,
                "y": $scope.poll.votes[option["optionId"]]/max
            });
        });
        console.log($scope.poll)
    }

    $scope.chartConfig = {
        options: {
            chart: {
                plotBackgroundColor: null,
                plotBorderWidth: null,
                plotShadow: false,
                type: 'pie'
            },
            tooltip: {
                pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    dataLabels: {
                        enabled: false
                    },
                    showInLegend: false
                }
            }
        },
        title: {
            text: '',
            style: {
                display: 'none'
            }
        },
        size: {
            height: 150
        },
        series: [{
            name: "Options",
            colorByPoint: true,
            data: series
        }]
    }
}]);

/* @ngInject */
var ComposePollController = function($scope, $modalInstance, $http, roomId) {
    $scope.input = {
        question: "",
        option: [{value: ""}, {value: ""}]
    };

    $scope.ok = function () {
        $modalInstance.close();
    };

    $scope.submit = function() {
        $scope.busy = true;
        var data = {
            "question": $scope.input.question,
            "options": $.map($scope.input.option, function(e) {return e.value})
        };
        $http({
            method: "POST",
            url: StringFormatter.format("/rest/rooms/{number}/polls/current", roomId),
            data: data
        }).success(function(data) {
            $modalInstance.close(data);
            $scope.busy = false;
        });
    };

    $scope.addOption = function() {
        $scope.input.option.push({value: ""});
    };

    $scope.removeOption = function(index) {
        if ($scope.input.option.length > 2) {
            $scope.input.option.splice(index, 1);
        }
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
};

/* @ngInject */
var ChattersController = function($scope, $location, $http, $modal, room, onlyBanned) {
    onlyBanned = onlyBanned ? true : false;
    $scope.users = [];
    $scope.search = null;
    $scope.room = room;
    $scope.totalPages = 0;
    $scope.page = 0;
    $scope.order = null;
    $scope.orderDesc = null;

    var loadPage = function() {
        $scope.users.length = 0;
        var params = {
            page: $scope.page,
            search: $scope.search,
            onlyBanned: onlyBanned
        };
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/rooms/{number}/chatters/all", room.id),
            params: params
        }).success(function (d) {
            $scope.users = d["data"];
            $scope.totalPages = d["pageCount"];
        });
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $scope.page = $scope.page - 1;
            loadPage();
        }
    };

    $scope.nextPage = function() {
        if (($scope.page+1) < $scope.totalPages) {
            $scope.page = $scope.page + 1;
            loadPage();
        }
    };

    $scope.hasNextPage = function() {
        return ($scope.page+1) < $scope.totalPages
    };

    $scope.doSearch = function() {
        if ($scope.searchInput) {
            $scope.page = 0;
            $scope.search = $scope.searchInput;
        } else {
            $scope.resetSearch();
        }
        loadPage();
    };

    $scope.resetSearch = function() {
        $scope.page = 0;
        $scope.search = null;
        $scope.searchInput = null;
        loadPage();
    };

    $scope.toggleBan = function(chatter) {
        $http({
            method: "PUT",
            url: StringFormatter.format("/rest/rooms/{number}/chatters/{string}", room.id, chatter.userName),
            data: {
                "banned": !chatter.banned
            }
        }).success(function () {
            chatter.banned = !chatter.banned;
        });
    };

    $scope.canBan = function(chatter) {
        return (ROLES[chatter.role] <= ROLES.MOD) && (ROLES[chatter.globalRole] < ROLES[document.SELF_ROLE]);
    };

    loadPage();
};

/* @ngInject */
var OnlineChattersController = function($scope, $location, $http, $modal, room) {
    $scope.users = [];
    $scope.search = null;
    $scope.room = room;
    $scope.filter = '';

    var loadPage = function() {
        $scope.users.length = 0;
        var params = {
            page: $scope.page,
            search: $scope.search
        };
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/rooms/{number}/chatters/online", room.id),
            params: params
        }).success(function (d) {
            $scope.users = d;
        });
    };

    $scope.toggleBan = function(chatter) {
        $http({
            method: "PUT",
            url: StringFormatter.format("/rest/rooms/{number}/chatters/{string}", room.id, chatter.userName),
            data: {
                "banned": !chatter.banned
            }
        }).success(function () {
            chatter.banned = !chatter.banned;
        });
    };

    $scope.canBan = function(chatter) {
        return (ROLES[chatter.role] <= ROLES.MOD) && (ROLES[chatter.globalRole] < ROLES[document.SELF_ROLE]);
    };

    loadPage();
};

/* @ngInject */
var TopChattersController = function($scope, $http, $modal, room) {
    $scope.room = room;
    $scope.entries = [];

    $http({method: "GET", url: StringFormatter.format("/rest/stats/room/{number}/topChatters", room.id)})
        .success(function(data) {
            $scope.entries = data;
        });

    $scope.showActivity = function(id, name) {
        $modal.open({
            templateUrl: "user_activity.html",
            controller: UserActivityController,
            resolve: {
                user: function () {
                    return {
                        id: id,
                        name: name
                    };
                }
            }
        });
    };
};

/* @ngInject */
var NewProxyController = function($scope, $http, $modalInstance, room) {
    $scope.error = null;
    $scope.providers = [];
    $scope.input = {
        outbound: false,
        authentication: false
    };

    $http({
        method: "GET",
        url: StringFormatter.format("/rest/rooms/{number}/proxies/providers", room.id)
    }).success(function (data) {
        $scope.providers = data;
    });

    $scope.reset = function() {
        $scope.input = {
            provider: $scope.input.provider,
            outbound: false,
            authentication: false
        }
    };

    $scope.submitForm = function() {
        if ($scope.input.provider) {
            $scope.error = null;
            var data = {
                providerName: $scope.input.provider.name,
                authId: $scope.input.auth ? $scope.input.auth.id : null,
                remoteRoom: $scope.input.room,
                enableOutbound: $scope.input.outbound
            };
            $http({
                method: "POST",
                data: data,
                url: StringFormatter.format("/rest/rooms/{number}/proxies", room.id)
            }).success(function(data) {
                $modalInstance.close(data);
            }).error(function (data) {
                $scope.error = data["message"];
            });
        }
    };

    $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
    };
};

/* @ngInject */
var TopicController = function($scope, $http) {
    $scope.inProgress = false;
    $scope.editing = false;
    $scope.input = {
        topic: ""
    };

    $scope.updateTopic = function() {
        $scope.inProgress = true;
        $http({
            method: "PUT",
            data: {
                topic: $scope.input.topic
            },
            url: "/rest/rooms/" + $scope.roomId
        }).success(function() {
            $scope.roomData.topic = $scope.input.topic;
            $scope.toggleEdit();
            $scope.inProgress = false;
        }).error(function(data) {
            alert(data);
            $scope.inProgress = false;
        });
    };

    $scope.toggleEdit = function() {
        if (!$scope.editing) {
            $scope.input.topic = $scope.roomData.topic;
        }
        $scope.editing = !$scope.editing;
    }
};

AdminApplication.controller("TopicController", ["$scope", "$http", TopicController]);

/* @ngInject */
var RoomController = function($scope, $location, $http, $sce, $modal, alert, title) {
    $scope.messages = [];
    $scope.journal = [];
    $scope.proxies = [];
    $scope.proxyProviders = [];
    $scope.poll = null;
    $scope.maxPollVotes = 0;
    $scope.chatterOffset = 0;

    var loadProxies = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/rooms/{number}/proxies/list", $scope.roomId)
        }).success(function (data) {
            $scope.proxies = data;
        });
    };

    var loadPage = function() {
        $scope.messages.length = 0;
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/history/peek", $scope.roomId)})
            .success(function (data) {
                $scope.messages = data;
            });
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/announcements/all", $scope.roomId)})
            .success(function (data) {
                $scope.announcements = data;
            });
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/info", $scope.roomId)})
            .success(function (data) {
                $scope.roomData = data;
                title.title = $scope.roomData.name;
            });
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/chatters/online", $scope.roomId)})
            .success(function (data) {
                $scope.chatterOffset = 0;
                $scope.chatters = data;
                title.secondary = $scope.chatters.length + " online"
            });
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/polls/current", $scope.roomId)})
            .success(function (data) {
                $scope.poll = data;
                $scope.maxPollVotes = Math.max.apply(null, $scope.poll.votes);
            });
        $http({method: "GET", url: "/rest/journal/room/" + $scope.roomId + "/peek"})
            .success(function (d) {
                $scope.journal = d.data;
            });
        $http({
            method: "GET",
            url: "/rest/stats/room/" + $scope.roomId + "/activity"
        }).success(function(data, status, headers, config) {
            var activity = {};
            angular.forEach(data, function(v, k) {
                activity[k/1000] = v;
            });
            var startDate = new Date();
            startDate.setDate(startDate.getDate() - 7);
            var cfg = {
                label: {
                    position: "left",
                    align: "left",
                    width: 50
                },itemSelector: "#roomActivity",
                domain: 'day',
                subDomain: 'hour',
                range: 8,
                cellSize: 20,
                domainGutter: 10,
                data: activity,
                start: startDate,
                colLimit: 24,
                verticalOrientation: true,
                itemName: ["message", "messages"],
                legend: [10, 25, 50, 100, 250, 500],
                legendColors: {
                    min: "#D7E3ED",
                    max: "#326A99",
                    empty: "white"
                }
            };
            var cal = new CalHeatMap();
            cal.init(cfg);
        });
        loadProxies();
    };

    $scope.showChatters = function() {
        $modal.open({
            templateUrl: '/templates/chatters.html',
            controller: ChattersController,
            resolve: {
                room: function () {
                    return $scope.roomData;
                },
                onlyBanned: function() {
                    return false;
                }
            }
        });
    };

    $scope.showBannedChatters = function() {
        $modal.open({
            templateUrl: '/templates/chatters.html',
            controller: ChattersController,
            resolve: {
                room: function() {
                    return $scope.roomData;
                },
                onlyBanned: function() {
                    return true;
                }
            }
        });
    };

    $scope.showOnlineChatters = function() {
        $modal.open({
            templateUrl: '/templates/online_chatters.html',
            controller: OnlineChattersController,
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.showTopChatters = function() {
        $modal.open({
            templateUrl: 'top_chatters.html',
            controller: TopChattersController,
            size: "sm",
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.showPolls = function() {
        $modal.open({
            templateUrl: 'polls.html',
            controller: PollsController,
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.showJournal = function() {
        $modal.open({
            template: '<journal-modal room="$ctrl.room"></journal-modal>',
            controller: Controller,
            controllerAs: '$ctrl',
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });

        /* ngInject */
        function Controller(room) {
            this.room = room;
        }
    };

    $scope.newProxy = function() {
        $modal.open({
            templateUrl: '/templates/new_proxy.html',
            controller: NewProxyController,
            size: "sm",
            resolve: {
                room: function() {
                    return $scope.roomData;
                }
            }
        }).result.then(function (data) {
            if (data) {
                $scope.proxies.push(data);
            }
        });
    };

    $scope.removeProxy = function(proxy) {
        $http({
            method: "DELETE",
            url: StringFormatter.format(
                "/rest/rooms/{number}/proxies/{string}/{string}",
                $scope.roomData.id, proxy.providerName, encodeURIComponent(proxy.remoteRoom)
            )
        }).success(function() {
            $scope.proxies.splice($scope.proxies.indexOf(proxy), 1);
        });
    };

    $scope.stopProxy = function(proxy) {
        $http({
            method: "POST",
            url: StringFormatter.format(
                "/rest/rooms/{number}/proxies/{string}/{string}/stop",
                $scope.roomData.id, proxy.providerName, encodeURIComponent(proxy.remoteRoom)
            )
        }).success(function() {
            loadProxies()
        });
    };

    $scope.startProxy = function(proxy) {
        $http({
            method: "POST",
            url: StringFormatter.format(
                "/rest/rooms/{number}/proxies/{string}/{string}/start",
                $scope.roomData.id, proxy.providerName, proxy.remoteRoom
            )
        }).success(function() {
            loadProxies()
        });
    };

    $scope.proxyStateClass = function(state) {
        switch (state) {
            case "NEW":
                return "label-primary";
            case "RUNNING":
                return "label-success";
            case "STOPPED":
                return "label-default";
            case "STOPPING":
            case "STARTING":
                return "label-warning";
            case "RECONNECTING":
                return "label-danger";
        }
    };

    $scope.closePoll = function() {
        if ($scope.poll.poll) {
            $http({
                method: "DELETE",
                url: StringFormatter.format("/rest/rooms/{number}/polls/current", $scope.roomData.id)
            }).success(function() {
                $scope.poll = null;
            });
        }
    };

    $scope.composePoll = function() {
        var modalInstance = $modal.open({
            templateUrl: 'compose_poll.html',
            controller: ComposePollController,
            size: "sm",
            resolve: {
                roomId: function () {
                    return $scope.roomData.id;
                }
            }
        });
        modalInstance.result.then(function (data) {
            if (data) {
                $scope.poll = data;
            }
        });
    };

    $scope.safe = function(text) {
        return $sce.trustAsHtml(text);
    };

    $scope.composeAnnouncement = function() {
        $modal.open({
            templateUrl: "compose_announcement.html",
            controller: ComposeAnnouncementController,
            resolve: {
                room: function() {
                    return $scope.roomData;
                }
            }
        }).result.then(function (data) {
            if (data) {
                $scope.announcements.push(data);
            }
        });
    };

    $scope.setAnnouncementInactive = function(id) {
        $http({
            method: "DELETE",
            url: StringFormatter.format("/rest/rooms/{number}/announcements/{number}", $scope.roomId, id)
        }).success(function() {
            $scope.announcements = $scope.announcements.filter(function(e) {
                return e.id !== id;
            });
        });
    };

    $scope.deleteRoom = function() {
        $http({
            method: "DELETE",
            url: "/rest/rooms/" + $scope.roomId
        }).success(function() {
            $location.path("/");
            $location.url($location.path());
        });
    };

    {
        var locationSearch = $location.search();
        $scope.roomId = locationSearch["id"];
        loadPage();
    }
};

/* @ngInject */
var ServicesController = function($scope, $location, $http) {
    $scope.data = null;

    var loadPage = function() {
        $http({
            method: "GET",
            url: "/rest/stats/global/runtime"
        }).success(function (d) {
            $scope.data = d;
        });
    };

    $scope.getMetricValueByName = function(name) {
        return $scope.data["metrics"][name]["value"];
    };

    $scope.getMetricByName = function(name) {
        return $scope.data["metrics"][name];
    };

    $scope.getUptime = function() {
        var uptime = $scope.getMetricValueByName("uptime");
        return new Date().getTime() - uptime;
    };

    $scope.isHealthy = function(service) {
        return $scope.data["healthChecks"][service]["healthy"];
    };

    $scope.getQueueSize = function(service) {
        return $scope.getMetricValueByName(service + ".queue.bufferSize");
    };

    $scope.getQueueLoad = function(service) {
        return $scope.getQueueSize(service) - $scope.getMetricValueByName(service + ".queue.remainingCapacity");
    };

    loadPage();
};

/* @ngInject */
var ComposeAnnouncementController = function($scope, $http, $modalInstance, room) {
    $scope.today = new Date();
    $scope.room = room;

    $scope.input = {
        text: "",
        onlyBroadcast: false
    };

    $scope.submitForm = function() {
        var onlyBroadcast = $scope.input.onlyBroadcast;
        var data = {
            "text": $scope.input.text,
            "onlyBroadcast": onlyBroadcast
        };
        $http({
            method: "POST",
            url: StringFormatter.format("/rest/rooms/{number}/announcements/new", room.id),
            data: data
        }).success(function(data) {
            if (onlyBroadcast) {
                $modalInstance.close();
            } else {
                $modalInstance.close(data);
            }
        }).error(function(error) {
            alert(error[0].message);
        });
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };
};

/* @ngInject */
var ProxyAuthController = function($scope, $http) {
    $scope.credentials = null;
    $scope.services = null;

    var loadPage = function() {
        $http({
            method: "GET",
            url: "/rest/proxy/auth/all"
        }).success(function (credentials) {
            $scope.credentials = credentials;
        });
        $http({
            method: "GET",
            url: "/rest/proxy/auth/services"
        }).success(function (services) {
            $scope.services = services;
        });
    };

    $scope.getIconClass = function(serviceName) {
        switch (serviceName) {
            case "twitch":
                return "fa-twitch";
            case "google":
                return "fa-google";
            case "twitter":
                return "fa-twitter";
            default:
                return "fa-key";
        }
    };

    $scope.deleteAuth = function(id) {
        $http({
            method: "DELETE",
            url: "/rest/proxy/auth/" + id
        }).success(loadPage);
    };

    loadPage();
};

/* @ngInject */
AdminApplication.config(["$routeProvider", "$locationProvider", function($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
    $routeProvider.when("/", {
        "title": "dashboard",
        "templateUrl": "/templates/dashboard.html",
        "controller": DashboardController,
        "menuId": "dashboard"
    });
    $routeProvider.when("/online", {
        "title": "online users",
        "templateUrl": "/templates/online_users.html",
        "controller": OnlineController,
        "menuId": "online"
    });
    $routeProvider.when("/journal", {
        "title": "journal",
        "templateUrl": "journal.html",
        "controller": JournalController,
        "menuId": "journal",
        reloadOnSearch: false
    });
    $routeProvider.when("/tickets", {
        "title": "tickets",
        "templateUrl": "tickets.html",
        "controller": TicketsController,
        "menuId": "tickets"
    });
    $routeProvider.when("/emoticons", {
        "title": "emoticons",
        "templateUrl": "/templates/emoticons.html",
        "controller": EmoticonsController,
        "menuId": "emoticons"
    });
    $routeProvider.when("/users", {
        "title": "users",
        "templateUrl": "/chat/admin/user/user_list.html",
        "controller": UsersController,
        "menuId": "users"
    });
    $routeProvider.when("/room", {
        "title": "room",
        "templateUrl": "/templates/room.html",
        "controller": RoomController,
        "menuId": "room"
    });
    $routeProvider.when("/services", {
        "title": "services",
        "templateUrl": "/templates/services.html",
        "controller": ServicesController,
        "menuId": "services"
    });
    $routeProvider.when("/proxyAuth", {
        "title": "Proxy credentials",
        "templateUrl": "/templates/proxy_auth.html",
        "controller": ProxyAuthController,
        "menuId": "proxyAuth"
    });
}]);

/* @ngInject */
AdminApplication.run(['$location', '$rootScope', "$route", "title", function($location, $rootScope, $route, title) {
    $rootScope.SELF_ROLE = document.SELF_ROLE;
    $rootScope.reloadCurrentRoute = function() {
        $route.reload();
    };
    $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
        $rootScope.menuId = current.$$route.menuId;
        title.title = current.$$route.title;
        title.secondary = null;
    });
}]);

Highcharts.setOptions({
    global: {
        useUTC: false
    }
});
