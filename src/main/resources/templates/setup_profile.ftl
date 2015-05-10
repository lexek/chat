<!DOCTYPE HTML>

<html>
<head>
    <title>Create user</title>
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
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
<div class="col-lg-offset-4 col-lg-4 col-md-offset-3 col-md-6 col-sm-offset-2 col-sm-8 col-xs-offset-1 col-xs-10">
    <div class="page-header">
        <h3>Create user</h3>
    </div>
    <#if new_account_error?has_content>
        <div class="alert alert-danger" role="alert">${new_account_error}</div>
    </#if>
    <form class="form" id="authForm" method="post" action="/setup_profile?newAccount=true">
        <div class="form-group">
            <label class="control-label">Username</label>
            <input type="text" class="form-control" name="username" placeholder="Username" pattern='[a-zA-Z0-9]{3,16}'/>
        </div>
        <input type="submit" class="btn btn-primary" value="Create user"/>
    </form>
    <div class="page-header">
        <h3>I already have account</h3>
    </div>
    <#if login_error?has_content>
        <div class="alert alert-danger" role="alert">${login_error}</div>
    </#if>
    <form class="form" id="loginForm" method="post" action="/setup_profile?newAccount=false">
        <div class="form-group">
            <label class="control-label">Username</label>
            <input type="text" class="form-control" name="username" placeholder="Username" pattern='[a-zA-Z0-9]{3,16}'/>
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
</body>
</html>
