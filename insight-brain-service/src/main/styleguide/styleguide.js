(function(angular) {
  'use strict';
  angular.module('styleModule', ['AngularCommon']);

  angular.module('formsStyle', ['AngularCommon', 'FormsModule']).run(['$rootScope', function ($rootScope) {
    $rootScope.emailOne = '';
    $rootScope.emailTwo = 'optional@sonatype.com';
    $rootScope.emailThree = '';
    $rootScope.emailFour = 'required@sonatype.com';
  }]);
}(angular));

// Styleguide shims to reduce the number of dependencies loaded
(function(angular) {
  'use strict';

  angular.module('ui.bootstrap', []);
  angular.module('ui.bootstrap.tpls', []);
  angular.module('ui.bootstrap.transition', []);
  angular.module('ngSanitize', []);
}(angular));
