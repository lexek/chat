var services = angular.module('chat.services', ["ngCookies"]);

var ytKey = "AIzaSyCTxR8OCdIZN5dUxINg6-pwmaDWQiH8KgY";

var now = function() {
    return Math.round(+new Date()/1000)
};

var CHAT_STATE = {
    DISCONNECTED: 1,
    CONNECTING: 2,
    AUTHENTICATING: 3,
    AUTHENTICATED: 4
};

services.service("chatService", ["$modal", "chatSettings", "$translate", "$http", "$timeout", "notificationService",
    function($modal, settings, $translate, $http, $timeout, notificationService) {
    var Message = function (type, body, user, showModButtons, id_, time, showTS, hidden) {
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
        this.ext = false;
        this.extOrigin = null;
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
        this.body = body;
        this.id_ = id_;
        this.time = time;
        this.hidden = hidden;
        this.likes = [];
    };

    var User = function(name, color, role, globalRole, service, serviceRes) {
        this.name = name;
        if (color) {
            this.color = color;
        } else {
            this.color = "#000000";
        }
        if (role) {
            this.role = role;
        } else {
            this.role = levels.GUEST;
        }
        this.globalRole = globalRole;
        this.online = 0;
        this.banned = false;
        this.timedOut = false;
        this.service = service;
        this.serviceRes = serviceRes;
    };

    User.prototype.rgbColor = function() {
        return hexToRgb(this.color);
    };

    /**
     * @constructor
     */
    var chatService = function() {
        this.connectionAttempt = 0;
        this.messagesUpdatedCallbacks = [];
        this.selfUpdatedCallbacks = [];
        this.stateUpdatedCallback = angular.noop;
        this.messages = {};
        this.unreadCount = {};
        this.unreadMentions = {};
        this.lastChatters = {};
        this.lastChatterInRoom = {};
        this.lastMessage = {};
        this.polls = {};
        this.pollsUpdatedCallback = angular.noop;
        this.ws = null;
        this.self = null;
        this.lastSent = "";
        this.emoticons = {};
        this.localRole = {};
        this.activeRoom = settings.getS("lastActiveRoom") || "#main";
        this.rooms = settings.getRooms();
        this.lastMessageTimeout = null;
        this.state = CHAT_STATE.DISCONNECTED;
        this.idCounter = 0;

        var c = this;
        User.prototype.clear = function() {
            c.sendMessage({"type": "CLEAR", args: [c.activeRoom, this.name]})
        };
        User.prototype.timeout = function() {
            c.sendMessage({"type": "TIMEOUT", args: [c.activeRoom, this.name]})
        };
        User.prototype.unban = function() {
            c.sendMessage({"type": "UNBAN", args: [c.activeRoom, this.name]})
        };
        User.prototype.ban = function() {
            c.sendMessage({"type": "BAN", args: [c.activeRoom, this.name]})
        };

        $http({method: 'GET', url: '/admin/api/emoticons'})
            .success(function(d, status, headers, config) {
                if (status == 200) {
                    var data = d["records"];
                    var emoticonCodeList = [];
                    angular.forEach(data, function (e) {
                        c.emoticons[e[1]] = {
                            "id": e[0],
                            "code": e[1],
                            "fileName": e[2],
                            "height": e[3],
                            "width": e[4]
                        };
                        emoticonCodeList.push(e[1]
                            .replace("\\", "\\\\")
                            .replace(")", "\\)")
                            .replace("(", "\\(")
                            .replace(".", "\\.")
                            .replace("*", "\\*"));
                    });
                    c.emoticonRegExp = new RegExp(emoticonCodeList.join("|"), "g");
                }
            })
            .error(function(data, status, headers, config) {
                console.log("failed to load emoticons");
            });
    };

    chatService.prototype.addMessage = function(message, room, hist, mention) {
        message.internalId = this.idCounter++;
        if (!room) {
            room = this.activeRoom;
        }
        if (!this.messages[room]) {
            this.messages[room] = []
        }
        if (this.messages[room].length > 100) {
            var e = this.messages[room][0];
            this.messages[room].remove(e);
        }
        this.lastMessage[room] = message;
        this.messages[room].push(message);

        if (room !== this.activeRoom && !hist && ((message.type==="ME") || (message.type==="MSG"))) {
            if (this.unreadCount[room]) {
                this.unreadCount[room]++;
            } else {
                this.unreadCount[room] = 1;
            }
            if (mention) {
                if (this.unreadMentions[room]) {
                    this.unreadMentions[room]++;
                } else {
                    this.unreadMentions[room] = 1;
                }
            }
        }
    };

    chatService.prototype.messagesUpdated = function() {
        angular.forEach(this.messagesUpdatedCallbacks, function (callback) {
            callback()
        });
    };

    chatService.prototype.messageUpdated = function(message) {
        angular.forEach(message.messageUpdatedCallbacks, function (callback) {
            callback()
        });
    };

    chatService.prototype.hideMessagesFromUser = function(room, name) {
        var chat = this;
        angular.forEach(this.messages[room], function(message) {
            if (!message.hidden && message.user && message.user.name == name) {
                if (message.type === "MSG_GROUP") {
                    angular.forEach(message.messages, function(msg) {
                        msg.hidden = true;
                    });
                } else {
                    message.hidden = true;
                }
                chat.messageUpdated(message);
            }
        });
    };

    chatService.prototype.addRoom = function(room) {
        if (this.rooms.indexOf(room) === -1) {
            this.rooms.push(room);
            settings.addRoom(room);
        }
    };

    chatService.prototype.deleteRoom = function(room) {
        this.rooms.remove(room);
        settings.deleteRoom(room);
    };

    chatService.prototype.setActiveRoom = function(room) {
        this.activeRoom = room;
        this.unreadCount[room] = 0;
        this.unreadMentions[room] = 0;
        settings.setS("lastActiveRoom", room);
    };

    chatService.prototype.lastChatter = function(room, name) {
        this.lastChatterInRoom[room] = name;
        if (name) {
            if (!this.lastChatters[room]) {
                this.lastChatters[room] = []
            }
            var a = this.lastChatters[room];
            var idx = a.indexOf(name);
            if (idx !== -1) {
                a.splice(idx, 1);
            }
            this.lastChatters[room].push(name);

            if (a.length > 50) {
                a.shift();
            }
        }
    };

    chatService.prototype.init = function() {
        var chat = this;
        var start = function() {
            chat.connectionAttempt = (chat.connectionAttempt > 7) ? chat.connectionAttempt : (chat.connectionAttempt + 1);
            chat.state = CHAT_STATE.CONNECTING;
            chat.ws = new WebSocket('wss://' + document.location.hostname + ':1488');
            chat.ws.onopen = function () {
                console.log("open");
                chat.state = CHAT_STATE.AUTHENTICATING;
                chat.stateUpdatedCallback();
                chat.sendMessage({"type": "SESSION", "args": [read_cookie("sid")]});
                if (chat.rooms.length > 0) {
                    angular.forEach(chat.rooms, function (e) {
                        if (e !== "#main") {
                            chat.sendMessage({"type": "JOIN", "args": [e]});
                        }
                    });
                }
                chat.connectionAttempt = 0;
            };
            chat.ws.onclose = function () {
                if (chat.connectionAttempt === 0) {
                    chat.reconnectAt = Date.now();
                    start();
                } else {
                    var time = 250 * Math.pow(2, chat.connectionAttempt);
                    chat.reconnectAt = Date.now() + time;
                    setTimeout(function () {
                        start();
                    }, time);
                }
                chat.polls = {};
                chat.pollsUpdatedCallback();
                chat.state = CHAT_STATE.DISCONNECTED;
                chat.stateUpdatedCallback();
            };
            chat.ws.onmessage = function (msg) {
                var message = angular.fromJson(msg.data);
                console.log(message);
                if (chat.lastMessageTimeout) {
                    clearTimeout(chat.lastMessageTimeout);
                }
                chat.lastMessageTimeout = setTimeout(function() {
                    chat.sendMessage({"type": "PING", "args": []});
                }, 30000);

                if (message.type !== "PONG") {
                    processMsg(chat, message, false);
                }
            };
        };

        start();

        window.addEventListener('message', function(event) {
            if (event.origin == 'https://' + HOST_NAME + ':1337') {
                if (event.data === 'auth-notify') {
                    chat.ws.close();
                    if (document.lastModal) {
                        document.lastModal.close();
                    }
                }
            }
        }, false);

        var fetchYoutubeTitle = function(videoId, ytKey) {
            var result = null;
            $.ajax({
                "url": "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + ytKey,
                "success": function (data) {
                    if (data.items && data.items[0] && data.items[0].snippet) {
                        result = "<span style=\"color:#cd201f;\" class=\"fa fa-youtube-play\"></span> " + htmlEscape(data.items[0].snippet.title);
                    }
                },
                "async": false,
                "timeout": 100
            });
            return result;
        };

        var processLink = function (completeLink, prefix, link) {
            var linkText = "";
            try {
                linkText = $.trim(decodeURIComponent(link));
                if (linkText.length === 0) {
                    linkText = link;
                }
            } catch (e) {
                linkText = link;
            }
            if (linkText.length > 37) {
                linkText = linkText.substr(0, 32) + "[...]";
            }
            linkText = htmlEscape(linkText);
            var notProcessed = true;
            var parsedUrl = new Uri(link);
            var host = parsedUrl.host();
            if (host === "youtube.com" || host === "www.youtube.com") {
                var videoId = null;
                if (parsedUrl.getQueryParamValue("v")) {
                    videoId = parsedUrl.getQueryParamValue("v");
                }
                if (parsedUrl.getQueryParamValue("watch")) {
                    videoId = parsedUrl.getQueryParamValue("watch");
                }
                if (videoId) {
                    var ytTitle = fetchYoutubeTitle(videoId, ytKey);
                    if (ytTitle) {
                        linkText = ytTitle;
                        notProcessed = false;
                    }
                }
            }
            if (host === "youtu.be") {
                var videoId = parsedUrl.uriParts.path;
                if (videoId[0] === "/") {
                    videoId = videoId.substr(1);
                }
                if (videoId) {
                    var ytTitle = fetchYoutubeTitle(videoId, ytKey);
                    if (ytTitle) {
                        linkText = ytTitle;
                        notProcessed = false;
                    }
                }
            }
            if (notProcessed) {
                var r = /http:\/\/store\.steampowered\.com\/app\/([0-9]+)\/.*/.exec(completeLink);
                if (r && r[1]) {
                    var id = r[1];
                    $.ajax({
                        "url": "resolve_steam",
                        "data": {"appid": id},
                        "success": function (data) {
                            if (data) {
                                linkText = "<span style=\"color: #156291;\" class=\"fa fa-steam-square\"></span> " + htmlEscape(data);
                                notProcessed = false;
                            }
                        },
                        "async": false,
                        "timeout": 100
                    });
                }
            }
            return "<a href=\"" + prefix + htmlEscape(link) + "\" target=\"_blank\" title=\"" + htmlEscape(link) + "\">" + linkText + "</a>";
        }

        var processTextPart = function(text, type, service) {
            text = htmlEscape(text);
            text = twemoji.parse(text, {
                base: "/img/",
                folder: "twemoji",
                ext: ".png",
                callback: function(icon, options, variant) {
                    switch ( icon ) {
                        case 'a9':      // � copyright
                        case 'ae':      // � registered trademark
                        case '2122':    // � trademark
                            return false;
                    }
                    return ''.concat(options.base, options.size, '/', icon, options.ext);
                }
            });
            if ((type === "MSG_EXT") && (service === "sc2tv.ru")) {
                text = text.replace(/\[\/?b\]/g, "**");
                text = text.replace(SC2TV_REGEX, function (match) {
                    var emoticon = SC2TV_EMOTE_MAP[match];
                    if (emoticon) {
                        return "<span class='faceCode' style='background-image: url(/img/sc2tv/" + emoticon.fileName +
                            "); height: " + emoticon.height + "px; width: " + emoticon.width + "px;' title='" + emoticon.code + "'></span>"
                    } else {
                        return null;
                    }
                });
            } else {
                text = text.replace(chat.emoticonRegExp, function (match) {
                    var emoticon = chat.emoticons[match];
                    if (emoticon) {
                        return "<span class='faceCode' style='background-image: url(emoticons/" + emoticon.fileName +
                            "); height: " + emoticon.height + "px; width: " + emoticon.width + "px;' title='" + emoticon.code + "'></span>"
                    } else {
                        return null;
                    }
                });
            }
            text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
            text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
            text = text.replace(/%%(.+?)%%/g, '<span class="spoiler">$1</span>');
            text = text.replace("@" + chat.self.name, function () {
                mention = true;
                return "<span class='mentionLabel'>@" + chat.self.name + "</span>"
            });
            return text;
        }

        var processMessageText = function(chat, text, type, service) {
            if ((type === "MSG_EXT") && (service === "sc2tv.ru")) {
                text = text.replace(/\[\/?url\]/g, "");
            }
            var match;
            var raw = text;
            var html = [];
            var i;
            while ((match = raw.match(/(https?:\/\/)([^\s]*)/))) {
                i = match.index;
                html.push(processTextPart(raw.substr(0, i), type, service));
                html.push(processLink(match[0], match[1], match[2]));
                raw = raw.substring(i + match[0].length);
            }
            html.push(processTextPart(raw, type, service));
            text = html.join('');
            if (text.startsWith("&gt;")) {
                text = "<span class=\"greenText\">" + text + "</span>";
            } else if (text.indexOf("!!!") === 0 && text.length > 3) {
                text = "<span class=\"nsfwLabel\">NSFW</span> <span class=\"spoiler\">" + text.substr(3) + "</span>";
            }
            return text;
        }

        var processMsg = function(chat, message, hist) {
            var room = message["room"] || chat.activeRoom;
            var type = message['type'];
            var user = message["user"];
            var text = message["text"];
            var name = message["name"];
            var mod  = message["mod"];
            var color = message["color"];
            var role = message["role"];
            var globalRole = message["globalRole"];
            var id   = message["messageId"];
            var time = message["time"];
            var currentPoll = chat.polls[room];
            var pollData = message["pollData"];

            switch (type) {
                case "CLEAR":
                case "CLEAR_EXT":
                    chat.lastChatter(room, null);
                    if ((chat.self.role >= globalLevels.MOD) || (chat.localRole[room] >= levels.MOD)) {
                        chat.addMessage(new Message(type, $translate.instant("CHAT_CLEAR_USER", {"mod": mod, "user": name})));
                    }
                    chat.messagesUpdated();
                    chat.hideMessagesFromUser(room, name);
                    break;
                case "CLEAR_ROOM":
                    chat.lastChatter(room, null);
                    if (!((chat.self.role >= globalLevels.MOD) || (chat.localRole[room] >= levels.MOD))) {
                        chat.messages[room].length = 0;
                    }
                    chat.messages[room].push(new Message("INFO", $translate.instant("CHAT_CLEAR")));
                    chat.messagesUpdated();
                    break;
                case "HIST":
                    angular.forEach(message["history"], function(e) {
                        processMsg(chat, e, true);
                    });
                    chat.messagesUpdated();
                    break;
                case "ME":
                case "MSG":
                case "MSG_EXT":
                {
                    /* === OUT ===
                     * |    0     |    1     |    2     |    3     |    4     |    5     |    6     |
                     * |  ROOM    |   NAME   |   ROLE   |  COLOR   |  MSG_ID  |   TIME   |   TEXT   |
                     */

                    var unprocessedText = text;
                    var service = type === "MSG_EXT" ? message["service"] : null;
                    var serviceRes = service ? message["serviceResource"] : null;
                    var lastChatter = chat.lastChatterInRoom[room];
                    user = new User(name, color, levels[role], globalLevels[globalRole], service, serviceRes);

                    var previousMessage = chat.lastMessage[room];
                    var mention = false;
                    var showModButtons = (user.role !== levels.ADMIN)
                        && (chat.self && (chat.self.name !== user.name) && (type != "ME")) &&
                        (((chat.localRole[room] >= levels.MOD) && (chat.localRole[room] > user.role) && (chat.self.role >= user.globalRole))
                        || ((chat.self.role >= globalLevels.MOD) && (chat.self.role > user.globalRole)));
                    var ignored = settings.getIgnored().indexOf(user.name.toLowerCase()) != -1;
                    var showIgnored = settings.getS("showIgnored");
                    var hideExt = settings.getS("hideExt");
                    var hidden = ignored && showIgnored;
                    var omit = (ignored && !showIgnored) ||
                        ((type === "MSG_EXT") && ((service === "sc2tv.ru") || (service === "cybergame.tv")) && hideExt);
                    var stackWithPrevious =
                        (previousMessage.type === "MSG_GROUP") && ((type === "MSG") || (type === "MSG_EXT")) &&
                        (lastChatter &&
                            (lastChatter.name.toLowerCase() === user.name.toLowerCase()) &&
                            (lastChatter.service === user.service)
                        ) &&
                        (previousMessage.messages.length < 5);

                    //BODY
                    text = processMessageText(chat, text, type, service);

                    if (!omit) {
                        chat.lastChatter(room, user);
                        var elem = null;
                        if (stackWithPrevious) {
                            previousMessage.messages.push(new GroupedMessage(text, id, time, hidden));
                            chat.messageUpdated(previousMessage);
                        } else {
                            if (type === "MSG" || type === "MSG_EXT") {
                                elem = new MessageGroup(user, showModButtons, hist);
                                elem.messages.push(new GroupedMessage(text, id, time, hidden));
                            } else {
                                elem = new Message(type, text, user, showModButtons, id, time, hist, hidden)
                            }
                        }
                        if (elem != null) {
                            chat.addMessage(elem, room, hist, mention);
                            if (!hist) {
                                chat.messagesUpdated();
                            }
                        }
                        if (mention && !hist) {
                            notificationService.notify(user.name, unprocessedText);
                        }
                    }
                    break;
                }
                case 'COLOR':
                {
                    chat.lastChatter(room, null);
                    text = "<span style=\"opacity: 0.7\">Your color is now </span> <span style=\"color:" + color + "\">" + color + "</span>";
                    chat.addMessage(new Message(type, text), room);
                    chat.messagesUpdated();
                    chat.self.color = color;
                    break;
                }
                case 'INFO':
                    chat.lastChatter(room, null);
                    chat.addMessage(new Message(type, text), room);
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
                            _id: function () {
                                return text;
                            },
                            isUser: chat.self.role === levels.USER
                        }
                    });
                    break;
                case 'SELF_JOIN':
                    chat.lastChatter(room, null);
                    chat.localRole[room] = levels[message["chatter"]["role"]];
                    if (chat.self.role === globalLevels.UNAUTHENTICATED) {
                        chat.addMessage(new Message(type, $translate.instant("CHAT_HELLO_UNAUTHENTICATED", {"room": room})), room);
                    } else {
                        chat.addMessage(new Message(type, $translate.instant("CHAT_HELLO",
                            {
                                "color": chat.self.color,
                                "name": chat.self.name,
                                "role": $translate.instant("ROLE_" + message["chatter"]["role"]),
                                "globalRole": $translate.instant("ROLE_" + chat.self.role.title),
                                "room": room
                            }
                        )), room);
                    }
                    chat.addRoom(room);
                    chat.messagesUpdated();
                    break;
                case 'AUTH_COMPLETE':
                    chat.self = user;
                    chat.self.role = globalLevels[chat.self.role];
                    chat.state = CHAT_STATE.AUTHENTICATED;
                    chat.stateUpdatedCallback();
                    break;
                case 'TIMEOUT':
                    chat.lastChatter(room, null);
                    chat.addMessage(new Message(type, $translate.instant("CHAT_TIMEOUT_USER", {"mod": mod, "user": name})), room);
                    chat.hideMessagesFromUser(room, name);
                    if (!hist) {
                        chat.messagesUpdated();
                    }
                    break;
                case 'BAN':
                    chat.lastChatter(room, null);
                    chat.addMessage(new Message(type, $translate.instant("CHAT_BAN_USER", {"mod": mod, "user": name})), room);
                    chat.hideMessagesFromUser(room, name);
                    if (!hist) {
                        chat.messagesUpdated();
                    }
                    break;
                case 'LOGIN':
                    chat.lastChatter(room, null);
                    chat.self = user;
                    chat.self.role = globalLevels[chat.self.role];
                    chat.addMessage(new Message(type, $translate.instant("CHAT_LOGIN_SUCCESS",
                        {
                            "name": chat.self.name,
                            "role": $translate.instant("ROLE_" + chat.self.role.title)
                        }
                    )), room);
                    chat.messagesUpdated();
                    break;
                case 'ROLE':
                    chat.lastChatter(room, null);
                    if (role) {
                        chat.self.role = globalLevels[role];
                        chat.addMessage(new Message(type, '<span style="opacity: 0.7">You are <strong>' + role + '</strong></span>'), room);
                    }
                    chat.messagesUpdated();
                    break;
                case 'ERROR':
                    chat.lastChatter(room, null);
                    chat.addMessage(new Message(type, $translate.instant("ERROR_"+text,
                        {
                            "args": message["errorData"]
                        }
                    )), room);
                    chat.messagesUpdated();
                    break;
                case "LIKE":
                    var msg = null;
                    var group = null;
                    angular.forEach(chat.messages[room], function(m) {
                        if (m.type === "MSG_GROUP") {
                            angular.forEach(m.messages, function(mm) {
                                if (mm.id_ === id) {
                                    msg = mm;
                                    group = m;
                                }
                            });
                        } else {
                            if (m.id_ === id) {
                                msg = m;
                            }
                        }
                    });
                    if (msg && msg.likes.indexOf(name) == -1) {
                        msg.likes.push(name);
                        if (group) {
                            chat.messageUpdated(group);
                        } else {
                            chat.messageUpdated(msg);
                        }
                    }
                    break;
                case "POLL":
                    chat.polls[room] = pollData;
                    pollData.voted = false;
                    pollData.open = true;
                    pollData.maxPollVotes = Math.max.apply(null, pollData.votes);
                    chat.pollsUpdatedCallback();
                    break;
                case "POLL_UPDATE":
                    if (currentPoll.poll.id === pollData.poll.id) {
                        currentPoll.votes = pollData.votes;
                        currentPoll.maxPollVotes = Math.max.apply(null, pollData.votes);
                        chat.pollsUpdatedCallback();
                    }
                    break;
                case "POLL_VOTED":
                    currentPoll.voted = true;
                    chat.pollsUpdatedCallback();
                    break;
                case "POLL_END":
                    if (currentPoll.poll.id === pollData.poll.id) {
                        currentPoll.votes = pollData.votes;
                        currentPoll.maxPollVotes = Math.max.apply(null, pollData.votes);
                        currentPoll.open = false;
                        chat.pollsUpdatedCallback();
                        $timeout(function() {
                            var poll = chat.polls[room];
                            if (poll.poll.id === pollData.poll.id) {
                                chat.polls[room] = null;
                            }
                        }, 60*1000);
                    }
                    break;
                default:
                    console.log(message);
                    break;
            }
        };
    };

    chatService.prototype.sendMessage = function(msg) {
        this.ws.send(JSON.stringify(msg));
    };

    return new chatService();
}]);

