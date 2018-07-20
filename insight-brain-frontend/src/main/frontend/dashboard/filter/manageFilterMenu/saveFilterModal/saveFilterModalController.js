/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

const OVERWRITE = 'overwrite';
const SAVE_AS = 'saveAs';
const NAME_IN_USE = 'nameInUse';

export default
function SaveFilterModalController($scope, $ngRedux, Messages, actions) {
  const vm = this;

  const unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);

  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });

  $scope.$on('pageChangeStarted', function($event) {
    $event.preventDefault();
  });

  $scope.$on('$destroy', unsubscribe);

  $scope.$watchGroup(['vm.saveFilterSaving', 'vm.saveFilterSuccess'], function([saving, success]) {
    if (success) {
      vm.formMask.showSuccessMaskBriefly().then(function() {
        $scope.$close(vm.getFilterNameToSave());
      });
    }
    else {
      vm.formMask[saving ? 'activateMask' : 'removeMask']();
    }
  });

  Object.assign(vm, {
    formMask: undefined,

    // value of the input box
    filterName: '',
    saveMode: vm.appliedFilterName ? OVERWRITE : SAVE_AS,

    // add constants to the scope so they are available in the template
    OVERWRITE,
    SAVE_AS,
    NAME_IN_USE,

    // try to save the filter. This will either result in saving it or result in a warning to the user
    // that they are about to overwrite an existing filter
    trySave() {
      if (!vm.isSaveEnabled()) {
        return;
      }
      else if (vm.warning) {
        // if a warning is already up and the user hit Continue, then go ahead and save
        vm.doSave();
      }
      else {
        if (vm.saveMode === OVERWRITE) {
          // show the Overwrite warning in the UI
          vm.warning = OVERWRITE;
        }
        else { // saveMode === saveAs
          const duplicate = vm.savedFilters.some(filter => vm.filterName === filter.name);

          if (duplicate) {
            vm.warning = NAME_IN_USE;
          }
          else {
            // no warning needed when creating a new filter with unused name
            vm.doSave();
          }
        }
      }
    },

    getFilterNameToSave() {
      return vm.saveMode === OVERWRITE ? vm.appliedFilterName : vm.filterName;
    },

    doSave() {
      vm.saveFilter(vm.getFilterNameToSave());
    },

    onCancel() {
      if (vm.warning === undefined) {
        $scope.$dismiss();
      }
      else {
        vm.warning = undefined;
      }
    },

    // Save is enabled if we are overwriting the existing filter or if the text box is valid
    isSaveEnabled() {
      const form = vm.saveFilterForm;

      return !!(form && (vm.filterSaveMode === OVERWRITE || !form.$invalid));
    }
  });

  function mapStateToThis({ manageFilters }) {
    const propsToCopy = ['savedFilters', 'appliedFilterName', 'saveFilterSaving', 'saveFilterSuccess',
          'saveFilterError'],
        copiedState = pick(propsToCopy, manageFilters);

    return Object.assign({ saveError: Messages.getHttpErrorMessage(manageFilters.saveFilterError) }, copiedState);
  }
}

SaveFilterModalController.$inject = ['$scope', '$ngRedux', 'Messages', 'manageFiltersActions'];
