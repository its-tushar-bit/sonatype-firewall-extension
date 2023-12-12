/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import SastScanPage from 'MainRoot/sastScan/SastScanPage';

const sastScanModule = angular
  .module('sastScanModule', ['ngRedux'])
  .component('sastScanPage', iqReact2Angular(SastScanPage, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('sastScan', {
    url: `/application/{applicationPublicId}/sastScan/{sastScanId}`,
    component: 'sastScanPage',
  });
}

routes.$inject = ['$stateProvider'];

export default sastScanModule;
