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

function dashboardResultsContainerController($ngRedux) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    isFilterLoaded() {
      return !vm.filterLoading && !vm.loadFilterError;
    },
  });
}

dashboardResultsContainerController.$inject = ['$ngRedux'];

function mapStateToThis(state) {
  return {
    filterLoading: state.dashboardFilter.loading,
    loadFilterError: state.dashboardFilter.loadError,
  };
}
