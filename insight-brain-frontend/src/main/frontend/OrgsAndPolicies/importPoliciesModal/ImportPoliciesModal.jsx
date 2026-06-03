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
  NxButton,
  NxFileUpload,
  NxFormGroup,
} from '@sonatype/react-shared-components';
import { actions } from './importPoliciesSlice';
import { selectHasCustomPolicies } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import TierTag from 'MainRoot/react/shared/TierTag';
import './_importPoliciesModal.scss';

export default function ImportPoliciesModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError, ownerFile } = useSelector(selectImportPoliciesSlice);
  const hasCustomPolicies = useSelector(selectHasCustomPolicies);

  const closeModal = () => dispatch(actions.closeModal());
  const importPolicies = () => dispatch(actions.importPolicies());
  const selectFile = (file) => dispatch(actions.selectFile(file));

  useEffect(() => {
    return () => closeModal();
  }, []);

  if (!isModalOpen) return null;

  if (!hasCustomPolicies) {
    return (
      <NxModal id="import-policy-modal" onCancel={closeModal}>
        <NxModal.Header>
          <NxH2>
            Import Policies <TierTag>Enterprise Feature</TierTag>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content className="iq-import-policy-modal--pro-tier">
          <EnterpriseFullWidthBanner description="Import policy configurations to reduce setup time and apply consistent rules across your organization." />
          <NxWarningAlert>
            <strong>Note:</strong> Importing policies is <strong>destructive</strong>, all existing policies, waivers,
            and license threat groups belonging to this organization and any of its descendants will be{' '}
            <strong>permanently deleted</strong> before importing.
          </NxWarningAlert>
          <NxFormGroup label="Policies File" sublabel="Accepted file types: JSON" isRequired>
            <NxFileUpload onChange={selectFile} accept=".json" isRequired {...ownerFile} />
          </NxFormGroup>
        </NxModal.Content>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <NxButton variant="tertiary" onClick={closeModal}>
              Close
            </NxButton>
          </div>
        </footer>
      </NxModal>
    );
  }

  return (
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
  );
}
