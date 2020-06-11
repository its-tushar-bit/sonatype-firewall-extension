/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import CLMLocationModule from '../../util/CLMLocation';
import dashboardFilter from './dashboardFilter/dashboardFilter';
import * as dashboardFilterService from './dashboardFilterService';
import manageFiltersReducer from './manageFiltersReducer';
import deleteFiltersModalController from './manageFilterMenu/deleteFiltersModal/deleteFiltersModalController';
import deleteFiltersModal from './manageFilterMenu/deleteFiltersModal/deleteFiltersModal';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import SaveFilterModalContainer from './manageFilterMenu/saveFilterModal/SaveFilterModalContainer';
import manageFilterMenu from './manageFilterMenu/manageFilterMenu';
import utilityModule from '../../utility/utility.module';
import storesModule from '../../util/Stores';
import dashboardUtilsModule from '../utils/dashboard.utils.module';
import dashboardFilterReducer from './dashboardFilterReducer';
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';
import componentsModule from '../../components/module';
import * as dashboardFilterActions from './dashboardFilterActions';
import * as manageFiltersActions from './manageFiltersActions';

var module = angular.module('dashboardFilter',
    [
      CLMLocationModule.name, storesModule.name, utilityModule.name, dashboardUtilsModule.name,
      dashboardServicesModule.name, dashboardResultsActionsModule.name, componentsModule.name, 'ngRedux'
    ])
    .value('dashboardFilterService', dashboardFilterService)
    .component('dashboardFilter', dashboardFilter)
    .controller('deleteFiltersModalController', deleteFiltersModalController)
    .service('deleteFiltersModal', deleteFiltersModal)
    .component('saveFilterModal', react2angular(withStoreProvider(SaveFilterModalContainer), [], ['$ngRedux']))
    .component('manageFilterMenu', manageFilterMenu)
    .value('dashboardFilterActions', dashboardFilterActions)
    .value('manageFiltersReducer', manageFiltersReducer)
    .value('manageFiltersActions', manageFiltersActions)
    .value('dashboardFilterReducer', dashboardFilterReducer);

export default module;
