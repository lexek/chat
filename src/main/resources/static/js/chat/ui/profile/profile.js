{
    var module = angular.module("chat.ui.profile", ["chat.ui.profile.email", "chat.ui.profile.password", "chat.services.chat"]);
    var ProfileController = function ($scope, $modalInstance, $modal, $http, chat) {
        makeClosable($scope, $modalInstance);
        makeProgressable($scope);

        $scope.self = chat.self;
        $scope.profile = null;

        $scope.showEmailSettings = function() {
            $modal.open({
                templateUrl: 'chat/ui/profile/email.html',
                controller: "EmailSettingsController"
            }).result.then(function() {
                loadProfile();
            });
        };

        $scope.showPasswordSettings = function() {
            $modal.open({
                templateUrl: 'chat/ui/profile/password.html',
                controller: "PasswordSettingsController"
            }).result.then(function() {
                loadProfile();
            });
        };

        var loadProfile = function() {
            if ($scope.self.role > globalLevels.USER_UNCONFIRMED) {
                $http({
                    method: 'get',
                    url: '/rest/profile'
                }).success(function(data) {
                    $scope.profile = data;
                });
            }
        };

        $scope.newToken = function () {
            $scope.startProgress();
            $http.post("/token").success(function(data) {
                var token = data["token"];
                if (token) {
                    $scope.apiToken = token;
                }
                $scope.stopProgress();
            }).error(function(data) {
                $scope.stopProgressWithError(data);
            });
        };

        loadProfile();
    };

    module.run(function($templateCache) {
        $templateCache.put("chat/ui/profile/profile.html",
            "<div class='modal-header'>" +
                "<h3>" +
                    "<i class='fa fa-user'></i>" +
                    "{{self.name}}" +
                    "<span class='btn btn-link btn-xs pull-right' ng-click='showPasswordSettings()'><i class='fa fa-key'></i></span>" +
                "</h3>" +
            "</div>" +
            "<div class='modal-body'>" +
                "<form class='form-horizontal' ng-if='profile'>" +
                    "<div class='form-group' ng-if='profile.user.email'>" +
                        "<label class='col-sm-2 control-label'>Email</label>" +
                        "<div class='col-sm-10'>" +
                            "<div class='form-control-static'>" +
                                "{{profile.user.email}} " +
                                "<i class='fa fa-fw fa-check text-success' ng-if='profile.user.emailVerified'></i>" +
                                "<i class='fa fa-fw fa-times text-danger' ng-if='!profile.user.emailVerified'></i>" +
                                "<span class='btn btn-link btn-xs pull-right' ng-click='showEmailSettings()'><i class='fa fa-pencil'></i></span>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "<div class='form-group'>" +
                        "<label for='inputName' class='col-sm-2 control-label'>" +
                            "Name" +
                        "</label>" +
                        "<div class='col-sm-10'>" +
                            "<div class='form-control-static'>" +
                                "{{profile.user.name}}" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "<div class='form-group'>" +
                        "<label for='inputRole' class='col-sm-2 control-label'>" +
                            "Role" +
                        "</label>" +
                        "<div class='col-sm-10'>" +
                            "<div class='form-control-static'>" +
                                "{{profile.user.role}}" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "<div class='form-group'>" +
                        "<label for='inputRole' class='col-sm-2 control-label'>" +
                            "Color" +
                        "</label>" +
                        "<div class='col-sm-10'>" +
                            "<div class='form-control-static'>" +
                                "{{profile.user.color}} <span class='fa fa-circle fa-fw' ng-style='{\"color\": profile.user.color}'></span>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                "</form>" +
                "<div class='panel panel-default'>" +
                    "<div class='panel-heading'>" +
                        "<div class='panel-title'>" +
                            "API token" +
                        "</div>" +
                    "</div>" +
                    "<div class='panel-body'>" +
                        "<div class='input-group'>" +
                            "<input type='text' class='form-control' ng-model='apiToken' readonly "+
                                   "placeholder='click button to get new api token'>" +
                            "<span class='input-group-btn'>" +
                                "<button class='btn btn-default' type='button' ng-click='newToken()'>" +
                                    "<i class='fa fa-fw fa-refresh'></i>" +
                                "</button>" +
                            "</span>" +
                        "</div>" +
                    "</div>" +
                "</div>" +
            "</div>" +
            "<div class='modal-footer'>" +
                "<div class='btn btn-warning' ng-click='close()' translate='CONTROLS_CLOSE'></div>" +
            "</div>"
        );
    });
    module.controller("ProfileController", ["$scope", "$modalInstance", "$modal", "$http", "chatService", ProfileController])
}