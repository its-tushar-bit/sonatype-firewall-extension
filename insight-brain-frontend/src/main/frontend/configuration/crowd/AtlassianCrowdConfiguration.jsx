/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxStatefulForm,
  NxButton,
  NxFontAwesomeIcon,
  NxTextInput,
  NxModal,
  NxWarningAlert,
  NxSuccessAlert,
  NxFormGroup,
  NxPageTitle,
  NxTile,
  NxLoadError,
  NxPageMain,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

import { actions } from './atlassianCrowdConfigurationSlice';
import {
  selectFormState,
  selectLoading,
  selectSubmitMaskState,
  selectSubmitMaskMessage,
  selectIsDirty,
  selectDeleteMaskState,
  selectTestError,
  selectLoadError,
  selectUpdateError,
  selectDeleteError,
  selectHasAllRequiredData,
  selectMustReenterPassword,
  selectTestSuccessMessage,
  selectServerData,
  selectShowModal,
} from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSelectors';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

export default function AtlassianCrowdConfiguration() {
  const dispatch = useDispatch();
  const { serverUrl, applicationName, applicationPassword } = useSelector(selectFormState);
  const loading = useSelector(selectLoading);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const submitMaskMessage = useSelector(selectSubmitMaskMessage);
  const isDirty = useSelector(selectIsDirty);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const testError = useSelector(selectTestError);
  const loadError = useSelector(selectLoadError);
  const updateError = useSelector(selectUpdateError);
  const deleteError = useSelector(selectDeleteError);
  const hasAllRequiredData = useSelector(selectHasAllRequiredData);
  const mustReenterPassword = useSelector(selectMustReenterPassword);
  const testSuccessMessage = useSelector(selectTestSuccessMessage);
  const serverData = useSelector(selectServerData);
  const showModal = useSelector(selectShowModal);
  const updateConf = () => dispatch(actions.update());
  const deleteConf = () => dispatch(actions.del());
  const testConf = () => dispatch(actions.test());
  const loadConf = () => dispatch(actions.load());
  const resetConfForm = () => dispatch(actions.resetForm());
  const setServerUrl = (value) => dispatch(actions.setInputValueServerUrl(value));
  const setApplicationName = (value) => dispatch(actions.setInputValueApplicationName(value));
  const setApplicationPassword = (value) => dispatch(actions.setInputValueApplicationPassword(value));
  const resetTestAlertMessages = () => dispatch(actions.resetTestAlertMessages());
  const setShowModal = (value) => dispatch(actions.setShowModal(value));

  useEffect(() => {
    loadConf();
  }, []);

  function modalCloseHandler() {
    setShowModal(false);
  }

  const getValidationErrors = () => {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    } else if (mustReenterPassword) {
      return 'Application Password must be provided when updating Server URL.';
    } else if (!hasAllRequiredData) {
      return 'Server URL, Application Name and Application Password are required data.';
    }
  };

  const generateSublabelPassword = () =>
    hasAllRequiredData &&
    mustReenterPassword && (
      <span className="nx-sub-label iq-password-sub-label">Must be re-entered when Server URL is modified.</span>
    );

  const deleteModal = (
    <NxModal
      id="crowd-config-delete-modal"
      onClose={modalCloseHandler}
      variant="narrow"
      aria-labelledby="crowd-delete-label-modal"
    >
      <NxStatefulForm
        onSubmit={() => deleteConf()}
        onCancel={modalCloseHandler}
        submitBtnText="OK"
        submitError={deleteError}
        submitMaskState={deleteMaskState}
        submitMaskMessage={submitMaskMessage}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="crowd-delete-label-modal">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Crowd Configuration?</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>This will remove the configured Crowd server data.</NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );

  return (
    <NxPageMain>
      <NxPageTitle>
        <h1 className="nx-h1">Atlassian Crowd</h1>
      </NxPageTitle>
      <NxTile id="crowd-config-form">
        <NxStatefulForm
          submitBtnClasses="iq-crowd-configuration-save-button"
          loading={loading}
          doLoad={loadConf}
          onCancel={resetConfForm}
          loadError={loadError}
          onSubmit={updateConf}
          submitMaskMessage={submitMaskMessage}
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Save Configuration"
          validationErrors={getValidationErrors()}
          additionalFooterBtns={
            <NxButton
              type="button"
              variant="tertiary"
              id="crowd-config-delete-button"
              onClick={() => setShowModal(true)}
              disabled={!serverData}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
              <span>Delete Configuration</span>
            </NxButton>
          }
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <h2 className="nx-h2">Configure Atlassian Crowd Connection</h2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxFormGroup label="Crowd Server URL" sublabel="Example http://your-host:8095/crowd" isRequired>
              <NxTextInput {...serverUrl} onChange={setServerUrl} id="crowd-config-server-url" validatable={true} />
            </NxFormGroup>
            <NxFormGroup label="Crowd Application Name" isRequired>
              <NxTextInput
                {...applicationName}
                onChange={setApplicationName}
                id="crowd-config-app-name"
                validatable={true}
              />
            </NxFormGroup>
            <NxFormGroup label="Crowd Application Password" sublabel={generateSublabelPassword()} isRequired>
              <NxTextInput
                {...applicationPassword}
                id="crowd-config-app-password"
                onChange={setApplicationPassword}
                onFocus={(evt) => {
                  evt.target.select();
                }}
                type="password"
                autoComplete="new-password"
                validatable={true}
              />
            </NxFormGroup>
            <div className="nx-form-row">
              <NxButton
                type="button"
                id="test-crowd-configuration"
                variant="tertiary"
                onClick={testConf}
                disabled={!hasAllRequiredData}
              >
                Test Configuration
              </NxButton>
            </div>
            {testSuccessMessage && (
              <NxSuccessAlert onClose={resetTestAlertMessages}>{testSuccessMessage}</NxSuccessAlert>
            )}
            {testError && (
              <NxLoadError
                id="crowd-test-connection-error"
                titleMessage="An error occurred testing the connection."
                error={testError}
                retryHandler={testConf}
              />
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
      {showModal && deleteModal}
    </NxPageMain>
  );
}
