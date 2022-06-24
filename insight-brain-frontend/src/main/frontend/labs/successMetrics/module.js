/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import successMetricsDataService from './successMetricsDataService';
import CLMLocationModule from '../../util/CLMLocation';
import directivesModule from '../../directives/module';
import commonServicesModule from '../../utilAngular/CommonServices';
import componentsModule from '../../components/module';
import SuccessMetricsReportContainer from './successMetricsReport/SuccessMetricsReportContainer';
import SuccessMetricsReportListContainer from './SuccessMetricsReportListContainer';

export default angular
  .module('successMetricsModule', [
    'components',
    CLMLocationModule.name,
    directivesModule.name,
    commonServicesModule.name,
    componentsModule.name,
  ])
  .service('successMetricsDataService', successMetricsDataService)
  .component('successMetricsReport', iqReact2Angular(SuccessMetricsReportContainer, [], ['$ngRedux', '$state']))
  .component('successMetricsReportList', iqReact2Angular(SuccessMetricsReportListContainer, [], ['$ngRedux', '$state']))
  .filter('abs', () => Math.abs);
