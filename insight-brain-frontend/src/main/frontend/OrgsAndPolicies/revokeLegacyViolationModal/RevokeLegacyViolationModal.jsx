/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectRevokeLegacyViolationModalSlice } from './revokeLegacyViolationModalSelectors';
import { NxModal, NxH2, NxP, NxStatefulForm } from '@sonatype/react-shared-components';
import { actions } from './revokeLegacyViolationModalSlice';

export default function RevokeLegacyViolationModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectRevokeLegacyViolationModalSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const revokeLegacyViolation = () => dispatch(actions.revokeLegacyViolation());

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="revoke-legacy-violation-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={revokeLegacyViolation}
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
