/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { grandfatheringSlice } from './grandfatheringSelectors';
import { NxModal, NxWarningAlert, NxH2, NxForm } from '@sonatype/react-shared-components';
import { actions } from './grandfatheringSlice';

export default function GrandfatheringModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(grandfatheringSlice);

  const closeModal = () => dispatch(actions.closeModal());
  const grandfathering = () => dispatch(actions.grandfathering());

  return isModalOpen ? (
    <NxModal id="grandfathering-modal" onCancel={closeModal}>
      <NxForm
        onSubmit={grandfathering}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Grandfather"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>Grandfather Policy Violations</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            Policy violations for the application will be grandfathered without performing an evaluation.
          </NxWarningAlert>
        </NxModal.Content>
      </NxForm>
    </NxModal>
  ) : null;
}
