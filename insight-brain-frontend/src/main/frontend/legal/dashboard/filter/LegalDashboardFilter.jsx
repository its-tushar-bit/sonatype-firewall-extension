/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxErrorAlert, NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../react/LoadWrapper';
import IqPopover from '../../../react/IqPopover';
import IqOrgAppPicker from '../../../components/iqOrgAppPicker/IqOrgAppPicker';
import Hexagon from '../../../react/Hexagon';
import * as PropTypes from 'prop-types';
import { curryN } from 'ramda';
import LegalDashboardFilterFooter from './LegalDashboardFilterFooter';
import ManageFiltersDropdown from '../../../dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';
import { filterToJson } from './legalDashboardFilterService';
import classnames from 'classnames';
import SaveLegalFilterModalContainer from './SaveLegalFilterModalContainer';
import DeleteLegalFilterModalContainer from './DeleteLegalFilterModalContainer';

export default function LegalDashboardFilter(props) {
  const {
    loading,
    loadError,
    loadErrorFilterName,
    applyFilterError,
    showDirtyAsterisk,
    filtersAreDirty,
    showSaveFilterModal,
    savedFilters,
    ownersMap,
    topParentOrganizationId,

    // filter items
    organizations,
    applications,
    categories,
    stages,
    progressOptions,

    // selected items
    appliedFilterName,
    selected,

    // actions
    applyFilter,
    applyFilterCancelled,
    setDisplaySaveFilterModal,
    loadFilter,
    revert,
    toggleFilter,
    toggleAppsAndOrgs,
    applyDefaultFilter,
    applySavedFilter,
    selectFilterToDelete,
    toggleFilterSidebar,
  } = props;

  const curriedToggleFilter = curryN(2, toggleFilter);
  const onCategoriesChange = curriedToggleFilter('categories');
  const onStagesChange = curriedToggleFilter('stages');
  const onProgressOptionsChange = curriedToggleFilter('progressOptions');

  const applicationCategoryTooltip = (prop) => (prop && prop.owner && `in ${prop.owner}`) || '';

  function handleCloseBtnClick() {
    if (filtersAreDirty) {
      return;
    }
    toggleFilterSidebar(false);
  }

  function handleRetry() {
    // Note: no args is passed to loadFilter()
    // loadFilter() action expects resultsType string or null
    // If we pass loadFilter function directly to retryHandler, it will be passed the event object,
    // which will break the logic in loadFilter() action.
    loadFilter();
  }

  const closeFilterBtnTooltip = filtersAreDirty ? 'Please apply or revert filter' : 'Close';

  return (
    <IqPopover onClose={() => toggleFilterSidebar(false)}>
      {showSaveFilterModal && <SaveLegalFilterModalContainer />}
      <IqPopover.Header
        className="legal-dashboard-filter-header"
        buttonId="legal-dashboard-filter-close-btn"
        onClose={handleCloseBtnClick}
        headerSize="h3"
        headerTitle="Filter"
        buttonClassnames={classnames({ disabled: filtersAreDirty })}
        closeTitle={closeFilterBtnTooltip}
      >
        {!loading && !loadError && (
          <ManageFiltersDropdown
            {...{
              appliedFilterName,
              showDirtyAsterisk,
              savedFilters,
              applyDefaultFilter,
              applySavedFilter,
              selectFilterToDelete,
              DeleteFilterModal: DeleteLegalFilterModalContainer,
            }}
          />
        )}
        {loadErrorFilterName && <NxErrorAlert>Failed to load {loadErrorFilterName}</NxErrorAlert>}
      </IqPopover.Header>
      <div className="legal-dashboard-filter nx-viewport-sized__scrollable">
        <LoadWrapper loading={loading} error={loadError} retryHandler={handleRetry}>
          {() => (
            <Fragment>
              <IqOrgAppPicker
                organizations={organizations}
                applications={applications}
                selectedApplications={selected.applications}
                selectedOrganizations={selected.organizations}
                onChange={toggleAppsAndOrgs}
                id="legal-org-app-filters"
                ownersMap={ownersMap}
                topParentOrganizationId={topParentOrganizationId}
              />
              <NxStatefulTreeViewMultiSelect
                options={categories}
                selectedIds={selected.categories}
                onChange={onCategoriesChange}
                optionTooltipGenerator={applicationCategoryTooltip}
                filterPlaceholder="Category"
                name="application categories"
                id="legal-category-filter"
              >
                <Hexagon className="size-16px size-fw outline" />
                <span>Application Categories</span>
              </NxStatefulTreeViewMultiSelect>
              <NxStatefulTreeViewMultiSelect
                options={stages}
                selectedIds={selected.stages}
                onChange={onStagesChange}
                filterPlaceholder="Stage"
                name="stages"
                id="legal-stage-filter"
              >
                Stages
              </NxStatefulTreeViewMultiSelect>
              <NxStatefulTreeViewMultiSelect
                options={progressOptions}
                selectedIds={selected.progressOptions}
                onChange={onProgressOptionsChange}
                filterPlaceholder="Review Progress"
                name="progressOptions"
                id="legal-progress-options-filter"
              >
                Review Progress
              </NxStatefulTreeViewMultiSelect>
            </Fragment>
          )}
        </LoadWrapper>
      </div>

      <IqPopover.Footer>
        <LegalDashboardFilterFooter
          {...{
            applyFilterError,
            filtersAreDirty,
            setDisplaySaveFilterModal,
            revert,
            onApplyCurrentFilter: () => applyFilter(filterToJson(selected), appliedFilterName),
            onCancelApplyFilter: applyFilterCancelled,
          }}
        />
      </IqPopover.Footer>
    </IqPopover>
  );
}

LegalDashboardFilter.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  loadErrorFilterName: PropTypes.string,
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  showAgeFilter: PropTypes.bool,
  showSaveFilterModal: PropTypes.bool,
  organizations: PropTypes.array,
  applications: PropTypes.array,
  categories: PropTypes.array,
  stages: PropTypes.array,
  progressOptions: PropTypes.array,
  selected: PropTypes.shape({
    organizations: PropTypes.instanceOf(Set).isRequired,
    applications: PropTypes.instanceOf(Set).isRequired,
    categories: PropTypes.instanceOf(Set).isRequired,
    stages: PropTypes.instanceOf(Set).isRequired,
    progressOptions: PropTypes.instanceOf(Set).isRequired,
  }),
  applyFilter: PropTypes.func.isRequired,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  loadFilter: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  selectAge: PropTypes.func,
  toggleAppsAndOrgs: PropTypes.func,
  toggleFilter: PropTypes.func,
  ...ManageFiltersDropdown.propTypes,
  ownersMap: PropTypes.object,
  topParentOrganizationId: PropTypes.string,
};
