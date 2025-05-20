/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxModal, NxH2, NxStatefulForm, NxP, NxButton } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './autoWaiverExclusionDeleteModalSlice';
import { selectAutoWaiverExclusionDeleteModalSlice } from './autoWaiverExclusionDeleteModalSelectors';

export default function AutoWaiverExclusionDeleteModal() {
  const dispatch = useDispatch();
  const closeModal = () => dispatch(actions.closeModal());
  const deleteAutoWaiverExclusion = () => dispatch(actions.deleteAutoWaiverExclusion());
  const { isModalOpen, submitMaskState, submitError } = useSelector(selectAutoWaiverExclusionDeleteModalSlice);

  const additionalFooterBtns = (
    <NxButton variant="tertiary" type="button" className="nx-form__cancel-btn" onClick={closeModal}>
      Cancel
    </NxButton>
  );

  useEffect(() => {
    return () => {
      closeModal();
    };
  }, []);

  return (
    <>
      {isModalOpen && (
        <NxModal
          variant="narrow"
          id="delete-auto-waiver-exclusion-modal"
          aria-labelledby="delete-auto-waiver-exclusion-modal-header"
          onCancel={closeModal}
        >
          <NxModal.Header id="delete-auto-waiver-exclusion-modal">
            <NxH2>Delete Exclusion</NxH2>
          </NxModal.Header>
          <NxStatefulForm
            onSubmit={deleteAutoWaiverExclusion}
            submitMaskMessage="Deleting exclusion..."
            submitMaskState={submitMaskState}
            submitError={submitError}
            submitBtnText="Continue"
            additionalFooterBtns={additionalFooterBtns}
          >
            <NxModal.Content>
              <NxP>Click Continue to resume auto-waiver eligibility for this violation</NxP>
            </NxModal.Content>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}
