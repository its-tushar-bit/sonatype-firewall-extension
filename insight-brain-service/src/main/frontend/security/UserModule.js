/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, ZeroClipboard, clmBuildTimestamp, $ */
/* eslint indent: "off"*/
import resourceModule from '../Resource';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityModule from '../utility/utility.module';
import permissionServiceModule from '../util/PermissionService';

angular.module('SecurityModule', ['ui.router', angularCommonModule.name, 'ApplicationSecurityModule',
  permissionServiceModule.name], ['$stateProvider',
    function($stateProvider) {
      $stateProvider.state('administrators', {
        url: '/administrators',
        template: '<div authorization-wrapper="isAuthorized">' +
                     '<div class="scrollable-root-container" maximize-container-height>' +
                       '<div class="iq-tile iq-tile--sys-prefs">' +
                         '<div class="iq-tile-header iq-tile-header--sys-prefs">' +
                           '<div class="iq-tile-header__title">' +
                             '<h2>Administrators</h2>' +
                           '</div>' +
                         '</div>' +
                         '<div class="iq-tile-content" ng-include="\'policy/components/app-security/app-security.html?' + clmBuildTimestamp + '\'"></div>' +
                       '</div>' +
                     '</div>' +
                   '</div>',
        data: {
          title: 'Administrators'
        },
        controller: 'AppSecurityController',
        resolve: {
          'isAuthorized': ['PermissionService', function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          }]
        }
      });
    }]);

var module = angular.module('UserModule', ['ui.router', 'SecurityModule', CLMLocationModule.name, resourceModule.name,
  utilityModule.name],
        ['$stateProvider', function($stateProvider) {
          $stateProvider.state('users', {
            url: '/users',
            controller: 'UserListController',
            templateUrl: 'security/user-list.html?' + clmBuildTimestamp,
            data: {
              title: 'Users',
              crumb: 'Users'
            },
            resolve: {
              'isAuthorized': ['PermissionService', function (PermissionService) {
                return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
              }]
            }
          }).state('users.create', {
            // NOTE This is currently only used for adding new users - editing users is done using an inline form
            url: '/_new_',
            templateUrl: 'security/user-create.html?' + clmBuildTimestamp,
            data: {
              title: 'New User',
              crumb: 'New User'
            }
          });
        }]);

module.service('UserStore', ['CLMLocations', 'StoreFactory', function(clmLocations, StoreFactory) {
  var config = {
    id: 'id',
    template: {
      id: null,
      username: '',
      password: '',
      firstName: '',
      lastName: '',
      email: ''
    },
    url: clmLocations.getUserUrl()
  }, store = StoreFactory.getStore(config);

  return store;
}]);

module.directive('clmMatch', function () {
  return {
    require: 'ngModel',
    link: function(scope, element, attrs, ctrl) {
      function emptyString(val) {
        if (val === '' || val === null) {
          return undefined;
        }
        return val;
      }

      ctrl.$validators.match = function (value) {
        return emptyString(value) === emptyString(scope.$eval(attrs.clmMatch));
      };

      scope.$watch(function () {
        return scope.$eval(attrs.clmMatch);
      }, function () {
        ctrl.$$parseAndValidate();
      });
    }
  };
});

module.directive('expandUserOnEvent', function() {
  return {
    restrict: 'A',
    link: function(scope, element, attrs) {
      scope.$on(attrs.expandUserOnEvent, function(event, data) {
        $('#collapse' + data.userId).collapse('show');
      });
    }
  };
});

module.directive('zeroClipboard', function() {
  ZeroClipboard.config({
    moviePath: 'lib/zeroclipboard/ZeroClipboard-1.3.2.swf'
  });
  return {
    restrict: 'A',
    link: function(scope, element, attrs) {
      var clip = new ZeroClipboard(element);

      clip.on('dataRequested', function () {
        clip.setText($('#' + attrs.zeroClipboard).val());
      });
    }
  };
});

//simple directive that will select the text in an input field
//when user clicks on it
module.directive('selectText', [function () {
  return function (scope, element) {
    element.bind('focus', function () {
      this.select();
    });
  };
}]);
