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
        $http({method: "GET", url: "/admin/api/ticket_count"})
            .success(function (d, status, headers, config) {
                self.count = d;
            })
            .error(function (data, status, headers, config) {
                //alert.alert("danger", data);
            });
    };

    TicketCountService.prototype.setCount = function(newCount) {
        this.count = newCount;
    };

    return new TicketCountService();
};

AdminServices.factory("alert", AlertServiceFactory);
AdminServices.factory("title", TitleServiceFactory);
AdminServices.factory("tickets", TicketCountServiceFactory);

var AdminApplication = angular.module("AdminApplication", ["ngRoute", "ngAnimate", "AdminServices", "relativeDate", "ui.inflector", "ui.bootstrap", "ui.bootstrap.datetimepicker"]);

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

var TitleController = function($scope, title) {
    $scope.title = function() {
        return title.title;
    };

    $scope.secondary = function() {
        return title.secondary;
    };
};

AdminApplication.controller("TitleController", TitleController);

var AnnouncementsWidgetController = function($scope, $http, $sce, alert) {
    $scope.announcements = [];
    $scope.show = false;

    var load = function() {
        $scope.announcements.length = 0;
        var url = "/admin/api/announcements";
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.announcements = d;
                angular.forEach($scope.announcements, function(e) {
                    e.text = $sce.trustAsHtml(e.text);
                });
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    load();
};

AdminApplication.controller("AnnouncementsWidgetController", AnnouncementsWidgetController);

var DashboardController = function($scope, $http, alert) {
    var loadMetrics = function () {
        $http({method: "GET", url: "/admin/api/metrics"})
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
                    values.push([metric.time, metric.value?metric.value:null]);
                });

                var bands = [];
                angular.forEach(streams, function(stream) {
                    bands.push({
                        "label": {
                            "text": stream.title,
                            rotation: -90,
                            textAlign: "right",
                            style: {
                                color: 'rgba(0,0,0,.5)',
                            }
                        },
                        "color": "rgba(68, 170, 213, 0.1)",
                        "from": new Date(stream.started),
                        "to": new Date(stream.ended),
                        "zIndex": 5
                    });
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
                        },
                        plotBands: bands
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

    $scope.entries = [];
    $scope.input = {};
    $scope.filter = "";

    var loadRooms = function() {
        var url = "/admin/api/rooms";
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.entries = d;
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
                $scope.entries.length = 0;
            });
    };

    $scope.add = function() {
        var data = $scope.input;
        data["action"] = "ADD";
        $scope.input = {};
        $http({method: "POST", data: $.param(data), url: "/admin/api/room"})
            .success(function (data, status, headers, config) {
                loadPage();
            })
            .error(function (data, status, headers, config) {
            });
    };

    $scope.remove = function(name) {
        var data = {
            "action": "DELETE",
            "NAME": name
        };
        $http({method: "POST", data: $.param(data), url: "/admin/api/rooms"})
            .success(function (data, status, headers, config) {
                loadPage();
            })
            .error(function (data, status, headers, config) {
            });
    };

    loadRooms();
    loadMetrics();
};

