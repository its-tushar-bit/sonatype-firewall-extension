/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulTreeViewMultiSelect, NxFooter, NxButton, NxDrawer } from '@sonatype/react-shared-components';

import PortalDrawer from 'MainRoot/react/PortalDrawer';

export default function UserActivityDetailsFilter({
  isOpen,
  onClose,
  selectedActivityTypes,
  selectedDomains,
  selectedErrorTypes,
  filterOptions,
  onActivityTypesChange,
  onDomainsChange,
  onErrorTypesChange,
  onApply,
  onReset,
  filtersAreDirty,
}) {
  // Convert filter options to TreeView format (no "All" option needed for multi-select)
  const activityTypeOptions = filterOptions.activityTypes.map((type) => ({ name: type, id: type }));

  const domainOptions = filterOptions.domains.map((domain) => ({ name: domain, id: domain }));

  const errorTypeOptions = filterOptions.errorTypes.map((status) => ({ name: status, id: status }));

  // Convert arrays to Sets for RSC components
  const activityTypeSelectedIds = new Set(Array.isArray(selectedActivityTypes) ? selectedActivityTypes : []);
  const domainSelectedIds = new Set(Array.isArray(selectedDomains) ? selectedDomains : []);
  const errorTypeSelectedIds = new Set(Array.isArray(selectedErrorTypes) ? selectedErrorTypes : []);

  // Handlers to convert Set back to Array for Redux state
  const handleActivityTypesChange = (selectedSet) => {
    onActivityTypesChange(Array.from(selectedSet));
  };

  const handleDomainsChange = (selectedSet) => {
    onDomainsChange(Array.from(selectedSet));
  };

  const handleErrorTypesChange = (selectedSet) => {
    onErrorTypesChange(Array.from(selectedSet));
  };

  const handleApply = () => {
    onApply();
    onClose();
  };

  const handleReset = () => {
    onReset();
    onClose();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <PortalDrawer open={isOpen} onClose={onClose} variant="narrow">
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle>Filters</NxDrawer.HeaderTitle>
      </NxDrawer.Header>

      <NxDrawer.Content>
        <div className="nx-form-group">
          <NxStatefulTreeViewMultiSelect
            id="user-activity-type-filter"
            options={activityTypeOptions}
            name="Activity Type Filter"
            onChange={handleActivityTypesChange}
            selectedIds={activityTypeSelectedIds}
            filterPlaceholder="Activity Type"
          >
            <span>Activity Type</span>
          </NxStatefulTreeViewMultiSelect>
        </div>

        <div className="nx-form-group">
          <NxStatefulTreeViewMultiSelect
            id="user-activity-domain-filter"
            options={domainOptions}
            name="Domain Filter"
            onChange={handleDomainsChange}
            selectedIds={domainSelectedIds}
            filterPlaceholder="Domain"
          >
            <span>Domain</span>
          </NxStatefulTreeViewMultiSelect>
        </div>

        <div className="nx-form-group">
          <NxStatefulTreeViewMultiSelect
            id="user-activity-error-type-filter"
            options={errorTypeOptions}
            name="Error Type Filter"
            onChange={handleErrorTypesChange}
            selectedIds={errorTypeSelectedIds}
            filterPlaceholder="Error Type"
          >
            <span>Error Type</span>
          </NxStatefulTreeViewMultiSelect>
        </div>
      </NxDrawer.Content>

      <NxFooter>
        <div className="nx-btn-bar">
          <NxButton variant="tertiary" onClick={handleReset} disabled={!filtersAreDirty}>
            Reset
          </NxButton>
          <NxButton variant="primary" onClick={handleApply} disabled={!filtersAreDirty}>
            Apply
          </NxButton>
        </div>
      </NxFooter>
    </PortalDrawer>
  );
}

UserActivityDetailsFilter.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  selectedActivityTypes: PropTypes.arrayOf(PropTypes.string),
  selectedDomains: PropTypes.arrayOf(PropTypes.string),
  selectedErrorTypes: PropTypes.arrayOf(PropTypes.string),
  filterOptions: PropTypes.shape({
    activityTypes: PropTypes.arrayOf(PropTypes.string),
    domains: PropTypes.arrayOf(PropTypes.string),
    errorTypes: PropTypes.arrayOf(PropTypes.string),
  }).isRequired,
  onActivityTypesChange: PropTypes.func.isRequired,
  onDomainsChange: PropTypes.func.isRequired,
  onErrorTypesChange: PropTypes.func.isRequired,
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
};
