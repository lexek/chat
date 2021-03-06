var langModule = angular.module("chat.lang", ["pascalprecht.translate", "chat.services.settings"]);

langModule.factory('$translateChatStorage', ["chatSettings", function $translateCookieStorageFactory($settings) {
    var $translateChatStorage = {
        get: function (name) {
            return $settings.getS(name);
        },

        set: function (name, value) {
            $settings.setS(name, value);
        },

        put: function (name, value) {
            $settings.setS(name, value);
        }
    };

    return $translateChatStorage;
}]);

langModule.config(['$translateProvider', function ($translateProvider) {
    $translateProvider.translations("en", {
        "CONTROLS_EMOTICONS": "emoticons",
        "CONTROLS_ICON": "icon",
        "CONTROLS_CODE": "code",
        "CONTROLS_OK": "Ok",
        "CONTROLS_CLOSE": "Close",
        "CONTROLS_CHANGE_NAME": "Change name",
        "CONTROLS_USERNAME": "Username",
        "CONTROLS_SET_EMAIL": "Set email",
        "CONTROLS_CHANGE_PASSWORD": "Change password",
        "CONTROLS_AUTHENTICATION_REQUIRED": "Authentication required",
        "CONTROLS_CAPTCHA_REQUIRED": "Captcha required",
        "CONTROLS_USE_TWITCH": "Use twitch.tv account",
        "CONTROLS_SIGN_IN": "Sign in",
        "CONTROLS_SIGN_UP": "Register",
        "CONTROLS_MENU_SIGN_IN_WITH": "with",
        "CONTROLS_PASSWORD": "Password required",

        "CONTROLS_MENU_ADMIN_PANEL": "admin panel",
        "CONTROLS_MENU_COLOR": "color",
        "CONTROLS_MENU_LOG_OUT": "log out",
        "CONTROLS_MENU_TWITCH_AUTH": "twitch.tv auth",
        "CONTROLS_MENU_SIGN_IN": "sign in",
        "CONTROLS_MENU_SIGN_UP": "register",
        "CONTROLS_MENU_EMOTICONS": "emoticons",
        "CONTROLS_MENU_POPOUT": "popout",
        "CONTROLS_MENU_SETTINGS": "Settings",
        "CONTROLS_MENU_HIDE_MOD_BUTTONS": "hide mod buttons",
        "CONTROLS_MENU_LEGACY_MODE": "legacy mode",
        "CONTROLS_MENU_SHOW_TIMESTAMPS": "show timestamps",
        "CONTROLS_MENU_SHOW_IGNORED": "show ignored as hidden",
        "CONTROLS_MENU_DARK": "dark theme",
        "CONTROLS_MENU_LANGUAGE": "Language",
        "CONTROLS_MENU_TICKETS": "tickets",
        "CONTROLS_MENU_HELP": "help",
        "CONTROLS_MENU_NOTIFICATIONS": "notifications",
        "CONTROLS_MENU_HIDE_EXT": "hide ext messages",

        "PROFILE_EMAIL_SETTINGS": "Email settings",
        "PROFILE_EMAIL_RESEND": "Resend verification email",
        "PROFILE_EMAIL_EMPTY": "not set",
        "PROFILE_NAME": "Name",
        "PROFILE_ROLE": "Role",
        "PROFILE_COLOR": "Color",
        "PROFILE_CHECK_IP": "Only allow sessions from a single IP address",

        "CHAT_HELLO_UNCONFIRMED": "You must verify your email before you can post messages often",
        "CHAT_HELLO_UNAUTHENTICATED": "Hello, you joined {{room}} and you are unauthenticated.",
        "CHAT_HELLO": "Hello, <span style=\"color: {{color}}\">{{name}}</span>! You joined {{room}} and you are {{role}}",
        "CHAT_LOGIN_SUCCESS": "You successfully logged in as {{name}} (<strong>{{role}}</strong>)",
        "CHAT_CONNECTING": "connecting",
        "CHAT_LOST_CONNECTION": "lost connection",
        "CHAT_MESSAGE_HIDDEN": "message deleted",
        "CHAT_LIKES": "Likes",
        "CHAT_NO_LIKES": "No likes yet",
        "CHAT_AND_X_OTHERS": "And {{count}} others",
        "CHAT_CLEAR": "Chat was cleared by moderator",
        "CHAT_CLEAR_USER": "{{mod}} cleared messages by {{user}}",
        "CHAT_TIMEOUT_USER": "{{user}} timed out by {{mod}}",
        "CHAT_BAN_USER": "{{user}} banned by {{mod}}",
        "CHAT_RECONNECT_CD": "reconnecting in",
        "CHAT_AUTHENTICATING": "waiting for authentication",
        "SUBSCRIBED": "subscribed for {{months}} months in a row",

        "USERS_MODS": "mods",
        "USERS_USERS": "users",

        "ROLE_UNAUTHENTICATED": "unauthenticated",
        "ROLE_GUEST": "guest",
        "ROLE_USER_UNCONFIRMED": "unconfirmed user",
        "ROLE_USER": "user",
        "ROLE_SUPPORTER": "supporter",
        "ROLE_MOD": "moderator",
        "ROLE_ADMIN": "administrator",
        "ROLE_SUPERADMIN": "administrator",

        "AUTH_SIGN_IN": "Sign in",
        "AUTH_NEW_ACCOUNT": "Create new account",
        "AUTH_USERNAME": "Username",
        "AUTH_NAME_OR_EMAIL": "Username or email",
        "AUTH_PASSWORD": "Password",
        "AUTH_OLD_PASSWORD": "Current password",
        "AUTH_CAPTCHA": "Captcha",
        "AUTH_WITH_TWITCH": "with twitch",
        "AUTH_EMAIL": "Email",
        "AUTH_NAME_FORMAT": "Name length must be between 3 and 16 symbols, must include only latin lower case " +
            "letters or '_' symbol and start with letter.",
        "AUTH_PASSWORD_FORMAT": "Password length must be between 6 and 30 symbols.",
        "AUTH_USERNAME_CHECK_PENDING": "Checking if this name is available...",
        "AUTH_USERNAME_NOT_AVAILABLE": "This username is not available.",
        "AUTH_SOCIAL": "Social sign in",
        "AUTH_CONNECT": "connect",
        "AUTH_DISCONNECT": "disconnect",
        "AUTH_METHODS": "Sign in methods",
        "AUTH_NEW_PASSWORD": "Add password",
        "AUTH_CHANGE_PASSWORD": "Change password",
        "AUTH_TOKEN": "API token",
        "AUTH_GET_TOKEN": "Press button to get token",
        "AUTH_FORGOT_PASSWORD": "Forgot password?",
        "AUTH_RESTORE_PASSWORD": "Restore password",

        "HELP_ABOUT": "About this chat",
        "HELP_ABOUT_TEXT": "Our github: <a href='https://github.com/lexek/chat' target='_blank'><i class='fa fa-fw fa-github'/>lexek/chat</a>",
        "HELP_MARKUP": "Markup",
        "HELP_BOLD_TEXT": "bold text",
        "HELP_ITALIC_TEXT": "italic text",
        "HELP_STRIKETHROUGH_TEXT": "strikethrough text",
        "HELP_SPOILER_TEXT": "spoiler text",
        "HELP_NSFW_TEXT": "NSFW text",
        "HELP_QUOTE_TEXT": "quote text",
        "HELP_MENTION": "username",
        "HELP_MENTION_NOTE": "It will appear this way only to person you mentioned.",
        "HELP_PREFIX_REQUIRED": "must be in begining of message.",
        "HELP_RENAME": "How do i change name?",
        "HELP_RENAME_TEXT1": "If you are allowed to change name type in chat",
        "HELP_RENAME_TEXT2": "Otherwise create a ticket with change name request",
        "HELP_RENAME_FORMAT": "What's the required format for user name?",
        "HELP_RENAME_FORMAT_TEXT": "Name length must be between 3 and 16 symbols, must include only latin lower case " +
            "letters or '_' symbol and start with letter.",
        "HELP_UNBAN": "I was banned, how do i get unbanned?",
        "HELP_UNBAN_TEXT": "You can contact one of moderators directly or submit a ticket with unban request.",
        "HELP_BROKEN": "Chat is broken.",
        "HELP_BROKEN_TEXT": "Try refreshing page and clearing browser cache. If it didnt help, submit a ticket.",
        "HELP_BUG": "I found a bug, what to do?",
        "HELP_BUG_TEXT1": "Submit a ticket with bug description. If you want to provide an image, use",
        "HELP_BUG_TEXT2": "image service to host it.",
        "HELP_IGNORE": "How to ignore someone?",
        "HELP_IGNORE_TEXT":
            "To ignore someone, use <code>/ignore <strong>name</strong></code> command.<br/>" +
            "To see messages from ignored users as hidden messages, check \"show ignored as hidden\" option in settings.",

        "TICKETS_MINE": "My tickets",
        "TICKETS_TYPE": "type",
        "TICKETS_MESSAGE": "message",
        "TICKETS_RESPONSE": "response",
        "TICKETS_NONE": "Nothing to show.",
        "TICKETS_TYPE_EMOTICON": "I want to request emoticon",
        "TICKETS_TYPE_BAN": "I want to be unbanned",
        "TICKETS_TYPE_RENAME": "I want to change name",
        "TICKETS_TYPE_BUG": "I found bug",
        "TICKETS_TYPE_OTHER": "Other",
        "TICKETS_TEXT": "text",
        "TICKETS_ADMIN_REPLY": "admin reply",
        "TICKETS_COMPOSE": "Compose ticket",

        "POLL_OPEN_POLL": "Open poll",
        "POLL_VOTE": "vote",
        "POLL_SKIP_VOTE": "skip vote",
        "POLL_ANNOUNCEMENT": "Poll started: {{question}}",

        "IGNORE_OK": "You have ignored <b>{{name}}</b>",
        "UNIGNORE_OK": "You have removed <b>{{name}}</b> from your ignore list",
        "IGNORE_LIST": "You are ignoring: {{names}}",

        "ERROR_BAN": "You are banned",
        "ERROR_BAN_DENIED": "You can't ban this user",
        "ERROR_CLEAR_DENIED": "You can't clear this user messages",
        "ERROR_MESSAGE_TOO_BIG": "Your message is too big",
        "ERROR_NAME_BAD_FORMAT": "You must follow given format for name",
        "ERROR_NAME_DENIED": "You can't change name",
        "ERROR_NAME_TAKEN": "This name is already taken",
        "ERROR_NOT_AUTHORIZED": "You are not authorized to do this",
        "ERROR_ROOM_ALREADY_JOINED": "You have already joined this room",
        "ERROR_ROOM_NOT_FOUND": "Unknown room",
        "ERROR_TIMEOUT": "You are timed out for {{args[0]}} seconds",
        "ERROR_TOO_FAST": "You are sending messages too fast",
        "ERROR_UNKNOWN_COMMAND": "Unknown command",
        "ERROR_UNKNOWN_ROLE": "Unknown role",
        "ERROR_UNKNOWN_USER": "Unknown user",
        "ERROR_UNVERIFIED_EMAIL": "You must verify your email before you can send any messages",
        "ERROR_WRONG_COLOR": "You can't use this color",
        "ERROR_ALREADY_IGNORED": "That user is already in ignore list",
        "ERROR_INTERNAL_ERROR": "Internal server error",
        "ERROR_EMAIL_NULL": "Email shoudn't be empty",
        "ERROR_EMAIL_SAME": "You can't change email to the same one",
        "ERROR_EMAIL_TOO_OFTEN": "You're changing email too often, try again in 30 minutes",
        "ERROR_WRONG_CAPTCHA": "Captcha is incorrect.",
        "ERROR_BAD_REGISTRATION_FORMAT": "Incorrect username, email or password format.",
        "ERROR_REGISTRATION_DENIED": "You can't register new accounts.",
        "ERROR_DUPLICATE_EMAIL_OR_USERNAME": "Provided username or email is already in use.",

        "REGISTRATION_SUCCESS": "You have successfuly registered. An email with information how to confirm your " +
        "account has been sent to address you entered.",

        "TWITTER_RETWEETED": "retweeted by",
        "MESSAGE_DONATED":
            "<i class='fa fa-fw fa-money'></i> <strong>{{name}} donated {{amount | number:2}} {{currency}}</strong>",
        "MESSAGE_DONATED_MESSAGE":
            "<i class='fa fa-fw fa-money'></i> <strong>{{name}} donated {{amount | number:2}} {{currency}}</strong>: {{text}}"
    });

    $translateProvider.translations("ru", {
        "CONTROLS_EMOTICONS": "смайлы",
        "CONTROLS_ICON": "иконка",
        "CONTROLS_CODE": "код",
        "CONTROLS_OK": "Ок",
        "CONTROLS_CLOSE": "Закрыть",
        "CONTROLS_CHANGE_NAME": "Поменять имя",
        "CONTROLS_USERNAME": "Имя",
        "CONTROLS_SET_EMAIL": "Изменить email",
        "CONTROLS_CHANGE_PASSWORD": "Поменять пароль",
        "CONTROLS_AUTHENTICATION_REQUIRED": "Необходима аутентификация",
        "CONTROLS_CAPTCHA__REQUIRED": "Необходимо ввести капчу",
        "CONTROLS_USE_TWITCH": "Вход через twitch.tv",
        "CONTROLS_SIGN_IN": "Войти",
        "CONTROLS_SIGN_UP": "Зарегистрироваться",
        "CONTROLS_MENU_SIGN_IN_WITH": "через",
        "CONTROLS_PASSWORD": "Введите пароль",

        "CONTROLS_MENU_ADMIN_PANEL": "админка",
        "CONTROLS_MENU_COLOR": "цвет",
        "CONTROLS_MENU_LOG_OUT": "выйти",
        "CONTROLS_MENU_TWITCH_AUTH": "вход через twitch.tv",
        "CONTROLS_MENU_SIGN_IN": "войти",
        "CONTROLS_MENU_SIGN_UP": "зарегистрироваться",
        "CONTROLS_MENU_EMOTICONS": "смайлы",
        "CONTROLS_MENU_POPOUT": "всплывающее окно",
        "CONTROLS_MENU_SETTINGS": "Настройки",
        "CONTROLS_MENU_HIDE_MOD_BUTTONS": "скрыть кнопки модерации",
        "CONTROLS_MENU_LEGACY_MODE": "legacy mode",
        "CONTROLS_MENU_SHOW_TIMESTAMPS": "временные отметки",
        "CONTROLS_MENU_SHOW_IGNORED": "скрыть игнорируемых",
        "CONTROLS_MENU_DARK": "тёмная тема",
        "CONTROLS_MENU_LANGUAGE": "Язык",
        "CONTROLS_MENU_TICKETS": "тикеты",
        "CONTROLS_MENU_HELP": "помощь",
        "CONTROLS_MENU_NOTIFICATIONS": "оповещения",
        "CONTROLS_MENU_HIDE_EXT": "не показывать внешние сообщения",

        "CHAT_HELLO_UNCONFIRMED": "Ты должен подтвердить email чтобы чаще отправлять сообщения",
        "CHAT_HELLO_UNAUTHENTICATED": "Привет, ты вошёл в {{room}} и ты гость.",
        "CHAT_HELLO": "Привет, <span style=\"color: {{color}}\">{{name}}</span>! Ты вошёл в {{room}} и ты {{role}}",
        "CHAT_LOGIN_SUCCESS": "Ты успешно вошёл как {{name}} (<strong>{{role}}</strong>)",
        "CHAT_CONNECTING": "соединение",
        "CHAT_LOST_CONNECTION": "потеряно соединение",
        "CHAT_MESSAGE_HIDDEN": "сообщение удалено",
        "CHAT_LIKES": "Лайки",
        "CHAT_NO_LIKES": "Ещё нет лайков",
        "CHAT_AND_X_OTHERS": "И {{count}} других",
        "CHAT_CLEAR": "Чат был очищен модератором",
        "CHAT_CLEAR_USER": "{{mod}} стёр сообщения от {{user}}",
        "CHAT_TIMEOUT_USER": "{{mod}} дал таймаут {{user}}",
        "CHAT_BAN_USER": "{{mod}} забанил {{user}}",
        "CHAT_RECONNECT_CD": "переподключение через",
        "CHAT_AUTHENTICATING": "ожидание аутентификации",
        "SUBSCRIBED": "подписан на {{months}} месяцев подряд",

        "PROFILE_EMAIL_SETTINGS": "Настройки email",
        "PROFILE_EMAIL_RESEND": "Выслать подтверждение",
        "PROFILE_EMAIL_EMPTY": "не задан",
        "PROFILE_NAME": "Имя",
        "PROFILE_ROLE": "Роль",
        "PROFILE_COLOR": "Цвет",
        "PROFILE_CHECK_IP": "Сессия только с одного IP-адреса",

        "USERS_MODS": "модераторы",
        "USERS_USERS": "пользователи",

        "ROLE_UNAUTHENTICATED": "неавторизованый",
        "ROLE_GUEST": "гость",
        "ROLE_USER_UNCONFIRMED": "неподтверждённый пользователь",
        "ROLE_USER": "пользователь",
        "ROLE_SUPPORTER": "supporter",
        "ROLE_MOD": "модератор",
        "ROLE_ADMIN": "администратор",
        "ROLE_SUPERADMIN": "администратор",

        "AUTH_SIGN_IN": "Войти",
        "AUTH_NEW_ACCOUNT": "Создать аккаунт",
        "AUTH_USERNAME": "Имя",
        "AUTH_NAME_OR_EMAIL": "Имя или email",
        "AUTH_PASSWORD": "Пароль",
        "AUTH_OLD_PASSWORD": "Текущий пароль",
        "AUTH_CAPTCHA": "Капча",
        "AUTH_WITH_TWITCH": "через твич",
        "AUTH_EMAIL": "Email",
        "AUTH_NAME_FORMAT": "Длина должна быть от 3 до 16 символов, состоять из строчных латинских букв или " +
            "символа '_' и начинаться с буквы.",
        "AUTH_PASSWORD_FORMAT": "Длина пароля должны быть от 6 до 30 символов.",
        "AUTH_USERNAME_CHECK_PENDING": "Проверка доступности имени...",
        "AUTH_USERNAME_NOT_AVAILABLE": "Это имя уже занято.",
        "AUTH_SOCIAL": "Социальные сети",
        "AUTH_CONNECT": "подключить",
        "AUTH_DISCONNECT": "отключить",
        "AUTH_METHODS": "Методы входа",
        "AUTH_NEW_PASSWORD": "Добавить пароль",
        "AUTH_CHANGE_PASSWORD": "Изменить пароль",
        "AUTH_TOKEN": "API токен",
        "AUTH_GET_TOKEN": "Нажми кнопку для получения токена",
        "AUTH_FORGOT_PASSWORD": "Забыли пароль?",
        "AUTH_RESTORE_PASSWORD": "Восстановление пароля",

        "HELP_ABOUT": "О чате",
        "HELP_ABOUT_TEXT": "Наш гитхаб: <a href='https://github.com/lexek/chat' target='_blank'><i class='fa fa-fw fa-github'/>lexek/chat</a>",
        "HELP_MARKUP": "Разметка",
        "HELP_BOLD_TEXT": "полужирный текст",
        "HELP_ITALIC_TEXT": "полужирный курсив",
        "HELP_STRIKETHROUGH_TEXT": "перечёркнутый текст",
        "HELP_SPOILER_TEXT": "текст под спойлером",
        "HELP_NSFW_TEXT": "NSFW текст",
        "HELP_QUOTE_TEXT": "цитата",
        "HELP_MENTION": "имя_пользователя",
        "HELP_MENTION_NOTE": "Это увидит пользователь, которого вы упомянули.",
        "HELP_PREFIX_REQUIRED": "должно быть в начале сообщения.",
        "HELP_RENAME": "Как поменять имя?",
        "HELP_RENAME_TEXT1": "Если вам разрешено менять имя, используйте команду",
        "HELP_RENAME_TEXT2": "Если нет, создайте тикет с просьбой о разрешении смены имени.",
        "HELP_RENAME_FORMAT": "Какой необходимый формат имени?",
        "HELP_RENAME_FORMAT_TEXT": "Длина должна быть от 3 до 16 символов, состоять из строчных латинских букв или " +
            "символа '_' и начинаться с буквы.",
        "HELP_UNBAN": "Меня забанили, как снять бан?",
        "HELP_UNBAN_TEXT": "Вы можете связаться с одним из модераторов напрямую или создать тикет с просьбой разбана.",
        "HELP_BROKEN": "Всё сломалось.",
        "HELP_BROKEN_TEXT": "Попробуйте обновить страницу и очистить кэш браузера. Если это не помогло, создайте тикет.",
        "HELP_BUG": "Я нашёл баг, что делать?",
        "HELP_BUG_TEXT1": "Создайте тикет с описанием этого бага. Если вы хотите добавить изображение, используйте",
        "HELP_BUG_TEXT2": "для хранения этого изображения.",
        "HELP_IGNORE": "Как добавить пользователя в игнор?",
        "HELP_IGNORE_TEXT":
            "Чтобы добавить пользователя в игнор, воспользуйся командой <code>/ignore <strong>name</strong></code>.<br/>" +
            "Если ты хочешь видеть сообщения от игнорируемых людей как скрытые, отметь \"скрыть игнорируемых\" в настройках.",

        "TICKETS_MINE": "Мои тикеты",
        "TICKETS_TYPE": "тип",
        "TICKETS_MESSAGE": "сообщение",
        "TICKETS_RESPONSE": "ответ",
        "TICKETS_NONE": "У вас нет тикетов.",
        "TICKETS_TYPE_EMOTICON": "Я хочу новый эмотикон",
        "TICKETS_TYPE_BAN": "Я хочу быть разбаненым",
        "TICKETS_TYPE_RENAME": "Я хочу поменять имя",
        "TICKETS_TYPE_BUG": "Я нашёл баг",
        "TICKETS_TYPE_OTHER": "Другое",
        "TICKETS_TEXT": "текст",
        "TICKETS_ADMIN_REPLY": "ответ администратора",
        "TICKETS_COMPOSE": "Создать тикет",

        "POLL_OPEN_POLL": "Текущий опрос",
        "POLL_VOTE": "проголосовать",
        "POLL_SKIP_VOTE": "не голосовать",
        "POLL_ANNOUNCEMENT": "Начался опрос: {{question}}",

        "IGNORE_OK": "<b>{{name}}</b> добавлен игнор",
        "UNIGNORE_OK": "<b>{{name}}</b> удалён из игнора",
        "IGNORE_LIST": "Ты игнорируешь: {{names}}",

        "ERROR_BAN": "Вы забанены",
        "ERROR_BAN_DENIED": "Вы не можете забанить этого пользователя",
        "ERROR_CLEAR_DENIED": "Вы не можете стереть сообщения этого пользователя",
        "ERROR_MESSAGE_TOO_BIG": "Ваше сообщение слишком большое",
        "ERROR_NAME_BAD_FORMAT": "Вы должны соблюдать заданый формат имени",
        "ERROR_NAME_DENIED": "Вы не можете поменять имя",
        "ERROR_NAME_TAKEN": "Это имя уже занято",
        "ERROR_NOT_AUTHORIZED": "Вы не можете это сделать",
        "ERROR_ROOM_ALREADY_JOINED": "Вы уже вошли в эту комнату",
        "ERROR_ROOM_NOT_FOUND": "Неизвестная комната",
        "ERROR_TIMEOUT": "Вы в таймауте на {{args[0]}} секунд",
        "ERROR_TOO_FAST": "Вы слишком быстро отправляете сообщения",
        "ERROR_UNKNOWN_COMMAND": "Неизвестная команда",
        "ERROR_UNKNOWN_ROLE": "Неизвестная роль",
        "ERROR_UNKNOWN_USER": "Неизвестный пользователь",
        "ERROR_UNVERIFIED_EMAIL": "Вы должны подтвердить свой email перед тем, как вы сможете отправлять сообщения",
        "ERROR_WRONG_COLOR": "Вы не можете использовать этот цвет",
        "ERROR_ALREADY_IGNORED": "Этот пользователь уже в игноре",
        "ERROR_INTERNAL_ERROR": "Внутренняя ошибка сервера",
        "ERROR_EMAIL_NULL": "Email не должен быть пустым",
        "ERROR_EMAIL_SAME": "Нельзя поменять email на такой же",
        "ERROR_EMAIL_TOO_OFTEN": "Вы слишком часто меняете email, попробуйте ещё раз через 30 минут",
        "ERROR_WRONG_CAPTCHA": "Неправильно введена капча.",
        "ERROR_BAD_REGISTRATION_FORMAT": "Некорректно введено имя пользователя, email или пароля.",
        "ERROR_REGISTRATION_DENIED": "Вы не можете создавать новые учётные записи.",
        "ERROR_DUPLICATE_EMAIL_OR_USERNAME": "Введённое имя или email уже используется другим пользователем.",

        "REGISTRATION_SUCCESS": "You have successfuly registered. An email with information how to confirm your " +
        "account has been sent to address you entered.",

        "TWITTER_RETWEETED": "ретвит от",
        "MESSAGE_DONATED":
            "<i class='fa fa-fw fa-money'></i> <strong>{{name}} пожертвовал {{amount | number:2}} {{currency}}</strong>",
        "MESSAGE_DONATED_MESSAGE":
            "<i class='fa fa-fw fa-money'></i> <strong>{{name}} пожертвовал {{amount | number:2}} {{currency}}</strong>: {{text}}"
    });

    $translateProvider.translations("ua", {
        "CONTROLS_EMOTICONS": "посміхайла",
        "CONTROLS_ICON": "іконка",
        "CONTROLS_CODE": "код",
        "CONTROLS_OK": "Ок",
        "CONTROLS_CLOSE": "Закрити",
        "CONTROLS_CHANGE_NAME": "Змінити ім'я",
        "CONTROLS_USERNAME": "Ім'я",
        "CONTROLS_CHANGE_PASSWORD": "Змінити пароль",
        "CONTROLS_AUTHENTICATION_REQUIRED": "Необхідна аутентифікація",
        "CONTROLS_CAPTCHA_REQUIRED": "Необхдна капча",
        "CONTROLS_USE_TWITCH": "Використати аккаунт twitch.tv",
        "CONTROLS_SIGN_IN": "Увійти",
        "CONTROLS_SIGN_UP": "Зареєструватись",

        "CONTROLS_MENU_ADMIN_PANEL": "панель адміністратора",
        "CONTROLS_MENU_COLOR": "колір",
        "CONTROLS_MENU_LOG_OUT": "вихід",
        "CONTROLS_MENU_TWITCH_AUTH": "логін twitch.tv ",
        "CONTROLS_MENU_SIGN_IN": "увійти",
        "CONTROLS_MENU_SIGN_UP": "зареєструватись",
        "CONTROLS_MENU_EMOTICONS": "посміхайла",
        "CONTROLS_MENU_POPOUT": "спливаюче вікно",
        "CONTROLS_MENU_SETTINGS": "Налаштування",
        "CONTROLS_MENU_HIDE_MOD_BUTTONS": "сховати кнопки модераторів",
        "CONTROLS_MENU_LEGACY_MODE": "спадщина",
        "CONTROLS_MENU_SHOW_TIMESTAMPS": "відобразити час",
        "CONTROLS_MENU_SHOW_IGNORED": "приховати заігнорених",
        "CONTROLS_MENU_DARK": "темна тема",
        "CONTROLS_MENU_LANGUAGE": "Мова",
        "CONTROLS_MENU_TICKETS": "квитки підтримки",
        "CONTROLS_MENU_HELP": "допомога",

        "CHAT_HELLO_UNAUTHENTICATED": "Привіт гість, ти долучився до {{room}}.",
        "CHAT_HELLO": "Привіт, <span style=\"color: {{color}}\">{{name}}</span>! Ти зайшов у {{room}} і ти {{role}}",
        "CHAT_LOGIN_SUCCESS": "Ти успішно зайшов як {{name}} (<strong>{{role}}</strong>)",
        "CHAT_CONNECTING": "підключаємося",
        "CHAT_LOST_CONNECTION": "з'єднання втрачено",
        "CHAT_MESSAGE_HIDDEN": "повідомлення видалено",
        "CHAT_AND_X_OTHERS": "Та {{count}} інших",
        "CHAT_CLEAR": "Чат очищено модератором",
        "CHAT_CLEAR_USER": "{{mod}} видалив повідомлення {{user}}",
        "CHAT_TIMEOUT_USER": "{{mod}} відправив на перерву {{user}}",
        "CHAT_BAN_USER": "{{mod}} забанив {{user}}",

        "USERS_MODS": "модератори",
        "USERS_USERS": "користувачі",

        "ROLE_UNAUTHENTICATED": "неаутентифікований",
        "ROLE_GUEST": "гість",
        "ROLE_USER_UNCONFIRMED": "непідтверджений користувач",
        "ROLE_USER": "користувач",
        "ROLE_SUPPORTER": "підтримувач",
        "ROLE_MOD": "модератор",
        "ROLE_ADMIN": "адміністратор",
        "ROLE_SUPERADMIN": "адміністратор"
    });

    $translateProvider.fallbackLanguage('en');

    $translateProvider.registerAvailableLanguageKeys(["en", "ru", "ua"], {
        "en*": "en",
        "ru*": "ru",
        "ua*": "ua",
        "*": "en"
    });
    $translateProvider.determinePreferredLanguage();
    $translateProvider.useStorage("$translateChatStorage");
    $translateProvider.useSanitizeValueStrategy("escapeParameters");
}]);
