/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxErrorAlert, NxFooter, NxSuccessAlert } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import {
  selectEnterpriseReportingFilter,
  selectIsFilterDirty,
  selectShowRevertButton,
  selectSaveButtonDisabled,
  selectInErrorState,
  selectMakeDefaultBaseDisabled,
  selectIsSavedFilterApplied,
} from './enterpriseReportingFilterSelectors';

export default function EnterpriseReportingFilterFooter({ isUserDefault }) {
  const dispatch = useDispatch();
  const {
    appliedFilterName,
    defaultFilterId,
    saveDefaultFilterError,
    showDefaultFilterSuccessAlert,
    loadingIframe,
  } = useSelector(selectEnterpriseReportingFilter);

  const isSavedFilterApplied = useSelector(selectIsSavedFilterApplied);
  const filtersAreDirty = useSelector(selectIsFilterDirty);
  const inErrorState = useSelector(selectInErrorState);
  const showRevertBtn = useSelector(selectShowRevertButton);
  const disableSaveBtn = useSelector(selectSaveButtonDisabled);
  const disableDefaultBtn = useSelector(selectMakeDefaultBaseDisabled);

  const isDefault = (appliedFilterName && isUserDefault(appliedFilterName)) || (!appliedFilterName && !defaultFilterId);
  const makeDefaultDisabled = isDefault || disableDefaultBtn;

  const handleClickOnApply = () => {
    if (filtersAreDirty) {
      dispatch(actions.setShowUnsavedFilterModal(true));
    } else {
      dispatch(actions.applySavedFilterAndRunDashboard());
    }
  };

  const handleSetDefault = () => {
    if (appliedFilterName) {
      dispatch(actions.saveDefaultFilter());
    } else {
      //if user wants to make 'Sonatype Default' their default, remove custom default set as their default
      dispatch(actions.deleteDefaultFilter());
    }
  };

  //prevent the drawer from automatically closing when alert is closed
  const handleClearAlert = () => {
    setTimeout(() => dispatch(actions.setClearDefaultAlert()), 0);
  };

  const alertMsg = saveDefaultFilterError ? (
    <NxErrorAlert onClose={handleClearAlert}>
      {saveDefaultFilterError} Click <strong>Make My Default</strong> again to retry.
    </NxErrorAlert>
  ) : showDefaultFilterSuccessAlert ? (
    <NxSuccessAlert onClose={handleClearAlert}>
      Success! <strong>{appliedFilterName || 'Sonatype Default'}</strong> is now your default filter set.
    </NxSuccessAlert>
  ) : null;

  return (
    <NxFooter>
      {alertMsg}
      <div className="nx-btn-bar">
        {showRevertBtn && (
          <NxButton onClick={() => dispatch(actions.revertFilterChanges())} disabled={inErrorState}>
            Revert
          </NxButton>
        )}
        <NxButton onClick={handleSetDefault} disabled={makeDefaultDisabled}>
          Make My Default
        </NxButton>
        {isSavedFilterApplied ? (
          <NxButton
            id="enterprise-reporting-dashboard-filter__save"
            onClick={() => dispatch(actions.setShowSaveFilterModal(true))}
            variant="primary"
            disabled={disableSaveBtn}
          >
            Save As
          </NxButton>
        ) : (
          <NxButton
            id="enterprise-reporting-dashboard-filter__apply"
            disabled={loadingIframe}
            onClick={handleClickOnApply}
            variant="primary"
          >
            Apply
          </NxButton>
        )}
      </div>
    </NxFooter>
  );
}

EnterpriseReportingFilterFooter.propTypes = {
  isUserDefault: PropTypes.func,
};
