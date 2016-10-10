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

(function() {
    'use strict';

    $(document).ready(function () {
        wide = false;
        setSizes();
        $('.tse-scrollable').TrackpadScrollEmulator({wrapContent: false, autoHide: false});
        $(window).resize(function() {
            setSizes();
            $('.tse-scrollable').TrackpadScrollEmulator('recalculate');
        });
    });
})();

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
    'chat.ui.profile',
    'chat.ui.tickets',
    'chat.common.message',
    'templates'
]);

document.chatApplication.config(['$compileProvider', function ($compileProvider) {
    'use strict';

    console.log('debug: ' + DEBUG);
    if (!DEBUG) {
        $compileProvider.debugInfoEnabled(false);
    }
}]);
