<!DOCTYPE HTML>

<html>
<head>
    <title>Create user</title>
    <link rel="stylesheet" href="/vendor/css/bootstrap.css" />
    <link rel="stylesheet" href="/css/font-awesome.css" />
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
        <h3>One last thing</h3>
        If you don't have account here, you must create one. Otherwise you can link this social account to existing user.
    </div>

    <div class="panel panel-default">
        <div class="panel-heading">
            <h4 class="panel-title"><i class="fa fa-fw fa-user-plus"></i> Create account</h4>
        </div>
        <div class="panel-body">
            <#if new_account_error?has_content>
                <div class="alert alert-danger" role="alert">${new_account_error}</div>
            </#if>
            <form class="form" id="authForm" method="post" action="/rest/auth/social/setup_profile?newAccount=true">
                <div class="form-group">
                    <label class="control-label">Username</label>
                    <input type="text" class="form-control" name="username" placeholder="Username" pattern='[a-zA-Z][a-zA-Z0-9_]{2,16}'/>
                </div>
                <input type="submit" class="btn btn-primary" value="Create account"/>
            </form>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading">
            <h4 class="panel-title"><i class="fa fa-fw fa-sign-in"></i> Link to existing user</h4>
        </div>
        <div class="panel-body">
            <#if login_error?has_content>
                <div class="alert alert-danger" role="alert">${login_error}</div>
            </#if>
            <form class="form" id="loginForm" method="post" action="/rest/auth/social/setup_profile?newAccount=false">
                <div class="form-group">
                    <label class="control-label">Username</label>
                    <input type="text" class="form-control" name="username" placeholder="Username" pattern='[a-zA-Z][a-zA-Z0-9_]{2,16}'/>
                </div>
                <div class="form-group">
                    <label class="control-label">Password</label>
                    <input type="password" class="form-control" name="password" placeholder="Password" pattern=".{6,30}"/>
                </div>
                <#if captchaRequired>
                    <div class="g-recaptcha" data-sitekey="6Lepxv4SAAAAAMFC4jmtZvnzyekEQ3XuX0xQ-3TB"></div>
                </#if>
                <input type="submit" class="btn btn-primary" value="Sign in"/>
            </form>
        </div>
    </div>
</div>
</body>
</html>
