/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faSync } from '@fortawesome/pro-solid-svg-icons';
import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxModal,
  NxPageMain,
  NxPageTitle,
  NxStatefulSubmitMask,
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

  const { loading, errorMessage, showReevaluationModal, repositoryInformation, submitMask } = useSelector(
    selectContainerRepositoryResults
  );

  const setShowReevaluationModal = (value) => dispatch(actions.setShowReevaluationModal(value));
  const reevaluateRepository = () => dispatch(actions.reevaluateRepository());

  const prevParams = useSelector(selectRouterPrevParams);
  const prevStateIsRepositoryManagerView = useSelector(selectPrevStateIsRepositoryManagerView);

  const load = async () => {
    dispatch(actions.setLoading(true));
    dispatch(actions.setRepositoryId(repositoryId));
    await dispatch(actions.loadRepositoryInformation());
    await dispatch(actions.loadTable());
  };

  useEffect(() => {
    load();
  }, [repositoryId]);

  const backButtonHref = prevStateIsRepositoryManagerView
    ? uiRouterState.href('management.view.repository_manager', {
        repositoryManagerId: prevParams.repositoryManagerId,
      })
    : uiRouterState.href('management.view.repository_container', {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });

  const backButtonText = prevStateIsRepositoryManagerView ? 'Repository Manager' : 'Repository Managers';

  return (
    <>
      <ContainerRepositoryResultsFilterDrawer repositoryId={repositoryId} />

      <NxPageMain className="container-repository-results-page">
        <MenuBarBackButton href={backButtonHref} text={`Back to ${backButtonText}`} />
        {submitMask.show && <NxStatefulSubmitMask success={submitMask.success} message="Re-Evaluating" />}
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

            <NxButtonBar>
              <NxButton
                id="container-repository-results-page__reevaluate-button"
                variant="tertiary"
                onClick={() => setShowReevaluationModal(true)}
              >
                <NxFontAwesomeIcon icon={faSync} />
                <span>Re-evaluate Repository</span>
              </NxButton>
            </NxButtonBar>

            {showReevaluationModal && (
              <NxModal aria-labelledby="reevaluation-modal__header" onCancel={() => setShowReevaluationModal(false)}>
                <NxModal.Header>
                  <NxH2 id="reevaluation-modal__header">Re-evaluate Repository</NxH2>
                </NxModal.Header>

                <NxModal.Content>
                  <NxInfoAlert>
                    Re-evaluating the repository can be a time-consuming process depending on repository size. If you
                    proceed, the re-evaluation will run in the background.
                  </NxInfoAlert>
                </NxModal.Content>

                <NxButtonBar className="reevaluation-modal__button-bar">
                  <NxButton
                    id="reevaluation-modal__cancel-button"
                    variant="secondary"
                    type="button"
                    onClick={() => setShowReevaluationModal(false)}
                  >
                    Cancel
                  </NxButton>
                  <a
                    id="reevaluation-modal__generate-report-button"
                    className="nx-btn nx-btn--primary"
                    onClick={() => {
                      setShowReevaluationModal(false);
                      reevaluateRepository();
                    }}
                  >
                    Re-evaluate
                  </a>
                </NxButtonBar>
              </NxModal>
            )}
          </NxPageTitle>

          <ContainerRepositoryResultsTable />
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
};

export default ContainerRepositoryResultsPage;
