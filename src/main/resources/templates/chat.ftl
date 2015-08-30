<!DOCTYPE HTML>

<html ng-app="chatApplication">

<head>
    <title>${title}</title>

    <meta name="viewport" content="width=device-width, height = device-height, user-scalable=no"/>
    <link rel="stylesheet" type="text/css" href="min/app.css"/>

    <script>
        /**
         * Protect window.console method calls, e.g. console is not defined on IE
         * unless dev tools are open, and IE doesn't define console.debug
         */
        (function() {
            if (!window.console) {
                window.console = {};
            }
            // union of Chrome, FF, IE, and Safari console methods
            var m = [
                "log", "info", "warn", "error", "debug", "trace", "dir", "group",
                "groupCollapsed", "groupEnd", "time", "timeEnd", "profile", "profileEnd",
                "dirxml", "assert", "count", "markTimeline", "timeStamp", "clear"
            ];
            // define undefined methods as noops to prevent errors
            for (var i = 0; i < m.length; i++) {
                if (!window.console[m[i]]) {
                    window.console[m[i]] = function() {};
                }
            }
        })();

        WEB_SOCKET_SWF_LOCATION = '/vendor/WebSocketMain.swf';
        WEB_SOCKET_DEBUG = true;

        HOST_NAME = document.location.hostname;

        SINGLE_ROOM = ${singleRoom?c};
    </script>

    <script src="https://www.google.com/recaptcha/api.js?onload=vcRecapthaApiLoaded&render=explicit" async defer></script>
    <script type="text/javascript" src="/min/app.js"></script>
</head>

<body>

<div id="content" ng-class="{'dark': isDark()}" ng-controller="StyleController">

<script type="text/ng-template" id="emoticons.html">
    <div class="modal-header">
        <h3>{{'CONTROLS_EMOTICONS' | translate}}</h3>
    </div>
    <div class="modal-body">
        <div style="max-height: 300px; overflow-y: auto">
            <table class="table table-hover">
                <thead>
                <tr>
                    <th class="col-xs-1">{{'CONTROLS_ICON' | translate}}</th>
                    <th class="col-xs-11" style="text-align: center">{{'CONTROLS_CODE' | translate}}</th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="emoticon in emoticons | orderBy:'code'" style="cursor: pointer;" ng-click="addToInput(unescapeCode(emoticon.code))">
                    <td><img ng-src="/emoticons/{{emoticon.fileName}}"/></td>
                    <td ng-bind="unescapeCode(emoticon.code)"></td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="modal-footer">
        <div class="btn btn-default btn-modal pull-left" ng-click="close()">{{'CONTROLS_CLOSE' | translate}}</div>
    </div>
</script>

<script type="text/ng-template" id="help.html">
    <div class="modal-header">
        <h3><i class='fa fa-info'></i> {{'CONTROLS_MENU_HELP' | translate}}</h3>
    </div>
    <div class="modal-body">
        <div style="max-height: 300px; overflow-y: auto; text-align: left">
            <h4>{{'HELP_MARKUP' | translate}}</h4>
            <ul>
                <li><code>**{{'HELP_BOLD_TEXT' | translate}}**</code> <span class="fa fa-long-arrow-right"></span> <strong>{{'HELP_BOLD_TEXT' | translate}}</strong></li>
                <li><code>*{{'HELP_ITALIC_TEXT' | translate}}*</code> <span class="fa fa-long-arrow-right"></span> <i>{{'HELP_ITALIC_TEXT' | translate}}</i></li>
                <li><code>%%{{'HELP_SPOILER_TEXT' | translate}}%%</code> <span class="fa fa-long-arrow-right"></span> <span class="spoiler">{{'HELP_SPOILER_TEXT' | translate}}</span></li>
                <li>
                    <code>!!!{{'HELP_NSFW_TEXT' | translate}}</code> <span class="fa fa-long-arrow-right"></span> <span class="nsfwLabel">NSFW</span> <span class="spoiler">{{'HELP_NSFW_TEXT' | translate}}</span><br/>
                    <code>!!!</code> {{'HELP_PREFIX_REQUIRED' | translate}}
                </li>
                <li>
                    <code>&gt;{{'HELP_QUOTE_TEXT' | translate}}</code> <span class="fa fa-long-arrow-right"></span> <span class="greenText">&gt;{{'HELP_QUOTE_TEXT' | translate}}</span><br/>
                    <code>&gt;</code> {{'HELP_PREFIX_REQUIRED' | translate}}
                </li>
                <li>
                    <code>@{{'HELP_MENTION' | translate}}</code> <span class="fa fa-long-arrow-right"></span> <span class="mentionLabel">@{{'HELP_MENTION' | translate}}</span><br/>
                    {{'HELP_MENTION_NOTE' | translate}}
                </li>
            </ul>
            <h4>{{'HELP_RENAME' | translate}}</h4>
            <p>
                {{'HELP_RENAME_TEXT1' | translate}}:<br/>
                <code>/name <strong>new_name</strong></code>.<br/>
                {{'HELP_RENAME_TEXT2' | translate}}.
            </p>
            <h4>{{'HELP_RENAME_FORMAT' | translate}}</h4>
            <p>
                {{'HELP_RENAME_FORMAT_TEXT' | translate}}
            </p>
            <h4>{{'HELP_UNBAN' | translate}}</h4>
            <p>
                {{'HELP_UNBAN_TEXT' | translate}}
            </p>
            <h4>{{'HELP_BROKEN' | translate}}</h4>
            <p>
                {{'HELP_BROKEN_TEXT' | translate}}
            </p>
            <h4>{{'HELP_BUG' | translate}}</h4>
            <p>
                {{'HELP_BUG_TEXT1' | translate}}
                <a href="http://imgur.com/" target="_blank">imgur</a> {{'HELP_BUG_TEXT2' | translate}}
            </p>
        </div>
    </div>
    <div class="modal-footer">
        <div class="btn btn-default pull-left" ng-click="close()">{{'CONTROLS_CLOSE' | translate}}</div>
    </div>
