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
  selectApplicationReportSlice,
  selectDependencyTreeIsOldReport,
  selectHasUnscannedComponents,
  selectIsPolicyTypeFilterEnabled,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as applicationReportActions from './applicationReportActions';
import { selectSelectedReport } from './applicationReportSelectors';
import { NxStatefulErrorAlert } from '@sonatype/react-shared-components';
import { isNilOrEmpty } from '../util/jsUtil';
import { useRouterState } from '../react/RouterStateContext';

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

  const { publicId, scanId, unknownjs, embeddable, policyViolationId } = routerCurrentParams;
  const loading =
    !applicationReport.loadError && (!!applicationReport.pendingLoads.size || !applicationReport.metadata);

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

  return (
    <Fragment>
      {reevaluateMaskState !== null && <NxStatefulSubmitMask success={reevaluateMaskState} message="Re-Evaluating" />}
      <main id="app-report" className="nx-page-main nx-viewport-sized iq-app-report">
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

          {reevaluationError === 'Insufficient permissions' && (
            <NxStatefulErrorAlert>Insufficient Permissions to Re-Evaluate</NxStatefulErrorAlert>
          )}

          <ReportStatusBar {...reportStatusBarProps} />
          <ReportContent />
        </NxLoadWrapper>
      </main>
    </Fragment>
  );
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
