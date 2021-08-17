/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp */
import { react2angular } from 'react2angular';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import angularCommonModule from '../../util/AngularCommon';
import CLMLocationModule from '../../util/CLMLocation';
import ProductLicenseContainer from './ProductLicenseContainer';

export default angular
  .module('ProductLicense', ['ui.router', angularCommonModule.name, CLMLocationModule.name])
  .component(
    'productLicenseDetail',
    react2angular(withStoreProvider(withRouterStateProvider(ProductLicenseContainer)), [], ['$ngRedux'])
  )
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('productlicense', {
        url: '/productlicense',
        component: 'productLicenseDetail',
        data: {
          title: 'Product License',
        },
      });
    },
  ]);
