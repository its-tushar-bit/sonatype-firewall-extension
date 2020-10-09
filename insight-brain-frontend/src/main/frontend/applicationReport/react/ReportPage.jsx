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
    setSorting,
    setSortingParameters,
    // state
    publicId,
    scanId,
    unknownjs,
    embeddable,
    policyViolationId,
    metadata,
    loadError,
    selectedReport,
    stateGo,
    sortConfiguration
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
              <ReportStatusBar selectedReport={selectedReport}
                               loadError={loadError}
              />
            </div>
            <div className="nx-tile">
              <ReportContent selectedReport={selectedReport}
                             setSorting={setSorting}
                             sortConfiguration={sortConfiguration}
                             setSortingParameters={setSortingParameters}/>
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
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
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
    reportVersion: PropTypes.number.isRequired,
    knownArtifactCount: PropTypes.number.isRequired,
    totalArtifactCount: PropTypes.number.isRequired,
    policyComponentCount: PropTypes.number.isRequired,
    grandfatheredPolicyViolationCount: PropTypes.number.isRequired,
    criticalViolationCount: PropTypes.number.isRequired,
    severeViolationCount: PropTypes.number.isRequired,
    moderateViolationCount: PropTypes.number.isRequired,
    nonLowViolationCount: PropTypes.number.isRequired,
    displayedEntries: PropTypes.arrayOf(PropTypes.shape({
      derivedComponentName: PropTypes.string,
      policyName: PropTypes.string,
      hash: PropTypes.string,
      derivedDependencyType: PropTypes.string,
      filenames: PropTypes.array,
      displayName: PropTypes.shape({
        name: PropTypes.string,
        parts: PropTypes.array
      }),
      waived: PropTypes.bool,
      grandfathered: PropTypes.bool,
      policyThreatLevel: PropTypes.number
    }))
  }),
  loadError: PropTypes.object,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string
  })
};
