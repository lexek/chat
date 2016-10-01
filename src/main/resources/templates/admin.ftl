<!DOCTYPE HTML>
<html ng-app="AdminApplication">
<head>
    <title ng-controller="TitleController" ng-bind="'Admin panel: ' + title()"></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <link rel="stylesheet" type="text/css" href='//fonts.googleapis.com/css?family=Roboto:400,400italic,700,700italic&subset=latin,cyrillic'>
    <link rel="stylesheet" type="text/css" href="/vendor/css/animate.css"/>
    <link rel="stylesheet" type="text/css" href="/vendor/css/bootstrap.css"/>
    <link rel="stylesheet" type="text/css" href="/vendor/css/cal-heatmap.css" />
    <link rel="stylesheet" type="text/css" href="/vendor/css/range-picker.css" />
    <link rel="stylesheet" type="text/css" href="/vendor/css/angular-busy.css" />
    <link rel="stylesheet" type="text/css" href="/css/font-awesome.css">
    <link rel="stylesheet" type="text/css" href="/css/admin.css">

    <base href="/admin/" />

    <script type="application/javascript">
        document.SELF_ROLE = "${user.role}";
    </script>

    <script src="/vendor/js/jquery-2.1.0.min.js"></script>
    <script src="/vendor/js/d3.min.js" charset="utf-8"></script>
    <script src="/vendor/js/stringformatter.js"></script>
    <script src="/vendor/js/cal-heatmap.js"></script>
    <script src="/vendor/js/twemoji.js"></script>
    <script src="/vendor/js/angular-new.js"></script>
    <script src="/vendor/js/angular-touch.js"></script>
    <script src="/vendor/js/angular-sanitize.js"></script>
    <script src="/vendor/js/angular-animate-new.js"></script>
    <script src="/vendor/js/angular-route.js"></script>
    <script src="/vendor/js/angular-ui-utils.js"></script>
    <script src="/vendor/js/angular-ui-bootstrap.2.js"></script>
    <script src="/vendor/js/angular-relative-date.js"></script>
    <script src="/vendor/js/angular-datetimepicker.js"></script>
    <script src="/vendor/js/angular-slider.js"></script>
    <script src="/vendor/js/angular-busy.js"></script>
    <script src="/vendor/js/range-picker.js"></script>
    <script src="/vendor/js/highcharts.js"></script>
    <script src="/vendor/js/highcharts-ng.js"></script>
    <script src="/js/admin.min.js"></script>
</head>
<body>

<script type="text/ng-template" id="range_pick.html">
    <div class="modal-header">
        <h3 class="modal-title">
            Choose date range
        </h3>
    </div>
    <div class="modal-body">
        <div rg-range-picker="range" labels="labels"></div>
        <div class="row">
            <div class="col-xs-6">
                <input class="form-control" type="number" min="0" max="23" ng-model="hours.from"/>
            </div>
            <div class="col-xs-6">
                <input class="form-control" type="number" min="0" max="23" ng-model="hours.to"/>
            </div>
        </div>
    </div>
    <div class="modal-footer">
        <div class="btn btn-success" ng-click="ok()">ok</div>
    </div>
</script>

<script type="text/ng-template" id="journal.html">
    <global-journal/>
</script>

<script type="text/ng-template" id="tickets.html">
    <ul class="nav nav-tabs nav-justified">
        <li ng-class="{active: opened}">
            <a href="" ng-click="setOpened(true)">
                <span class="fa fa-exclamation-circle text-danger"></span> open
            </a>
        </li>
        <li ng-class="{active: !opened}">
            <a href="" ng-click="setOpened(false)">
                <span class="fa fa-check-circle text-success"></span> closed
            </a>
        </li>
    </ul>
    <div class="panel panel-default" style="border-top: 0">
        <div class="panel-body" ng-if="entries.length === 0">
            <div class="alert alert-info" >
                nothing to show
            </div>
        </div>
        <div class="list-group">
            <div class="list-group-item" ng-repeat="entry in entries" ng-controller="TicketController">
                <div>
                    <div ng-if="entry.ticket.isOpen" class="pull-right">
                        <div class="btn btn-lg" ng-class="{'btn-link-success': !showReply, 'btn-link-danger': showReply}" ng-click="toggleReply()">
                            <i class="fa fa-reply" ng-class="{'fa-reply': !showReply, 'fa-times': showReply}"></i>
                        </div>
                    </div>
                    <div>
                        <h4 class="list-group-item-heading">
                            {{entry.ticket.category}}
                            <small>
                                from <a ng-href="/admin/users?search={{entry.user.name}}" user-link="entry.user.id">{{entry.user.name}}</a>
                            </small>
                            <small class="pull-right">
                                <abbr title="{{entry.ticket.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{entry.ticket.timestamp | relativeDate}}</abbr>
                            </small>
                        </h4>
                        <div class="list-group-item-text">
                                <div>
                                    <strong>Text</strong>: <span ng-bind-html="entry.ticket.text | linky:'_blank'"></span>
                                </div>
                                <div ng-if="entry.ticket.adminReply">
                                    <strong>Reply from {{entry.closedBy}}</strong>: <span ng-bind-html="entry.ticket.adminReply | linky:'_blank'"></span>
                                </div>
                        </div>
                    </div>
                    <div ng-if="showReply" class="row">
                        <form name="form" class="col-xs-12" ng-submit="close(entry.ticket.id, text)">
                            <div class="input-group" ng-class="{'has-error': form.text.$invalid && form.text.$dirty, 'has-success': form.text.$dirty && !form.text.$invalid}">
                                <input
                                        class="form-control"
                                        required
                                        type="text"
                                        name="text"
                                        ng-model="text"
                                        style="resize: none;"
                                        minlength="0"
                                        maxlength="1024"
                                        ></input>
                                <div class="input-group-btn">
                                    <input ng-disabled="form.$invalid" ng-if="!submitting" type="submit" class="btn btn-primary" value="Submit"/>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        <div class="panel-footer" ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
        </div>
    </div>
