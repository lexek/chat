angular.module("chat.twitter", [])
    .directive("twitterHandle", function() {
        return {
            restrict: "E",
            scope: {
                "name": "=",
                "muted": "="
            },
            template:
                "<a ng-class='::{\"role\": muted}' target='_blank' ng-href='https://twitter.com/{{::name}}'>@{{::name}}</a>"
        }
    })
    .directive("fullTwitterHandle", function() {
        return {
            restrict: "E",
            scope: {
                "name": "=",
                "fullName": "="
            },
            template:
                "<span>" +
                    "<a ng-href='https://twitter.com/{{::name}}' target='_blank'>{{::fullName}}</a> " +
                    "<small><twitter-handle name='::name' muted='true'></small>" +
                "</span>"
        }
    })
    .directive("twitterAvatar", function() {
        return {
            restrict: "E",
            scope: {
                "url": "=",
                "name": "="
            },
            link: function(scope) {
                if (scope.url.indexOf("https://") == -1) {
                    scope.url = scope.url.replace("http://", "https://")
                }
            },
            template:
                "<a ng-href='https://twitter.com/{{::name}}' target='_blank'>" +
                    "<img ng-src='{{::url}}' style='height:16px; width:16px; float: left;'></img>" +
                "</a>"
        }
    })
    .directive("twitterTimeLink", function() {
        return {
            restrict: "E",
            scope: {
                "time": "=",
                "name": "=",
                "id": "=",
            },
            template:
                "<a ng-href='https://twitter.com/{{::name}}/status/{{::id}}' target='_blank' class='role'>{{::time | date:\"HH:mm\"}}</a>"
        }
    })
    .directive("twitterButtons", function() {
        return {
            restrict: "E",
            scope: {
                "id": "=",
            },
            template:
                '<div class="btn-group modButtons pull-right">'+
                    '<a class="btn btn-link btn-x" ng-href="https://twitter.com/intent/tweet?in_reply_to={{::id}}"><i class="fa fa-fw fa-reply" title="reply"></i></a>' +
                    '<a class="btn btn-link btn-x" ng-href="https://twitter.com/intent/retweet?tweet_id={{::id}}"><i class="fa fa-fw fa-retweet" title="retweet"></i></a>' +
                    '<a class="btn btn-link btn-x" ng-href="https://twitter.com/intent/like?tweet_id={{::id}}"><i class="fa fa-fw fa-heart" title="like"></i></a>' +
                '</div>'
        }
    })
    .directive("twitterHeading", function() {
        return {
            restrict: "E",
            scope: false,
            template:
                '<div class="messageHeading">' +
                    '<div class="time">' +
                        '<twitter-time-link ' +
                            'time="::displayTweet.when" ' +
                            'name="::displayTweet.from" ' +
                            'id="::displayTweet.id"/>'+
                    '</div>' +
                    '<twitter-avatar ' +
                        'class="pull-left" style="margin-right: 2px;" ' +
                        'url="::displayTweet.fromAvatar" ' +
                        'name="::displayTweet.from"' +
                        '></twitter-avatar>' +
                    '<i class="fa fa-fw fa-twitter" style="color: #55acee;"></i>' +
                    '<full-twitter-handle ' +
                        'full-name="::displayTweet.fromFullName" ' +
                        'name="::displayTweet.from"' +
                        '></full-twitter-handle> ' +
                    '<small ng-if="::tweet.retweetedStatus" >' +
                        '<span style="color:#19cf86;">{{"TWITTER_RETWEETED" | translate}}</span> ' +
                        '<twitter-handle name="::tweet.from" muted="true"/>'+
                    '</small>' +
                '</div>'
        }
    })
    .directive("twitterTweet", function() {
        return {
            restrict: "E",
            scope: {
                "tweet": "="
            },
            controller: function($scope) {
                var tweet = $scope.tweet;
                $scope.displayTweet = tweet;
                if (tweet.retweetedStatus) {
                    $scope.displayTweet = tweet.retweetedStatus
                }
            },
            template:
                '<twitter-heading></twitter-heading>' +
                '<twitter-buttons id="::tweet.id" class="pull-right"></twitter-buttons>' +
                '<div class="userMessageBody">' +
                    '<span ng-bind-html="::displayTweet.text"></span>' +
                '</div>' +
                '<div class="userMessageBody" ng-if="::tweet.quotedStatus" class="alert-msg-twitter-quoted">' +
                    '<full-twitter-handle ' +
                        'full-name="::tweet.quotedStatus.fromFullName" ' +
                        'name="::tweet.quotedStatus.from"' +
                        '></full-twitter-handle> ' +
                    '<i class="fa fa-fw fa-quote-left role"></i>' +
                    '<span ng-bind-html="::tweet.quotedStatus.text"></span>' +
                    '<i class="fa fa-fw fa-quote-right role"></i>' +
                '</div>'
        }
    })
