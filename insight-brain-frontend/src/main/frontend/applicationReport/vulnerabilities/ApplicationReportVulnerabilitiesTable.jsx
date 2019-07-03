import React from 'react';
import * as PropTypes from 'prop-types';

import ComponentDisplay, { componentPropTypes } from '../../ComponentDisplay/ReactComponentDisplay';
import MaximizedContainer from '../../react/MaximizedContainer';
import ThreatIndication from '../../react/ThreatIndication';
import { getBaseUrl } from '../../util/urlUtil';

function createRow(data) {
  const { securityCode, cvssScore, key, policyThreatLevel } = data;
  const linkUrl = getBaseUrl(window.location.href) + '/ui/links/vln/' + encodeURIComponent(securityCode);

  return (
    <tr key={key} className="iq-table-row">
      <td className="iq-cell iq-cell--vulnerability-policy-threat-level">
        <ThreatIndication policyThreatLevel={policyThreatLevel} />
        <span className="iq-threat-number">{policyThreatLevel}</span>
      </td>
      <td className="iq-cell iq-cell--vulnerability-security-code">
        <a href={linkUrl}>{securityCode}</a>
        <a className="iq-vulnerability-printable-link" href={linkUrl}>{linkUrl}</a>
      </td>
      <td className="iq-cell iq-cell--vulnerability-cvss">{cvssScore.toFixed(1)}</td>
      <td className="iq-cell iq-cell--vulnerability-component-display">
        { data.waived &&
          <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
            Waived<i className="fa fa-check" />
          </span>
        }
        { data.grandfathered &&
          <span className="iq-text-indicator iq-text-indicator--grandfathered iq-pull-right">
            Grandfathered<i className="fa fa-history" />
          </span>
        }
        <ComponentDisplay component={data} truncate={true} />
      </td>
    </tr>
  );
}

export default function ApplicationReportVulnerabilitiesTable({ vulnerabilities }) {
  return (
    <MaximizedContainer className="nx-tile-content">
      <div className="iq-scrollable iq-scrollable--full-height">
        <table id="application-report-vulnerabilities-table" className="iq-table">
          <thead>
            <tr className="iq-table-row">
              <th className="iq-cell iq-cell--header iq-cell--vulnerability-policy-threat-level">Threat</th>
              <th className="iq-cell iq-cell--header iq-cell--vulnerability-security-code">Security Issue</th>
              <th className="iq-cell iq-cell--header iq-cell--vulnerability-cvss">CVSS Score</th>
              <th className="iq-cell iq-cell--header iq-cell--vulnerability-component-display">Component</th>
            </tr>
          </thead>
          <tbody>
            { vulnerabilities.map(createRow) }
          </tbody>
        </table>
      </div>
    </MaximizedContainer>
  );
}

export const vulnerabilitiesPropType = PropTypes.arrayOf(PropTypes.shape({
  ...componentPropTypes,
  securityCode: PropTypes.string.isRequired,
  cvssScore: PropTypes.number.isRequired
}));

ApplicationReportVulnerabilitiesTable.propTypes = {
  vulnerabilities: vulnerabilitiesPropType.isRequired
};
