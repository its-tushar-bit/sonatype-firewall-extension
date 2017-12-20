/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationModule from '../../util/CLMLocation';
import utilityModule from '../../utility/utility.module';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardResultsActionsModule from './dashboardResultsActions';

import violations from './violations/violations';
import components from './components/components';
import applications from './applications/applications';
import dashboardResultsContainer from './dashboardResultsContainer';
import violationsTableRow from './violations/violationsTableRow/violationsTableRow';
import dashboardCommonResults from './dashboardCommonResults/dashboardCommonResults';
import dashboardTabs from './dashboardTabs/dashboardTabs';

export default angular.module('dashboardResultsModule',
    [CLMLocationModule.name, utilityModule.name, dashboardUtilsModule.name, dashboardResultsActionsModule.name])
    .component('violations', violations)
    .component('components', components)
    .component('applications', applications)
    .component('dashboardResultsContainer', dashboardResultsContainer)
    .directive('violationsTableRow', violationsTableRow)
    .component('dashboardCommonResults', dashboardCommonResults)
    .component('dashboardTabs', dashboardTabs);
