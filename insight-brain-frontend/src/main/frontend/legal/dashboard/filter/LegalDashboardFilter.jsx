/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import { NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../react/LoadWrapper';
import IqOrgAppPicker from '../../../components/iqOrgAppPicker/IqOrgAppPicker';
import Hexagon from '../../../react/Hexagon';
import * as PropTypes from 'prop-types';
import { curryN } from 'ramda';
import LegalDashboardFilterFooter from './LegalDashboardFilterFooter';
import { setToArray } from '../../../util/jsUtil';

export default function LegalDashboardFilter(props) {

  const {
    loading,
    loadError,
    // loadErrorFilterName,
    applyFilterError,
    // showDirtyAsterisk,
    filtersAreDirty,
    needsAcknowledgement,
    // showSaveFilterModal,
    // savedFilters,
    // filtersDropdownOpen,
    // filterToDelete,

    // filter items
    organizations,
    applications,
    categories,
    stages,

    // selected items
    // appliedFilterName,
    selected,

    // actions
    applyFilter,
    // applyFilterCancelled,
    setDisplaySaveFilterModal,
    loadFilter,
    revert,
    toggleFilter,
    toggleAppsAndOrgs
    // applyDefaultFilter,
    // applySavedFilter,
    // toggleFiltersDropdown,
    // selectFilterToDelete,
    // handleDocumentClick
  } = props;

  const filterToJson = (filter) => {
    return {
      organizationFilters: setToArray(filter.organizations),
      applicationFilters: setToArray(filter.applications),
      policyThreatCategoryFilters: setToArray(filter.policyTypes),
      stageTypeFilters: setToArray(filter.stages)
    };
  };

  useEffect(() => { loadFilter(); }, []);

  const curriedToggleFilter = curryN(2, toggleFilter);
  const onCategoriesChange = curriedToggleFilter('categories');
  const onStagesChange = curriedToggleFilter('stages');

  const appliedFilterName = '';
  const applyFilterCancelled = () => {};

  const applicationCategoryTooltip = (prop) => prop && prop.owner && `in ${prop.owner}` || '';

  return (
    <Fragment>
      <div className="dashboard-filter nx-viewport-sized__scrollable">
        <LoadWrapper loading={loading} error={loadError} retryHandler={loadFilter}>
          {() =>
            <Fragment>
              <IqOrgAppPicker organizations={organizations}
                              applications={applications}
                              selectedApplications={selected.applications}
                              selectedOrganizations={selected.organizations}
                              onChange={toggleAppsAndOrgs}
                              id="legal-org-app-filters"/>
              <NxStatefulTreeViewMultiSelect options={categories}
                                             selectedIds={selected.categories}
                                             onChange={onCategoriesChange}
                                             optionTooltipGenerator={ applicationCategoryTooltip }
                                             filterPlaceholder="Category"
                                             name="application categories"
                                             id="legal-category-filter">
                <Hexagon className="size-16px size-fw outline" /><span>Application Categories</span>
              </NxStatefulTreeViewMultiSelect>
              <NxStatefulTreeViewMultiSelect options={stages}
                                             selectedIds={selected.stages}
                                             onChange={onStagesChange}
                                             filterPlaceholder="Stage"
                                             name="stages"
                                             id="legal-stage-filter">
                Stages
              </NxStatefulTreeViewMultiSelect>
            </Fragment>
          }
        </LoadWrapper>
      </div>

      <LegalDashboardFilterFooter {...({
        applyFilterError,
        filtersAreDirty,
        needsAcknowledgement,
        setDisplaySaveFilterModal,
        revert,
        onApplyCurrentFilter: () => applyFilter(filterToJson(selected), appliedFilterName),
        onCancelApplyFilter: applyFilterCancelled
      })} />
    </Fragment>
  );
}

LegalDashboardFilter.propTypes = {
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
  selected: PropTypes.shape({
    organizations: PropTypes.instanceOf(Set).isRequired,
    applications: PropTypes.instanceOf(Set).isRequired,
    categories: PropTypes.instanceOf(Set).isRequired,
    stages: PropTypes.instanceOf(Set).isRequired
  }),
  applyFilter: PropTypes.func.isRequired,
  setDisplaySaveFilterModal: PropTypes.func.isRequired,
  loadFilter: PropTypes.func.isRequired,
  revert: PropTypes.func.isRequired,
  toggleAppsAndOrgs: PropTypes.func,
  toggleFilter: PropTypes.func
};