</script>

<script type="text/ng-template" id="chat/ui/tickets/compose.html">
    <div class="modal-header">
        <h3>
            <i class="fa fa-ticket"></i> {{'TICKETS_MINE' | translate}}
        </h3>
    </div>
    <form name="form" class="" ng-submit="submitTicket(name)">
        <div class="modal-body">
            <div class="alert" ng-if="response" ng-class="{'alert-success': response.success, 'alert-warning': !response.success}">
                {{response.text}}
            </div>
            <div class="form-group">
                <label class="control-label">{{'TICKETS_TYPE' | translate}}</label>
                <select
                        class="form-control"
                        required
                        ng-model="input.category"
                        >
                    <option value="BAN">{{'TICKETS_TYPE_BAN' | translate}}</option>
                    <option value="RENAME">{{'TICKETS_TYPE_RENAME' | translate}}</option>
                    <option value="BUG">{{'TICKETS_TYPE_BUG' | translate}}</option>
                    <option value="OTHER">{{'TICKETS_TYPE_OTHER' | translate}}</option>
                </select>
            </div>
            <div class="form-group" ng-class="{'has-error': form.text.$invalid && form.text.$dirty, 'has-success': form.text.$dirty && !form.text.$invalid}">
                <label class="control-label">{{'TICKETS_TEXT' | translate}}</label>
                <textarea
                        class="form-control"
                        required
                        name="text"
                        ng-model="input.text"
                        style="resize: none;"
                        minlength="6"
                        maxlength="1024"
                        ></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <div class='btn btn-warning pull-left' ng-click='close()' translate='CONTROLS_CLOSE'></div>
            <input ng-disabled="form.$invalid" ng-if="!submitting" type="submit" class="btn btn-primary" value="{{'CONTROLS_OK' | translate}}"/>
            <button disabled ng-if="submitting" class="btn btn-success"><span class="fa fa-spinner fa-spin"></span></button>
        </div>
    </form>
</script>

<script type="text/ng-template" id="chat/ui/tickets/list.html">
    <div class="modal-header">
        <h3>
            <i class="fa fa-ticket"></i> {{'TICKETS_MINE' | translate}}
        </h3>
    </div>
    <div class="modal-body">
        <div class="list-group">
            <div class="list-group-item"
                 ng-repeat="ticket in entries"
                 ng-class="{'list-group-item-success': !ticket.isOpen}">
                <h4 class="list-group-item-heading">
                    {{('TICKETS_TYPE_' + ticket.category) | translate}}
                </h4>
                <div class="list-group-item-text">
                    <div>
                        <strong translate="TICKETS_TEXT"></strong>: {{ticket.text}}
                    </div>
                    <div ng-if="ticket.adminReply">
                        <strong translate="TICKETS_ADMIN_REPLY"></strong>: {{ticket.adminReply}}
                    </div>
                </div>
            </div>
        </div>
        <div class="alert alert-info" ng-if="entries.length === 0">Nothing to show.</div>
    </div>
    <div class='modal-footer'>
        <div class="btn btn-default pull-left" ng-click="close()">{{'CONTROLS_CLOSE' | translate}}</div>
        <div class="btn btn-primary" ng-click="compose()" translate="TICKETS_COMPOSE"></div>
    </div>
</script>

<script type="text/ng-template" id="chat/ui/profile/email.html">
    <div class='modal-header'>
        <h3><i class='fa fa-envelope'></i> {{'PROFILE_EMAIL_SETTINGS' | translate}}</h3>
    </div>
    <form class="panel-body" name="form" ng-if="!hasPendingVerification">
        <div class='modal-body'>
            <div ng-if="hasPendingVerification">
                <div class="btn btn-default" ng-click="resendVerification()" translate="PROFILE_EMAIL_RESEND"></div>
            </div>
                <div class='alert alert-danger' ng-if='error' ng-bind='error'></div>
                <div class='alert alert-info' ng-if='info' ng-bind='info'></div>
                <div class="form-group" ng-class="{'has-error': form.email.$invalid && form.email.$dirty, 'has-success': !form.email.$invalid}">
                    <label for="email" class="control-label" translate="AUTH_EMAIL"></label>
                    <input
                        ng-model="email"
                        id="email"
                        type="email"
                        class="form-control"
                        name="email"
                        ng-placeholder="'AUTH_EMAIL' | translate"
                        required
                        />
                    </div>
        </div>
        <div class='modal-footer'>
            <div class='btn btn-warning pull-left' ng-click='close()' translate='CONTROLS_CLOSE'></div>
            <input
                    type="submit"
                    class="btn btn-primary"
                    ng-if="!hasPendingVerification"
                    ng-value="'CONTROLS_SET_EMAIL' | translate"
                    ng-click="setEmail(email)"
                    ng-disabled="inProgress || form.$invalid"/>
        </div>
    </form>
</script>

