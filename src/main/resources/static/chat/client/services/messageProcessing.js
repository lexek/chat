var module = angular.module("chat.messageProcessing", ["chat.services.settings", "chat.services.notifications"]);

module.service("messageProcessingService", ["$q", "$sce", "$translate", "$modal", "$timeout", "chatSettings", "notificationService", function($q, $sce, $translate, $modal, $timeout, settings, notificationService) {
    'use strict';

    var Message = function (type, body, user, showModButtons, id_, time, showTS, hidden) {
        this.type = type;
        this.body = $sce.trustAsHtml(body);
        this.showModButtons = showModButtons;
        this.id_ = id_;
        this.user = user;
        this.hidden = hidden;
        this.likes = [];
        this.time = time;
        this.showTS = showTS;
        this.messageUpdatedCallbacks = [];
        this.addToInputCallback = angular.noop;
    };

    var MessageGroup = function(user, showModButtons, showTS) {
        this.type = "MSG_GROUP";
        this.user = user;
        this.showModButtons = showModButtons;
        this.messages = [];
        this.messageUpdatedCallbacks = [];
        this.addToInputCallback = angular.noop;
        this.showTS = showTS;
    };

    var GroupedChatMessage = function(body, id_, time, hidden) {
        this.body = body;
        this.id_ = id_;
        this.time = time;
        this.hidden = hidden;
        this.likes = [];
    };

    var ChatMessage = function (type, body, user, showModButtons, id_, time, showTS, hidden) {
        this.type = type;
        this.body = body;
        this.showModButtons = showModButtons;
        this.id_ = id_;
        this.user = user;
        this.hidden = hidden;
        this.likes = [];
        this.time = time;
        this.showTS = showTS;
        this.messageUpdatedCallbacks = [];
        this.addToInputCallback = angular.noop;
    };

    var processTweetMessage = function(tweet) {
        tweet.text = twemoji.parse(tweet.text, {
            base: "/img/",
            folder: "twemoji",
            ext: ".png",
            callback: function(icon, options) {
                switch ( icon ) {
                    case 'a9':      // copyright
                    case 'ae':      // registered trademark
                    case '2122':    // trademark
                        return false;
                }
                return ''.concat(options.base, options.size, '/', icon, options.ext);
            }
        });
        tweet.text = $sce.trustAsHtml(tweet.text);
        if (tweet.retweetedStatus) {
            processTweetMessage(tweet.retweetedStatus);
        }
        if (tweet.quotedStatus) {
            processTweetMessage(tweet.quotedStatus)
        }
    };

    var TweetMessage = function(tweet) {
        this.type = "TWEET";
        this.tweet = tweet;
        processTweetMessage(this.tweet);
        this.messageUpdatedCallbacks = [];
        this.addToInputCallback = angular.noop;
    };

    var processClearMessage = function (chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if ((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room)) {
            chat.addMessage(
                new Message("INFO", $translate.instant("CHAT_CLEAR_USER", {"mod": msg.mod, "user": msg.name}))
            );
            chat.incMessageCount();
        }
        chat.hideMessagesFromUser(ctx.room, msg.name);
        chat.messagesUpdated();
    };

    var processProxyClearMessage = function (chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if ((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room)) {
            chat.addMessage(
                new Message("INFO", $translate.instant("CHAT_CLEAR_USER", {"mod": ctx.msg.service, "user": msg.name}))
            );
            chat.incMessageCount();
        }
        chat.hideMessagesFromUser(ctx.room, msg.name, ctx.msg.service, ctx.msg.serviceResource);
        chat.messagesUpdated();
    };

    var processClearRoomMessage = function(chat, ctx) {
        chat.lastChatter(ctx.room, null);
        if (!((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room))) {
            chat.messages[ctx.room].length = 0;
        }
        chat.addMessage(new Message("INFO", $translate.instant("CHAT_CLEAR")), ctx.room);
        chat.messagesUpdated();
    };

    function checkForMention(chat, msg) {
        var notify = false;
        angular.forEach(msg, function (e) {
            if (e.children) {
                notify = checkForMention(chat, e.children) || notify;
            }
            if (e.type === 'MENTION') {
                if (e.text === chat.self.name) {
                    e.currentUser = true;
                    notify = true;
                }
            }
        });
        return notify;
    }

    function processChatMessage(chat, ctx, msg) {
        ctx.proc = {
            unprocessedText: msg.text,
            mention: false
        };
        var service = msg.type === "MSG_EXT" ? ctx.msg["service"] : null;
        var serviceRes = service ? ctx.msg["serviceResource"] : null;
        var serviceResName = service ? ctx.msg["serviceResourceName"] : null;
        var user = new User(msg.name, msg.color, levels[msg.role], globalLevels[msg.globalRole], service, serviceRes, serviceResName);
        ctx.proc.user = user;

        var showModButtons = (user.role !== levels.ADMIN) &&
            (
                chat.self &&
                (chat.self.name !== user.name)
            ) && (
                (
                    (chat.localRole[ctx.room] >= levels.MOD) &&
                    (chat.localRole[ctx.room] > user.role) &&
                    (chat.self.role >= user.globalRole)
                ) || (
                    (chat.self.role >= globalLevels.MOD) &&
                    (chat.self.role > user.globalRole)
                )
            ) && (
                (msg.type !== 'MSG_EXT') || chat.isProxyModerationEnabled(ctx.room, service, serviceRes)
            );
        var ignored = chat.ignoredNames.indexOf(user.name.toLowerCase()) != -1;
        var showIgnored = settings.getS("showIgnored");
        var hideExt = settings.getS("hideExt");
        var hidden = ignored && showIgnored;
        var omit = (ignored && !showIgnored) ||
            ((msg.type === "MSG_EXT") && !chat.isProxyOutboundEnabled(ctx.room, service, serviceRes) && hideExt);

        if (!omit) {
            var mention = checkForMention(chat, ctx.msg.messageNodes);

            chat.incMessageCount();
            var elem = null;
            var lastChatter = chat.lastChatterInRoom[ctx.room];
            var previousMessage = chat.lastMessage[ctx.room];
            var stackWithPrevious =
                (previousMessage.type === "MSG_GROUP") && ((msg.type === "MSG") || (msg.type === "MSG_EXT")) &&
                (lastChatter &&
                    (lastChatter.name.toLowerCase() === user.name.toLowerCase()) &&
                    (lastChatter.service === user.service) && (lastChatter.serviceRes === user.serviceRes)
                ) &&
                (previousMessage.messages.length < 5);
            chat.lastChatter(ctx.room, user);
            if (stackWithPrevious) {
                var e = new GroupedChatMessage(ctx.msg.messageNodes, msg.id, msg.time, hidden);
                previousMessage.messages.push(e);
                chat.messageUpdated(previousMessage);
                chat.messagesUpdated();
            } else {
                if (msg.type === "MSG" || msg.type === "MSG_EXT") {
                    elem = new MessageGroup(user, showModButtons, ctx.history);
                    var e = new GroupedChatMessage(ctx.msg.messageNodes, msg.id, msg.time, hidden);
                    elem.messages.push(e);
                } else {
                    elem = new ChatMessage(msg.type, ctx.msg.messageNodes, user, showModButtons, msg.id, msg.time, ctx.history, hidden);
                }
            }
            if (elem != null) {
                chat.addMessage(elem, ctx.room, ctx.history, mention);
                chat.messagesUpdated();
            }
            if (mention && !ctx.history) {
                notificationService.notify(user.name, ctx.msg.messageNodes.map(function (e) {
                    return e.text;
                }).join());
            }
        }
    };

    var processColorMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message(
                "INFO",
                "<span style=\"opacity: 0.7\">Your color is now </span> <span style=\"color:" + msg.color + "\">" +
                    msg.color + "</span>"
            ),
            ctx.room
        );
        chat.messagesUpdated();
        chat.self.color = msg.color;
    };

    var processSelfJoinMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.localRole[ctx.room] = levels[ctx.msg["chatter"]["role"]];
        if (chat.self.role === globalLevels.UNAUTHENTICATED) {
            chat.addMessage(
                new Message("INFO", $translate.instant("CHAT_HELLO_UNAUTHENTICATED", {"room": ctx.room})), ctx.room);
        } else {
            chat.addMessage(new Message("INFO", $translate.instant("CHAT_HELLO",
                {
                    "color": chat.self.color,
                    "name": chat.self.name,
                    "role": $translate.instant("ROLE_" + ctx.msg["chatter"]["role"]),
                    "room": ctx.room
                }
            )), ctx.room);
        }
        if (chat.self.role === globalLevels.USER_UNCONFIRMED) {
            chat.addMessage(new Message("ERROR", $translate.instant("CHAT_HELLO_UNCONFIRMED"), ctx.room));
        }
        chat.addRoom(ctx.room);
        chat.messagesUpdated();
    };

    var processDonationMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if (!msg.text) {
            chat.addMessage(new Message("INFO", $translate.instant("MESSAGE_DONATED", msg)), ctx.room);
        } else {
            chat.addMessage(new Message("INFO", $translate.instant("MESSAGE_DONATED_MESSAGE", msg)), ctx.room);
        }
        if (!ctx.history) {
            chat.messagesUpdated();
        }
    };

    var processTimeoutMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message("INFO", $translate.instant("CHAT_TIMEOUT_USER", {"mod": msg.mod, "user": msg.name})), ctx.room);
        chat.incMessageCount();
        chat.hideMessagesFromUser(ctx.room, msg.name);
        if (!ctx.history) {
            chat.messagesUpdated();
        }
    };

    var processBanMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message("INFO", $translate.instant("CHAT_BAN_USER", {"mod": msg.mod, "user": msg.name})), ctx.room);
        chat.incMessageCount();
        chat.hideMessagesFromUser(ctx.room, msg.name);
        if (!ctx.history) {
            chat.messagesUpdated();
        }
    };

    var processRoleMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if (msg.role) {
            chat.self.role = globalLevels[msg.role];
            chat.addMessage(
                new Message("INFO", '<span style="opacity: 0.7">You are <strong>' + msg.role + '</strong></span>'),
                ctx.room
            );
        }
        chat.messagesUpdated();
    };

    var processLikeMessage = function(chat, ctx, msg) {
        var likedMessage = null;
        var group = null;
        angular.forEach(chat.messages[ctx.room], function(m) {
            if (m.type === "MSG_GROUP") {
                angular.forEach(m.messages, function(mm) {
                    if (mm.id_ === msg.id) {
                        likedMessage = mm;
                        group = m;
                    }
                });
            } else {
                if (m.id_ === msg.id) {
                    likedMessage = m;
                }
            }
        });
        if (likedMessage && likedMessage.likes.indexOf(msg.name) == -1) {
            likedMessage.likes.push(msg.name);
            if (group) {
                chat.messageUpdated(group);
            } else {
                chat.messageUpdated(likedMessage);
            }
        }
    };

    var processTweet = function(chat, msg) {
        chat.addMessage(new TweetMessage(msg.tweet), msg.room);
        chat.messagesUpdated();
        chat.incMessageCount();
    };

    var MessageProcessingService = function() {

    };

    MessageProcessingService.prototype.processMessage = function(chat, m, hist) {
        var self = this;
        var ctx = {
            msg: m,
            room: m["room"] || chat.activeRoom,
            history: hist || false
        };
        var message = {
            type: m["type"],
            user: m["user"],
            text: m["text"],
            name: m["name"],
            mod: m["mod"],
            color: m["color"],
            role: m["role"],
            globalRole: m["globalRole"],
            id: m["messageId"],
            time: m["time"],
            pollData: m["pollData"]
        };
        ctx.currentPoll = chat.polls[ctx.room];

        switch (message.type) {
            case "PROXY_CLEAR":
                processProxyClearMessage(chat, ctx, message);
                break;
            case "CLEAR":
                processClearMessage(chat, ctx, message);
                break;
            case "CLEAR_ROOM":
                processClearRoomMessage(chat, ctx);
                break;
            case "HIST":
                angular.forEach(m["history"], function(e) {
                    self.processMessage(chat, e, true);
                });
                chat.messagesUpdated();
                break;
            case "ME":
            case "MSG":
            case "MSG_EXT":
            {
                processChatMessage(chat, ctx, message);
                break;
            }
            case 'COLOR':
            {
                processColorMessage(chat, ctx, message);
                break;
            }
            case 'INFO':
                chat.lastChatter(ctx.room, null);
                chat.addMessage(new Message(message.type, message.text), ctx.room);
                chat.messagesUpdated();
                break;
            case 'AUTH_REQUIRED':
                chat.addToInputCallback(chat.lastSent);
                $modal.open({
                    templateUrl: 'authentication.html',
                    controller: AuthenticationController,
                    size: "sm",
                    resolve: {
                        "action": function() { return "sign_in"; },
                        "chat": function () { return chat; }
                    }
                });
                break;
            case 'RECAPTCHA':
                $modal.open({
                    templateUrl: 'anonCaptcha.html',
                    controller: AnonCaptchaController,
                    resolve: {
                        id: function () {
                            return message.text;
                        },
                        isUser: chat.self.role === levels.USER
                    }
                });
                break;
            case 'SELF_JOIN':
                processSelfJoinMessage(chat, ctx, message);
                break;
            case 'AUTH_COMPLETE':
                chat.self = message.user;
                chat.self.role = globalLevels[chat.self.role];
                chat.state = CHAT_STATE.AUTHENTICATED;
                chat.stateUpdated();
                if (chat.rooms.length > 0) {
                    angular.forEach(chat.rooms, function (e) {
                        chat.sendMessage({"type": "JOIN", "room": e});
                    });
                }
                break;
            case 'TIMEOUT':
                processTimeoutMessage(chat, ctx, message);
                break;
            case 'BAN':
                processBanMessage(chat, ctx, message);
                break;
            case 'LOGIN':
                chat.lastChatter(ctx.room, null);
                chat.self = message.user;
                chat.self.role = globalLevels[chat.self.role];
                chat.addMessage(new Message(message.type, $translate.instant("CHAT_LOGIN_SUCCESS",
                    {
                        "name": chat.self.name,
                        "role": $translate.instant("ROLE_" + chat.self.role.title)
                    }
                )), ctx.room);
                chat.messagesUpdated();
                break;
            case 'ROLE':
                processRoleMessage(chat, ctx, message);
                break;
            case 'ERROR':
                chat.lastChatter(ctx.room, null);
                chat.addMessage(new Message(message.type, $translate.instant("ERROR_"+message.text,
                    {
                        "args": m["errorData"]
                    }
                )), ctx.room);
                chat.messagesUpdated();
                break;
            case "LIKE":
                processLikeMessage(chat, ctx, message);
                break;
            case "POLL":
                chat.polls[ctx.room] = message.pollData;
                message.pollData.voted = false;
                message.pollData.open = true;
                message.pollData.maxPollVotes = Math.max.apply(null, message.pollData.votes);
                chat.pollsUpdatedCallback();
                chat.lastChatter(ctx.room, null);
                //show announcement-like message
                chat.addMessage(new Message("INFO", $translate.instant("POLL_ANNOUNCEMENT",
                    {
                        "question": message.pollData.poll.question
                    }
                )), ctx.room);
                chat.messagesUpdated();
                break;
            case "POLL_UPDATE":
                if (ctx.currentPoll.poll.id === message.pollData.poll.id) {
                    ctx.currentPoll.votes = message.pollData.votes;
                    ctx.currentPoll.maxPollVotes = Math.max.apply(null, message.pollData.votes);
                    chat.pollsUpdatedCallback();
                }
                break;
            case "POLL_VOTED":
                ctx.currentPoll.voted = true;
                chat.pollsUpdatedCallback();
                break;
            case "POLL_END":
                if (ctx.currentPoll.poll.id === message.pollData.poll.id) {
                    ctx.currentPoll.votes = message.pollData.votes;
                    ctx.currentPoll.maxPollVotes = Math.max.apply(null, message.pollData.votes);
                    ctx.currentPoll.open = false;
                    chat.pollsUpdatedCallback();
                    $timeout(function() {
                        var poll = chat.polls[ctx.room];
                        if (poll.poll.id === message.pollData.poll.id) {
                            chat.polls[ctx.room] = null;
                        }
                    }, 60*1000);
                }
                break;
            case "PROXIES":
                chat.proxies[ctx.room] = ctx.msg.proxies;
                console.log(chat.proxies);
                break;
            case "PROTOCOL_VERSION":
                if (ctx.msg.version !== PROTOCOL_VERSION) {
                    chat.reconnect = false;
                    document.location.reload(true);
                } else {
                    console.log("protocol version matches");
                }
                break;
            case "IGNORED":
                chat.ignoredNames = ctx.msg.names;
                break;
            case "IGNORE":
                chat.addMessage(new Message("INFO", $translate.instant("IGNORE_OK", {
                    "name": message.name
                }), ctx.room));
                chat.messagesUpdated();
                break;
            case "UNIGNORE":
                chat.addMessage(new Message("INFO", $translate.instant("UNIGNORE_OK", {
                    "name": message.name
                }), ctx.room));
                chat.messagesUpdated();
                break;
            case "INTERNAL_IGNORE_LIST":
                chat.addMessage(new Message("INFO", $translate.instant("IGNORE_LIST", {
                    "names": chat.ignoredNames.join(", ")
                }), ctx.room));
                break;
            case "TWEET":
                processTweet(chat, ctx.msg);
                break;
            case "DONATION":
                processDonationMessage(chat, ctx, ctx.msg);
                break;
            default:
                console.log(message);
                break;
        }
    };

    return new MessageProcessingService();
}]);
