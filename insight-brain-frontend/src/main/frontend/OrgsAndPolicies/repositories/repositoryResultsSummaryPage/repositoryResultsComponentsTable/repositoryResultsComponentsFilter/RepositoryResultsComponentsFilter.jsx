/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import IqPopover from 'MainRoot/react/IqPopover';
import {
  NxButton,
  NxButtonBar,
  NxDrawer,
  NxFooter,
  NxStatefulCollapsibleMultiSelect,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectMatchStateFilters,
  selectShowFilterPopover,
  selectThreatLevelFilters,
  selectViolationStateFilters,
} from '../../repositoryResultsSummaryPageSelectors';
import { actions } from '../../repositoryResultsSummaryPageSlice';
import * as PropTypes from 'prop-types';
import IqTreeViewPolicyThreatSlider from 'MainRoot/react/IqTreeViewPolicyThreatSlider';

const RepositoryResultsComponentsFilter = ({ repositoryId }) => {
  const dispatch = useDispatch();

  const matchStateFilters = [
    { id: 'MATCH_STATE_EXACT', name: 'Exact' },
    { id: 'MATCH_STATE_UNKNOWN', name: 'Unknown' },
  ];

  const violationStateFilters = [
    { id: 'VIOLATION_STATE_NOT_VIOLATING', name: 'Not Violating' },
    { id: 'VIOLATION_STATE_OPEN', name: 'Open' },
    { id: 'VIOLATION_STATE_QUARANTINED', name: 'Quarantined' },
    { id: 'VIOLATION_STATE_WAIVED', name: 'Waived' },
  ];

  const selectedViolationStateFilters = useSelector(selectViolationStateFilters);
  const selectedMatchStateFilters = useSelector(selectMatchStateFilters);
  const showFilterPopover = useSelector(selectShowFilterPopover);
  const threatLevelFilters = useSelector(selectThreatLevelFilters);

  const applyFilters = () => {
    dispatch(actions.applyFilters());
    dispatch(actions.getRepositoryComponents(repositoryId));
  };
  const clearFilters = () => {
    dispatch(actions.clearFilters());
  };
  const toggleMatchStateFilters = (payload) => dispatch(actions.changeMachstateFilters(payload));
  const toggleViolationStateFilters = (payload) => dispatch(actions.changeViolationStateFilters(payload));
  const closeFilterPopover = () => dispatch(actions.setShowFilterPopover(false));
  const changeThreatLevelFilters = (payload) => dispatch(actions.changeThreatLevelFilters(payload));

  return (
    <NxDrawer
      id="iq-summary-page-components-filter"
      variant="narrow"
      data-testid="components-filter-popover"
      open={showFilterPopover}
      onClose={closeFilterPopover}
      aria-labelledby="repository-results-component-filter-header"
    >
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle id="repository-results-component-filter-header">Filters</NxDrawer.HeaderTitle>
      </NxDrawer.Header>
      <NxDrawer.Content>
        <div className="iq-summary-page-filters">
          <NxStatefulCollapsibleMultiSelect
            options={matchStateFilters}
            selectedIds={selectedMatchStateFilters}
            onChange={toggleMatchStateFilters}
            name="components-match-state-filter"
            id="components-match-state-filter"
          >
            <span>Component Match State</span>
          </NxStatefulCollapsibleMultiSelect>
          <NxStatefulCollapsibleMultiSelect
            options={violationStateFilters}
            selectedIds={selectedViolationStateFilters}
            onChange={toggleViolationStateFilters}
            name="components-violations-filter"
            id="components-violations-filter"
          >
            <span>Violations</span>
          </NxStatefulCollapsibleMultiSelect>

          <IqTreeViewPolicyThreatSlider
            id="repository-threat-level-filter"
            value={[...threatLevelFilters]}
            onChange={(threatLevelRange) => changeThreatLevelFilters(threatLevelRange)}
          >
            <span>Policy Threat Level</span>
          </IqTreeViewPolicyThreatSlider>
        </div>
      </NxDrawer.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton id="iq-summary-page-components-filter__clear" onClick={clearFilters}>
            Clear
          </NxButton>
          <NxButton id="iq-summary-page-components-filter__apply" variant="primary" onClick={applyFilters}>
            Apply
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxDrawer>
  );
};

export default RepositoryResultsComponentsFilter;

RepositoryResultsComponentsFilter.propTypes = {
  repositoryId: PropTypes.string.isRequired,
};
