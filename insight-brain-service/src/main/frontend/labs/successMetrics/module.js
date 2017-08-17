/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsDataService from './successMetricsDataService';
import successMetrics from './successMetrics';
import mttrChart from './mttrChart/mttrChart';
import violationAveragesChart from './violationAveragesChart/violationAveragesChart';
import applicationCountsChart from './applicationCountsChart/applicationCountsChart';
import summaryStatementTile from './summaryStatementTile/summaryStatementTile';
import componentCountsChart from './componentCountsChart/componentCountsChart';
import rootOrganization from './rootOrganization/rootOrganization';
import chartUtilsService from './chartUtilsService';

export default angular.module('successMetricsModule', ['components', 'CLMLocation', 'ProductFeaturesModule'])
    .service('successMetricsDataService', successMetricsDataService)
    .service('chartUtilsService', chartUtilsService)
    .component('successMetrics', successMetrics)
    .component('mttrChart', mttrChart)
    .component('violationAveragesChart', violationAveragesChart)
    .component('applicationCountsChart', applicationCountsChart)
    .component('summaryStatementTile', summaryStatementTile)
    .component('componentCountsChart', componentCountsChart)
    .component('rootOrganization', rootOrganization);

