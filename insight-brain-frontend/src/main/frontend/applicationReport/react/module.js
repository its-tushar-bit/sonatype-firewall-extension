/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ReportPageContainer from './ReportContainer';
import withStoreProvider from '../../reactAdapter/StoreProvider';

export default angular
  .module('appReport', [])
  .component(
    'appReport',
    react2angular(
      withStoreProvider(ReportPageContainer),
      [],
      ['$ngRedux', '$state']
    )
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('reactAppReport', {
    component: 'appReport',
    data: {
      title: 'Application Report',
    },
    url:
      '/reactAppReport/{publicId}/{scanId}?unknownjs&embeddable&policyViolationId',
  });
}

routes.$inject = ['$stateProvider'];
