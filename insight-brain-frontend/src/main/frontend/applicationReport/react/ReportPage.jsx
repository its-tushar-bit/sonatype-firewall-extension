/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import { NxLoadWrapper, NxStatefulSubmitMask } from '@sonatype/react-shared-components';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilterPopover from './ReportFilterPopover';
import ReportTitle from './ReportTitle';
import * as PropTypes from 'prop-types';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

export default function ReportPage(props) {
  const {
    // actions
    setReportParameters,
    loadReportIfNeeded: loadReport,
    reevaluateMaskState,
    setExactValueFilter,
    // state
    publicId,
    scanId,
    unknownjs,
    embeddable,
    policyViolationId,
    loading,
    loadError,
    exactValueFilters,
  } = props;

  useEffect(() => {
    if (publicId && scanId) {
      setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId);
      loadReport();
    }
  }, [publicId, scanId]);

  return (
    <Fragment>
      {reevaluateMaskState !== null && <NxStatefulSubmitMask success={reevaluateMaskState} message="Re-Evaluating" />}
      <main id="app-report" className="nx-page-main nx-viewport-sized iq-app-report">
        <MenuBarBackButton text="All Reports" stateName={'violations'} />
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
          <ReportFilterPopover
            {...{
              setExactValueFilter,
              exactValueFilters,
            }}
          />
          <ReportTitle />
          <ReportStatusBar />
          <ReportContent />
        </NxLoadWrapper>
      </main>
    </Fragment>
  );
}

ReportPage.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }),
  // actions
  setReportParameters: PropTypes.func.isRequired,
  loadReportIfNeeded: PropTypes.func.isRequired,
  setExactValueFilter: PropTypes.func.isRequired,
  // state
  publicId: PropTypes.string,
  scanId: PropTypes.string,
  unknownjs: PropTypes.bool,
  embeddable: PropTypes.bool,
  policyViolationId: PropTypes.string,
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
    displayedEntries: PropTypes.arrayOf(
      PropTypes.shape({
        derivedComponentName: PropTypes.string,
        policyName: PropTypes.string,
        hash: PropTypes.string,
        derivedDependencyType: PropTypes.string,
        filenames: PropTypes.array,
        displayName: PropTypes.shape({
          name: PropTypes.string,
          parts: PropTypes.array,
        }),
        waived: PropTypes.bool,
        grandfathered: PropTypes.bool,
        policyThreatLevel: PropTypes.number,
      })
    ),
  }),
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  exactValueFilters: PropTypes.object.isRequired,
  reevaluateMaskState: PropTypes.bool,
  goToDependencyTreePage: PropTypes.func.isRequired,
  dependencyTreeIsAvailable: PropTypes.bool,
  dependencyTreeUnavailableMessage: PropTypes.string,
  toggleShowFilterPopover: PropTypes.func.isRequired,
};
