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

import { selectApplicationReportMetaData, selectSelectedReport } from './applicationReportSelectors';
import { selectRouterCurrentParams, selectIsPrioritiesPageContainer } from 'MainRoot/reduxUiRouter/routerSelectors';
import { reevaluateReport as reevaluateR } from './applicationReportActions';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { getDownloadPdfUrl, getExportCycloneDxUrl, getExportSpdxUrl } from 'MainRoot/util/CLMLocation';

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
  const dispatch = useDispatch();
  const metadataDetails = useSelector(selectApplicationReportMetaData);
  const { publicId, scanId } = useSelector(selectRouterCurrentParams);
  const selectedReport = useSelector(selectSelectedReport);
  const uiRouterState = useRouterState();
  const reevaluateReport = (...args) => dispatch(reevaluateR(...args));
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);

  const pdfUrl = getDownloadPdfUrl(publicId, scanId);
  const sbomUrl = getExportCycloneDxUrl(metadataDetails.application.id, scanId);
  const spdxUrl = getExportSpdxUrl(metadataDetails.application.id, scanId);
  const rawDataUrl = uiRouterState.href(
    isPrioritiesPageContainer ? 'prioritiesPageContainer.rawData' : 'applicationReport.rawData',
    { publicId, scanId }
  );
  const legacyReportUrl = uiRouterState.href('report', { publicId, scanId });
  const vulnerabilitiesUrl = uiRouterState.href(
    isPrioritiesPageContainer ? 'prioritiesPageContainer.vulnerabilities' : 'applicationReport.vulnerabilities',
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
        <NxButton id="reevaluate-report-button" className="nx-btn--tertiary" onClick={reevaluateReport}>
          <NxFontAwesomeIcon icon={faSync} />
          <span>Re-Evaluate Report</span>
        </NxButton>
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
