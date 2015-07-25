var wide = false;

var setSizes = function() {
    var windowWidth = $(window).width();
    var windowHeight = $(window).height();
    if (window.SINGLE_ROOM) {
        $(".chat").height(windowHeight - 35 + "px");
        $(".online").height(windowHeight - 35 + "px");
    } else {
        $("#roomSelector").width(windowWidth - 61 + "px");
        $(".chat").height(windowHeight - 55 + "px");
        $(".online").height(windowHeight - 55 + "px");
    }
    $(".left-part").width(windowWidth - 62 + "px");
};

$(document).ready(function () {
    wide = false;
    setSizes();
    $('.tse-scrollable').TrackpadScrollEmulator({wrapContent: false, autoHide: false});
    $( window ).resize(function() {
        setSizes();
        $('.tse-scrollable').TrackpadScrollEmulator('recalculate');
    });
});

document.chatApplication = angular.module("chatApplication", [
    "ngAnimate",
    "chat.lang",
    "chat.services",
    "chat.controls",
    "chat.messages",
    "chat.users",
    "luegg.directives",
    "ui.utils",
    "pasvaz.bindonce",
    "ngTextcomplete",
    "relativeDate",
    "ngSanitize",
    "chat.ui.profile",
    "chat.ui.tickets"
]);
