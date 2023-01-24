/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxH2,
  NxModal,
  NxTable,
  NxTextLink,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { actions } from './repositoriesConfigurationSlice';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectDeleteModal,
  selectDeleteModalInfo,
  selectRepositories,
  selectRepositoriesLoadError,
  selectRepositoriesLoading,
  selectSubmitMaskState,
  selectRepositoriesDeleteError,
  selectSortConfiguration,
} from './repositoriesConfigurationSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const RepositoriesConfigurationTile = () => {
  const dispatch = useDispatch();

  const loadRepositories = () => dispatch(actions.loadRepositories());
  const setShowDeleteModal = (isShown) => dispatch(actions.setShowDeleteModal(isShown));
  const deleteRepository = () => dispatch(actions.deleteRepository());
  const setSort = (column) => dispatch(actions.setSort(column));
  const openDeleteModal = (modalInfo) => dispatch(actions.openDeleteModal(modalInfo));

  const repositories = useSelector(selectRepositories);
  const isLoading = useSelector(selectRepositoriesLoading);
  const loadError = useSelector(selectRepositoriesLoadError);
  const deleteError = useSelector(selectRepositoriesDeleteError);
  const showDeleteModal = useSelector(selectDeleteModal);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const deleteModalInfo = useSelector(selectDeleteModalInfo);
  const sortConfiguration = useSelector(selectSortConfiguration);

  const uiRouterState = useRouterState();

  useEffect(() => {
    loadRepositories();
  }, []);

  const deleteModal = (
    <NxModal
      id="repositories-delete-modal"
      data-testid="delete-modal"
      onCancel={() => setShowDeleteModal(false)}
      variant="narrow"
      aria-labelledby="repositories-delete-label-modal"
    >
      <NxStatefulForm
        onSubmit={deleteRepository}
        onCancel={() => setShowDeleteModal(false)}
        submitBtnText="Continue"
        submitError={deleteError}
        submitMaskState={submitMaskState}
        submitMaskMessage="Removing…"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="repositories-delete-label-modal">
            <span>Remove Repository</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            Are you sure you want to remove the Repository with ID &quot;{deleteModalInfo.publicId}&quot;? This action
            is not reversible.
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );

  const mapRepositoryToRow = (repository) => {
    const repositoryData = repository.repository;
    return (
      <NxTable.Row key={repositoryData.id}>
        <NxTable.Cell className="iq-repositories-configuration-table-repository">
          <NxTextLink newTab href={uiRouterState.href('repository-report', { repositoryId: repositoryData.id })}>
            {repositoryData.publicId}
          </NxTextLink>
        </NxTable.Cell>
        <NxTable.Cell className="iq-repositories-configuration-table-repository-manager">
          {repository.managerInstanceId}
        </NxTable.Cell>
        <NxTable.Cell>{repositoryData.enabled ? ' Enabled' : 'Disabled'}</NxTable.Cell>
        <NxTable.Cell>
          <div className="nx-btn-bar">
            <NxButton
              data-testid="repository-delete-button"
              variant="icon-only"
              title="Delete"
              onClick={() => openDeleteModal({ publicId: repositoryData.publicId, id: repositoryData.id })}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTable.Cell>
      </NxTable.Row>
    );
  };

  return (
    <NxTile id="repositories-pill-configuration" data-testid="repositories_configuration">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Configuration</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxTable id="iq-repositories-configuration-table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell
                isSortable
                sortDir={sortConfiguration?.column === 'publicId' ? sortConfiguration.dir : null}
                onClick={() => setSort('publicId')}
              >
                Repository
              </NxTable.Cell>
              <NxTable.Cell
                isSortable
                sortDir={sortConfiguration?.column === 'managerInstanceId' ? sortConfiguration.dir : null}
                onClick={() => setSort('managerInstanceId')}
              >
                Repository Manager
              </NxTable.Cell>
              <NxTable.Cell
                isSortable
                sortDir={sortConfiguration?.column === 'enabled' ? sortConfiguration.dir : null}
                onClick={() => setSort('enabled')}
              >
                Status
              </NxTable.Cell>
              <NxTable.Cell />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            emptyMessage="There are no repositories registered with the server."
            error={loadError}
            isLoading={isLoading}
            retryHandler={loadRepositories}
          >
            {repositories.map(mapRepositoryToRow)}
          </NxTable.Body>
        </NxTable>
        {showDeleteModal && deleteModal}
      </NxTile.Content>
    </NxTile>
  );
};

export default RepositoriesConfigurationTile;
