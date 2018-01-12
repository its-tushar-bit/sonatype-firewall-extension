/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationModule from '../../util/CLMLocation';

import dashboardFilterDimension from './dashboardFilterDimension/dashboardFilterDimension';
import dashboardFilterRadioDimension from './dashboardFilterRadioDimension/dashboardFilterRadioDimension';
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
import dashboardFilterActionsModule from './dashboardFilterActions';

var module = angular.module('dashboardFilter',
    [
      CLMLocationModule.name, storesModule.name, utilityModule.name, dashboardUtilsModule.name,
      dashboardFilterActionsModule.name
    ])
    .directive('dashboardFilterDimension', dashboardFilterDimension)
    .directive('dashboardFilterRadioDimension', dashboardFilterRadioDimension)
    .service('dashboardFilterService', dashboardFilterService)
    .component('dashboardFilter', dashboardFilter)

    // manage filter modal
    .controller('deleteFiltersModalController', deleteFiltersModalController)
    .service('deleteFiltersModal', deleteFiltersModal)
    .controller('saveFilterModalController', saveFilterModalController)
    .service('saveFilterModal', saveFilterModal)
    .component('manageFilterMenu', manageFilterMenu)
    .factory('manageFiltersActions', manageFiltersActions)
    .value('manageFiltersReducer', manageFiltersReducer);

export default module;
