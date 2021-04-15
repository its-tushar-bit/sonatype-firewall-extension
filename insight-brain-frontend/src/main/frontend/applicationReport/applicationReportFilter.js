/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './applicationReportFilter.html';
import { policyTypes } from '../dashboard/filter/staticFilterEntries';
import { equals, head, last, map, pick, range, reduce, reject } from 'ramda';
import { fetchStageTypes } from '../stages/stagesActions';
import { lookup, setToArray, union } from '../util/jsUtil';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportFilterController,
};

// Map from checkbox option id to violationState filter set
const violationStateCheckboxFilterMapping = {
  notViolating: new Set(['notViolating']),
  open: new Set(['open']),
  waived: new Set(['waived', 'waived+grandfathered']),
  grandfathered: new Set(['grandfathered', 'waived+grandfathered']),
};

export function ApplicationReportFilterController(
  $scope,
  $ngRedux,
  applicationReportActions
) {
  const vm = this;

  Object.assign(vm, {
    availableProprietaryFilterOptions: [
      { id: false, name: 'Non-Proprietary' },
      { id: true, name: 'Proprietary' },
    ],

    availableMatchStateFilterOptions: [
      { id: 'exact', name: 'Exact' },
      { id: 'similar', name: 'Similar' },
      { id: 'unknown', name: 'Unknown' },
    ],

    availableViolationStateFilterOptions: [
      { id: 'notViolating', name: 'Not Violating' },
      { id: 'open', name: 'Open' },
      { id: 'waived', name: 'Waived' },
      { id: 'grandfathered', name: 'Grandfathered' },
    ],

    availableDependencyTypeFilterOptions: [
      { id: 'direct', name: 'Direct Dependencies' },
      { id: 'transitive', name: 'Transitive Dependencies' },
      { id: 'unknown', name: 'Unknown' },
    ],

    availablePolicyTypeFilterOptions: policyTypes,

    violationStateCheckedIds: new Set(),

    policyThreatLevelFilterSelectedRange: undefined,

    faArrowToRightIcon: faArrowToRight,

    $onInit() {
      const actions = {
        ...pick(
          [
            'setAggregateReportEntries',
            'setExactValueFilter',
            'reevaluateReport',
            'loadInnerSourceReports',
            'toggleFilterSidebar',
          ],
          applicationReportActions
        ),
        fetchStageTypes,
      };

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
      vm.fetchStageTypes('action');
      document.addEventListener('keydown', vm.escapeKeyHandler);
      document.addEventListener('mousedown', vm.offClickHandler);

      $scope.$watch(
        'vm.exactValueFilters.derivedViolationState',
        function (derivedViolationState) {
          const violationStateFilter = derivedViolationState || new Set(),
            // the 'waived+grandfathered' value is redundant for these purposes, and the other possible values
            // all map perfectly to the checkbox ids
            checkedIds = reject(
              equals('waived+grandfathered'),
              setToArray(violationStateFilter)
            );

          vm.violationStateCheckedIds = new Set(checkedIds);
        }
      );

      $scope.$watch(
        'vm.exactValueFilters.policyThreatLevel',
        function (allowedValues) {
          vm.policyThreatLevelFilterSelectedRange = toSelectedRange(
            allowedValues
          );
        }
      );
    },

    $onDestroy() {
      vm.unsubscribe();
      document.removeEventListener('keydown', vm.escapeKeyHandler);
      document.removeEventListener('mousedown', vm.offClickHandler);
    },

    setProprietaryFilterOptions(selectedIds) {
      vm.setExactValueFilter('proprietary', selectedIds);
    },

    setMatchStateFilterOptions(selectedIds) {
      vm.setExactValueFilter('matchState', selectedIds);
    },

    setViolationStateFilterOptions(selectedIds) {
      const selectedFilters = map(
          lookup(violationStateCheckboxFilterMapping),
          setToArray(selectedIds)
        ),
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
      vm.setExactValueFilter(
        'policyThreatLevel',
        fromSelectedRange(selectedRange)
      );
    },

    closeSideBarFilterIfOpen() {
      if (vm.filterSidebarOpen) {
        vm.toggleFilterSidebar(false);
      }
    },

    escapeKeyHandler({ key }) {
      if (key === 'Escape' || key === 'Esc') {
        vm.closeSideBarFilterIfOpen();
      }
    },

    offClickHandler(event) {
      if (!vm.filterSidebarOpen) {
        return;
      }

      const filterMainElement = document.getElementById(
        'application-report-sidebar'
      );
      const isClickOnReportFilter = filterMainElement.contains(event.target);
      if (!isClickOnReportFilter) {
        vm.closeSideBarFilterIfOpen();
      }
    },
  });
}

function mapStateToThis(state) {
  return pick(
    [
      'policyTypeFilterEnabled',
      'aggregate',
      'exactValueFilters',
      'reportParameters',
      'filterSidebarOpen',
    ],
    state.applicationReport || {}
  );
}

ApplicationReportFilterController.$inject = [
  '$scope',
  '$ngRedux',
  'applicationReportActions',
];

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
