<!DOCTYPE HTML>
<html ng-app="AdminApplication">
<head>
    <title ng-controller="TitleController" ng-bind="'Admin panel: ' + title()"></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <link rel="stylesheet" type="text/css" href='//fonts.googleapis.com/css?family=Roboto:400,400italic,700,700italic&subset=latin,cyrillic'>
    <link rel="stylesheet" type="text/css" href="/vendor/css/animate.css"/>
    <link rel="stylesheet" type="text/css" href="/vendor/css/bootstrap.css"/>
    <link rel="stylesheet" type="text/css" href="/css/font-awesome.css">

    <base href="/admin/" />

    <script type="application/javascript">
        document.SELF_ROLE = "${user.role}";
    </script>

    <script src="//code.jquery.com/jquery-2.1.0.min.js"></script>
    <script src="/vendor/js/angular.js"></script>
    <script src="/vendor/js/angular-animate.2.js"></script>
    <script src="/vendor/js/angular-route.js"></script>
    <script src="/vendor/js/angular-ui-utils.js"></script>
    <script src="/vendor/js/angular-ui-bootstrap.2.js"></script>
    <script src="/vendor/js/angular-relative-date.js"></script>
    <script src="/vendor/js/angular-datetimepicker.js"></script>
    <script src="/vendor/js/highcharts.js"></script>
    <script src="/js/admin/main.js"></script>

    <style>
        .datetimepicker-wrapper {
            vertical-align: middle;
            display: inline-block;
        }

        .datetimepicker-wrapper > input {
            margin-bottom: 0 !important;
            width: 130px;
        }

        .datetimepicker-wrapper [ng-model=hours],
        .datetimepicker-wrapper [ng-model=minutes] {
            width: 46px !important;
        }

        .pager {
            margin: 0;
        }

        body {
            font-family: "Roboto", sans-serif;
            min-width: 600px;
        }

        .btn, .form-control {
            border-radius: 0;
        }

        .sidebar {
            display: none;
        }
        @media (min-width: 768px) {
            .sidebar {
                position: fixed;
                top: 0;
                left: 0;
                bottom: 0;
                z-index: 1000;
                display: block;
                padding: 20px 20px 20px;
                background-color: #f5f5f5;
                border-right: 1px solid #e7e7e7;
                width: 200px
            }

            .main {
                margin-left: 200px;
            }
        }

        .main {
            overflow-x: hidden;
            padding-right: 15px;
            padding-left: 15px;
        }

        .nav-sidebar {
            margin-left: -20px;
            margin-right: -21px; /* 20px padding + 1px border */
            margin-bottom: 20px;
        }
        .nav-sidebar > li:first-child > a {
            border-top: 1px solid #e1e1e1;
        }
        .nav-sidebar > li > a {
            padding-left: 20px;
            padding-right: 20px;
            outline: 0;
            color: #5b5b5b;
            border-bottom: 1px solid #e1e1e1;
        }
        .nav-sidebar > .active > a,
        .nav-sidebar > .active > a:hover,
        .nav-sidebar > .active > a:focus {
            color: #fff;
            background-color: #5b5b5b;
            transition: all ease-in-out .2s;
        }

        .nav-sidebar .active-add
        .nav-sidebar .active-remove {
        }

        .pager li > a, .pager li > span {
            border-radius: 0;
        }

        .main .page-header {
            margin-top: 0;
        }

        .sortableTitle {
            cursor: pointer;
        }

        .sortableTitle:hover {
            background-color: #efefef;
        }

        .sortableTitle .fa {
            padding-top: 3px;
        }

        .btn-link-success {
            color: #5cb85c;
        }
        .btn-link-success:hover,
        .btn-link-success:focus,
        .btn-link-success:active,
        .btn-link-success.active {
            color: #449d44;
        }
        .btn-link-success:active,
        .btn-link-success.active {
            background-image: none;
        }
        .btn-link-success.disabled,
        .btn-link-success[disabled],
        fieldset[disabled] .btn-link-success,
        .btn-link-success.disabled:hover,
        .btn-link-success[disabled]:hover,
        fieldset[disabled] .btn-link-success:hover,
        .btn-link-success.disabled:focus,
        .btn-link-success[disabled]:focus,
        fieldset[disabled] .btn-link-success:focus,
        .btn-link-success.disabled:active,
        .btn-link-success[disabled]:active,
        fieldset[disabled] .btn-link-success:active,
        .btn-link-success.disabled.active,
        .btn-link-success[disabled].active,
        fieldset[disabled] .btn-link-success.active {
            color: #5cb85c;
        }


        .btn-link-danger {
            color: #d9534f;
        }
        .btn-link-danger:hover,
        .btn-link-danger:focus,
        .btn-link-danger:active,
        .btn-link-danger.active {
            color: #c9302c;
        }
        .btn-link-danger:active,
        .btn-link-danger.active {
            background-image: none;
        }
        .btn-link-danger.disabled,
        .btn-link-danger[disabled],
        fieldset[disabled] .btn-link-danger,
        .btn-link-danger.disabled:hover,
        .btn-link-danger[disabled]:hover,
        fieldset[disabled] .btn-link-danger:hover,
        .btn-link-danger.disabled:focus,
        .btn-link-danger[disabled]:focus,
        fieldset[disabled] .btn-link-danger:focus,
        .btn-link-danger.disabled:active,
        .btn-link-danger[disabled]:active,
        fieldset[disabled] .btn-link-danger:active,
        .btn-link-danger.disabled.active,
        .btn-link-danger[disabled].active,
        fieldset[disabled] .btn-link-danger.active {
            color: #d9534f;
        }

        .btn.btn-xs {
            padding: 0;
			font-size: 14px;
        }

        .alertOverlay {
            position: absolute;
            bottom: 0;
            left: 0;
            z-index: 2000;
        }

        .announcementContainer img {
            width: 100%;
            height: auto;
        }

        .active .badge {
            color: #5b5b5b;
            background-color: #fff;
        }

        .auth {
            color: #cdcdcd;
        }

        .auth.fa-twitch.active {
            color: #6441A5;
        }

        .auth.default.active {
            color: #5b5b5b;
        }

        .panel-heading {
            border-radius: 0;
        }

        .form-control-static {
            padding-bottom: 0;
        }

        .mainContent.ng-enter {
            -webkit-animation-name: fadeIn;
            -webkit-animation-duration: .50s;
            -moz-animation-name: fadeIn;
            -moz-animation-duration: .50s;
            animation-name: fadeIn;
            animation-duration: .50s;
        }

        tr.ng-enter {
            -webkit-animation-name: zoomInLeft;
            -webkit-animation-duration: .2s;
            -moz-animation-name: zoomInLeft;
            -moz-animation-duration: .2s;
            animation-name: zoomInLeft;
            animation-duration: .2s;
        }

        table.ng-enter {
            height: 0px;
        }
        table.ng-enter.ng-enter-active {
            heigh: auto;
            transition: height ease-in .2s;
        }

        .userPanel.ng-enter {
            right: -100%;
            -webkit-transition: all .2s;
            -moz-transition: all .2s;
            -ms-transition: all .2s;
            -o-transition: all .2s;
            transition: all .2s;
            -webkit-transition-delay: .2s;
            -moz-transition-delay: .2s;
            -ms-transition-delay: .2s;
            -o-transition-delay: .2s;
            transition-delay: .2s;
        }

        .userPanel.ng-enter-active {
            right: 0;
        }

        .userPanel.ng-leave {
            right: 0;
            -webkit-transition: all .2s;
            -moz-transition: all .2s;
            -ms-transition: all .2s;
            -o-transition: all .2s;
            transition: all .2s;
        }

        .userPanel.ng-leave-active {
            right: -100%;
        }

        .usersPanel.open-add {
            -webkit-transition: width .2s;
            -moz-transition: width .2s;
            transition: width .2s;
        }

        .usersPanel.open-remove {
            -webkit-transition: width .2s;
            -moz-transition: width .2s;
            transition: width .2s;
            -webkit-transition-delay: .2s;
            -moz-transition-delay: .2s;
            -ms-transition-delay: .2s;
            -o-transition-delay: .2s;
            transition-delay: .2s;
        }
    </style>
