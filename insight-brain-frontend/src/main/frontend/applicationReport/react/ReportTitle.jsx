/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxStatefulDropdown, NxDropdownDivider, NxButton } from '@sonatype/react-shared-components';
import NxFontAwesomeIcon from '@sonatype/react-shared-components/components/NxFontAwesomeIcon/NxFontAwesomeIcon';
import { faFilePdf, faSync, faFile } from '@fortawesome/pro-solid-svg-icons';
import { getDownloadPdfUrl } from '../../util/CLMLocation';
import moment from 'moment';
import NxTooltip from '@sonatype/react-shared-components/components/NxTooltip/NxTooltip';

export default function ReportTitle(props) {
  const {
    stateGo,
    metadataDetails,
    publicId,
    scanId,
    selectedReport,
    reevaluateReport
  } = props;

  const pdfUrl = getDownloadPdfUrl(publicId, scanId);
  const vulnerabilitiesPageDisable = selectedReport && selectedReport.reportVersion < 5 ? true : false;
  const applyBtnClasses = classnames('nx-dropdown-link', {'disabled': vulnerabilitiesPageDisable});

  const formatDate = date => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]Z');

  const onRawDataClick = () => {
    stateGo('applicationReport.rawData', {
      publicId: publicId,
      scanId: scanId
    });
  };

  const onVulnerabilitiesDetailsClick = () => {
    stateGo('applicationReport.vulnerabilities', {
      publicId: publicId,
      scanId: scanId
    });
  };

  const onLegacyReportClick = () => {
    stateGo('report', {
      publicId: publicId,
      scanId: scanId
    });
  };

  const vulnPageTooltip =
      vulnerabilitiesPageDisable ? 'Reevaluate the report in order to enable Vulnerabilities view' : '';

  return (
    <div className="nx-page-title">
      <div className="nx-btn-bar">
        <NxButton className="nx-btn--tertiary" onClick={reevaluateReport}>
          <NxFontAwesomeIcon icon={faSync} />
          <span>Re-Evaluate Report</span>
        </NxButton>
        <NxStatefulDropdown label="Options" className="nx-dropdown--navigation iq-report-actions">
          <a className="nx-dropdown-button" href={pdfUrl}>
            <NxFontAwesomeIcon icon={faFilePdf}/>
            <span>Generate PDF</span>
          </a>
          <NxDropdownDivider/>
          <a className="nx-dropdown-link" onClick={onRawDataClick}>
            <NxFontAwesomeIcon icon={faFile}/>
            <span>View raw data</span>
          </a>
          <NxTooltip title={vulnPageTooltip} placement="top">
            <a className={applyBtnClasses} onClick={onVulnerabilitiesDetailsClick} id="viewVulnBtn">
              <NxFontAwesomeIcon icon={faFile}/>
              <span>View vulnerabilities</span>
            </a>
          </NxTooltip>
          <a className="nx-dropdown-link" onClick={onLegacyReportClick}>
            <NxFontAwesomeIcon icon={faFile}/>
            <span>View legacy report</span>
          </a>
        </NxStatefulDropdown>
      </div>
      <h1 className="nx-h1">{metadataDetails.application.name} {metadataDetails.reportTitle}</h1>
      <div className="nx-page-title__description">
        { metadataDetails.reportTime && <span>{formatDate(metadataDetails.reportTime)}</span> }
        { metadataDetails.commitHash && <span> — Commit {metadataDetails.commitHash}</span> }
      </div>
    </div>
  );
}

ReportTitle.propTypes = {
  // state
  metadataDetails: PropTypes.shape({
    reportTitle: PropTypes.string.isRequired,
    reportTime: PropTypes.number.isRequired,
    scanTriggerType: PropTypes.string.isRequired,
    reevaluation: PropTypes.bool.isRequired,
    forMonitoring: PropTypes.bool.isRequired,
    commitHash: PropTypes.string,
    application: PropTypes.shape({
      name: PropTypes.string.isRequired
    })
  }),
  publicId: PropTypes.string,
  scanId: PropTypes.string,
  selectedReport: PropTypes.shape({
    reportVersion: PropTypes.number.isRequired
  }),
  stateGo: PropTypes.func.isRequired,
  // actions
  reevaluateReport: PropTypes.func.isRequired
};
