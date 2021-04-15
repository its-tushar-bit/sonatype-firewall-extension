/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsDataService from './successMetricsDataService';
import successMetricsReportList from './successMetricsReportList';
import addSuccessMetricsReportModal from './addSuccessMetricsReportModal/addSuccessMetricsReportModal';
import mttrChart from './successMetricsReport/mttrChart/mttrChart';
import violationAveragesChart from './successMetricsReport/violationAveragesChart/violationAveragesChart';
import applicationCountsChart from './successMetricsReport/applicationCountsChart/applicationCountsChart';
import summaryStatementTile from './successMetricsReport/summaryStatementTile/summaryStatementTile';
import componentCountsChart from './successMetricsReport/componentCountsChart/componentCountsChart';
import violationsByCategoryChart from './successMetricsReport/violationsByCategoryChart/violationsByCategoryChart';
import successMetricsReport from './successMetricsReport/successMetricsReport';
import CLMLocationModule from '../../util/CLMLocation';
import productFeaturesModule from '../../util/ProductFeatures';
import directivesModule from '../../directives/module';
import commonServicesModule from '../../util/CommonServices';
import componentsModule from '../../components/module';
import renderCombinedTrendsChart from './successMetricsReport/violationTrendsChart/renderCombinedTrendsChart';
import violationTrendsChart from './successMetricsReport/violationTrendsChart/violationTrendsChart';

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
  .component('successMetricsReportList', successMetricsReportList)
  .component('addSuccessMetricsReportModal', addSuccessMetricsReportModal)
  .component('mttrChart', mttrChart)
  .component('violationAveragesChart', violationAveragesChart)
  .component('applicationCountsChart', applicationCountsChart)
  .component('violationsByCategoryChart', violationsByCategoryChart)
  .component('summaryStatementTile', summaryStatementTile)
  .component('componentCountsChart', componentCountsChart)
  .component('successMetricsReport', successMetricsReport)
  .directive('renderCombinedTrendsChart', renderCombinedTrendsChart)
  .component('violationTrendsChart', violationTrendsChart)
  .filter('abs', () => Math.abs);
