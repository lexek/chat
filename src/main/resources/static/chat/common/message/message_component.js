(function() {
    'use strict';

    angular
        .module('chat.common.message')
        .component('message', {
            bindings: {
                'nodes': '<'
            },
            controller: MessageComponentController,
            controllerAs: 'vm',
            templateUrl: '/chat/common/message/message_component.html'
        });

    function MessageComponentController() {
        var vm = this;
        console.log(vm.nodes);
    }
})();
