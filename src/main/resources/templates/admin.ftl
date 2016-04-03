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
    <script src="/vendor/js/angular.js"></script>
    <script src="/vendor/js/angular-touch.js"></script>
    <script src="/vendor/js/angular-sanitize.js"></script>
    <script src="/vendor/js/angular-animate.2.js"></script>
    <script src="/vendor/js/angular-route.js"></script>
    <script src="/vendor/js/angular-ui-utils.js"></script>
    <script src="/vendor/js/angular-ui-bootstrap.2.js"></script>
    <script src="/vendor/js/angular-relative-date.js"></script>
    <script src="/vendor/js/angular-datetimepicker.js"></script>
    <script src="/vendor/js/angular-slider.js"></script>
    <script src="/vendor/js/range-picker.js"></script>
    <script src="/vendor/js/highcharts.js"></script>
    <script src="/vendor/js/highcharts-ng.js"></script>
    <script src="/js/admin/main.js"></script>
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
                <form action="/rest/emoticons/add" method="post" enctype="multipart/form-data" role="form" acceptcharset="UTF-8">
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
        <div class="list-group">
            <div class="list-group-item" ng-repeat="entry in entries" ng-class="getClassForJournalAction(entry.action)">
                <h4 class="list-group-item-heading">
                    {{translateAction(entry.action)}}
                    <small class="" ng-if="entry.admin">
                        <i class="fa fa-fw fa-wrench" tooltip="admin"></i><!--
                        --><a href="" ng-click="showUser(entry.admin.id)">{{entry.admin.name}}</a>
                    </small>
                    <small class="" ng-if="entry.user">
                        <i class="fa fa-fw fa-user" tooltip="user"></i><!--
                        --><a href="" ng-click="showUser(entry.user.id)">{{entry.user.name}}</a>
                    </small>
                    <small class="pull-right">
                        <a href="" ng-click="showBanContext(entry.time)" ng-if="entry.action==='ROOM_BAN'"><!--
                                    --><span class="fa fa-fw fa-comments"></span><!--
                                --></a>
                        <abbr title="{{entry.time | date:'dd.MM.yyyy HH:mm'}}">{{entry.time | relativeDate}}</abbr>
                    </small>
                </h4>
                <p class="list-group-item-text">
                    <div class="" ng-if="entry.actionDescription">
                        <div ng-switch="entry.action">
                            <div ng-switch-when="NEW_EMOTICON">
                                <img ng-src="/emoticons/{{entry.actionDescription.fileName}}">
                                <code ng-bind="entry.actionDescription.code"></code>
                            </div>
                            <div ng-switch-when="IMAGE_EMOTICON">
                                <img ng-src="/emoticons/{{entry.actionDescription.oldImage}}">
                                <span class="fa fa-long-arrow-right"></span>
                                <img ng-src="/emoticons/{{entry.actionDescription.newImage}}">
                                <code ng-bind="entry.actionDescription.code"></code>
                            </div>
                            <div ng-switch-when="DELETED_EMOTICON">
                                <img ng-src="/emoticons/{{entry.actionDescription.fileName}}">
                                <code ng-bind="entry.actionDescription.code"></code>
                            </div>
                            <div ng-switch-when="DELETED_ROOM">
                                <code ng-bind="entry.actionDescription.name"></code>
                            </div>
                            <div ng-switch-when="NEW_ROOM">
                                <code ng-bind="entry.actionDescription.name"></code>
                            </div>
                            <div ng-switch-when="NAME_CHANGE">
                                <code ng-bind="entry.actionDescription.oldName"></code>
                                <span class="fa fa-long-arrow-right"></span>
                                <code ng-bind="entry.actionDescription.newName"></code>
                            </div>
                            <div ng-switch-when="USER_UPDATE">
                                <table class="table">
                                    <thead>
                                    <tr>
                                        <th class="col-xs-6">
                                            attribute
                                        </th>
                                        <th class="col-xs-6">
                                            value
                                        </th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <tr ng-repeat="(k,v) in entry.actionDescription.oldState"
                                        ng-if="entry.actionDescription.newState.hasOwnProperty(k)">
                                        <td ng-bind="k"></td>
                                        <td>
                                            {{v}}
                                            <span class="fa fa-long-arrow-right"></span>
                                            {{entry.actionDescription.newState[k]}}
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                            <span ng-switch-default ng-bind="entry.actionDescription | json"></span>
                        </div>
                    </div>
                </p>
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
                                from <a ng-href="/admin/users?search={{entry.user.name}}" ng-click="showUser(entry.user.id, $event)">{{entry.user.name}}</a>
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


