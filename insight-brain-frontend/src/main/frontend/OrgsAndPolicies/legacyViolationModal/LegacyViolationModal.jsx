/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { legacyViolationModalSlice } from './legacyViolationModalSelectors';
import { NxModal, NxInfoAlert, NxH2, NxStatefulForm, NxP } from '@sonatype/react-shared-components';
import { actions } from './legacyViolationModalSlice';

export default function LegacyViolationModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(legacyViolationModalSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const legacyViolation = () => dispatch(actions.legacyViolation());

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="legacy-violation-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={legacyViolation}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Update"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>Grant Legacy Violation Status</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxP>
            Existing policy violations will be acknowledged as Legacy Violations in subsequent scans and will not cause
            a policy action.
          </NxP>
          <NxInfoAlert>This action itself does not perform a new scan or re-evaluation.</NxInfoAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
