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
import Application from './Application';
import Organization from './Organization';
import {
  selectIsRootOrganization,
  selectIsOrganization,
  selectIsApplication,
  selectApplicationId,
  selectIsManagementViewRouterState,
  selectIncludesManagementView,
  selectIsRepositoriesRelated,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsScmEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectRepositoriesLength } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import { selectIsFirewallOnlyLicense } from 'MainRoot/configuration/license/licenseSelectors';

export default function OwnerSideNav() {
  const dispatch = useDispatch();

  const {
    loading,
    loadError,
    showRepositories,
    displayedOrganization,
    toggleOrganizationsCheck,
    toggleApplicationsCheck,
    filterQuery,
    filteredEntries,
    filterLoading,
  } = useSelector(selectOwnerSideNavSlice);
  const isRootOrganization = useSelector(selectIsRootOrganization);
  const isOrganizationTopOfHierarchyForUser = useSelector(selectIsOrganizationTopOfHierarchyForUser);
  const isOrganization = useSelector(selectIsOrganization);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isApplication = useSelector(selectIsApplication);
  const showRepositoriesLink = showRepositories && (isOrganizationTopOfHierarchyForUser || isRepositoriesRelated);
  const selectedApplicationId = useSelector(selectApplicationId);
  const isManagementViewRoute = useSelector(selectIsManagementViewRouterState);
  const isSummaryPage = useSelector(selectIncludesManagementView);
  const repositoriesCounter = useSelector(selectRepositoriesLength);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isScmEnabled = useSelector(selectIsScmEnabled);

  const uiRouterState = useRouterState();
  const goToRepositoriesUrl = uiRouterState.href('management.view.repository_container');
  const treeViewPageHref = uiRouterState.href('management.tree');

  const onSearch = (query) => dispatch(actions.filterSidebarEntries(query));
  const openOwnerEditorModal = (isApp) => dispatch(ownerModalActions.openModal({ isApp }));

  const onToggleOrganizationsCollapse = () => {
    dispatch(actions.toggleOrganizationsCollapse());
  };
  const onToggleApplicationsCollapse = () => {
    dispatch(actions.toggleApplicationsCollapse());
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
  }, [isSummaryPage, isManagementViewRoute]);

  const renderParentOrganizationItem = (displayedOrganization) => {
    const orgClassnames = classnames('iq-navbar-item iq-selected-org', {
      active: isOrganization,
    });

    const organizationUrl = uiRouterState.href('management.view.organization', {
      organizationId: displayedOrganization.id,
    });

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
        <div className="iq-owner-name">Repositories</div>
        <div className="iq-children-counter">
          <span>({repositoriesCounter})</span>
        </div>
      </a>
    );
  };

  const renderFilteredResults = (entries) => {
    if (filterLoading) {
      return <NxLoadingSpinner />;
    }

    if (isNilOrEmpty(entries.organizations) && isNilOrEmpty(entries.applications)) {
      return <div className="iq-orgs-and-policies-summary-sidebar__filtered-not-found">No Results Found</div>;
    }

    return (
      <>
        {renderFilteredOrganizations(entries.organizations)}
        {renderFilteredApplications(entries.applications)}
      </>
    );
  };

  const renderFilteredOrganizations = (organizations) => {
    if (isEmpty(organizations)) {
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
    if (isEmpty(applications)) {
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
        {isScmEnabled && (
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
  const shouldShowFilter = !isFirewallOnlyLicense;
  const shouldShowOrgsAndApps = !isFirewallOnlyLicense;
  const shouldShowTreeView = !isFirewallOnlyLicense;

  return (
    <>
      <MenuBarStatefulBreadcrumb />
      <NxLoadWrapper loading={loading || !displayedOrganization} error={loadError} retryHandler={load}>
        {() => {
          return (
            <>
              <header className="iq-orgs-and-policies-summary-sidebar__header" data-testid="sidebar-header">
                {shouldShowFilter && (
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
                    {shouldShowOrgsAndApps && (
                      <>
                        {renderOrganizations(displayedOrganization)}
                        {renderApplications(displayedOrganization)}
                      </>
                    )}
                  </>
                )}
              </nav>
              {shouldShowTreeView && (
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
