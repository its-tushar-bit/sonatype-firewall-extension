import React from 'react';
import * as PropTypes from 'prop-types';
import { NxThreatBar } from '@sonatype/react-shared-components';

import ComponentDisplay, { componentPropTypes } from '../../ComponentDisplay/ReactComponentDisplay';
import MaximizedContainer from '../../react/MaximizedContainer';
import { getBaseUrl } from '../../util/urlUtil';

function createRow(data) {
  const { securityCode, cvssScore, key, policyThreatLevel } = data;
  const linkUrl = getBaseUrl(window.location.href) + '/ui/links/vln/' + encodeURIComponent(securityCode);

  return (
    <tr key={key} className="nx-table-row">
      <td className="nx-cell iq-cell--vulnerability-policy-threat-level">
        <NxThreatBar policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </td>
      <td className="nx-cell iq-cell--vulnerability-security-code">
        <a href={linkUrl}>{securityCode}</a>
        <a className="iq-vulnerability-printable-link" href={linkUrl}>{linkUrl}</a>
      </td>
      <td className="nx-cell iq-cell--vulnerability-cvss">{cvssScore.toFixed(1)}</td>
      <td className="nx-cell iq-cell--vulnerability-component-display">
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

const emptyRow = (
  <tr className="nx-table-row">
    <td colSpan="4" className="nx-cell nx-cell--empty">
      This report contains no vulnerabilities.
    </td>
  </tr>
);

export default function ApplicationReportVulnerabilitiesTable({ vulnerabilities }) {
  const rows = vulnerabilities.length ? vulnerabilities.map(createRow) : emptyRow;

  return (
    <MaximizedContainer className="nx-tile-content">
      <div className="iq-scrollable iq-scrollable--full-height">
        <table id="application-report-vulnerabilities-table" className="nx-table">
          <thead>
            <tr className="nx-table-row nx-table-row--header">
              <th className="nx-cell nx-cell--header iq-cell--vulnerability-policy-threat-level">Threat</th>
              <th className="nx-cell nx-cell--header iq-cell--vulnerability-security-code">Security Issue</th>
              <th className="nx-cell nx-cell--header iq-cell--vulnerability-cvss">CVSS Score</th>
              <th className="nx-cell nx-cell--header iq-cell--vulnerability-component-display">Component</th>
            </tr>
          </thead>
          <tbody>{ rows }</tbody>
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
