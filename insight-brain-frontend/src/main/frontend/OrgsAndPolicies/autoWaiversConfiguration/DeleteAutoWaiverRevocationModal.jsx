/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxModal, NxH2, NxStatefulForm, NxP } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversRevocationsSlice';
import { createPortal } from 'react-dom';
import { selectAutomatedWaiversRevocationSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversRevocationsSelector';

export default function DeleteExclusionModal({ showModal, onClose, autoPolicyWaiverId, autoPolicyWaiverRevocationId }) {
  const dispatch = useDispatch();
  const { deleteRevocationSubmitMaskState, deleteRevocationSubmitError } = useSelector(
    selectAutomatedWaiversRevocationSlice
  );
  const handleSubmit = () => {
    dispatch(
      actions.deleteAutoWaiverRevocation({
        autoPolicyWaiverId,
        autoPolicyWaiverRevocationId,
      })
    );
  };

  if (!showModal) return null;

  return createPortal(
    <NxModal id="iq-delete-auto-waiver-revocation-modal" variant="narrow" onCancel={onClose}>
      <NxModal.Header>
        <NxH2>Delete Exclusion</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxStatefulForm
          className="nx-form"
          onSubmit={handleSubmit}
          onCancel={onClose}
          submitMaskMessage="Deleting exclusion..."
          submitMaskState={deleteRevocationSubmitMaskState}
          submitError={deleteRevocationSubmitError}
          submitBtnText="Continue"
        >
          <NxP>Click Continue to resume automated waiver eligibility for this violation</NxP>
        </NxStatefulForm>
      </NxModal.Content>
    </NxModal>,
    document.body
  );
}

DeleteExclusionModal.propTypes = {
  showModal: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  autoPolicyWaiverId: PropTypes.string.isRequired,
  autoPolicyWaiverRevocationId: PropTypes.string.isRequired,
};
