/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxH1,
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxSmallThreatCounter,
  NxStatefulSubmitMask,
  NxTile,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectRouterPrevParams,
  selectPrevStateIsRepositoryManagerView,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';

import ContainerRepositoryResultsFilterDrawer from './containerRepositoryResultsFilterDrawer/ContainerRepositoryResultsFilterDrawer';
import selectContainerRepositoryResults from './containerRepositoryResultsPageSelectors';
import { actions } from './containerRepositoryResultsPageSlice';
import ContainerRepositoryResultsTable from './containerRepositoryResultsTable/ContainerRepositoryResultsTable';

import './ContainerRepositoryResultsPage.scss';

const ContainerRepositoryResultsPage = () => {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();

  const { repositoryId } = useSelector(selectRouterCurrentParams);

  const { errorMessage, evaluationSummary, loading, repositoryInformation, submitMask } = useSelector(
    selectContainerRepositoryResults
  );

  const prevParams = useSelector(selectRouterPrevParams);
  const prevStateIsRepositoryManagerView = useSelector(selectPrevStateIsRepositoryManagerView);

  const load = async () => {
    dispatch(actions.setLoading(true));
    dispatch(actions.setRepositoryId(repositoryId));
    await dispatch(actions.loadRepositoryInformation());
    await dispatch(actions.loadEvaluationSummary());
    await dispatch(actions.loadTable());
    dispatch(actions.setLoading(false));
  };

  useEffect(() => {
    load();
  }, [repositoryId]);

  const backButtonHref = prevStateIsRepositoryManagerView
    ? uiRouterState.href('firewall.management.view.repository_manager', {
        repositoryManagerId: prevParams.repositoryManagerId,
      })
    : uiRouterState.href('firewall.management.view.repository_container', {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });

  const backButtonText = prevStateIsRepositoryManagerView ? 'Repository Manager' : 'Repository Managers';

  return (
    <>
      <ContainerRepositoryResultsFilterDrawer repositoryId={repositoryId} />

      <NxPageMain id="container-repository-results-page" className="container-repository-results-page">
        <MenuBarBackButton href={backButtonHref} text={`Back to ${backButtonText}`} />
        <NxLoadWrapper
          className="nx-viewport-sized__container"
          retryHandler={() => load()}
          loading={loading}
          error={errorMessage}
        >
          <NxPageTitle>
            <NxH1 id="container-repository-results-page__title">
              {repositoryInformation?.publicId || ''} Repository Results
            </NxH1>
          </NxPageTitle>

          <NxTile className="container-repository-results-page__summary-tile">
            <div className="container-repository-results-page__summary">
              <div className="container-repository-results-page__summary-item">
                <NxSmallThreatCounter
                  criticalCount={evaluationSummary.criticalViolationCount}
                  severeCount={evaluationSummary.severeViolationCount}
                  moderateCount={evaluationSummary.moderateViolationCount}
                />
                <div className="container-repository-results-page__summary-item__label">
                  <span>{evaluationSummary.totalContainerImageViolationCount} VIOLATIONS</span>
                  <span>Affecting {evaluationSummary.affectedContainerImageCount} containers</span>
                </div>
              </div>
              <div className="container-repository-results-page__summary-item">
                <span>{evaluationSummary.totalContainerImageCount} CONTAINERS</span>
              </div>
              <div className="container-repository-results-page__summary-item">
                <div
                  className="container-repository-results-page__summary-item__value"
                  data-testid="evaluation-summary-quarantined-count"
                >
                  {evaluationSummary.quarantinedContainerImageCount}
                </div>
                <div className="container-repository-results-page__summary-item__label">
                  <span>QUARANTINED</span>
                  <span>Components</span>
                </div>
              </div>
            </div>
          </NxTile>

          <ContainerRepositoryResultsTable />
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
};

export default ContainerRepositoryResultsPage;
