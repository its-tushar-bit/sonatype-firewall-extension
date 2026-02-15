/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxModal,
  NxStatefulForm,
  NxFieldset,
  NxRadio,
  NxFormGroup,
  NxTextInput,
  NxH2,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import {
  selectAccountType,
  selectOrganizationName,
  selectIsModalOpen,
  selectIsGitHubAppRegistrationInProgress,
  selectGitHubAppSetupError,
} from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSelectors';
import './_gitHubAppRegistrationModal.scss';

const GitHubAppRegistrationModal = () => {
  const dispatch = useDispatch();
  const isOpen = useSelector(selectIsModalOpen);
  const accountType = useSelector(selectAccountType);
  const organizationName = useSelector(selectOrganizationName);
  const registrationInProgress = useSelector(selectIsGitHubAppRegistrationInProgress);
  const setupError = useSelector(selectGitHubAppSetupError);

  const handleSubmit = () => {
    dispatch(
      actions.initiateGitHubAppRegistration({
        accountType,
        organizationName: accountType === 'organization' ? organizationName : null,
      })
    );
  };

  const handleCancel = () => {
    dispatch(actions.resetGitHubAppState());
    dispatch(actions.closeModal());
  };

  // Form-level validation: check if organization name is required but empty
  const getFormLevelValidationError = () => {
    if (accountType === 'organization' && !organizationName.trimmedValue) {
      return 'Organization name is required';
    }
    return null;
  };

  const validationErrors = getFormLevelValidationError();

  return isOpen ? (
    <NxModal
      id="github-app-registration-modal"
      onCancel={handleCancel}
      aria-labelledby="github-app-registration-modal-title"
    >
      <NxStatefulForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        submitBtnText="Register & Create GitHub App"
        submitError={setupError}
        submitMaskState={registrationInProgress ? false : null}
        validationErrors={validationErrors}
      >
        <NxModal.Header>
          <NxH2 id="github-app-registration-modal-title">Connect to GitHub</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxFieldset label="GitHub Account Type" isRequired>
            <NxRadio
              name="accountType"
              value="organization"
              onChange={() => dispatch(actions.setAccountType('organization'))}
              isChecked={accountType === 'organization'}
            >
              Organization Account (recommended)
            </NxRadio>
            {accountType === 'organization' && (
              <p className="nx-p iq-github-app-registration-modal__account-helper-text">
                Match the name shown in your GitHub URL (e.g., <em>github.com/your-org-name</em>). The GitHub App will
                be registered under this organization.
              </p>
            )}
            {accountType === 'organization' && (
              <NxFormGroup
                label="Organization Name"
                isRequired
                className="iq-github-app-registration-modal__org-name-input"
              >
                <NxTextInput
                  id="github-org-name"
                  type="text"
                  placeholder="your-org-name"
                  validatable
                  {...organizationName}
                  onChange={(value) => dispatch(actions.setOrganizationName(value))}
                />
              </NxFormGroup>
            )}
            <NxRadio
              name="accountType"
              value="personal"
              onChange={() => dispatch(actions.setAccountType('personal'))}
              isChecked={accountType === 'personal'}
            >
              Personal Account
            </NxRadio>
          </NxFieldset>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
};

export default GitHubAppRegistrationModal;
