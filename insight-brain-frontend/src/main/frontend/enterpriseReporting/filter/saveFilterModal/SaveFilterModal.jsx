/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useToggle, NxCheckbox } from '@sonatype/react-shared-components';
import SaveFilterModalContent from 'MainRoot/dashboard/filter/saveFilterModal/SaveFilterModalContent';
import { selectEnterpriseReportingFilter } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import { actions, EI_DEFAULT_FILTER_NAME } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import './_saveFilterModal.scss';

export default function EnterpriseSaveFilterModal() {
  const dispatch = useDispatch();
  const {
    appliedFilterName,
    saveFilterError,
    saveFilterMaskState,
    saveFilterWarning,
    duplicateFilterName,
  } = useSelector(selectEnterpriseReportingFilter);
  const [isDefault, toggleIsDefault] = useToggle(false);

  const trySave = ({ name, isOverwriting }) => {
    dispatch(actions.trySaveFilter({ name, isDefault, isOverwriting }));
  };

  const defaultContent = (
    <NxCheckbox
      className="enterprise-reporting-save-filter-modal__checkbox"
      onChange={toggleIsDefault}
      isChecked={isDefault}
    >
      Make this my default
    </NxCheckbox>
  );

  return (
    <SaveFilterModalContent
      appliedFilterName={appliedFilterName}
      existingDuplicateFilterName={duplicateFilterName}
      saveError={saveFilterError}
      saveFilter={trySave}
      saveFilterMaskState={saveFilterMaskState}
      saveFilterWarning={saveFilterWarning}
      cancelSaveFilter={() => dispatch(actions.setShowSaveFilterModal(false))}
      defaultFilterName={EI_DEFAULT_FILTER_NAME}
      maxNameLength={35}
      saveAsLabel="Save New Filter Set"
      additionalFooterBtns={saveFilterWarning ? null : defaultContent}
    />
  );
}
