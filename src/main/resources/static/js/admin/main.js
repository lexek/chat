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

var TicketCountServiceFactory = function($http) {
    var TicketCountService = function() {
        this.count = "0";

        var self = this;
        $http({
            method: "GET",
            url: "/rest/tickets/open/count"
        }).success(function (d) {
            self.count = d["count"];
        });
    };

    TicketCountService.prototype.setCount = function(newCount) {
        this.count = newCount;
    };

    return new TicketCountService();
};

var MessageFilter = function($http, $sce) {
    var emoticons = {};
    var emoticonRegExp = null;
    $http({
        method: 'GET',
        url: '/rest/emoticons/all'
    }).success(function(data) {
        emoticons = {};
        var emoticonCodeList = [];
        angular.forEach(data, function (e) {
            emoticons[e.code] = e;
            emoticonCodeList.push(
                e.code
                    .replace("\\", "\\\\")
                    .replace(")", "\\)")
                    .replace("(", "\\(")
                    .replace(".", "\\.")
                    .replace("*", "\\*")
            );
        });
        emoticonRegExp = new RegExp(emoticonCodeList.join("|"), "g");
    });

    return function(input) {
        var text = input.replace(/</gi, '&lt;');
        text = twemoji.parse(text, {
            base: "/img/",
            folder: "twemoji",
            ext: ".png",
            callback: function(icon, options, variant) {
                switch ( icon ) {
                    case 'a9':      // � copyright
                    case 'ae':      // � registered trademark
                    case '2122':    // � trademark
                        return false;
                }
                return ''.concat(options.base, options.size, '/', icon, options.ext);
            }
        });
        if (emoticonRegExp) {
            text = text.replace(emoticonRegExp, function (match) {
                var emoticon = emoticons[match];
                if (emoticon) {
                    return "<img class='faceCode' src='/emoticons/" + emoticon.fileName + "' title='" + emoticon.code + "'></img>"
                } else {
                    return null;
                }
            });
        }
        return $sce.trustAsHtml(text);
    };
};

AdminServices.filter('slice', function() {
  return function(arr, start, end) {
    return (arr || []).slice(start, end);
  };
});

AdminServices.filter("message", ["$http", "$sce", MessageFilter]);
AdminServices.factory("alert", AlertServiceFactory);
AdminServices.factory("title", TitleServiceFactory);
AdminServices.factory("tickets", TicketCountServiceFactory);

var AdminApplication = angular.module("AdminApplication", ["ngRoute", "ngAnimate", "AdminServices", "relativeDate",
    "ui.inflector", "ui.bootstrap", "ui.bootstrap.datetimepicker", "highcharts-ng", "ngSanitize", "rgkevin.datetimeRangePicker"]);

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
    'USER': new Role("user", 2),
    "SUPPORTER": new Role("supporter", 3),
    'MOD': new Role("moderator", 4),
    'ADMIN': new Role("administrator", 5),
    'SUPERADMIN': new Role("administrator", 6)
};

var AlertController = function($scope, $timeout, alert) {
    alert.newAlertCallback = function(a) {
        $timeout(function(){alert.alerts.remove(a)}, 1000);
    };

    $scope.list = function() {
        return alert.alerts;
    }
};

AdminApplication.controller("AlertController", AlertController);

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

var TitleController = function($scope, title) {
    $scope.title = function() {
        return title.title;
    };

    $scope.secondary = function() {
        return title.secondary;
    };
};

AdminApplication.controller("TitleController", TitleController);

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

