/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTable, NxTextLink, NxVulnerabilityIndicator } from '@sonatype/react-shared-components';
import { getSvUrl, getSvName } from '../utils/securityUtils';

import './SecurityIssuesSection.scss';

/**
 * Component for displaying security vulnerability information
 */
export default function SecurityIssuesSection({ matchState, identificationSource, securityVulnerabilities }) {
  let body;

  // if the component is unknown or claimed, show a message
  if (matchState === 'unknown') {
    body = <NxTable.Body emptyMessage="The component is unknown; security data is not available." />;
  } else if (identificationSource === 'Manual') {
    body = <NxTable.Body emptyMessage="The component is claimed; security data is not available." />;
  } else {
    body = (
      <NxTable.Body emptyMessage="None">
        {securityVulnerabilities.map((issue, index) => {
          const url = getSvUrl(issue);
          const name = getSvName(issue);

          return (
            <NxTable.Row key={index}>
              <NxTable.Cell>
                <NxVulnerabilityIndicator score={issue.severity} />
                <span>{issue.severity !== null ? issue.severity : 'Unscored'}</span>
              </NxTable.Cell>
              <NxTable.Cell className="iq-viewdetails-refid">
                {url ? (
                  <NxTextLink external href={url} target="_blank">
                    {name}
                  </NxTextLink>
                ) : (
                  name
                )}
              </NxTable.Cell>
              <NxTable.Cell>{issue.status}</NxTable.Cell>
              <NxTable.Cell>{issue.summary}</NxTable.Cell>
            </NxTable.Row>
          );
        })}
      </NxTable.Body>
    );
  }

  return (
    <NxTable caption="Security Issues">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>CVSS Score</NxTable.Cell>
          <NxTable.Cell>Problem Code</NxTable.Cell>
          <NxTable.Cell>Status</NxTable.Cell>
          <NxTable.Cell>Summary</NxTable.Cell>
        </NxTable.Row>
      </NxTable.Head>
      {body}
    </NxTable>
  );
}

SecurityIssuesSection.propTypes = {
  matchState: PropTypes.string,
  identificationSource: PropTypes.string,
  securityVulnerabilities: PropTypes.arrayOf(
    PropTypes.shape({
      severity: PropTypes.number,
      status: PropTypes.string,
      summary: PropTypes.string,
      reference: PropTypes.string,
      url: PropTypes.string,
    })
  ),
};
