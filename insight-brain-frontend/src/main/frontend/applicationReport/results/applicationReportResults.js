/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { findIndex, includes, isNil, propEq, splitAt } from 'ramda';

import template from './applicationReportResults.html';
import cipModalWrapper from './cipModalWrapper.html';
import { stateGo } from '../../reduxUiRouter/routerActions';

export default {
  template,
  controllerAs: 'vm',
  controller: ApplicationReportResultsController,
};

function ApplicationReportResultsController(
  $state,
  $ngRedux,
  $scope,
  $timeout,
  applicationReportActions,
  Modal,
  OwnerContext,
  CLMLocations
) {
  const vm = this;

  Object.assign(vm, {
    $state,

    renderedEntries: [],

    updateRenderedEntriesPromise: null,

    aggregateByComponentToggleLabel: 'Aggregate by component',

    aggregateByComponentToggleTooltip:
      'By default the Application Report aggregates violations by component. ' +
      'To see all violations not Aggregated by Component, please switch the toggle off.',

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
      $scope.$watch('vm.reportParameters', function (reportParameters) {
        if (reportParameters) {
          OwnerContext.setOwnerId(reportParameters.appId);
          OwnerContext.setScanId(reportParameters.scanId);
          OwnerContext.setOwnerType('application');
        }
      });

      $scope.$watch('vm.selectedReport.displayedEntries', function (newValue, oldValue) {
        updateRenderedEntries();
        if (newValue && !oldValue) {
          showCipModalIfNecessary();
        }
      });
    },

    aggregateByComponentToggle() {
      vm.setAggregateReportEntries(!vm.aggregate);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    reload() {
      vm.loadReport($state.params.publicId, $state.params.scanId, !!$state.params.unknownjs);
    },

    coveragePercent() {
      const { totalArtifactCount, knownArtifactCount } = vm.selectedReport;

      return totalArtifactCount ? Math.round((100 * knownArtifactCount) / totalArtifactCount) : 0;
    },

    onRowClick(componentIndex) {
      if ($state.params.roarelSaysCip) {
        vm.openCipModal(componentIndex);
      } else {
        vm.selectComponent(componentIndex);
        vm.goToComponentDetailsPage(vm.selectedComponent.hash);
      }
    },

    goToComponentDetailsPage(hash) {
      $ngRedux.dispatch(stateGo('applicationReport.componentDetails', { hash }));
    },

    goToDependencyTree() {
      if (vm.dependencyTree) {
        $ngRedux.dispatch(stateGo('applicationReport.dependencyTree'));
      }
    },

    openCipModal(componentIndex) {
      vm.selectComponent(componentIndex);
      Modal.open({
        template: cipModalWrapper,
        windowClass: 'iq-modal iq-modal__cip',
        backdropClass: 'iq-modal-backdrop',
      });
    },

    refreshReportUrlRemovePolicyViolationId() {
      $ngRedux.dispatch(
        stateGo($state.current.name, {
          ...$state.params,
          policyViolationId: undefined,
        })
      );
    },

    refreshReportUrlRemoveComponentHashAndTabId() {
      $ngRedux.dispatch(
        stateGo($state.current.name, {
          ...$state.params,
          componentHash: undefined,
          tabId: undefined,
        })
      );
    },

    onDerivedComponentNameFilterChange() {
      vm.setStringFieldFilter('derivedComponentName', vm.derivedComponentNameSubstringFilter);
    },

    onPolicyNameFilterChange() {
      vm.setStringFieldFilter('policyName', vm.policyNameSubstringFilter);
    },

    getReportPdfDownloadUrl: function () {
      return CLMLocations.getReportPdfDownloadUrl(vm.metadata.application.publicId, vm.reportParameters.scanId);
    },

    getViewSbomUrl: function () {
      return CLMLocations.getViewSbomUrl(vm.metadata.application.id, vm.reportParameters.scanId);
    },

    getTransitiveViolationsCount: function (component) {
      const transitiveComponentViolations = vm.selectedReport.allEntries.filter(
        (e) =>
          !!(
            e.policyThreatLevel &&
            !e.waived &&
            !e.grandfathered &&
            e.dependencyInfo &&
            e.dependencyInfo.rootAncestors &&
            includes(component.componentIdentifier, e.dependencyInfo.rootAncestors)
          )
      );
      return (
        transitiveComponentViolations.length +
        ' transitive violation' +
        (transitiveComponentViolations.length === 1 ? '' : 's')
      );
    },

    getInnerSourceParentsTooltip: function (component) {
      const componentWord = component.innerSourceParentsDerivedComponentNames.length > 1 ? 'components' : 'component';
      let result = `This component was brought in by the following InnerSource ${componentWord}:`;
      component.innerSourceParentsDerivedComponentNames.forEach((componentName) => {
        result += `<br/>&#8226; ${componentName}`;
      });
      return result;
    },
  });

  // rendering thousands of rows at once can cause a noticeable UI freeze while all the angular code runs.
  // To help mitigate this, add the rows in chunks
  function updateRenderedEntries() {
    if (vm.updateRenderedEntriesPromise) {
      $timeout.cancel(vm.updateRenderedEntriesPromise);
    }

    vm.renderedEntries = [];

    if (!isNil(vm.selectedReport)) {
      doUpdateStep(vm.selectedReport.displayedEntries);
    }
  }

  function doUpdateStep(remainingEntries) {
    const [firstChunk, furtherRemainingEntries] = splitAt(100, remainingEntries);

    vm.renderedEntries = vm.renderedEntries.concat(firstChunk);

    if (furtherRemainingEntries.length) {
      // NOTE the delay of 1 is only necessary due to unit tests - angular-mock's fake implementation
      // of $timeout gets confused when everything has a delay of zero and flushes chained timeouts as
      // opposed to only timeouts that existed at the time flush was called
      vm.updateRenderedEntriesPromise = $timeout(doUpdateStep, 1, true, furtherRemainingEntries);
    }
  }

  function showCipModalIfNecessary() {
    const { policyViolationId } = vm.reportParameters || {};
    const { componentHash } = vm.reportParameters || {};
    if (isNil(vm.selectedComponentIndex) && policyViolationId) {
      const findPredicate = propEq('policyViolationId', policyViolationId);
      let selectedComponentIndex = findIndex(findPredicate, vm.selectedReport.displayedEntries);
      if (selectedComponentIndex >= 0) {
        vm.openCipModal(selectedComponentIndex);
        vm.refreshReportUrlRemovePolicyViolationId();
      } else {
        // attempt to find in all entries in case it's not currently displayed
        selectedComponentIndex = findIndex(findPredicate, vm.selectedReport.allEntries);
        if (selectedComponentIndex >= 0) {
          showCipModalForIndexResolvedFromAllEntries(selectedComponentIndex);
        }
      }
    } else if (isNil(vm.selectedComponentIndex) && componentHash) {
      const findPredicate = propEq('hash', componentHash);
      let selectedComponentIndex = findIndex(findPredicate, vm.selectedReport.displayedEntries);
      if (selectedComponentIndex >= 0) {
        vm.openCipModal(selectedComponentIndex);
        vm.refreshReportUrlRemoveComponentHashAndTabId();
      } else {
        // attempt to find in all entries in case it's not currently displayed
        selectedComponentIndex = findIndex(findPredicate, vm.selectedReport.allEntries);
        if (selectedComponentIndex >= 0) {
          vm.openCipModal(selectedComponentIndex);
          vm.refreshReportUrlRemoveComponentHashAndTabId();
        }
      }
    }
  }

  function showCipModalForIndexResolvedFromAllEntries(selectedComponentIndex) {
    const foundEntryWithOriginPolicyViolationId = vm.selectedReport.allEntries[selectedComponentIndex];
    const findPredicateByHash = propEq('hash', foundEntryWithOriginPolicyViolationId.hash);
    const componentIndexByHash = findIndex(findPredicateByHash, vm.selectedReport.displayedEntries);
    if (componentIndexByHash >= 0) {
      vm.openCipModal(componentIndexByHash);
      vm.refreshReportUrlRemovePolicyViolationId();
    }
  }
}

export function mapStateToThis({ applicationReport }) {
  const { policyName, derivedComponentName } = applicationReport.substringFilters;

  return {
    ...applicationReport,
    loading: !!applicationReport.pendingLoads.size,
    policyNameSubstringFilter: policyName,
    derivedComponentNameSubstringFilter: derivedComponentName,
  };
}

ApplicationReportResultsController.$inject = [
  '$state',
  '$ngRedux',
  '$scope',
  '$timeout',
  'applicationReportActions',
  'Modal',
  'OwnerContext',
  'CLMLocations',
];
