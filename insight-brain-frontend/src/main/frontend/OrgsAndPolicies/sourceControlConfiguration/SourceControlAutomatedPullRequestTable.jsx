/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { isEmpty } from 'ramda';
import { NxTile, NxH2, NxTable, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faCheckCircle, faTimesCircle } from '@fortawesome/pro-solid-svg-icons';
import { formatDate, STANDARD_DATE_TIME_FORMAT } from 'MainRoot/util/dateUtils';
import { faExclamationTriangle, faInfoCircle } from '@fortawesome/free-solid-svg-icons';

function SourceControlAutomatedPullRequestTable({ automatedPullRequests }) {
  const renderTableContent = (pullRequests) => {
    if (isEmpty(pullRequests)) return null;
    return pullRequests.map(renderPullRequestRows);
  };

  const renderPullRequestRows = (pullRequest) => {
    return (
      <NxTable.Row key={pullRequest.title}>
        <NxTable.Cell className="iq-automated-pr-table_title-column">{pullRequest.title}</NxTable.Cell>
        <NxTable.Cell>
          <NxTooltip title={pullRequest.reasoning}>{getStatusIcon(pullRequest)}</NxTooltip>
        </NxTable.Cell>

        <NxTable.Cell>{pullRequest.totalTime}</NxTable.Cell>

        <NxTable.Cell>{formatDate(pullRequest.startTime, STANDARD_DATE_TIME_FORMAT).toUpperCase()}</NxTable.Cell>
      </NxTable.Row>
    );
  };

  return (
    <>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Daily Automated Pull Requests</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTable className="iq-automated-pr-table">
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell className="iq-automated-pr-table_title-column">Title</NxTable.Cell>
            <NxTable.Cell>
              <span>Status</span>
              <NxTooltip title="Indicates if PR creation succeeded or encountered errors/exceptions.">
                <NxFontAwesomeIcon icon={faInfoCircle} className="iq-developer-dashboard-info-tooltip" />
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell>
              <span>Time Spent (MS)</span>
              <NxTooltip title="Indicates the total time taken (in milliseconds) to checkout, remediate, push, and create pull request.">
                <NxFontAwesomeIcon icon={faInfoCircle} className="iq-developer-dashboard-info-tooltip" />
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell>Start Time</NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body emptyMessage={'No results available'}>{renderTableContent(automatedPullRequests)}</NxTable.Body>
      </NxTable>
    </>
  );
}

function getStatusIcon(pullRequest) {
  if (pullRequest.exceptionThrown) {
    return <NxFontAwesomeIcon icon={faTimesCircle} className="pr-created-error-icon" />;
  }

  if (pullRequest.successful) {
    return <NxFontAwesomeIcon icon={faCheckCircle} className="pr-created-success-icon" />;
  }

  return <NxFontAwesomeIcon icon={faExclamationTriangle} className="pr-created-warning-icon" />;
}

SourceControlAutomatedPullRequestTable.propTypes = {
  automatedPullRequests: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default React.memo(SourceControlAutomatedPullRequestTable);
