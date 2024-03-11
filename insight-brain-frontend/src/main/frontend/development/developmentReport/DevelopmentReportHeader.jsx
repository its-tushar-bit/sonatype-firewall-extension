/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import { NxCode, NxFontAwesomeIcon, NxH1, NxPageTitle, NxTooltip } from '@sonatype/react-shared-components';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { faCheckCircle, faExclamationCircle } from '@fortawesome/pro-solid-svg-icons';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import moment from 'moment';

const stageMap = {
  build: 'Build',
  source: 'Source',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

const COPY_STATUS_TOOLTIP_TIMEOUT = 1500;

const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss');

export default function DevelopmentReportHeader() {
  const metadataDetails = useSelector(selectApplicationReportMetaData);

  const { scanTriggerType, forMonitoring, reevaluation, reportTime, commitHash, stageId, application } =
    metadataDetails || {};

  const appName = application?.name;
  const triggerText = scanTriggerType
    ? `${scanTriggerType} ${forMonitoring ? '(Continuous Monitoring)' : reevaluation ? '(Re-evaluation)' : ''}`
    : null;
  const formattedDate = reportTime ? formatDate(reportTime) : null;
  const stageName = stageMap[stageId];

  return (
    <NxPageTitle.Headings>
      <NxH1>{appName} - Priorities</NxH1>
      <NxPageTitle.Description className="iq-development-report-desc">
        <TriggerText triggerText={triggerText} />
        <div className="iq-development-report-desc-details">
          <Timestamp formattedDate={formattedDate} />
          <Commit commitHash={commitHash} />
          <Stage stageName={stageName} />
        </div>
      </NxPageTitle.Description>
    </NxPageTitle.Headings>
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
        <NxFontAwesomeIcon className="iq-development-report-copy-success" icon={faCheckCircle} />
      </span>
    );
  } else {
    return (
      <span>
        Copy failed
        <NxFontAwesomeIcon className="iq-development-report-copy-fail" icon={faExclamationCircle} />
      </span>
    );
  }
}

function TriggerText({ triggerText }) {
  return (
    <>
      {triggerText && (
        <div>
          <strong>Triggered by </strong> {triggerText}
        </div>
      )}
    </>
  );
}

function Timestamp({ formattedDate }) {
  return (
    <>
      {formattedDate && (
        <span>
          <strong>On </strong> {formattedDate}
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
          <strong>Commit </strong>
          <NxCode className="iq-development-report-commit">{commitHash?.substring(0, 7)}</NxCode>
          <NxTooltip title={<TooltipTitle copySuccess={copySuccess} />}>
            <NxFontAwesomeIcon
              className="iq-development-report-copy-commit-btn"
              icon={faCopy}
              onClick={copyToClipboard}
            />
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
          <strong>Stage </strong> {stageName}
        </span>
      )}
    </>
  );
}
