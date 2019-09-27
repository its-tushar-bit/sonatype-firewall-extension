/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import samlConfiguration from './samlConfiguration';
import clmContextLocationModule from '../../util/CLMContextLocation';
import utilityModule from '../../utility/utility.module';

export default angular.module('samlModule', [clmContextLocationModule.name, utilityModule.name])
    .component('samlConfiguration', samlConfiguration)
    .config([
      '$stateProvider', function($stateProvider) {
        $stateProvider.state('saml', {
          url: '/saml',
          component: 'samlConfiguration',
          data: {
            title: 'SAML'
          },
          resolve: {
            isAuthorized: [
              'PermissionService', function(PermissionService) {
                return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
              }
            ]
          }
        });
      }
    ])
    .directive('onFileChangeSaml', function() {
      return {
        restrict: 'A',
        scope: false,
        link: function(scope, elem, attr) {
          angular.element(elem).bind('change', function() {
            scope.file = elem[0].files[0];
            scope.$apply(attr.onFileChangeSaml);
            elem[0].value = '';
          });
        }
      };
    });
