angular.module("chat.admin.utils", ["ui.bootstrap.modal"])
    .directive("userLink", function($modal) {
        return {
            restrict: "A",
            scope: {
                userLink: "="
            },
            link: function(scope, element) {
                element.on("click", function() {
                    $modal.open({
                        templateUrl: "/templates/user.html",
                        controller: UserModalController,
                        size: "sm",
                        resolve: {
                            id: function () {
                                return scope.userLink;
                            }
                        }
                    });
                });
            }
        }
    })
    .directive("historyPopup", function($modal) {
        return {
            restrict: "A",
            scope: {
                historyPopup: "=",
                historyPopupAround: "=?"
            },
            link: function(scope, element) {
                element.on("click", function() {
                    $modal.open({
                        templateUrl: '/templates/history.html',
                        controller: HistoryController,
                        resolve: {
                            options: function () {
                                return {
                                    "room": scope.historyPopup,
                                    "since": scope.historyPopupAround ? scope.historyPopupAround - 600000 : null,
                                    "until": scope.historyPopupAround ? scope.historyPopupAround + 600000 : null
                                }
                            }
                        }
                    });
                });
            }
        }
    })
    .directive("userInput", function($http) {
        return {
            restrict: "E",
            scope: {
                ngModel: "=",
                ngChange: "=",
                class: "@?"
            },
            controller: function($scope) {
                $scope.findUsers = function(partialName) {
                    return $http.get("/rest/users/search", {
                        params: {
                            search: partialName
                        }
                    }).then(function(response) {
                        return response.data;
                    });
                };
            },
            template: '<input ' +
                'title="{{ngModel}}" ' +
                'type="text" ' +
                'ng-model="ngModel" ' +
                'ng-change="ngChange" ' +
                'placeholder="User name" ' +
                'typeahead="user as user.name for user in findUsers($viewValue)" ' +
                'typeahead-editable="false" ' +
                'class="form-control {class}">'
        }
    });
