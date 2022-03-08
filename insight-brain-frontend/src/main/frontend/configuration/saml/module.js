/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocationModule from '../../util/CLMContextLocation';
import utilityModule from '../../utility/utility.module';
import SAMLConfigurationPage from './SAMLConfigurationPage';
import { react2angular } from 'react2angular';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';

export default angular
  .module('samlModule', [clmContextLocationModule.name, utilityModule.name])
  .component(
    'samlConfigurationPage',
    react2angular(withStoreProvider(withRouterStateProvider(SAMLConfigurationPage)), [], ['$ngRedux', '$state'])
  )
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('saml', {
        url: '/saml',
        component: 'samlConfigurationPage',
        data: {
          title: 'SAML',
          isDirty: ['samlConfiguration', 'isDirty'],
        },
      });
    },
  ])
  .directive('onFileChangeSaml', function () {
    return {
      restrict: 'A',
      scope: false,
      link: function (scope, elem, attr) {
        angular.element(elem).bind('change', function () {
          scope.file = elem[0].files[0];
          scope.$apply(attr.onFileChangeSaml);
          elem[0].value = '';
        });
      },
    };
  });
