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

//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith#Polyfill
if (!String.prototype.startsWith) {
  String.prototype.startsWith = function(searchString, position) {
    position = position || 0;
    return this.indexOf(searchString, position) === position;
  };
}

//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/filter#Polyfill
if (!Array.prototype.filter) {
    Array.prototype.filter = function(fun/*, thisArg*/) {
        'use strict';

        if (this === void 0 || this === null) {
            throw new TypeError();
        }

        var t = Object(this);
        var len = t.length >>> 0;
        if (typeof fun !== 'function') {
            throw new TypeError();
        }

        var res = [];
        var thisArg = arguments.length >= 2 ? arguments[1] : void 0;
        for (var i = 0; i < len; i++) {
            if (i in t) {
                var val = t[i];

                // NOTE: Technically this should Object.defineProperty at
                //       the next index, as push can be affected by
                //       properties on Object.prototype and Array.prototype.
                //       But that method's new, and collisions should be
                //       rare, so use the more-compatible alternative.
                if (fun.call(thisArg, val, i, t)) {
                    res.push(val);
                }
            }
        }

        return res;
    };
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

angular.module('luegg.directives', []).directive('scrollGlue', [function() {
    return {
        priority: 1,
        require: ['?ngModel'],
        restrict: 'A',
        link: function(scope, $el, attrs, ctrls){
            var el = $el[0],
                ngModel = ctrls[0] || fakeNgModel(true);
            var lastHeight = 0;

            function scrollToBottom(){
                el.scrollTop = el.scrollHeight;
            }

            function shouldActivateAutoScroll() {
                var downsize = el.clientHeight < lastHeight;
                lastHeight = el.clientHeight;
                // + 1 catches off by one errors in chrome
                return downsize || (el.scrollTop + el.clientHeight + 1 >= el.scrollHeight);
            }

            scope.$watch(function(){
                if(ngModel.$viewValue){
                    scrollToBottom();
                }
            });

            $el.bind('scroll', function() {
                scope.$apply(ngModel.$setViewValue.bind(ngModel, shouldActivateAutoScroll()));
            });

            document.setGlueCallback(function() {
                scope.$apply(ngModel.$setViewValue.bind(ngModel, shouldActivateAutoScroll()));
            });
        }
    };
}]);
