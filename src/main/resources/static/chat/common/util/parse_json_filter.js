(function () {
    'use strict';

    angular.module('chat.common.util', [])
        .filter('parseJson', ParseJsonFilter);

    function ParseJsonFilter() {
        return function (input) {
            return JSON.parse(input);
        };
    }
})();
