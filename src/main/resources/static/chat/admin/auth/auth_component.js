(function() {
    'use strict';

    angular
        .module('chat.admin.auth')
        .component('auth', {
            bindings: {
                'auth': '<'
            },
            controller: AuthComponentController,
            templateUrl: '/chat/admin/auth/auth_component.html'
        });

    function AuthComponentController() {
        var vm = this;

        vm.getIcon = getIcon;

        function getIcon(service) {
            switch (service) {
                case 'twitch':
                    return 'fa fa-fw fa-twitch';
                case 'google':
                    return 'fa fa-fw fa-google';
                case 'vk':
                    return 'fa fa-fw fa-vk';
                case 'twitter':
                    return 'fa fa-fw fa-twitter';
                case 'token':
                    return 'fa fa-fw fa-globe';
                case 'password':
                    return 'fa fa-fw fa-key';
                case 'goodgame':
                    return 'gg-icon';
                default:
                    return 'fa fa-fw fa-question';
            }
        }
    }
})();
