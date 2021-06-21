/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, $ */
/* eslint indent: "off"*/
import { react2angular } from 'react2angular';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import resourceModule from '../Resource';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityModule from '../utility/utility.module';
import pendoModule from '../pendo/module';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import permissionServiceModule from '../util/PermissionService';
import ApplicationSecurityModule from '../policy/AppSecurityController';
import UserListController from './user.list.controller';
import userForm from './userForm/userForm';
import telemetryServiceModule from '../services/telemetryService';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';
import userListTemplate from './user-list.html';
import administratorsTemplate from '../policy/components/app-security/app-security.html';
import UserFormContainer from './userForm/UserFormContainer';

export const SecurityModule = angular.module(
  'SecurityModule',
  ['ui.router', angularCommonModule.name, ApplicationSecurityModule.name, permissionServiceModule.name],
  [
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('administrators', {
        url: '/administrators',
        template: administratorsTemplate,
        data: {
          title: 'Administrators',
        },
        controller: 'AppSecurityController',
        resolve: {
          isAuthorized: [
            'PermissionService',
            function (PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            },
          ],
        },
      });
    },
  ]
);

export const UserModule = angular
  .module('UserModule', [
    'ui.router',
    SecurityModule.name,
    CLMLocationModule.name,
    resourceModule.name,
    utilityModule.name,
    utilityDirectivesModule.name,
    telemetryServiceModule.name,
    pendoModule.name,
  ])
  .controller('UserListController', UserListController)
  .component('userForm', userForm)
  .component(
    'createUser',
    react2angular(withStoreProvider(withRouterStateProvider(UserFormContainer)), [], ['$ngRedux', '$state'])
  )
  .factory('userActions', userActions)
  .value('userReducer', userReducer)
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('users', {
      url: '/users',
      controller: 'UserListController',
      template: userListTemplate,
      data: {
        title: 'Users',
        crumb: 'Users',
      },
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          },
        ],
      },
    })
    .state('create', {
      url: '/users/_new_',
      component: 'createUser',
      data: {
        title: 'Add New User',
        isDirty: ['userForm', 'isDirty'],
      },
    });
}

routes.$inject = ['$stateProvider'];

export default UserModule;

UserModule.service('UserStore', [
  'CLMLocations',
  'StoreFactory',
  function (clmLocations, StoreFactory) {
    var config = {
        id: 'id',
        template: {
          id: null,
          username: null,
          password: null,
          firstName: null,
          lastName: null,
          email: null,
        },
        url: clmLocations.getUserUrl(),
      },
      store = StoreFactory.getStore(config);

    return store;
  },
]);

UserModule.directive('clmMatch', function () {
  return {
    require: 'ngModel',
    link: function (scope, element, attrs, ctrl) {
      function emptyString(val) {
        if (val === '' || val === null) {
          return undefined;
        }
        return val;
      }

      ctrl.$validators.match = function (value) {
        return emptyString(value) === emptyString(scope.$eval(attrs.clmMatch));
      };

      scope.$watch(
        function () {
          return scope.$eval(attrs.clmMatch);
        },
        function () {
          ctrl.$$parseAndValidate();
        }
      );
    },
  };
});

UserModule.directive('expandUserOnEvent', function () {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {
      scope.$on(attrs.expandUserOnEvent, function (event, data) {
        $('#collapse' + data.userId).collapse('show');
      });
    },
  };
});

//simple directive that will select the text in an input field
//when user clicks on it
UserModule.directive('selectText', [
  function () {
    return function (scope, element) {
      element.bind('focus', function () {
        this.select();
      });
    };
  },
]);
