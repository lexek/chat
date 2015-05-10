jQuery.fn.slideRightHide = function(speed, callback) {
    this.animate({
        width: "hide",
        paddingLeft: "hide",
        paddingRight: "hide",
        marginLeft: "hide",
        marginRight: "hide"
    }, speed, callback);
};

jQuery.fn.slideRightShow = function(speed, callback) {
    this.animate({
        width: "show",
        paddingLeft: "show",
        paddingRight: "show",
        marginLeft: "show",
        marginRight: "show"
    }, speed, callback);
};

function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

//from google closure
Array.prototype.remove = function(obj) {
    var i = this.indexOf(obj);
    var rv;
    if ((rv = i >= 0)) {
        this.splice(i, 1);
    }
    return rv;
};

var amperRe_ = /&/g;
var ltRe_ = /</g;
var gtRe_ = />/g;
var quotRe_ = /\"/g;
var allRe_ = /[&<>\"]/;

var htmlEscape = function(str, opt_isLikelyToContainHtmlChars) {
    if (opt_isLikelyToContainHtmlChars) {
        return str.replace(amperRe_, '&amp;')
            .replace(ltRe_, '&lt;')
            .replace(gtRe_, '&gt;')
            .replace(quotRe_, '&quot;');

    } else {
        // quick test helps in the case when there are no chars to replace, in
        // worst case this makes barely a difference to the time taken
        if (!allRe_.test(str)) return str;

        // str.indexOf is faster than regex.test in this case
        if (str.indexOf('&') != -1) {
            str = str.replace(amperRe_, '&amp;');
        }
        if (str.indexOf('<') != -1) {
            str = str.replace(ltRe_, '&lt;');
        }
        if (str.indexOf('>') != -1) {
            str = str.replace(gtRe_, '&gt;');
        }
        if (str.indexOf('"') != -1) {
            str = str.replace(quotRe_, '&quot;');
        }
        return str;
    }
};

//*****************
function read_cookie(k,r){return(r=RegExp('(^|; )'+encodeURIComponent(k)+'=([^;]*)').exec(document.cookie))?r[2]:null;}

//*****************
function fakeNgModel(initValue){
    return {
        $setViewValue: function(value){
            this.$viewValue = value;
        },
        $viewValue: initValue
    };
}

angular.module('luegg.directives', []).directive('scrollGlue', function(){
    return {
        priority: 1,
        require: ['?ngModel'],
        restrict: 'A',
        link: function(scope, $el, attrs, ctrls){
            var el = $el[0],
                ngModel = ctrls[0] || fakeNgModel(true);

            function scrollToBottom(){
                el.scrollTop = el.scrollHeight;
            }

            function shouldActivateAutoScroll(){
                // + 1 catches off by one errors in chrome
                return el.scrollTop + el.clientHeight + 1 >= el.scrollHeight;
            }

            scope.$watch(function(){
                if(ngModel.$viewValue){
                    scrollToBottom();
                }
            });

            $el.bind('scroll', function(){
                scope.$apply(ngModel.$setViewValue.bind(ngModel, shouldActivateAutoScroll()));
            });
        }
    };
});

/*!
 * angular-translate - v2.4.1 - 2014-10-03
 * http://github.com/PascalPrecht/angular-translate
 * Copyright (c) 2014 ; Licensed MIT
 */
angular.module('pascalprecht.translate').factory('$translateCookieStorage', [
    '$cookieStore',
    function ($cookieStore) {
        var $translateCookieStorage = {
            get: function (name) {
                return $cookieStore.get(name);
            },
            set: function (name, value) {
                $cookieStore.put(name, value);
            }
        };
        return $translateCookieStorage;
    }
]);
