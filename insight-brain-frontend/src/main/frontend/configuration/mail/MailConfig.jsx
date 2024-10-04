/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  hasValidationErrors,
  NxButton,
  NxButtonBar,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxH2,
  NxInfoAlert,
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
  'It appears you do not have permission to access this page.  ' +
  'If you believe this to be incorrect please contact your administrator.';

export default function MailConfig(props) {
  const {
      load,
      save,
      del,
      resetForm,
      setHostname,
      setPort,
      setUsername,
      setPassword,
      setSslEnabled,
      setStartTlsEnabled,
      setSystemEmail,
      setShowDeleteModal,
      setTestEmail,
      sendTestEmail,
    } = props,
    {
      loading,
      submitMaskState,
      submitMaskMessage,
      hasAllRequiredData,
      isDirty,
      isValid,
      loadError: loadErrorProp,
      saveError,
      deleteError,
      testEmailError,
      serverData,
      showDeleteModal,
      hostnameState,
      portState,
      usernameState,
      passwordState,
      sslEnabledState,
      startTlsEnabledState,
      systemEmailState,
      mustReenterPassword,
      testEmailState,
      testEmailSent,
      isAuthorized,
      isEmailStopped,
    } = props,
    loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Fetch Email Configuration when page is opened
  useEffect(() => {
    load();
  }, []);

  function warningMessage() {
    if (isEmailStopped) {
      return 'This will disable all email notifications.';
    }
    return "This will fall back to using Sonatype's mail service";
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

  const sslInput = (
    <NxCheckbox
      id="email-config-ssl-enabled"
      isChecked={sslEnabledState}
      onChange={() => setSslEnabled(!sslEnabledState)}
    >
      SSL Enabled
    </NxCheckbox>
  );

  const tlsInput = (
    <NxCheckbox
      id="email-config-starttls-enabled"
      isChecked={startTlsEnabledState}
      onChange={() => setStartTlsEnabled(!startTlsEnabledState)}
    >
      STARTTLS Enabled
    </NxCheckbox>
  );

  const modal = (
    <NxModal id="mail-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <NxStatefulForm
        onSubmit={del}
        onCancel={() => setShowDeleteModal(false)}
        submitBtnText="OK"
        submitError={deleteError}
      >
        <NxModal.Header>
          <NxH2>Delete Email Configuration?</NxH2>
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
    mustReenterPassword ? 'Password must be provided when updating Hostname or Port.' : null,
    hasAllRequiredData ? null : 'Hostname, Port and System Email are required details.',
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

  // If required fields are not filled in, mention that
  // Otherwise either mention password is required (if it is), or no tooltip
  const sendTestEmailTooltipText =
    !hasAllRequiredData || !testEmailState.trimmedValue
      ? 'Hostname, Port, System Email and Recipient address are required.'
      : mustReenterPassword
      ? 'Password must be provided when updating Hostname or Port.'
      : '';

  function sendTestEmailOnClickHandler() {
    if (isSendTestEmailEnabled()) {
      sendTestEmail();
    }
  }

  function isSendTestEmailEnabled() {
    return hasAllRequiredData && isValid && testEmailState.trimmedValue && !mustReenterPassword;
  }

  const passwordSublabel =
    hasAllRequiredData && mustReenterPassword ? 'Must be re-entered when Hostname or Port is modified.' : null;

  const cancelAndDeleteBtns = (
    <>
      <NxButton type="button" id="email-config-delete" onClick={() => setShowDeleteModal(true)} disabled={!serverData}>
        <NxFontAwesomeIcon icon={faTrashAlt} />
        <span>Delete Configuration</span>
      </NxButton>
      <NxButton type="button" id="email-config-cancel" onClick={resetForm} disabled={!isDirty}>
        Cancel
      </NxButton>
    </>
  );
  return (
    <NxPageMain id="mail-config-page-container">
      <NxTile id="email-configuration">
        <NxStatefulForm
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={save}
          submitBtnText="Save"
          submitError={submitError}
          validationErrors={submitMaskMessage !== 'Deleting' ? formValidationErrors : null}
          // If an there is a validationError alert, it's cleared on "Delete Configuration"
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          additionalFooterBtns={cancelAndDeleteBtns}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Email</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              To receive email notifications for events enter the details of your SMTP Server here. For further details
              see the{' '}
              <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/email-configuration">
                documentation
              </NxTextLink>
              .
            </NxP>
            {/* Input Fields */}
            {field(hostnameState, setHostname, 'smtp.server.com', 'email-config-hostname', 'Hostname')}
            {field(portState, setPort, '465', 'email-config-port', 'Port')}
            {field(usernameState, setUsername, 'admin', 'email-config-username', 'Username', true, false)}

            <NxFormGroup label="Password" sublabel={passwordSublabel}>
              <NxTextInput
                {...passwordState}
                id="email-config-password"
                onChange={setPassword}
                onFocus={(evt) => {
                  evt.target.select();
                }}
                className="nx-text-input--long"
                type="password"
                autoComplete="new-password"
              />
            </NxFormGroup>

            {field(systemEmailState, setSystemEmail, 'nexus@iqserver', 'email-config-systemEmail', 'System Email')}
            <NxFieldset label="Security Options">
              {sslInput}
              {tlsInput}
            </NxFieldset>

            <NxTile.Subsection>
              <NxFormRow>
                <NxFormGroup label="Test Configuration" sublabel="Send a test email to verify the configuration.">
                  <NxTextInput
                    {...testEmailState}
                    id="email-config-test-email-recipient"
                    onChange={setTestEmail}
                    onFocus={(evt) => {
                      evt.target.select();
                    }}
                    className="nx-text-input--long"
                    autoComplete="new-password"
                  />
                </NxFormGroup>
                <NxButtonBar>
                  <NxButton
                    title={sendTestEmailTooltipText || ''}
                    type="button"
                    id="email-config-test-email-send"
                    onClick={sendTestEmailOnClickHandler}
                    className={classnames({ disabled: !isSendTestEmailEnabled() })}
                  >
                    Send Test Email
                  </NxButton>
                </NxButtonBar>
              </NxFormRow>

              {testEmailSent && <NxInfoAlert>A test email has been sent. Please check your mailbox.</NxInfoAlert>}
              {testEmailError && (
                <LoadError
                  titleMessage="Unabled to send test email."
                  error={testEmailError}
                  retryHandler={sendTestEmail}
                />
              )}
            </NxTile.Subsection>
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

MailConfig.propTypes = {
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  del: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setPort: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setSslEnabled: PropTypes.func.isRequired,
  setStartTlsEnabled: PropTypes.func.isRequired,
  setSystemEmail: PropTypes.func.isRequired,
  setShowDeleteModal: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
  hostnameState: textInputPropType.isRequired,
  portState: textInputPropType.isRequired,
  usernameState: textInputPropType.isRequired,
  passwordState: textInputPropType.isRequired,
  systemEmailState: textInputPropType.isRequired,
  sslEnabledState: PropTypes.bool.isRequired,
  startTlsEnabledState: PropTypes.bool.isRequired,
  hasAllRequiredData: PropTypes.bool.isRequired,
  isDirty: PropTypes.bool.isRequired,
  isValid: PropTypes.bool.isRequired,
  loadError: LoadError.propTypes.error,
  saveError: LoadError.propTypes.error,
  deleteError: LoadError.propTypes.error,
  testEmailError: LoadError.propTypes.error,
  serverData: PropTypes.any,
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  testEmailState: textInputPropType.isRequired,
  sendTestEmail: PropTypes.func.isRequired,
  setTestEmail: PropTypes.func.isRequired,
  testEmailSent: PropTypes.bool,
  isAuthorized: PropTypes.bool.isRequired,
  isEmailStopped: PropTypes.bool.isRequired,
};
