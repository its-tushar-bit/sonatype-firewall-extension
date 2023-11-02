/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxFontAwesomeIcon,
  NxTable,
  NxTableRow,
  NxTableBody,
  NxTableHead,
  NxTableCell,
  NxThreatIndicator,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/free-solid-svg-icons';

import ComponentDisplay, { componentPropTypes } from '../../ComponentDisplay/ReactComponentDisplay';
import { getBaseUrl } from '../../util/urlUtil';
import { useRouterState } from '../../react/RouterStateContext';

function createRow(data, uiRouterState) {
  const { securityCode, cvssScore, key, policyThreatLevel } = data;
  const linkUrl = getBaseUrl(window.location.href) + '/ui/links/vln/' + encodeURIComponent(securityCode);

  return (
    <NxTableRow key={key}>
      <NxTableCell>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell>
        <NxTextLink
          className="iq-vulnerability-refid-link"
          href={uiRouterState.href('vulnerabilitySearchDetail', { id: securityCode })}
        >
          {securityCode}
        </NxTextLink>
        <NxTextLink className="iq-vulnerability-printable-link" href={linkUrl}>
          {linkUrl}
        </NxTextLink>
      </NxTableCell>
      <NxTableCell isNumeric>{cvssScore.toFixed(1)}</NxTableCell>
      <NxTableCell>
        <div className="iq-vulnerability-component-name">
          {data.waived && (
            <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
              <span>Waived</span>
              <NxFontAwesomeIcon icon={faCheck} />
            </span>
          )}
          {data.legacyViolation && (
            <span className="iq-text-indicator iq-text-indicator--legacy-violation iq-pull-right">
              <span>Legacy</span>
              <NxFontAwesomeIcon icon={faHistory} />
            </span>
          )}
          <ComponentDisplay component={data} truncate={true} />
        </div>
      </NxTableCell>
    </NxTableRow>
  );
}

export default function ApplicationReportVulnerabilitiesTable({ vulnerabilities }) {
  const uiRouterState = useRouterState();
  const rows = vulnerabilities.map((vuln) => createRow(vuln, uiRouterState));

  return (
    <div className="nx-tile-content nx-viewport-sized__container">
      <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
        <NxTable id="application-report-vulnerabilities-table">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell>Threat</NxTableCell>
              <NxTableCell>Security Issue</NxTableCell>
              <NxTableCell isNumeric>CVSS Score</NxTableCell>
              <NxTableCell>Component</NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody emptyMessage="This report contains no vulnerabilities.">{rows}</NxTableBody>
        </NxTable>
      </div>
    </div>
  );
}

export const vulnerabilitiesPropType = PropTypes.arrayOf(
  PropTypes.shape({
    ...componentPropTypes,
    securityCode: PropTypes.string.isRequired,
    cvssScore: PropTypes.number.isRequired,
  })
);

ApplicationReportVulnerabilitiesTable.propTypes = {
  vulnerabilities: vulnerabilitiesPropType.isRequired,
};
