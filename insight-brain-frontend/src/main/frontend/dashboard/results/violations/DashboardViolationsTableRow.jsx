/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTableCell, NxTableRow, NxThreatIndicator, NxOverflowTooltip } from '@sonatype/react-shared-components';

import { terseAgo } from '../../../utilAngular/CommonServices';
import ComponentDisplay from '../../../ComponentDisplay/ReactComponentDisplay';

export default function DashboardViolationsTableRow({ stateGo, violation, page }) {
  const { policyViolationId, threatLevel, policyName, applicationName, firstOccurrenceTime } = violation,
    displayTime = terseAgo(firstOccurrenceTime);

  const goToViolationDetails = () => {
    stateGo('sidebarView.violation', {
      id: policyViolationId,
      type: 'violation',
      sidebarReference: 'filter',
      page: page + 1,
    });
  };

  return (
    <NxTableRow key={policyViolationId} onClick={goToViolationDetails} className="iq-dashboard-violation" isClickable>
      <NxTableCell className="iq-threat-cell">
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span className="nx-threat-number">{threatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-policy-cell">
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{policyName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{applicationName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell>
        <ComponentDisplay component={violation} truncate={true} />
      </NxTableCell>
      <NxTableCell>{displayTime}</NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

export const violationPropTypes = PropTypes.shape({
  policyViolationId: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  policyName: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  firstOccurrenceTime: PropTypes.number.isRequired,
});

DashboardViolationsTableRow.propTypes = {
  stateGo: PropTypes.func.isRequired,
  violation: violationPropTypes,
  page: PropTypes.number,
};
