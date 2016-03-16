var controlsModule = angular.module("chat.controls", ["chat.services.chat", "ui.bootstrap", "colorpicker.module", "ngCookies", "vcRecaptcha", "ui.bootstrap.popover"]);

controlsModule.controller("RoomWidgetController", ["$scope", "chatService", function($scope, chatService) {
    $scope.open = false;

    $scope.getActiveRoom = function() {
        return chatService.activeRoom;
    };

    $scope.setActiveRoom = function(newActiveRoom) {
        if (chatService.activeRoom !== newActiveRoom) {
            chatService.setActiveRoom(newActiveRoom);
            $scope.open = false;
        }
    };

    $scope.joinRoom = function() {
        var roomName = prompt("Enter name of the room to join.");
        if (roomName) {
            chatService.setActiveRoom(roomName);
            chatService.sendMessage({"type": "JOIN", "room": roomName});
        }
    };

    $scope.partRoom = function() {
        if (chatService.activeRoom !== DEFAULT_ROOM) {
            var room = chatService.activeRoom;
            chatService.sendMessage({"type": "PART", "room": chatService.activeRoom});
            chatService.setActiveRoom(DEFAULT_ROOM);
            chatService.deleteRoom(room);
        }
    };

    $scope.getRooms = function() {
        return chatService.rooms;
    };

    $scope.unreadCount = function(room) {
        return chatService.unreadCount[room];
    };

    $scope.unreadMentions = function(room) {
        return chatService.unreadMentions[room];
    };

    $scope.anyUnread = function() {
        var result = false;
        angular.forEach(chatService.rooms, function(room) {
            if (chatService.unreadCount[room] > 0) {
                result = true;
            }
        });
        return result;
    };

    $scope.anyMentions = function() {
        var result = false;
        angular.forEach(chatService.rooms, function(room) {
            if (chatService.unreadMentions[room] > 0) {
                result = true;
            }
        });
        return result;
    }
}]);

controlsModule.controller("PollWidgetController", ["$scope", "chatService", function($scope, chatService) {
    chatService.pollsUpdatedCallback = function() {$scope.$digest()};

    $scope.open = false;
    $scope.input = {};

    $scope.getPollForActiveRoom = function() {
        return chatService.polls[chatService.activeRoom];
    };

    $scope.hasActivePoll = function() {
        return chatService.polls[chatService.activeRoom] ? true : false;
    };

    $scope.getActivePollQuestion = function() {
        return chatService.polls[chatService.activeRoom].poll.question;
    };

    $scope.getCurrentPollOptions = function() {
        return chatService.polls[chatService.activeRoom].poll.options;
    };

    $scope.getCurrentPollVotes = function() {
        return chatService.polls[chatService.activeRoom].votes;
    };

    $scope.getCurrentPollMaxVotes = function() {
        return chatService.polls[chatService.activeRoom].maxPollVotes;
    };

    $scope.hasVoted = function() {
        return chatService.polls[chatService.activeRoom].voted;
    };

    $scope.isOpen = function() {
        return chatService.polls[chatService.activeRoom].open;
    };

    $scope.canVote = function() {
        return chatService.self.role >= globalLevels.USER;
    };

    $scope.skipVote = function() {
        return chatService.polls[chatService.activeRoom].voted = true;
    };

    $scope.vote = function(option) {
        return chatService.sendMessage({
            "type": "POLL_VOTE",
            "room": chatService.activeRoom,
            "pollOption": option
        });
    };
}]);

controlsModule.controller("MenuController", ["$scope", function($scope) {
    $scope.active = null;

    $scope.toggle = function(name) {
        if ($scope.active === name) {
            $scope.active = null;
        } else {
            $scope.active = name;
        }
    };

    $scope.hideEmoticons = function() {
        $scope.active = null;
    };

    $scope.inSideMenu = function() {
        return ["settings", "online"].indexOf($scope.active) !== -1;
    }
}]);

