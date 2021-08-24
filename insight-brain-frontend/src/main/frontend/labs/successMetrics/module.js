/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import successMetricsDataService from './successMetricsDataService';
import CLMLocationModule from '../../util/CLMLocation';
import productFeaturesModule from '../../util/ProductFeatures';
import directivesModule from '../../directives/module';
import commonServicesModule from '../../util/CommonServices';
import componentsModule from '../../components/module';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';
import SuccessMetricsReportContainer from './successMetricsReport/SuccessMetricsReportContainer';
import SuccessMetricsReportListContainer from './SuccessMetricsReportListContainer';

export default angular
  .module('successMetricsModule', [
    'components',
    CLMLocationModule.name,
    productFeaturesModule.name,
    directivesModule.name,
    commonServicesModule.name,
    componentsModule.name,
  ])
  .service('successMetricsDataService', successMetricsDataService)
  .component(
    'successMetricsReport',
    react2angular(withStoreProvider(withRouterStateProvider(SuccessMetricsReportContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'successMetricsReportList',
    react2angular(
      withStoreProvider(withRouterStateProvider(SuccessMetricsReportListContainer)),
      [],
      ['$ngRedux', '$state']
    )
  )
  .filter('abs', () => Math.abs);