services.service("chatSettings", [function() {
    var settings = function() {
        $.cookie.json = true;
    };

    settings.prototype.setS = function (name, value) {
        $.cookie(name, value, {expires : 365});
    };

    settings.prototype.getS = function(name) {
        return $.cookie(name);
    };

    settings.prototype.getIgnored = function() {
        var s = this.getS("ignored");
        var result;
        if (s) {
            result =  $.parseJSON(s);
        } else {
            result = [];
        }
        return result;
    };

    settings.prototype.addIgnored = function(name) {
        var ignored = this.getIgnored();
        if (ignored) {
            ignored.push(name);
        } else {
            ignored = [name];
        }
        this.setS("ignored", JSON.stringify(ignored));
    };

    settings.prototype.deleteIgnored = function(name) {
        var ignored = this.getIgnored();
        if (ignored) {
            ignored.remove(name);
        }
        this.setS("ignored", JSON.stringify(ignored));
    };

    settings.prototype.getRooms = function() {
        var s = this.getS("rooms");
        var result;
        if (s) {
            result =  $.parseJSON(s);
        } else {
            result = ["#main"];
        }
        return result;
    };

    settings.prototype.addRoom = function(name) {
        var rooms = this.getRooms();
        if (rooms) {
            rooms.push(name);
        }
        this.setS("rooms", JSON.stringify(rooms));
    };

    settings.prototype.deleteRoom = function(name) {
        var rooms = this.getRooms();
        if (rooms) {
            rooms.remove(name);
        }
        this.setS("rooms", JSON.stringify(rooms));
    };

    return new settings();
}]);

