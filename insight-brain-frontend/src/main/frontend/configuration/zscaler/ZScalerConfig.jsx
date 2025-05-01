/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  hasValidationErrors,
  NxButton,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxH2,
  NxModal,
  NxP,
  NxPageMain,
  NxStatefulForm,
  NxTextInput,
  NxTextLink,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { reject, isNil } from 'ramda';
import LoadError from '../../react/LoadError';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const authErrorMessage =
  'It appears you do not have permission to access this page. ' +
  'If you believe this to be incorrect please contact your administrator.';

export default function ZScalerConfig(props) {
  const { load, save, del, resetForm, setUsername, setPassword, setHostname, setApiKey, setShowDeleteModal } = props,
    {
      loading,
      submitMaskState,
      submitMaskMessage,
      hasAllRequiredData,
      isDirty,
      loadError: loadErrorProp,
      saveError,
      deleteError,
      serverData,
      showDeleteModal,
      hostnameState,
      usernameState,
      passwordState,
      apiKeyState,
      mustReenterPassword,
      isAuthorized,
    } = props,
    loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Fetch ZScaler Configuration when page is opened
  useEffect(() => {
    load();
  }, []);

  function warningMessage() {
    return 'This action cannot be undone. Are you sure you want to delete this configuration?';
  }

  function field(fieldState, onChange, placeholder, id, label, optional = false, validatable = true) {
    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <NxFormGroup label={label} isRequired={!optional}>
        <NxTextInput
          {...fieldState}
          {...{ onChange, placeholder, id, validatable }}
          className="nx-text-input--long"
          autoComplete="new-password"
        />
      </NxFormGroup>
    );
  }

  const modal = (
    <NxModal id="mail-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <NxStatefulForm
        onSubmit={del}
        onCancel={() => setShowDeleteModal(false)}
        submitBtnText="OK"
        submitError={deleteError}
      >
        <NxModal.Header>
          <NxH2>Delete zScaler Configuration?</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            <span>{warningMessage()}</span>
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );

  const formValidationErrors = reject(isNil, [
    mustReenterPassword ? 'Password must be provided when updating Username, Hostname or ApiKey.' : null,
    hasAllRequiredData ? null : 'Username, Password, Hostname and ApiKey are required details.',
    isDirty ? null : MSG_NO_CHANGES_TO_SAVE,
  ]);

  /*
   * Do not pass the submitError to the form when there are validation errors.
   * This is a workaround for a likely bug in RSC where it prefers to show the submit error over the
   * validation error when both are present, while it really should probably do the opposite. If the
   * submit error is displayed when there are also validation errors, the Retry button in the submit
   * error does nothing
   */
  const submitError = hasValidationErrors(formValidationErrors) ? null : saveError;

  const passwordSublabel =
    hasAllRequiredData && mustReenterPassword ? 'Must be re-entered when any fields are modified.' : null;

  const cancelAndDeleteBtns = (
    <>
      <NxButton
        type="button"
        id="zscaler-config-delete"
        onClick={() => setShowDeleteModal(true)}
        disabled={!serverData}
      >
        <NxFontAwesomeIcon icon={faTrashAlt} />
        <span>Delete Configuration</span>
      </NxButton>
      <NxButton type="button" id="zscaler-config-cancel" onClick={resetForm} disabled={!isDirty}>
        Cancel
      </NxButton>
    </>
  );

  return (
    <NxPageMain id="zscaler-config-page-container">
      <NxTile id="zscaler-configuration">
        <NxStatefulForm
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={save}
          submitBtnText="Save"
          submitError={submitError}
          validationErrors={submitMaskMessage !== 'Deleting' ? formValidationErrors : null}
          // If there is a validationError alert, it's cleared on "Delete Configuration"
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          additionalFooterBtns={cancelAndDeleteBtns}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>zScaler Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              To protect users at the network level, integrate our data with your zScaler infrastructure. For further
              details see the{' '}
              <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/zscaler-configuration">
                documentation
              </NxTextLink>
              .
            </NxP>
            {/* Input Fields */}
            {field(usernameState, setUsername, 'user', 'zscaler-config-username', 'Username')}
            <NxFormGroup label="Password" sublabel={passwordSublabel} isRequired>
              <NxTextInput
                {...passwordState}
                id="zscaler-config-password"
                onChange={setPassword}
                onFocus={(evt) => {
                  evt.target.select();
                }}
                className="nx-text-input--long"
                type="password"
                autoComplete="new-password"
                validatable
              />
            </NxFormGroup>

            {field(hostnameState, setHostname, 'https://zsapi.zscalertwo.net', 'zscaler-config-hostname', 'Hostname')}
            {field(apiKeyState, setApiKey, '465', 'zscaler-config-api-key', 'ApiKey')}
          </NxTile.Content>
        </NxStatefulForm>
        {showDeleteModal && modal}
      </NxTile>
    </NxPageMain>
  );
}

const textInputPropType = PropTypes.shape({
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string]),
});

ZScalerConfig.propTypes = {
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  del: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setApiKey: PropTypes.func.isRequired,
  setShowDeleteModal: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
  hostnameState: textInputPropType.isRequired,
  apiKeyState: textInputPropType.isRequired,
  usernameState: textInputPropType.isRequired,
  passwordState: textInputPropType.isRequired,
  hasAllRequiredData: PropTypes.bool.isRequired,
  isDirty: PropTypes.bool.isRequired,
  isValid: PropTypes.bool.isRequired,
  loadError: LoadError.propTypes.error,
  saveError: LoadError.propTypes.error,
  deleteError: LoadError.propTypes.error,
  serverData: PropTypes.any,
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  isAuthorized: PropTypes.bool.isRequired,
};
