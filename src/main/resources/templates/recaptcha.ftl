<!DOCTYPE HTML>

<html>
<head>
    <title>Captcha</title>
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
        <h3>Enter captcha to continue</h3>
    </div>
    <div class="row">
        <div class="well" style="word-wrap: break-word">
            <form action="/recaptcha/${id}" method="POST">
                <div class="g-recaptcha" data-sitekey="6Lepxv4SAAAAAMFC4jmtZvnzyekEQ3XuX0xQ-3TB"></div>
                <br/>
                <input type="submit" value="Submit">
            </form>
        </div>
        <div class="page-footer">
            Kappa server 1.3.3.7
        </div>
    </div>
</div>
</body>
</html>