controlsModule.controller("SettingsController", ["$scope", "chatService", "$modal", "chatSettings", "$cookieStore",
"$translate", "notificationService", function($scope, chat, $modal, settings, $cookieStore, $translate, notificationService) {
    $scope.langs = {
        "ru": {"short": "ru", "title": "russian"},
        "en": {"short": "en", "title": "english"},
        "ua": {"short": "ua", "title": "ukrainian"}
    };
    $scope.selectedLanguage = $scope.langs[$translate.use()];

    $scope.$watch("selectedLanguage", function(newValue) {
        $translate.use(newValue.short);
    });

    $scope.compact = settings.getS("compact");
    $scope.$watch("compact", function() {
        settings.setS("compact", $scope.compact);
    });
    $scope.showIgnored = settings.getS("showIgnored");
    $scope.$watch("showIgnored", function() {
        settings.setS("showIgnored", $scope.showIgnored);
    });
    $scope.showTS = settings.getS("showTS");
    $scope.$watch("showTS", function() {
        settings.setS("showTS", $scope.showTS);
    });
    $scope.hideMB = settings.getS("hideMB");
    $scope.$watch("hideMB", function() {
        settings.setS("hideMB", $scope.hideMB);
    });
    $scope.dark = settings.getS("dark");
    $scope.$watch("dark", function() {
        settings.setS("dark", $scope.dark);
    });
    $scope.hideExt = settings.getS("hideExt");
    $scope.$watch("hideExt", function() {
        settings.setS("hideExt", $scope.hideExt);
    });

    $scope.notifications = settings.getS("notifications");
    $scope.$watch("notifications", function() {
        if ($scope.notifications) {
            if (notificationService.hasPermission()) {
                settings.setS("notifications", true);
            } else {
                notificationService.requestPermissionAndEnable();
            }
        } else {
            settings.setS("notifications", false);
        }
    });

    $scope.notificationsAvailable = function() {
        return notificationService.isAvailable();
    };

    chat.selfUpdatedCallbacks.push(function() {
        var c = chat.self.color;
        if (c == "black") {
            c = "#000000"
        }
        $scope.color = c;
        $scope.$digest();
    });

    $scope.color = chat.self ? chat.self.color : "#ffffff";

    $scope.getSelf = function() {
        return chat.self;
    };

    $scope.isAdmin = function() {
        return chat.self && (chat.self.role >= globalLevels.ADMIN);
    };

    $scope.showAdmin = function() {
        window.open("/admin/", '_blank');
    };

    $scope.showProfile = function() {
        $modal.open({
            templateUrl: 'chat/ui/profile/profile.html',
            controller: "ProfileController",
            size: "sm"
        });
    };

    $scope.showTickets = function() {
        $modal.open({
            templateUrl: 'chat/ui/tickets/list.html',
            controller: "TicketsController"
        });
    };

    $scope.showHelp = function() {
        $modal.open({
            templateUrl: 'help.html',
            controller: HelpController
        });
    };

    $scope.canLogin = function() {
        return !chat.self || (chat.self.role < globalLevels.USER);
    };

    $scope.setColor = function(ok) {
        if (ok) {
            chat.sendMessage({"type": "COLOR", "color": $scope.color});
        } else {
            $scope.color = chat.self.color;
        }
    };

    $scope.twitchAuth = function() {
        window.open("https://" + HOST_NAME + ":1337/twitch_auth");
    };

    $scope.showSignIn = function () {
        $modal.open({
            templateUrl: 'authentication.html',
            controller: AuthenticationController,
            resolve: {
                "action": function () { return "sign_in"; },
                "chat": function () { return chat; }
            }
        });
    };

    $scope.showSignUp = function () {
        $modal.open({
            templateUrl: 'authentication.html',
            controller: AuthenticationController,
            resolve: {
                "action": function () { return "registration"; },
                "chat": function () { return chat; }
            }
        });
    };

    $scope.logOut = function() {
        $cookieStore.remove("sid");
        chat.sendMessage({"type": "LOGOUT"});
        chat.ws.close();
    };

    $scope.lebannen = function() {
        return chat.self && (chat.self.name.toLowerCase() === "atplay");
    };

    $scope.popout = function() {
        var body = $("body");
        window.open(document.location, "_blank", "height="+body.height()+", width=" + body.width());
    };

    $scope.colorFilter = function(color) {
        if (chat.self && chat.self.role === globalLevels.SUPERADMIN) {
            return true;
        } else {
            var brightness = (color.r * 3 + color.b + color.g * 4) >> 3;
            return brightness < 170 && brightness > 70;
        }
    };
}]);