var EmoticonsController = function($scope, $http, alert) {
    $scope.emoticons = [];
    $scope.formData = {};
    $scope.order = "code";
    $scope.orderDesc = false;

    var loadEmoticons = function() {
        $scope.emoticons.length = 0;
        $http({
            method: "GET",
            url: "/rest/emoticons/all"
        }).success(function (d) {
            $scope.emoticons = d;
        }).error(function (data) {
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

    $scope.orderBy = function(orderVar) {
        if (orderVar === $scope.order) {
            $scope.orderDesc = !$scope.orderDesc;
        } else {
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

    loadEmoticons();
};

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
            $scope.users = [];
            angular.forEach(d["data"], function(e) {
                var u = e.user;
                u.authServices = e.authServices;
                u.authNames = e.authNames;
                $scope.users.push(u);
            });
            $scope.totalPages = d["pageCount"];
            title.secondary = "page " + ($scope.page+1) + "/" + ($scope.totalPages);
            $scope.user = null;
        });
    };

    $scope.selectUser = function(u) {
        if ($scope.user && u && ($scope.user.id === u.id)) {
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

    $scope.userModal = function(id, evt) {
        if (evt) {
            evt.preventDefault();
        }
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
        });
    };

    {
        loadOnline();
        loadBlockedIps();
    }

};


var UserActivityController = function($scope, $http, $modal, user) {
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
};

var UserController = function($scope, $route, $http, $modal, alert, id) {
    var editing = '';
    $scope.auth = {};
    $scope.input = {};

    $scope.availableRoles = [
        "USER",
        "MOD"
    ];

    if (document.SELF_ROLE == "SUPERADMIN") {
        $scope.availableRoles.push("ADMIN");
    }

    $scope.isUser = function() {
        return $scope.user.role === "USER";
    };

    $scope.saveRenameAvailable = function() {
        $http({
            method: "PUT",
            data: {
                rename: $scope.input.renameAvailable
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function() {
            $scope.user.renameAvailable = $scope.input.renameAvailable;
        });
    };

    $scope.saveBanned = function() {
        $http({
            method: "PUT",
            data: {
                banned: $scope.input.banned
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function() {
            $scope.user.banned = $scope.input.banned;
        });
    };

    $scope.saveRole = function() {
        if ($scope.input.role === "USER" || $scope.input.role === "MOD" || $scope.input.role === "ADMIN") {
            $http({
                method: "PUT",
                data: {
                    role: $scope.input.role
                },
                url: "/rest/users/" + $scope.user.id
            }).success(function () {
                $scope.user.role = $scope.input.role;
                $scope.edit("");
            });
        }
    };

    $scope.saveName = function() {
        $http({
            method: "PUT",
            data: {
                name: $scope.input.name
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function () {
            $scope.user.name = $scope.input.name;
            $scope.edit("");
        });
    };

    $scope.editing = function(variable) {
        return variable === editing;
    };

    $scope.edit = function(variable) {
        editing = variable;
        $scope.input[variable] = $scope.user[variable];
    };

    $scope.canEdit = function(variable) {
        if (variable === "name") {
            return ($scope.user.role === "USER") && (document.SELF_ROLE === "SUPERADMIN");
        }
        if (variable === "role") {
            return ROLES[$scope.user.role] < ROLES[document.SELF_ROLE];
        }
    };

    $scope.reset = function(variable) {
        $scope.edit("");
        $scope.input[variable] = $scope.user[variable];
    };

    $scope.requestDelete = function() {
        if (confirm("You sure that you want to delete user \"" + $scope.user.name + "\"?")) {
            $http({
                method: "POST",
                url: "/rest/users/" + $scope.user.id
            }).success(function() {
                $route.reload();
            });
        }
    };

    $scope.hasAuth = function(auth) {
        return auth in $scope.auth;
    };

    $scope.showActivity = function() {
        $modal.open({
            templateUrl: "user_activity.html",
            controller: UserActivityController,
            resolve: {
                user: function () {
                    return $scope.user;
                }
            }
        });
    };

    var init = function() {
        $scope.auth = {};
        if ($scope.user && $scope.user.authServices) {
            var namesArray = $scope.user.authNames.split(",");
            angular.forEach($scope.user.authServices.split(","), function(e, i) {
                $scope.auth[e] = namesArray[i];
            });
        }
        $scope.input.name = $scope.user.name;
        $scope.input.role = $scope.user.role;
        $scope.input.banned = $scope.user.banned;
        $scope.input.renameAvailable = $scope.user.renameAvailable;
    };

    init();

    $scope.$watch("user", function() {
        init();
    });
};

AdminApplication.controller("UserController", ["$scope", "$route", "$http", "$modal", "alert", UserController]);

var UserModalController = function($scope, $http, $modal, $modalInstance, id) {
    var editing = '';
    $scope.input = {};
    $scope.auth = [];

    $scope.availableRoles = [
        "USER",
        "MOD"
    ];

    if (document.SELF_ROLE == "SUPERADMIN") {
        $scope.availableRoles.push("ADMIN");
    }

    var loadPage = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/users/{number}", id)
        }).success(function (d) {
            $scope.user = d.user;
            $scope.input.name = $scope.user.name;
            $scope.input.role = $scope.user.role;
            $scope.input.banned = $scope.user.banned;
            $scope.input.renameAvailable = $scope.user.renameAvailable;
            if (d.authServices) {
                var namesArray = d.authNames.split(",");
                angular.forEach(d.authServices.split(","), function(e, i) {
                    $scope.auth[e] = namesArray[i];
                });
            }
        });
    };

    $scope.isUser = function() {
        return $scope.user.role === "USER";
    };

    $scope.saveRenameAvailable = function() {
        $http({
            method: "PUT",
            data: {
                rename: $scope.input.renameAvailable
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function() {
            $scope.user.renameAvailable = $scope.input.renameAvailable;
        });
    };

    $scope.saveBanned = function() {
        $http({
            method: "PUT",
            data: {
                banned: $scope.input.banned
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function() {
            $scope.user.banned = $scope.input.banned;
        });
    };

    $scope.saveRole = function() {
        if ($scope.input.role === "USER" || $scope.input.role === "MOD" || $scope.input.role === "ADMIN") {
            $http({
                method: "PUT",
                data: {
                    role: $scope.input.role
                },
                url: "/rest/users/" + $scope.user.id
            }).success(function () {
                $scope.user.role = $scope.input.role;
                $scope.edit("");
            });
        }
    };

    $scope.saveName = function() {
        $http({
            method: "PUT",
            data: {
                name: $scope.input.name
            },
            url: "/rest/users/" + $scope.user.id
        }).success(function () {
            $scope.user.name = $scope.input.name;
            $scope.edit("");
        });
    };

    $scope.editing = function(variable) {
        return variable === editing;
    };

    $scope.edit = function(variable) {
        editing = variable;
        $scope.input[variable] = $scope.user[variable];
    };

    $scope.canEdit = function(variable) {
        if (variable === "name") {
            return ($scope.user.role === "USER") && (document.SELF_ROLE === "SUPERADMIN");
        }
        if (variable === "role") {
            return ROLES[$scope.user.role] < ROLES[document.SELF_ROLE];
        }
    };

    $scope.reset = function(variable) {
        $scope.edit("");
        $scope.input[variable] = $scope.user[variable];
    };

    $scope.requestDelete = function() {
        if (confirm("You sure that you want to delete user \"" + $scope.user.name + "\"?")) {
            $http({
                method: "DELETE",
                url: "/rest/users/" + $scope.user.id
            }).success(function() {
                $route.reload();
            });
        }
    };

    $scope.hasAuth = function(auth) {
        return auth in $scope.auth;
    };

    $scope.showActivity = function() {
        $modal.open({
            templateUrl: "user_activity.html",
            controller: UserActivityController,
            resolve: {
                user: function () {
                    return $scope.user;
                }
            }
        });
    };

    $scope.closeModal = function() {
        $modalInstance.dismiss('cancel');
    };

    loadPage();
};

var JournalController = function($scope, $location, $http, $modal, alert, title) {
    $scope.entries = [];
    $scope.totalPages = 0;
    $scope.secondaryTitle = $scope.page;

    var loadPage = function() {
        $http({method: "GET", url: "/rest/journal/global", params: {page: $scope.page}})
            .success(function (d, status, headers, config) {
                $scope.entries = d["data"];
                $scope.totalPages = d["pageCount"];
                title.secondary = "page " + ($scope.page+1) + "/" + ($scope.totalPages);
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
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

    $scope.showUser = function(id) {
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
        });
    };

    var classMap = {
        "DELETED_EMOTICON": "warning"
    };

    var actionMap = {
        "USER_UPDATE": "User changed",
        "NAME_CHANGE": "User name changed",
        "NEW_EMOTICON": "New emoticon",
        "DELETED_EMOTICON": "Deleted emoticon",
        "NEW_ROOM": "Created room",
        "DELETED_ROOM": "Deleted room"
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
            $location.search("page", "0");
        } else {
            $scope.page = page;
            loadPage();
        }
    }
};

var RoomJournalModalController = function($scope, $http, $modal, room) {
    $scope.journal = [];
    $scope.totalPages = 0;
    $scope.page = 0;

    var loadPage = function() {
        $http({
            method: "GET",
            url: StringFormatter.format("/rest/journal/room/{number}", room.id),
            params: {page: $scope.page}
        }).success(function (d, status, headers, config) {
            $scope.journal = d["data"];
            $scope.totalPages = d["pageCount"];
        });
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $scope.page--;
            loadPage();
        }
    };

    $scope.nextPage = function() {
        if (($scope.page+1) < $scope.totalPages) {
            $scope.page++;
            loadPage();
        }
    };

    $scope.hasNextPage = function() {
        return ($scope.page+1) < $scope.totalPages
    };

    $scope.showUser = function(id) {
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
        });
    };

    var classMap = {
        "ROOM_BAN": "warning",
        "DELETED_PROXY": "warning",
        "ROOM_UNBAN": "success",
        "ROOM_ROLE": "success"
    };

    var actionMap = {
        "NEW_PROXY": "Proxy added",
        "DELETED_PROXY": "Proxy removed",
        "NEW_ROOM": "Room created",
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

    $scope.showBanContext = function(time) {
        $modal.open({
            templateUrl: 'history.html',
            controller: HistoryController,
            resolve: {
                options: function () {
                    return {
                        "room": room,
                        "since": time - 600000,
                        "until": time + 600000
                    }
                }
            }
        });
    };

    loadPage();
};

var TicketController = function($scope, $http) {
    $scope.showReply = false;
    $scope.text = "";

    $scope.toggleReply = function() {
        $scope.showReply = !$scope.showReply;
    };
};

AdminApplication.controller("TicketController", ["$scope", "$http", TicketController]);

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

    $scope.getLabelClass = function(cat) {
        if (cat === "BAN") {
            return "label-warning";
        } else if (cat === "BUG") {
            return "label-danger";
        } else if (cat === "RENAME") {
            return "label-primary";
        } else if (cat === "OTHER") {
            return "label-default";
        }
    };

    $scope.showUser = function(id, evt) {
        if (evt) {
            evt.preventDefault();
        }
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
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

var HistoryController = function($scope, $http, $modal, title, options) {
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
            params["user"] = $scope.users;
        }
        if ($scope.since) {
            params["since"] = $scope.since;
        }
        if ($scope.until) {
            params["until"] = $scope.until;
        }
        $http({method: "GET", url: StringFormatter.format("/rest/rooms/{number}/history/all", $scope.room.id), params: params})
            .success(function (d) {
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
        if (user && (user.length > 0) && ($scope.users.indexOf(user) === -1)) {
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

    loadPage();
};

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

var ChattersController = function($scope, $location, $http, $modal, room) {
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
            search: $scope.search
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

    $scope.showUser = function(id, evt) {
        if (evt) {
            evt.preventDefault();
        }
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
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

    $scope.showUser = function(id, evt) {
        if (evt) {
            evt.preventDefault();
        }
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
            }
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
                authName: $scope.input.authentication ? $scope.input.name : null,
                authKey: $scope.input.authentication ? $scope.input.key : null,
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
        loadProxies();
    };

    $scope.updateTopic = function() {
        var newTopic = prompt("New topic", $scope.roomData.topic);
        if (newTopic) {
            $http({
                method: "PUT",
                data: {
                    topic: newTopic
                },
                url: "/rest/rooms/" + $scope.roomId
            }).success(function() {
                $scope.roomData.topic = newTopic;
            });
        }
    };

    $scope.showHistory = function() {
        $modal.open({
            templateUrl: 'history.html',
            controller: HistoryController,
            resolve: {
                options: function () {
                    return {
                        "room": $scope.roomData
                    }
                }
            }
        });
    };

    $scope.showChatters = function() {
        $modal.open({
            templateUrl: '/templates/chatters.html',
            controller: ChattersController,
            resolve: {
                room: function () {
                    return $scope.roomData;
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
            templateUrl: 'journal_modal.html',
            controller: RoomJournalModalController,
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.newProxy = function() {
        $modal.open({
            templateUrl: 'new_proxy.html',
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
                $scope.roomData.id, proxy.providerName, proxy.remoteRoom
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
                $scope.roomData.id, proxy.providerName, proxy.remoteRoom
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
            case "FAILED":
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

    $scope.showUser = function(id, evt) {
        if (evt) {
            evt.preventDefault();
        }
        $modal.open({
            templateUrl: "user.html",
            controller: UserModalController,
            size: "sm",
            resolve: {
                id: function () {
                    return id;
                }
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
            var object = null;
            $scope.announcements = $scope.announcements.filter(function(e) {
                return e.id !== id;
            });
        });
    };

    var classMap = {
        "ROOM_BAN": "warning",
        "DELETED_PROXY": "warning",
        "ROOM_UNBAN": "success",
        "ROOM_ROLE": "success"
    };

    var actionMap = {
        "NEW_PROXY": "Proxy added",
        "DELETED_PROXY": "Proxy removed",
        "NEW_POLL": "Poll created",
        "CLOSE_POLL": "Poll closed",
        "ROOM_BAN": "User banned",
        "ROOM_UNBAN": "User unbanned",
        "ROOM_ROLE": "Role changed",
        "NEW_ANNOUNCEMENT": "Announcement created",
        "INACTIVE_ANNOUNCEMENT": "Announcement archived",
        "TOPIC_CHANGED": "Changed topic"
    };

    $scope.getClassForJournalAction = function(action) {
        return 'list-group-item-' + classMap[action];
    };

    $scope.translateAction = function(action) {
        return actionMap[action];
    };

    $scope.showBanContext = function(time) {
        $modal.open({
            templateUrl: 'history.html',
            controller: HistoryController,
            resolve: {
                options: function () {
                    return {
                        "room": $scope.roomData,
                        "since": time - 600000,
                        "until": time + 600000
                    }
                }
            }
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

var ComposeAnnouncementController = function($scope, $http, $modalInstance, room) {
    $scope.today = new Date();
    $scope.room = room;

    $scope.input = {
        text: "",
        onlyBroadcast: false
    };

    $scope.submitForm = function() {
        var data = {
            "text": $scope.input.text,
            "onlyBroadcast": $scope.input.onlyBroadcast
        };
        $http({method: "POST", url: StringFormatter.format("/rest/rooms/{number}/announcements/new", room.id), data: data})
            .success(function(data) {
                $modalInstance.close(data);
            });
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };
};

AdminApplication.config(["$routeProvider", "$locationProvider", function($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
    $routeProvider.when("/", {
        "title": "dashboard",
        "templateUrl": "dashboard.html",
        "controller": DashboardController,
        "menuId": "dashboard"
    });
    $routeProvider.when("/online", {
        "title": "online users",
        "templateUrl": "online.html",
        "controller": OnlineController,
        "menuId": "online"
    });
    $routeProvider.when("/journal", {
        "title": "journal",
        "templateUrl": "journal.html",
        "controller": JournalController,
        "menuId": "journal"
    });
    $routeProvider.when("/tickets", {
        "title": "tickets",
        "templateUrl": "tickets.html",
        "controller": TicketsController,
        "menuId": "tickets"
    });
    $routeProvider.when("/emoticons", {
        "title": "emoticons",
        "templateUrl": "emoticons.html",
        "controller": EmoticonsController,
        "menuId": "emoticons"
    });
    $routeProvider.when("/users", {
        "title": "users",
        "templateUrl": "users.html",
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
        "templateUrl": "services.html",
        "controller": ServicesController,
        "menuId": "services"
    });
}]);

AdminApplication.run(['$location', '$rootScope', "$route", "title", "tickets", function($location, $rootScope, $route, title, tickets) {
    $rootScope.SELF_ROLE = document.SELF_ROLE;
    $rootScope.reloadCurrentRoute = function() {
        $route.reload();
    };
    $rootScope.getOpenTicketCount = function() {
        return tickets.count;
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