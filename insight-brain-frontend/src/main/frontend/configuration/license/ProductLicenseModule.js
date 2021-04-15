/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp */
import angularCommonModule from '../../util/AngularCommon';
import CLMLocationModule from '../../util/CLMLocation';
import productLicense from './ProductLicense';
import UninstallLicenseController from './uninstall.license.controller';

export default angular
  .module('ProductLicense', [
    'ui.router',
    angularCommonModule.name,
    'ngCookies',
    CLMLocationModule.name,
  ])
  .directive('onFileChange', function () {
    return {
      restrict: 'A',
      scope: false,
      link: function (scope, elem, attr) {
        angular.element(elem).bind('change', function () {
          if (attr.onFileChange) {
            scope.$apply(attr.onFileChange);
          }
        });
      },
    };
  })
  .directive('manualFileClear', function () {
    return {
      restrict: 'A',
      link: function (scope, elem) {
        scope.clearValue = function () {
          elem.wrap('<form>').closest('form').get(0).reset();
          elem.unwrap();
        };
      },
    };
  })
  .component('productLicense', productLicense)
  .controller('uninstall.license.controller', UninstallLicenseController)
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('productlicense', {
        url: '/productlicense',
        component: 'productLicense',
        data: {
          title: 'Product License',
        },
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
  ]);