var AnonCaptchaController = function($scope, $modalInstance, id, isUser) {
    document.lastModal = $modalInstance;

    $scope.isUser = isUser;

    $scope.ok = function () {
        $.post("recaptcha/" + id, $("#anonCaptchaForm").serialize(), function(data) {
            if (data == "OK") {
                $modalInstance.close();
            } else {
                $scope.error = true;
                $scope.message = "Wrong captcha";
                $scope.$digest();
            }
        });
    };

    $scope.twitchAuth = function() {
        window.open("https://" + HOST_NAME + ":1337/twitch_auth");
    };

    $scope.showSignIn = function() {
        window.open("https://" + HOST_NAME + ":1337/login");
    };

    $scope.showSignUp = function() {
        window.open("https://" + HOST_NAME + ":1337/register");
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };
};

var AuthenticationController = function($scope, $modalInstance, chat, action) {
    $scope.action = action;
    $scope.captchaRequired = false;
    $scope.input = {};
    $scope.recaptchaWidgetId = null;
    $scope.busy = false;

    $scope.recaptchaCreated = function(widgetId) {
        console.log(widgetId);
        $scope.recaptchaWidgetId = widgetId;
    };

    $scope.twitchAuth = function() {
        window.open("https://" + HOST_NAME + ":1337/twitch_auth");
    };

    $scope.submitSignIn = function() {
        $scope.busy = true;
        $.post("/login", $("#authForm").serialize(), function(data) {
            if (data["success"]) {
                $scope.busy = false;
                $scope.error = null;
                $scope.info = null;
                $modalInstance.close();
                chat.ws.close();
            } else {
                $scope.info = null;
                $scope.busy = false;
                $(".g-recaptcha-response").value = "";
                $scope.error = data["error"];
                $scope.captchaRequired = data["captchaRequired"];
                if ($scope.recaptchaWidgetId !== null) {
                    $(".pls-container").remove();
                    grecaptcha.reset($scope.recaptchaWidgetId);
                }
                $scope.$digest();
            }
        });
    };

    $scope.submitRegistration = function() {
        $scope.busy = true;
        $.post("/register", $("#regForm").serialize(), function(data) {
            if (data["success"]) {
                $scope.busy = false;
                $scope.info = "You have successfuly registered. An email with information how to confirm your " +
                    "account has been sent to address you entered.";
                $scope.switchTo("sign_in");
                $scope.$digest();
            } else {
                $scope.info = null;
                $scope.busy = false;
                $(".g-recaptcha-response").value = "";
                $scope.error = data["error"];
                if ($scope.recaptchaWidgetId !== null) {
                    $(".pls-container").remove();
                    grecaptcha.reset($scope.recaptchaWidgetId);
                }
                $scope.$digest();
            }
        });
    };

    $scope.switchTo = function(action) {
        $scope.action = action;
        $(".pls-container").remove();
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };

    $.ajax("/login").done(function(data) {
        $scope.captchaRequired = data["captchaRequired"];
        $scope.$digest();
    });

    document.lastModal = $modalInstance;
    $(".pls-container").remove();
};

