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
import { faFilePdf, faFile, faFileCode, faFilterList } from '@fortawesome/pro-solid-svg-icons';
import {
  NxStatefulDropdown,
  NxDropdownDivider,
  NxTooltip,
  NxFontAwesomeIcon,
  NxTextLink,
} from '@sonatype/react-shared-components';
import {
  selectApplicationReportMetaData,
  selectSelectedReport,
  selectIsContainerImagesEvaluationEnabledAndProxyStage,
  selectIsHrcReport,
} from './applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getReportDisplayName } from './reportEntryUtils';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  getDownloadPdfUrl,
  getExportCycloneDxUrl,
  getExportSpdxUrl,
  getHrcDownloadPdfUrl,
  getHrcExportCycloneDxUrl,
  getHrcExportSpdxUrl,
} from 'MainRoot/util/CLMLocation';
import ReevaluationModal from 'MainRoot/applicationReport/ReevaluationModal';

const renderDescription = (metadataDetails) => {
  const {
    scanTriggerType,
    forMonitoring,
    reevaluation,
    reportTime,
    commitHash,
    containerScanningMode,
  } = metadataDetails;

  const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]ZZ');
  const description = [
    scanTriggerType && `Triggered by ${scanTriggerType}`,
    forMonitoring && '(Continuous Monitoring)',
    containerScanningMode && 'sonatype' === containerScanningMode && '(Sonatype Container)',
    !forMonitoring && reevaluation && '(Re-evaluation)',
    reportTime && `on ${formatDate(reportTime)}`,
    commitHash && `- Commit ${commitHash}`,
  ];

  return pipe(filter(Boolean), join(' '))(description);
};

export default function ReportTitle() {
  const metadataDetails = useSelector(selectApplicationReportMetaData);
  const routerParams = useSelector(selectRouterCurrentParams);
  const { publicId, scanId, componentDisplayName, hrcId: hrcIdFromParams } = routerParams;
  const selectedReport = useSelector(selectSelectedReport);
  const uiRouterState = useRouterState();

  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isFirewallForDocker = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);
  // selectOwnerType is URL-first (returns HOSTED_REPOSITORY_COMPONENT whenever params.hrcId
  // is present), so this single source is safe from the initial-mount race and there is no
  // need for a "|| !!hrcIdFromParams" fallback anymore.
  const isHrcReport = useSelector(selectIsHrcReport);

  // For HRC reports: prefer metadata.hrcId (from backend) with a fallback to the route param.
  // For application reports: use publicId from route params.
  const hrcId = metadataDetails?.hrcId || hrcIdFromParams;

  const titleName = getReportDisplayName(metadataDetails, { componentDisplayName, hrcId });

  // Use HRC-specific URLs for HRC reports, application URLs for application reports
  const pdfUrl = isHrcReport ? getHrcDownloadPdfUrl(hrcId, scanId) : getDownloadPdfUrl(publicId, scanId);
  const sbomUrl = isHrcReport
    ? getHrcExportCycloneDxUrl(hrcId, scanId)
    : getExportCycloneDxUrl(metadataDetails?.application?.id, scanId);
  const spdxUrl = isHrcReport
    ? getHrcExportSpdxUrl(hrcId, scanId)
    : getExportSpdxUrl(metadataDetails?.application?.id, scanId);

  const prioritiesUrl = uiRouterState.href('prioritiesPageFromReports', { publicAppId: publicId, scanId });

  // Thread componentDisplayName through the HRC-scoped links so the destination pages'
  // headers render the friendly component name (e.g. "org.apache.commons : commons-compress")
  // instead of falling back to the raw hrcId. metadata.application is null on HRC, so
  // getReportDisplayName on the destination reads componentDisplayName from the URL.
  const rawDataUrl = isHrcReport
    ? uiRouterState.href('hostedRepositoryComponentReport.rawData', { hrcId, scanId, componentDisplayName })
    : uiRouterState.href('applicationReport.rawData', { publicId, scanId });

  const latestEvaluationsUrl = isHrcReport
    ? uiRouterState.href('hostedRepositoryComponentLatestEvaluations', {
        hrcId,
        stageId: metadataDetails?.stageId,
        scanId,
        componentDisplayName,
      })
    : uiRouterState.href('applicationLatestEvaluations', {
        applicationPublicId: publicId,
        stageId: metadataDetails?.stageId,
      });

  const vulnerabilitiesUrl = isHrcReport
    ? uiRouterState.href('hostedRepositoryComponentReport.vulnerabilities', {
        hrcId,
        scanId,
        componentDisplayName,
      })
    : uiRouterState.href('applicationReport.vulnerabilities', {
        publicId,
        scanId,
      });
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
          {/* Priorities is deferred to CLM-44516 for HRC reports */}
          {isDeveloperDashboardEnabled && !isFirewallForDocker && !isHrcReport && (
            <NxTextLink
              className="nx-dropdown-button iq-developer-priorities-link-from-options-dropdown"
              external
              href={prioritiesUrl}
              data-analytics-id="iq-developer-priorities-link-from-options-dropdown"
            >
              <NxFontAwesomeIcon icon={faFilterList} />
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
        {titleName} {metadataDetails?.reportTitle}
      </h1>
      <div className="nx-page-title__description">{metadataDetails && renderDescription(metadataDetails)}</div>
    </div>
  );
}
