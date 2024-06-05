/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import { createPortal } from 'react-dom';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxDrawer, NxCollapsibleItems, NxCheckbox, NxThreatIndicator } from '@sonatype/react-shared-components';
import { debounce } from 'debounce';
import { keys, map } from 'ramda';

import { actions } from '../billOfMaterialsComponentsTileSlice';
import { selectBillOfMaterialsComponentsTile } from '../billOfMaterialsComponentsTileSelectors';

import { capitalize } from 'MainRoot/util/jsUtil';

import './componentsFilterDrawer.scss';

export const COMPONENTS_FILTER_DRAWER_PORTAL_TARGET_CLASSNAME = 'nx-page-content';

const LOAD_COMPONENTS_DEBOUNCE_TIMEOUT_MS = 750;

const THREAT_INDICATOR_MAP = {
  critical: 'critical',
  high: 'severe',
  medium: 'moderate',
  low: 'low',
};

export default function ComponentsFilterDrawer({ internalAppId, sbomVersion }) {
  const {
    filterDrawer: { showDrawer, collapsibleItems },
    filterConfiguration,
  } = useSelector(selectBillOfMaterialsComponentsTile);

  const dispatch = useDispatch();
  const toggleDrawer = () => dispatch(actions.toggleShowFilterDrawer());

  const debouncedLoadComponents = useCallback(
    debounce(
      () => dispatch(actions.loadComponents({ internalAppId, sbomVersion })),
      LOAD_COMPONENTS_DEBOUNCE_TIMEOUT_MS
    ),
    []
  );

  const toggleShowDependencyTypesCollapsibleItems = () =>
    dispatch(actions.setShowDependencyTypesCollapsibleItems(!collapsibleItems.showDependencyTypes));

  const toggleShowVulnerabilityThreatLevelsCollapsibleItems = () =>
    dispatch(actions.setShowVulnerabilityThreatLevelsCollapsibleItems(!collapsibleItems.showVulnerabilityThreatLevels));

  const createFilterCheckboxHandler = (filterState, filterSetter) => (field) => ({
    className: 'sbom-manager-components-filter-drawer__checkbox',
    isChecked: filterState[field],
    onChange: (value) => {
      dispatch(actions.setLoadingComponents(true));
      dispatch(filterSetter({ field, value }));
      dispatch(actions.setCurrentPage(0));
      debouncedLoadComponents();
    },
    inputAttributes: { role: 'menuitemcheckbox' },
  });

  const createFilterDependencyTypeCheckboxHandler = createFilterCheckboxHandler(
    filterConfiguration.dependencyTypes,
    actions.setFilterDependencyTypes
  );

  const createFilterVulnerabilityThreatLevelTypeCheckboxHandler = createFilterCheckboxHandler(
    filterConfiguration.vulnerabilityThreatLevels,
    actions.setFilterVulnerabilityThreatLevels
  );

  const vulnerabilityThreatLevelCollapsibleItems = map(
    (field) => (
      <NxCollapsibleItems.Child key={field}>
        <NxCheckbox {...createFilterVulnerabilityThreatLevelTypeCheckboxHandler(field)}>
          <NxThreatIndicator threatLevelCategory={THREAT_INDICATOR_MAP[field]} presentational />
          <span>{capitalize(field)}</span>
        </NxCheckbox>
      </NxCollapsibleItems.Child>
    ),
    keys(filterConfiguration.vulnerabilityThreatLevels)
  );

  const dependencyTypeCollapsibleItems = map(
    (field) => (
      <NxCollapsibleItems.Child key={field}>
        <NxCheckbox {...createFilterDependencyTypeCheckboxHandler(field)}>{capitalize(field)}</NxCheckbox>
      </NxCollapsibleItems.Child>
    ),
    keys(filterConfiguration.dependencyTypes)
  );

  const collapsibleItemsTriggerContent = (title, filterCount, filterCountTestId) => (
    <span className="sbom-manager-components-filter-drawer__collapsible-items__trigger-element">
      <span>{title}</span>
      <span
        className="sbom-manager-components-filter-drawer__collapsible-items__filter-count"
        data-testid={filterCountTestId}
      >
        {filterCount}
      </span>
    </span>
  );

  return createPortal(
    <NxDrawer
      id="components-filter-drawer"
      className="sbom-manager-components-filter-drawer__collapsible-items sbom-manager-components-filter-drawer"
      aria-labelledby="components-filter-drawer-title"
      open={showDrawer}
      onClose={() => toggleDrawer()}
    >
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle id="components-filter-drawer-title">Filter By</NxDrawer.HeaderTitle>
      </NxDrawer.Header>
      <NxDrawer.Content tabIndex={0}>
        <NxCollapsibleItems
          className="sbom-manager-components-filter-drawer__collapsible-items sbom-manager-components-filter-drawer__vulnerability-threat-level"
          isOpen={collapsibleItems.showVulnerabilityThreatLevels}
          onToggleCollapse={toggleShowVulnerabilityThreatLevelsCollapsibleItems}
          triggerContent={collapsibleItemsTriggerContent(
            'Vulnerability Threat Level',
            vulnerabilityThreatLevelCollapsibleItems.length,
            'vulnerability-threat-level-filter-count'
          )}
        >
          {vulnerabilityThreatLevelCollapsibleItems}
        </NxCollapsibleItems>
        <NxCollapsibleItems
          className="sbom-manager-components-filter-drawer__collapsible-items sbom-manager-components-filter-drawer__dependency-type"
          isOpen={collapsibleItems.showDependencyTypes}
          onToggleCollapse={toggleShowDependencyTypesCollapsibleItems}
          triggerContent={collapsibleItemsTriggerContent(
            'Dependency Type',
            dependencyTypeCollapsibleItems.length,
            'dependency-type-filter-count'
          )}
        >
          {dependencyTypeCollapsibleItems}
        </NxCollapsibleItems>
      </NxDrawer.Content>
    </NxDrawer>,
    document.querySelector(`.${COMPONENTS_FILTER_DRAWER_PORTAL_TARGET_CLASSNAME}`)
  );
}

ComponentsFilterDrawer.propTypes = {
  internalAppId: PropTypes.string,
  sbomVersion: PropTypes.string,
};
