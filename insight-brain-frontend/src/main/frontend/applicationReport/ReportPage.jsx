/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, Fragment } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { pick } from 'ramda';
import {
  NxButton,
  NxErrorAlert,
  NxFooter,
  NxLoadWrapper,
  NxModal,
  NxStatefulSubmitMask,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilterPopover from './ReportFilterPopover';
import ReportTitle from './ReportTitle';
import UnscannedComponentsTable from './unscannedComponentsTable/UnscannedComponentsTable';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import {
  selectApplicationReportLoading,
  selectApplicationReportSlice,
  selectDependencyTreeIsOldReport,
  selectHasUnscannedComponents,
  selectIsPolicyTypeFilterEnabled,
  selectReportStageId,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as applicationReportActions from './applicationReportActions';
import { actions as latestReportForStageActions } from './latestReportForStageSlice';
import { selectSelectedReport } from './applicationReportSelectors';
import { NxStatefulErrorAlert } from '@sonatype/react-shared-components';
import { isNilOrEmpty } from '../util/jsUtil';
import { useRouterState } from '../react/RouterStateContext';
import { NewerReportAvailable } from 'MainRoot/applicationReport/NewerReportAvailable';

export default function ReportPage() {
  const applicationReport = useSelector(selectApplicationReportSlice);
  const reevaluationError = applicationReport.reevaluationError;
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const isPolicyTypeFilterEnabled = useSelector(selectIsPolicyTypeFilterEnabled);
  const isOldReportWithNoDependencyInfo = useSelector(selectDependencyTreeIsOldReport);
  const hasUnscannedComponents = useSelector(selectHasUnscannedComponents);
  const selectedReport = useSelector(selectSelectedReport);
  const { loadError, reevaluateMaskState } = pick(['loadError', 'reevaluateMaskState'], applicationReport);
  const [showUnscannedComponentsModal, setShowUnscannedComponentsModal] = useState(false);
  const modalCloseHandler = () => setShowUnscannedComponentsModal(false);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const stageId = useSelector(selectReportStageId);

  const { publicId, scanId, unknownjs, embeddable, policyViolationId } = routerCurrentParams;

  const loading = useSelector(selectApplicationReportLoading);

  const dispatch = useDispatch();
  const loadReport = () => dispatch(applicationReportActions.loadReportIfNeeded());

  const setReportParameters = (appId, scanId, isUnknownJs, embeddable, policyViolationId, componentHash, tabId) =>
    dispatch(
      applicationReportActions.setReportParameters(
        appId,
        scanId,
        isUnknownJs,
        embeddable,
        policyViolationId,
        componentHash,
        tabId
      )
    );

  const totalApplicationRisk = isNilOrEmpty(applicationReport?.metadata?.totalRisk)
    ? 'N/A'
    : applicationReport.metadata.totalRisk;

  const reportStatusBarProps = { ...selectedReport, totalApplicationRisk, isDeveloperDashboardEnabled };

  useEffect(() => {
    if (publicId && scanId) {
      setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId);
      loadReport();
    }
  }, [publicId, scanId]);

  useEffect(() => {
    if (publicId && stageId) {
      dispatch(
        latestReportForStageActions.loadLatestReportForStage({ applicationPublicId: publicId, stageTypeId: stageId })
      );
    }
  }, [publicId, stageId]);

  return (
    <Fragment>
      {reevaluateMaskState !== null && <NxStatefulSubmitMask success={reevaluateMaskState} message="Re-Evaluating" />}
      <main id="app-report" className="nx-page-main iq-app-report">
        <BackButton />
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
          <ReportFilterPopover />
          {hasUnscannedComponents && (
            <NxErrorAlert id="application-report-unscannable-components-error">
              <span>You have unscannable components in this build</span>
              <div className="nx-btn-bar">
                <NxButton variant="error" onClick={() => setShowUnscannedComponentsModal(true)}>
                  View
                </NxButton>
              </div>
            </NxErrorAlert>
          )}
          {showUnscannedComponentsModal && (
            <NxModal
              onCancel={modalCloseHandler}
              aria-labelledby="unscanned-modal-header-text"
              id="unscanned-components-modal"
            >
              <NxModal.Header>
                <h2 className="nx-h2">Unscannable Components</h2>
              </NxModal.Header>
              <NxModal.Content tabIndex={0}>
                <UnscannedComponentsTable />
              </NxModal.Content>
              <NxFooter>
                <div className="nx-btn-bar">
                  <NxButton onClick={modalCloseHandler}>Close</NxButton>
                </div>
              </NxFooter>
            </NxModal>
          )}

          <ReportTitle />

          <NewerReportAvailable />

          {!isPolicyTypeFilterEnabled && (
            <NxWarningAlert id="application-report-policy-type-filter-warning">
              This report has not been upgraded for the new Policy Types filter introduced in release 61. Re-evaluate in
              order to enable the Policy Types filter.
            </NxWarningAlert>
          )}
          {isOldReportWithNoDependencyInfo && (
            <NxWarningAlert id="application-report-no-dependency-info-warning">
              This report was generated with an older version of IQ. Please re-scan the application.
            </NxWarningAlert>
          )}

          <ReevaluationError reevaluationError={reevaluationError} />

          <ReportStatusBar {...reportStatusBarProps} />
          <ReportContent />
        </NxLoadWrapper>
      </main>
    </Fragment>
  );
}

function ReevaluationError({ reevaluationError }) {
  if (reevaluationError === 'Insufficient permissions') {
    return <NxStatefulErrorAlert>Insufficient Permissions to Re-Evaluate</NxStatefulErrorAlert>;
  } else if (!isNilOrEmpty(reevaluationError)) {
    return <NxStatefulErrorAlert>{reevaluationError}</NxStatefulErrorAlert>;
  } else {
    return null;
  }
}

function BackButton() {
  const uiRouterState = useRouterState();
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);

  const { publicId, scanId } = useSelector(selectRouterCurrentParams);
  if (isPrioritiesPageContainer) {
    const prioritiesPageHref = uiRouterState.href(prioritiesPageName, {
      scanId: scanId,
      publicAppId: publicId,
    });
    return <MenuBarBackButton href={prioritiesPageHref} text="Back to Priorities" />;
  }
  return <MenuBarBackButton text="All Reports" stateName={'violations'} />;
}
