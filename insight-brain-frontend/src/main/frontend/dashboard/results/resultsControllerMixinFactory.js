/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {stateGo} from '../../reduxUiRouter/routerActions';
/**
 * The controllers for the applications, components, and violations tab are all mostly the same. This function
 * implements the commonality that can be mixed in to those controllers along with their custom code
 */
export default function resultsControllerMixinFactory($ngRedux, dashboardDataService, $scope, actions, resultsType) {
  return {
    maxResults: dashboardDataService.MAX_RESULTS,

    $onInit() {
      const vm = this;

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, {...actions, stateGo})(vm);
      if (!vm.filterLoading && !vm.needsAcknowledgement) {
        vm.loadResults(resultsType);
      }

      $scope.$watch('vm.filtersAreDirty', function(filtersAreDirty) {
        vm.maskController[filtersAreDirty ? 'activateMask' : 'removeMask']();
      });
    },

    $onDestroy() {
      this.unsubscribe();
    },

    getColor(score) {
      return this.results[resultsType].classyBrew.getColor(score);
    },

    getTextColorClass(score) {
      return score === 0 ? 'grey-text' : this.results[resultsType].classyBrew.isWhiteText(score) ? 'white-text'
        : undefined;
    },

    reload() {
      this.loadResults(resultsType);
    }
  };
}

// Which part of the Redux global state does our component want to receive?
function mapStateToThis(state) {
  return {
    results: state.dashboard,
    filterLoading: state.dashboardFilter.loading,
    needsAcknowledgement: state.dashboardFilter.needsAcknowledgement,
    filtersAreDirty: state.dashboardFilter.filtersAreDirty,
    appliedFilter: state.dashboardFilter.appliedFilter
  };
}
