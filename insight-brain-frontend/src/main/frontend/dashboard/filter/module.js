/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationModule from '../../util/CLMLocation';

import dashboardFilter from './dashboardFilter/dashboardFilter';
import dashboardFilterService from './dashboardFilterService';
import manageFiltersActions from './manageFiltersActions';
import manageFiltersReducer from './manageFiltersReducer';

import deleteFiltersModalController from './manageFilterMenu/deleteFiltersModal/deleteFiltersModalController';
import deleteFiltersModal from './manageFilterMenu/deleteFiltersModal/deleteFiltersModal';
import saveFilterModalController from './manageFilterMenu/saveFilterModal/saveFilterModalController';
import saveFilterModal from './manageFilterMenu/saveFilterModal/saveFilterModal';
import manageFilterMenu from './manageFilterMenu/manageFilterMenu';
import utilityModule from '../../utility/utility.module';
import storesModule from '../../util/Stores';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardFilterActions from './dashboardFilterActions';
import dashboardFilterReducer from './dashboardFilterReducer';
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';
import componentsModule from '../../components/module';

var module = angular.module('dashboardFilter',
    [
      CLMLocationModule.name, storesModule.name, utilityModule.name, dashboardUtilsModule.name,
      dashboardServicesModule.name, dashboardResultsActionsModule.name, componentsModule.name
    ])
    .service('dashboardFilterService', dashboardFilterService)
    .component('dashboardFilter', dashboardFilter)

    // manage filter modal
    .controller('deleteFiltersModalController', deleteFiltersModalController)
    .service('deleteFiltersModal', deleteFiltersModal)
    .controller('saveFilterModalController', saveFilterModalController)
    .service('saveFilterModal', saveFilterModal)
    .component('manageFilterMenu', manageFilterMenu)
    .factory('dashboardFilterActions', dashboardFilterActions)
    .factory('manageFiltersActions', manageFiltersActions)
    .value('manageFiltersReducer', manageFiltersReducer)
    .value('dashboardFilterReducer', dashboardFilterReducer);

export default module;