<script type="text/ng-template" id="chat/ui/profile/profile.html">
    <div class='modal-header'>
        <h3>
            <i class='fa fa-user'></i>
            {{self.name}}
            <span class='btn btn-link btn-xs pull-right' ng-if="profile" ng-click='showPasswordSettings()'><i class='fa fa-key'></i></span>
        </h3>
    </div>
    <div class='modal-body'>
        <form class='form-horizontal' ng-if='profile'>
            <div class='form-group' ng-if='profile.user.email'>
                <label class='col-sm-2 control-label'>Email</label>
                <div class='col-sm-10'>
                    <div class='form-control-static'>
                        {{profile.user.email}}
                        <i class='fa fa-fw fa-check text-success' ng-if='profile.user.emailVerified'></i>
                        <i class='fa fa-fw fa-times text-danger' ng-if='!profile.user.emailVerified'></i>
                        <span class='btn btn-link btn-xs pull-right' ng-click='showEmailSettings()'><i class='fa fa-pencil'></i></span>
                    </div>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-sm-2 control-label'>Name</label>
                <div class='col-sm-10'>
                    <div class='form-control-static'>
                        {{profile.user.name}}
                    </div>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-sm-2 control-label'>Role</label>
                <div class='col-sm-10'>
                    <div class='form-control-static'>
                        {{profile.user.role}}
                    </div>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-sm-2 control-label'>
                    Color
                    </label>
                <div class='col-sm-10'>
                    <div class='form-control-static'>
                        {{profile.user.color}} <span class='fa fa-circle fa-fw' ng-style='{"color": profile.user.color}'></span>
                        </div>
                    </div>
                </div>
            </form>
        <div class='panel panel-default' ng-if="profile">
            <div class='panel-heading'>
                <div class='panel-title'>API token</div>
            </div>
            <div class='panel-body'>
                <div class='input-group'>
                    <input type='text' class='form-control' ng-model='apiToken' readonly
                        placeholder='press button to get token'>
                    <span class='input-group-btn'>
                        <button class='btn btn-default' type='button' ng-click='newToken()'>
                            <i class='fa fa-fw fa-refresh'></i>
                        </button>
                    </span>
                </div>
            </div>
        </div>
    </div>
    <div class='modal-footer'>
        <div class='btn btn-default pull-left' ng-click='close()' translate='CONTROLS_CLOSE'></div>
    </div>
</script>

<script type="text/ng-template" id="chat/ui/profile/password.html">
    <div class='modal-header'>
        <h3><i class='fa fa-key'></i> {{'CONTROLS_CHANGE_PASSWORD' | translate}}</h3>
    </div>
    <form class='panel-body' name='pwForm' >
        <div class='modal-body'>
                <div class='alert alert-info' ng-if='info'>
                    {{info}}
                    </div>
                <div class='alert alert-danger' ng-if='error'>
                    {{error}}
                    </div>
                <div class='form-group' ng-class='{"has-error": pwForm.password.$invalid && pwForm.password.$dirty, "has-success": pwForm.password.$dirty && !pwForm.password.$invalid}'>
                    <label for='password' class='control-label' translate='AUTH_PASSWORD'></label>
                    <input
                        ng-model='password'
                        id='password'
                        type='password'
                        class='form-control'
                        name='password'
                        placeholder='{{"AUTH_PASSWORD" | translate}}'
                        pattern='.{6,30}'
                        title='{{"AUTH_PASSWORD_FORMAT" | translate}}'
                        required
                        />
                    </div>
            </div>
        <div class='modal-footer'>
            <div class='btn btn-warning pull-left' ng-click='close()' translate='CONTROLS_CLOSE'></div>
            <input
                    type="submit"
                    class="btn btn-primary"
                    ng-click='changePassword(password)'
                    ng-value="'CONTROLS_CHANGE_PASSWORD' | translate"
                    ng-disabled='pwForm.$invalid'/>
        </div>
    </form>
</script>