<script type="text/ng-template" id="history.html">
    <div class="modal-header">
        <h3 class="modal-title">
            History for room {{room.name}} <small>page <a href="" ng-click="goToPage()">{{page+1}}</a>/{{totalPages}}</small>
            <div class="input-group pull-right" style="max-width:200px;">
                <input ng-model="input.user" type="text" class="form-control" placeholder="User filter">
                <span class="input-group-btn">
                    <div class="btn btn-default" type="button" ng-click="addUserFilter(input.user)">
                        <i class="fa fa-fw fa-search"></i>
                    </div>
                    <div class="btn btn-default" type="button" ng-click="pickRange()">
                        <i class="fa fa-fw fa-calendar"></i>
                    </div>
                </span>
            </div>
            <small ng-if="since || until">
                <br/>
                <span ng-if="since">since {{since|date:'dd.MM.yyyy HH:mm'}} </span>
                <span ng-if="until">until {{until|date:'dd.MM.yyyy HH:mm'}} </span>
            </small>
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
        <div ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
            <br/>
        </div>
        <div class="list-group">
            <div class="list-group-item"
                 style="word-break: break-word"
                 ng-repeat="message in entries"
                 ng-class="{'list-group-item-warning': message.hidden, 'list-group-item-info': ((message.type==='CLEAR') || (message.type==='BAN') || (message.type==='TIMEOUT'))}">
                <small>
                    <abbr title="{{message.timestamp | date:'dd.MM.yyyy HH:mm'}}">{{message.timestamp | relativeDate}}</abbr>
                </small>
                <strong class="list-group-item-heading">
                    &lt;{{message.userName}}&gt;
                </strong>
                <span
                        class="list-group-item-text"
                        ng-bind-html="message.message | message"
                        ng-if="(message.type==='MSG') || (message.type==='ME') || (message.type==='MSG_EXT')"></span>
                <span class="list-group-item-text" ng-if="(message.type==='CLEAR') || (message.type==='BAN') || (message.type==='TIMEOUT')">
                    cleared messages of {{message.message}}
                </span>
            </div>
        </div>
        <div class="" ng-if="(page !== 0) || hasNextPage()">
            <ul class="pager">
                <li class="previous" ng-if="page !== 0" ng-click="previousPage()"><a href="">&larr; Previous page</a></li>
                <li class="next" ng-if="hasNextPage()" ng-click="nextPage()"><a href="">Next page &rarr;</a></li>
            </ul>
        </div>
    </div>
</script>

