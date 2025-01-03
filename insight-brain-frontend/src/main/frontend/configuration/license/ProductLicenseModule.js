/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import CLMLocationModule from '../../util/CLMLocation';
import ProductLicenseContainer from './ProductLicenseContainer';

export default angular
  .module('ProductLicense', ['ui.router', CLMLocationModule.name])
  .component('productLicenseDetail', iqReact2Angular(ProductLicenseContainer, [], ['$ngRedux']))
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
