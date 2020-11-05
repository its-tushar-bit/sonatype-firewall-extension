/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { equals, head, last, map, pick, range, reduce, reject } from 'ramda';

import { lookup, setToArray, union } from '../util/jsUtil';

import template from './applicationReport.html';
import reevaluationErrorModalWrapperTemplate from './reevaluationErrorModal/reevaluationErrorModalWrapper.html';
import { policyTypes } from '../dashboard/filter/staticFilterEntries';
import { fetchStageTypes } from '../stages/stagesActions';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportController
};

// Map from checkbox option id to violationState filter set
const violationStateCheckboxFilterMapping = {
  notViolating: new Set(['notViolating']),
  open: new Set(['open']),
  waived: new Set(['waived', 'waived+grandfathered']),
  grandfathered: new Set(['grandfathered', 'waived+grandfathered'])
};

function ApplicationReportController($scope, $ngRedux, applicationReportActions, Modal) {
  const vm = this;

  let reevaluationErrorModal = undefined;

  Object.assign(vm, {
    availableProprietaryFilterOptions: [
      { id: false, name: 'Non-Proprietary' },
      { id: true, name: 'Proprietary' }
    ],

    availableMatchStateFilterOptions: [
      { id: 'exact', name: 'Exact' },
      { id: 'similar', name: 'Similar' },
      { id: 'unknown', name: 'Unknown' }
    ],

    availableViolationStateFilterOptions: [
      { id: 'notViolating', name: 'Not Violating' },
      { id: 'open', name: 'Open' },
      { id: 'waived', name: 'Waived' },
      { id: 'grandfathered', name: 'Grandfathered' }
    ],

    availableDependencyTypeFilterOptions: [
      { id: 'direct', name: 'Direct Dependencies' },
      { id: 'transitive', name: 'Transitive Dependencies' },
      { id: 'unknown', name: 'Unknown' }
    ],

    availablePolicyTypeFilterOptions: policyTypes,

    violationStateCheckedIds: new Set(),

    policyThreatLevelFilterSelectedRange: undefined,

    $onInit() {
      const actions = {
        ...pick(
            ['setAggregateReportEntries', 'setExactValueFilter', 'reevaluateReport',
              'reevaluateReportCancelled', 'loadReport', 'loadInnerSourceReports'],
            applicationReportActions),
        fetchStageTypes
      };

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
      vm.loadReport();
      vm.fetchStageTypes('action');

      $scope.$watch('vm.reevaluating', function(reevaluating) {
        if (reevaluating) {
          vm.formMaskController.activateMask();
        }
        else if (!vm.reevaluationError) {
          vm.formMaskController.showSuccessMaskBriefly();
        }
        else {
          vm.formMaskController.removeMask();
        }
      });

      $scope.$watch('vm.reevaluationError', function(reevaluationError) {
        if (reevaluationError && !reevaluationErrorModal) {
          vm.openReevaluationErrorModal();
        }
        else if (!reevaluationError) {
          vm.dismissReevaluationErrorModal();
        }
      });

      $scope.$watch('vm.exactValueFilters.derivedViolationState', function(derivedViolationState) {
        const violationStateFilter = derivedViolationState || new Set(),

            // the 'waived+grandfathered' value is redundant for these purposes, and the other possible values
            // all map perfectly to the checkbox ids
            checkedIds = reject(equals('waived+grandfathered'), setToArray(violationStateFilter));

        vm.violationStateCheckedIds = new Set(checkedIds);
      });

      $scope.$watch('vm.exactValueFilters.policyThreatLevel', function(allowedValues) {
        vm.policyThreatLevelFilterSelectedRange = toSelectedRange(allowedValues);
      });
    },

    $onDestroy() {
      vm.dismissReevaluationErrorModal();
      vm.unsubscribe();
    },

    openReevaluationErrorModal() {
      function modalController($scope) {
        Object.assign($scope, {
          retry: vm.reevaluateReport,
          cancel: vm.reevaluateReportCancelled
        });
      }

      modalController.$inject = ['$scope'];

      reevaluationErrorModal = Modal.open({
        template: reevaluationErrorModalWrapperTemplate,
        controller: modalController
      });
    },

    dismissReevaluationErrorModal() {
      if (reevaluationErrorModal) {
        reevaluationErrorModal.dismiss();
        reevaluationErrorModal = undefined;
      }
    },

    setProprietaryFilterOptions(selectedIds) {
      vm.setExactValueFilter('proprietary', selectedIds);
    },

    setMatchStateFilterOptions(selectedIds) {
      vm.setExactValueFilter('matchState', selectedIds);
    },

    setViolationStateFilterOptions(selectedIds) {
      const selectedFilters = map(lookup(violationStateCheckboxFilterMapping), setToArray(selectedIds)),
          mergedFilter = reduce(union, new Set(), selectedFilters);

      vm.setExactValueFilter('derivedViolationState', mergedFilter);
    },

    setPolicyTypeFilterOptions(selectedIds) {
      vm.setExactValueFilter('policyThreatCategory', selectedIds);
    },

    setDependencyTypeFilterOptions(selectedIds) {
      vm.setExactValueFilter('derivedDependencyType', selectedIds);
    },

    setPolicyThreatLevelFilter(selectedRange) {
      vm.setExactValueFilter('policyThreatLevel', fromSelectedRange(selectedRange));
    }
  });
}

function mapStateToThis(state) {
  return pick([
    'policyTypeFilterEnabled',
    'aggregate',
    'reevaluating',
    'reevaluationError',
    'exactValueFilters',
    'reportParameters'
  ], state.applicationReport || {});
}

ApplicationReportController.$inject = ['$scope', '$ngRedux', 'applicationReportActions', 'Modal'];

function toSelectedRange(allowedValues) {
  if (allowedValues && allowedValues.size) {
    const rangeArray = setToArray(allowedValues);
    return [Math.min(...rangeArray), Math.max(...rangeArray)];
  }
  // if filter is empty - set slider to full range
  return [0, 10];
}

function fromSelectedRange(selectedRange) {
  // if whole range is selected - don't do any filtering
  return equals([0, 10], selectedRange)
    ? new Set()
    : new Set(range(head(selectedRange), last(selectedRange) + 1));
}
