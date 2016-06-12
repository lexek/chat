(function() {
    'use strict';

    function Role(title, value) {
        this.title = title;
        this.value = value;
    }

    Role.prototype.valueOf = function() {
        return this.value;
    };

    var roles = {
        'UNAUTHENTICATED': new Role('unauthenticated', 0),
        'GUEST': new Role('guest', 1),
        'USER_UNCONFIRMED': new Role('unconfirmed user', 2),
        'USER': new Role('user', 2),
        'SUPPORTER': new Role('supporter', 3),
        'MOD': new Role('moderator', 4),
        'ADMIN': new Role('administrator', 5),
        'SUPERADMIN': new Role('administrator', 6)
    };

    angular.module('chat.admin.user').constant('roles', roles);
})();
