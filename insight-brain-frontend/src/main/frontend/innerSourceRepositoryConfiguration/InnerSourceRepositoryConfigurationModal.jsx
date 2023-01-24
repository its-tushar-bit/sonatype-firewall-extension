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
  NxFormSelect,
  NxModal,
  NxRadio,
  NxSuccessAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';

import LoadError from 'MainRoot/react/LoadError';

import {
  actions,
  MISSING_OR_INVALID_DATA_MESSAGE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';

import {
  selectFormState,
  selectHasAllRequiredData,
  selectInnerSourceRepositoryConfigurationModalSlice,
  selectValidationErrors,
  selectIsUpdate,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';

export default function InnerSourceRepositoryConfigurationModal() {
  const dispatch = useDispatch();

  const innerSourceRepositoryConfigurationModal = useSelector(selectInnerSourceRepositoryConfigurationModalSlice);
  const hasAllRequiredData = useSelector(selectHasAllRequiredData);
  const validationErrors = useSelector(selectValidationErrors);
  const { format, baseUrlState, isAnonymous, usernameState, passwordState } = useSelector(selectFormState);
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
  } = innerSourceRepositoryConfigurationModal;

  const setFormat = (value) => dispatch(actions.setFormat(value));
  const setBaseUrl = (value) => dispatch(actions.setBaseUrl(value));
  const setAnonymous = (value) => dispatch(actions.setAnonymous(value));
  const setUsername = (value) => dispatch(actions.setUsername(value));
  const setPassword = (value) => dispatch(actions.setPassword(value));

  const loadConfiguration = () => dispatch(actions.loadConfiguration());
  const testConfiguration = () => dispatch(actions.testConfiguration());
  const saveConfiguration = () => dispatch(actions.saveConfiguration());

  return (
    <>
      {showModal && (
        <NxModal id="innersource-repository-configuration-modal">
          <header className="nx-modal-header">
            <h2 className="nx-h2">{isUpdate ? 'Edit' : 'Add'} InnerSource Repository Configuration</h2>
          </header>
          <NxStatefulForm
            id="innersource-repository-configuration-modal-form"
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
              <NxFormGroup label="Repository Format" sublabel="Only one repository per format is allowed." isRequired>
                <NxFormSelect
                  id="innersource-repository-configuration-modal-format-select"
                  value={format}
                  onChange={(event) => setFormat(event.currentTarget.value)}
                >
                  <option value="generic">generic (all formats)</option>
                  <option value="maven">maven</option>
                  <option value="npm">npm</option>
                </NxFormSelect>
              </NxFormGroup>
              <NxFormGroup label="Repository Base URL" sublabel="Example http://your-host:8081/" isRequired>
                <NxTextInput
                  id="innersource-repository-configuration-modal-base-url"
                  {...baseUrlState}
                  onChange={setBaseUrl}
                  aria-required="true"
                />
              </NxFormGroup>
              <NxFieldset label="Repository Authentication" isRequired aria-required="true" role="radiogroup">
                <NxRadio
                  id="innersource-repository-configuration-modal-anonymous-radio"
                  name="repositoryAuthentication"
                  isChecked={isAnonymous}
                  onChange={() => setAnonymous(true)}
                  value="1"
                >
                  Allow Anonymous Access
                </NxRadio>
                <NxRadio
                  id="innersource-repository-configuration-modal-credentials-radio"
                  name="repositoryAuthentication"
                  isChecked={!isAnonymous}
                  onChange={() => setAnonymous(false)}
                  value="0"
                >
                  Enter Username and Password
                </NxRadio>
              </NxFieldset>
              {!isAnonymous && (
                <fieldset id="innersource-repository-configuration-modal-authentication">
                  <NxFormGroup label="Username" sublabel="Enter Username or Token User Code" isRequired>
                    <NxTextInput
                      id="innersource-repository-configuration-modal-username"
                      {...usernameState}
                      onChange={setUsername}
                      autoComplete="username"
                      aria-required="true"
                    />
                  </NxFormGroup>
                  <NxFormGroup label="Password" sublabel="Enter Password or Token Pass Code" isRequired>
                    <NxTextInput
                      id="innersource-repository-configuration-modal-password"
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
                  id="innersource-repository-configuration-modal-test-button"
                  variant="tertiary"
                  title={hasAllRequiredData ? null : MISSING_OR_INVALID_DATA_MESSAGE}
                  className={classnames({ disabled: !hasAllRequiredData })}
                  aria-disabled={!hasAllRequiredData}
                >
                  Test Configuration
                </NxButton>
              </div>
              {testConfigurationSuccessful && (
                <NxSuccessAlert>Repository configuration test successful.</NxSuccessAlert>
              )}
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
      )}
    </>
  );
}
