<!DOCTYPE HTML>

<html>
<head>
    <title>${status.reasonPhrase()}</title>
    <link rel="stylesheet" href="/css/bootstrap.css" />
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
        <h3>Response status ${status.code()}: ${status.reasonPhrase()}</h3>
    </div>
    <div class="row">
        <#if text?has_content>
            <div class="well" style="word-wrap: break-word">
                ${text}
            </div>
        </#if>
        <div class="page-footer">
            Kappa server 1.3.3.7
        </div>
    </div>
</div>
</body>
</html>