controlsModule.controller("EmoticonsController", ["$scope", "chatService", function($scope, chat) {
    $scope.emoticons = chat.emoticons;

    $scope.unescapeCode = function(code) {
        return code.replace(/\\(.)/, "$1").replace("&lt;", "<").replace("&gt;", ">").replace("&#59;", ";");
    };

    $scope.addToInput = function(text) {
        chat.addToInputCallback(text);
        $scope.hideEmoticons();
    };
}]);

var HelpController = function($scope, $modalInstance) {
    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };
};

controlsModule.controller("StyleController", ["$scope", "chatSettings", function($scope, settings) {
    $scope.isDark = function() {
        return settings.getS("dark");
    }
}]);

controlsModule.directive('usernameValidation', function($q, $http) {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$asyncValidators.username = function(modelValue, viewValue) {
                if (ctrl.$isEmpty(modelValue)) {
                    // consider empty model valid
                    return $q.when();
                }

                var def = $q.defer();

                $http.post("/rest/checkUsername", {"name": modelValue})
                    .success(function (data) {
                        if (data["available"]) {
                            def.resolve()
                        } else {
                            def.reject();
                        }
                    })
                    .error(function () {
                        def.reject();
                    });

                return def.promise;
            }
        }
    };
});

controlsModule.directive('textcomplete', ['Textcomplete', "chatService", function(Textcomplete, chat) {
    return {
        restrict: 'EA',
        scope: {
            members: '=',
            message: '='
        },
        template: "<input id=\"userInput\" placeholder=\"Chat about this nostream\" ng-trim=\"false\"" +
            "ng-model=\"message\" type=\"text\" ng-model-options=\"{ debounce: 100 }\"/>",
        link: function(scope, iElement, iAttrs) {
            var ta = iElement.find('#userInput');
            var textcomplete = new Textcomplete(ta, [
                {
                    match: /\B@(\w*)$/,
                    search: function(term, callback) {
                        var result = [];
                        if (chat.lastChatters[chat.activeRoom]) {
                            result = $.map(chat.lastChatters[chat.activeRoom], function (u) {
                                var name = u.name;
                                return name.indexOf(term) === 0 ? name : null;
                            })
                        }

                        callback(result);
                    },
                    index: 1,
                    replace: function(mention) {
                        return '@' + mention + ' ';
                    },
                    placement: "auto",
                    maxCount: 5
                }
            ]);

            $(textcomplete).on({
                'textComplete:select': function (e, value) {
                    scope.$digest(function() {
                        scope.message = value
                    })
                },
                'textComplete:show': function (e) {
                    $(this).data('autocompleting', true);
                },
                'textComplete:hide': function (e) {
                    $(this).data('autocompleting', false);
                }
            });
        }
    }
}]);