</head>
<body>
<script type="text/ng-template" id="dashboard.html">
    <div class="col-xs-8">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <i class="fa fa-area-chart"></i> online
                </h4>
            </div>
            <div id="onlineConnectionsChart" style="height: 350px;"></div>
        </div>
    </div>
    <div class="col-xs-4">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <i class="fa fa-comments-o"></i> rooms
                </h4>
            </div>
            <div class="panel-body">
                <input
                        class="form-control pull-right input-sm"
                        type="input"
                        placeholder="filter"
                        ng-model="filter"
                        />
            </div>
            <div class="list-group">
                <a ng-repeat="room in entries | filter:{'name': filter} | orderBy:'id'" href="/admin/room?id={{room.id}}" class="list-group-item">
                    <span class="badge">{{room.online}}</span>
                    <h4 class="list-group-item-heading">{{room.name}}</h4>
                    <p class="list-group-item-text">{{room.topic}}</p>
                </a>
            </div>
            <#if user.role == "SUPERADMIN">
                <div class="panel-footer">
                    <div class="btn btn-primary">create room</div>
                </div>
            </#if>
        </div>
    </div>
</script>

<script type="text/ng-template" id="emoticons.html">
    <div class="col-sm-8">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h4 class="panel-title">
                    emoticons
                </h4>
            </div>
            <table class="table table-hover">
                <thead>
                <tr>
                    <th class="col-xs-1">Icon</th>
                    <th class="col-xs-10 sortableTitle" ng-click="orderBy('code')">
                        Code <span class="fa fa-fw pull-right" ng-class="getSortIconClass('code')"></span>
                    </th>
                    <th class="col-xs-1" style="text-align: center;">Delete</th>
                </tr>
                </thead>
                <tr ng-repeat="emoticon in emoticons | orderBy:order:orderDesc">
                    <td><img ng-src="/emoticons/{{emoticon.fileName}}"/></td>
                    <td ng-bind="emoticon.code"></td>
                    <td style="text-align: center;">
                        <div class="btn btn-link btn-link-danger btn-xs" ng-click="requestDelete(emoticon.id)">
                            <span class="fa fa-trash fa-fw"></span>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
    </div>

    <div class="col-sm-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <i class="fa fa-plus"></i> add emoticon
                </h4>
            </div>
            <div class="panel-body">
                <form action="/admin/api/emoticons" method="post" enctype="multipart/form-data" role="form" acceptcharset="UTF-8">
                    <div class="form-group">
                        <label>Emoticon code</label>
                        <input type="text" name="code" placeholder="enter emoticon code" class="form-control"/>
                    </div>
                    <div class="form-group">
                        <label>Emoticon file</label>
                        <input type="file" name="file">
                    </div>
                    <div class="form-group">
                        <button type="submit" class="btn btn-default">Submit</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</script>

