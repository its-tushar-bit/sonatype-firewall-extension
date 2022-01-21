/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFieldset, NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';

import IqTreeViewPolicyThreatSlider from '../../react/IqTreeViewPolicyThreatSlider';
import { policyTypes } from '../../dashboard/filter/staticFilterEntries';
import { lookup, setToArray, union } from '../../util/jsUtil';
import * as PropTypes from 'prop-types';
import { equals, head, last, map, range, reduce, reject } from 'ramda';

const proprietaryFilterOptions = [
  { id: 'nonProprietary', name: 'Non-Proprietary' },
  { id: 'proprietary', name: 'Proprietary' },
];

const matchStateFilterOptions = [
  { id: 'exact', name: 'Exact' },
  { id: 'similar', name: 'Similar' },
  { id: 'unknown', name: 'Unknown' },
];

const violationStateFilterOptions = [
  { id: 'notViolating', name: 'Not Violating' },
  { id: 'open', name: 'Open' },
  { id: 'waived', name: 'Waived' },
  { id: 'grandfathered', name: 'Grandfathered' },
];

const dependencyTypeFilterOptions = [
  { id: 'direct', name: 'Direct Dependencies' },
  { id: 'transitive', name: 'Transitive Dependencies' },
  { id: 'unknown', name: 'Unknown' },
];

// Map from checkbox option id to violationState filter set
const violationStateCheckboxFilterMapping = {
  notViolating: new Set(['notViolating']),
  open: new Set(['open']),
  waived: new Set(['waived', 'waived+grandfathered']),
  grandfathered: new Set(['grandfathered', 'waived+grandfathered']),
};

export default function ReportFilters(props) {
  const {
    //actions
    setExactValueFilter,
    //state
    exactValueFilters,
  } = props;

  const { proprietary = [], derivedViolationState = new Set(), policyThreatLevel } = exactValueFilters;

  const derivedSelectedProprietaryOptions = new Set(
    [...proprietary].map((option) => (option ? 'proprietary' : 'nonProprietary'))
  );

  const violationStateCheckedIds = new Set(reject(equals('waived+grandfathered'), setToArray(derivedViolationState)));

  const onSelectionChange = (filterName, selectedIds) => {
    setExactValueFilter(filterName, new Set(mapActionIds(filterName, selectedIds)));
  };

  const setSelectedProprietaryOptions = (selectedIds) => onSelectionChange('proprietary', selectedIds),
    setSelectedMatchStateOptions = (selectedIds) => onSelectionChange('matchState', selectedIds),
    setSelectedDependencyTypeOptions = (selectedIds) => onSelectionChange('derivedDependencyType', selectedIds),
    setSelectedPolicyTypeOptions = (selectedIds) => onSelectionChange('policyThreatCategory', selectedIds);

  /**
   * The new react report filters uses `NxTreeViewMultiSelect` which supports a list of strings as options.
   *  whereas the existing angular report adopts a boolean true/false for the proprietary/Non-proprietary values
   *  (also dispatches the same action value). This function translates the new string value to the existing boolean,
   *  so that the same action dispatch can be reused.
   */
  const mapActionIds = (filterName, selectedIds) => {
    if (filterName === 'proprietary') {
      selectedIds = [...selectedIds].map((id) => id === 'proprietary');
    }
    return selectedIds;
  };

  const setSelectedViolationStateOptions = (selectedIds) => {
    const selectedFilters = map(lookup(violationStateCheckboxFilterMapping), setToArray(selectedIds)),
      mergedFilter = reduce(union, new Set(), selectedFilters);
    setExactValueFilter('derivedViolationState', mergedFilter);
  };

  const setSelectedPolicyThreatChange = (selectedRange) => {
    setExactValueFilter('policyThreatLevel', fromSelectedPolicyThreatRange(selectedRange));
  };

  const policyThreatLevelFilterSelectedRange = toSelectedRange(policyThreatLevel);

  function toSelectedRange(allowedValues) {
    if (allowedValues && allowedValues.size) {
      const rangeArray = setToArray(allowedValues);
      return [Math.min(...rangeArray), Math.max(...rangeArray)];
    }
    // if filter is empty - set slider to full range
    return [0, 10];
  }

  const fromSelectedPolicyThreatRange = (selectedRange) => {
    // if whole range is selected - don't do any filtering
    return equals([0, 10], selectedRange) ? new Set() : new Set(range(head(selectedRange), last(selectedRange) + 1));
  };

  return (
    <aside className="nx-page-sidebar" id="report-sidebar">
      <NxFieldset label="Filters">
        <div className="report-filters">
          <NxStatefulTreeViewMultiSelect
            options={proprietaryFilterOptions}
            selectedIds={derivedSelectedProprietaryOptions}
            onChange={setSelectedProprietaryOptions}
            name="proprietary-filter"
            id="proprietary-filter"
          >
            <span>Proprietary</span>
          </NxStatefulTreeViewMultiSelect>

          <NxStatefulTreeViewMultiSelect
            options={matchStateFilterOptions}
            selectedIds={exactValueFilters.matchState}
            onChange={setSelectedMatchStateOptions}
            name="match-state-filter"
            id="match-state-filter"
          >
            <span>Component Match State</span>
          </NxStatefulTreeViewMultiSelect>

          <NxStatefulTreeViewMultiSelect
            options={violationStateFilterOptions}
            selectedIds={violationStateCheckedIds}
            onChange={setSelectedViolationStateOptions}
            name="violation-state-filter"
            id="violation-state-filter"
          >
            <span>Violation State</span>
          </NxStatefulTreeViewMultiSelect>

          <NxStatefulTreeViewMultiSelect
            options={dependencyTypeFilterOptions}
            selectedIds={exactValueFilters.derivedDependencyType}
            onChange={setSelectedDependencyTypeOptions}
            name="dependency-type-filter"
            id="dependency-type-filter"
          >
            <span>Dependency Type</span>
          </NxStatefulTreeViewMultiSelect>

          <NxStatefulTreeViewMultiSelect
            options={policyTypes}
            selectedIds={exactValueFilters.policyThreatCategory}
            onChange={setSelectedPolicyTypeOptions}
            name="policy-type-filter"
            id="policy-type-filter"
          >
            <span>Policy Types</span>
          </NxStatefulTreeViewMultiSelect>

          <IqTreeViewPolicyThreatSlider
            id="threat-level-filter"
            value={policyThreatLevelFilterSelectedRange}
            onChange={setSelectedPolicyThreatChange}
          >
            <span>Policy Threat Level</span>
          </IqTreeViewPolicyThreatSlider>
        </div>
      </NxFieldset>
    </aside>
  );
}

ReportFilters.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }),
  exactValueFilters: PropTypes.object.isRequired,
  setExactValueFilter: PropTypes.func.isRequired,
};
