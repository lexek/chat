(function() {
    'use strict';

    angular.module('chat.admin.utils', ['ui.bootstrap.modal'])
        .directive('historyPopup', function($modal) {
            return {
                restrict: 'A',
                scope: {
                    historyPopup: '=',
                    historyPopupAround: '=?'
                },
                link: function(scope, element) {
                    element.on('click', function() {
                        $modal.open({
                            templateUrl: '/templates/history.html',
                            controller: 'HistoryController',
                            resolve: {
                                options: function () {
                                    return {
                                        'room': scope.historyPopup,
                                        'since': scope.historyPopupAround ? scope.historyPopupAround - 600000 : null,
                                        'until': scope.historyPopupAround ? scope.historyPopupAround + 600000 : null
                                    };
                                }
                            }
                        });
                    });
                }
            };
        });
})();
