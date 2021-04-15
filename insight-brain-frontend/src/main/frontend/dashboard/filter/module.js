/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import CLMLocationModule from '../../util/CLMLocation';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import utilityModule from '../../utility/utility.module';
import storesModule from '../../util/Stores';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardFilterReducer from './dashboardFilterReducer';
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';
import componentsModule from '../../components/module';
import DashboardFilterContainer from './dashboardFilter/DashboardFilterContainer';
import * as dashboardFilterActions from './dashboardFilterActions';

const module = angular
  .module('dashboardFilter', [
    CLMLocationModule.name,
    storesModule.name,
    utilityModule.name,
    dashboardUtilsModule.name,
    dashboardServicesModule.name,
    dashboardResultsActionsModule.name,
    componentsModule.name,
    'ngRedux',
  ])
  .component(
    'dashboardFilter',
    react2angular(withStoreProvider(DashboardFilterContainer), [], ['$ngRedux'])
  )
  .value('dashboardFilterActions', dashboardFilterActions)
  .value('dashboardFilterReducer', dashboardFilterReducer);

export default module;
