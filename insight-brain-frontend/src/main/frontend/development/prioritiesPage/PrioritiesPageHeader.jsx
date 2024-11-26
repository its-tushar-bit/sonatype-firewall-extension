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
  NxH1,
  NxPageTitle,
  NxTooltip,
  NxTile,
  NxH2,
  NxSmallThreatCounter,
  NxStatefulDropdown,
} from '@sonatype/react-shared-components';
import { selectRouterCurrentParams, selectPrioritiesPageContainerName } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectApplicationReportMetaData,
  selectSelectedReport,
  selectWaivedViolationCountFromAggregatedComponentList,
  selectDependencyTreeIsAvailable,
  selectDependencyTreeUnavailableMessage,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { faCheckCircle, faExclamationCircle, faExternalLinkAlt } from '@fortawesome/pro-solid-svg-icons';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import moment from 'moment';
import { propOr } from 'ramda';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const stageMap = {
  build: 'Build',
  source: 'Source',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

const COPY_STATUS_TOOLTIP_TIMEOUT = 1500;

const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss');

export default function PrioritiesPageHeader() {
  const uiRouterState = useRouterState();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const selectedReport = useSelector(selectSelectedReport);
  const waivedViolationCount = useSelector(selectWaivedViolationCountFromAggregatedComponentList);
  const dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable);
  const dependencyTreeUnavailableMessage = useSelector(selectDependencyTreeUnavailableMessage);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const getHrefToDependencyTree = () => {
    if (prioritiesPageContainerName === 'prioritiesPageFromDashboard') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree';
    } else if (prioritiesPageContainerName === 'prioritiesPageFromReports') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromReports.dependencyTree';
    }
  };

  const getReportProp = (propName) => propOr(0, propName, selectedReport);
  const criticalViolationCount = getReportProp('criticalViolationCount');
  const severeViolationCount = getReportProp('severeViolationCount');
  const moderateViolationCount = getReportProp('moderateViolationCount');
  const nonLowViolationCount = getReportProp('nonLowViolationCount');
  const policyComponentCount = getReportProp('policyComponentCount');
  const totalArtifactCount = getReportProp('totalArtifactCount');
  const legacyPolicyViolationsCount =
    getReportProp('legacyViolationCount') || getReportProp('grandfatheredPolicyViolationCount');

  const pluralTermination = (components) => (components === 1 ? '' : 's');

  const metadata = useSelector(selectApplicationReportMetaData);

  const { scanTriggerType, forMonitoring, reevaluation, reportTime, commitHash, stageId, application } = metadata || {};

  const appName = application?.name;
  const triggerText = scanTriggerType
    ? `${scanTriggerType} ${forMonitoring ? '(Continuous Monitoring)' : reevaluation ? '(Re-evaluation)' : ''}`
    : null;
  const formattedDate = reportTime ? formatDate(reportTime) : null;
  const stageName = stageMap[stageId];

  const lifecycleReportHref = uiRouterState.href('applicationReport.policy', {
    publicId: publicAppId,
    scanId: scanId,
  });

  const dependencyTreeHref = uiRouterState.href(getHrefToDependencyTree(), {
    publicId: publicAppId,
    scanId,
  });

  return (
    <>
      <NxPageTitle>
        <NxH1>{appName} - Priorities</NxH1>

        <div className="nx-btn-bar">
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
      </NxPageTitle>
      <NxTile data-testid="iq-priorities-page-summary-section">
        <NxTile.Header>
          <NxTile.Headings>
            <div className="iq-priorities-page-header-title">
              <NxTile.HeaderTitle>
                <NxH2>
                  <span>
                    {nonLowViolationCount} Violation
                    {pluralTermination(nonLowViolationCount)}
                  </span>
                </NxH2>
              </NxTile.HeaderTitle>
              <NxSmallThreatCounter
                criticalCount={criticalViolationCount || null}
                severeCount={severeViolationCount || null}
                moderateCount={moderateViolationCount || null}
              />
              <ViolationTag type="legacy" count={legacyPolicyViolationsCount} />
              <ViolationTag type="waived" count={waivedViolationCount} />
            </div>

            <NxTile.HeaderSubtitle>
              Affecting {policyComponentCount} of {totalArtifactCount} identified component
              {pluralTermination(totalArtifactCount)}
            </NxTile.HeaderSubtitle>
          </NxTile.Headings>
        </NxTile.Header>
        <NxTile.Content>
          <div className="iq-priorities-page-desc-details">
            <TriggerText triggerText={triggerText} />
            <Timestamp formattedDate={formattedDate} />
            <Commit commitHash={commitHash} />
            <Stage stageName={stageName} />
          </div>
        </NxTile.Content>
      </NxTile>
    </>
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

function Timestamp({ formattedDate }) {
  return (
    <>
      {formattedDate && (
        <span>
          <span className="iq-priorities-page-desc-title">Timestamp: </span> {formattedDate}
        </span>
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

function ViolationTag({ type, count }) {
  const violationClass = `iq-priorities-page-header-tag ${type}`;
  return (
    <NxTooltip title={type === 'legacy' ? 'Legacy Violations' : 'Waived Violations'}>
      <div className={violationClass}>
        <span className="iq-priorities-page-header-tag-count">{count}</span>
        <span className="iq-priorities-page-header-tag-type">
          {type === 'legacy' && 'Legacy'}
          {type === 'waived' && 'Waived'}
        </span>
      </div>
    </NxTooltip>
  );
}
