/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { isEmpty } from 'ramda';
import { NxTile, NxH2, NxTable, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { formatDate, SIMPLE_TIME_FORMAT } from 'MainRoot/util/dateUtils';

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
          {pullRequest.successful ? (
            <NxFontAwesomeIcon icon={faCheckCircle} className="pr-created-success-icon" />
          ) : (
            <NxFontAwesomeIcon icon={faExclamationTriangle} className="pr-created-warning-icon" />
          )}
        </NxTable.Cell>
        <NxTable.Cell>{pullRequest.totalTime}</NxTable.Cell>
        <NxTable.Cell>{pullRequest.exceptionThrown.toString()}</NxTable.Cell>
        <NxTable.Cell>{formatDate(pullRequest.startTime, SIMPLE_TIME_FORMAT).toUpperCase()}</NxTable.Cell>
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
            <NxTable.Cell>PR Created?</NxTable.Cell>
            <NxTable.Cell>Time Spent (MS)</NxTable.Cell>
            <NxTable.Cell>Errors</NxTable.Cell>
            <NxTable.Cell>Started</NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body emptyMessage={'No results available'}>{renderTableContent(automatedPullRequests)}</NxTable.Body>
      </NxTable>
    </>
  );
}

SourceControlAutomatedPullRequestTable.propTypes = {
  automatedPullRequests: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default React.memo(SourceControlAutomatedPullRequestTable);
