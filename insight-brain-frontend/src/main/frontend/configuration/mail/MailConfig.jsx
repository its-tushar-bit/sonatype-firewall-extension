/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxModal,
  NxTextInput,
  NxTooltip,
  NxWarningAlert,
  NxInfoAlert,
  NxStatefulSubmitMask,
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import MaximizedContainer from '../../react/MaximizedContainer';

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
        sendTestEmail
      } = props,
      {
        loading,
        submitMaskState,
        submitMaskMessage,
        hasAllRequiredData,
        isDirty,
        isValid,
        error,
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
        isAuthorized
      } = props,
      isSubmitEnabled = hasAllRequiredData && isDirty && isValid && !mustReenterPassword;

  // Fetch Email Configuration when page is opened
  useEffect(() => { load(); }, []);

  function onSubmit(evt) {
    evt.preventDefault();

    if (isSubmitEnabled) {
      save();
    }
  }

  function field(fieldState, onChange, placeholder, id, label) {
    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <div className="nx-form-group">
        <label className="nx-label">
          <span className="nx-label__text">{label}</span>
          <NxTextInput { ...fieldState }
                       { ...({ onChange, placeholder, id }) }
                       className="nx-text-input nx-text-input--long"
                       autoComplete="new-password"/>
        </label>
      </div>
    );
  }

  const sslInput = (
    <NxCheckbox id="email-config-ssl-enabled"
                isChecked={sslEnabledState}
                onChange={() => setSslEnabled(!sslEnabledState)}>
      SSL Enabled
    </NxCheckbox>
  );

  const tlsInput = (
    <NxCheckbox id="email-config-starttls-enabled"
                isChecked={startTlsEnabledState}
                onChange={() => setStartTlsEnabled(!startTlsEnabledState)}>
      STARTTLS Enabled
    </NxCheckbox>
  );

  const modal = (
    <NxModal id="mail-config-delete-modal">
      <header className="nx-modal-header">
        <h2 className="nx-h2">Delete Email Configuration?</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert><span>This will disable all email notifications.</span></NxWarningAlert>
      </div>
      <footer className="nx-modal-footer">
        <div className="nx-btn-bar">
          <NxButton type="button"
                    id="mail-config-delete-ok"
                    onClick={del}
                    className="nx-btn nx-btn--primary">
            OK
          </NxButton>
          <NxButton type="button"
                    id="mail-config-delete-cancel"
                    onClick={() => setShowDeleteModal(false)}
                    className="nx-btn">
            Cancel
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );

  const saveButtonTooltipText = !hasAllRequiredData ? 'Hostname, Port and System Email are required details.'
    : 'Password must be provided when updating Hostname or Port.';

  // If required fields are not filled in, mention that
  // Otherwise either mention password is required (if it is), or no tooltip
  const sendTestEmailTooltipText = !hasAllRequiredData || !testEmailState.trimmedValue ?
    'Hostname, Port, System Email and Recipient address are required.' :
    mustReenterPassword ? 'Password must be provided when updating Hostname or Port.' : '';

  function sendTestEmailOnClickHandler() {
    if (isSendTestEmailEnabled()) {
      sendTestEmail();
    }
  }

  function isSendTestEmailEnabled() {
    return hasAllRequiredData && isValid && testEmailState.trimmedValue && !mustReenterPassword;
  }

  const form = (
    <form className="nx-form" onSubmit={onSubmit}>
      {/* Input Fields */}
      {field(hostnameState, setHostname, 'smtp.server.com', 'email-config-hostname', 'Hostname')}
      {field(portState, setPort, '465', 'email-config-port', 'Port')}
      {field(usernameState, setUsername, 'admin', 'email-config-username', 'Username')}

      <div className="nx-form-group">
        <label className="nx-label">
          <span className="nx-label__text">Password</span>
          {
            hasAllRequiredData && mustReenterPassword &&
            <span className="nx-sub-label">Must be re-entered when Hostname or Port is modified.</span>
          }
          <NxTextInput { ...passwordState }
                       id="email-config-password"
                       onChange={setPassword}
                       onFocus={evt => { evt.target.select(); }}
                       className="nx-text-input nx-text-input--long"
                       type="password"
                       autoComplete="new-password" />
        </label>
      </div>

      {field(systemEmailState, setSystemEmail, 'nexus@iqserver', 'email-config-systemEmail', 'System Email')}
      <fieldset className="nx-fieldset">
        <legend className="nx-label">Security Options</legend>
        {sslInput}
        {tlsInput}
      </fieldset>

      <hr />

      <div className="nx-form-row">
        <div className="nx-form-group">
          <label className="nx-label">
            <span className="nx-label__text">Test Configuration</span>
            <span className="nx-sub-label">Send a test email to verify the configuration.</span>
            <NxTextInput { ...testEmailState }
                         id="email-config-test-email-recipient"
                         onChange={setTestEmail}
                         onFocus={evt => { evt.target.select(); }}
                         className="nx-text-input nx-text-input--long"
                         autoComplete="new-password" />
          </label>
        </div>
        <div className="nx-form-group">
          <NxTooltip
              title={sendTestEmailTooltipText}>
            <NxButton type="button"
                      id="email-config-test-email-send"
                      onClick={sendTestEmailOnClickHandler}
                      className={classnames({disabled: !isSendTestEmailEnabled()})}>
              Send Test Email
            </NxButton>
          </NxTooltip>
        </div>
      </div>

      {/* Messages */}
      { error && <NxErrorAlert><span>{error}</span></NxErrorAlert> }
      { testEmailSent && <NxInfoAlert><span>A test email has been sent. Please check your mailbox.</span></NxInfoAlert>}

      {/* Buttons */}
      <div className='iq-tile-footer'>
        <div className="nx-btn-bar">
          <NxTooltip title={!isDirty || isSubmitEnabled ? '' : saveButtonTooltipText}>
            <NxButton type="submit"
                      className={classnames({ disabled: !isSubmitEnabled })}
                      id="email-config-save"
                      variant="primary">
              Save
            </NxButton>
          </NxTooltip>
          <NxButton type="button"
                    id="email-config-cancel"
                    onClick={resetForm}
                    disabled={!isDirty}>
            Cancel
          </NxButton>
          <NxButton type="button"
                    id="email-config-delete"
                    onClick={() => setShowDeleteModal(true)}
                    disabled={!serverData}>
            <NxFontAwesomeIcon icon={faTrashAlt}/>
            <span>Delete Configuration</span>
          </NxButton>
          { showDeleteModal && modal }
        </div>
      </div>
    </form>
  );

  return (
    <LoadWrapper loading={loading}>
      <MaximizedContainer id="mail-config-page-container" className="iq-body-container iq-body-container--single-pane">
        <div id="email-configuration" className="iq-tile iq-tile--sys-prefs">
          {isAuthorized &&
            <Fragment>
              <div className="iq-tile-header">
                <div className="iq-tile-header__title">
                  <h2>Email</h2>
                </div>
              </div>
              <div>
                <p>
                  To receive email notifications for events enter the details of your SMTP Server here.
                  For further details see the <a className={'iq-external-link'}
                                                 href='http://links.sonatype.com/products/nxiq/doc/email-configuration'>
                  documentation.&nbsp;<i className="fa fa-external-link fa-fw"/></a>
                </p>
                {submitMaskState !== null &&
                  <NxStatefulSubmitMask success={submitMaskState} message={submitMaskMessage} />}
                {form}
              </div>
            </Fragment>
          }
          {!isAuthorized &&
            <NxErrorAlert id="email-config-insufficient-permissions-error">
              <strong>Error</strong> It appears you do not have permission to access this page.
              If you believe this to be incorrect please contact your administrator.
            </NxErrorAlert>
          }
        </div>
      </MaximizedContainer>
    </LoadWrapper>
  );
}

const textInputPropType = PropTypes.shape({
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string])
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
  error: PropTypes.string,
  serverData: PropTypes.any,
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  testEmailState: textInputPropType.isRequired,
  sendTestEmail: PropTypes.func.isRequired,
  setTestEmail: PropTypes.func.isRequired,
  testEmailSent: PropTypes.bool,
  isAuthorized: PropTypes.bool.isRequired
};
