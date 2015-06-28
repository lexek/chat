var module = angular.module("chat.services.linkResolver", []);

module.service("linkResolver", [function() {
    var fetchYoutubeTitle = function(videoId, ytKey) {
        var result = null;
        $.ajax({
            "url": "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + ytKey,
            "success": function (data) {
                if (data.items && data.items[0] && data.items[0].snippet) {
                    result = "<span style=\"color:#cd201f;\" class=\"fa fa-youtube-play\"></span> " + htmlEscape(data.items[0].snippet.title);
                }
            },
            "async": false,
            "timeout": 100
        });
        return result;
    };

    var LinkResolverService = function() {

    };

    //TODO: async
    LinkResolverService.prototype.resolve = function (completeLink, prefix, link) {
        var linkText = "";
        try {
            linkText = $.trim(decodeURIComponent(link));
            if (linkText.length === 0) {
                linkText = link;
            }
        } catch (e) {
            linkText = link;
        }
        if (linkText.length > 37) {
            linkText = linkText.substr(0, 32) + "[...]";
        }
        linkText = htmlEscape(linkText);
        var notProcessed = true;
        var parsedUrl = new Uri(link);
        var host = parsedUrl.host();
        if (host === "youtube.com" || host === "www.youtube.com") {
            var videoId = null;
            if (parsedUrl.getQueryParamValue("v")) {
                videoId = parsedUrl.getQueryParamValue("v");
            }
            if (parsedUrl.getQueryParamValue("watch")) {
                videoId = parsedUrl.getQueryParamValue("watch");
            }
            if (videoId) {
                var ytTitle = fetchYoutubeTitle(videoId, ytKey);
                if (ytTitle) {
                    linkText = ytTitle;
                    notProcessed = false;
                }
            }
        }
        if (host === "youtu.be") {
            var videoId = parsedUrl.uriParts.path;
            if (videoId[0] === "/") {
                videoId = videoId.substr(1);
            }
            if (videoId) {
                var ytTitle = fetchYoutubeTitle(videoId, ytKey);
                if (ytTitle) {
                    linkText = ytTitle;
                    notProcessed = false;
                }
            }
        }
        if (notProcessed) {
            var r = /http:\/\/store\.steampowered\.com\/app\/([0-9]+)\/.*/.exec(completeLink);
            if (r && r[1]) {
                var id = r[1];
                $.ajax({
                    "url": "resolve_steam",
                    "data": {"appid": id},
                    "success": function (data) {
                        if (data) {
                            linkText = "<span style=\"color: #156291;\" class=\"fa fa-steam-square\"></span> " + htmlEscape(data);
                            notProcessed = false;
                        }
                    },
                    "async": false,
                    "timeout": 100
                });
            }
        }
        return "<a href=\"" + prefix + htmlEscape(link) + "\" target=\"_blank\" title=\"" + htmlEscape(link) + "\">" + linkText + "</a>";
    };

    return new LinkResolverService();
}]);
