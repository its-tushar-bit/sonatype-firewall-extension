/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import * as PropTypes from 'prop-types';
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
import { faCopy, faPen, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { selectHasEditIqPermission } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import AddProxyRepositoryModal from 'MainRoot/firewall/iqProxy/AddProxyRepositoryModal';
import { isPccsEligible } from 'MainRoot/firewall/iqProxy/proxyRepositoryFormats';
import { actions, VIEW_TYPES } from './repositoriesConfigurationSlice';
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
import {
  selectNonVirtualRepoManagerOwnersEntriesSorted,
  selectVirtualRepoManagerOwnersEntriesSorted,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsRepositoryManager,
  selectIsVirtualRepositoryContainer,
  selectPrevStateIsRepositorySection,
} from 'MainRoot/reduxUiRouter/routerSelectors';

const RepositoriesConfigurationTile = ({ virtualOnly = false, showHostedRepoLink = false }) => {
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
  const goToRepositorySummaryView = (repositoryId) => dispatch(actions.goToRepositorySummaryView(repositoryId));

  const repositoriesByManagerInstanceId = useSelector(selectRepositoriesByManagerInstanceId);
  const nonVirtualRepoManagerOwnersEntries = useSelector(selectNonVirtualRepoManagerOwnersEntriesSorted);
  const virtualRepoManagerOwnersEntries = useSelector(selectVirtualRepoManagerOwnersEntriesSorted);
  const repoManagerOwnersEntries = virtualOnly ? virtualRepoManagerOwnersEntries : nonVirtualRepoManagerOwnersEntries;
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
  const prevStateIsRepositorySection = useSelector(selectPrevStateIsRepositorySection);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);

  const [isAddProxyModalOpen, setIsAddProxyModalOpen] = useState(false);

  const isVirtualRepositoryContainerView = useSelector(selectIsVirtualRepositoryContainer);
  // True when viewing a single Virtual Repository Manager's detail page.
  const isVirtualRepositoryManagerView = isRepositoryManager && owner?.managerType === 'virtual';
  // Applies whenever the tile is presenting proxy repositories owned by a Virtual Repository
  // Manager — either a single VRM's detail page or the top-level Virtual Repository Managers
  // container view (which lists every VRM's proxy repos).
  const isProxyRepositoriesView = isVirtualRepositoryManagerView || isVirtualRepositoryContainerView;

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

  const validateRepositoryName = (name) => {
    let regex = /^[a-zA-Z0-9._-]+(?: [a-zA-Z0-9._-]+)*$/;
    if (name.length === 0) return 'Must be non-empty';
    if (name.length > 200) return 'Repository name must be 200 characters or less.';
    if (regex.test(name)) {
      return null;
    } else {
      return 'Not a valid Repository Name';
    }
  };

  const prevOwnerIdRef = useRef(owner?.id);
  const prevIsRepositoryManagerRef = useRef(isRepositoryManager);
  // Captured as a ref so its value at effect execution time is used without
  // adding it to the dependency array (which would cause spurious reloads
  // whenever router prevState changes mid-session).
  const prevStateIsRepositorySectionRef = useRef(prevStateIsRepositorySection);
  prevStateIsRepositorySectionRef.current = prevStateIsRepositorySection;

  useEffect(() => {
    const viewType = isRepositoryManager ? VIEW_TYPES.MANAGER : VIEW_TYPES.CONTAINER;
    const prevOwnerId = prevOwnerIdRef.current;
    const ownerChanged = prevOwnerId !== owner?.id;
    // Skip reset when owner transitions undefined → defined: that is an async
    // data-load, not a navigation event, and should not clear active filters.
    const ownerJustLoaded = prevOwnerId === undefined && owner?.id !== undefined;

    // Reset filters when navigating to a different container/manager (owner changed),
    // or when arriving from outside the repository section entirely (e.g. Dashboard).
    // Do NOT reset when navigating within the section (e.g. container -> repository -> back),
    // so the filter is preserved when the user drills into a repo and returns.
    // ownerJustLoaded only suppresses the ownerChanged branch — not the !prevStateIsRepositorySection
    // branch, so cold deep-links while owner loads async still clear stale cross-session filters.
    if ((!ownerJustLoaded && ownerChanged) || !prevStateIsRepositorySectionRef.current) {
      const prevViewType = prevIsRepositoryManagerRef.current ? VIEW_TYPES.MANAGER : VIEW_TYPES.CONTAINER;
      dispatch(actions.resetViewFilters(prevViewType));
    }
    prevOwnerIdRef.current = owner?.id;
    prevIsRepositoryManagerRef.current = isRepositoryManager;

    dispatch(actions.setCurrentView(viewType));
    if (isRepositoryManager) {
      loadRepositoriesByManagerId();
    } else {
      loadRepositories();
    }
  }, [isRepositoryManager, owner?.id]);

  // When the user navigates away to a non-repository section without changing owner or view
  // type (so the main effect's deps don't change and its cleanup never fires), we still need
  // to reset the active filters so they don't persist stale across that navigation.
  // Watching prevStateIsRepositorySection going false is the signal for that case.
  const currentViewTypeRef = useRef(isRepositoryManager ? VIEW_TYPES.MANAGER : VIEW_TYPES.CONTAINER);
  currentViewTypeRef.current = isRepositoryManager ? VIEW_TYPES.MANAGER : VIEW_TYPES.CONTAINER;
  useEffect(() => {
    if (!prevStateIsRepositorySection) {
      dispatch(actions.resetViewFilters(currentViewTypeRef.current));
    }
  }, [prevStateIsRepositorySection]);

  const onCloseAddProxyModal = (created) => {
    setIsAddProxyModalOpen(false);
    if (created) {
      loadRepositoriesByManagerId();
    }
  };

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
          <NxH2>{virtualOnly ? 'Edit Virtual Repository Manager' : 'Edit Repository Manager'}</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxReadOnly>
            <NxReadOnly.Label>
              {virtualOnly ? 'Virtual Repository Manager ID' : 'Repository Manager ID'}
            </NxReadOnly.Label>
            <NxReadOnly.Data>{editRepositoryManagerNameModalInfo.managerInstanceId}</NxReadOnly.Data>
          </NxReadOnly>
          <NxFormGroup label={virtualOnly ? 'Virtual Repository Manager Name' : 'Repository Manager Name'}>
            <NxStatefulTextInput
              validator={(value) => validateRepositoryName(value)}
              defaultValue={editRepositoryManagerNameModalInfo.managerName || ''}
              onChange={(value) => setRepositoryManagerName(value.trim())}
            />
          </NxFormGroup>
          <NxP>
            {virtualOnly
              ? 'Any changes made will apply to all proxy repositories for this virtual repository manager.'
              : 'Any changes made will apply to all repositories for this repository manager.'}
          </NxP>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );

  const mapRepositoryToRow = (managerId, isVirtualManager) =>
    function RepositoryRow(repository) {
      const repositoryData = repository.repository;
      const copyProxyUrl = () => {
        navigator.clipboard.writeText(repository.proxyUrl);
      };
      return (
        <NxTable.Row key={repositoryData.id}>
          <NxTable.Cell className="iq-repositories-configuration-table-repository">
            {repositoryData.repositoryType === 'hosted' ? (
              <NxOverflowTooltip>
                {showHostedRepoLink && owner?.instanceId ? (
                  <NxTextLink
                    className="nx-truncate-ellipsis"
                    data-testid="repositories_configuration-hosted-link"
                    href={uiRouterState.href('hostedRepoComponents', {
                      repositoryId: repositoryData.id,
                      repositoryManagerId: owner.instanceId,
                      repositoryPublicId: repositoryData.publicId,
                    })}
                  >
                    {repositoryData.publicId}
                  </NxTextLink>
                ) : (
                  <div className="nx-truncate-ellipsis">{repositoryData.publicId}</div>
                )}
              </NxOverflowTooltip>
            ) : (
              <NxOverflowTooltip>
                <NxTextLink
                  className="nx-truncate-ellipsis"
                  data-testid="repositories_configuration-link"
                  href={
                    repositoryData.format === 'docker'
                      ? uiRouterState.href('firewall.containerRepositoryResults', { repositoryId: repositoryData.id })
                      : uiRouterState.href('firewall.repository-report', { repositoryId: repositoryData.id })
                  }
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
          {!isProxyRepositoriesView && (
            <NxTable.Cell className="iq-repositories-configuration-table-repository-type">
              <NxOverflowTooltip>
                <div className="nx-truncate-ellipsis">{repositoryData.repositoryType}</div>
              </NxOverflowTooltip>
            </NxTable.Cell>
          )}
          <NxTable.Cell>
            <NxOverflowTooltip>
              <div className="nx-truncate-ellipsis">{getEnablement(repositoryData)}</div>
            </NxOverflowTooltip>
          </NxTable.Cell>
          {isProxyRepositoriesView && (
            <NxTable.Cell>
              <span
                className={`iq-repositories-configuration-table__pccs-badge ${getPccsBadge(repositoryData).className}`}
              >
                {getPccsBadge(repositoryData).label}
              </span>
            </NxTable.Cell>
          )}
          <NxTable.Cell>
            <div className="nx-btn-bar">
              <NxButton
                data-testid="repository-copy-url-button"
                variant="icon-only"
                title="Copy Proxy URL"
                onClick={copyProxyUrl}
                className={
                  isVirtualManager && repositoryData.repositoryType === 'proxy'
                    ? undefined
                    : 'iq-copy-url-button--hidden'
                }
                aria-hidden={isVirtualManager && repositoryData.repositoryType === 'proxy' ? undefined : true}
                tabIndex={isVirtualManager && repositoryData.repositoryType === 'proxy' ? undefined : -1}
              >
                <NxFontAwesomeIcon icon={faCopy} />
              </NxButton>
              <NxButton
                data-testid="repository-edit-button"
                variant="icon-only"
                title="Edit"
                onClick={() => goToRepositorySummaryView(repositoryData.id)}
              >
                <NxFontAwesomeIcon icon={faPen} />
              </NxButton>
            </div>
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
            {repositoriesByManagerInstanceId[repoManager.instanceId]?.map(
              mapRepositoryToRow(repoManager.instanceId, repoManager.managerType === 'virtual')
            )}
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
        {repositoriesByManagerInstanceId[owner.instanceId]?.map(
          mapRepositoryToRow(owner?.instanceId, owner.managerType === 'virtual')
        )}
      </NxTable.Body>
    );
  };

  const tileTitle = isProxyRepositoriesView ? 'Proxy Repositories' : 'Configuration';

  return (
    <NxTile id="repositories-pill-configuration" data-testid="repositories_configuration">
      <NxTile.Header>
        <NxTile.Headings>
          <NxTile.HeaderTitle>
            <NxH2>{tileTitle}</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Headings>
        {isVirtualRepositoryManagerView && hasEditIqPermission && (
          <NxTile.HeaderActions>
            <NxButton
              variant="tertiary"
              type="button"
              onClick={() => setIsAddProxyModalOpen(true)}
              data-testid="add-proxy-repository-button"
            >
              + Add Proxy Repository
            </NxButton>
          </NxTile.HeaderActions>
        )}
      </NxTile.Header>
      <NxTile.Content>
        <NxTable
          id="iq-repositories-configuration-table"
          className={isProxyRepositoriesView ? 'iq-repositories-configuration-table--with-pccs' : undefined}
        >
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
              {!isProxyRepositoriesView && (
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
              )}
              <NxTable.Cell id="repository-enablement-column-header">
                <NxOverflowTooltip>
                  <span className="nx-truncate-ellipsis">Enablement</span>
                </NxOverflowTooltip>
              </NxTable.Cell>
              {isProxyRepositoriesView && (
                <NxTable.Cell id="repository-pccs-column-header">
                  <NxOverflowTooltip>
                    <span className="nx-truncate-ellipsis">PCCS</span>
                  </NxOverflowTooltip>
                </NxTable.Cell>
              )}
              <NxTable.Cell />
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
      {isAddProxyModalOpen && <AddProxyRepositoryModal managerId={owner?.id} onClose={onCloseAddProxyModal} />}
    </NxTile>
  );
};

RepositoriesConfigurationTile.propTypes = {
  virtualOnly: PropTypes.bool,
  showHostedRepoLink: PropTypes.bool,
};

function getPccsBadge(repository) {
  if (!isPccsEligible(repository?.format)) {
    return { label: 'N/A', className: 'iq-repositories-configuration-table__pccs-badge--na' };
  }
  const pccsEnabled = repository?.policyCompliantComponentSelectionEnabled ?? repository?.pccsEnabled;
  return pccsEnabled
    ? { label: 'On', className: 'iq-repositories-configuration-table__pccs-badge--on' }
    : { label: 'Off', className: 'iq-repositories-configuration-table__pccs-badge--off' };
}

export default RepositoriesConfigurationTile;
