/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxH1,
  NxH2,
  NxModal,
  NxPageMain,
  NxPageTitle,
  NxStatefulForm,
  NxTextInput,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

import { actions } from './baseUrlConfigurationSlice';
import {
  selectFormState,
  selectLoading,
  selectSubmitMaskState,
  selectSubmitMaskMessage,
  selectIsDirty,
  selectDeleteMaskState,
  selectLoadError,
  selectUpdateError,
  selectDeleteError,
  selectHasAllRequiredFields,
  selectServerData,
  selectShowDeleteModal,
} from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

export default function BaseUrlConfiguration() {
  const dispatch = useDispatch();
  const { baseUrl } = useSelector(selectFormState);
  const loading = useSelector(selectLoading);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const submitMaskMessage = useSelector(selectSubmitMaskMessage);
  const isDirty = useSelector(selectIsDirty);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const loadError = useSelector(selectLoadError);
  const updateError = useSelector(selectUpdateError);
  const deleteError = useSelector(selectDeleteError);
  const hasAllRequiredFields = useSelector(selectHasAllRequiredFields);
  const serverData = useSelector(selectServerData);
  const showDeleteModal = useSelector(selectShowDeleteModal);
  const updateConf = () => dispatch(actions.update());
  const deleteConf = () => dispatch(actions.del());
  const loadConf = () => dispatch(actions.load());
  const resetConfForm = () => dispatch(actions.resetForm());
  const setBaseUrl = (value) => dispatch(actions.setInputValueBaseUrl(value));
  const setShowDeleteModal = (value) => dispatch(actions.setShowDeleteModal(value));

  useEffect(() => {
    loadConf();
  }, []);

  function modalCloseHandler() {
    setShowDeleteModal(false);
  }

  const getValidationErrors = () => {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    } else if (!hasAllRequiredFields) {
      return 'Base URL is required data.';
    }
  };

  const deleteModal = (
    <NxModal
      id="base-url-config-delete-modal"
      onClose={modalCloseHandler}
      variant="narrow"
      aria-labelledby="base-url-delete-label-modal"
    >
      <NxStatefulForm
        onSubmit={deleteConf}
        onCancel={modalCloseHandler}
        submitBtnText="OK"
        submitError={deleteError}
        submitMaskState={deleteMaskState}
        submitMaskMessage={submitMaskMessage}
      >
        <NxModal.Header>
          <NxH2 id="base-url-delete-label-modal">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Base URL Configuration?</span>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>This will remove the configured base URL.</NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );

  return (
    <NxPageMain>
      <NxPageTitle>
        <NxH1 className="nx-h1">Base URL</NxH1>
      </NxPageTitle>
      <NxTile id="base-url-config-form">
        <NxStatefulForm
          submitBtnClasses="iq-base-url-configuration-save-button"
          loading={loading}
          doLoad={loadConf}
          loadError={loadError}
          onSubmit={updateConf}
          submitMaskMessage={submitMaskMessage}
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Save Configuration"
          validationErrors={getValidationErrors()}
          additionalFooterBtns={
            <>
              <NxButton
                type="button"
                variant="tertiary"
                id="base-url-config-delete-button"
                onClick={() => setShowDeleteModal(true)}
                disabled={!serverData?.baseUrl}
              >
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Configuration</span>
              </NxButton>
              <NxButton type="button" id="base-url-cancel" onClick={resetConfForm} disabled={!isDirty}>
                Cancel
              </NxButton>
            </>
          }
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Configure Base URL</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>
              This setting is required for features such as email, SCM, and Jira integration
            </NxTile.HeaderSubtitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxFormGroup label="Base URL" sublabel="Example http://nexus-iq-server.example.com/" isRequired>
              <NxTextInput
                {...baseUrl}
                onChange={setBaseUrl}
                id="config-base-url"
                validatable={true}
                placeholder={'Entry'}
              />
            </NxFormGroup>
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
      {showDeleteModal && deleteModal}
    </NxPageMain>
  );
}
