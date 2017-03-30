(function() {
    'use strict';

    angular
        .module('chat.common.message')
        .component('messageLink', {
            bindings: {
                'text': '<'
            },
            controller: MessageLinkComponentController,
            controllerAs: 'vm',
            templateUrl: '/chat/common/message/message_link.html'
        });

    /* @ngInject */
    function MessageLinkComponentController(messageLinkService) {
        var vm = this;

        vm.model = {
            'type': 'pending',
            'link': vm.text,
            'text': vm.text
        };

        activate();

        function activate() {
            var match = vm.text.match(/(https?:\/\/)([^\s]*)/);
            messageLinkService
                .resolve(match[0], match[1], match[2])
                .then(function(newModel) {
                    vm.model = newModel;
                });
        }
    }
})();
