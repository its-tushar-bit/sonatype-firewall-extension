/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import withStoreProvider from '../../reactAdapter/StoreProvider';
import CLMLocationModule from '../../util/CLMLocation';
import utilityModule from '../../utility/utility.module';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardResultsActionsModule from './dashboardResultsActions';

import applications from './applications/applications';
import dashboardResultsContainer from './dashboardResultsContainer';
import dashboardCommonResults from './dashboardCommonResults/dashboardCommonResults';
import dashboardTabs from './dashboardTabs/dashboardTabs';
import DashboardViolationsContainer from './violations/DashboardViolationsContainer';
import DashboardComponentsContainer from './components/DashboardComponentsContainer';

export default angular.module('dashboardResultsModule',
    [CLMLocationModule.name, utilityModule.name, dashboardUtilsModule.name, dashboardResultsActionsModule.name])
    .component('violations', react2angular(withStoreProvider(DashboardViolationsContainer), [], ['$ngRedux']))
    .component('components', react2angular(withStoreProvider(DashboardComponentsContainer), [], ['$ngRedux']))
    .component('applications', applications)
    .component('dashboardResultsContainer', dashboardResultsContainer)
    .component('dashboardCommonResults', dashboardCommonResults)
    .component('dashboardTabs', dashboardTabs);