<script type="text/ng-template" id="journal.html">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <h4 class="panel-title">
                journal
            </h4>
        </div>
        <table class="table table-hover">
            <thead>
            <tr>
                <th style="width: 120px">
                    date
                </th>
                <th style="width: 50px">
                    tag
                </th>
                <th>
                    message
                </th>
                <th style="width: 100px">
                    user
                </th>
            </tr>
            </thead>
            <tr ng-repeat="entry in entries">
                <td>
                    <abbr title="{{entry.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{entry.timestamp | relativeDate}}</abbr>
                </td>
                <td>
                    <span class="label" ng-class="getTagLabelClass(entry.tag)">
                        {{entry.tag}}
                    </span>
                </td>
                <td>
                    {{entry.message}}
                </td>
                <td>
                    {{entry.user}}
                </td>
            </tr>
        </table>
        <div class="panel-footer" ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
        </div>
    </div>
</script>

<script type="text/ng-template" id="tickets.html">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <h4 class="panel-title">
                tickets
            </h4>
        </div>
        <div class="panel-body">
            <span class="btn btn-xs btn-link btn-link-success" ng-class="{disabled: opened}" ng-click="setOpened(true)"><span class="fa fa-exclamation-circle"></span> open</span>
            <span class="btn btn-xs btn-link btn-link-danger" ng-class="{disabled: !opened}" ng-click="setOpened(false)"><span class="fa fa-check-circle"></span> closed</span>
            <div class="alert alert-info" ng-if="entries.length === 0">
                nothing to show
            </div>
        </div>
        <table class="table table-hover" ng-if="entries.length > 0">
            <thead>
            <tr>
                <th style="width: 120px">
                    date
                </th>
                <th style="width: 100px">
                    user
                </th>
                <th style="width: 50px">
                    category
                </th>
                <th style="width: 100px" ng-if="!opened">
                    closed by
                </th>
                <th>
                    message
                </th>
                <th ng-if="!opened">
                    admin comment
                </th>
                <th style="width: 20px" ng-if="opened">
                </th>
            </tr>
            </thead>
            <tr ng-repeat="entry in entries">
                <td>
                    <abbr title="{{entry.ticket.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{entry.ticket.timestamp | relativeDate}}</abbr>
                </td>
                <td>
                    <a ng-href="/admin/users?search={{entry.user.name}}" ng-click="showUser(entry.user.id, $event)">{{entry.user.name}}</a>
                </td>
                <td>
                    <span class="label" ng-class="getLabelClass(entry.ticket.category)">{{entry.ticket.category}}</span>
                </td>
                <td ng-if="!opened">
                    {{entry.closedBy}}
                </td>
                <td>
                    {{entry.ticket.text}}
                </td>
                <td ng-if="!opened">
                    {{entry.ticket.adminReply}}
                </td>
                <td ng-if="opened">
                    <span class="btn btn-xs btn-link btn-link-success" title="close" ng-click="tryClose(entry.ticket.id)">
                        <span class="fa fa-check-circle"></span>
                    </span>
                </td>
            </tr>
        </table>
        <div class="panel-footer" ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
        </div>
    </div>
</script>


<script type="text/ng-template" id="history.html">
    <div class="modal-header">
        <h3 class="modal-title">
            History for room {{room.name}} <small>page {{page+1}}/{{totalPages+1}}</small>
            <div class="input-group pull-right" style="max-width:200px;">
                <input ng-model="input.user" type="text" class="form-control" placeholder="User filter">
                <span class="input-group-btn">
                    <button class="btn btn-default" type="button" ng-click="addUserFilter(input.user)">add</button>
                </span>
            </div>
        </h3>
    </div>
    <div class="modal-body">
        <div class="well well-sm" ng-if="users.length > 0">
            <div ng-if="users">
                <div ng-repeat="user in users" class="btn-group btn-group-xs" style="margin-right: 5px; margin-bottom: 5px">
                    <div class="btn btn-default disabled">{{user}}</div>
                    <div class="btn btn-default" ng-click="removeUserFilter(user)"><span class="fa fa-times"></span></div>
                </div>
            </div>
        </div>
        <div class="panel panel-default">
            <table class="table table-hover table-striped">
                <thead>
                <tr>
                    <th style="width: 120px">
                        date
                    </th>
                    <th>
                        message
                    </th>
                </tr>
                </thead>
                <tr ng-repeat="entry in entries" ng-class="{'warning': entry.hidden, 'info': ((entry.type==='CLEAR') || (entry.type==='BAN') || (entry.type==='TIMEOUT'))}">
                    <td>
                        <abbr title="{{entry.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{entry.timestamp | relativeDate}}</abbr>
                    </td>
                    <td ng-if="(entry.type==='MSG') || (entry.type==='ME') || (entry.type==='MSG_EXT')">
                        <span class="btn-link" ng-click="addUserFilter(entry.name)"><strong>&lt;{{entry.name}}&gt;</strong></span>
                        {{entry.message}}
                    </td>
                    <td ng-if="(entry.type==='CLEAR') || (entry.type==='BAN') || (entry.type==='TIMEOUT')">
                        {{entry.name}} cleared messages of {{entry.message}}
                    </td>
                </tr>
            </table>
        </div>
    </div>
    <div class="modal-footer" ng-if="(page !== 0) || hasNextPage()">
        <ul class="pager">
            <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
            <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
        </ul>
    </div>
