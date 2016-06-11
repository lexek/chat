var makeProgressable = function($scope) {
    $scope.info = null;
    $scope.error = null;
    $scope.inProgress = false;

    $scope.startProgress = function() {
        $scope.info = null;
        $scope.error = null;
        $scope.inProgress = true;
    };

    $scope.stopProgress = function(info) {
        $scope.info = info;
        $scope.inProgress = false;
    };

    $scope.stopProgressWithError = function(error) {
        $scope.inProgress = false;
        $scope.error = error;
    };
};
