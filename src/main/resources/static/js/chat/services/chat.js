var module = angular.module("chat.services.chat", ["chat.messageProcessing", "chat.services.settings", "chat.services.notifications"]);

module.service("chatService",
["$modal", "chatSettings", "$translate", "$http", "$timeout", "notificationService", "messageProcessingService",
function($modal, settings, $translate, $http, $timeout, notificationService, msgs) {
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
        this.limit = document.IS_MOBILE ? 20 : 50;
        this.proxies = {};

        var c = this;

        $http({method: 'GET', url: '/rest/emoticons/all'})
            .success(function(d, status, headers, config) {
                if (status == 200) {
                    var data = d["records"];
                    if (data) {
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
                }
            })
            .error(function() {
                console.log("failed to load emoticons");
            });
    };

    chatService.prototype.isProxyModerationEnabled = function(room, providerName, remoteRoom) {
        var proxies = this.proxies[room];
        var result = false;
        if (proxies && proxies.length) {
            $.each(proxies, function(i, e) {
                if ((e.providerName === providerName) && (e.remoteRoom === remoteRoom)) {
                    result = e.moderationEnabled;
                }
            });
        }
        return result;
    };

    chatService.prototype.isProxyOutboundEnabled = function(room, providerName, remoteRoom) {
        var proxies = this.proxies[room];
        var result = false;
        if (proxies && proxies.length) {
            $.each(proxies, function(i, e) {
                if ((e.providerName === providerName) && (e.remoteRoom === remoteRoom)) {
                    result = e.outboundEnabled;
                }
            });
        }
        return result;
    };


    /**
     * @param {User} user
     */
    chatService.prototype.clear = function(user) {
        this.sendMessage({
            "type": "CLEAR",
            "room": this.activeRoom,
            "name": user.name
        })
    };

    /**
     * @param {User} user
     */
    chatService.prototype.timeout = function(user) {
        this.sendMessage({
            "type": "TIMEOUT",
            "room": this.activeRoom,
            "name": user.name
        })
    };

    /**
     * @param {User} user
     */
    chatService.prototype.unban = function(user) {
        this.sendMessage({
            "type": "UNBAN",
            "room": this.activeRoom,
            "name": user.name
        })
    };

    /**
     * @param {User} user
     */
    chatService.prototype.ban = function(user) {
        this.sendMessage({
            "type": "BAN",
            "room": this.activeRoom,
            "name": user.name
        })
    };

    /**
     * @param {Level} role
     * @param {String} roomName
     * @returns {boolean}
     */
    chatService.prototype.hasLocalRole = function(role, roomName) {
        return this.localRole[roomName] >= role;
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

    chatService.prototype.hideMessagesFromUser = function(room, name, service, serviceRes) {
        var chat = this;
        angular.forEach(this.messages[room], function(message) {
            if (!message.hidden && message.user && message.user.name == name) {
                if (service && (message.user.service === service) && (message.user.serviceRes === serviceRes) || !service) {
                    if (message.type === "MSG_GROUP") {
                        angular.forEach(message.messages, function (msg) {
                            msg.hidden = true;
                        });
                    } else {
                        message.hidden = true;
                    }
                    chat.messageUpdated(message);
                }
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

            if (a.length > this.limit) {
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
                chat.sendMessage({"type": "SESSION", "text": read_cookie("sid")});
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
                    msgs.processMessage(chat, message, false);
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
    };

    chatService.prototype.sendMessage = function(msg) {
        this.ws.send(JSON.stringify(msg));
    };

    return new chatService();
}]);
