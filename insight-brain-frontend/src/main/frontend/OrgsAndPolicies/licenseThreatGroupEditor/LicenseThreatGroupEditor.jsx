/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxButton,
  NxTextInput,
  NxFormGroup,
  NxModal,
  NxFontAwesomeIcon,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  selectIsLoading,
  selectLicenseThreatGroupIsEditMode,
  selectLicenseThreatGroupIsDirty,
  selectLicenseThreatGroupSubmitError,
  selectLicenseThreatGroupLoadError,
  selectDirtyLicenseThreatGroup,
  selectSubmitMaskState,
  selectValidationError,
  selectDeleteMaskState,
  selectDeleteError,
  selectAvailableLicenses,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import LtgTransferList from './LtgTransferList';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function LicenseThreatGroupEditor() {
  const dispatch = useDispatch();

  const setLicenseThreatGroupName = (val) => dispatch(actions.setLicenseThreatGroupName(val));
  const setLicenseThreatGroupThreatLevel = (val) => dispatch(actions.setLicenseThreatGroupThreatLevel(val));
  const setPickedLicenses = useCallback((val) => dispatch(actions.setPickedLicenses(val)), []);
  const saveLicenseThreatGroup = () => dispatch(actions.saveLicenseThreatGroup());
  const removeLicenseThreatGroup = () => dispatch(actions.removeLicenseThreatGroup());
  const clearDeleteError = () => dispatch(actions.clearDeleteError());
  const doLoad = () => dispatch(actions.loadLicenseThreatGroupEditor());

  const loading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLicenseThreatGroupLoadError);
  const isEditMode = useSelector(selectLicenseThreatGroupIsEditMode);
  const isDirty = useSelector(selectLicenseThreatGroupIsDirty);
  const submitError = useSelector(selectLicenseThreatGroupSubmitError);
  const dirtyLTG = useSelector(selectDirtyLicenseThreatGroup);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const deleteError = useSelector(selectDeleteError);
  const validationError = useSelector(selectValidationError);
  const allLicenses = useSelector(selectAvailableLicenses);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const openDeleteModal = () => {
    clearDeleteError();
    setIsDeleteModalOpen(true);
  };
  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <NxPageTitle>
        <NxH1>{isEditMode ? 'Edit' : 'New'} License Threat Group</NxH1>
      </NxPageTitle>
      <NxTile id="license-threat-group-editor">
        <NxStatefulForm
          onSubmit={saveLicenseThreatGroup}
          submitBtnText={isEditMode ? 'Update' : 'Create'}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          doLoad={doLoad}
          loadError={loadError}
          loading={loading}
          validationErrors={getValidationMessage(isDirty, validationError)}
          submitError={submitError}
          additionalFooterBtns={
            isEditMode ? (
              <NxButton id="delete-ltg-button" onClick={openDeleteModal} variant="tertiary" type="button">
                Delete
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
            <div className="nx-form-row">
              <NxFormGroup id="editor-label-name" label="Group Name" isRequired>
                <NxTextInput validatable={true} {...dirtyLTG.name} onChange={setLicenseThreatGroupName} />
              </NxFormGroup>
              <ThreatDropdownSelector
                threatLevel={dirtyLTG.threatLevel}
                onSelectThreatLevel={setLicenseThreatGroupThreatLevel}
              />
            </div>
            <LtgTransferList
              licenseIds={dirtyLTG.licenseIds}
              allLicenses={allLicenses}
              setSelectedLicenses={setPickedLicenses}
            />
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
      {isDeleteModalOpen && (
        <NxModal
          id="ltg-config-delete-modal"
          aria-labelledby="ltg-config-delete-modal-header"
          onClose={closeDeleteModal}
        >
          <NxStatefulForm
            onSubmit={removeLicenseThreatGroup}
            submitMaskState={deleteMaskState}
            onCancel={closeDeleteModal}
            submitBtnText="Delete"
            submitMaskMessage="Deleting…"
            submitError={deleteError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="ltg-config-delete-modal-header">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete License Threat Group</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                You are about to permanently remove {dirtyLTG.name.value}. This action cannot be undone.
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}
