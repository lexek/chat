(function() {
    'use strict';

    angular
        .module('chat.ui.emoticons')
        .component('emoticons', {
            bindings: {
                close: '&'
            },
            controller: MessageComponentController,
            controllerAs: 'vm',
            templateUrl: '/chat/client-new/ui/emoticons/emoticons_component.html'
        });

    /* @ngInject */
    function MessageComponentController($http, chatService) {
        var vm = this;
        vm.ready = false;
        vm.error = false;
        vm.emoticons = [];

        vm.addToInput = addToInput;
        activate();

        function activate() {
            $http({
                'url': '/rest/emoticons/all',
                'method': 'GET'
            }).success(function (data) {
                vm.ready = true;
                vm.emoticons = data;
            }).error(function () {
                vm.ready = true;
                vm.error = true;
            });
        }

        function addToInput(code) {
            chatService.addToInputCallback(code);
            vm.close();
        }
    }
})();
