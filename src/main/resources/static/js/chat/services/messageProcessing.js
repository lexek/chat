var module = angular.module("chat.messageProcessing", ["chat.services.settings", "chat.services.notifications", "chat.services.linkResolver"]);

module.service("messageProcessingService", ["$q", "$sce", "$translate", "$modal", "$timeout", "chatSettings", "notificationService", "linkResolver", function($q, $sce, $translate, $modal, $timeout, settings, notificationService, linkResolver) {
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

    var GroupedMessage = function(body, id_, time, hidden) {
        this.body = $sce.trustAsHtml(body);
        this.id_ = id_;
        this.time = time;
        this.hidden = hidden;
        this.likes = [];
    };

    var processClearMessage = function (chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if ((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room)) {
            chat.addMessage(
                new Message(msg.type, $translate.instant("CHAT_CLEAR_USER", {"mod": msg.mod, "user": msg.name}))
            );
        }
        chat.messagesUpdated();
        chat.hideMessagesFromUser(ctx.room, msg.name);
    };

    var processProxyClearMessage = function (chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        if ((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room)) {
            chat.addMessage(
                new Message(msg.type, $translate.instant("CHAT_CLEAR_USER", {"mod": ctx.msg.service, "user": msg.name}))
            );
        }
        chat.messagesUpdated();
        chat.hideMessagesFromUser(ctx.room, msg.name, ctx.msg.service, ctx.msg.serviceResource);
    };

    var processClearRoomMessage = function(chat, ctx) {
        chat.lastChatter(ctx.room, null);
        if (!((chat.self.role >= globalLevels.MOD) || chat.hasLocalRole(levels.MOD, ctx.room))) {
            chat.messages[ctx.room].length = 0;
        }
        chat.messages[ctx.room].push(new Message("INFO", $translate.instant("CHAT_CLEAR")));
        chat.messagesUpdated();
    };

    var processTextMessage = function(chat, ctx, msg) {
        ctx.proc = {
            unprocessedText: msg.text,
            mention: false
        };
        var service = msg.type === "MSG_EXT" ? ctx.msg["service"] : null;
        var serviceRes = service ? ctx.msg["serviceResource"] : null;
        var user = new User(msg.name, msg.color, levels[msg.role], globalLevels[msg.globalRole], service, serviceRes);
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
                (msg.type !== "MSG_EXT") || chat.isProxyModerationEnabled(ctx.room, service, serviceRes)
            );
        var ignored = settings.getIgnored().indexOf(user.name.toLowerCase()) != -1;
        var showIgnored = settings.getS("showIgnored");
        var hideExt = settings.getS("hideExt");
        var hidden = ignored && showIgnored;
        var omit = (ignored && !showIgnored) ||
            ((msg.type === "MSG_EXT") && !chat.isProxyOutboundEnabled(ctx.room, service, serviceRes) && hideExt);
        console.log(chat.isProxyModerationEnabled(ctx.room, service, serviceRes));
        console.log(chat.isProxyOutboundEnabled(ctx.room, service, serviceRes));

        if (!omit) {
            var tempText = htmlEscape(ctx.proc.unprocessedText);
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
                var e = new GroupedMessage(tempText, msg.id, msg.time, hidden);
                previousMessage.messages.push(e);
                processMessageText(chat, ctx, msg).then(function() {
                    e.body = $sce.trustAsHtml(ctx.proc.text);
                });
            } else {
                if (msg.type === "MSG" || msg.type === "MSG_EXT") {
                    elem = new MessageGroup(user, showModButtons, ctx.history);
                    var e = new GroupedMessage(tempText, msg.id, msg.time, hidden);
                    elem.messages.push(e);
                    processMessageText(chat, ctx, msg).then(function() {
                        e.body = $sce.trustAsHtml(ctx.proc.text);
                    });
                } else {
                    elem = new Message(msg.type, tempText, user, showModButtons, msg.id, msg.time, ctx.history, hidden);
                    processMessageText(chat, ctx, msg).then(function() {
                        elem.body = $sce.trustAsHtml(ctx.proc.text);
                    });
                }
            }
            if (elem != null) {
                chat.addMessage(elem, ctx.room, ctx.history, ctx.proc.mention);
            }
            if (ctx.proc.mention && !ctx.history) {
                notificationService.notify(user.name, ctx.proc.unprocessedText);
            }
        }
    };

    var processColorMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message(
                msg.type,
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
                new Message(msg.type, $translate.instant("CHAT_HELLO_UNAUTHENTICATED", {"room": ctx.room})), ctx.room);
        } else {
            chat.addMessage(new Message(msg.type, $translate.instant("CHAT_HELLO",
                {
                    "color": chat.self.color,
                    "name": chat.self.name,
                    "role": $translate.instant("ROLE_" + ctx.msg["chatter"]["role"]),
                    "room": ctx.room
                }
            )), ctx.room);
        }
        chat.addRoom(ctx.room);
        chat.messagesUpdated();
    };

    var processTimeoutMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message(msg.type, $translate.instant("CHAT_TIMEOUT_USER", {"mod": msg.mod, "user": msg.name})), ctx.room);
        chat.hideMessagesFromUser(ctx.room, msg.name);
        if (!ctx.history) {
            chat.messagesUpdated();
        }
    };

    var processBanMessage = function(chat, ctx, msg) {
        chat.lastChatter(ctx.room, null);
        chat.addMessage(
            new Message(msg.type, $translate.instant("CHAT_BAN_USER", {"mod": msg.mod, "user": msg.name})), ctx.room);
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
                new Message(msg.type, '<span style="opacity: 0.7">You are <strong>' + msg.role + '</strong></span>'),
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
                processTextMessage(chat, ctx, message);
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
                chat.stateUpdatedCallback();
                if (chat.rooms.length > 0) {
                    angular.forEach(chat.rooms, function (e) {
                        if (e !== "#main") {
                            chat.sendMessage({"type": "JOIN", "room": e});
                        }
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
            default:
                console.log(message);
                break;
        }
    };

    var processTextPart = function(chat, ctx, msg, text) {
        text = htmlEscape(text);
        text = twemoji.parse(text, {
            base: "/img/",
            folder: "twemoji",
            ext: ".png",
            callback: function(icon, options) {
                switch ( icon ) {
                    case 'a9':      // � copyright
                    case 'ae':      // � registered trademark
                    case '2122':    // � trademark
                        return false;
                }
                return ''.concat(options.base, options.size, '/', icon, options.ext);
            }
        });
        if ((msg.type === "MSG_EXT") && (ctx.proc.user.service === "sc2tv.ru")) {
            text = text.replace(/\[\/?b]/g, "**");
            text = text.replace(SC2TV_REGEX, function (match) {
                var emoticon = SC2TV_EMOTE_MAP[match];
                if (emoticon) {
                    return "<img class='emoticon' " +
                        "src='/img/sc2tv/" + emoticon.fileName + "' " +
                        "style='height: " + emoticon.height + "px; width: " + emoticon.width + "px;' " +
                        "title='" + emoticon.code + "'" +
                        "alt='" + emoticon.code + "'></img>"
                } else {
                    return null;
                }
            });
        } else {
            if (chat.emoticons.length !== 0) {
                text = text.replace(chat.emoticonRegExp, function (match) {
                    var emoticon = chat.emoticons[match];
                    if (emoticon) {
                        return "<img class='emoticon' " +
                            "src='emoticons/" + emoticon.fileName + "' " +
                            "style='height: " + emoticon.height + "px; width: " + emoticon.width + "px;' " +
                            "title='" + emoticon.code + "'" +
                            "alt='" + emoticon.code + "'></img>"
                    } else {
                        return null;
                    }
                });
            }
        }
        text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        text = text.replace("@" + chat.self.name, function () {
            ctx.proc.mention = true;
            return "<span class='mentionLabel'>@" + chat.self.name + "</span>"
        });
        return text;
    };

    var isPromise = function(o) {
        return o && angular.isFunction(o.then);
    };

    var doSpoilers = function(data) {
        var spoilers = [];
        angular.forEach(data, function(e, i) {
            if (!isPromise(e)) {
                var idx = -1;
                while ((idx = e.indexOf("%%", idx !== -1 ? idx+2 : 0)) !== -1) {
                    if (idx != -1) {
                        spoilers.push({
                            "chunk": i,
                            "i": idx
                        });
                    }
                }
            }
        });
        if (!(spoilers.length % 2 === 0)) {
            spoilers.pop();
        }
        var chunkOffset = {};
        angular.forEach(spoilers, function(e, i) {
            var s = data[e.chunk];
            var offset = (chunkOffset[e.chunk] || 0);
            var idx = e.i + offset;
            if (i % 2 === 0) {
                data[e.chunk] = s.substring(0, idx) + "<span class=\"spoiler\">" + s.substring(idx+2);
                chunkOffset[e.chunk] = offset + 20;
            } else {
                data[e.chunk] = s.substring(0, idx) + "</span>" + s.substring(idx+2);
                chunkOffset[e.chunk] = offset + 5;
            }
        });
    };

    var processMessageText = function(chat, ctx, msg) {
        var deferred = $q.defer();
        ctx.proc.text = ctx.proc.unprocessedText;
        if ((msg.type === "MSG_EXT") && (ctx.proc.user.service === "sc2tv.ru")) {
            ctx.proc.text = ctx.proc.text.replace(/\[\/?url]/g, "");
        }
        var match;
        var raw = ctx.proc.text;
        var html = [];
        var i;
        while ((match = raw.match(/(https?:\/\/)([^\s]*)/))) {
            i = match.index;
            html.push(processTextPart(chat, ctx, msg, raw.substr(0, i)));
            html.push(linkResolver.resolve(match[0], match[1], match[2]));
            raw = raw.substring(i + match[0].length);
        }
        html.push(processTextPart(chat, ctx, msg, raw));
        var promises = [];
        doSpoilers(html);
        angular.forEach(html, function(e) {
            if (isPromise(e)) {
                promises.push(e);
            }
        });
        $q.all(promises).then(function(result) {
            var i = 0;
            html = $.map(html, function(e) {
                if (isPromise(e)) {
                    return result[i++];
                } else {
                    return e;
                }
            });
            var text = html.join('');
            if (text.startsWith("&gt;")) {
                text = "<span class=\"greenText\">" + text + "</span>";
            } else if (text.indexOf("!!!") === 0 && text.length > 3) {
                text = "<span class=\"nsfwLabel\">NSFW</span> <span class=\"spoiler\">" + text.substr(3) + "</span>";
            }
            ctx.proc.text = text;
            deferred.resolve();
        });
        return deferred.promise;
    };

    return new MessageProcessingService();
}]);
