var wide = false;

var setSizes = function() {
    'use strict';

    var windowWidth = $(window).width();
    var windowHeight = $(window).height();
    if (window.SINGLE_ROOM) {
        $('.chat').height(windowHeight - 38 + 'px');
        $('.online').height(windowHeight - 38 + 'px');
    } else {
        $('#roomSelector').width(windowWidth - 61 + 'px');
        $('.chat').height(windowHeight - 58 + 'px');
        $('.online').height(windowHeight - 58 + 'px');
    }
    $('.left-part').width(windowWidth - 80 + 'px');
};

document.IS_MOBILE = (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i).test(navigator.userAgent);
document.reflow = angular.noop;

document.chatApplication = angular.module('chatApplication', [
    'ngAnimate',
    'ngTouch',
    'chat.lang',
    'chat.services',
    'chat.controls',
    'chat.messages',
    'chat.users',
    'chat.twitter',
    'luegg.directives',
    'ui.utils',
    'pasvaz.bindonce',
    'ngTextcomplete',
    'relativeDate',
    'ngSanitize',
    'chat.client.utils',
    'chat.ui.auth',
    'chat.ui.profile',
    'chat.ui.tickets',
    'chat.ui.emoticons',
    'chat.sub',
    'chat.common.message',
    'templates'
]);

document.chatApplication.config(['$compileProvider', function($compileProvider) {
    'use strict';

    console.log('debug: ' + DEBUG);
    if (!DEBUG) {
        $compileProvider.debugInfoEnabled(false);
    }
}]);

document.chatApplication.run(['$rootScope', 'windowState', function($root, windowState) {
    wide = false;
    setSizes();
    $('.tse-scrollable').TrackpadScrollEmulator({wrapContent: false, autoHide: false});

    var update = function() {
        $root.$apply(function() {
            console.log("reflow");
            setSizes();
            $('.tse-scrollable').TrackpadScrollEmulator('recalculate');
        });
    };

    $(window).resize(update);
    windowState.focus(update);
    windowState.blur(update);

    window.addEventListener('message', function(event) {
        if (event.data === 'reflow') {
            update();
        }
    }, false);

}]);
