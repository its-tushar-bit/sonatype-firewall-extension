/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxModal, NxH2, NxStatefulForm, NxP } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversExclusionsSlice';
import { selectAutomatedWaiversExclusionSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversExclusionsSelector';

export default function DeleteExclusionModal({ showModal, onClose, autoPolicyWaiverId, autoPolicyWaiverExclusionId }) {
  const dispatch = useDispatch();
  const { deleteExclusionSubmitMaskState, deleteExclusionSubmitError } = useSelector(
    selectAutomatedWaiversExclusionSlice
  );
  const handleSubmit = () => {
    dispatch(
      actions.deleteAutoWaiverExclusion({
        autoPolicyWaiverId,
        autoPolicyWaiverExclusionId,
      })
    );
  };

  if (!showModal) return null;

  return (
    <NxModal id="iq-delete-auto-waiver-exclusion-modal" variant="narrow" onCancel={onClose}>
      <NxModal.Header>
        <NxH2>Delete Exclusion</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxStatefulForm
          className="nx-form"
          onSubmit={handleSubmit}
          onCancel={onClose}
          submitMaskMessage="Deleting exclusion..."
          submitMaskState={deleteExclusionSubmitMaskState}
          submitError={deleteExclusionSubmitError}
          submitBtnText="Continue"
        >
          <NxP>Click Continue to resume automated waiver eligibility for this violation</NxP>
        </NxStatefulForm>
      </NxModal.Content>
    </NxModal>
  );
}

DeleteExclusionModal.propTypes = {
  showModal: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  autoPolicyWaiverId: PropTypes.string.isRequired,
  autoPolicyWaiverExclusionId: PropTypes.string.isRequired,
};
