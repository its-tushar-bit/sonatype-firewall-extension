/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsDataService from './successMetricsDataService';
import successMetricsList from './successMetricsList';
import addSuccessMetricsModal from './addSuccessMetricsModal/addSuccessMetricsModal';
import mttrChart from './mttrChart/mttrChart';
import violationAveragesChart from './violationAveragesChart/violationAveragesChart';
import applicationCountsChart from './applicationCountsChart/applicationCountsChart';
import summaryStatementTile from './summaryStatementTile/summaryStatementTile';
import componentCountsChart from './componentCountsChart/componentCountsChart';
import successMetricsChartPage from './successMetricsChartPage/successMetricsChartPage';
import chartUtilsService from './chartUtilsService';
import CLMLocationModule from '../../util/CLMLocation';
import productFeaturesModule from '../../util/ProductFeatures';
import commonServicesModule from '../../util/CommonServices';

export default angular.module('successMetricsModule', ['components', CLMLocationModule.name,
  productFeaturesModule.name, commonServicesModule.name])
    .service('successMetricsDataService', successMetricsDataService)
    .service('chartUtilsService', chartUtilsService)
    .component('successMetricsList', successMetricsList)
    .component('addSuccessMetricsModal', addSuccessMetricsModal)
    .component('mttrChart', mttrChart)
    .component('violationAveragesChart', violationAveragesChart)
    .component('applicationCountsChart', applicationCountsChart)
    .component('summaryStatementTile', summaryStatementTile)
    .component('componentCountsChart', componentCountsChart)
    .component('successMetricsChartPage', successMetricsChartPage);
