/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import CLMLocationModule from '../../util/CLMLocation';

import dashboardFilterService from './dashboardFilterService';
import manageFiltersActions from './manageFiltersActions';
import manageFiltersReducer from './manageFiltersReducer';

import deleteFiltersModalController from './manageFilterMenu/deleteFiltersModal/deleteFiltersModalController';
import deleteFiltersModal from './manageFilterMenu/deleteFiltersModal/deleteFiltersModal';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import SaveFilterModalContainer from './manageFilterMenu/saveFilterModal/SaveFilterModalContainer';
import manageFilterMenu from './manageFilterMenu/manageFilterMenu';
import utilityModule from '../../utility/utility.module';
import storesModule from '../../util/Stores';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardFilterActions from './dashboardFilterActions';
import dashboardFilterReducer from './dashboardFilterReducer';
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';
import componentsModule from '../../components/module';
import DashboardFilterContainer from './dashboardFilter/DashboardFilterContainer';

var module = angular.module('dashboardFilter',
    [
      CLMLocationModule.name, storesModule.name, utilityModule.name, dashboardUtilsModule.name,
      dashboardServicesModule.name, dashboardResultsActionsModule.name, componentsModule.name, 'ngRedux'
    ])
    .service('dashboardFilterService', dashboardFilterService)
    .component('dashboardFilter', react2angular(withStoreProvider(DashboardFilterContainer), [],
        ['$ngRedux', 'manageFiltersActions', 'dashboardFilterActions']))

    // manage filter modal
    .controller('deleteFiltersModalController', deleteFiltersModalController)
    .service('deleteFiltersModal', deleteFiltersModal)
    .component('saveFilterModal', react2angular(withStoreProvider(SaveFilterModalContainer), [],
        ['$ngRedux', 'manageFiltersActions', 'dashboardFilterActions', 'Messages']))
    .component('manageFilterMenu', manageFilterMenu)
    .factory('dashboardFilterActions', dashboardFilterActions)
    .factory('manageFiltersActions', manageFiltersActions)
    .value('manageFiltersReducer', manageFiltersReducer)
    .value('dashboardFilterReducer', dashboardFilterReducer);

export default module;
