var makeClosable = function($scope, modal) {
    $scope.close = function() {
        modal.dismiss('cancel');
    };
};

