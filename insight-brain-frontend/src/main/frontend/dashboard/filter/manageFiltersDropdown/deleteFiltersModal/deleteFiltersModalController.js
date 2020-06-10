/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

function mapStateToThis({ manageFilters }) {
  return {
    savedFilters: manageFilters.savedFilters
  };
}

export default
function DeleteFiltersModalController($scope, $ngRedux, DeleteModalService, actions) {
  const vm = this;
  let originalFilters;

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.isDirty()) {
      vm.unsavedModalVisible = true;
      event.preventDefault();
    }
  });

  $scope.$on('pageChangeCanceled', function() {
    vm.unsavedModalVisible = false;
  });

  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });

  const unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);

  $scope.$on('$destroy', unsubscribe);

  Object.assign(vm, {
    doLoad() {
      var previousFilters = angular.copy(vm.filters);
      vm.filters = {};
      vm.savedFilters.forEach(function(filter) {
        vm.filters[filter.name] = previousFilters && previousFilters[filter.name] || false;
      });
      originalFilters = originalFilters || angular.copy(vm.filters);
    },

    deleteFilters() {
      const filtersToDelete = Object.keys(vm.filters).filter(function(filter) {
            return vm.filters[filter] === true;
          }),
          deleteModalTitle = 'Delete Filters',
          deleteModalBody = 'You are about to remove ' + filtersToDelete.length +
              ' filter(s). This action cannot be undone.',
          deleteModalMask = 'Removing',
          continueAction = vm.deleteSpecifiedFilters.bind(null, filtersToDelete);

      function deleteModalStateMapper(state) {
        const { manageFilters } = state;
        return {
          errorState: manageFilters.deleteFiltersError,
          deleting: manageFilters.deleteFiltersSaving,
          success: manageFilters.deleteFiltersSuccess
        };
      }

      function deleteModalErrorHandler(error) {
        if (error) {
          vm.deleteError = error;
        }
        vm.deleteMode = false;
        vm.isLoading = false;
        vm.fetchSavedFilters();
      }

      if (filtersToDelete && filtersToDelete.length > 0) {
        vm.isLoading = true;
        vm.deleteMode = true;

        DeleteModalService.deleteRedux(deleteModalTitle, deleteModalBody, deleteModalMask, continueAction,
            deleteModalStateMapper)
            .then(vm.close)
            .catch(deleteModalErrorHandler);
      }
    },

    isDirty() {
      return !angular.equals(originalFilters, vm.filters);
    },

    toggleSelected(filter) {
      vm.filters[filter] = !vm.filters[filter];
    },

    close() {
      $scope.$close(vm.savedFilters);
    }
  });

  vm.doLoad();
}

DeleteFiltersModalController.$inject = ['$scope', '$ngRedux', 'DeleteModalService', 'manageFiltersActions'];
