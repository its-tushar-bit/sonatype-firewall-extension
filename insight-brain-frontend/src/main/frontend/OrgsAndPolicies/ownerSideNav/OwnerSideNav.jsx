/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';
import { isEmpty } from 'ramda';

import {
  NxP,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxCollapsibleItems,
  NxFontAwesomeIcon,
  NxFilterInput,
  NxLoadingSpinner,
  NxButton,
  NxStatefulIconDropdown,
} from '@sonatype/react-shared-components';
import { faPlus, faFolderTree } from '@fortawesome/pro-solid-svg-icons';

import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import OwnerModal from 'MainRoot/OrgsAndPolicies/ownerModal/OwnerModal';
import { actions as ownerModalActions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { actions } from './ownerSideNavSlice';
import { selectOwnerSideNavSlice, selectIsOrganizationTopOfHierarchyForUser } from './ownerSideNavSelectors';
import { selectLoading as selectOwnerSummaryLoading } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import Application from './Application';
import Organization from './Organization';
import RepositoryManager from './RepositoryManager';
import Repository from './Repository';
import {
  selectIsRootOrganization,
  selectIsOrganization,
  selectIsApplication,
  selectApplicationId,
  selectIsManagementViewRouterState,
  selectIncludesManagementView,
  selectIsRepositoriesRelated,
  selectIsRepositoryContainer,
  selectIsRepositoryManager,
  selectIsRepository,
  selectRepositoryId,
  selectRouterCurrentParams,
  selectIsSbomManager,
  selectIsStandaloneFirewall,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsScmEnabled,
  selectIsOrgsAndAppsEnabled,
  selectIsFirewallSupported,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectRepositoriesLength } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import { getOwnerInfo } from './utils';

const getId = (repositoryOrId) => {
  if (typeof repositoryOrId === 'string') return repositoryOrId;
  return repositoryOrId.id;
};

export default function OwnerSideNav() {
  const dispatch = useDispatch();

  const {
    loading,
    loadError,
    displayedOrganization,
    toggleOrganizationsCheck,
    toggleApplicationsCheck,
    toggleRepositoryManagersCheck,
    toggleRepositoriesCheck,
    filterQuery,
    filteredEntries,
    filterLoading,
    ownersMap,
  } = useSelector(selectOwnerSideNavSlice);
  const loadingOwnerSummary = useSelector(selectOwnerSummaryLoading);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isRootOrganization = useSelector(selectIsRootOrganization);
  const isOrganizationTopOfHierarchyForUser = useSelector(selectIsOrganizationTopOfHierarchyForUser);
  const isOrganization = useSelector(selectIsOrganization);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isRepository = useSelector(selectIsRepository);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isRepositoryContainer = useSelector(selectIsRepositoryContainer);
  const isApplication = useSelector(selectIsApplication);
  const repositoriesCounter = useSelector(selectRepositoriesLength);
  const showRepositoriesLink =
    repositoriesCounter && isOrganizationTopOfHierarchyForUser && !isRepositoriesRelated && !isSbomManager;
  const selectedApplicationId = useSelector(selectApplicationId);
  const selectedRepositoryId = useSelector(selectRepositoryId);
  const isManagementViewRoute = useSelector(selectIsManagementViewRouterState);
  const isSummaryPage = useSelector(selectIncludesManagementView);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const error = loadError || noSbomManagerEnabledError;

  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isFirewallSupported = useSelector(selectIsFirewallSupported);
  const isScmEnabled = useSelector(selectIsScmEnabled);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);

  const uiRouterState = useRouterState();
  const goToRepositoriesUrl = uiRouterState.href('management.view.repository_container', {
    repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
  });

  const treeViewPageHref = uiRouterState.href(`${isSbomManager ? 'sbomManager.' : ''}management.tree`);

  const onSearch = (query) => dispatch(actions.filterSidebarEntries(query));
  const openOwnerEditorModal = (isApp) => dispatch(ownerModalActions.openModal({ isApp }));

  const onToggleOrganizationsCollapse = () => {
    dispatch(actions.toggleOrganizationsCollapse());
  };
  const onToggleApplicationsCollapse = () => {
    dispatch(actions.toggleApplicationsCollapse());
  };
  const onToggleRepositoryManagersCollapse = () => {
    dispatch(actions.toggleRepositoryManagersCollapse());
  };
  const onToggleRepositoriesCollapse = () => {
    dispatch(actions.toggleRepositoresCollapse());
  };

  const load = () => {
    dispatch(actions.load());
  };

  // in addition to initial loading -> handles one particular case: when user clicks
  // Orgs and Policies icon on global sidebar being on org/app summary page
  // (component does not unmount thus load is not triggered)
  useEffect(() => {
    if (isSummaryPage || isManagementViewRoute) {
      load();
    }
  }, [isSummaryPage, isManagementViewRoute, routerCurrentParams]);

  const renderParentOrganizationItem = (displayedOrganization) => {
    const orgClassnames = classnames('iq-navbar-item iq-selected-org', {
      active: isOrganization || isRepositoryContainer || isRepositoryManager,
    });

    const [, routeParams] = getOwnerInfo(displayedOrganization);
    const organizationUrl = uiRouterState.href(
      `${isSbomManager ? 'sbomManager.' : ''}management.view.${displayedOrganization.type}`,
      routeParams
    );

    return (
      <>
        <NxOverflowTooltip>
          <a className={orgClassnames} href={organizationUrl}>
            {displayedOrganization.name}
          </a>
        </NxOverflowTooltip>
        {(isApplication || isRepositoriesRelated) && <div className="iq-selected-org__pseudo-border"></div>}
      </>
    );
  };

  const renderRepositoriesNavigationItem = () => {
    if (!showRepositoriesLink) return null;
    const repositoriesClassnames = classnames('iq-navbar-item iq-repositories-link', {
      active: isRepositoriesRelated,
    });
    return (
      <a className={repositoriesClassnames} href={goToRepositoriesUrl}>
        <div className="iq-owner-name">{ownersMap['REPOSITORY_CONTAINER_ID'].name}</div>
        <div className="iq-children-counter">
          <span>({repositoriesCounter})</span>
        </div>
      </a>
    );
  };

  const renderRepositoryManagers = (owner = {}) => {
    if (!isRepositoryContainer || !owner || !ownersMap) return null;

    const repositoryManagerIds = owner.repositoryManagerIds || [];

    return (
      <NxCollapsibleItems
        role="menu"
        onToggleCollapse={onToggleRepositoryManagersCollapse}
        isOpen={toggleRepositoryManagersCheck}
        id="repository-managers-collapsible"
        triggerContent="Repository Managers"
      >
        {repositoryManagerIds.map((repositoryManagerId) => {
          return (
            <NxCollapsibleItems.Child role="menuitem" key={repositoryManagerId}>
              <RepositoryManager repositoryManagerId={repositoryManagerId} />
            </NxCollapsibleItems.Child>
          );
        })}
      </NxCollapsibleItems>
    );
  };

  const renderFilteredResults = (entries) => {
    if (filterLoading) {
      return <NxLoadingSpinner />;
    }

    if (
      (isRepositoryContainer && isNilOrEmpty(entries.repositoryManagers)) ||
      (isRepositoryManager && isNilOrEmpty(entries.repositories)) ||
      ((isOrganization || isApplication) && isNilOrEmpty(entries.organizations) && isNilOrEmpty(entries.applications))
    ) {
      return <div className="iq-orgs-and-policies-summary-sidebar__filtered-not-found">No Results Found</div>;
    }

    return (
      <>
        {renderFilteredRepositoryManagers(entries.repositoryManagers)}
        {renderFilteredOrganizations(entries.organizations)}
        {renderFilteredApplications(entries.applications)}
        {renderRepositories(entries.repositories, { filtered: true })}
      </>
    );
  };

  const renderFilteredOrganizations = (organizations) => {
    if (isRepositoriesRelated || isEmpty(organizations)) {
      return null;
    }

    return (
      <NxCollapsibleItems
        role="menu"
        onToggleCollapse={onToggleOrganizationsCollapse}
        isOpen={toggleOrganizationsCheck}
        triggerContent="Organizations"
      >
        {organizations.map(({ id }) => (
          <NxCollapsibleItems.Child role="menuitem" key={id}>
            <Organization organizationId={id} displayParentNameInTooltip />
          </NxCollapsibleItems.Child>
        ))}
      </NxCollapsibleItems>
    );
  };

  const renderFilteredApplications = (applications) => {
    if (isRepositoriesRelated || isEmpty(applications)) {
      return null;
    }

    return (
      <NxCollapsibleItems
        role="menu"
        onToggleCollapse={onToggleApplicationsCollapse}
        isOpen={toggleApplicationsCheck}
        triggerContent="Applications"
        id="applications-collapsible"
      >
        {applications.map(({ publicId }) => (
          <NxCollapsibleItems.Child role="menuitem" key={publicId}>
            <Application applicationPublicId={publicId} isFilteredResult />
          </NxCollapsibleItems.Child>
        ))}
      </NxCollapsibleItems>
    );
  };

  const renderRepositories = (repositories = [], options = {}) => {
    if ((!isRepositoryManager && !isRepository) || isRepositoryContainer) return null;

    const { filtered = false } = options;

    return (
      <NxCollapsibleItems
        role="menu"
        onToggleCollapse={onToggleRepositoriesCollapse}
        isOpen={toggleRepositoriesCheck}
        triggerContent="Repositories"
        id="repositories-collapsible"
      >
        {repositories.map((repository) => {
          const repositoryId = getId(repository);
          const repositoryClassNames = filtered ? '' : classnames({ active: repositoryId === selectedRepositoryId });

          return (
            <NxCollapsibleItems.Child role="menuitem" key={repositoryId} className={repositoryClassNames}>
              <Repository repositoryId={repositoryId} />
            </NxCollapsibleItems.Child>
          );
        })}
      </NxCollapsibleItems>
    );
  };

  const renderFilteredRepositoryManagers = (repositoryManagers) => {
    if (!isRepositoryContainer || isEmpty(repositoryManagers)) return null;

    return (
      <NxCollapsibleItems
        role="menu"
        onToggleCollapse={onToggleRepositoryManagersCollapse}
        isOpen={toggleRepositoryManagersCheck}
        id="repository-managers-collapsible"
        triggerContent="Repository Managers"
      >
        {repositoryManagers.map((repositoryManager) => {
          return (
            <NxCollapsibleItems.Child role="menuitem" key={repositoryManager.id}>
              <RepositoryManager repositoryManagerId={repositoryManager.id} />
            </NxCollapsibleItems.Child>
          );
        })}
      </NxCollapsibleItems>
    );
  };

  const renderApplications = (organization) => {
    if (isRootOrganization || isRepositoriesRelated) {
      return null;
    }
    const childApplicationIds = organization.applicationIds ?? [];

    const scmOnboardingHref = uiRouterState.href('scmOnboardingOrg', {
      organizationId: displayedOrganization.id,
    });

    const plusButton = !organization.synthetic ? (
      <NxStatefulIconDropdown icon={faPlus} title="Add Application">
        <button onClick={() => openOwnerEditorModal(true)} className="nx-dropdown-button">
          New Application
        </button>
        {!isSbomManager && isScmEnabled && (
          <a href={scmOnboardingHref} className="nx-dropdown-button">
            Import Applications
          </a>
        )}
      </NxStatefulIconDropdown>
    ) : (
      <></>
    );

    return (
      <NxCollapsibleItems
        key={`${organization.id}/apps`}
        role="menu"
        onToggleCollapse={onToggleApplicationsCollapse}
        isOpen={toggleApplicationsCheck}
        id="applications-collapsible"
        triggerContent="Applications"
        actionContent={plusButton}
      >
        {childApplicationIds.map((applicationPublicId) => {
          const selectedAppClassnames = classnames({
            active: isApplication && selectedApplicationId === applicationPublicId,
          });
          return (
            <NxCollapsibleItems.Child role="menuitem" className={selectedAppClassnames} key={applicationPublicId}>
              <Application applicationPublicId={applicationPublicId} />
            </NxCollapsibleItems.Child>
          );
        })}
      </NxCollapsibleItems>
    );
  };

  const renderOrganizations = (organization) => {
    if (isRepositoriesRelated) return null;

    const childOrganizationIds = organization.organizationIds ?? [];

    const plusButton = !organization.synthetic ? (
      <NxButton
        data-testid="organizations-add"
        variant="icon-only"
        title="Add New Organization"
        onClick={() => openOwnerEditorModal(false)}
      >
        <NxFontAwesomeIcon icon={faPlus} />
      </NxButton>
    ) : (
      <></>
    );

    return (
      <NxCollapsibleItems
        key={`${organization.id}/orgs`}
        role="menu"
        onToggleCollapse={onToggleOrganizationsCollapse}
        isOpen={toggleOrganizationsCheck}
        id="organizations-collapsible"
        triggerContent="Organizations"
        actionContent={plusButton}
      >
        {childOrganizationIds.map((organizationId) => {
          return (
            <NxCollapsibleItems.Child role="menuitem" key={organizationId}>
              <Organization organizationId={organizationId} />
            </NxCollapsibleItems.Child>
          );
        })}
      </NxCollapsibleItems>
    );
  };

  const filterActive = filterQuery.value.length >= 3;

  return (
    <>
      <MenuBarStatefulBreadcrumb />
      <NxLoadWrapper
        loading={loading || loadingOwnerSummary || !displayedOrganization}
        error={error}
        retryHandler={load}
      >
        {() => {
          return (
            <>
              <header className="iq-orgs-and-policies-summary-sidebar__header" data-testid="sidebar-header">
                {isOrgsAndAppsEnabled && !isStandaloneFirewall && (
                  <NxFilterInput
                    searchIcon
                    id="owner-sidebar-filter"
                    placeholder="Org or App Name"
                    value={filterQuery.value}
                    onChange={onSearch}
                  />
                )}
                {filterQuery.validationErrors ? (
                  <NxP className="iq-orgs-and-policies-summary-sidebar__filter-warning">
                    {filterQuery.validationErrors}
                  </NxP>
                ) : null}
                {filterActive ? (
                  <p className="iq-orgs-and-policies-summary-sidebar__filtered-header">Filtered Results:</p>
                ) : (
                  renderParentOrganizationItem(displayedOrganization)
                )}
              </header>
              <nav className="nx-viewport-sized__scrollable iq-orgs-and-policies-summary-sidebar__content">
                {filterActive ? (
                  renderFilteredResults(filteredEntries)
                ) : (
                  <>
                    {renderRepositoriesNavigationItem()}
                    {isFirewallSupported && !isSbomManager && (
                      <>
                        {renderRepositoryManagers(displayedOrganization)}
                        {renderRepositories(displayedOrganization.repositoryIds)}
                      </>
                    )}
                    {isOrgsAndAppsEnabled && !isStandaloneFirewall && (
                      <>
                        {renderOrganizations(displayedOrganization)}
                        {renderApplications(displayedOrganization)}
                      </>
                    )}
                  </>
                )}
              </nav>
              {isOrgsAndAppsEnabled && !isStandaloneFirewall && (
                <footer className="iq-orgs-and-policies-summary-sidebar__footer">
                  <a href={treeViewPageHref} className="nx-btn nx-btn--tertiary iq-tree-view-button">
                    <NxFontAwesomeIcon icon={faFolderTree} />
                    <span>Tree View</span>
                  </a>
                </footer>
              )}
            </>
          );
        }}
      </NxLoadWrapper>
      <OwnerModal />
    </>
  );
}