<script type="text/ng-template" id="services.html">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <h4 class="panel-title">
                Runtime statistics
            </h4>
        </div>
        <ul class="list-group" ng-if="data">
            <li class="list-group-item">
                <h4 class="list-group-item-heading">JVM information</h4>
                <div class="list-group-item-text">
                    <table class="table">
                        <tr>
                            <td class="col-xs-2">
                                JVM
                            </td>
                            <td class="col-xs-10" ng-bind="getMetricValueByName('vendor')">
                            </td>
                        </tr>
                        <tr>
                            <td class="col-xs-2">
                                Socket implementation
                            </td>
                            <td class="col-xs-10" ng-bind="getMetricValueByName('socket.implementation')">
                            </td>
                        </tr>
                        <tr>
                            <td class="col-xs-2">
                                SSL implementation
                            </td>
                            <td class="col-xs-10" ng-bind="getMetricValueByName('ssl.implementation')">
                            </td>
                        </tr>
                        <tr>
                            <td class="col-xs-2">
                                SSL ciphersuites
                            </td>
                            <td class="col-xs-10">
                                <table class="table">
                                    <tr ng-repeat="cipher in getMetricValueByName('ssl.cipherSuites')">
                                        <td ng-bind="cipher">

                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                Running since
                            </td>
                            <td ng-bind="getUptime() | date:'dd.MM.yyyy HH:mm'">
                            </td>
                        </tr>
                        <tr>
                            <td>
                                Heap
                            </td>
                            <td>
                                <progressbar class="progress"
                                        max="getMetricValueByName('heap.committed')"
                                        value="getMetricValueByName('heap.used')">
                                    {{getMetricValueByName('heap.used')/1048576 | number:0}}/{{getMetricValueByName('heap.committed')/1048576 | number:0}} MB
                                </progressbar>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                Non-heap
                            </td>
                            <td>
                                <progressbar class="progress"
                                             max="getMetricValueByName('non-heap.committed')"
                                             value="getMetricValueByName('non-heap.used')">
                                    {{getMetricValueByName('non-heap.used')/1048576 | number:0}}/{{getMetricValueByName('non-heap.committed')/1048576 | number:0}} MB
                                </progressbar>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                Total
                            </td>
                            <td>
                                <progressbar class="progress"
                                             max="getMetricValueByName('total.committed')"
                                             value="getMetricValueByName('total.used')">
                                    {{getMetricValueByName('total.used')/1048576 | number:0}}/{{getMetricValueByName('total.committed')/1048576 | number:0}} MB
                                </progressbar>
                            </td>
                        </tr>
                    </table>
                </div>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('connection-pool.pool.ConnectivityCheck') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('connection-pool.pool.ConnectivityCheck'), 'fa-times' : !isHealthy('connection-pool.pool.ConnectivityCheck')}"
                            ></span> Database connection pool
                </h4>
                <table class="table">
                    <tr>
                        <td class="col-xs-2">
                            Pool usage
                        </td>
                        <td class="col-xs-10">
                            <progressbar class="progress"
                                         max="getMetricValueByName('connection-pool.pool.TotalConnections')"
                                         value="getMetricValueByName('connection-pool.pool.ActiveConnections')">
                                {{getMetricValueByName('connection-pool.pool.ActiveConnections')}}/{{getMetricValueByName('connection-pool.pool.TotalConnections')}}
                            </progressbar>
                        </td>
                    </tr>
                    <tr>
                        <td class="col-xs-2">
                            Connection wait
                        </td>
                        <td class="col-xs-10">
                            <table class="table table-condensed">
                                <tr>
                                    <td class="col-xs-2">
                                        min
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Wait').min | number}} ms
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        max
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Wait').max | number}} ms
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        mean
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Wait').mean | number}} ms
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="col-xs-2">
                            In use
                        </td>
                        <td class="col-xs-10">
                            <table class="table table-condensed">
                                <tr>
                                    <td class="col-xs-2">
                                        min
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Usage').min | number}} ms
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        max
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Usage').max | number}} ms
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        mean
                                    </td>
                                    <td>
                                        {{getMetricByName('connection-pool.pool.Usage').mean | number}} ms
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('httpServer') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('httpServer'), 'fa-times' : !isHealthy('httpServer')}"
                            ></span> Http server <small>running on port {{getMetricValueByName('httpServer.port')}}</small>
                </h4>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('websocketServer') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('websocketServer'), 'fa-times' : !isHealthy('websocketServer')}"
                            ></span> Websocket server <small>running on port {{getMetricValueByName('websocketServer.port')}}</small>
                </h4>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('ircServer') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('ircServer'), 'fa-times' : !isHealthy('ircServer')}"
                            ></span> IRC server <small>running on port {{getMetricValueByName('ircServer.port')}}</small>
                </h4>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('messageReactor') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('messageReactor'), 'fa-times' : !isHealthy('messageReactor')}"
                            ></span> Message reactor
                </h4>
                <table class="table">
                    <tr>
                        <td class="col-xs-2">
                            Queue load
                        </td>
                        <td class="col-xs-10">
                            <progressbar class="progress"
                                         max="getQueueSize('messageReactor')"
                                         value="getQueueLoad('messageReactor')">
                                {{getQueueLoad('messageReactor')}}/{{getQueueSize('messageReactor')}}
                            </progressbar>
                        </td>
                    </tr>
                </table>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('messageBroadcaster') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('messageBroadcaster'), 'fa-times' : !isHealthy('messageBroadcaster')}"
                            ></span> Message broadcaster
                </h4>
                <table class="table">
                    <tr>
                        <td class="col-xs-2">
                            Queue load
                        </td>
                        <td class="col-xs-10">
                            <progressbar class="progress"
                                         max="getQueueSize('messageBroadcaster')"
                                         value="getQueueLoad('messageBroadcaster')">
                                {{getQueueLoad('messageBroadcaster')}}/{{getQueueSize('messageBroadcaster')}}
                            </progressbar>
                        </td>
                    </tr>
                </table>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('authenticationService') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('authenticationService'), 'fa-times' : !isHealthy('authenticationService')}"
                            ></span> Authentication service
                </h4>
                <table class="table">
                    <tr>
                        <td class="col-xs-2">
                            Queue load
                        </td>
                        <td class="col-xs-10">
                            <progressbar class="progress"
                                         max="getQueueSize('authenticationService')"
                                         value="getQueueLoad('authenticationService')">
                                {{getQueueLoad('authenticationService')}}/{{getQueueSize('authenticationService')}}
                            </progressbar>
                        </td>
                    </tr>
                </table>
            </li>
            <li class="list-group-item">
                <h4 class="list-group-item-heading">
                    <span
                            class="fa fa-fw"
                            ng-style="{'color': isHealthy('eventDispatcher') ? 'green' : 'red'}"
                            ng-class="{'fa-check' : isHealthy('eventDispatcher'), 'fa-times' : !isHealthy('eventDispatcher')}"
                            ></span> Event dispatcher
                </h4>
                <table class="table">
                    <tr>
                        <td class="col-xs-2">
                            Queue load
                        </td>
                        <td class="col-xs-10">
                            <progressbar class="progress"
                                         max="getQueueSize('notificationService')"
                                         value="getQueueLoad('notificationService')">
                                {{getQueueLoad('notificationService')}}/{{getQueueSize('notificationService')}}
                            </progressbar>
                        </td>
                    </tr>
                </table>
            </li>
        </ul>
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
                                <span class="auth default fa fa-fw fa-key" ng-class="{'active': hasAuth('password')}" tooltip="Password"></span>
                                <span class="auth fa fa-fw fa-twitch" ng-class="{'active': hasAuth('twitch.tv')}" tooltip="{{auth['twitch.tv']}}"></span>
                                <span class="auth default fa fa-fw fa-globe" ng-class="{'active': hasAuth('token')}" tooltip="API access token"></span>
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
                <div class="btn btn-link" ng-click="changePassword()" ng-if="user.role !== 'SUPERADMIN'">
                    <i class="fa fa-fw fa-key"></i> change password
                </div>
                <hr/>
                <div class="btn btn-link" ng-click="showActivity()">
                    <i class="fa fa-fw fa-bar-chart"></i> Show user activity
                </div>
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

