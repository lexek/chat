(function() {
    'use strict';

    var gulp = require('gulp');
    var bytediff = require('gulp-bytediff');
    var sourcemaps = require('gulp-sourcemaps');
    var ngAnnotate = require('gulp-ng-annotate');
    var concat = require('gulp-concat');
    var uglify = require('gulp-uglify');
    var angularTemplateCache = require('gulp-angular-templatecache');
    var addStream = require('add-stream');

    var basePath = './src/main/resources/static';
    var adminPath = basePath + '/chat/admin/';
    var clientPath = basePath + '/chat/client/';
    var commonPath = basePath + '/chat/common/';

    function prepareTemplates(appPath) {
        return gulp.src([
            commonPath + '**/*.html',
            basePath + appPath + '**/*.html'
        ]).pipe(angularTemplateCache(
            'templates.js',
            {
                module: 'templates',
                root: appPath,
                standAlone: false
            }
        ));
    }

    gulp.task('admin', function () {
        var files = [
            commonPath + '**/*.module.js',
            adminPath + '**/*.module.js',
            commonPath + '**/*.js',
            adminPath + '**/*.js'
        ];

        return gulp.src(files)
            .pipe(addStream.obj(prepareTemplates('/chat/admin/')))
            .pipe(sourcemaps.init())
            .pipe(concat('admin.min.js', {newLine: ';'}))
            .pipe(ngAnnotate({
                // true helps add where @ngInject is not used. It infers.
                // Doesn't work with resolve, so we must be explicit there
                add: true
            }))
            .pipe(bytediff.start())
            .pipe(uglify({mangle: true}))
            .pipe(bytediff.stop())
            .pipe(sourcemaps.write('./target/classes/static/js'))
            .pipe(gulp.dest('./target/classes/static/js/'));
    });

    gulp.task('client', function() {
        //todo: use wildcards after client update
        var files = [
            basePath + '/vendor/js/modernizr.js',
            basePath + '/vendor/js/jquery-1.11.1.js',
            basePath + '/vendor/js/twemoji.js',
            basePath + '/vendor/js/url.js',
            basePath + '/vendor/js/jquery.cookie.js',
            basePath + '/vendor/js/tse.js',
            basePath + '/vendor/js/swfobject.js',
            basePath + '/vendor/js/web_socket.js',
            basePath + '/vendor/js/angular-new.js',
            basePath + '/vendor/js/angular-sanitize.js',
            basePath + '/vendor/js/bindonce.js',
            basePath + '/vendor/js/angular-ui-utils.js',
            basePath + '/vendor/js/angular-animate.2.js',
            basePath + '/vendor/js/angular-touch.js',
            basePath + '/vendor/js/angular-ui-bootstrap.js',
            basePath + '/vendor/js/angular-cookies.js',
            basePath + '/vendor/js/angular-translate.js',
            basePath + '/vendor/js/angular-translate-storage-cookie.js',
            basePath + '/vendor/js/angular-textcomplete.js',
            basePath + '/vendor/js/angular-relative-date.js',
            basePath + '/vendor/js/angular-recaptcha.js',
            basePath + '/vendor/js/bootstrap-colorpicker.js',
            clientPath + '/mixins/**',
            clientPath + '/types/chatState.js',
            clientPath + '/types/role.js',
            clientPath + '/types/user.js',
            clientPath + '/libs.js',
            clientPath + '/sc2emotes.js',
            clientPath + '/lang.js',
            clientPath + '/services/linkResolver.js',
            clientPath + '/services/settings.js',
            clientPath + '/services/windowState.js',
            clientPath + '/services/notifications.js',
            clientPath + '/services/messageProcessing.js',
            clientPath + '/services/chat.js',
            clientPath + '/services.js',
            clientPath + '/twitter.js',
            clientPath + '/messages.js',
            clientPath + '/users.js',
            clientPath + '/controls.js',
            clientPath + '/ui/profile/email.js',
            clientPath + '/ui/profile/password.js',
            clientPath + '/ui/profile/profile.js',
            clientPath + '/ui/tickets/list.js',
            clientPath + '/ui/tickets/compose.js',
            commonPath + '**/*.module.js',
            commonPath + '**/*.js',
            clientPath + '/chat.js'
        ];

        return gulp.src(files)
            .pipe(sourcemaps.init())
            .pipe(concat('client.min.js', {newLine: ';'}))
            .pipe(ngAnnotate({
                // true helps add where @ngInject is not used. It infers.
                // Doesn't work with resolve, so we must be explicit there
                add: true
            }))
            .pipe(bytediff.start())
            .pipe(uglify({mangle: false}))
            .pipe(bytediff.stop())
            .pipe(sourcemaps.write('./'))
            .pipe(gulp.dest('./target/classes/static/js'));
    });

    gulp.task('watch', function () {
        gulp.watch([
            adminPath + '**/*.js',
            adminPath + '**/*.html',
            commonPath + '**/*.js',
            commonPath + '**/*.html'
        ], ['admin']);
        gulp.watch([
            clientPath + '**/*.js',
            clientPath + '**/*.html',
            commonPath + '**/*.js',
            commonPath + '**/*.html'
        ], ['client']);
    });

    gulp.task('default', ['admin', 'client', 'watch']);
})();