<script type="text/ng-template" id="authentication.html">
    <div class="modal-header">
        <h3 ng-if="action === 'sign_in'">{{'AUTH_SIGN_IN' | translate}}</h3>
        <h3 ng-if="action === 'registration'">{{'AUTH_NEW_ACCOUNT' | translate}}</h3>
    </div>
    <div class="modal-body">
        <div ng-if="action === 'sign_in'">
            <div class="form-group">
                <div class="btn btn-default btn-modal" ng-click="twitchAuth()"><span class="fa fa-twitch"></span> {{'AUTH_WITH_TWITCH' | translate}}</div>
            </div>
            <div ng-if="error" class="alert alert-danger" role="alert">{{error}}</div>
            <div ng-if="info" class="alert alert-info" role="alert">{{info}}</div>

            <form class="form" name="form" id="authForm" ng-submit="submitSignIn()">
                <div class="form-group" ng-class="{'has-error': form.username.$invalid && form.username.$dirty, 'has-success': !form.username.$invalid}">
                    <label for="username" class="control-label">{{'AUTH_USERNAME' | translate}}</label>
                    <input
                        ng-model="input.username"
                        id="username"
                        type="text"
                        class="form-control"
                        name="username"
                        placeholder="{{'AUTH_USERNAME' | translate}}"
                        pattern="[a-zA-Z][a-zA-Z0-9_]{2,16}"
                        title="{{'AUTH_NAME_FORMAT' | translate}}"
                        required
                    />
                </div>
                <div class="form-group" ng-class="{'has-error': form.password.$invalid && form.password.$dirty, 'has-success': !form.password.$invalid}">
                    <label for="password" class="control-label">{{'AUTH_PASSWORD' | translate}}</label>
                    <input
                        ng-model="input.password"
                        id="password"
                        type="password"
                        class="form-control"
                        name="password"
                        placeholder="{{'AUTH_PASSWORD' | translate}}"
                        pattern=".{6,30}"
                        title="{{'AUTH_PASSWORD_FORMAT' | translate}}"
                        required
                    />
                </div>
                <div ng-if="captchaRequired" class="form-group">
                    <label class="control-label">{{'AUTH_CAPTCHA' | translate}}</label>
                    <div vc-recaptcha="" key="'6Lepxv4SAAAAAMFC4jmtZvnzyekEQ3XuX0xQ-3TB'" on-create="recaptchaCreated(widgetId)"></div>
                </div>
                <div class="form-group">
                    <div ng-disabled="busy" class="btn btn-default" ng-click="switchTo('registration')">{{'AUTH_NEW_ACCOUNT' | translate}}</div>
                    <input type="submit" ng-disabled="busy || form.$invalid" class="btn btn-primary pull-right" value="{{'AUTH_SIGN_IN' | translate}}"/>
                </div>
            </form>
        </div>

        <div ng-if="action === 'registration'">
            <div class="form-group">
                <div class="btn btn-default btn-modal" ng-click="twitchAuth()"><span class="fa fa-twitch"></span> {{'AUTH_WITH_TWITCH' | translate}}</div>
            </div>
            <div ng-if="error" class="alert alert-danger" role="alert">{{error}}</div>

            <form name="form" class="form" id="regForm" ng-submit="submitRegistration()">
                <div class="form-group" ng-class="{'has-error': form.username.$invalid && form.username.$dirty, 'has-success': !form.username.$invalid}">
                    <label for="username" class="control-label">{{'AUTH_USERNAME' | translate}}</label>
                    <input
                           ng-model="input.username"
                           ng-model-options="{ updateOn: 'default blur', debounce: { default: 500, blur: 0 } }"
                           id="username"
                           type="text"
                           class="form-control"
                           name="username"
                           placeholder="{{'AUTH_USERNAME' | translate}}"
                           pattern="[a-zA-Z][a-zA-Z0-9_]{2,16}"
                           title="{{'AUTH_NAME_FORMAT' | translate}}"
                           username-validation
                           required
                    />
                    <span class="help-block" ng-show="form.username.$pending.username">{{'AUTH_USERNAME_CHECK_PENDING' | translate}}</span>
                    <span class="help-block" ng-show="form.username.$error.username">{{'AUTH_USERNAME_NOT_AVAILABLE' | translate}}</span>
                </div>
                <div class="form-group" ng-class="{'has-error': form.password.$invalid && form.password.$dirty, 'has-success': !form.password.$invalid}">
                    <label for="password" class="control-label">{{'AUTH_PASSWORD' | translate}}</label>
                    <input
                           ng-model="input.password"
                           id="password"
                           type="password"
                           class="form-control"
                           name="password"
                           placeholder="{{'AUTH_PASSWORD' | translate}}"
                           pattern=".{6,30}"
                           title="{{'AUTH_PASSWORD_FORMAT' | translate}}"
                           required
                    />
                </div>
                <div class="form-group" ng-class="{'has-error': form.email.$invalid && form.email.$dirty, 'has-success': !form.email.$invalid}">
                    <label for="email" class="control-label">{{'AUTH_EMAIL' | translate}}</label>
                    <input
                           ng-model="input.email"
                           id="email"
                           type="email"
                           class="form-control"
                           name="email"
                           placeholder="Email"
                           required
                    />
                </div>
                <div class="form-group">
                    <label class="control-label">{{'AUTH_CAPTCHA' | translate}}</label>
                    <div vc-recaptcha="" key="'6Lepxv4SAAAAAMFC4jmtZvnzyekEQ3XuX0xQ-3TB'" on-create="recaptchaCreated(widgetId)"></div>
                </div>
                <div class="form-group">
                    <div ng-disabled="busy" class="btn btn-default" ng-click="switchTo('sign_in')">{{'AUTH_SIGN_IN' | translate}}</div>
                    <input ng-disabled="busy || form.$invalid" type="submit" class="btn btn-primary pull-right" value="{{'AUTH_NEW_ACCOUNT' | translate}}"/>
                </div>
            </form>
        </div>
    </div>
</script>

<script type="text/ng-template" id="likedTemplate.html">
    <div ng-if="msg.likes.length === 0">
        {{'CHAT_NO_LIKES' | translate}}
    </div>
    <ul ng-if="msg.likes.length > 0" class="list-unstyled">
        <li ng-repeat="user in msg.likes | limitTo:3">{{user}}</li>
        <li ng-if="msg.likes.length > 3">{{'CHAT_AND_X_OTHERS' | translate:{'count': (msg.likes.length - 3)} }}</li>
    </ul>
</script>


<script type="text/ng-template" id="anonCaptcha.html">
    <div class="modal-header">
        <h3>{{'CONTROLS_CAPTCHA_REQUIRED' | translate}}</h3>
    </div>
    <div class="modal-body">
        <div class="alert alert-danger" ng-if="error">{{message}}</div>
        <form class="form-horizontal" id="anonCaptchaForm" ng-submit="ok()">
            <div vc-recaptcha="" key="'6Lepxv4SAAAAAMFC4jmtZvnzyekEQ3XuX0xQ-3TB'"></div>
            <br />
            <button class="btn btn-primary btn-modal" ng-click="ok()">{{'CONTROLS_OK' | translate}}</button>
        </form>
    </div>
