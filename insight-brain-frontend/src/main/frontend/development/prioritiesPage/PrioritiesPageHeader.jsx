/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import {
  NxCode,
  NxFontAwesomeIcon,
  NxTooltip,
  NxH2,
  NxStatefulDropdown,
  NxTextLink,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import {
  selectRouterCurrentParams,
  selectPrioritiesPageContainerName,
  selectCurrentRouteName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectApplicationReportMetaData,
  selectDependencyTreeIsAvailable,
  selectDependencyTreeUnavailableMessage,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { faCheckCircle, faCodeBranch, faExclamationCircle, faExternalLinkAlt } from '@fortawesome/pro-solid-svg-icons';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import moment from 'moment';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { isNil } from 'ramda';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';

const stageMap = {
  build: 'Build',
  source: 'Source',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

const COPY_STATUS_TOOLTIP_TIMEOUT = 1500;

const commonDefaultBranchNames = ['master', 'main', 'develop'];

const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]ZZ');

export default function PrioritiesPageHeader() {
  const uiRouterState = useRouterState();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable);
  const dependencyTreeUnavailableMessage = useSelector(selectDependencyTreeUnavailableMessage);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const getHrefToDependencyTree = () => {
    if (prioritiesPageContainerName === 'prioritiesPageFromDashboard') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree';
    } else if (prioritiesPageContainerName === 'prioritiesPageFromReports') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromReports.dependencyTree';
    } else if (prioritiesPageContainerName === 'prioritiesPageFromIntegrations') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations.dependencyTree';
    }
  };

  const metadata = useSelector(selectApplicationReportMetaData);

  const { scanTriggerType, forMonitoring, reevaluation, reportTime, commitHash, stageId, application, branchName } =
    metadata || {};

  const appName = application?.name;
  const triggerText = scanTriggerType
    ? `${scanTriggerType} ${forMonitoring ? '(Continuous Monitoring)' : reevaluation ? '(Re-evaluation)' : ''}`
    : null;
  const stageName = stageMap[stageId];

  const lifecycleReportHref = uiRouterState.href('applicationReport.policy', {
    publicId: publicAppId,
    scanId: scanId,
  });

  const dependencyTreeHref = uiRouterState.href(getHrefToDependencyTree(), {
    publicId: publicAppId,
    scanId,
  });

  const getDynamicHrefForBreadcrumbs = () => {
    if (currentRouteName === 'prioritiesPageFromReports' || currentRouteName === 'prioritiesPageFromIntegrations') {
      return {
        href: uiRouterState.href('developer.priorities'),
        text: 'Priorities',
      };
    }
    return {
      href: uiRouterState.href('developer.dashboard'),
      text: 'Developer Dashboard',
    };
  };

  const { href, text } = getDynamicHrefForBreadcrumbs();
  const getTitle = () => {
    if (isNil(branchName)) {
      return `${appName} - Priorities`;
    } else if (commonDefaultBranchNames.includes(branchName.toLowerCase())) {
      return `${capitalizeFirstLetter(branchName)} Branch Priorities`;
    } else {
      return 'Feature Branch Priorities';
    }
  };

  return (
    <div className="iq-priorities-page-header" data-testid="iq-priorities-page-summary-section">
      <div className="iq-priorities-page-header-actions">
        <div className="iq-priorities-page-breadcrumbs">
          <span className="iq-priorities-page-breadcrumbs-crumb">
            <NxTextLink className="iq-priorities-page-breadcrumbs-crumb" href={href}>
              {text}
            </NxTextLink>
          </span>
          <span className="iq-priorities-page-breadcrumbs-crumb">{appName}</span>
          {branchName && (
            <>
              <NxOverflowTooltip>
                <span className="nx-truncate-ellipsis iq-priorities-page-branch-name iq-priorities-page-breadcrumbs-crumb">
                  <NxFontAwesomeIcon icon={faCodeBranch} />
                  <NxCode>{branchName}</NxCode>
                </span>
              </NxOverflowTooltip>
            </>
          )}
        </div>
        <div>
          <NxStatefulDropdown label="View" className="iq-priorities-page-view-dropdown">
            <a className="nx-dropdown-link" href={lifecycleReportHref} target="_blank" rel="noreferrer">
              <span>Lifecycle Report</span>
              <NxFontAwesomeIcon icon={faExternalLinkAlt} />
            </a>
            <NxTooltip title={dependencyTreeUnavailableMessage}>
              <a
                className={`nx-dropdown-link ${dependencyTreeIsAvailable ? '' : 'disabled'}`}
                aria-disabled={!dependencyTreeIsAvailable}
                href={dependencyTreeHref}
              >
                <span>Dependencies</span>
              </a>
            </NxTooltip>
          </NxStatefulDropdown>
        </div>
      </div>
      <div className="iq-priorities-page-header-title">
        <NxH2>{getTitle()}</NxH2>
        <div className="iq-priorities-page-header-title-metadata">
          <TriggerText triggerText={triggerText} />
          <Timestamp reportTime={reportTime} />
          <Commit commitHash={commitHash} />
          <Stage stageName={stageName} />
        </div>
      </div>
    </div>
  );
}

function TooltipTitle({ copySuccess }) {
  if (copySuccess === null) {
    return <span>Copy</span>;
  }

  if (copySuccess) {
    return (
      <span>
        Copied
        <NxFontAwesomeIcon className="iq-priorities-page-copy-success" icon={faCheckCircle} />
      </span>
    );
  } else {
    return (
      <span>
        Copy failed
        <NxFontAwesomeIcon className="iq-priorities-page-copy-fail" icon={faExclamationCircle} />
      </span>
    );
  }
}

function TriggerText({ triggerText }) {
  return (
    <>
      {triggerText && (
        <span>
          <span className="iq-priorities-page-desc-title">Triggered by: </span> {triggerText}
        </span>
      )}
    </>
  );
}

function Timestamp({ reportTime }) {
  const formattedDate = reportTime ? formatDate(reportTime) : null;
  return (
    <>
      {formattedDate && (
        <NxTooltip title={formattedDate}>
          <span>
            <span className="iq-priorities-page-desc-title">Timestamp: </span> {moment(reportTime).fromNow()}
          </span>
        </NxTooltip>
      )}
    </>
  );
}

function Commit({ commitHash }) {
  const [copySuccess, setCopySuccess] = useState(null);
  const copyToClipboard = () => {
    try {
      navigator.clipboard.writeText(commitHash);
      setCopySuccess(true);
    } catch (error) {
      setCopySuccess(false);
    }

    setTimeout(() => {
      setCopySuccess(null);
    }, COPY_STATUS_TOOLTIP_TIMEOUT);
  };

  return (
    <>
      {commitHash && (
        <span>
          <span className="iq-priorities-page-desc-title">Commit: </span>
          <NxCode className="iq-priorities-page-commit">{commitHash?.substring(0, 7)}</NxCode>
          <NxTooltip title={<TooltipTitle copySuccess={copySuccess} />}>
            <NxFontAwesomeIcon className="iq-priorities-page-copy-commit-btn" icon={faCopy} onClick={copyToClipboard} />
          </NxTooltip>
        </span>
      )}
    </>
  );
}

function Stage({ stageName }) {
  return (
    <>
      {stageName && (
        <span>
          <span className="iq-priorities-page-desc-title">Stage: </span> {stageName}
        </span>
      )}
    </>
  );
}
