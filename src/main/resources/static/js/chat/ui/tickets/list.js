{
    var module = angular.module("chat.ui.tickets", ["chat.ui.tickets.compose"]);
    var TicketsController = function($scope, $http, $modalInstance, $modal) {
        makeClosable($scope, $modalInstance);

        $scope.entries = [];
        $scope.status = null;
        $scope.response = null;

        var loadData = function() {
            $scope.entries.length = 0;
            $http({method: "GET", url: "/api/tickets"})
                .success(function (d) {
                    $scope.entries = d;
                })
                .error(function (data) {
                    alert.alert("danger", data);
                });
        };

        $scope.compose = function() {
            $modal.open({
                templateUrl: 'chat/ui/tickets/compose.html',
                controller: "ComposeTicketController",
                size: "sm"
            }).result.then(function() {
                loadData();
            });
        };

        loadData();
    };

    module.controller("TicketsController", ["$scope", "$http", "$modalInstance", "$modal", TicketsController])
}