</script>

<#if !singleRoom>
<div id="roomWidget" ng-controller="RoomWidgetController">
    <div id="roomSelector" class="btn-group btn-group-xxs" ng-class="{open: open}" style="vertical-align: top">
        <div class="btn btn-default" ng-class="{active: open}" ng-click="open=!open" style="width: 100%;">
            <span>{{getActiveRoom()}}</span>
            <span class="pull-right">
                <span ng-if="anyMentions()" class="fa fa-fw fa-at" style="color: red"></span>
                <span ng-if="anyUnread()" class="fa fa-fw fa-asterisk" style="color: red"></span>
                <span class="fa fa-fw fa-ellipsis-v"></span>
            </span>
        </div>
        <ul class="dropdown-menu" role="menu" style="width: 100%">
            <li ng-repeat="room in getRooms()" ng-click="setActiveRoom(room)">
                <a href="" ng-class="{disabled: room===getActiveRoom()}" style="white-space: normal; outline: none">
                    {{room}}
                    <div class="pull-right">
                        <div ng-if="unreadCount(room)>0" class="badge">
                            <span ng-if="unreadMentions(room)>0" class="fa fa-fw fa-at"></span>
                            {{unreadCount(room)}}
                        </div>
                    </div>
                </a>
            </li>
        </ul>
    </div>
    <div class="btn-group btn-group-xxs pull-right">
        <div class="btn btn-default" ng-click="joinRoom()"><span class="fa fa-fw fa-plus"></span></div>
        <div class="btn btn-default" ng-class="{disabled: getActiveRoom()==='#main'}" ng-click="partRoom()">
            <span class="fa fa-fw fa-times"></span>
        </div>
    </div>
</div>
</#if>

<div id="poll" class="center-block" ng-controller="PollWidgetController">
    <div id="pollHead" class="" ng-class="{open: open}" style="vertical-align: top" ng-if="hasActivePoll()">
        <div class="btn btn-xs btn-primary" ng-class="{active: open}" ng-click="open=!open" style="width: 100%;">
            <span>{{'POLL_OPEN_POLL' | translate}}: {{getActivePollQuestion()}}</span>
        </div>
        <div class="dropdown-menu pollMenu" role="menu" style="width: 100%">
            <div class="col-xs-12">
                <form ng-if="!hasVoted() && isOpen() && canVote()" ng-submit="vote(input.selectedOption)">
                    <div ng-repeat="option in getCurrentPollOptions()">
                        <div class="radio">
                            <label>
                                <input type="radio" ng-model="input.selectedOption" ng-value="option.optionId">
                                {{option.text}}
                            </label>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-sm btn-primary">{{'POLL_VOTE' | translate}}</button>
                </form>
                <div ng-repeat="option in getCurrentPollOptions()" ng-if="hasVoted() || !isOpen() || !canVote()">
                    <small>{{option.text}}</small>
                    <progressbar value="(getCurrentPollVotes()[option.optionId]/getCurrentPollMaxVotes())*100" max="100" type="success">{{getCurrentPollVotes()[option.optionId]}}</progressbar>
                </div>
            </div>
        </div>
    </div>
</div>


