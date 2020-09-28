/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import MaximizedContainer from '../../react/MaximizedContainer';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilters from './ReportFilters';
import ReportTitle from './ReportTitle';
import * as PropTypes from 'prop-types';

export default function ReportPage(props) {
  const {
    // actions
    setReportParameters,
    loadReport,
    reevaluateReport,
    // state
    publicId,
    scanId,
    unknownjs,
    embeddable,
    policyViolationId,
    metadata,
    loadError,
    selectedReport,
    stateGo
  } = props;

  useEffect(() => {
    if (publicId && scanId) {
      setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId);
      loadReport();
    }
  }, [publicId, scanId]);

  return (
    <MaximizedContainer id="app-react-report">
      <div className="nx-page">
        <div className="nx-page-content">
          <aside className="nx-page-sidebar" id="report-sidebar">
            <ReportFilters/>
          </aside>
          <div className="nx-page-main">
            <ReportTitle metadataDetails={metadata}
                         scanId={scanId}
                         publicId={publicId}
                         selectedReport={selectedReport}
                         reevaluateReport={reevaluateReport}
                         loadError={loadError}
                         stateGo={stateGo}
            />
            <div className="nx-tile">
              <ReportStatusBar/>
            </div>
            <div className="nx-tile iq-report-content">
              <ReportContent/>
            </div>
          </div>
        </div>
      </div>
    </MaximizedContainer>
  );
}

ReportPage.propTypes = {
  // actions
  setReportParameters: PropTypes.func.isRequired,
  loadReport: PropTypes.func.isRequired,
  reevaluateReport: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  // state
  publicId: PropTypes.string,
  scanId: PropTypes.string,
  unknownjs: PropTypes.bool,
  embeddable: PropTypes.bool,
  policyViolationId: PropTypes.string,
  metadata: PropTypes.shape({
    reportTitle: PropTypes.string.isRequired,
    reportTime: PropTypes.number.isRequired,
    commitHash: PropTypes.string,
    application: PropTypes.shape({
      name: PropTypes.string.isRequired
    })
  }),
  selectedReport: PropTypes.shape({
    reportVersion: PropTypes.number.isRequired
  }),
  loadError: PropTypes.object
};
