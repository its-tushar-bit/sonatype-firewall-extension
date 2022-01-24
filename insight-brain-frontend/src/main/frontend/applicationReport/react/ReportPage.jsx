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
    reevaluateReport,
    reevaluateMaskState,
    setSorting,
    setSortingParameters,
    setExactValueFilter,
    toggleAggregateReportEntries,
    setStringFieldFilter,
    toggleShowFilterPopover,
    selectComponent,
    goToComponentDetailsPage,
    goToDependencyTreePage,
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
    dependencyTreeIsAvailable,
    dependencyTreeUnavailableMessage,
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
          <ReportTitle
            metadataDetails={metadata}
            scanId={scanId}
            publicId={publicId}
            selectedReport={selectedReport}
            reevaluateReport={reevaluateReport}
            stateGo={stateGo}
          />
          <ReportStatusBar selectedReport={selectedReport} />
          <ReportContent
            aggregate={aggregate}
            selectedReport={selectedReport}
            substringFilters={substringFilters}
            setSorting={setSorting}
            sortConfiguration={sortConfiguration}
            setStringFieldFilter={setStringFieldFilter}
            setSortingParameters={setSortingParameters}
            toggleShowFilterPopover={toggleShowFilterPopover}
            selectComponent={selectComponent}
            goToComponentDetailsPage={goToComponentDetailsPage}
            goToDependencyTreePage={goToDependencyTreePage}
            dependencyTreeIsAvailable={dependencyTreeIsAvailable}
            dependencyTreeUnavailableMessage={dependencyTreeUnavailableMessage}
            toggleAggregateReportEntries={toggleAggregateReportEntries}
          />
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
  reevaluateReport: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  setSorting: PropTypes.func,
  setStringFieldFilter: PropTypes.func,
  setSortingParameters: PropTypes.func,
  setExactValueFilter: PropTypes.func.isRequired,
  toggleAggregateReportEntries: PropTypes.func.isRequired,
  selectComponent: PropTypes.func.isRequired,
  goToComponentDetailsPage: PropTypes.func.isRequired,
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
      id: PropTypes.string,
      name: PropTypes.string.isRequired,
    }),
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
  aggregate: PropTypes.bool.isRequired,
  exactValueFilters: PropTypes.object.isRequired,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
  }),
  substringFilters: PropTypes.shape({
    policyName: PropTypes.string,
    derivedComponentName: PropTypes.string,
  }),
  reevaluateMaskState: PropTypes.bool,
  goToDependencyTreePage: PropTypes.func.isRequired,
  dependencyTreeIsAvailable: PropTypes.bool,
  dependencyTreeUnavailableMessage: PropTypes.string,
  toggleShowFilterPopover: PropTypes.func.isRequired,
};