</script>

<script type="text/ng-template" id="services.html">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <h4 class="panel-title">
                services
            </h4>
        </div>
        <table class="table table-hover table-striped">
            <thead>
            <tr>
                <th style="width: 120px">
                    name
                </th>
                <th style="width: 70px">
                    state
                </th>
                <th>

                </th>
            </tr>
            </thead>
            <tr ng-repeat="entry in entries">
                <td>
                    {{entry.name}}
                </td>
                <td>
                    <div class="label" ng-class="'label-' + getLabelClass(entry.state)">{{entry.state}}</div>
                </td>
                <td>
                    <span ng-if="entry.name==='announcements'">
                        Last delivery: <abbr title="{{entry.stateData | date:'dd.MM.yyyy HH:mm'}}">{{entry.stateData | relativeDate}}</abbr>
                    </span>
                    <span ng-if="entry.name==='twitch.tv'">
                        <strong>Receiver</strong>: {{entry.stateData.receiverConnected ? "connected" : "not connected"}}.
                        <strong>Outbound connections</strong>: {{entry.stateData.activeOutboundConnections}}
                    </span>
                    <span ng-if="(entry.name!=='twitch.tv') && (entry.name!=='announcements')">
                        {{entry.stateData}}
                    </span>
                </td>
            </tr>
        </table>
    </div>
</script>

