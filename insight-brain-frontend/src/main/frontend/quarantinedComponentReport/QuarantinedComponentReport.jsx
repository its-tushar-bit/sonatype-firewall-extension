/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { NxWarningAlert, NxInfoAlert } from '@sonatype/react-shared-components';
import { formatDate } from 'MainRoot/util/dateUtils';

import QuarantineComponentOverviewTile from './componentOverviewTile/QuarantineComponentOverviewTile';
import QuarantineComponentOverviewDescriptionTile from './componentOverviewTile/QuarantinedComponentOverviewDescriptionTile';
import PolicyViolationsTile from 'MainRoot/quarantinedComponentReport/policyViolationsTile/PolicyViolationsTile';
import OtherVersionsTile from 'MainRoot/quarantinedComponentReport/otherVersionsTile/OtherVersionsTile';
import { RiskRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediation';

import { actions as riskRemediationActions } from './riskRemediationTile/riskRemediationSlice';
import {
  selectVersionExplorerData,
  selectSelectedVersionData,
  selectCurrentVersionComparisonData,
  selectSelectedVersionComparisonData,
} from './riskRemediationTile/riskRemediationSelectors';

export default function QuarantinedComponentReport(props) {
  const dispatch = useDispatch();

  // Props
  const { token, loadQuarantineReportData } = props;

  // Actions
  const loadVersionExplorerData = () => dispatch(riskRemediationActions.loadVersionExplorerData(token));
  const loadSelectedVersionData = (version) =>
    dispatch(riskRemediationActions.loadSelectedVersionData({ token: token, version: version }));

  // Selectors
  const selectedVersionData = useSelector(selectSelectedVersionData);
  const selectedVersionExplorerData = useSelector(selectVersionExplorerData);
  const currentVersionDetails = useSelector(selectCurrentVersionComparisonData);
  const selectedVersionDetails = useSelector(selectSelectedVersionComparisonData);

  // viewState
  const { loadError, componentOverview, violations, violationsLoading, violationsLoadError } = props;

  const dataLoading = componentOverview.componentOverviewLoading || !componentOverview.componentDisplayName;

  useEffect(() => {
    loadQuarantineReportData(token);
  }, [token]);

  return (
    <main id="quarantined-component-report" className="nx-page-main">
      {componentOverview?.tokenExpiryTime && (
        <NxInfoAlert>This report will expire on {formatDate(componentOverview.tokenExpiryTime)}</NxInfoAlert>
      )}
      <div className="nx-page-title">
        <h1 className="nx-h1">Quarantined Component View</h1>
        <div className="nx-page-title__description">{formatDate(new Date())}</div>
      </div>

      {loadError != null ? (
        <NxWarningAlert>{loadError?.response?.data}</NxWarningAlert>
      ) : (
        <>
          <QuarantineComponentOverviewDescriptionTile />

          <LoadWrapper retryHandler={() => loadQuarantineReportData(token)} error={loadError} loading={dataLoading}>
            <QuarantineComponentOverviewTile componentOverview={componentOverview} />
          </LoadWrapper>

          <RiskRemediation
            stageId="proxy"
            currentVersion={componentOverview.componentVersion}
            routeName=""
            componentInformation={{}}
            versionExplorerData={selectedVersionExplorerData}
            selectedVersionData={selectedVersionData}
            loadVersionExplorerData={loadVersionExplorerData}
            loadSelectedVersionData={loadSelectedVersionData}
            currentVersionComparisonData={currentVersionDetails}
            selectedVersionComparisonData={selectedVersionDetails}
          />

          <LoadWrapper
            retryHandler={() => loadQuarantineReportData(token)}
            error={violationsLoadError}
            loading={violationsLoading}
          >
            <PolicyViolationsTile violations={violations} />
          </LoadWrapper>

          <OtherVersionsTile />
        </>
      )}
    </main>
  );
}

export const ConditionPropType = {
  conditionType: PropTypes.string.isRequired,
  conditionSummary: PropTypes.string.isRequired,
  conditionReason: PropTypes.string.isRequired,
  conditionTriggerReference: PropTypes.string,
};

export const ConstraintPropType = {
  constraintId: PropTypes.string.isRequired,
  constraintName: PropTypes.string.isRequired,
  constraintOperator: PropTypes.string.isRequired,
  conditions: PropTypes.arrayOf(PropTypes.shape(ConditionPropType)),
};

export const PolicyViolationPropType = {
  policyId: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  policyThreatLevel: PropTypes.number.isRequired,
  constraints: PropTypes.arrayOf(PropTypes.shape(ConstraintPropType)),
};

QuarantinedComponentReport.propTypes = {
  token: PropTypes.string.isRequired,
  loadQuarantineReportData: PropTypes.func.isRequired,
  loadError: PropTypes.object,
  componentOverview: PropTypes.object,
  violations: PropTypes.arrayOf(PropTypes.shape({ ...PolicyViolationPropType })),
  violationsLoading: PropTypes.bool,
  violationsLoadError: PropTypes.object,
};
