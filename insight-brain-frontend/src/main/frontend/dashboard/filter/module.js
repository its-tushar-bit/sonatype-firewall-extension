/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import CLMLocationModule from '../../util/CLMLocation';
import manageFiltersReducer from './manageFiltersReducer';
import deleteFiltersModalController from './manageFilterMenu/deleteFiltersModal/deleteFiltersModalController';
import deleteFiltersModal from './manageFilterMenu/deleteFiltersModal/deleteFiltersModal';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import manageFilterMenu from './manageFilterMenu/manageFilterMenu';
import utilityModule from '../../utility/utility.module';
import storesModule from '../../util/Stores';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardFilterReducer from './dashboardFilterReducer';
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';
import componentsModule from '../../components/module';
import DashboardFilterContainer from './dashboardFilter/DashboardFilterContainer';
import * as dashboardFilterActions from './dashboardFilterActions';

var module = angular.module('dashboardFilter',
    [
      CLMLocationModule.name, storesModule.name, utilityModule.name, dashboardUtilsModule.name,
      dashboardServicesModule.name, dashboardResultsActionsModule.name, componentsModule.name, 'ngRedux'
    ])
    .component('dashboardFilter', react2angular(withStoreProvider(DashboardFilterContainer), [], ['$ngRedux']))

    // manage filter modal
    .controller('deleteFiltersModalController', deleteFiltersModalController)
    .service('deleteFiltersModal', deleteFiltersModal)
    .component('manageFilterMenu', manageFilterMenu)
    .value('dashboardFilterActions', dashboardFilterActions)
    .value('manageFiltersReducer', manageFiltersReducer)
    .value('dashboardFilterReducer', dashboardFilterReducer);

export default module;
