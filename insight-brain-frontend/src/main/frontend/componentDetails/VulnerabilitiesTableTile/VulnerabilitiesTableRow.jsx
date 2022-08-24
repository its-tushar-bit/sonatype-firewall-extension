/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

const VulnerabilitiesTableRow = ({ vulnerability, toggleVulnerabilityPopoverWithEffects }) => {
  const renderAliases = (aliases) => (!aliases ? '' : aliases.map((a) => <div key={a}>{a}</div>));
  return (
    <NxTableRow
      className="iq-vulnerabilities-row"
      isClickable
      onClick={() => toggleVulnerabilityPopoverWithEffects(vulnerability.refId)}
    >
      <NxTableCell>
        <span>{Math.floor(vulnerability.severity)}</span>
      </NxTableCell>
      <NxTableCell>
        <div>{vulnerability.refId}</div>
        {!vulnerability?.aliases?.isEmpty && renderAliases(vulnerability.aliases)}
      </NxTableCell>
      <NxTableCell>
        <span>{vulnerability.status}</span>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
};

export const vulnerabilityPropTypes = PropTypes.shape({
  refId: PropTypes.string.isRequired,
  severity: PropTypes.number.isRequired,
  status: PropTypes.string.isRequired,
  aliases: PropTypes.arrayOf(PropTypes.string),
});

VulnerabilitiesTableRow.propTypes = {
  vulnerability: vulnerabilityPropTypes,
  toggleVulnerabilityPopoverWithEffects: PropTypes.func.isRequired,
};

export default VulnerabilitiesTableRow;
