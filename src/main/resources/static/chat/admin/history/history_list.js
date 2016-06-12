(function() {
    'use strict';

    angular
        .module('chat.admin.history')
        .component('historyList', {
            bindings: {
                items: '<'
            },
            controller: HistoryList,
            templateUrl: '/chat/admin/history/history_list.html'
        });

    function HistoryList() {
    }
})();
