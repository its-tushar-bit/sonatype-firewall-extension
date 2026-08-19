/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { NxH2, NxModal, NxStatefulForm, NxWarningAlert } from '@sonatype/react-shared-components';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';

const UpdateSourceControlConfirmationModal = () => {
  const dispatch = useDispatch();

  const { isConfirmationModalOpen } = useSelector(selectSourceControlConfigurationSlice);
  const ownerName = useSelector(selectSelectedOwnerName);

  const closeUpdateModal = () => dispatch(actions.closeConfirmUpdateModal());
  const save = () => dispatch(actions.save());

  return isConfirmationModalOpen ? (
    <NxModal id="update-source-control-url-modal" onCancel={closeUpdateModal}>
      <NxStatefulForm onSubmit={save} onCancel={closeUpdateModal} submitBtnText="Continue">
        <NxModal.Header>
          <NxH2>Update Source Control Repository URL</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            Changing the repository URL will reset source control data for {ownerName}. Are you sure you want to
            continue?
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
};

export default UpdateSourceControlConfirmationModal;
