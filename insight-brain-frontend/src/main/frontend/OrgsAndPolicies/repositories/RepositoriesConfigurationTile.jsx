/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxStatefulForm,
  NxH2,
  NxModal,
  NxTable,
  NxTextLink,
  NxTile,
  NxWarningAlert,
  NxStatefulTextInput,
  NxP,
  NxReadOnly,
  NxFormGroup,
  NxFontAwesomeIcon,
  NxButton,
  NxFilterInput,
  NxStatefulFilterDropdown,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import { faPen, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { actions } from './repositoriesConfigurationSlice';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectDeleteModal,
  selectDeleteModalInfo,
  selectRepositoriesLoadError,
  selectRepositoriesLoading,
  selectSubmitMaskState,
  selectRepositoriesDeleteError,
  selectSortConfiguration,
  selectShowEditRepositoryManagerNameModal,
  selectEditRepositoryManagerNameError,
  selectEditRepositoryManagerNameModalInfo,
  selectRepositoriesByManagerInstanceId,
  selectRepositoryFormats,
  selectRepositoryFormatsFilter,
  selectRepositoryPublicIdFilter,
} from './repositoriesConfigurationSelectors';
import { selectRepoManagerOwnersEntriesSorted } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsRepositoryManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const RepositoriesConfigurationTile = () => {
  const dispatch = useDispatch();

  const loadRepositories = () => dispatch(actions.loadRepositories());
  const setShowDeleteModal = (isShown) => dispatch(actions.setShowDeleteModal(isShown));
  const setShowEditRepositoryManagerNameModal = (isShown) =>
    dispatch(actions.setShowEditRepositoryManagerNameModal(isShown));
  const deleteRepository = () => dispatch(actions.deleteRepository());
  const openDeleteModal = (modalInfo) => dispatch(actions.openDeleteModal(modalInfo));
  const openEditRepositoryManagerNameModal = (modalInfo) =>
    dispatch(actions.openEditRepositoryManagerNameModal(modalInfo));
  const sortRepositories = (column) => dispatch(actions.sortRepositories(column));
  const setRepositoryManagerName = (name) => dispatch(actions.setRepositoryManagerName(name));
  const editRepositoryManagerName = () => dispatch(actions.editRepositoryManagerName());
  const setRepositoryPublicIdFilter = (value) => dispatch(actions.setRepositoryPublicIdFilter(value));
  const setRepositoryFormatsFilter = (value) => dispatch(actions.setRepositoryFormatsFilter(value));
  const loadRepositoriesByManagerId = () => dispatch(actions.loadRepositoriesByManagerId(owner.id));

  const repositoriesByManagerInstanceId = useSelector(selectRepositoriesByManagerInstanceId);
  const repoManagerOwnersEntries = useSelector(selectRepoManagerOwnersEntriesSorted);
  const isLoading = useSelector(selectRepositoriesLoading);
  const loadError = useSelector(selectRepositoriesLoadError);
  const deleteError = useSelector(selectRepositoriesDeleteError);
  const editRepositoryManagerNameError = useSelector(selectEditRepositoryManagerNameError);
  const showDeleteModal = useSelector(selectDeleteModal);
  const showEditRepositoryManagerNameModal = useSelector(selectShowEditRepositoryManagerNameModal);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const deleteModalInfo = useSelector(selectDeleteModalInfo);
  const editRepositoryManagerNameModalInfo = useSelector(selectEditRepositoryManagerNameModalInfo);
  const sortConfiguration = useSelector(selectSortConfiguration);
  const repositoryPublicIdFilter = useSelector(selectRepositoryPublicIdFilter);
  const repositoryFormats = useSelector(selectRepositoryFormats);
  const repositoryFormatsFilter = useSelector(selectRepositoryFormatsFilter);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const owner = useSelector(selectSelectedOwner);

  const uiRouterState = useRouterState();

  const getEnablement = (repository) => {
    const enablement = [];
    if (repository.auditEnabled) {
      enablement.push('Audit');
    }
    if (repository.quarantineEnabled) {
      enablement.push('Quarantine');
    }
    if (repository.namespaceConfusionProtectionEnabled) {
      enablement.push('Namespace Scanning');
    }
    return enablement.join(', ');
  };

  useEffect(() => {
    if (isRepositoryManager) {
      loadRepositoriesByManagerId();
    } else {
      loadRepositories();
    }
  }, [isRepositoryManager]);

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

  const editRepositoryManagerNameModal = (
    <NxModal
      id="edit-repository-manager-name-modal"
      data-testid="edit-repository-manager-name-modal"
      onCancel={() => setShowEditRepositoryManagerNameModal(false)}
      aria-labelledby="repositories-delete-label-modal"
    >
      <NxStatefulForm
        onSubmit={editRepositoryManagerName}
        onCancel={() => setShowEditRepositoryManagerNameModal(false)}
        submitBtnText="Update"
        submitError={editRepositoryManagerNameError}
        submitMaskState={submitMaskState}
        submitMaskMessage="Updating…"
      >
        <NxModal.Header>
          <NxH2>Edit Repository Manager</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxReadOnly>
            <NxReadOnly.Label>Repository Manager ID</NxReadOnly.Label>
            <NxReadOnly.Data>{editRepositoryManagerNameModalInfo.managerInstanceId}</NxReadOnly.Data>
          </NxReadOnly>
          <NxFormGroup label="Repository Manager Name">
            <NxStatefulTextInput
              validator={(value) => (value.length ? null : 'Must be non-empty')}
              defaultValue={editRepositoryManagerNameModalInfo.managerName || ''}
              onChange={setRepositoryManagerName}
            />
          </NxFormGroup>
          <NxP>Any changes made will apply to all repositories for this repository manager.</NxP>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );

  const mapRepositoryToRow = (repository) => {
    const repositoryData = repository.repository;
    return (
      <NxTable.Row key={repositoryData.id}>
        <NxTable.Cell className="iq-repositories-configuration-table-repository">
          {repositoryData.repositoryType === 'hosted' ? (
            <NxOverflowTooltip>
              <div className="nx-truncate-ellipsis">{repositoryData.publicId}</div>
            </NxOverflowTooltip>
          ) : (
            <NxOverflowTooltip>
              <NxTextLink
                className="nx-truncate-ellipsis"
                data-testid="repositories_configuration-link"
                href={uiRouterState.href('repository-report', { repositoryId: repositoryData.id })}
              >
                {repositoryData.publicId}
              </NxTextLink>
            </NxOverflowTooltip>
          )}
        </NxTable.Cell>
        <NxTable.Cell className="iq-repositories-configuration-table-repository-format">
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">{repositoryData.format}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell className="iq-repositories-configuration-table-repository-type">
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">{repositoryData.repositoryType}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">{getEnablement(repositoryData)}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
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

  const showHighlight = (column) => (sortConfiguration[0].key === column ? sortConfiguration[0].dir : null);

  const renderConfigTableRowsAtRepoContainerLevel = () => {
    if (repoManagerOwnersEntries.length === 0) {
      return [
        <NxTable.Body
          key="empty-repo-manager"
          emptyMessage="There are no repositories registered with the server."
          error={loadError}
          isLoading={isLoading}
          retryHandler={loadRepositories}
        />,
      ];
    }

    return repoManagerOwnersEntries.map((repoManager) => {
      const headerTitle = repoManager.name || repoManager.instanceId;
      return (
        <NxTable.Body
          key={repoManager.instanceId}
          error={loadError}
          isLoading={isLoading}
          retryHandler={loadRepositories}
        >
          <IqCollapsibleRow
            headerTitle={headerTitle}
            noItemsMessage={'There are no repositories registered with the server.'}
            isCollapsible={true}
            colSpan={4}
            rowBtnIcon={faPen}
            rowBtnTitle="Edit"
            rowBtnAction={() =>
              openEditRepositoryManagerNameModal({
                repoManagerId: repoManager.id,
                managerName: repoManager.name,
                managerInstanceId: repoManager.instanceId,
              })
            }
          >
            {repositoriesByManagerInstanceId[repoManager.instanceId]?.map(mapRepositoryToRow)}
          </IqCollapsibleRow>
        </NxTable.Body>
      );
    });
  };

  const renderConfigTableRowsAtRepoManagerLevel = () => {
    return (
      <NxTable.Body
        emptyMessage="There are no repositories registered with the server."
        error={loadError}
        isLoading={isLoading}
        retryHandler={loadRepositoriesByManagerId}
      >
        {repositoriesByManagerInstanceId[owner.instanceId]?.map(mapRepositoryToRow)}
      </NxTable.Body>
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
                id="repository-column-header"
                isSortable
                sortDir={showHighlight('publicId')}
                onClick={() => sortRepositories('publicId')}
              >
                <NxOverflowTooltip>
                  <span className="nx-truncate-ellipsis">Repository</span>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell
                id="repository-format-column-header"
                isSortable
                sortDir={showHighlight('format')}
                onClick={() => sortRepositories('format')}
              >
                <NxOverflowTooltip>
                  <span className="nx-truncate-ellipsis">Format</span>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell
                id="repository-type-column-header"
                isSortable
                sortDir={showHighlight('repositoryType')}
                onClick={() => sortRepositories('repositoryType')}
              >
                <NxOverflowTooltip>
                  <span className="nx-truncate-ellipsis">Type</span>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell id="repository-enablement-column-header">
                <NxOverflowTooltip>
                  <span className="nx-truncate-ellipsis">Enablement</span>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell />
            </NxTable.Row>
            <NxTable.Row isFilterHeader>
              <NxTable.Cell>
                <NxFilterInput
                  placeholder="Repository name"
                  onChange={setRepositoryPublicIdFilter}
                  value={repositoryPublicIdFilter}
                  className="iq-repositories-configuration-table-filter"
                />
              </NxTable.Cell>
              <NxTable.Cell>
                <NxStatefulFilterDropdown
                  placeholder="Format"
                  options={repositoryFormats.map((format) => {
                    return { id: format, displayName: format };
                  })}
                  selectedIds={repositoryFormatsFilter}
                  onChange={setRepositoryFormatsFilter}
                  showReset={true}
                  className="iq-repositories-configuration-table-filter"
                />
              </NxTable.Cell>
              <NxTable.Cell />
              <NxTable.Cell />
              <NxTable.Cell />
            </NxTable.Row>
          </NxTable.Head>
          {isRepositoryManager
            ? renderConfigTableRowsAtRepoManagerLevel()
            : renderConfigTableRowsAtRepoContainerLevel()}
        </NxTable>
        {showDeleteModal && deleteModal}
        {showEditRepositoryManagerNameModal && editRepositoryManagerNameModal}
      </NxTile.Content>
    </NxTile>
  );
};

export default RepositoriesConfigurationTile;
