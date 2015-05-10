<!DOCTYPE HTML>

<html>
<head>
    <link rel="stylesheet" type="text/css" href="/css/style.css" media="all"/>
    <script>
        if(window.opener) {
            window.opener.postMessage("auth-notify", "https://"+document.location.hostname+":1337");
            window.close();
        }
    </script>
</head>
<body>
Authentication complete.
If you can see this text, close this page and refresh chat.
</body>
</html>
