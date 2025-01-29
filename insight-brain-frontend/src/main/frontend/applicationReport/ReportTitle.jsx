/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import classnames from 'classnames';
import moment from 'moment-timezone';
import { filter, join, pipe } from 'ramda';
import { faFilePdf, faFile, faFileCode } from '@fortawesome/pro-solid-svg-icons';
import {
  NxStatefulDropdown,
  NxDropdownDivider,
  NxTooltip,
  NxFontAwesomeIcon,
  NxTextLink,
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
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { getDownloadPdfUrl, getExportCycloneDxUrl, getExportSpdxUrl } from 'MainRoot/util/CLMLocation';
import ReevaluationModal from 'MainRoot/applicationReport/ReevaluationModal';

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

  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const pdfUrl = getDownloadPdfUrl(publicId, scanId);
  const sbomUrl = getExportCycloneDxUrl(metadataDetails.application.id, scanId);
  const spdxUrl = getExportSpdxUrl(metadataDetails.application.id, scanId);

  const prioritiesUrl = uiRouterState.href('prioritiesPageFromReports', { publicAppId: publicId, scanId });

  const rawDataUrl = uiRouterState.href('applicationReport.rawData', { publicId, scanId });

  const latestEvaluationsUrl = uiRouterState.href('applicationLatestEvaluations', {
    applicationPublicId: publicId,
    stageId: metadataDetails.stageId,
  });

  const vulnerabilitiesUrl = uiRouterState.href('applicationReport.vulnerabilities', { publicId, scanId });
  const vulnerabilitiesPageDisable = selectedReport && selectedReport.reportVersion < 5 ? true : false;
  const viewVulnerabilitiesLinkClasses = classnames('nx-dropdown-link', { disabled: vulnerabilitiesPageDisable });

  const vulnPageTooltip = vulnerabilitiesPageDisable
    ? 'Reevaluate the report in order to enable Vulnerabilities view'
    : '';

  return (
    <div className="nx-page-title">
      <div className="nx-btn-bar">
        <ReevaluationModal />

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
            <span>Export CycloneDX</span>
          </a>
          <a className="nx-dropdown-button" href={spdxUrl}>
            <NxFontAwesomeIcon icon={faFilePdf} />
            <span>Export SPDX</span>
          </a>
          {isDeveloperDashboardEnabled && (
            <NxTextLink
              className="nx-dropdown-button iq-developer-priorities-link-from-options-dropdown"
              external
              href={prioritiesUrl}
              data-analytics-id="iq-developer-priorities-link-from-options-dropdown"
            >
              <img src={faFilterList} className="iq-priorities-icon" />
              <span>Priorities</span>
            </NxTextLink>
          )}
          <NxDropdownDivider />
          <a className="nx-dropdown-link" href={latestEvaluationsUrl}>
            <NxFontAwesomeIcon icon={faFile} />
            <span>View Latest Evaluations</span>
          </a>
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
        </NxStatefulDropdown>
      </div>
      <h1 className="nx-h1">
        {metadataDetails.application.name} {metadataDetails.reportTitle}
      </h1>
      <div className="nx-page-title__description">{renderDescription(metadataDetails)}</div>
    </div>
  );
}
