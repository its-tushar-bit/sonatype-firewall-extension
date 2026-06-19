/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect, useState, Fragment } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { pick, pathOr } from 'ramda';
import {
  NxButton,
  NxErrorAlert,
  NxFooter,
  NxLoadWrapper,
  NxModal,
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
  selectIsContainerImagesEvaluationEnabledAndProxyStage,
  selectApplicationReportMetaData,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageName,
  selectPrevStateIsFirewallDashboard,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  FIREWALL_CONTAINER_REPOSITORY_RESULTS,
  FIREWALL_FIREWALLPAGE_CONTAINERS,
} from 'MainRoot/constants/states/firewall';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as applicationReportActions from './applicationReportActions';
import { actions as latestReportForStageActions } from './latestReportForStageSlice';
import { selectSelectedReport } from './applicationReportSelectors';
import { NxStatefulErrorAlert } from '@sonatype/react-shared-components';
import { isNilOrEmpty } from '../util/jsUtil';
import { useRouterState } from '../react/RouterStateContext';
import { NewerReportAvailable } from 'MainRoot/applicationReport/NewerReportAvailable';
import ReevaluationStatusModal from 'MainRoot/applicationReport/ReevaluationStatusModal';
import LegacyScannerBanner from 'MainRoot/applicationReport/LegacyScannerBanner';

export default function ReportPage() {
  const applicationReport = useSelector(selectApplicationReportSlice);
  const reevaluationError = applicationReport.reevaluationError;
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const isPolicyTypeFilterEnabled = useSelector(selectIsPolicyTypeFilterEnabled);
  const isOldReportWithNoDependencyInfo = useSelector(selectDependencyTreeIsOldReport);
  const hasUnscannedComponents = useSelector(selectHasUnscannedComponents);
  const selectedReport = useSelector(selectSelectedReport);
  const { loadError, reevaluating } = pick(['loadError', 'reevaluating'], applicationReport);
  const [showUnscannedComponentsModal, setShowUnscannedComponentsModal] = useState(false);
  const modalCloseHandler = () => setShowUnscannedComponentsModal(false);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const stageId = useSelector(selectReportStageId);
  const isContainerImagesEvaluation = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);

  const { publicId, scanId } = routerCurrentParams;

  const loading = useSelector(selectApplicationReportLoading);

  const dispatch = useDispatch();
  const loadReport = () => dispatch(applicationReportActions.loadReportIfNeeded());

  // Load report data when ReportPage is rendered without ApplicationReportRoot as a parent
  // (e.g. firewall.containerReport route). When ApplicationReportRoot IS the parent, this
  // is a harmless no-op since setReportParameters + loadReportIfNeeded are idempotent for
  // the same publicId/scanId. Deps restricted to [publicId, scanId] to avoid the race
  // condition described in ApplicationReportRoot.jsx.
  useEffect(() => {
    if (publicId && scanId) {
      dispatch(
        applicationReportActions.setReportParameters(
          publicId,
          scanId,
          !!routerCurrentParams.unknownjs,
          !!routerCurrentParams.embeddable,
          routerCurrentParams.policyViolationId,
          routerCurrentParams.componentHash,
          routerCurrentParams.tabId,
          true
        )
      );
      dispatch(applicationReportActions.loadReportIfNeeded());
    }
  }, [dispatch, publicId, scanId]);

  const totalApplicationRisk = isNilOrEmpty(applicationReport?.metadata?.totalRisk)
    ? 'N/A'
    : applicationReport.metadata.totalRisk;

  const { origin } = routerCurrentParams;
  const isHostedRepoComponent = origin === 'hostedRepoComponents';

  const reportStatusBarProps = {
    ...selectedReport,
    totalApplicationRisk,
    isDeveloperDashboardEnabled,
    isContainerImagesEvaluation,
    isHostedRepoComponent,
  };

  useEffect(() => {
    if (publicId && stageId) {
      dispatch(
        latestReportForStageActions.loadLatestReportForStage({ applicationPublicId: publicId, stageTypeId: stageId })
      );
    }
  }, [publicId, stageId]);

  return (
    <Fragment>
      {!reevaluationError && <ReevaluationStatusModal reevaluating={reevaluating} />}
      <ReportFilterPopover />
      <main id="app-report" className="nx-page-main iq-app-report">
        <BackButton />
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
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

          <LegacyScannerBanner />

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
  const isContainerImagesEvaluation = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);
  const isPrevFirewallDashboardPage = useSelector(selectPrevStateIsFirewallDashboard);
  const metadataDetails = useSelector(selectApplicationReportMetaData);
  const repositoryId = pathOr('', ['application', 'organization', 'relatedRepositoryId'], metadataDetails);

  const {
    publicId,
    scanId,
    origin,
    repositoryManagerId,
    repositoryId: hostedRepositoryId,
    repositoryPublicId,
  } = useSelector(selectRouterCurrentParams);

  if (origin === 'hostedRepoComponents') {
    const backHref = uiRouterState.href('hostedRepoComponents', {
      repositoryManagerId,
      repositoryId: hostedRepositoryId,
      repositoryPublicId,
    });
    const backText = repositoryPublicId ? `Back to ${repositoryPublicId}` : 'Back to Repository Components';
    return <MenuBarBackButton href={backHref} text={backText} />;
  }

  if (isPrioritiesPageContainer) {
    const prioritiesPageHref = uiRouterState.href(prioritiesPageName, {
      scanId: scanId,
      publicAppId: publicId,
    });
    return <MenuBarBackButton href={prioritiesPageHref} text="Back to Priorities" />;
  }

  if (origin === FIREWALL_FIREWALLPAGE_CONTAINERS) {
    const backHref = uiRouterState.href(FIREWALL_FIREWALLPAGE_CONTAINERS);
    return <MenuBarBackButton href={backHref} text="Back to Firewall Dashboard" />;
  } else if (origin === FIREWALL_CONTAINER_REPOSITORY_RESULTS) {
    const backHref = uiRouterState.href(FIREWALL_CONTAINER_REPOSITORY_RESULTS, {
      repositoryId: repositoryId,
    });
    return <MenuBarBackButton href={backHref} text="Back to Repository Results" />;
  } else if (isPrevFirewallDashboardPage) {
    const backHref = uiRouterState.href(FIREWALL_FIREWALLPAGE_CONTAINERS);
    return <MenuBarBackButton href={backHref} text="Back to Firewall Dashboard" />;
  } else if (isContainerImagesEvaluation) {
    const backHref = uiRouterState.href(FIREWALL_CONTAINER_REPOSITORY_RESULTS, {
      repositoryId: repositoryId,
    });
    return <MenuBarBackButton href={backHref} text="Back to Repository Results" />;
  }

  return <MenuBarBackButton text="All Reports" stateName={'violations'} />;
}
