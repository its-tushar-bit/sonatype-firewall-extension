/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import classnames from 'classnames';
import moment from 'moment-timezone';
import { filter, join, pipe } from 'ramda';
import { faFilePdf, faSync, faFile, faFileCode } from '@fortawesome/pro-solid-svg-icons';
import {
  NxStatefulDropdown,
  NxDropdownDivider,
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import faFilterList from '../../frontend/img/icon-filter-list.svg';

import { selectApplicationReportMetaData, selectSelectedReport } from './applicationReportSelectors';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageContainerName,
  selectPrioritiesPageName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { reevaluateReport as reevaluateR } from './applicationReportActions';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { getDownloadPdfUrl, getExportCycloneDxUrl, getExportSpdxUrl } from 'MainRoot/util/CLMLocation';
import {
  selectIsLatestReportForStageRequestPending,
  selectLatestReportForStageId,
} from 'MainRoot/applicationReport/latestReportForStageSelectors';

const renderDescription = (metadataDetails) => {
  const { scanTriggerType, forMonitoring, reevaluation, reportTime, commitHash } = metadataDetails;

  const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]ZZ');
  const description = [
    scanTriggerType && `Triggered by ${scanTriggerType}`,
    forMonitoring && '(Continuous Monitoring)',
    !forMonitoring && reevaluation && '(Re-evaluation)',
    reportTime && `on ${formatDate(reportTime)}`,
    commitHash && `- Commit ${commitHash}`,
  ];

  return pipe(filter(Boolean), join(' '))(description);
};

export default function ReportTitle() {
  const metadataDetails = useSelector(selectApplicationReportMetaData);
  const { publicId, scanId } = useSelector(selectRouterCurrentParams);
  const selectedReport = useSelector(selectSelectedReport);
  const uiRouterState = useRouterState();

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const pdfUrl = getDownloadPdfUrl(publicId, scanId);
  const sbomUrl = getExportCycloneDxUrl(metadataDetails.application.id, scanId);
  const spdxUrl = getExportSpdxUrl(metadataDetails.application.id, scanId);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);

  const prioritiesUrl = uiRouterState.href(
    isPrioritiesPageContainer ? prioritiesPageName : 'prioritiesPageFromAppReport',
    { publicAppId: publicId, scanId }
  );

  const rawDataUrl = uiRouterState.href(
    isPrioritiesPageContainer ? `${prioritiesPageContainerName}.rawData` : 'applicationReport.rawData',
    { publicId, scanId }
  );
  const legacyReportUrl = uiRouterState.href('report', { publicId, scanId });
  const vulnerabilitiesUrl = uiRouterState.href(
    isPrioritiesPageContainer ? `${prioritiesPageContainerName}.vulnerabilities` : 'applicationReport.vulnerabilities',
    { publicId, scanId }
  );
  const vulnerabilitiesPageDisable = selectedReport && selectedReport.reportVersion < 5 ? true : false;
  const viewVulnerabilitiesLinkClasses = classnames('nx-dropdown-link', { disabled: vulnerabilitiesPageDisable });

  const vulnPageTooltip = vulnerabilitiesPageDisable
    ? 'Reevaluate the report in order to enable Vulnerabilities view'
    : '';

  return (
    <div className="nx-page-title">
      <div className="nx-btn-bar">
        <ReEvaluationButton />

        <NxStatefulDropdown
          id="iq-report-options-dropdown"
          label="Options"
          className="nx-dropdown--navigation iq-report-actions"
        >
          <a className="nx-dropdown-button" href={pdfUrl}>
            <NxFontAwesomeIcon icon={faFilePdf} />
            <span>Export PDF</span>
          </a>
          <a className="nx-dropdown-button" href={sbomUrl} target="_blank" rel="noreferrer">
            <NxFontAwesomeIcon icon={faFileCode} />
            <span>Export CycloneDx</span>
          </a>
          <a className="nx-dropdown-button" href={spdxUrl}>
            <NxFontAwesomeIcon icon={faFilePdf} />
            <span>Export SPDX</span>
          </a>
          {isDeveloperDashboardEnabled && (
            <a
              className="nx-dropdown-button iq-developer-priorities-link-from-options-dropdown"
              href={prioritiesUrl}
              data-analytics-id="iq-developer-priorities-link-from-options-dropdown"
            >
              <img src={faFilterList} className="iq-priorities-icon" />
              <span>Priorities</span>
            </a>
          )}
          <NxDropdownDivider />
          <a className="nx-dropdown-link" href={rawDataUrl}>
            <NxFontAwesomeIcon icon={faFile} />
            <span>View raw data</span>
          </a>
          <NxTooltip title={vulnPageTooltip} placement="top">
            <a
              id="viewVulnBtn"
              className={viewVulnerabilitiesLinkClasses}
              href={vulnerabilitiesUrl}
              onClick={(evt) => {
                if (vulnerabilitiesPageDisable) {
                  evt.preventDefault();
                }
              }}
              aria-disabled={vulnerabilitiesPageDisable}
            >
              <NxFontAwesomeIcon icon={faFile} />
              <span>View vulnerabilities</span>
            </a>
          </NxTooltip>
          <a className="nx-dropdown-link" href={legacyReportUrl}>
            <NxFontAwesomeIcon icon={faFile} />
            <span>View legacy report</span>
          </a>
        </NxStatefulDropdown>
      </div>
      <h1 className="nx-h1">
        {metadataDetails.application.name} {metadataDetails.reportTitle}
      </h1>
      <div className="nx-page-title__description">{renderDescription(metadataDetails)}</div>
    </div>
  );
}

function ReEvaluationButton() {
  const { scanId } = useSelector(selectRouterCurrentParams);
  const isLatestReportForStageRequestPending = useSelector(selectIsLatestReportForStageRequestPending);
  const latestReportId = useSelector(selectLatestReportForStageId);

  const dispatch = useDispatch();

  const reevaluateReport = (...args) => dispatch(reevaluateR(...args));

  return (
    <NxTooltip title={getTooltipMessage()}>
      <span>
        <NxButton
          id="reevaluate-report-button"
          className="nx-btn--tertiary"
          onClick={reevaluateReport}
          disabled={shouldDisableReevaluation()}
        >
          <NxFontAwesomeIcon icon={faSync} />
          <span>Re-Evaluate Report</span>
        </NxButton>
      </span>
    </NxTooltip>
  );

  function isSameAsCurrentScan() {
    return latestReportId === scanId;
  }

  function shouldDisableReevaluation() {
    return isLatestReportForStageRequestPending || !isSameAsCurrentScan();
  }

  function getTooltipMessage() {
    if (shouldDisableReevaluation()) {
      return 'Re-Evaluation is only allowed on the latest scan of a given stage.';
    } else {
      return null;
    }
  }
}
