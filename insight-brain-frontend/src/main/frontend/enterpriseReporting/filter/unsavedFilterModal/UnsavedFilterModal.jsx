/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxFooter, NxH2, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';

import { actions, EI_DEFAULT_FILTER_NAME } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import { selectEnterpriseReportingFilter } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';

export default function UnsavedFilterModal() {
  const dispatch = useDispatch();
  const { previewFilterName } = useSelector(selectEnterpriseReportingFilter);

  const handleApplyFilters = () => {
    dispatch(actions.applySavedFilterAndRunDashboard());
    dispatch(actions.setShowUnsavedFilterModal(false));
  };

  const handleSaveFilters = () => {
    dispatch(actions.setShowSaveFilterModal(true));
    dispatch(actions.setShowUnsavedFilterModal(false));
  };

  return (
    <NxModal onCancel={() => dispatch(actions.setShowUnsavedFilterModal(false))}>
      <NxModal.Header>
        <NxH2>Unsaved filters will be lost</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert>
          You&apos;ve applied filters that haven&apos;t been saved. Applying{' '}
          <strong>&quot;{previewFilterName || EI_DEFAULT_FILTER_NAME}&quot;</strong> will overwrite them. To keep your
          current filters, save them as a new filter set first.
        </NxWarningAlert>
      </NxModal.Content>
      <NxFooter>
        <div className="nx-btn-bar">
          <NxButton onClick={() => dispatch(actions.setShowUnsavedFilterModal(false))}>Cancel</NxButton>
          <NxButton onClick={handleSaveFilters}>Save Current Filters</NxButton>
          <NxButton onClick={handleApplyFilters} variant="primary">
            Apply Anyway
          </NxButton>
        </div>
      </NxFooter>
    </NxModal>
  );
}
