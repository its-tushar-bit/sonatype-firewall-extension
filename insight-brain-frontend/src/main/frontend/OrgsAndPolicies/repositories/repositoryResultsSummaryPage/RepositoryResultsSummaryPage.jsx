/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  NxPageMain,
  NxPageTitle,
  NxH1,
  NxButton,
  NxFontAwesomeIcon,
  NxButtonBar,
  NxLoadWrapper,
  NxModal,
  NxInfoAlert,
  NxStatefulSubmitMask,
} from '@sonatype/react-shared-components';
import { faSync } from '@fortawesome/pro-solid-svg-icons';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectPrevStateIsFirewallDashboard,
  selectPrevStateIsRepositoryManagerView,
  selectRouterCurrentParams,
  selectRouterPrevParams,
  selectHideBackButtonParam,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { actions } from './repositoryResultsSummaryPageSlice';
import {
  selectReEvaluateMaskSuccess,
  selectRepositoryInformation,
  selectRepositoryResultsSummaryPageSlice,
  selectShowMaskSuccessDialog,
} from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import ReportStatusBar from 'MainRoot/applicationReport/ReportStatusBar';
import RepositoryResultsComponentsTable from './repositoryResultsComponentsTable/RepositoryResultsComponentsTable';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { selectOwnersMap } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';

export default function RepositoryResultsSummaryPage() {
  const params = useSelector(selectRouterCurrentParams);
  const repositoryInfo = useSelector(selectRepositoryInformation);
  const repositorySummary = useSelector(selectRepositoryResultsSummaryPageSlice);
  const errorSummaryTile = repositorySummary.errorSummaryTile;
  const errorRepositoryInformation = repositorySummary.errorRepositoryInformation;
  const isLoading = repositorySummary.loadingSummaryTile || repositorySummary.loadingRepositoryInformation;
  const errorFound = errorSummaryTile || errorRepositoryInformation;
  const showReEvaluateMaskSuccess = useSelector(selectReEvaluateMaskSuccess);
  const showMaskSuccessDialog = useSelector(selectShowMaskSuccessDialog);
  const ownersMap = useSelector(selectOwnersMap);
  const prevParams = useSelector(selectRouterPrevParams);
  const prevStateIsFirewall = useSelector(selectPrevStateIsFirewallDashboard);
  const prevStateIsRepositoryManagerView = useSelector(selectPrevStateIsRepositoryManagerView);
  const hideBackButton = useSelector(selectHideBackButtonParam);

  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const [showReEvaluateReportModal, setShowReEvaluateReportModal] = useState(false);

  const modalCloseHandler = () => setShowReEvaluateReportModal(false);
  const cancelReEvaluateReport = () => setShowReEvaluateReportModal(false);

  function reEvaluatePolicy() {
    dispatch(actions.reevaluateRepository(params.repositoryId));
  }

  const loadInitData = () => {
    dispatch(ownerSideNavActions.load());
    dispatch(actions.loadData(params.repositoryId));
  };
  useEffect(() => {
    loadInitData();
  }, []);

  const repositoryManagerName = prevStateIsRepositoryManagerView ? ownersMap[prevParams.repositoryManagerId].name : '';

  const backButtonHref = () => {
    if (prevStateIsFirewall) {
      return uiRouterState.href('firewall.firewallPage');
    }
    if (prevStateIsRepositoryManagerView) {
      return uiRouterState.href('management.view.repository_manager', {
        repositoryManagerId: prevParams.repositoryManagerId,
      });
    } else {
      return uiRouterState.href('management.view.repository_container', {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });
    }
  };

  const backButtonText = prevStateIsFirewall
    ? 'Firewall Dashboard'
    : prevStateIsRepositoryManagerView
    ? repositoryManagerName
    : 'Repository Managers';

  return (
    <>
      <RepositoryResultsComponentsFilter repositoryId={params.repositoryId} />
      <NxPageMain>
        {!hideBackButton && <MenuBarBackButton href={backButtonHref()} text={`Back to ${backButtonText}`} />}
        {showMaskSuccessDialog && <NxStatefulSubmitMask success={showReEvaluateMaskSuccess} message="Re-Evaluating" />}
        <NxLoadWrapper
          retryHandler={() => {
            loadInitData();
          }}
          loading={isLoading}
          error={errorFound}
          className="nx-viewport-sized__container"
        >
          <NxPageTitle>
            <NxH1>{repositoryInfo ? repositoryInfo.publicId : ''} Repository Results</NxH1>
            <NxButtonBar>
              <NxButton
                id="iq-repository-results-summary-page__reevaluate-button"
                variant="tertiary"
                onClick={() => {
                  setShowReEvaluateReportModal(true);
                }}
              >
                <NxFontAwesomeIcon icon={faSync} />
                <span>Re-Evaluate Repository</span>
              </NxButton>
            </NxButtonBar>

            {showReEvaluateReportModal && (
              <NxModal onCancel={modalCloseHandler} aria-labelledby="modal-header-text">
                <header className="nx-modal-header">
                  <h2 className="nx-h2" id="modal-header-text">
                    Re-evaluate Repository
                  </h2>
                </header>
                <div className="nx-modal-content">
                  <NxInfoAlert>
                    Re-evaluating the repository can be a time-consuming process depending on repository size. If you
                    proceed, the re-evaluation will run in the background.
                  </NxInfoAlert>
                </div>
                <div className="iq-reevaluate-modal-btn-bar nx-btn-bar">
                  <NxButton
                    variant="secondary"
                    id="re-evaluate-report-cancel-button"
                    type="button"
                    onClick={cancelReEvaluateReport}
                  >
                    Cancel
                  </NxButton>
                  <a
                    id="re-evaluate-report-generate-report-button"
                    onClick={() => {
                      setShowReEvaluateReportModal(false);
                      reEvaluatePolicy();
                    }}
                    className="nx-btn nx-btn--primary"
                  >
                    Re-evaluate
                  </a>
                </div>
              </NxModal>
            )}
          </NxPageTitle>
          <ReportStatusBar
            criticalViolationCount={repositorySummary.criticalViolationCount}
            severeViolationCount={repositorySummary.severeViolationCount}
            moderateViolationCount={repositorySummary.moderateViolationCount}
            knownArtifactCount={repositorySummary.knownComponentCount}
            totalArtifactCount={repositorySummary.totalComponentCount}
            quarantinedComponentCount={repositorySummary.quarantinedComponentCount}
            policyComponentCount={repositorySummary.affectedComponentCount}
            nonLowViolationCount={
              repositorySummary.criticalViolationCount +
              repositorySummary.severeViolationCount +
              repositorySummary.moderateViolationCount
            }
            showQuarantinedSection={true}
            showLegacyViolationsSection={false}
          />
          <RepositoryResultsComponentsTable repositoryId={params.repositoryId} />
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
}
