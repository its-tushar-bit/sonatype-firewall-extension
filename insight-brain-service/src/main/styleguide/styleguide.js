(function () {
  'use strict';
  angular.module('formsStyle', ['AngularCommon', 'FormsModule']).run(['$rootScope', function ($rootScope) {
    $rootScope.emailOne = '';
    $rootScope.emailTwo = 'optional@sonatype.com';
    $rootScope.emailThree = '';
    $rootScope.emailFour = 'required@sonatype.com';
  }]);
}());

