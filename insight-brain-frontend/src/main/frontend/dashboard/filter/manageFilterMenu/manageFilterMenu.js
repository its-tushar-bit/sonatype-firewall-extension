/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import template from './manageFilterMenu.html';
import * as manageFiltersActions from '../manageFiltersActions';
import { applySavedFilter, setDisplaySaveFilterModal } from '../dashboardFilterActions';

var manageFilterMenu = {
  template: template,
  controller: ManageFilterMenuController,
  controllerAs: 'vm'
};

export default manageFilterMenu;

function mapStateToThis({ manageFilters, dashboardFilter }) {
  const manageFiltersProps = pick([
    'appliedFilterName', 'savedFilters', 'savedFilterListError', 'currentlyOpenModal', 'filtersToDelete',
    'pageChangePending'
  ], manageFilters);

  const dashboardFilterProps = pick(['filtersAreDirty'], dashboardFilter);

  return {...manageFiltersProps, ...dashboardFilterProps};
}

function ManageFilterMenuController($ngRedux, DeleteFiltersModal) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      const actions = {
        applySavedFilter,
        setDisplaySaveFilterModal,
        ...manageFiltersActions
      };
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    openSaveFilterModal($event) {
      if (vm.filtersAreDirty) {
        $event.stopPropagation();
        return;
      }
      vm.setDisplaySaveFilterModal(true);
    },

    openDeleteFiltersModal($event) {
      if (!vm.hasSavedFilters()) {
        $event.stopPropagation();
        return;
      }

      DeleteFiltersModal.open(vm.savedFilters).finally(vm.resetDeleteFiltersStatus);
    },

    isLoadingSavedFilters() {
      return vm.savedFilters === null && !vm.savedFilterListError;
    },

    hasSavedFilters() {
      return vm.savedFilters !== null && vm.savedFilters.length > 0;
    }
  });
}

ManageFilterMenuController.$inject = ['$ngRedux', 'deleteFiltersModal'];
