/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectRevokeGrandfatheringSlice } from './revokeGrandfatheringSelectors';
import { NxModal, NxWarningAlert, NxH2, NxForm } from '@sonatype/react-shared-components';
import { actions } from './revokeGrandfatheringSlice';

export default function RevokeGrandfatheringModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectRevokeGrandfatheringSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const revokeGrandfathering = () => dispatch(actions.revokeGrandfathering());

  return isModalOpen ? (
    <NxModal id="revoke-grandfathering-modal" onCancel={closeModal}>
      <NxForm
        onSubmit={revokeGrandfathering}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Revoke"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>Revoke Grandfathered Policy Violations</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            Revoking the grandfathered policy violations for the application will reinstate violations if applicable.
          </NxWarningAlert>
        </NxModal.Content>
      </NxForm>
    </NxModal>
  ) : null;
}
