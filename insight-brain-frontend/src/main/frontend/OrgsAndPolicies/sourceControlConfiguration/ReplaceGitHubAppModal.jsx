/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { actions as gitHubAppActions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { NxH2, NxModal, NxP, NxStatefulForm } from '@sonatype/react-shared-components';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { selectIsGithubAppAuthenticationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

const ReplaceGitHubAppModal = () => {
  const dispatch = useDispatch();
  const { isReplaceGitHubAppModalOpen } = useSelector(selectSourceControlConfigurationSlice);
  const isFeatureEnabled = useSelector(selectIsGithubAppAuthenticationEnabled);

  const closeModal = () => dispatch(actions.closeReplaceGitHubAppModal());
  const continueToRegistration = () => {
    dispatch(actions.closeReplaceGitHubAppModal());
    dispatch(gitHubAppActions.openModal());
  };

  return isReplaceGitHubAppModalOpen && isFeatureEnabled ? (
    <NxModal id="replace-github-app-modal" onCancel={closeModal}>
      <NxStatefulForm onSubmit={continueToRegistration} onCancel={closeModal} submitBtnText="Continue">
        <NxModal.Header>
          <NxH2>Replace GitHub App Configuration</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxP>
            This action replaces the existing GitHub App connection and starts a new configuration flow. The current
            configuration will be overwritten.
          </NxP>
          <NxP>Confirm to proceed with registering a new GitHub App.</NxP>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
};

export default ReplaceGitHubAppModal;
