<!DOCTYPE HTML>

<html>
<head>
    <title>Email confirmation.</title>
    <link rel="stylesheet" href="/vendor/css/bootstrap.css" />
    <style>
        html {
            position: relative;
            min-height: 100%;
        }

        .page-header {
            word-break: break-all;
        }
    </style>
</head>
<body>
<div class="col-lg-offset-4 col-lg-4 col-md-offset-3 col-md-6 col-sm-offset-2 col-sm-8 col-xs-offset-1 col-xs-10">
    <div class="page-header">
        <#if success>
            <h3>Your email is successfuly confirmed.</h3>
        </#if>

        <#if !success>
            <h3>Unknown code.</h3>
        </#if>
    </div>
</div>
</body>
</html>
