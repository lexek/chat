(function() {
    'use strict';

    angular
        .module('chat.admin.journal')
        .component('journalList', {
            bindings: {
                items: '<',
                room: '<'
            },
            controller: ListController,
            templateUrl: '/chat/admin/journal/journal_list.html'
        });

    function ListController() {
        var vm = this;

        vm.getClassForJournalAction = getClassForJournalAction;
        vm.translateAction = translateAction;

        var classMap = {
            'DELETED_EMOTICON': 'warning',
            'ROOM_BAN': 'warning',
            'DELETED_PROXY': 'warning',
            'ROOM_UNBAN': 'success',
            'ROOM_ROLE': 'success'
        };
        var actionMap = {
            'USER_UPDATE': 'User changed',
            'USER_CREATED': 'User created',
            'NAME_CHANGE': 'User name changed',
            'NEW_EMOTICON': 'New emoticon',
            'IMAGE_EMOTICON': 'Updated emoticon image',
            'DELETED_EMOTICON': 'Deleted emoticon',
            'NEW_ROOM': 'Created room',
            'DELETED_ROOM': 'Deleted room',
            'PASSWORD': 'Changed password',
            'NEW_PROXY': 'Proxy added',
            'DELETED_PROXY': 'Proxy removed',
            'NEW_POLL': 'Poll created',
            'CLOSE_POLL': 'Poll closed',
            'ROOM_BAN': 'User banned',
            'ROOM_UNBAN': 'User unbanned',
            'ROOM_ROLE': 'Role changed',
            'NEW_ANNOUNCEMENT': 'Announcement created',
            'INACTIVE_ANNOUNCEMENT': 'Announcement archived'
        };

        function getClassForJournalAction(action) {
            return 'list-group-item-' + classMap[action];
        }

        function translateAction(action) {
            return actionMap[action];
        }
    }
})();
