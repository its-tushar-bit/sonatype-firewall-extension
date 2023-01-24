/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectImportPoliciesSlice } from './importPoliciesSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import {
  NxModal,
  NxWarningAlert,
  NxH2,
  NxStatefulForm,
  NxFileUpload,
  NxFormGroup,
} from '@sonatype/react-shared-components';
import { actions } from './importPoliciesSlice';

export default function ImportPoliciesModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError, ownerFile } = useSelector(selectImportPoliciesSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const importPolicies = () => dispatch(actions.importPolicies());
  const selectFile = (file) => dispatch(actions.selectFile(file));

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="import-policy-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={importPolicies}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Import"
        submitError={submitError}
        validationErrors={ownerFile.files ? null : GLOBAL_FORM_VALIDATION_ERROR}
      >
        <NxModal.Header>
          <NxH2>Import Policies</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            <strong>Note:</strong> Importing policies is <strong>destructive</strong>, all existing policies, waivers,
            and license threat groups belonging to this organization and any of its descendants will be{' '}
            <strong>permanently deleted</strong> before importing.
          </NxWarningAlert>
          <NxFormGroup label="Policies File" sublabel="Accepted file types: JSON" isRequired>
            <NxFileUpload onChange={selectFile} accept=".json" isRequired {...ownerFile} />
          </NxFormGroup>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
