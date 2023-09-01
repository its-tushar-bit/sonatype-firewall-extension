/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectRevokeGrandfatheringSlice } from './revokeGrandfatheringSelectors';
import { NxModal, NxH2, NxP, NxStatefulForm } from '@sonatype/react-shared-components';
import { actions } from './revokeGrandfatheringSlice';

export default function RevokeGrandfatheringModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectRevokeGrandfatheringSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const revokeGrandfathering = () => dispatch(actions.revokeGrandfathering());

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="revoke-grandfathering-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={revokeGrandfathering}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Revoke"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>Revoke Legacy Violation Status</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxP>
            Subsequent scans and re-evaluations will treat applicable policy violations as active and trigger configured
            actions.
          </NxP>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
