/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxButtonBar,
  NxDrawer,
  NxFooter,
  NxStatefulCollapsibleMultiSelect,
} from '@sonatype/react-shared-components';
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import PortalDrawer from 'MainRoot/react/PortalDrawer';
import IqTreeViewPolicyThreatSlider from 'MainRoot/react/IqTreeViewPolicyThreatSlider';

import selectContainerRepositoryResultsPage from '../containerRepositoryResultsPageSelectors';
import { actions } from '../containerRepositoryResultsPageSlice';

const VIOLATION_STATE_FILTERS = [
  { id: 'VIOLATION_STATE_NOT_VIOLATING', name: 'Not Violating' },
  { id: 'VIOLATION_STATE_OPEN', name: 'Open' },
  { id: 'VIOLATION_STATE_QUARANTINED', name: 'Quarantined' },
  { id: 'VIOLATION_STATE_WAIVED', name: 'Waived' },
];

const ContainerRepositoryResultsFilterDrawer = () => {
  const dispatch = useDispatch();

  const { showFilterDrawer, violationStateFilters, threatLevelRange } = useSelector(
    selectContainerRepositoryResultsPage
  );

  const setShowComponentsFilterDrawer = (value) => dispatch(actions.setShowFilterDrawer(value));

  const applyFilters = () => {
    dispatch(actions.setLoading(true));
    dispatch(actions.setPage(1));
    dispatch(actions.loadTable());
    dispatch(actions.setLoading(false));
  };

  const clearFilters = () => {
    dispatch(actions.clearDrawerFilters());
    dispatch(actions.setPage(1));
    dispatch(actions.loadTable());
  };

  return (
    <PortalDrawer
      id="container-results-filter-drawer"
      data-testid="container-results-filter-drawer"
      aria-labelledby="container-results-filter-drawer-header"
      open={showFilterDrawer}
      onClose={() => setShowComponentsFilterDrawer(false)}
      variant="narrow"
    >
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle id="container-results-filter-drawer-header">Filters</NxDrawer.HeaderTitle>
      </NxDrawer.Header>
      <NxDrawer.Content>
        <div className="container-results-filter-drawer-filters">
          <NxStatefulCollapsibleMultiSelect
            id="container-results-filter-drawer__violations-filter"
            name="violations-filter"
            options={VIOLATION_STATE_FILTERS}
            selectedIds={new Set(violationStateFilters)}
            onChange={(selectedIds) => {
              dispatch(actions.setViolationStateFilters(Array.from(selectedIds)));
            }}
          >
            <span>Violations</span>
          </NxStatefulCollapsibleMultiSelect>

          <IqTreeViewPolicyThreatSlider
            id="container-results-filter-drawer__policy-threat-slider"
            value={[...threatLevelRange]}
            onChange={(newThreatLevelRange) => {
              dispatch(actions.setThreatLevelRange(newThreatLevelRange));
            }}
          >
            <span>Policy Threat Level</span>
          </IqTreeViewPolicyThreatSlider>
        </div>
      </NxDrawer.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton id="container-results-filter-drawer__clear-button" onClick={clearFilters}>
            Clear
          </NxButton>
          <NxButton id="container-results-filter-drawer__apply-button" variant="primary" onClick={applyFilters}>
            Apply
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </PortalDrawer>
  );
};

export default ContainerRepositoryResultsFilterDrawer;