var EmoticonsController = function($scope, $http, alert) {
    $scope.emoticons = [];
    $scope.formData = {};
    $scope.order = "code";
    $scope.orderDesc = false;

    var loadEmoticons = function() {
        $scope.emoticons.length = 0;
        $http({method: "GET", url: "/admin/api/emoticons"})
            .success(function (d, status, headers, config) {
                var data = d["records"];
                angular.forEach(data, function (e) {
                    $scope.emoticons.push({
                        "id": e[0],
                        "code": e[1],
                        "fileName": e[2],
                        "height": e[3],
                        "width": e[4]
                    });
                });
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.requestDelete = function(id) {
        $http({method: "POST", url: "/admin/api/emoticons?delete="+id})
            .success(function(data, status, headers, config) {
                loadEmoticons();
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.submitForm = function() {
        $http({
            method  : "POST",
            url     : "/admin/api/emoticons",
            data    : $scope.formData,
            headers : { "Content-Type": "multipart/form-data" }
        })
            .success(function() {
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
        var order = $scope.order;
        var orderDesc = $scope.orderDesc;
        var url = "/admin/api/users?page=" + $scope.page;
        if (order && order !== "") {
            url += "&orderBy=" + order;
            if (orderDesc) {
                url += "&orderDesc=true";
            }
        }
        if ($scope.search) {
            url += "&search=" + encodeURI($scope.search);
        }
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
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
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
                $scope.users.length = 0;
                $scope.user = null;
            });
    };

    $scope.selectUser = function(u) {
        if ($scope.user && u && ($scope.user.id === u.id)) {
            $scope.user = null;
        } else {
            $scope.user = u;
        }
    }

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
        $http({method: "GET", url: "/admin/api/online"})
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
        $http({method: "GET", url: "/admin/api/blockedip"})
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
        $http({method: "POST", url: "/admin/api/blockedip?add="+ip})
            .success(function (data, status, headers, config) {
                $scope.blockedIps = data;
            })
            .error(function (data, status, headers, config) {
            });
    };

    $scope.unblockIp = function(ip) {
        $http({method: "POST", url: "/admin/api/blockedip?remove="+ip})
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


var UserController = function($scope, $route, $http, alert, id) {
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
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            rename: $scope.input.renameAvailable
        };
        $http({method: "POST", data: $.param(data),  url: "/admin/api/user"})
            .success(function(data, status, headers, config) {
                $scope.user.renameAvailable = $scope.input.renameAvailable;
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.saveBanned = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            banned: $scope.input.banned
        };
        $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
            .success(function(data, status, headers, config) {
                $scope.user.banned = $scope.input.banned;
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.saveRole = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            role: $scope.input.role
        };
        if ($scope.input.role === "USER" || $scope.input.role === "MOD" || $scope.input.role === "ADMIN") {
            $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
                .success(function (data, status, headers, config) {
                    $scope.user.role = $scope.input.role;
                    $scope.edit("");
                })
                .error(function (data, status, headers, config) {
                    $scope.edit("");
                    alert.alert("danger", data);
                });
        }
    };

    $scope.saveName = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            name: $scope.input.name
        };
        $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
            .success(function (data, status, headers, config) {
                $scope.user.name = $scope.input.name;
                $scope.edit("");
            })
            .error(function (data, status, headers, config) {
                $scope.edit("");
                alert.alert("danger", data);
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
    }

    $scope.requestDelete = function() {
        var data = {
            action: "DELETE",
            id: $scope.user.id
        };
        if (confirm("You sure that you want to delete user \"" + $scope.user.name + "\"?")) {
            $http({method: "POST", data: $.param(data), url: "/admin/api/users"})
                .success(function(data, status, headers, config) {
                    $route.reload();
                })
                .error(function(data, status, headers, config) {
                    alert.alert("danger", data);
                });
        }
    };

    $scope.hasAuth = function(auth) {
        return auth in $scope.auth;
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

AdminApplication.controller("UserController", ["$scope", "$route", "$http", "alert", UserController]);

var UserModalController = function($scope, $http, $modalInstance, id) {
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
        var url = "/admin/api/user?id=" + id;
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
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
            })
            .error(function (data, status, headers, config) {
            });
    };

    $scope.isUser = function() {
        return $scope.user.role === "USER";
    };

    $scope.saveRenameAvailable = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            rename: $scope.input.renameAvailable
        };
        $http({method: "POST", data: $.param(data),  url: "/admin/api/user"})
            .success(function(data, status, headers, config) {
                $scope.user.renameAvailable = $scope.input.renameAvailable;
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.saveBanned = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            banned: $scope.input.banned
        };
        $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
            .success(function(data, status, headers, config) {
                $scope.user.banned = $scope.input.banned;
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.saveRole = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            role: $scope.input.role
        };
        if ($scope.input.role === "USER" || $scope.input.role === "MOD" || $scope.input.role === "ADMIN") {
            $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
                .success(function (data, status, headers, config) {
                    $scope.user.role = $scope.input.role;
                    $scope.edit("");
                })
                .error(function (data, status, headers, config) {
                    $scope.edit("");
                    alert.alert("danger", data);
                });
        }
    };

    $scope.saveName = function() {
        var data = {
            action: "UPDATE",
            id: $scope.user.id,
            name: $scope.input.name
        };
        $http({method: "POST", data: $.param(data), url: "/admin/api/user"})
            .success(function (data, status, headers, config) {
                $scope.user.name = $scope.input.name;
                $scope.edit("");
            })
            .error(function (data, status, headers, config) {
                $scope.edit("");
                alert.alert("danger", data);
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
    }

    $scope.requestDelete = function() {
        var data = {
            action: "DELETE",
            id: $scope.user.id
        };
        if (confirm("You sure that you want to delete user \"" + $scope.user.name + "\"?")) {
            $http({method: "POST", data: $.param(data), url: "/admin/api/users"})
                .success(function(data, status, headers, config) {
                    $route.reload();
                })
                .error(function(data, status, headers, config) {
                    alert.alert("danger", data);
                });
        }
    };

    $scope.hasAuth = function(auth) {
        return auth in $scope.auth;
    };

    $scope.closeModal = function() {
        $modalInstance.dismiss('cancel');
    };

    loadPage();
};

var JournalController = function($scope, $location, $http, alert, title) {
    $scope.entries = [];
    $scope.totalPages = 0;
    $scope.secondaryTitle = $scope.page;

    var loadPage = function() {
        var url = "/admin/api/journal?page=" + $scope.page;
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.entries = d["data"];
                $scope.totalPages = d["totalPages"];
                title.secondary = "page " + ($scope.page+1) + "/" + ($scope.totalPages+1);
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
        if (page < $scope.totalPages) {
            $location.search("page", ($scope.page+1).toString());
        }
    };

    $scope.hasNextPage = function() {
        return page < $scope.totalPages
    };

    $scope.getTagLabelClass = function(tag) {
        if (tag === "error") {
            return "label-danger";
        } else if (tag === "mod") {
            return "label-primary";
        } else if (tag === "admin") {
            return "label-success";
        } else if (tag === "user") {
            return "label-info";
        }
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

var TicketsController = function($scope, $location, $http, $modal, alert, title) {
    $scope.entries = [];
    $scope.totalPages = 0;
    $scope.secondaryTitle = $scope.page;
    $scope.opened = true;

    var loadPage = function() {
        var url = "/admin/api/tickets?page=" + $scope.page;
        if (!$scope.opened) {
            url = url + "&open=false";
        }
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.entries = d["data"];
                $scope.totalPages = d["pageCount"];
                title.secondary = ($scope.opened ? "opened" : "closed") + ", page " + ($scope.page+1) + "/" + ($scope.totalPages+1);
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
                $scope.entries.length = 0;
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

    $scope.tryClose = function(id) {
        var comment = prompt("Type in closing comment.");
        if (comment != null) {
            var data = {
                "id": id,
                "comment": comment
            };
            $http({method: "POST", url: "/admin/api/tickets", data: $.param(data), headers: {"Content-Type": "application/x-www-form-urlencoded"}})
                .success(function(data, status, headers, config) {
                    loadPage();
                })
                .error(function(data, status, headers, config) {
                    alert.alert("danger", data);
                });
        }
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


var HistoryController = function($scope, $http, title, room) {
    $scope.entries = [];
    $scope.page = 0;
    $scope.totalPages = 0;
    $scope.room = room;
    $scope.input = {};
    $scope.users = [];

    var loadPage = function() {
        var url = "/admin/api/history?page=" + $scope.page;
        url += "&room=" + $scope.room.id;
        if ($scope.users) {
            angular.forEach($scope.users, function(e) {
                url += "&user=" + encodeURIComponent(e);
            });
        }
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.entries = d["data"];
                $scope.totalPages = d["pageCount"];
            })
            .error(function (data, status, headers, config) {
                $scope.entries.length = 0;
            });
    };

    $scope.removeUserFilter = function(user) {
        $scope.users.remove(user);
        $scope.page = 0;
        loadPage();
    };

    $scope.addUserFilter = function(user) {
        if ((user.length > 0) && ($scope.users.indexOf(user) === -1)) {
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

    loadPage();
};

var PollsController = function($scope, $http, room) {
    $scope.polls = [];

    var loadPage = function() {
        $scope.polls.length = 0;
        var url = "/admin/api/polls?room=" + room.id;
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.polls = d;
                angular.forEach($scope.polls, function(e) {
                    e.maxPollVotes = Math.max.apply(null, e.votes)
                });
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    loadPage();
};

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
            "action": "CREATE",
            "room": roomId,
            "question": $scope.input.question,
            "option": $.map($scope.input.option, function(e) {return e.value})
        };
        $http({method: "POST", url: "/admin/api/poll", data: $.param(data, true), headers: {"Content-Type": "application/x-www-form-urlencoded"}})
            .success(function(data, status, headers, config) {
                $modalInstance.close();
                $scope.busy = false;
            })
            .error(function(data, status, headers, config) {
                $scope.error = data;
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
        var order = $scope.order;
        var orderDesc = $scope.orderDesc;
        var url = "/admin/api/chatters?page=" + $scope.page + "&room=" + $scope.room.id;
        if ($scope.search) {
            url += "&search=" + encodeURI($scope.search);
        }
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.users = d["data"];
                $scope.totalPages = d["pageCount"];
            })
            .error(function (data, status, headers, config) {

            });
    };

    $scope.previousPage = function() {
        if ($scope.page !== 0) {
            $scope.page = $scope.page - 1;
            loadPage();
        }
    };

    $scope.nextPage = function() {
        if ($scope.page < $scope.totalPages) {
            $scope.page = $scope.page + 1;
            loadPage();
        }
    };

    $scope.hasNextPage = function() {
        return $scope.page < $scope.totalPages
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

    loadPage();
};

var RoomController = function($scope, $location, $http, $sce, $modal, alert, title) {
    $scope.messages = [];
    $scope.poll = null;
    $scope.maxPollVotes = 0;

    var loadPage = function() {
        $scope.messages.length = 0;
        $http({method: "GET", url: "/admin/api/room?id="+$scope.roomId})
            .success(function (d, status, headers, config) {
                $scope.messages = d["history"];
                $scope.chatters = d["chatters"];
                $scope.roomData = d["room"];
                $scope.announcements = d["announcements"];
                title.title = $scope.roomData.name;
                title.secondary = $scope.chatters.length + " online"
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
        $http({method: "GET", url: "/admin/api/poll?room="+$scope.roomId})
            .success(function (d, status, headers, config) {
                $scope.poll = d;
                $scope.maxPollVotes = Math.max.apply(null, $scope.poll.votes);
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.showHistory = function() {
        $modal.open({
            templateUrl: 'history.html',
            controller: HistoryController,
            size: "lg",
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.showChatters = function() {
        $modal.open({
            templateUrl: 'chatters.html',
            controller: ChattersController,
            size: "lg",
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
            size: "sm",
            resolve: {
                room: function () {
                    return $scope.roomData;
                }
            }
        });
    };

    $scope.closePoll = function() {
        if ($scope.poll.poll) {
            var data = {
                "action": "CLOSE",
                "room": $scope.roomData.id
            };
            $http({method: "POST", url: "/admin/api/poll", data: $.param(data), headers: {"Content-Type": "application/x-www-form-urlencoded"}})
                .success(function(data, status, headers, config) {
                    loadPage();
                })
                .error(function(data, status, headers, config) {
                    alert.alert("danger", data);
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
        modalInstance.result.then(function () {
            loadPage();
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
    }

    $scope.composeAnnouncement = function() {
        $modal.open({
            templateUrl: "compose_announcement.html",
            controller: ComposeAnnouncementController,
            resolve: {
                room: function() {
                    return $scope.roomData;
                }
            }
        }).result.then(function () {
                loadPage();
            });
    }

    $scope.setAnnouncementInactive = function(id) {
        var data = {
            "action": "DELETE",
            "id": id
        };

        $http({method: "POST", data: $.param(data), url: "/admin/api/announcement"})
            .success(function(data, status, headers, config) {
                loadPage();
            })
            .error(function(data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    {
        var locationSearch = $location.search();
        $scope.roomId = locationSearch["id"];
        loadPage();
    }
};


var ServicesController = function($scope, $location, $http, alert) {
    $scope.entries = [];
    $scope.input = {};

    var loadPage = function() {
        $scope.entries.length = 0;
        var url = "/admin/api/services";
        $http({method: "GET", url: url})
            .success(function (d, status, headers, config) {
                $scope.entries = d;
            })
            .error(function (data, status, headers, config) {
                alert.alert("danger", data);
            });
    };

    $scope.getLabelClass = function(state) {
        if (state === "RUNNING") {
            return "success";
        } else if (state === "NEW") {
            return "info"
        } else if (state === "STARTING") {
            return "primary"
        } else if (state === "STOPPED") {
            return "default";
        }
    };

    loadPage();
};

var ComposeAnnouncementController = function($scope, $http, $modalInstance, room) {
    $scope.today = new Date();
    $scope.room = room;

    $scope.input = {
        until: $scope.today,
        text: ""
    };

    $scope.submitForm = function() {
        var data = {
            "action": "ADD",
            "room": room.id,
            "text": $scope.input.text
        };
        $http({method: "POST", url: "/admin/api/announcement", data: $.param(data), headers: {"Content-Type": "application/x-www-form-urlencoded"}})
            .success(function(data, status, headers, config) {
                $modalInstance.close();
            })
            .error(function(data, status, headers, config) {
                $scope.error = data;
            });
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };
}

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
        "templateUrl": "room.html",
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