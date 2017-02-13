(function () {
    'use strict';

    angular
        .module('chat.sub')
        .component('subMessage', {
            bindings: {
                'message': '<'
            },
            controller: MessageComponentController,
            controllerAs: 'vm',
            templateUrl: '/chat/client-new/sub/sub_message_component.html'
        });

    function MessageComponentController() {
        var vm = this;

        console.log(vm.message);
    }
})();
