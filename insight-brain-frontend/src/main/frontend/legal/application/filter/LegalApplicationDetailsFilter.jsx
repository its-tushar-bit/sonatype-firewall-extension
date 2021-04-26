/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { curryN, prop, sortBy } from 'ramda';
import { expandedProgressOptions } from '../../dashboard/legalDashboardConstants';

export default function LegalApplicationDetailsFilter(props) {
  const { toggleFilter, licenseThreatGroups, selected } = props;

  const licenseThreatGroupOptions = sortBy(prop('name'))(
    licenseThreatGroups.map((group) => ({ id: group, name: group }))
  );

  const curriedToggleFilter = curryN(2, toggleFilter);
  const onLicenseThreatGroupsChange = curriedToggleFilter('licenseThreatGroups');
  const onProgressOptionsChange = curriedToggleFilter('progressOptions');

  return (
    <div className="dashboard-filter nx-viewport-sized__scrollable">
      <Fragment>
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
      </Fragment>
    </div>
  );
}

LegalApplicationDetailsFilter.propTypes = {
  licenseThreatGroups: PropTypes.array,
  selected: PropTypes.shape({
    progressOptions: PropTypes.instanceOf(Set).isRequired,
    licenseThreatGroups: PropTypes.instanceOf(Set).isRequired,
  }),
  toggleFilter: PropTypes.func,
};
