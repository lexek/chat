angular.module("chat.admin.auth", [])
    .directive("auth", function() {
        return {
            restrict: "E",
            scope: {
                "auth": "="
            },
            controller: function($scope) {
                $scope.getIcon = function(service) {
                    switch (service) {
                        case "twitch":
                            return "fa fa-fw fa-twitch";
                        case "google":
                            return "fa fa-fw fa-google";
                        case "twitter":
                            return "fa fa-fw fa-twitter";
                        case "token":
                            return "fa fa-fw fa-globe";
                        case "password":
                            return "fa fa-fw fa-key";
                        case "goodgame":
                            return "gg-icon";
                        default:
                            return "fa fa-fw fa-question";
                    }
                };
            },
            templateUrl: "/templates/auth.html"
        }
    })