<script type="text/ng-template" id="users.html">
    <div class="col-xs-12 usersPanel" ng-class="{'col-sm-6 col-md-7 col-lg-8 open': user, 'col-xs-12': !user}">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h4 class="panel-title">
                    users
                </h4>
            </div>
            <div class="panel-body">
                <form ng-submit="doSearch()">
                    <div class="input-group">
                        <input type="text" class="form-control" placeholder="Search" ng-model="searchInput">
                    <span class="input-group-btn">
                        <button type="button" class="btn btn-danger" ng-if="search" ng-click="resetSearch()">
                            <span class="fa fa-times fa-fw"></span>
                        </button>
                        <button type="submit" class="btn btn-default">
                            <i class="fa fa-fw fa-search"></i>
                        </button>
                    </span>
                    </div>
                </form>
            </div>
            <div class="list-group">
                <a
                        href="#"
                        class="list-group-item"
                        ng-repeat="u in users"
                        ng-class="{'active': user.id === u.id}"
                        ng-click="selectUser(u)">
                    <h4 class="list-group-item-heading"><small>${r"#"}{{u.id}}</small> {{u.name}} <small>{{u.role}}</small></h4>
                </a>
            </div>
            <div class="panel-body" ng-if="users.length === 0">
                <div class="alert alert-warning" role="alert">Nothing to show.</div>
            </div>
            <div class="panel-footer" ng-if="(page !== 0) || hasNextPage()">
                <ul class="pager">
                    <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                    <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
                </ul>
            </div>
        </div>
    </div>
    <div class="col-sm-6 col-md-5 col-lg-4 userPanel" ng-if="user" ng-controller="UserController">
        <div class="panel panel-success">
            <div class="panel-heading">
                <h4 class="panel-title">
                    User profile: {{user.name}}
                </h4>
            </div>
            <div class="panel-body">
                <form class="form-horizontal" ng-if="user">
                    <div class="form-group">
                        <label class="col-sm-2 control-label">Id</label>
                        <div class="col-sm-10">
                            <div class="form-control-static" ng-bind="user.id"></div>
                        </div>
                    </div>
                    <div class="form-group" ng-if="user.email">
                        <label class="col-sm-2 control-label">Email</label>
                        <div class="col-sm-10">
                            <div class="form-control-static" ng-bind="user.email"></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-2 control-label">Auth</label>
                        <div class="col-sm-10">
                            <div class="form-control-static">
                                <span class="auth default fa fa-fw fa-key" ng-class="{'active': hasAuth('password')}"></span>
                                <span class="auth fa fa-fw fa-twitch" ng-class="{'active': hasAuth('twitch.tv')}" tooltip="{{auth['twitch.tv']}}"></span>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="inputName" class="col-sm-2 control-label">
                            Name
                        </label>
                        <div class="col-sm-10">
                            <div class="input-group input-group-sm" ng-if="canEdit('name')">
                                <input
                                        ng-readonly="!editing('name')"
                                        type="text"
                                        class="form-control"
                                        id="inputName"
                                        placeholder="user name"
                                        ng-model="input.name">
                        <span class="input-group-btn">
                            <button ng-show="editing('name')" class="btn btn-warning" type="button" ng-click="reset('name')"><span class="fa fa-times fa-fw"></span></button>
                            <button ng-show="editing('name')" class="btn btn-success" type="button" ng-click="saveName()"><span class="fa fa-check fa-fw"></span></button>
                            <button ng-show="!editing('name')" class="btn btn-default" type="button" ng-click="edit('name')"><span class="fa fa-pencil fa-fw"></span></button>
                        </span>
                            </div>
                            <div class="form-control-static" ng-if="!canEdit('name')">
                                {{user.name}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="inputRole" class="col-sm-2 control-label">
                            Role
                        </label>
                        <div class="col-sm-10">
                            <div class="input-group input-group-sm" ng-if="canEdit('role')">
                                <select ng-disabled="!editing('role')" class="form-control" id="inputRole" ng-options="role for role in availableRoles" ng-model="input.role"></select>
                        <span class="input-group-btn">
                            <button ng-show="editing('role')" class="btn btn-warning" type="button" ng-click="reset('role')"><span class="fa fa-times fa-fw"></span></button>
                            <button ng-show="editing('role')" class="btn btn-success" type="button" ng-click="saveRole()"><span class="fa fa-check fa-fw"></span></button>
                            <button ng-show="!editing('role')" class="btn btn-default" type="button" ng-click="edit('role')"><span class="fa fa-pencil fa-fw"></span></button>
                        </span>
                            </div>
                            <div class="form-control-static" ng-if="!canEdit('role')">
                                {{user.role}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="inputRole" class="col-sm-2 control-label">
                            Color
                        </label>
                        <div class="col-sm-10">
                            <div class="form-control-static">
                                {{user.color}} <span class="fa fa-circle fa-fw" ng-style="{'color': user.color}"></span>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-sm-offset-2 col-sm-10">
                            <div class="checkbox">
                                <label>
                                    <input
                                            type="checkbox"
                                            ng-disabled="!isUser()"
                                            id="inputBanned"
                                            ng-model="input.banned"
                                            ng-change="saveBanned()"/> Banned
                                </label>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-sm-offset-2 col-sm-10">
                            <div class="checkbox">
                                <label>
                                    <input
                                            type="checkbox"
                                            id="inputRenameAvailable"
                                            ng-model="input.renameAvailable"
                                            ng-change="saveRenameAvailable()"/> Can change name
                                </label>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="panel-footer">
                <#if user.role == "SUPERADMIN">
                    <div ng-disabled="user && !canEdit('name')" ng-click="requestDelete()" class="btn btn-danger">Delete user</div>
                </#if>
                <div class="btn btn-warning pull-right" ng-click="selectUser(null)">Close</div>
            </div>
        </div>
    </div>
</script>

<script type="text/ng-template" id="user.html">
    <div class="modal-header">
        <h3 class="modal-title">
            User profile: {{user.name}}
        </h3>
    </div>
    <div class="modal-body">
        <form class="form-horizontal" ng-if="user">
            <div class="form-group">
                <label class="col-sm-2 control-label">Id</label>
                <div class="col-sm-10">
                    <div class="form-control-static" ng-bind="user.id"></div>
                </div>
            </div>
            <div class="form-group" ng-if="user.email">
                <label class="col-sm-2 control-label">Email</label>
                <div class="col-sm-10">
                    <div class="form-control-static" ng-bind="user.email"></div>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">Auth</label>
                <div class="col-sm-10">
                    <div class="form-control-static">
                        <span class="auth default fa fa-fw fa-key" ng-class="{'active': hasAuth('password')}"></span>
                        <span class="auth fa fa-fw fa-twitch" ng-class="{'active': hasAuth('twitch.tv')}" tooltip="{{auth['twitch.tv']}}"></span>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <label for="inputName" class="col-sm-2 control-label">
                    Name
                </label>
                <div class="col-sm-10">
                    <div class="input-group input-group-sm" ng-if="canEdit('name')">
                        <input ng-readonly="!editing('name')" type="text" class="form-control" id="inputName" placeholder="user name" ng-model="input.name">
                        <span class="input-group-btn">
                            <button ng-show="editing('name')" class="btn btn-warning" type="button" ng-click="reset('name')"><span class="fa fa-times fa-fw"></span></button>
                            <button ng-show="editing('name')" class="btn btn-success" type="button" ng-click="saveName()"><span class="fa fa-check fa-fw"></span></button>
                            <button ng-show="!editing('name')" class="btn btn-default" type="button" ng-click="edit('name')"><span class="fa fa-pencil fa-fw"></span></button>
                        </span>
                    </div>
                    <div class="form-control-static" ng-if="!canEdit('name')">
                        {{user.name}}
                    </div>
                </div>
            </div>
            <div class="form-group">
                <label for="inputRole" class="col-sm-2 control-label">
                    Role
                </label>
                <div class="col-sm-10">
                    <div class="input-group input-group-sm" ng-if="canEdit('role')">
                        <select ng-disabled="!editing('role')" class="form-control" id="inputRole" ng-options="role for role in availableRoles" ng-model="input.role"></select>
                        <span class="input-group-btn">
                            <button ng-show="editing('role')" class="btn btn-warning" type="button" ng-click="reset('role')"><span class="fa fa-times fa-fw"></span></button>
                            <button ng-show="editing('role')" class="btn btn-success" type="button" ng-click="saveRole()"><span class="fa fa-check fa-fw"></span></button>
                            <button ng-show="!editing('role')" class="btn btn-default" type="button" ng-click="edit('role')"><span class="fa fa-pencil fa-fw"></span></button>
                        </span>
                    </div>
                    <div class="form-control-static" ng-if="!canEdit('role')">
                        {{user.role}}
                    </div>
                </div>
            </div>
            <div class="form-group">
                <label for="inputRole" class="col-sm-2 control-label">
                    Color
                </label>
                <div class="col-sm-10">
                    <div class="form-control-static">
                        {{user.color}} <span class="fa fa-circle fa-fw" ng-style="{'color': user.color}"></span>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <div class="checkbox">
                        <label>
                            <input
                                    type="checkbox"
                                    ng-disabled="!isUser()"
                                    id="inputBanned"
                                    ng-model="input.banned"
                                    ng-change="saveBanned()"/> Banned
                        </label>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <div class="checkbox">
                        <label>
                            <input
                                    type="checkbox"
                                    id="inputRenameAvailable"
                                    ng-model="input.renameAvailable"
                                    ng-change="saveRenameAvailable()"/> Can change name
                        </label>
                    </div>
                </div>
            </div>
        </form>
    </div>
    <div class="modal-footer">
        <#if user.role == "SUPERADMIN">
            <div ng-disabled="user && !canEdit('name')" ng-click="requestDelete()" class="btn btn-danger pull-left">Delete user</div>
        </#if>
        <div class="btn btn-warning" ng-click="closeModal()">Close</div>
    </div>
</script>

<script type="text/ng-template" id="chatters.html">
    <div class="modal-header">
        <h3 class="modal-title">
            Chatters for room {{room.name}} <small>page {{page+1}}/{{totalPages+1}}</small>
            <div class="pull-right" style="max-width: 200px">
                <form ng-submit="doSearch()">
                    <div class="input-group">
                        <input type="text" class="form-control" placeholder="Search" ng-model="searchInput">
                        <span class="input-group-btn">
                            <button type="button" class="btn btn-danger" ng-if="search" ng-click="resetSearch()">
                                <span class="fa fa-times fa-fw"></span>
                            </button>
                            <button type="submit" class="btn btn-default">
                                <i class="fa fa-fw fa-search"></i>
                            </button>
                        </span>
                    </div>
                </form>
            </div>
        </h3>
    </div>

    <div class="modal-body">
        <div class="panel panel-default">
            <table class="table table-hover" ng-if="users.length > 0">
                <thead>
                <tr>
                    <th style="width:100px">
                        Id
                    </th>
                    <th>
                        User
                    </th>
                    <th>
                        Role
                    </th>
                    <th style="width:70px">
                        Banned
                    </th>
                </tr>
                </thead>
                <tr ng-repeat="user in users">
                    <td>{{user.id}}</td>
                    <td>
                        <a href="/admin/users?page=0&search={{user.userName}}" ng-click="showUser(user.userId, $event)">{{user.userName}}</a>
                    </td>
                    <td>
                        {{user.role}}
                    </td>
                    <td style="text-align: center;">
                        <span>
                            <span class="fa fa-fw" ng-class="{'fa-check-square-o': user.banned, 'fa-square-o': !user.banned}"></span>
                        </span>
                    </td>
                </tr>
            </table>
            <div class="panel-body" ng-if="users.length === 0">
                <div class="alert alert-warning" role="alert">Nothing to show.</div>
            </div>
        </div>
    </div>
    <div class="modal-footer" ng-if="(page !== 0) || hasNextPage()">
        <ul class="pager">
            <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
            <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
        </ul>
    </div>
</script>

<script type="text/ng-template" id="online.html">
    <div class="col-sm-8">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <i class="fa fa-users"></i> chatters
                </h4>
            </div>
            <div class="panel-body" ng-if="connections.length === 0">
                <div class="alert alert-info">
                    nothing to show
                </div>
            </div>
            <table class="table table-hover" ng-if="connections.length > 0">
                <thead>
                <tr>
                    <th ng-click="orderBy('user.name')" class="sortableTitle">
                        Name <span class="fa fa-fw pull-right" ng-class="getSortIconClass('user.name')"></span>
                    </th>
                    <th ng-click="orderBy('user.role')" class="sortableTitle">
                        Role <span class="fa fa-fw pull-right" ng-class="getSortIconClass('user.role')"></span>
                    </th>
                    <th>Ip</th>
                    <th>Banned</th>
                    <th>Color</th>
                </tr>
                </thead>
                <tr ng-repeat="c in connections | orderBy:orderVar:orderDesc">
                    <td>
                        <a href="/admin/users?search={{c.user.name}}&page=0" ng-click="userModal(c.user.id, $event)">{{c.user.name}}</a>
                    </td>
                    <td>{{c.user.role}}</td>
                    <td>
                        {{c.ip}}
                    <span class="btn btn-link btn-link-danger btn-xs pull-right" ng-if="!isBlocked(c.ip)" ng-click="blockIp(c.ip)">
                        <span class="fa fa-ban fa-fw"></span>
                    </span>
                    <span class="btn btn-link btn-link-success btn-xs pull-right" ng-if="isBlocked(c.ip)" ng-click="unblockIp(c.ip)">
                        <span class="fa fa-check fa-fw"></span>
                    </span>
                    </td>
                    <td>
                        {{c.user.banned ? "YES" : "NO"}}
                    </td>
                    <td ng-style="{'color': user.color}">{{c.user.color}}</td>
                </tr>
            </table>
        </div>
    </div>

    <div class="col-sm-4">
        <div class="panel panel-danger">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <i class="fa fa-user-times"></i> block IP address
                </h4>
            </div>
            <div class="panel-body">
                <div class="input-group">
                    <input type="text" class="form-control" ng-model="ipInput">
                <span class="input-group-btn">
                    <button class="btn btn-default" type="button" ng-click="blockIp(ipInput)">add</button>
                </span>
                </div>
            </div>
            <table class="table table-hover">
                <tr ng-repeat="ip in blockedIps">
                    <td>
                        {{ip}}
                <span class="btn btn-link btn-xs pull-right" ng-click="unblockIp(ip)">
                    <span class="fa fa-times fa-fw"></span>
                </span>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</script>

<script type="text/ng-template" id="polls.html">
    <div class="modal-header">
        <h3 class="modal-title">Past polls for room {{room.name}}</h3>
    </div>
    <div class="modal-body">
        <div class="list-group">
            <div ng-repeat="poll in polls" class="list-group-item">
                <h4 class="list-group-item-heading">{{poll.poll.question}}</h4>
                <p class="list-group-item-text">
                    <div ng-repeat="option in poll.poll.options">
                        <small>{{option.text}}</small>
                        <progressbar value="poll.votes[option.optionId]" max="poll.maxPollVotes" type="success">{{poll.votes[option.optionId]}}</progressbar>
                    </div>
                </p>
            </div>
        </div>
    </div>
</script>

<script type="text/ng-template" id="compose_poll.html">
    <div class="modal-header">
        <h3 class="modal-title">Create poll</h3>
    </div>
    <form name="form" class="form" id="form" ng-submit="submit()">
        <div class="modal-body">
            <div ng-if="error" class="alert alert-danger" role="alert">{{error}}</div>
            <div class="form-group" ng-class="{'has-error': form.question.$invalid && form.question.$dirty, 'has-success': !form.question.$invalid}">
                <label for="question" class="control-label">Question</label>
                <input
                        ng-model="input.question"
                        id="question"
                        type="text"
                        class="form-control"
                        name="question"
                        placeholder="question"
                        minlength="4"
                        maxlength="40"
                        required
                        />
            </div>
            <div ng-repeat="option in input.option" class="form-group" ng-class="{'has-error': optionForm.option.$invalid && optionForm.option.$dirty, 'has-success': !optionForm.option.$invalid}">
                <ng-form name="optionForm">
                    <label for="option" class="control-label">Option &num;{{$index}}</label>
                    <span ng-show="input.option.length > 2" class="btn btn-link btn-link-danger pull-right" ng-click="removeOption($index)">remove option</span>
                    <input
                            ng-model="input.option[$index].value"
                            type="text"
                            class="form-control"
                            name="option"
                            placeholder="option"
                            minlength="1"
                            maxlength="10"
                            required
                            />
                </ng-form>
            </div>
            <div class="btn btn-success" ng-click="addOption()">Add option</div>
        </div>
        <div class="modal-footer">
            <input ng-disabled="busy || form.$invalid || (input.option.length < 2) || (input.option.length > 5)" type="submit" class="btn btn-primary" value="submit"/>
            <button class="btn btn-warning" ng-click="cancel()">Cancel</button>
        </div>
    </form>
</script>

<script type="text/ng-template" id="room.html">
    <div>
        <div class="col-md-7">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h4 class="panel-title">
                        <i class="fa fa-fw fa-comment-o"></i> recent messages
                    </h4>
                </div>
                <table class="table table-hover table-striped">
                    <tr ng-repeat="entry in messages" ng-class="{'warning': entry.hidden, 'info': ((entry.type==='CLEAR') || (entry.type==='BAN') || (entry.type==='TIMEOUT'))}">
                        <td style="width:120px">
                            <abbr title="{{entry.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{entry.timestamp | relativeDate}}</abbr>
                        </td>
                        <td style="word-break: break-all" ng-if="(entry.type==='MSG') || (entry.type==='ME') || (entry.type==='MSG_EXT')">
                            <strong>&lt;{{entry.name}}&gt;</strong>
                            {{entry.message}}
                        </td>
                        <td ng-if="(entry.type==='CLEAR') || (entry.type==='BAN') || (entry.type==='TIMEOUT')">
                            {{entry.name}} cleared messages of {{entry.message}}
                        </td>
                    </tr>
                </table>
                <div class="panel-footer">
                    <div class="btn btn-primary" ng-click="showHistory()">all messages</div>
                </div>
            </div>
        </div>
        <div class="col-md-5">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h4 class="panel-title">
                        <i class="fa fa-fw fa-bar-chart"></i> current poll<span ng-if="poll.poll">: <strong>{{poll.poll.question}}</strong></span>
                    </h4>
                </div>
                <div class="panel-body">
                    <div ng-if="!poll.poll" class="alert alert-info" role="alert">There is no open poll at the moment.</div>
                    <div ng-if="poll.poll" ng-repeat="option in poll.poll.options">
                        <small>{{option.text}}</small>
                        <progressbar value="poll.votes[option.optionId]" max="maxPollVotes" type="success">{{poll.votes[option.optionId]}}</progressbar>
                    </div>
                </div>
                <div class="panel-footer">
                    <div class="btn btn-primary" ng-if="!poll.poll" ng-click="composePoll()">create poll</div>
                    <div class="btn btn-warning" ng-if="poll.poll" ng-click="closePoll()">close poll</div>
                    <div class="btn btn-primary pull-right" ng-click="showPolls()">view old polls</div>
                </div>
            </div>
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h4 class="panel-title">
                        <i class="fa fa-fw fa-bullhorn"></i> announcements
                    </h4>
                </div>
                <div class="panel-body" ng-if="announcements.length === 0">
                    <div class="alert alert-info" role="alert">
                        There are no announcements at the moment.
                    </div>
                </div>
                <table class="table table-hover" ng-if="announcements.length > 0">
                    <thead>
                    <tr>
                        <th>
                            Announcement
                        </th>
                        <th style="width: 50px">
                            remove
                        </th>
                    </tr>
                    </thead>
                    <tr ng-repeat="announcement in announcements | orderBy:id">
                        <td>
                            <div class="announcementContainer" ng-bind-html="safe(announcement.text)">
                            </div>
                        </td>
                        <td style="text-align: center;">
                            <div class="btn btn-link btn-link-danger btn-xs" ng-click="setAnnouncementInactive(announcement.id)">
                                <span class="fa fa-trash fa-fw"></span>
                            </div>
                        </td>
                    </tr>
                </table>
                <div class="panel-footer">
                    <div class="btn btn-primary" ng-click="composeAnnouncement()">create announcement</div>
                </div>
            </div>
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h4 class="panel-title">
                        <i class="fa fa-fw fa-users"></i> online chatters
                    </h4>
                </div>
                <table class="table table-hover" ng-if="chatters.length > 0">
                    <thead>
                        <tr>
                            <th style="width:65px">
                                Id
                            </th>
                            <th>
                                User
                            </th>
                            <th style="width:65px">
                                Role
                            </th>
                            <th style="width:65px">
                                Banned
                            </th>
                        </tr>
                    </thead>
                    <tr ng-repeat="user in chatters | orderBy:'id'">
                        <td>{{user.id}}</td>
                        <td>
                            <a href="/admin/users?search={{user.name}}&page=0" ng-click="showUser(user.userId, $event)">{{user.name}}</a>
                        </td>
                        <td>
                            {{user.role}}
                        </td>
                        <td style="text-align: center;">
                            <span>
                                <span class="fa fa-fw" ng-class="{'fa-check-square-o': user.banned, 'fa-square-o': !user.banned}"></span>
                            </span>
                        </td>
                    </tr>
                </table>
                <div class="panel-footer">
                    <div class="btn btn-primary" ng-click="showChatters()">all chatters</div>
                </div>
            </div>
        </div>
    </div>
</script>

<script type="text/ng-template" id="compose_announcement.html">
    <div class="modal-header">
        <h3 class="modal-title">Compose announcement</h3>
    </div>
    <form ng-submit="submitForm()" class="form-horizontal">
        <div class="modal-body">
            <div class="form-group">
                <label class="col-sm-3 control-label">Announcement</label>
                <div class="col-sm-9">
                    <textarea
                            style="resize:vertical;"
                            class="form-control"
                            ng-model="input.text"
                            required
                            />
                </div>
            </div>
        </div>
        <div class="modal-footer">
            <input type="submit" class="btn btn-primary" value="submit"/>
            <input type="button" class="btn btn-warning" value="cancel" ng-click="close()"/>
        </div>
    </form>
</script>

<div id="body">
    <div class="container-fluid">
        <div class="row">
            <div class="sidebar">
                <ul class="nav nav-sidebar">
                    <li ng-class="{'active': menuId === 'dashboard'}">
                        <a href="/admin/"><i class="fa fa-fw fa-dashboard"></i> dashboard</a>
                    </li>
                <#if user.role == "SUPERADMIN">
                    <li ng-class="{'active': menuId === 'online'}">
                        <a href="/admin/online"><i class="fa fa-fw fa-list"></i> online users</a>
                    </li>
                    <li ng-class="{'active': menuId === 'journal'}">
                        <a href="/admin/journal?page=0"><i class="fa fa-fw fa-book"></i> journal</a>
                    </li>
                </#if>
                </ul>
                <ul class="nav nav-sidebar">
                    <li ng-class="{'active': menuId === 'emoticons'}">
                        <a href="/admin/emoticons"><i class="fa fa-fw fa-smile-o"></i> emoticons</a>
                    </li>
                    <li ng-class="{'active': menuId === 'users'}">
                        <a href="/admin/users?page=0"><i class="fa fa-fw fa-users"></i> users</a>
                    </li>
                <#if user.role == "SUPERADMIN">
                    <li ng-class="{'active': menuId === 'tickets'}">
                        <a href="/admin/tickets?page=0">
                            <i class="fa fa-fw fa-ticket"></i> tickets<span class="badge pull-right">{{getOpenTicketCount()}}</span>
                        </a>
                    </li>
                    <li ng-class="{'active': menuId === 'services'}">
                        <a href="/admin/services">
                            <i class="fa fa-fw fa-cogs"></i> services
                        </a>
                    </li>
                </#if>
                </ul>
            </div>
            <div class="main">
                <div class="page-header">
                    <h1 ng-controller="TitleController" class="mainTitle">
                        {{title() | inflector:'capital'}}
                        <span class="small secondaryTitle" ng-show="secondary()">{{secondary()}}</span><!--
                     --><span class="btn btn-link" ng-click="reloadCurrentRoute()">
                            <i class="fa fa-fw fa-refresh"></i>
                        </span>
                    </h1>
                </div>
                <div class="mainContent" ng-view></div>
            </div>
        </div>
    </div>

    <div class="alertOverlay" style="width: 200px; padding-left: 15px; padding-right:15px" ng-controller="AlertController">
        <div ng-repeat="alert in list() | limitTo:2" class="alert" ng-class="'alert-' + alert.type">
            {{alert.message}}
        </div>
    </div>
</div>
</body>
</html>