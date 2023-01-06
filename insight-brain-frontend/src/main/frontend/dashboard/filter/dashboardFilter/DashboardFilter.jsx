/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { map, curryN } from 'ramda';
import classnames from 'classnames';
import {
  NxErrorAlert,
  NxStatefulTreeViewMultiSelect,
  NxStatefulTreeViewRadioSelect,
  NxStatefulCollapsibleMultiSelect,
} from '@sonatype/react-shared-components';

import IqOrgAppPicker from '../../../components/iqOrgAppPicker/IqOrgAppPicker';
import IqTreeViewPolicyThreatSlider from '../../../react/IqTreeViewPolicyThreatSlider';
import IqPopover from '../../../react/IqPopover';
import LoadWrapper from '../../../react/LoadWrapper';
import Hexagon from '../../../react/Hexagon';
import { filterToJson } from '../dashboardFilterService';

import DashboardFilterFooter from './DashboardFilterFooter';
import SaveFilterModalContainer from '../saveFilterModal/SaveFilterModalContainer';
import ManageFiltersDropdown from '../manageFiltersDropdown/ManageFiltersDropdown';
import DeleteFilterModalContainer from '../deleteFilterModal/DeleteFilterModalContainer';

export default function DashboardFilter(props) {
  const {
    loading,
    loadError,
    loadErrorFilterName,
    applyFilterError,
    showDirtyAsterisk,
    filtersAreDirty,
    needsAcknowledgement,
    showAgeFilter,
    showStagesFilter,
    showViolationStateFilter,
    showExpirationDateFilter,
    showRepositoriesFilter,
    showSaveFilterModal,
    savedFilters,

    // filter items
    organizations,
    applications,
    categories,
    stages,
    ages,
    policyTypes,
    expirationDates,
    policyViolationStates,
    repositories,

    // selected items
    appliedFilterName,
    selected,

    // actions
    applyFilter,
    applyFilterCancelled,
    setDisplaySaveFilterModal,
    loadFilter,
    revert,
    selectAge,
    selectExpirationDate: onExpirationDatesChange,
    toggleFilter,
    toggleAppsAndOrgs,
    applyDefaultFilter,
    applySavedFilter,
    selectFilterToDelete,
    toggleFilterSidebar,
  } = props;

  const curriedToggleFilter = curryN(2, toggleFilter),
    onCategoriesChange = curriedToggleFilter('categories'),
    onStagesChange = curriedToggleFilter('stages'),
    onRepositoriesChange = curriedToggleFilter('repositories'),
    onPolicyTypesChange = curriedToggleFilter('policyTypes'),
    onPolicyViolationStatesChange = curriedToggleFilter('policyViolationStates'),
    onPolicyThreatChange = curriedToggleFilter('policyThreatLevels');

  /**
   * IQ uses numbers for the age filter but `NxTreeViewRadioSelect` does not
   * so we need to parse them into string when first receiving them,
   * and parse them back to number when applying the filter.
   */
  const stringifyNullableAgeOption = ({ id, ...rest }) => ({
      ...rest,
      id: id ? id.toString() : null,
    }),
    stringifiedAges = showAgeFilter ? map(stringifyNullableAgeOption, ages) : [],
    stringifiedSelectedAge = selected.maxDaysOld ? selected.maxDaysOld.toString() : selected.maxDaysOld;

  function onAgeChange(selectedAge) {
    const ageAsNumber = selectedAge ? parseInt(selectedAge, 10) : null;
    selectAge(ageAsNumber);
  }

  function handleCloseBtnClick() {
    if (filtersAreDirty || needsAcknowledgement) {
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

  const applicationCategoryTooltip = (prop) => (prop && prop.owner && `in ${prop.owner}`) || '';

  const closeFilterBtnTooltip = needsAcknowledgement
    ? 'Please apply a filter'
    : filtersAreDirty
    ? 'Please apply or revert filter'
    : 'Close';

  return (
    <IqPopover id="dashboard-filter-container" onClose={() => toggleFilterSidebar(false)}>
      {showSaveFilterModal && <SaveFilterModalContainer />}
      <IqPopover.Header
        className="dashboard-filter-header"
        id="dashboard-filter-header"
        headerSize="h3"
        headerTitle="Filter"
        buttonId="dashboard-filter-close-btn"
        onClose={handleCloseBtnClick}
        buttonClassnames={classnames({
          disabled: filtersAreDirty || needsAcknowledgement,
        })}
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
              DeleteFilterModal: DeleteFilterModalContainer,
            }}
          />
        )}
        {loadErrorFilterName && <NxErrorAlert>Failed to load {loadErrorFilterName}</NxErrorAlert>}
      </IqPopover.Header>

      <div className="dashboard-filter">
        <LoadWrapper loading={loading} error={loadError} retryHandler={handleRetry}>
          {() => (
            <Fragment>
              <IqOrgAppPicker
                organizations={organizations}
                applications={applications}
                selectedApplications={selected.applications}
                selectedOrganizations={selected.organizations}
                onChange={toggleAppsAndOrgs}
                id="org-app-filters"
              />
              {showRepositoriesFilter && (
                <NxStatefulCollapsibleMultiSelect
                  options={repositories}
                  optionTooltipGenerator={(repository) => repository.fullName}
                  selectedIds={selected.repositories}
                  onChange={onRepositoriesChange}
                  filterThreshold={3}
                  filterPlaceholder="Repository Name"
                  name="repositories"
                  id="repositories-filter"
                >
                  <span>Repositories</span>
                </NxStatefulCollapsibleMultiSelect>
              )}
              <NxStatefulTreeViewMultiSelect
                options={categories}
                selectedIds={selected.categories}
                onChange={onCategoriesChange}
                optionTooltipGenerator={applicationCategoryTooltip}
                filterPlaceholder="Category"
                name="application categories"
                id="category-filter"
              >
                <Hexagon className="size-16px size-fw outline" />
                <span>Application Categories</span>
              </NxStatefulTreeViewMultiSelect>
              {showStagesFilter && (
                <NxStatefulTreeViewMultiSelect
                  options={stages}
                  selectedIds={selected.stages}
                  onChange={onStagesChange}
                  filterPlaceholder="Stage"
                  name="stages"
                  id="stage-filter"
                >
                  <span>Stages</span>
                </NxStatefulTreeViewMultiSelect>
              )}
              <NxStatefulTreeViewMultiSelect
                options={policyTypes}
                selectedIds={selected.policyTypes}
                onChange={onPolicyTypesChange}
                filterPlaceholder="Policy Type"
                name="policy types"
                id="policy-type-filter"
              >
                <span>Policy Types</span>
              </NxStatefulTreeViewMultiSelect>
              {showViolationStateFilter && (
                <NxStatefulTreeViewMultiSelect
                  options={policyViolationStates}
                  selectedIds={selected.policyViolationStates}
                  onChange={onPolicyViolationStatesChange}
                  filterPlaceholder="Violation State"
                  name="violation states"
                  id="policy-violation-state-filter"
                >
                  <span>Violation State</span>
                </NxStatefulTreeViewMultiSelect>
              )}
              {showExpirationDateFilter && (
                <NxStatefulTreeViewRadioSelect
                  id="expiration-date-filter"
                  options={expirationDates}
                  name="expiration date"
                  onChange={onExpirationDatesChange}
                  selectedId={selected.expirationDate}
                >
                  <span>Expiration Date</span>
                </NxStatefulTreeViewRadioSelect>
              )}
              {showAgeFilter && (
                <NxStatefulTreeViewRadioSelect
                  id="age-filter"
                  options={stringifiedAges}
                  name="Age Filter"
                  onChange={onAgeChange}
                  selectedId={stringifiedSelectedAge}
                >
                  <span>Age</span>
                </NxStatefulTreeViewRadioSelect>
              )}
              <IqTreeViewPolicyThreatSlider
                id="threat-level-filter"
                value={selected.policyThreatLevels}
                onChange={onPolicyThreatChange}
              >
                <span>Policy Threat Level</span>
              </IqTreeViewPolicyThreatSlider>
            </Fragment>
          )}
        </LoadWrapper>
      </div>

      <IqPopover.Footer className="dashboard-filter-footer">
        <DashboardFilterFooter
          {...{
            applyFilterError,
            filtersAreDirty,
            needsAcknowledgement,
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

export const dashboardFilterPropTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  loadErrorFilterName: PropTypes.string,
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  needsAcknowledgement: PropTypes.bool,
  showAgeFilter: PropTypes.bool,
  showSaveFilterModal: PropTypes.bool,
  showStagesFilter: PropTypes.bool,
  showViolationStateFilter: PropTypes.bool,
  showExpirationDateFilter: PropTypes.bool,
  showRepositoriesFilter: PropTypes.bool,
  selectExpirationDate: PropTypes.func,
  expirationDates: PropTypes.array,
  organizations: PropTypes.array,
  applications: PropTypes.array,
  categories: PropTypes.array,
  stages: PropTypes.array,
  repositories: PropTypes.array,
  ages: PropTypes.array,
  policyTypes: PropTypes.array,
  policyViolationStates: PropTypes.array,
  selected: PropTypes.shape({
    organizations: PropTypes.instanceOf(Set).isRequired,
    applications: PropTypes.instanceOf(Set).isRequired,
    categories: PropTypes.instanceOf(Set).isRequired,
    stages: PropTypes.instanceOf(Set).isRequired,
    policyTypes: PropTypes.instanceOf(Set).isRequired,
    policyViolationStates: PropTypes.instanceOf(Set).isRequired,
    maxDaysOld: PropTypes.number,
    policyThreatLevels: PropTypes.arrayOf(PropTypes.number).isRequired,
    expirationDate: PropTypes.string.isRequired,
  }),
  applyFilter: PropTypes.func.isRequired,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  loadFilter: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  selectAge: PropTypes.func,
  toggleAppsAndOrgs: PropTypes.func,
  toggleFilter: PropTypes.func,
  toggleFilterSidebar: PropTypes.func,
  ...ManageFiltersDropdown.propTypes,
};

DashboardFilter.propTypes = {
  ...dashboardFilterPropTypes,
};
