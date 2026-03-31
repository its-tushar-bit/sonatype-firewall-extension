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
  NxInfoAlert,
  NxButton,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import {
  selectAccountType,
  selectOrganizationName,
  selectIsModalOpen,
  selectIsGitHubAppRegistrationInProgress,
  selectGitHubAppSetupError,
} from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSelectors';
import { selectIsGithubAppAuthenticationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { GITHUB_ACCOUNT_TYPES, GITHUB_URLS } from './utils';
import './_gitHubAppRegistrationModal.scss';

const GitHubAppRegistrationModal = () => {
  const dispatch = useDispatch();
  const isOpen = useSelector(selectIsModalOpen);
  const isFeatureEnabled = useSelector(selectIsGithubAppAuthenticationEnabled);
  const accountType = useSelector(selectAccountType);
  const organizationName = useSelector(selectOrganizationName);
  const registrationInProgress = useSelector(selectIsGitHubAppRegistrationInProgress);
  const setupError = useSelector(selectGitHubAppSetupError);

  const handleSubmit = () => {
    dispatch(
      actions.initiateGitHubAppRegistration({
        accountType,
        organizationName: accountType === GITHUB_ACCOUNT_TYPES.ORGANIZATION ? organizationName : null,
      })
    );
  };

  const handleCancel = () => {
    dispatch(actions.resetGitHubAppState());
    dispatch(actions.closeModal());
  };

  // Form-level validation: check if organization name is required but empty
  const getFormLevelValidationError = () => {
    if (accountType === GITHUB_ACCOUNT_TYPES.ORGANIZATION && !organizationName.trimmedValue) {
      return 'Organization name is required';
    }
    return null;
  };

  const validationErrors = getFormLevelValidationError();

  const handleOpenGitHub = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const newWindow = window.open(GITHUB_URLS.LOGIN, '_blank', 'noopener,noreferrer');
    if (newWindow) newWindow.opener = null;
  };

  return isOpen && isFeatureEnabled ? (
    <NxModal
      id="github-app-registration-modal"
      onCancel={handleCancel}
      aria-labelledby="github-app-registration-modal-title"
    >
      <NxStatefulForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        submitBtnText="Register & Create GitHub App"
        submitBtnClasses="iq-github-app-registration-submit-button"
        submitError={setupError}
        submitMaskState={registrationInProgress ? false : null}
        validationErrors={validationErrors}
      >
        <NxModal.Header>
          <NxH2 id="github-app-registration-modal-title">Connect to GitHub</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxInfoAlert>
            <div className="iq-github-app-registration-modal__alert-content">
              <span>You must be logged in to GitHub to continue.</span>
              <NxButton id="github-app-open-github-button" type="button" variant="primary" onClick={handleOpenGitHub}>
                Open GitHub
              </NxButton>
            </div>
          </NxInfoAlert>
          <NxFieldset label="GitHub Account Type" isRequired>
            <NxRadio
              radioId="github-account-type-org-radio"
              name="accountType"
              value={GITHUB_ACCOUNT_TYPES.ORGANIZATION}
              onChange={() => dispatch(actions.setAccountType(GITHUB_ACCOUNT_TYPES.ORGANIZATION))}
              isChecked={accountType === GITHUB_ACCOUNT_TYPES.ORGANIZATION}
            >
              Organization Account (recommended)
            </NxRadio>
            {accountType === GITHUB_ACCOUNT_TYPES.ORGANIZATION && (
              <p className="nx-p iq-github-app-registration-modal__account-helper-text">
                Match the name shown in your GitHub URL (e.g., <em>github.com/your-org-name</em>). The GitHub App will
                be registered under this organization.
              </p>
            )}
            {accountType === GITHUB_ACCOUNT_TYPES.ORGANIZATION && (
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
              radioId="github-account-type-personal-radio"
              name="accountType"
              value={GITHUB_ACCOUNT_TYPES.PERSONAL}
              onChange={() => dispatch(actions.setAccountType(GITHUB_ACCOUNT_TYPES.PERSONAL))}
              isChecked={accountType === GITHUB_ACCOUNT_TYPES.PERSONAL}
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
