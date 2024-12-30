/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';

import {
  NxCheckbox,
  NxH2,
  NxH3,
  NxInfoAlert,
  NxModal,
  NxP,
  NxStatefulForm,
  useToggle,
} from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversRevocationsSlice';
import { selectAutomatedWaiversRevocationSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversRevocationsSelector';

export default function DeleteAutoWaiverModal({ showModal, setShowModal, onClose }) {
  const dispatch = useDispatch();
  const { submitMaskState, submitError } = useSelector(selectAutomatedWaiversRevocationSlice);
  const [isDeleteAutoWaiverConfirmed, toggleDeleteWaiverConfirmation] = useToggle(false);

  const handleSubmit = () => {
    dispatch(actions.createAutoWaiverRevocation());
  };

  useEffect(() => {
    if (!showModal) {
      dispatch(actions.clearAutoWaiverRevocationMaskState());
    }
  }, [showModal]);

  useEffect(() => {
    if (submitMaskState === true) {
      setShowModal(false);
    }
  }, [submitMaskState]);

  return (
    <>
      {showModal && (
        <NxModal id="iq-delete-auto-waiver-modal" variant="narrow" onCancel={onClose}>
          <NxModal.Header>
            <NxH2>Remove Auto-Waiver</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxStatefulForm
              className="nx-form"
              onSubmit={handleSubmit}
              onCancel={onClose}
              validationErrors={validationError(isDeleteAutoWaiverConfirmed)}
              submitMaskMessage="Creating auto-waiver revocation"
              submitMaskState={submitMaskState}
              submitError={submitError}
            >
              <NxH3>Do not auto-waive this violation</NxH3>
              <NxP>
                <i>Remove waiver and exclude from future automations</i>
              </NxP>
              <NxCheckbox
                checkboxId="delete-auto-waiver-confirmation"
                onChange={toggleDeleteWaiverConfirmation}
                isChecked={isDeleteAutoWaiverConfirmed}
              >
                Remove auto-waiver
              </NxCheckbox>
              <NxInfoAlert>Removing this waiver does not disable all automated waivers.</NxInfoAlert>
            </NxStatefulForm>
          </NxModal.Content>
        </NxModal>
      )}
    </>
  );
}

const validationError = (isDeleteAutoWaiverConfirmed) => {
  return isDeleteAutoWaiverConfirmed ? null : 'You must confirm the removal of the auto-waiver';
};

DeleteAutoWaiverModal.propTypes = {
  showModal: PropTypes.bool.isRequired,
  setShowModal: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
