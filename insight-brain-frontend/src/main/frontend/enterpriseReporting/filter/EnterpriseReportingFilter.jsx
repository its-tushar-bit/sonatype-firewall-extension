/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxCollapsibleItems, NxDrawer, NxLoadWrapper, useToggle } from '@sonatype/react-shared-components';
import { take } from 'ramda';

import {
  selectEnterpriseReportingFilter,
  selectShowRevertButton,
  selectInErrorState,
  selectCombinedLoading,
  selectCombinedErrors,
  selectFiltersToDisplay,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import { actions, EI_DEFAULT_FILTER_NAME } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import EnterpriseReportingFilterFooter from 'MainRoot/enterpriseReporting/filter/EnterpriseReportingFilterFooter';
import UnsavedFilterModal from 'MainRoot/enterpriseReporting/filter/unsavedFilterModal/UnsavedFilterModal';
import DeleteFilterModal from 'MainRoot/dashboard/filter/deleteFilterModal/DeleteFilterModal';
import SaveFilterModal from 'MainRoot/enterpriseReporting/filter/saveFilterModal/SaveFilterModal';
import ManageFiltersDropdown from 'MainRoot/dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';
import PortalDrawer from 'MainRoot/react/PortalDrawer';
import { calculateIsFilterDirty, calculateIsFilterDefault } from 'MainRoot/enterpriseReporting/utils';
import './_enterpriseReportingFilter.scss';

const MAX_APPS = 500;

export default function EnterpriseReportingFilter() {
  const dispatch = useDispatch();
  const {
    isOpen,
    loadingIframe,
    savedFilters,
    appliedFilterName,
    previewFilterName,
    defaultFilterId,
    showSaveFilterModal,
    showUnsavedFilterModal,
    showDeleteFilterModal,
    filterToDelete,
    deleteFilterMaskState,
    deleteFilterError,
    filterState,
  } = useSelector(selectEnterpriseReportingFilter);
  const isPreviewFilterDirty = useSelector(selectShowRevertButton);
  const inErrorState = useSelector(selectInErrorState);
  const combineLoading = useSelector(selectCombinedLoading);
  const combineErrors = useSelector(selectCombinedErrors);
  const filtersToDisplay = useSelector(selectFiltersToDisplay);

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const closeDisabled = isDropdownOpen || showSaveFilterModal || showDeleteFilterModal || showUnsavedFilterModal;

  const setSavedFilterAsSelected = (val) => dispatch(actions.setSavedFilterAsSelected(val));
  const setDefaultFilterAsSelected = () => dispatch(actions.setDefaultFilterAsSelected());
  const displayDeleteFilterModal = () => dispatch(actions.setShowDeleteFilterModal(true));
  const hideDeleteFilterModal = () => dispatch(actions.setShowDeleteFilterModal(false));
  const deleteFilter = (val) => dispatch(actions.deleteFilter(val));
  const selectFilterToDelete = (val) => dispatch(actions.setFilterToDelete(val));

  useEffect(() => {
    dispatch(actions.initializeFilters());

    return () => {
      dispatch(actions.reset());
    };
  }, []);

  const closeDrawer = () => {
    //If the user selects a filter from the dropdown but does not apply it, reset the filter name to last applied filter
    if (previewFilterName !== appliedFilterName) {
      dispatch(actions.setPreviewFilterName(appliedFilterName));
    }
    dispatch(actions.setClearDefaultAlert());
    dispatch(actions.toggleShowFilter());
  };

  //prevent the drawer from automatically closing when retryHandler called
  const retryHandler = () => {
    setTimeout(() => {
      dispatch(actions.loadSavedFilters());
      dispatch(actions.loadDefaultFilter());
    }, 0);
  };

  return (
    <>
      {showSaveFilterModal && <SaveFilterModal />}
      {showUnsavedFilterModal && <UnsavedFilterModal />}
      {showDeleteFilterModal && (
        <DeleteFilterModal
          filterToDelete={filterToDelete}
          deleteFilter={deleteFilter}
          deleteFilterMaskState={deleteFilterMaskState}
          hideDeleteFilterModal={hideDeleteFilterModal}
          deleteFilterError={deleteFilterError}
          isUserDefault={(val) => calculateIsFilterDefault(val, defaultFilterId, savedFilters)}
        />
      )}
      <PortalDrawer id="enterprise-reporting-filter" open={isOpen} onClose={closeDrawer} closeDisabled={closeDisabled}>
        <NxDrawer.Header>
          <NxDrawer.HeaderTitle>Saved Filters</NxDrawer.HeaderTitle>
          <NxDrawer.HeaderDescription>
            <span className="enterprise-reporting-filter-description">
              The Enterprise Reporting <strong>Sonatype Default</strong> filter set is always available and cannot be
              changed. To adjust filters, save your selections as a new filter set. Any sets you create can be edited
              deleted as needed. Scheduled deliveries are tied to the filters you chose at the time of scheduling and
              and will not update automatically if a filter set is later edited.
            </span>
          </NxDrawer.HeaderDescription>
          <ManageFiltersDropdown
            appliedFilterName={previewFilterName}
            applyDefaultFilter={setDefaultFilterAsSelected}
            applySavedFilter={setSavedFilterAsSelected}
            displayDeleteFilterModal={displayDeleteFilterModal}
            savedFilters={savedFilters}
            selectFilterToDelete={selectFilterToDelete}
            showDirtyAsterisk={isPreviewFilterDirty}
            //Enterprise Reporting specific props
            defaultFilterName={EI_DEFAULT_FILTER_NAME}
            calculateIsOptionDirty={(filterName) => calculateIsFilterDirty(filterName, appliedFilterName, filterState)}
            defaultFilterId={defaultFilterId}
            handleIsDropdownOpen={setIsDropdownOpen}
            disabled={loadingIframe || inErrorState}
          />
        </NxDrawer.Header>
        <NxDrawer.Content>
          <NxLoadWrapper loading={combineLoading} error={combineErrors} retryHandler={retryHandler}>
            {Object.entries(filtersToDisplay).map(([key, value], idx) => (
              <CollapsibleFilter key={idx} filterName={key} values={value} />
            ))}
          </NxLoadWrapper>
        </NxDrawer.Content>
        <EnterpriseReportingFilterFooter
          isUserDefault={(val) => calculateIsFilterDefault(val, defaultFilterId, savedFilters)}
        />
      </PortalDrawer>
    </>
  );
}

const CollapsibleFilter = ({ filterName, values }) => {
  const [isOpen, toggleIsOpen] = useToggle(false);
  const options = values && typeof values === 'string' ? values.split(',') : [];
  const filteredOptions = take(MAX_APPS, options);

  const triggerContent = (
    <>
      <span>{filterName}</span>
      {!!options.length && <div className="nx-counter nx-counter--active">{options.length}</div>}
    </>
  );

  return (
    <NxCollapsibleItems isOpen={isOpen} onToggleCollapse={toggleIsOpen} triggerContent={triggerContent}>
      {filteredOptions.length > 0 ? (
        filteredOptions.map((val, idx) => <NxCollapsibleItems.Child key={idx}>{val}</NxCollapsibleItems.Child>)
      ) : (
        <NxCollapsibleItems.Child>is any value</NxCollapsibleItems.Child>
      )}
      {options.length > MAX_APPS ? (
        <NxCollapsibleItems.Child>
          <span>+{options.length - MAX_APPS} more</span>
        </NxCollapsibleItems.Child>
      ) : null}
    </NxCollapsibleItems>
  );
};

CollapsibleFilter.propTypes = {
  filterName: PropTypes.string.isRequired,
  values: PropTypes.string,
};
