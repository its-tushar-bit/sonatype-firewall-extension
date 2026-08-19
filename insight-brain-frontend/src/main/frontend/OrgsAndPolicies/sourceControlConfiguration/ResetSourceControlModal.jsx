/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { NxH2, NxModal, NxStatefulForm, NxWarningAlert } from '@sonatype/react-shared-components';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';

const ResetSourceControlModal = () => {
  const dispatch = useDispatch();
  const { isResetModalOpen, resetSubmitError } = useSelector(selectSourceControlConfigurationSlice);
  const ownerName = useSelector(selectSelectedOwnerName);

  const closeResetModal = () => dispatch(actions.closeResetModal());
  const reset = () => dispatch(actions.reset());

  return isResetModalOpen ? (
    <NxModal id="reset-source-control-modal" onCancel={closeResetModal}>
      <NxStatefulForm
        onSubmit={reset}
        onCancel={closeResetModal}
        submitBtnText="Continue"
        submitError={resetSubmitError}
      >
        <NxModal.Header>
          <NxH2>Reset Source Control</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            You are about to reset the Source Control configuration for {ownerName}. This action cannot be undone.
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
};

export default ResetSourceControlModal;
