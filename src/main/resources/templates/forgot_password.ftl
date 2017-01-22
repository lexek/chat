<!DOCTYPE HTML>

<html>
<head>
    <title>Password reset</title>
    <link rel="stylesheet" href="/vendor/css/bootstrap.css"/>
    <link rel="stylesheet" href="/css/font-awesome.css"/>
    <style>
        html {
            position: relative;
            min-height: 100%;
        }

        .page-header {
            word-break: break-all;
        }

        .panel-heading {
            border-radius: 0;
        }
    </style>
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
<div class="col-lg-offset-4 col-lg-4 col-md-offset-3 col-md-6 col-sm-offset-2 col-sm-8 col-xs-offset-1 col-xs-10">
    <div class="page-header">
        <h3>Change password</h3>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading">
            <h4 class="panel-title"><i class="fa fa-fw fa-key"></i> Change password</h4>
        </div>
        <div class="panel-body">
        <#if error?has_content>
            <div class="alert alert-danger" role="alert">${error}</div>
        </#if>
            <form class="form" id="authForm" method="post" action="/rest/auth/forgotPassword">
                <input type="hidden" value="${token?html}" name="token"/>
                <input type="hidden" value="${uid?html}" name="uid"/>
                <div class="form-group">
                    <label class="control-label">New password</label>
                    <input type="password" class="form-control" name="password" placeholder="New password" required/>
                </div>
                <input type="submit" class="btn btn-primary" value="Create account"/>
            </form>
        </div>
    </div>
</div>
</body>
</html>