</script>

<script type="text/ng-template" id="user_activity.html">
    <div class="modal-header">
        <h3 class="modal-title">
            <i class="fa fa-fw fa-user"></i> {{user.name}} activity
        </h3>
    </div>
    <div class="modal-body">
        <div id="userActivity"></div>
    </div>
</script>

<script type="text/ng-template" id="top_chatters.html">
    <div class="modal-header">
        <h3 class="modal-title">
            <i class="fa fa-fw fa-trophy"></i> {{room.name}} top chatters
        </h3>
    </div>
    <div class="modal-body">
        <div class="list-group">
            <a href="" ng-repeat="entry in entries" class="list-group-item" ng-click="showActivity(entry.userId, entry.name)">
                <h4 class="list-group-item-heading">
                    {{entry.name}}
                    <small class="pull-right">
                        {{entry.count}} messages
                    </small>
                </h4>
            </a>
        </div>
    </div>
</script>

<script type="text/ng-template" id="polls.html">
    <div class="modal-header">
        <h3 class="modal-title">Past polls for room {{room.name}}</h3>
    </div>
    <div class="modal-body">
        <div class="list-group">
            <div ng-repeat="poll in polls" class="list-group-item" ng-controller="PollController">
                <h4 class="list-group-item-heading">{{poll.poll.question}}</h4>
                <div class="list-group-item-text">
                    <div class="row">
                        <div class="col-xs-6">
                            <div ng-repeat="option in poll.poll.options">
                                <small>{{option.text}}</small>
                                <progressbar value="poll.votes[option.optionId]" max="poll.maxPollVotes" type="success">{{poll.votes[option.optionId]}}</progressbar>
                            </div>
                        </div>
                        <div class="col-xs-6">
                            <highchart config="chartConfig"></highchart>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal-footer" ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
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
                    <label for="option-{{$index}}" class="control-label">Option &num;{{$index}}</label>
                    <span ng-show="input.option.length > 2" class="btn btn-link btn-link-danger pull-right" ng-click="removeOption($index)">remove option</span>
                    <input
                            id="option-{{$index}}"
                            ng-model="input.option[$index].value"
                            type="text"
                            class="form-control"
                            name="option"
                            placeholder="option"
                            minlength="1"
                            maxlength="20"
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
            <div class="form-group">
                <div class="col-sm-offset-3 col-sm-9">
                    <div class="checkbox">
                        <label>
                            <input type="checkbox" ng-model="input.onlyBroadcast"> only broadcast
                        </label>
                    </div>
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
                    <li ng-class="{'active': menuId === 'services'}">
                        <a href="/admin/services">
                            <i class="fa fa-fw fa-cogs"></i> runtime information
                        </a>
                    </li>
                </#if>
                </ul>
                <ul class="nav nav-sidebar">
                    <li ng-class="{'active': menuId === 'tickets'}">
                        <a href="/admin/tickets?page=0">
                            <i class="fa fa-fw fa-ticket"></i> tickets<span class="badge pull-right"><ticket-count/></span>
                        </a>
                    </li>
                    <li ng-class="{'active': menuId === 'proxyAuth'}">
                        <a href="/admin/proxyAuth">
                            <i class="fa fa-fw fa-key"></i> proxy auth
                        </a>
                    </li>
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
