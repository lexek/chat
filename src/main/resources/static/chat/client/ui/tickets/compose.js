{
    var module = angular.module("chat.ui.tickets.compose", []);

    var ComposeTicketController = function($scope, $modalInstance, $http) {
        makeClosable($scope, $modalInstance);

        $scope.input = {
            "category": "BAN",
            "text": ""
        };
        $scope.submitting = false;

        $scope.submitTicket = function () {
            $scope.submitting = true;
            $http({
                method: "POST",
                url: "/api/tickets",
                data: $.param($scope.input),
                headers: {"Content-Type": "application/x-www-form-urlencoded"}
            }).success(function (data) {
                $scope.submitting = false;
                if (data === "ok") {
                    $modalInstance.close();
                    alert("Your ticket is successfully submitted.")
                } else {
                    $scope.response = {
                        "success": false,
                        "text": data
                    }
                }
            }).error(function (data) {
                $scope.submitting = false;
                $scope.response = {
                    "success": false,
                    "text": data
                }
            });
        };

        $scope.resetFormData = function () {
            $scope.input.text = "";
            $scope.response = null;
        };
    };

    module.controller("ComposeTicketController", ["$scope", "$modalInstance", "$http", ComposeTicketController])
}
