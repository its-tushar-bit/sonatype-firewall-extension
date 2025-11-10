/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './dashboardResultsContainer.html';

export default {
  template,
  controller: dashboardResultsContainerController,
  controllerAs: 'vm',
};

function dashboardResultsContainerController($ngRedux, $scope) {
  const vm = this;

  function mapStateToThis(state) {
    return {
      filterLoading: state.dashboardFilter.loading,
      loadFilterError: state.dashboardFilter.loadError,
    };
  }

  function updateState() {
    const state = $ngRedux.getState();
    const mapped = mapStateToThis(state);
    Object.assign(vm, mapped);
  }

  Object.assign(vm, {
    $onInit() {
      updateState();
      const unsubscribe = $ngRedux.subscribe(() => {
        updateState();
        $scope.$applyAsync();
      });
      $scope.$on('$destroy', unsubscribe);
    },

    isFilterLoaded() {
      return !vm.filterLoading && !vm.loadFilterError;
    },
  });
}

dashboardResultsContainerController.$inject = ['$ngRedux', '$scope'];
