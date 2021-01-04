/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilters from './ReportFilters';
import ReportTitle from './ReportTitle';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';

export default function ReportPage(props) {
  const {
    // actions
    setReportParameters,
    loadReport,
    reevaluateReport,
    setSorting,
    setSortingParameters,
    setExactValueFilter,
    setAggregateReportEntries,
    setStringFieldFilter,
    // state
    publicId,
    scanId,
    unknownjs,
    embeddable,
    policyViolationId,
    metadata,
    loading,
    loadError,
    selectedReport,
    stateGo,
    sortConfiguration,
    aggregate,
    exactValueFilters,
    substringFilters,
    $state
  } = props;

  useEffect(() => {
    if (publicId && scanId) {
      setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId);
      loadReport();
    }
  }, [publicId, scanId]);

  return (
    <Fragment>
      <ReportFilters {...({
        $state,
        setAggregateReportEntries,
        setExactValueFilter,
        exactValueFilters,
        aggregate
      })}/>
      <main className="nx-page-main iq-app-report">
        <LoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
          <ReportTitle metadataDetails={metadata}
                       scanId={scanId}
                       publicId={publicId}
                       selectedReport={selectedReport}
                       reevaluateReport={reevaluateReport}
                       stateGo={stateGo} />
          <ReportStatusBar selectedReport={selectedReport} />
          <ReportContent selectedReport={selectedReport}
                         substringFilters={substringFilters}
                         setSorting={setSorting}
                         sortConfiguration={sortConfiguration}
                         setStringFieldFilter={setStringFieldFilter}
                         setSortingParameters={setSortingParameters}/>
        </LoadWrapper>
      </main>
    </Fragment>
  );
}

ReportPage.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired
  }),
  // actions
  setReportParameters: PropTypes.func.isRequired,
  loadReport: PropTypes.func.isRequired,
  reevaluateReport: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  setSorting: PropTypes.func,
  setStringFieldFilter: PropTypes.func,
  setSortingParameters: PropTypes.func,
  setExactValueFilter: PropTypes.func.isRequired,
  setAggregateReportEntries: PropTypes.func.isRequired,
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
  loading: PropTypes.bool,
  loadError: LoadWrapper.propTypes.error,
  aggregate: PropTypes.bool.isRequired,
  exactValueFilters: PropTypes.object.isRequired,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string
  }),
  substringFilters: PropTypes.shape({
    policyName: PropTypes.string,
    derivedComponentName: PropTypes.string
  })
};
