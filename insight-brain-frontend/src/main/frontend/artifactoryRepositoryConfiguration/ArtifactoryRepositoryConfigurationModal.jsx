/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';

import {
  NxButton,
  NxFieldset,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxRadio,
  NxSuccessAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';

import LoadError from 'MainRoot/react/LoadError';

import {
  actions,
  MISSING_OR_INVALID_DATA_MESSAGE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';

import {
  selectFormState,
  selectHasAllRequiredData,
  selectArtifactoryRepositoryConfigurationModalSlice,
  selectValidationErrors,
  selectIsUpdate,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';

export default function ArtifactoryRepositoryConfigurationModal() {
  const dispatch = useDispatch();

  const artifactoryRepositoryConfigurationModal = useSelector(selectArtifactoryRepositoryConfigurationModalSlice);
  const hasAllRequiredData = useSelector(selectHasAllRequiredData);
  const validationErrors = useSelector(selectValidationErrors);
  const { baseUrlState, isAnonymous, usernameState, passwordState } = useSelector(selectFormState);
  const isUpdate = useSelector(selectIsUpdate);
  const {
    showModal,
    loading,
    loadConfigurationError,
    saveConfigurationError,
    testConfigurationSuccessful,
    testConfigurationError,
    submitMaskState,
    submitMaskMessage,
  } = artifactoryRepositoryConfigurationModal;

  const setBaseUrl = (value) => dispatch(actions.setBaseUrl(value));
  const setAnonymous = (value) => dispatch(actions.setAnonymous(value));
  const setUsername = (value) => dispatch(actions.setUsername(value));
  const setPassword = (value) => dispatch(actions.setPassword(value));

  const loadConfiguration = () => dispatch(actions.loadConfiguration());
  const testConfiguration = () => dispatch(actions.testConfiguration());
  const saveConfiguration = () => dispatch(actions.saveConfiguration());

  return (
    showModal && (
      <NxModal
        id="artifactory-repository-configuration-modal"
        aria-labelledby="artifactory-repository-configuration-modal-header"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="artifactory-repository-configuration-modal-header">
            {isUpdate ? 'Edit' : 'Add'} Artifactory Repository Configuration
          </h2>
        </header>
        <NxStatefulForm
          id="artifactory-repository-configuration-modal-form"
          loading={loading}
          doLoad={loadConfiguration}
          loadError={loadConfigurationError}
          onSubmit={saveConfiguration}
          submitError={saveConfigurationError}
          validationErrors={validationErrors}
          submitBtnText={isUpdate ? 'Update' : 'Create'}
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          onCancel={() => dispatch(actions.reset())}
        >
          <div className="nx-modal-content">
            <NxFormGroup label="Repository Base URL" sublabel="Example http://your-host:8081/" isRequired>
              <NxTextInput
                id="artifactory-repository-configuration-modal-base-url"
                {...baseUrlState}
                onChange={setBaseUrl}
                aria-required="true"
              />
            </NxFormGroup>
            <NxFieldset label="Repository Authentication" isRequired role="radiogroup">
              <NxRadio
                id="artifactory-repository-configuration-modal-anonymous-radio"
                name="artifactoryAuthentication"
                isChecked={isAnonymous}
                onChange={() => setAnonymous(true)}
                value="1"
              >
                Allow Anonymous Access
              </NxRadio>
              <NxRadio
                id="artifactory-repository-configuration-modal-credentials-radio"
                name="artifactoryAuthentication"
                isChecked={!isAnonymous}
                onChange={() => setAnonymous(false)}
                value="0"
              >
                Enter Username and Password
              </NxRadio>
            </NxFieldset>
            {!isAnonymous && (
              <fieldset id="artifactory-repository-configuration-modal-authentication">
                <NxFormGroup label="Username" sublabel="Enter Username or Token User Code" isRequired>
                  <NxTextInput
                    id="artifactory-repository-configuration-modal-username"
                    {...usernameState}
                    onChange={setUsername}
                    autoComplete="username"
                    aria-required="true"
                  />
                </NxFormGroup>
                <NxFormGroup label="Password" sublabel="Enter Password or Token Pass Code" isRequired>
                  <NxTextInput
                    id="artifactory-repository-configuration-modal-password"
                    {...passwordState}
                    type="password"
                    onChange={setPassword}
                    autoComplete="new-password"
                    aria-required="true"
                  />
                </NxFormGroup>
              </fieldset>
            )}
            <div className="nx-form-row">
              <NxButton
                type="button"
                onClick={hasAllRequiredData ? testConfiguration : null}
                id="artifactory-repository-configuration-modal-test-button"
                variant="tertiary"
                title={hasAllRequiredData ? null : MISSING_OR_INVALID_DATA_MESSAGE}
                className={classnames({ disabled: !hasAllRequiredData })}
                aria-disabled={!hasAllRequiredData}
              >
                Test Configuration
              </NxButton>
            </div>
            {testConfigurationSuccessful && <NxSuccessAlert>Repository configuration test successful.</NxSuccessAlert>}
            {testConfigurationError && (
              <LoadError
                titleMessage="Unable to connect to the configured repository."
                error={testConfigurationError}
                onClose={() => dispatch(actions.resetTestConfigurationError())}
              />
            )}
          </div>
        </NxStatefulForm>
      </NxModal>
    )
  );
}
