/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { curryN, prop, sortBy } from 'ramda';
import { expandedProgressOptions } from '../../dashboard/legalDashboardConstants';
import IqPopover from '../../../react/IqPopover';

export default function LegalApplicationDetailsFilter(props) {
  const {
    toggleFilter,
    components: { licenseThreatGroups },
    selected,
    toggleFilterSidebar,
  } = props;

  const licenseThreatGroupOptions = sortBy(prop('name'))(
    licenseThreatGroups.map((group) => ({ id: group, name: group }))
  );

  const curriedToggleFilter = curryN(2, toggleFilter);
  const onLicenseThreatGroupsChange = curriedToggleFilter('licenseThreatGroups');
  const onProgressOptionsChange = curriedToggleFilter('progressOptions');

  return (
    <IqPopover onClose={() => toggleFilterSidebar(false)}>
      <IqPopover.Header
        className="legal-application-details-filter-header"
        buttonId="legal-dashboard-filter-close-btn"
        onClose={() => toggleFilterSidebar(false)}
        headerSize="h3"
        headerTitle="Filter"
      />
      <div className="legal-application-details-filter">
        <NxStatefulTreeViewMultiSelect
          options={expandedProgressOptions}
          selectedIds={selected.progressOptions}
          onChange={onProgressOptionsChange}
          filterPlaceholder="Review Progress"
          name="progressOptions"
          id="legal-progress-options-filter"
        >
          Review Status
        </NxStatefulTreeViewMultiSelect>
        <NxStatefulTreeViewMultiSelect
          options={licenseThreatGroupOptions}
          selectedIds={licenseThreatGroupOptions.length > 0 ? selected.licenseThreatGroups : null}
          onChange={onLicenseThreatGroupsChange}
          filterPlaceholder="License Threat Groups"
          name="licenseThreatGroups"
          id="legal-license-threat-groups-filter"
        >
          License Threat Group
        </NxStatefulTreeViewMultiSelect>
      </div>
    </IqPopover>
  );
}

LegalApplicationDetailsFilter.propTypes = {
  components: PropTypes.shape({
    licenseThreatGroups: PropTypes.array,
  }),
  selected: PropTypes.shape({
    progressOptions: PropTypes.instanceOf(Set).isRequired,
    licenseThreatGroups: PropTypes.instanceOf(Set).isRequired,
  }),
  toggleFilter: PropTypes.func,
  toggleFilterSidebar: PropTypes.func,
};