services.service("windowStateService", function() {
    var WindowStateService = function() {
        this.active = true;
        var ref = this;
        if (window === window.top) {
            $(window).focus(function () {
                ref.active = true;
                console.log("active");
            });
            $(window).blur(function () {
                ref.active = false;
                console.log("blur");
            });
        }
    };
    WindowStateService.prototype.isActive = function() {
        return this.active;
    };
    return new WindowStateService();
});

services.service("notificationService", ["chatSettings", "windowStateService", function(settings, windowState) {
    var NotificationService = function() {

    };

    NotificationService.prototype.isAvailable = function() {
        return window.Notification ? true : false;
    };

    NotificationService.prototype.hasPermission = function() {
        return Notification.permission == "granted";
    };

    NotificationService.prototype.requestPermissionAndEnable = function() {
        Notification.requestPermission(function(result) {
            console.log(result);
            if (result == "granted") {
                settings.setS("notifications", true);
            }
        });
    };

    NotificationService.prototype.notify = function(title, body) {
        if (this.isAvailable() && this.hasPermission() && !windowState.isActive()) {
            new Notification(title, {"body": body});
        }
    };

    return new NotificationService();
}]);

services.directive("faCheckbox", function() {
    return {
        restrict: "E",
        scope: {
            model: "=",
            label: "@"
        },
        template:
            "<span class=\"checkboxWrapper\" ng-click=\"toggle()\" style=\"cursor: pointer\">" +
            "<span class=\"btn-link btn-checkbox fa pull-left\" ng-class=\"{'fa-square-o': !model, 'fa-check-square-o': model}\"></span> {{label}}" +
            "</span>",
        link: function(scope, element, attrs) {
            scope.model = scope.model || false;

            scope.toggle = function() {
                scope.model = !scope.model;
            }
        }
    }
});