messagesModule.controller("UserInputController", ["$scope", "$modal", "chatService", "chatSettings", "$cookieStore", function($scope, $modal, chat, settings, $cookieStore) {
    chat.stateUpdatedCallback = function() {$scope.$digest()};
    $scope.message = "";

    chat.addToInputCallback = function(string) {
        if ((string.indexOf("@") === 0) && ($scope.message.indexOf("/") === 0)) {
            string = string.substr(1);
        }
        $scope.message = $scope.message + string;
        $("#userInput").focus();
    };

    $scope.isConnected = function() {
        return chat.state === CHAT_STATE.AUTHENTICATED;
    };

    $scope.isAuthenticated = function() {
        return chat.self && (chat.self.role >= globalLevels.USER)
    };

    $scope.isConnecting = function() {
        return chat.state === CHAT_STATE.CONNECTING;
    };

    $scope.isAuthenticating = function() {
        return chat.state === CHAT_STATE.AUTHENTICATING;
    };

    $scope.isDisconnected = function() {
        return chat.state === CHAT_STATE.DISCONNECTED;
    };

    $scope.getReconnectTime = function() {
        return chat.reconnectAt;
    };


    $scope.showSignIn = function () {
        $modal.open({
            templateUrl: 'authentication.html',
            controller: AuthenticationController,
            resolve: {
                "action": function () { return "sign_in"; },
                "chat": function () { return chat; }
            }
        });
    };

    $scope.sendMessage = function(e) {
        if (e) {
            e.preventDefault();
        }

        var msg = $scope.message;
        var send = true;
        var message = {};
        if (msg[0] == '/') {
            msg = msg.slice(1);
            var tmp = msg.split(' ');
            message["type"] = tmp[0].toUpperCase();

            if (tmp[0].toUpperCase() === "LOGOUT") {
                $cookieStore.remove("sid");
                chat.ws.close();
                send = false;
            } else if (tmp[0].toUpperCase() === 'LOGIN') {
                send = false;
            } else if (tmp[0].toUpperCase() === 'ME') {
                message["room"] = chat.activeRoom;
                message["text"] = msg.substr(msg.indexOf(" ") + 1);
            } else if (tmp[0].toUpperCase() === 'NAME') {
                message['name'] = msg.substr(msg.indexOf(" ") + 1);
            } else if (tmp[0].toUpperCase() === 'IGNORE') {
                if (tmp[1]) {
                    message["name"] = tmp[1].toLowerCase();
                } else {
                    send = false;
                    chat.showIgnoreList();
                }
            } else if (tmp[0].toUpperCase() === "UNIGNORE") {
                if (tmp[1]) {
                    message["name"] = tmp[1].toLowerCase();
                }
            } else if (tmp[0].toUpperCase() === "CLEAR") {
                message["room"] = chat.activeRoom;
                if (tmp[1]) {
                    message["name"] = tmp[1];
                } else {
                    message["type"] = "CLEAR_ROOM"
                }
            } else if ((tmp[0].toUpperCase() === "BAN")||(tmp[0].toUpperCase() === "UNBAN")||(tmp[0].toUpperCase() === "TIMEOUT")) {
                message["room"] = chat.activeRoom;
                message["name"] = tmp[1];
            } else if ((tmp[0].toUpperCase() === "PROXYMOD")) {
                message["type"] = "PROXY_MOD";
                message["room"] = chat.activeRoom;
                message["service"] = tmp[1];
                message["serviceResource"] = tmp[2];
                message["text"] = tmp[3].toUpperCase();
                message["name"] = tmp[4];
            } else if (tmp[0].toUpperCase() === "ROLE") {
                message["room"] = chat.activeRoom;
                message["name"] = tmp[1];
                message["role"] = tmp[2];
            } else if (tmp[0].toUpperCase() === "COLOR") {
                message["color"] = tmp[1];
            }
        } else {
            if (msg.length > 0) {
                message['type'] = 'MSG';
                message["room"] = chat.activeRoom;
                message["text"] = msg;
            } else {
                send = false;
            }
        }

        if (send) {
            chat.lastSent = msg;
            chat.sendMessage(message);
            console.log('OUT: ' + message);
        }
        $scope.message = "";
    }
}]);

controlsModule.directive('timer', ["$timeout", function($timeout) {
    return {
        restrict: 'E',
        scope: {
            until: '='
        },
        template: "{{time}}",
        link: function($scope) {
            var timeout = null;
            $scope.time = Math.floor(($scope.until - Date.now())/1000);
            var onTimeout = function() {
                $scope.time = Math.floor(($scope.until - Date.now())/1000);
                if($scope.time < 0) {
                    $scope.time = 0;
                }
                timeout = $timeout(onTimeout, 1000);
            };
            timeout = $timeout(onTimeout, 1000);
            $scope.$on('$destroy', function() {
                console.log("destroy");
                $timeout.cancel(timeout);
            });
        }
    }
}]);

controlsModule.controller("TitleController", ["$scope", "chatService", function($scope, chatService) {
    $scope.getRoomName = function() {
        return chatService.activeRoom;
    }
}])

