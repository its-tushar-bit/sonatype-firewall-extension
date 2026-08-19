/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

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
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverExclusionCreateModalSlice';
import { selectAutoWaiverExclusionCreateModalSlice } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverExclusionCreateModalSelectors';

export default function AutoWaiverExclusionCreateModal() {
  const dispatch = useDispatch();
  const { submitMaskState, submitError } = useSelector(selectAutoWaiverExclusionCreateModalSlice);
  const [createExclusionCheckbox, toggleCreateExclusionCheckbox] = useToggle(false);
  const { isOpen } = useSelector(selectAutoWaiverExclusionCreateModalSlice);

  const handleSubmit = () => {
    dispatch(actions.createAutoWaiverExclusion());
  };

  const onClose = () => dispatch(actions.closeModal());

  if (!isOpen) {
    return null;
  }

  return (
    <NxModal variant="narrow" onCancel={onClose}>
      <NxStatefulForm
        onSubmit={handleSubmit}
        onCancel={onClose}
        validationErrors={validationError(createExclusionCheckbox)}
        submitMaskMessage="Creating auto-waiver exclusion..."
        submitMaskState={submitMaskState}
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>Remove Automated Waiver</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxH3>Do not auto-waive this violation</NxH3>
          <NxP>
            <i>Remove auto-waiver and exclude from future automations</i>
          </NxP>
          <NxCheckbox
            checkboxId="delete-auto-waiver-confirmation"
            onChange={toggleCreateExclusionCheckbox}
            isChecked={createExclusionCheckbox}
          >
            Remove auto-waiver from this violation
          </NxCheckbox>
          <NxInfoAlert>Removing this auto-waiver does not disable all automated waivers.</NxInfoAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}

const validationError = (createExclusionCheckbox) => {
  return createExclusionCheckbox ? null : 'You must confirm the removal of the auto-waiver';
};