<script type="text/ng-template" id="new_proxy.html">
    <div class="modal-header">
        <h3 class="modal-title">New proxy</h3>
    </div>
    <form ng-submit="submitForm()">
        <div class="modal-body">
            <div class="alert alert-danger" ng-if="error" ng-bind="error"></div>
            <div class="form-group">
                <label class="control-label" for="provider">Provider</label>
                <select
                        class="form-control"
                        id="provider"
                        ng-options="item as item.name for item in providers track by item.name"
                        ng-model="input.provider"
                        ng-change="reset()"
                        required
                        >
                    <option style="display: none" value="">Select proxy provider</option>
                </select>
            </div>
            <div class="form-group" ng-if="input.provider">
                <label class="control-label" for="roomName">Room name</label>
                <input
                        type="text"
                        class="form-control"
                        id="roomName"
                        ng-model="input.room"
                        required
                        ></input>
            </div>
            <div class="form-group" ng-if="input.provider && input.provider.supportsAuthentication">
                <div class="checkbox">
                    <label>
                        <input type="checkbox" ng-model="input.authentication"> authenticate (needed for moderation)
                    </label>
                </div>
            </div>
            <div class="form-group" ng-if="input.authentication">
                <label class="control-label" for="username">Username</label>
                <input
                        type="text"
                        class="form-control"
                        id="username"
                        ng-model="input.name"
                        required
                        ></input>
            </div>
            <div class="form-group" ng-if="input.authentication">
                <label class="control-label" for="key">Password/token</label>
                <input
                        type="password"
                        class="form-control"
                        id="key"
                        ng-model="input.key"
                        required
                        ></input>
            </div>
            <div class="form-group" ng-if="input.provider && input.provider.supportsOutbound">
                <div class="checkbox">
                    <label>
                        <input type="checkbox" ng-model="input.outbound"> enable outbound passing
                    </label>
                </div>
            </div>
        </div>
        <div class="modal-footer">
            <input type="button" class="btn btn-warning pull-left" value="cancel" ng-click="cancel()"/>
            <input type="submit" class="btn btn-primary" value="submit"/>
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
                    <li ng-class="{'active': menuId === 'tickets'}">
                        <a href="/admin/tickets?page=0">
                            <i class="fa fa-fw fa-ticket"></i> tickets<span class="badge pull-right">{{getOpenTicketCount()}}</span>
                        </a>
                    </li>
                <#if user.role == "SUPERADMIN">
                    <li ng-class="{'active': menuId === 'services'}">
                        <a href="/admin/services">
                            <i class="fa fa-fw fa-cogs"></i> runtime information
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
