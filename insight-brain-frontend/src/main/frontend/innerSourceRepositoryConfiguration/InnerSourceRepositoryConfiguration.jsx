/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import {
  NxButton,
  NxFieldset,
  NxForm,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxModal,
  NxRadio,
  NxTextInput,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import LoadError from 'MainRoot/react/LoadError';
import {
  actions,
  MISSING_OR_INVALID_DATA_MESSAGE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSlice';
import classnames from 'classnames';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectFormState,
  selectHasAllRequiredData,
  selectInnerSourceRepositoryConfigurationSlice,
  selectIsDirty,
  selectIsUpdate,
  selectValidationErrors,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSelectors';

export default function InnerSourceRepositoryConfiguration() {
  const dispatch = useDispatch();
  const isUpdate = useSelector(selectIsUpdate);
  const innerSourceRepositoryConfiguration = useSelector(selectInnerSourceRepositoryConfigurationSlice);
  const isDirty = useSelector(selectIsDirty);
  const hasAllRequiredData = useSelector(selectHasAllRequiredData);
  const validationErrors = useSelector(selectValidationErrors);
  const { format, baseUrlState, isAnonymous, usernameState, passwordState } = useSelector(selectFormState);
  const {
    loading,
    loadConfigurationError,
    saveConfigurationError,
    testConfigurationSuccessful,
    testConfigurationError,
    showDeleteModal,
    deleteConfigurationError,
    deleteSubmitMaskState,
    submitMaskState,
    submitMaskMessage,
  } = innerSourceRepositoryConfiguration;

  const setFormat = (value) => dispatch(actions.setFormat(value));
  const setBaseUrl = (value) => dispatch(actions.setBaseUrl(value));
  const setAnonymous = (value) => dispatch(actions.setAnonymous(value));
  const setUsername = (value) => dispatch(actions.setUsername(value));
  const setPassword = (value) => dispatch(actions.setPassword(value));
  const loadConfiguration = () => dispatch(actions.loadConfiguration());
  const cancel = () => dispatch(actions.cancel());
  const saveConfiguration = () => dispatch(actions.saveConfiguration());
  const testConfiguration = () => dispatch(actions.testConfiguration());
  const setShowDeleteModal = (value) => dispatch(actions.setShowDeleteModal(value));
  const deleteConfiguration = () => dispatch(actions.deleteConfiguration());

  useEffect(() => {
    if (isUpdate) {
      loadConfiguration();
    }
  }, [isUpdate]);

  return (
    <main id="innersource-repository-configuration-page-container" className="nx-page-main">
      <header className="nx-page-title">
        <h1 className="nx-h1">{isUpdate ? 'Edit' : 'Add'} Repository Configuration</h1>
      </header>
      <section className="nx-tile">
        <NxForm
          id="innersource-repository-configuration-form"
          loading={loading}
          doLoad={loadConfiguration}
          loadError={loadConfigurationError}
          onSubmit={saveConfiguration}
          submitError={saveConfigurationError}
          validationErrors={validationErrors}
          submitBtnText={isUpdate ? 'Update' : 'Create'}
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          additionalFooterBtns={
            <>
              {isUpdate && (
                <NxButton
                  id="innersource-repository-configuration-delete-button"
                  type="button"
                  onClick={() => setShowDeleteModal(true)}
                >
                  Delete Configuration
                </NxButton>
              )}
              <NxButton
                id="innersource-repository-configuration-cancel-button"
                type="button"
                onClick={cancel}
                variant="tertiary"
                disabled={!isDirty}
              >
                Cancel
              </NxButton>
            </>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configuration Details</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <p className="nx-p">Configure the repository you want to use to identify InnerSource components.</p>
            <NxFormGroup label="Repository Format" sublabel="Only one repository per format is allowed." isRequired>
              <NxFormSelect
                id="innersource-repository-configuration-format-select"
                value={format}
                onChange={(event) => setFormat(event.currentTarget.value)}
              >
                <option value="generic">generic (all formats)</option>
                <option value="maven">maven</option>
                <option value="npm">npm</option>
              </NxFormSelect>
            </NxFormGroup>
            <NxFormGroup label="Repository Base URL" sublabel="Example http://your-host:8081/" isRequired>
              <NxTextInput id="innersource-repository-configuration-base-url" {...baseUrlState} onChange={setBaseUrl} />
            </NxFormGroup>
            <NxFieldset label="Repository Authentication" isRequired>
              <NxRadio
                id="innersource-repository-configuration-anonymous-radio"
                name="repositoryAuthentication"
                value="0"
                isChecked={isAnonymous}
                onChange={() => setAnonymous(true)}
              >
                Allow Anonymous Access
              </NxRadio>
              <NxRadio
                id="innersource-repository-configuration-credentials-radio"
                name="repositoryAuthentication"
                value="1"
                isChecked={!isAnonymous}
                onChange={() => setAnonymous(false)}
              >
                Enter Username and Password
              </NxRadio>
            </NxFieldset>
            {!isAnonymous && (
              <div id="innersource-repository-configuration-authentication">
                <NxFormGroup label="Username" sublabel="Enter Username or Token User Code" isRequired>
                  <NxTextInput
                    id="innersource-repository-configuration-username"
                    {...usernameState}
                    onChange={setUsername}
                    autoComplete="username"
                  />
                </NxFormGroup>
                <NxFormGroup label="Password" sublabel="Enter Password or Token Pass Code" isRequired>
                  <NxTextInput
                    id="innersource-repository-configuration-password"
                    {...passwordState}
                    type="password"
                    onChange={setPassword}
                    autoComplete="new-password"
                  />
                </NxFormGroup>
              </div>
            )}
            <div className="nx-form-row">
              <NxButton
                type="button"
                onClick={hasAllRequiredData ? testConfiguration : null}
                id="innersource-repository-configuration-test-button"
                variant="tertiary"
                title={hasAllRequiredData ? null : MISSING_OR_INVALID_DATA_MESSAGE}
                className={classnames({ disabled: !hasAllRequiredData })}
              >
                Test Configuration
              </NxButton>
            </div>
            {testConfigurationSuccessful && <NxInfoAlert>Repository configuration test successful.</NxInfoAlert>}
            {testConfigurationError && (
              <LoadError
                titleMessage="Unable to connect to the configured repository."
                error={testConfigurationError}
                retryHandler={testConfiguration}
              />
            )}
          </div>
        </NxForm>
        {showDeleteModal && (
          <NxModal id="innersource-repository-configuration-delete-modal" onCancel={() => setShowDeleteModal(false)}>
            <NxForm
              onSubmit={deleteConfiguration}
              onCancel={() => setShowDeleteModal(false)}
              submitError={deleteConfigurationError}
              submitBtnText="OK"
              submitErrorTitleMessage="Unable to delete the configured repository."
              submitMaskState={deleteSubmitMaskState}
              submitMaskMessage={submitMaskMessage}
            >
              <header className="nx-modal-header">
                <h2 className="nx-h2">Delete Repository Configuration?</h2>
              </header>
              <div className="nx-modal-content">
                <NxWarningAlert>This will disable querying your configured repository.</NxWarningAlert>
              </div>
            </NxForm>
          </NxModal>
        )}
      </section>
    </main>
  );
}
