(function() {
    'use strict';

    var gulp = require('gulp');
    var bytediff = require('gulp-bytediff');
    var sourcemaps = require('gulp-sourcemaps');
    var ngAnnotate = require('gulp-ng-annotate');
    var concat = require('gulp-concat');
    var uglify = require('gulp-uglify');

    var adminPath = './src/main/resources/static/chat/admin/';

    gulp.task('admin', function () {

        var files = [
            adminPath + '**/*.module.js',
            adminPath + '**/*.js'
        ];

        return gulp.src(files)
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

    gulp.task('admin-watch', function () {
        gulp.watch(adminPath + '**/.js', null, ['admin']);
    });
})();
