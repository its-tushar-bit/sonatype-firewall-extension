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
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { actions } from './repositoryResultsSummaryPageSlice';
import {
  selectReEvaluateMaskSuccess,
  selectRepositoryInformation,
  selectRepositoryResultsSummaryPageSlice,
  selectShowFilterPopover,
  selectShowMaskSuccessDialog,
} from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import ReportStatusBar from 'MainRoot/applicationReport/ReportStatusBar';
import RepositoryResultsComponentsTable from './repositoryResultsComponentsTable/RepositoryResultsComponentsTable';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';

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

  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const [showReEvaluateReportModal, setShowReEvaluateReportModal] = useState(false);

  const modalCloseHandler = () => setShowReEvaluateReportModal(false);
  const cancelReEvaluateReport = () => setShowReEvaluateReportModal(false);

  function reEvaluatePolicy() {
    dispatch(actions.reevaluateRepository(params.repositoryId));
  }

  const loadInitData = () => {
    dispatch(actions.loadData(params.repositoryId));
  };
  useEffect(() => {
    loadInitData();
  }, []);

  return (
    <>
      <RepositoryResultsComponentsFilter repositoryId={params.repositoryId} />
      <NxPageMain>
        <MenuBarBackButton href={uiRouterState.href('management.view.repositories')} text="Back to Repositories" />
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
            showGrandfatheredSection={false}
          />
          <RepositoryResultsComponentsTable repositoryId={params.repositoryId} />
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
}
