/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxSmallThreatCounter, NxTable, NxTextLink, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faHourglassHalf } from '@fortawesome/pro-regular-svg-icons';
import { formatTimeAgo } from 'MainRoot/util/dateUtils';

const ReportsPageViolationCell = ({ stage, app, hrefUiRouterState, isDeveloperDashboardEnabled }) => {
  const results = app.policyEvaluationsResults[stage],
    evaluation = app.policyEvaluations[stage],
    publicAppId = app.publicId,
    hasPendingSourceControlPolicyEvaluation = app.hasPendingSourceControlPolicyEvaluation;

  // if results are pending, show hourglass
  if (stage === 'source' && hasPendingSourceControlPolicyEvaluation && !results) {
    return (
      <NxTable.Cell key={`${stage}${app.publicId}`}>
        <NxFontAwesomeIcon icon={faHourglassHalf} />
        <span>pending</span>
      </NxTable.Cell>
    );
  }

  // if no evaluations, show an empty cell
  if (!results) {
    return <NxTable.Cell key={`${stage}${app.publicId}`} />;
  }

  const hasViolations = !!(
    results.criticalComponentCount +
    results.severeComponentCount +
    results.moderateComponentCount
  );

  return (
    <NxTable.Cell key={`${stage}${app.publicId}`}>
      {!hasViolations && <div>No violations</div>}
      {hasViolations && (
        <NxSmallThreatCounter
          criticalCount={results.criticalPolicyViolationCount}
          severeCount={results.severePolicyViolationCount}
          moderateCount={results.moderatePolicyViolationCount}
        />
      )}
      <div className="iq-report-age">{formatTimeAgo(evaluation.time)}</div>
      <div className="iq-report-links-container">
        <NxTextLink
          id="iq-report-link"
          href={hrefUiRouterState('applicationReport.policy', {
            publicId: publicAppId,
            scanId: evaluation.scanId,
          })}
        >
          {isDeveloperDashboardEnabled ? 'Report' : 'View Report'}
        </NxTextLink>
        {isDeveloperDashboardEnabled && (
          <>
            <span>|</span>
            <NxTextLink
              id="iq-developer-priorities-link-from-reports-pag"
              data-analytics-id="iq-developer-priorities-link-from-reports-page"
              href={hrefUiRouterState('prioritiesPageFromReports', {
                publicAppId,
                scanId: evaluation.scanId,
              })}
            >
              Priorities
            </NxTextLink>
          </>
        )}
      </div>
    </NxTable.Cell>
  );
};

ReportsPageViolationCell.propTypes = {
  stage: PropTypes.arrayOf(PropTypes.string).isRequired,
  app: PropTypes.arrayOf(
    PropTypes.shape({
      policyEvaluationsResults: PropTypes.object,
      policyEvaluations: PropTypes.object,
      publicId: PropTypes.string,
      hasPendingSourceControlPolicyEvaluation: PropTypes.bool,
    })
  ).isRequired,
  hrefUiRouterState: PropTypes.func.isRequired,
  isDeveloperDashboardEnabled: PropTypes.bool.isRequired,
};

export default ReportsPageViolationCell;