<div class="chat" ng-controller="MessagesController">
    <div class="messagesContainer tse-scrollable" style="width: 100%; height: 100%">
        <div class="tse-scroll-content" scroll-glue="">
            <div class="tse-content">
                <div ng-if="compact()" class="messages compact " ng-cloak="">
                    <div bindonce="" ng-repeat="message in messages[getActiveRoom()] track by message.internalId" class="messageBody" ng-controller="MessageController">
                        <div class="message" bo-if="(message.type === 'MSG_GROUP')">
                            <span class="timeCompact" ng-if="showTimestamps()">{{message.messages[0].time | date:'HH:mm'}}</span><!--
                            --><span class="btn-group btn-group-xs" ng-if="showModButtons()" style="margin-right: 3px">
                                <span class="btn btn-default" ng-click="clear()" title="clear"><span class="fa fa-eraser"></span></span>
                                <span class="btn btn-default" ng-click="ban()" title="ban"><span class="fa fa-ban"></span></span>
                                <span class="btn btn-default" ng-click="timeout()" title="time out"><span class="fa fa-clock-o"></span></span>
                            </span><!--
                            --><span bo-if="isMod()" class="mod">M</span><!--
                            --><span bo-if="isAdmin()" class="admin">A</span><!--
                            --><span bo-if="message.user.service!==null" class="ext" tooltip="{{message.user.serviceRes}}"
                                     tooltip-trigger="mouseenter" tooltip-placement="right"><span bo-if="message.user.service==='twitch'" class="fa fa-twitch" style="color: #6441A5"></span><!--
                            --><span bo-if="message.user.service==='cybergame'" class="fa fa-gamepad" style="color: #21b384"></span><!--
                            --><strong bo-if="message.user.service==='goodgame'" style="color: #73ADFF">GG</strong><!--
                            --><span bo-if="message.user.service==='sc2tv'" class="sc2tvIcon"></span></span><!--
                            --><span class="username" bo-style="{'color': message.user.color}" ng-click="addToInput($event)" bo-bind="message.user.name | inflector:'capital'"></span>:
                            <span class="userMessageContainer" ng-repeat="msg in message.messages track by $index">
                                <br bo-if="!$first"/>
                                <#if like>
                                    <span class="like" popover-append-to-body="true" popover-template="'likedTemplate.html'" popover-title="Liked this:" popover-trigger="mouseenter" popover-placement="left">
                                        <span class="likeButton btn btn-link btn-xs" ng-click="like(msg.id_)" ng-class="{likedButton: msg.likes.length &gt; 0}">
                                            <span class="fa fa-heart"></span><!--
                                            --><span class="likeCount" ng-if="msg.likes.length &gt; 0">&nbsp;{{msg.likes.length}}</span>
                                        </span>
                                    </span>
                                </#if>
                                <span bo-if="!$first">&gt; </span>
                                <span class="userMessageBody" ng-if="!msg.hidden" ng-bind-html="msg.body"></span>
                                <a class="userMessageBody" ng-if="msg.hidden" ng-click="msg.hidden=false">[{{'CHAT_MESSAGE_HIDDEN' | translate}}]</a>
                            </span>
                        </div>
                        <div class="message" bo-if="(message.type == 'ME')" bo-style="{'color': message.user.color}">
                            <span class="timeCompact" ng-if="showTimestamps()">{{message.time | date:'HH:mm'}}</span><!--
                                    --><span class="btn-group btn-group-xs" ng-if="showModButtons()">
                                        <span class="btn btn-default" ng-click="clear()" title="clear"><span class="fa fa-eraser"></span></span>
                                        <span class="btn btn-default" ng-click="ban()" title="ban"><span class="fa fa-ban"></span></span>
                                        <span class="btn btn-default" ng-click="timeout()" title="time out"><span class="fa fa-clock-o"></span></span>
                                    </span><!--
                                    --><span ng-if="isMod()" class="mod">M</span><!--
                                    --><span ng-if="isAdmin()" class="admin">A</span><!--
                                    --><span ng-if="message.ext" class="ext" tooltip="{{message.extOriginRes}}"
                                             tooltip-trigger="mouseenter" tooltip-placement="right"><span class="fa fa-twitch" style="color: #6441A5"></span></span><!--
                                --><#if like><span class="like" popover-append-to-body="true" popover-template="'likedTemplate.html'" popover-title="Liked this:" popover-trigger="mouseenter" popover-placement="left">
                                    <span class="likeButton btn btn-link btn-xs" ng-click="like(message.id_)" ng-class="{likedButton: message.likes.length &gt; 0}">
                                        <span class="fa fa-heart"></span><!--
                                        --><span class="likeCount" ng-if="message.likes.length &gt; 0">&nbsp;{{message.likes.length}}</span>
                                    </span>
                                </span></#if><!--
                         --><span class="username" ng-click="addToInput($event)" bo-bind="message.user.name | inflector:'capital'"></span>
                            <span class="userMessageBody" ng-if="!message.hidden" ng-bind-html="message.body"></span>
                            <a class="userMessageBody" ng-if="message.hidden" ng-click="message.hidden=false">[{{'CHAT_MESSAGE_HIDDEN' | translate}}]</a>
                        </div>
                        <div class="alert-msg alert-msg-danger" bo-if="message.type === 'ERROR'">
                            <span ng-bind-html="message.body"></span>
                        </div>
                        <div class="alert-msg alert-msg-info" bo-if="!((message.type === 'ME') || (message.type === 'MSG_GROUP') || (message.type === 'ERROR'))">
                            <span ng-bind-html="message.body"></span>
                        </div>
                    </div>
                </div>
                <div ng-if="!compact()" class="messages" ng-cloak="">
                    <div bindonce="" ng-repeat="message in messages[getActiveRoom()] track by message.internalId" class="messageBody" ng-controller="MessageController">
                        <div class="message" bo-if="(message.type == 'MSG_GROUP')" bo-style="{'border-color': message.user.color}">
                            <div class="messageHeading">
                                <span class="username" bo-style="{'color': message.user.color}" ng-click="addToInput($event)" bo-bind="message.user.name | inflector:'capital'"></span>
                                <small class="role" bo-if="message.user.service===null" bo-bind="'ROLE_' + getHighestRole().role.title | translate"></small>
                                <small class="role" bo-if="message.user.service!==null"><span bo-bind="message.user.serviceRes"></span> <!--
                                --><a bo-if="message.user.service==='twitch'" bo-href="extUrl()" target="_blank"><span class="fa fa-twitch" style="color: #999999"></span></a><!--
                                --><span bo-if="message.user.service==='cybergame'" class="fa fa-gamepad" style="color: #999999"></span><!--
                                --><strong bo-if="message.user.service==='goodgame'" style="color: #73ADFF">GG</strong><!--
                                --><span bo-if="message.user.service==='sc2tv'" class="sc2tvIcon"></span></small>
                                <div class="pull-right btn-group modButtons" ng-if="showModButtons()">
                                    <div class="btn btn-link btn-x" ng-click="clear()"><span class="fa fa-eraser"></span></div>
                                    <div class="btn btn-link btn-x" ng-click="ban()"><span class="fa fa-ban"></span></div>
                                    <div class="btn btn-link btn-x" ng-click="timeout()"><span class="fa fa-clock-o"></span></div>
                                </div>
                            </div>
                            <div class="userMessageContainer" ng-repeat="msg in message.messages track by $index">
                                <div class="time" bo-bind="msg.time | date:'HH:mm'"></div>
                                <#if like><div class="like" popover-append-to-body="true" popover-template="'likedTemplate.html'" popover-title="Liked this:" popover-trigger="mouseenter" popover-placement="left">
                                        <span class="likeButton btn btn-link btn-xs" ng-click="like(msg.id_)" ng-class="{likedButton: msg.likes.length &gt; 0}">
                                            <span class="fa fa-heart"></span><!--
                                            --><span class="likeCount" ng-if="msg.likes.length &gt; 0">&nbsp;{{msg.likes.length}}</span>
                                        </span>
                                </div></#if>
                                <div class="userMessageBody" bo-style="{'border-color': message.user.color}" ng-if="!msg.hidden" ng-bind-html="msg.body"></div>
                                <a class="userMessageBody" bo-style="{'border-color': message.user.color}" ng-if="msg.hidden" ng-click="msg.hidden=false">[{{'CHAT_MESSAGE_HIDDEN' | translate}}]</a>
                            </div>
                        </div>
                        <div class="message meMessage" bo-if="(message.type == 'ME')" bo-style="{'color': message.user.color, 'border-color': message.user.color}">
                            <div class="time" bo-bind="message.time | date:'HH:mm'"></div>
                            <#if like><div class="like" popover-append-to-body="true" popover-template="'likedTemplate.html'" popover-title="Liked this:" popover-trigger="mouseenter" popover-placement="left">
                                    <span class="likeButton btn btn-link btn-xs" ng-click="like(message.id_)" ng-class="{likedButton: message.likes.length &gt; 0}">
                                        <span class="fa fa-heart"></span><!--
                                        --><span class="likeCount" ng-if="message.likes.length &gt; 0">&nbsp;{{message.likes.length}}</span>
                                    </span>
                            </div></#if>
                            <div class="me">
                                    <span class="username"
                                          ng-click="addToInput($event)" bo-bind="message.user.name | inflector:'capital'"></span>
                                <span class="userMessageBodyMe" ng-if="!message.hidden" ng-bind-html="message.body"></span>
                                <a class="userMessageBodyMe" ng-if="message.hidden" ng-click="message.hidden=false">[{{'CHAT_MESSAGE_HIDDEN' | translate}}]</a>
                            </div>
                        </div>
                        <div class="alert-msg alert-msg-danger" bo-if="message.type == 'ERROR'">
                            <span ng-bind-html="message.body"></span>
                        </div>
                        <div class="alert-msg alert-msg-info" bo-if="!((message.type == 'ME') || (message.type == 'MSG_GROUP') || (message.type == 'ERROR'))">
                            <span ng-bind-html="message.body"></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="inputDiv" ng-controller="UserInputController">
    <div class="left-part input-group" style="text-align: center">
        <form ng-show="isConnected()"  ng-submit="sendMessage()">
            <textcomplete members='members' message='message' class="dropup"></textcomplete>
        </form>
        <div ng-if="isConnecting()" class="connectionState connecting">
            <i class="fa fa-fw fa-circle-o-notch fa-spin"></i> {{"CHAT_CONNECTING" | translate}}
        </div>
        <div ng-if="isAuthenticating()" class="connectionState authenticating">
            <i class="fa fa-fw fa-circle-o-notch fa-spin"></i> {{"CHAT_AUTHENTICATING" | translate}}
        </div>
        <div ng-if="isDisconnected()" class="connectionState disconnected">
            <i class="fa fa-fw fa-exclamation"></i> {{"CHAT_LOST_CONNECTION" | translate}} ({{"CHAT_RECONNECT_CD" | translate}} <timer until="getReconnectTime()"></timer>)
        </div>
    </div>
    <div class="right-part">
        <div class="btn-group" ng-controller="MenuController">
            <div class="btn-group">
                <div id="listButton" class="btn btn-link btn-link-default" ng-click="toggleOnline()" ng-class="{active: showOnline}">
                    <span class="fa fa-list"></span>
                </div>
            </div>
            <div class="btn-group">
                <div id="settingsButton" class="btn btn-link btn-link-default" ng-click="toggleSettings()" ng-class="{active: showSettings}">
                    <span class="fa fa-cog"></span>
                </div>
            </div>
            <div id="sideMenu" class="tse-scrollable online <#if !singleRoom>offset</#if>" style="display: none">
                <div class="tse-scroll-content">
                    <div id="sideMenuContent" class="tse-content" style="width: 100%;">
                        <div style="width: 100%; height: 100%" ng-controller="UsersController" ng-if="showOnline">
                            <div class="list-group online-list" ng-if="(users | mods).length > 0">
                                <div class="list-group-item"><h4 style="font-weight: bold">{{'USERS_MODS' | translate}}</h4></div>
                                <a class="list-group-item" ng-repeat-start="user in users | mods | orderBy:'name'"
                                   ng-controller="UserController" ng-class="{active: user.showDescription}" ng-click="toggleDescription()">
                                    {{user.name | inflector:'capital'}}
                                </a>
                                <div ng-repeat-end="" class="list-group-item desc" ng-class="{show: user.showDescription}" ng-controller="UserController" style="display: none">
                                    <div class="onlineUserDescription small"><span class="fa fa-user"></span> {{"ROLE_" + user.role.title | translate}}</div>
                                    <div class="onlineUserDescription small"><span class="fa fa-star"></span> {{"ROLE_" + user.globalRole.title | translate}}</div>
                                    <div ng-if="canBan()" class="btn btn-default btn-x" ng-click="ban()"><span class="fa fa-ban"></span></div>
                                    <div ng-if="canUnban()" class="btn btn-default btn-x" ng-click="unban()"><span class="fa fa-check-circle-o"></span></div>
                                    <div ng-if="canTimeOut()" class="btn btn-default btn-x" ng-click="timeout()"><span class="fa fa-clock-o"></span></div>
                                </div>
                            </div>
                            <div class="list-group online-list" ng-if="(users | users).length > 0">
                                <div class="list-group-item"><h4 style="font-weight: bold">{{'USERS_USERS' | translate}}</h4></div>
                                <a class="list-group-item" ng-repeat-start="user in users | users | orderBy:'name'"
                                   ng-controller="UserController" ng-class="{active: user.showDescription}" ng-click="toggleDescription()">
                                    {{user.name | inflector:'capital'}}
                                </a>
                                <div ng-repeat-end="" class="list-group-item desc" ng-class="{show: user.showDescription}" ng-controller="UserController" style="display: none">
                                    <div class="onlineUserDescription small"><span class="fa fa-user"></span> {{"ROLE_" + user.role.title | translate}}</div>
                                    <div ng-if="showModButtons()" class="btn btn-default btn-x" ng-click="clear()"><span class="fa fa-eraser"></span></div>
                                    <div ng-if="canBan()" class="btn btn-default btn-x" ng-click="ban()"><span class="fa fa-ban"></span></div>
                                    <div ng-if="canUnban()" class="btn btn-default btn-x" ng-click="unban()"><span class="fa fa-check-circle-o"></span></div>
                                    <div ng-if="canTimeOut()" class="btn btn-default btn-x" ng-click="timeout()"><span class="fa fa-clock-o"></span></div>
                                </div>
                            </div>
                        </div>
                        <ul class="settings-menu" ng-show="showSettings" ng-controller="SettingsController">
                            <li><a ng-click="showProfile()"><h4><span class="fa fa-user"></span> {{getSelf().name | inflector:'capital'}}</h4></a></li>
                            <li ng-if="isAdmin()"><a href="/admin/" target="_blank"><span class="fa fa-cogs"></span> {{'CONTROLS_MENU_ADMIN_PANEL' | translate}}</a></li>
                            <li>
                                <a colorpicker="hex" ng-model="color" colorpicker-position="left"
                                   colorpicker-filter="colorFilter" colorpicker-cls="setColor">
                                    <span class="fa fa-circle" ng-style="{color: getSelf().color}" id="color"></span> {{'CONTROLS_MENU_COLOR' | translate}}</a>
                            </li>
                            <li ng-if="!canLogin()"><a ng-click="logOut()"><span class="fa fa-sign-out"></span> {{'CONTROLS_MENU_LOG_OUT' | translate}}</a></li>
                            <li ng-if="canLogin()"><a ng-click="twitchAuth()"><span class="fa fa-twitch"></span> {{'CONTROLS_MENU_TWITCH_AUTH' | translate}}</a></li>
                            <li ng-if="canLogin()"><a ng-click="showSignIn()"><span class="fa fa-sign-in"></span> {{'CONTROLS_MENU_SIGN_IN' | translate}}</a></li>
                            <li ng-if="canLogin()"><a ng-click="showSignUp()"><span class="fa fa-sign-in"></span> {{'CONTROLS_MENU_SIGN_UP' | translate}}</a></li>
                            <li class="divider"></li>
                            <li ng-if="!canLogin()"><a ng-click="showTickets()"><span class="fa fa-ticket"></span> {{'CONTROLS_MENU_TICKETS' | translate}}</a></li>
                            <li><a ng-click="showEmoticons()"><span class="fa fa-smile-o"></span> {{'CONTROLS_MENU_EMOTICONS' | translate}}</a></li>
                            <li><a ng-click="showHelp()"><span class="fa fa-fw fa-info"></span> {{'CONTROLS_MENU_HELP' | translate}}</a></li>
                            <li><a ng-click="popout()"><span class="fa fa-external-link"></span> {{'CONTROLS_MENU_POPOUT' | translate}}</a></li>
                            <li class="divider"></li>
                            <li><h4 class="list-padded">{{'CONTROLS_MENU_SETTINGS' | translate}}</h4></li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="hideMB" label="{{'CONTROLS_MENU_HIDE_MOD_BUTTONS' | translate}}"/>
                            </li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="compact" label="{{'CONTROLS_MENU_LEGACY_MODE' | translate}}"/>
                            </li>
                            <li ng-show="compact" style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="showTS" label="{{'CONTROLS_MENU_SHOW_TIMESTAMPS' | translate}}"/>
                            </li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="showIgnored" label="{{'CONTROLS_MENU_SHOW_IGNORED' | translate}}"/>
                            </li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="dark" label="{{'CONTROLS_MENU_DARK' | translate}}"/>
                            </li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="hideExt" label="{{'CONTROLS_MENU_HIDE_EXT' | translate}}"/>
                            </li>
                            <li ng-show="notificationsAvailable()" style="padding-left: 20px; padding-right: 20px">
                                <fa-checkbox model="notifications" label="{{'CONTROLS_MENU_NOTIFICATIONS' | translate}}"/>
                            </li>
                            <li class="divider"></li>
                            <li><h4 class="list-padded">{{'CONTROLS_MENU_LANGUAGE' | translate}}</h4></li>
                            <li style="padding-left: 20px; padding-right: 20px">
                                <select class="form-control"
                                        ng-model="selectedLanguage"
                                        ng-options="lang.title for (key, lang) in langs"></select>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</div>
</body>
</html>
