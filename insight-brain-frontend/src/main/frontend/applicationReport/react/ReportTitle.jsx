/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import moment from 'moment';
import classnames from 'classnames';
import {
  NxStatefulDropdown,
  NxDropdownDivider,
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';

import { faFilePdf, faSync, faFile, faFileCode } from '@fortawesome/pro-solid-svg-icons';

import { selectApplicationReportMetaData, selectSelectedReport } from '../applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { reevaluateReport as reevaluateR } from '../applicationReportActions';
import { stateGo as stateG } from 'MainRoot/reduxUiRouter/routerActions';

import { getDownloadPdfUrl, getViewSbomUrl } from 'MainRoot/util/CLMLocation';

import { compose, filter, join } from 'ramda';

const renderDescription = (metadataDetails) => {
  const { scanTriggerType, forMonitoring, reevaluation, reportTime, commitHash } = metadataDetails;

  const formatDate = (date) => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]Z');
  const description = [
    scanTriggerType && `Triggered by ${scanTriggerType}`,
    forMonitoring && '(Continuous Monitoring)',
    !forMonitoring && reevaluation && '(Re-evaluation)',
    reportTime && `on ${formatDate(reportTime)}`,
    commitHash && `- Commit ${commitHash}`,
  ];

  return compose(join(' '), filter(Boolean))(description);
};
export default function ReportTitle() {
  const dispatch = useDispatch();
  const metadataDetails = useSelector(selectApplicationReportMetaData);
  const { publicId, scanId } = useSelector(selectRouterCurrentParams);
  const selectedReport = useSelector(selectSelectedReport);
  const stateGo = (...args) => dispatch(stateG(...args));
  const reevaluateReport = (...args) => dispatch(reevaluateR(...args));

  const pdfUrl = getDownloadPdfUrl(publicId, scanId);
  const sbomUrl = getViewSbomUrl(metadataDetails.application.id, scanId);
  const vulnerabilitiesPageDisable = selectedReport && selectedReport.reportVersion < 5 ? true : false;
  const applyBtnClasses = classnames('nx-dropdown-link', {
    disabled: vulnerabilitiesPageDisable,
  });

  const onRawDataClick = () => {
    stateGo('applicationReport.rawData', {
      publicId: publicId,
      scanId: scanId,
    });
  };

  const onVulnerabilitiesDetailsClick = () => {
    stateGo('applicationReport.vulnerabilities', {
      publicId: publicId,
      scanId: scanId,
    });
  };

  const onLegacyReportClick = () => {
    stateGo('report', {
      publicId: publicId,
      scanId: scanId,
    });
  };

  const vulnPageTooltip = vulnerabilitiesPageDisable
    ? 'Reevaluate the report in order to enable Vulnerabilities view'
    : '';

  return (
    <div className="nx-page-title">
      <div className="nx-btn-bar">
        <NxButton className="nx-btn--tertiary" onClick={reevaluateReport}>
          <NxFontAwesomeIcon icon={faSync} />
          <span>Re-Evaluate Report</span>
        </NxButton>
        <NxStatefulDropdown label="Options" className="nx-dropdown--navigation iq-report-actions">
          <a className="nx-dropdown-button" href={pdfUrl}>
            <NxFontAwesomeIcon icon={faFilePdf} />
            <span>Generate PDF</span>
          </a>
          <a className="nx-dropdown-button" href={sbomUrl} target="_blank" rel="noreferrer">
            <NxFontAwesomeIcon icon={faFileCode} />
            <span>View SBOM</span>
          </a>
          <NxDropdownDivider />
          <a className="nx-dropdown-link" onClick={onRawDataClick} role="link">
            <NxFontAwesomeIcon icon={faFile} />
            <span>View raw data</span>
          </a>
          <NxTooltip title={vulnPageTooltip} placement="top">
            <a className={applyBtnClasses} onClick={onVulnerabilitiesDetailsClick} id="viewVulnBtn" role="link">
              <NxFontAwesomeIcon icon={faFile} />
              <span>View vulnerabilities</span>
            </a>
          </NxTooltip>
          <a className="nx-dropdown-link" onClick={onLegacyReportClick} role="link">
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
