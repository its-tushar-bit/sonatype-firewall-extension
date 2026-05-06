/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxLoadingSpinner,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faXmarkCircle } from '@fortawesome/pro-solid-svg-icons';
import {
  AUTOMATED_REMEDIATION_STATUS,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS,
} from 'MainRoot/constants/automatedRemediationStatus';

export const PR_STATUS_HIDDEN_REASONS = [
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.UNSUPPORTED_STAGE,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.UNSUPPORTED_DEPENDENCY_TYPE,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.UNSUPPORTED_FORMAT,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.REMEDIATION_EVENT_EXISTS,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.NO_REMEDIATION_VERSION_AVAILABLE,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.INSUFFICIENT_PERMISSIONS,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.NOT_SUPPORTED_FOR_MTIQ,
  MANUAL_PULL_REQUEST_NOT_POSSIBLE_REASONS.NON_DEFAULT_BRANCH,
];

// A reusable component that displays different presentations based on PR status ( see above status)
export default function PRStatus({
  automatedRemediationStatus,
  defaultPrLinkText = 'View PR',
  onCreatePR,
  defaultContent,
  onRetry,
}) {
  function getDisabledReason() {
    return automatedRemediationStatus.reason === 'SCM_NOT_CONFIGURED'
      ? 'Source Control is not configured'
      : 'Manual Pull Requests are disabled';
  }

  function getPRLinkText() {
    if (automatedRemediationStatus?.pullRequestId) {
      return `PR #${automatedRemediationStatus.pullRequestId}`;
    }
    return defaultPrLinkText;
  }

  switch (automatedRemediationStatus?.status) {
    case AUTOMATED_REMEDIATION_STATUS.MANUAL_PULL_REQUEST_POSSIBLE:
      return (
        <NxButton className="iq-pr-status__btn nx-btn--tertiary nx-btn--small" onClick={onCreatePR}>
          Create PR
        </NxButton>
      );
    case AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST_CREATION_PENDING:
      return <NxLoadingSpinner>Creating PR…</NxLoadingSpinner>;
    case AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST_CREATION_FAILED:
      return (
        <NxTooltip title={`Failure to create PR. ${automatedRemediationStatus.reason}`} placement="top-end">
          <button className="nx-text-link iq-pr-status__btn--failed" onClick={onRetry}>
            <NxFontAwesomeIcon className="iq-pr-status__failed-icon" icon={faXmarkCircle} />
            Retry
          </button>
        </NxTooltip>
      );
    case AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST:
      return (
        <NxTextLink href={automatedRemediationStatus.url} external className="iq-pr-status__view-pr-link">
          {getPRLinkText()}
        </NxTextLink>
      );
    case AUTOMATED_REMEDIATION_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE:
      if (!PR_STATUS_HIDDEN_REASONS.includes(automatedRemediationStatus.reason)) {
        return (
          <NxTooltip title={getDisabledReason()} placement="top-end">
            <NxButton className="iq-pr-status__btn nx-btn--small disabled">Create PR</NxButton>
          </NxTooltip>
        );
      }
    /* falls through */
    default:
      return defaultContent ?? null;
  }
}

PRStatus.propTypes = {
  automatedRemediationStatus: PropTypes.shape({
    status: PropTypes.oneOf(Object.values(AUTOMATED_REMEDIATION_STATUS)),
    reason: PropTypes.string,
    url: PropTypes.string,
    pullRequestId: PropTypes.number,
  }),
  defaultPrLinkText: PropTypes.string,
  onCreatePR: PropTypes.func.isRequired,
  defaultContent: PropTypes.node,
  onRetry: PropTypes.func.isRequired,
};
