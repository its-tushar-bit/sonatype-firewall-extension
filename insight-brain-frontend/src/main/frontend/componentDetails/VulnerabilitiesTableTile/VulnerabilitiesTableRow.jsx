/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

const VulnerabilitiesTableRow = ({ vulnerability, toggleVulnerabilityPopoverWithEffects }) => {
  return (
    <NxTableRow
      className="iq-policy-violation-row"
      isClickable
      onClick={() => toggleVulnerabilityPopoverWithEffects(vulnerability.refId)}
    >
      <NxTableCell>
        <span>{Math.floor(vulnerability.severity)}</span>
      </NxTableCell>
      <NxTableCell>
        <span>{vulnerability.refId}</span>
      </NxTableCell>
      <NxTableCell>
        <span>{vulnerability.status}</span>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
};

export const vulnerabilitiyPropTypes = PropTypes.shape({
  refId: PropTypes.string.isRequired,
  severity: PropTypes.number.isRequired,
  status: PropTypes.string.isRequired,
});

VulnerabilitiesTableRow.propTypes = {
  vulnerability: vulnerabilitiyPropTypes,
  toggleVulnerabilityPopoverWithEffects: PropTypes.func.isRequired,
};

export default VulnerabilitiesTableRow;
