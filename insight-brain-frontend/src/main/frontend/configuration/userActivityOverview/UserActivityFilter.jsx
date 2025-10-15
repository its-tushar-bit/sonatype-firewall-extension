/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulTreeViewRadioSelect, NxFooter, NxButton, NxDrawer } from '@sonatype/react-shared-components';

import PortalDrawer from '../../react/PortalDrawer';

// Age filter options - limited to short-term periods for audit compliance
const ages = [
  { name: 'past 24 hours', id: 1 },
  { name: 'past 7 days', id: 7 },
  { name: 'past 30 days', id: 30 },
];

export default function UserActivityFilter({
  isOpen,
  onClose,
  selectedAge,
  onAgeChange,
  onApply,
  onReset,
  filtersAreDirty,
}) {
  // Convert age numbers to strings for NxTreeViewRadioSelect
  const stringifyAgeOption = ({ id, ...rest }) => ({
    ...rest,
    id: id.toString(),
  });

  const stringifiedAges = ages.map(stringifyAgeOption);
  const stringifiedSelectedAge = selectedAge.toString();

  const handleAgeChange = (selectedAgeStr) => {
    const ageAsNumber = parseInt(selectedAgeStr, 10);
    onAgeChange(ageAsNumber);
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
          <NxStatefulTreeViewRadioSelect
            id="user-activity-age-filter"
            options={stringifiedAges}
            name="Time Frame Filter"
            onChange={handleAgeChange}
            selectedId={stringifiedSelectedAge}
          >
            <span>Time Frame</span>
          </NxStatefulTreeViewRadioSelect>
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

UserActivityFilter.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  selectedAge: PropTypes.number,
  onAgeChange: PropTypes.func.isRequired,
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
};
