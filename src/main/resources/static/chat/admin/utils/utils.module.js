(function() {
    'use strict';

    angular.module('chat.admin.utils', ['ui.bootstrap.modal'])
        .directive('userLink', function($modal) {
            return {
                restrict: 'A',
                scope: {
                    userLink: '='
                },
                link: function(scope, element) {
                    element.on('click', function() {
                        $modal.open({
                            templateUrl: '/templates/user.html',
                            //todo: define controller
                            controller: UserModalController,
                            size: 'sm',
                            resolve: {
                                id: function () {
                                    return scope.userLink;
                                }
                            }
                        });
                    });
                }
            };
        })
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
                            //todo: define controller
                            controller: HistoryController,
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