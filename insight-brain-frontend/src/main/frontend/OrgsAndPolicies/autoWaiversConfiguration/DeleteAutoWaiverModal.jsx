/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFontAwesomeIcon, NxH2, NxModal, NxStatefulForm, NxWarningAlert } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectApplicableAutoWaivers } from 'MainRoot/OrgsAndPolicies/autoWaiversSelectors';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/applicableAutoWaiversSlice';

export default function DeleteAutoWaiverModal() {
  const dispatch = useDispatch();
  const applicableAutoWaivers = useSelector(selectApplicableAutoWaivers);

  const { deleteSubmitMask, deleteError } = applicableAutoWaivers || {};

  const handleCancel = () => {
    dispatch(actions.closeDeleteModal());
  };

  const handleDelete = () => {
    dispatch(actions.deleteAutoWaiver());
  };

  return (
    <NxModal
      onCancel={handleCancel}
      variant="narrow"
      data-testid="iq-delete-auto-waiver-modal"
      aria-labelledby="modal-header-text"
    >
      <NxStatefulForm
        className="nx-form"
        onSubmit={handleDelete}
        submitMaskState={deleteSubmitMask}
        onCancel={handleCancel}
        submitBtnText="Delete"
        submitError={deleteError}
      >
        <NxModal.Header>
          <NxH2 id="modal-header-text">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Auto-Waiver</span>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            You are about to permanently delete an auto-waiver. This action cannot be undone.
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}
