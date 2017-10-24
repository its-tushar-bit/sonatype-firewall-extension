/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const OVERWRITE = 'overwrite';
const SAVE_AS = 'saveAs';
const NAME_IN_USE = 'nameInUse';

export default
function SaveFilterModalController($scope, $http, CLMLocations, filterJson, filterName, existingFilters, Messages) {
  var vm = this;

  vm.formMask = undefined;
  vm.saveError = undefined;

  // add constants to the scope so they are available in the template
  vm.OVERWRITE = OVERWRITE;
  vm.SAVE_AS = SAVE_AS;
  vm.NAME_IN_USE = NAME_IN_USE;

  // The existing name of the filter - does not get changed
  vm.savedFilterName = filterName || undefined;

  // The value of the name input box
  vm.filterName = '';
  vm.saveMode = filterName ? OVERWRITE : SAVE_AS;

  // Three possible states: undefined, OVERWRITE, and NAME_IN_USE
  vm.warning = undefined;

  vm.trySave = trySave;
  vm.isSaveEnabled = isSaveEnabled;
  vm.onCancel = onCancel;

  // try to save the filter. This will either result in saving it or result in a warning to the user
  // that they are about to overwrite an existing filter
  function trySave() {
    if (!vm.isSaveEnabled()) {
      return;
    }
    else if (vm.warning) {
      // if a warning is already up and the user hit Continue, then go ahead and save
      doSave();
    }
    else {
      if (vm.saveMode === OVERWRITE) {
        // show the Overwrite warning in the UI
        vm.warning = OVERWRITE;
      }
      else { // saveMode === saveAs
        const duplicate = existingFilters.some(filter => vm.filterName === filter.name);

        if (duplicate) {
          vm.warning = NAME_IN_USE;
        }
        else {
          // no warning needed when creating a new filter with unused name
          doSave();
        }
      }
    }
  }

  function doSave() {
    var namedFilter = {
      name: vm.saveMode === OVERWRITE ? vm.savedFilterName : vm.filterName,
      filter: filterJson
    };

    vm.formMask.wrap($http.put(CLMLocations.getDashboardSavedFilters(), namedFilter)).then(function() {
      $scope.$close(namedFilter.name);
    }, function(error) {
      vm.saveError = Messages.getHttpErrorMessage(error);
    });
  }

  // Save is enabled if we are overwriting the existing filter or if the filterName text box is valid
  function isSaveEnabled() {
    const form = vm.saveFilterForm;

    return !!(form && (vm.saveMode === OVERWRITE || !form.$invalid));
  }

  // if we are on the initial view, Cancel should close the modal. If we are on a warning, Cancel should go back
  // to the initial view
  function onCancel() {
    if (vm.warning === undefined) {
      $scope.$dismiss();
    }
    else {
      vm.warning = undefined;
    }
  }
}

SaveFilterModalController.$inject = [
  '$scope', '$http', 'CLMLocations', 'filterJson', 'filterName',
  'existingFilters', 'Messages'
];
