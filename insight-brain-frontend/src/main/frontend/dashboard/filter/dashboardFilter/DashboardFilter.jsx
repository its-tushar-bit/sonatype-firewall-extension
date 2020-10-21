/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { map, curryN } from 'ramda';
import {
  NxErrorAlert,
  NxStatefulTreeViewMultiSelect,
  NxStatefulTreeViewRadioSelect
} from '@sonatype/react-shared-components';

import IqOrgAppPicker from '../../../components/iqOrgAppPicker/IqOrgAppPicker';
import IqTreeViewPolicyThreatSlider from '../../../react/IqTreeViewPolicyThreatSlider';
import LoadWrapper from '../../../react/LoadWrapper';
import Hexagon from '../../../react/Hexagon';
import { filterToJson } from '../dashboardFilterService';

import DashboardFilterFooter from './DashboardFilterFooter';
import SaveFilterModalContainer from '../saveFilterModal/SaveFilterModalContainer';
import ManageFiltersDropdown from '../manageFiltersDropdown/ManageFiltersDropdown';
import DeleteFilterModalContainer from '../deleteFilterModal/DeleteFilterModalContainer';
import TopModalRenderer from '../../../react/TopModalRenderer';

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
    showSaveFilterModal,
    savedFilters,
    filtersDropdownOpen,
    filterToDelete,

    // filter items
    organizations,
    applications,
    categories,
    stages,
    ages,
    policyTypes,
    policyViolationStates,

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
    toggleFilter,
    toggleAppsAndOrgs,
    applyDefaultFilter,
    applySavedFilter,
    toggleFiltersDropdown,
    selectFilterToDelete,
    handleDocumentClick
  } = props;

  useEffect(() => { loadFilter(); }, []);

  const curriedToggleFilter = curryN(2, toggleFilter),
      onCategoriesChange = curriedToggleFilter('categories'),
      onStagesChange = curriedToggleFilter('stages'),
      onPolicyTypesChange = curriedToggleFilter('policyTypes'),
      onPolicyViolationStatesChange = curriedToggleFilter('policyViolationStates'),
      onPolicyThreatChange = curriedToggleFilter('policyThreatLevels');

  /**
   * IQ uses numbers for the age filter but `NxTreeViewRadioSelect` does not
   * so we need to parse them into string when first receiving them,
   * and parse them back to number when applying the filter.
   */
  const stringifyNullableAgeOption = ({id, ...rest}) => ({ ...rest, id: id ? id.toString() : null }),
      stringifiedAges = showAgeFilter ? map(stringifyNullableAgeOption, ages) : [],
      stringifiedSelectedAge = selected.maxDaysOld ? selected.maxDaysOld.toString() : selected.maxDaysOld;

  function onAgeChange(selectedAge) {
    const ageAsNumber = selectedAge ? parseInt(selectedAge, 10) : null;
    selectAge(ageAsNumber);
  }

  const applicationCategoryTooltip = (prop) => prop && prop.owner && `in ${prop.owner}` || '';

  return (
    <div className="dashboard-filter-container">
      { showSaveFilterModal && <TopModalRenderer><SaveFilterModalContainer/></TopModalRenderer> }
      { filterToDelete && <DeleteFilterModalContainer/> }
      <div className="dashboard-filter-header" id="dashboard-filter-header">
        {/* Not wrapping ManageFiltersDropdown with label to prevent label clicks from triggering dropdown toggle */}
        <label className="nx-label">
          <span className="nx-label__text">Filter</span>
        </label>
        {!loading && !loadError &&
          <ManageFiltersDropdown {...{
            appliedFilterName,
            showDirtyAsterisk,
            savedFilters,
            applyDefaultFilter,
            applySavedFilter,
            filtersDropdownOpen,
            toggleFiltersDropdown,
            selectFilterToDelete,
            handleDocumentClick
          }}/>
        }
        {loadErrorFilterName && <NxErrorAlert>Failed to load {loadErrorFilterName}</NxErrorAlert>}
      </div>

      <div className="dashboard-filter">
        <LoadWrapper loading={loading} error={loadError} retryHandler={loadFilter}>
          {() =>
            <Fragment>
              <IqOrgAppPicker organizations={organizations}
                              applications={applications}
                              selectedApplications={selected.applications}
                              selectedOrganizations={selected.organizations}
                              onChange={toggleAppsAndOrgs}
                              id="org-app-filters"/>

              <NxStatefulTreeViewMultiSelect options={categories}
                                             selectedIds={selected.categories}
                                             onChange={onCategoriesChange}
                                             optionTooltipGenerator={ applicationCategoryTooltip }
                                             filterPlaceholder="Category"
                                             name="application categories"
                                             id="category-filter">
                <Hexagon className="size-16px size-fw outline" /><span>Application Categories</span>
              </NxStatefulTreeViewMultiSelect>

              <NxStatefulTreeViewMultiSelect options={stages}
                                             selectedIds={selected.stages}
                                             onChange={onStagesChange}
                                             filterPlaceholder="Stage"
                                             name="stages"
                                             id="stage-filter">
                <span>Stages</span>
              </NxStatefulTreeViewMultiSelect>

              <NxStatefulTreeViewMultiSelect options={policyTypes}
                                             selectedIds={selected.policyTypes}
                                             onChange={onPolicyTypesChange}
                                             filterPlaceholder="Policy Type"
                                             name="policy types"
                                             id="policy-type-filter">
                <span>Policy Types</span>
              </NxStatefulTreeViewMultiSelect>

              <NxStatefulTreeViewMultiSelect options={policyViolationStates}
                                             selectedIds={selected.policyViolationStates}
                                             onChange={onPolicyViolationStatesChange}
                                             filterPlaceholder="Violation State"
                                             name="violation states"
                                             id="policy-violation-state-filter">
                <span>Violation State</span>
              </NxStatefulTreeViewMultiSelect>

              {
                showAgeFilter &&
                <NxStatefulTreeViewRadioSelect id="age-filter"
                                               options={stringifiedAges}
                                               name="Age Filter"
                                               onChange={onAgeChange}
                                               selectedId={stringifiedSelectedAge}>
                  <span>Age</span>
                </NxStatefulTreeViewRadioSelect>
              }

              <IqTreeViewPolicyThreatSlider id="threat-level-filter"
                                            value={selected.policyThreatLevels}
                                            onChange={onPolicyThreatChange}>
                <span>Policy Threat Level</span>
              </IqTreeViewPolicyThreatSlider>
            </Fragment>
          }
        </LoadWrapper>
      </div>

      <DashboardFilterFooter {...({
        applyFilterError,
        filtersAreDirty,
        needsAcknowledgement,
        setDisplaySaveFilterModal,
        revert,
        onApplyCurrentFilter: () => applyFilter(filterToJson(selected), appliedFilterName),
        onCancelApplyFilter: applyFilterCancelled
      })} />
    </div>
  );
}

DashboardFilter.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  loadErrorFilterName: PropTypes.string,
  applyFilterError: PropTypes.string,
  filtersAreDirty: PropTypes.bool,
  needsAcknowledgement: PropTypes.bool,
  showAgeFilter: PropTypes.bool,
  showSaveFilterModal: PropTypes.bool,
  organizations: PropTypes.array,
  applications: PropTypes.array,
  categories: PropTypes.array,
  stages: PropTypes.array,
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
    policyThreatLevels: PropTypes.arrayOf(PropTypes.number).isRequired
  }),
  applyFilter: PropTypes.func.isRequired,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  loadFilter: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  selectAge: PropTypes.func,
  toggleAppsAndOrgs: PropTypes.func,
  toggleFilter: PropTypes.func,
  ...ManageFiltersDropdown.propTypes
};
