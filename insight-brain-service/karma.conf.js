module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    files: [
      'grunt/working/dist/assets/lib/jquery/jquery-*.min.js',
      'grunt/working/dist/assets/lib/bootstrap/bootstrap-*.min.js',
      'grunt/working/dist/assets/lib/angular/angular-1.2.9.js',
      'grunt/working/dist/assets/lib/angular/angular-route-1.2.9.js',
      'grunt/working/dist/assets/lib/angular/angular-sanitize-1.2.9.js',
      'grunt/working/dist/assets/lib/ui-bootstrap-tpls-*.min.js',
      'grunt/working/dist/assets/lib/X-editable/xeditable-0.1.7.min.js',
       { pattern: 'src/main/resources/assets/assets/components/**/*.html', included: true, served: true },
      'grunt/working/dist/**/*.js',
      'grunt/working/.tmp/**/*.js',
      'src/test/resources/assets/SpecUtil.js',
      'src/test/resources/assets/**/lib/*.js',
      'src/test/resources/assets/**/*MockData.js',
      'src/test/resources/assets/**/*Spec.js'
    ],
    exclude: [
      'src/test/resources/assets/**/cip*.js',
      'src/test/resources/assets/**/CIP*.js'
    ],
    port: 9090,
    logLevel: config.LOG_INFO,
    autoWatch: false,
    browsers: ['PhantomJS'],
    singleRun: false
  });
};