/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulTreeViewMultiSelect,
  NxTooltip,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { curryN, prop, sortBy } from 'ramda';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';
import { expandedProgressOptions } from '../../dashboard/legalDashboardConstants';
import IqPopover from '../../../react/IqPopover';

export default function LegalApplicationDetailsFilter(props) {
  const { toggleFilter, licenseThreatGroups, selected, toggleFilterSidebar } = props;

  const licenseThreatGroupOptions = sortBy(prop('name'))(
    licenseThreatGroups.map((group) => ({ id: group, name: group }))
  );

  const curriedToggleFilter = curryN(2, toggleFilter);
  const onLicenseThreatGroupsChange = curriedToggleFilter('licenseThreatGroups');
  const onProgressOptionsChange = curriedToggleFilter('progressOptions');

  return (
    <IqPopover onClose={() => toggleFilterSidebar(false)}>
      <IqPopover.Header className="legal-application-details-filter-header">
        <div className="legal-application-details-filter-header__title">
          <h3 className="nx-h3 legal-application-details-filter-header__title-text">Filter</h3>
          <NxTooltip id="legal-dashboard-filter-close-btn-tooltip" placement="top-end">
            <NxButton
              id="legal-dashboard-filter-close-btn"
              onClick={() => toggleFilterSidebar(false)}
              variant="icon-only"
            >
              <NxFontAwesomeIcon icon={faArrowToRight} />
            </NxButton>
          </NxTooltip>
        </div>
      </IqPopover.Header>
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
  licenseThreatGroups: PropTypes.array,
  selected: PropTypes.shape({
    progressOptions: PropTypes.instanceOf(Set).isRequired,
    licenseThreatGroups: PropTypes.instanceOf(Set).isRequired,
  }),
  toggleFilter: PropTypes.func,
  toggleFilterSidebar: PropTypes.func,
};
