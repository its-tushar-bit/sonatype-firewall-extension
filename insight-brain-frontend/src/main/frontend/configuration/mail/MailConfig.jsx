/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {Fragment, useEffect} from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import {faTrashAlt} from '@fortawesome/pro-regular-svg-icons';
import {
  NxButton,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxInfoAlert,
  NxModal,
  NxSubmitMask,
  NxTextInput,
  NxTooltip,
  NxWarningAlert
} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import LoadError from '../../react/LoadError';
import MaximizedContainer from '../../react/MaximizedContainer';
import NxExternalLink from '../../react/NxExternalLink';

const authErrorMessage = 'It appears you do not have permission to access this page.  ' +
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
        sendTestEmail
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
        isAuthorized
      } = props,
      isSubmitEnabled = hasAllRequiredData && isDirty && isValid && !mustReenterPassword,
      loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Fetch Email Configuration when page is opened
  useEffect(() => { load(); }, []);

  function onSubmit(evt) {
    evt.preventDefault();

    if (isSubmitEnabled) {
      save();
    }
  }

  function field(fieldState, onChange, placeholder, id, label, optional = false, validatable = true) {
    const labelClasses = classnames('nx-label', { 'nx-label--optional': optional });

    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <div className="nx-form-group">
        <label className={labelClasses}>
          <span className="nx-label__text">{label}</span>
          <NxTextInput { ...fieldState }
                       { ...({ onChange, placeholder, id, validatable }) }
                       className="nx-text-input--long"
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
    <NxModal id="mail-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Delete Email Configuration?</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert><span>This will disable all email notifications.</span></NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton type="button"
                    id="mail-config-delete-cancel"
                    onClick={() => setShowDeleteModal(false)}
                    className="nx-btn">
            Cancel
          </NxButton>
          <NxButton type="button"
                    id="mail-config-delete-ok"
                    onClick={del}
                    className="nx-btn nx-btn--primary">
            OK
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

  const isSaveTooltipHidden = !isDirty || isSubmitEnabled;
  const form = (
    <Fragment>
      {/* Input Fields */}
      {field(hostnameState, setHostname, 'smtp.server.com', 'email-config-hostname', 'Hostname')}
      {field(portState, setPort, '465', 'email-config-port', 'Port')}
      {field(usernameState, setUsername, 'admin', 'email-config-username', 'Username', true, false)}

      <div className="nx-form-group">
        <label className="nx-label nx-label--optional">
          <span className="nx-label__text">Password</span>
          {
            hasAllRequiredData && mustReenterPassword &&
            <span className="nx-sub-label">Must be re-entered when Hostname or Port is modified.</span>
          }
          <NxTextInput { ...passwordState }
                       id="email-config-password"
                       onChange={setPassword}
                       onFocus={evt => { evt.target.select(); }}
                       className="nx-text-input--long"
                       type="password"
                       autoComplete="new-password" />
        </label>
      </div>

      {field(systemEmailState, setSystemEmail, 'nexus@iqserver', 'email-config-systemEmail', 'System Email')}
      <fieldset className="nx-fieldset">
        <legend className="nx-legend">
          <span className="nx-legend__text">Security Options</span>
        </legend>
        {sslInput}
        {tlsInput}
      </fieldset>

      <section className="nx-tile-subsection">
        <div className="nx-form-row">
          <div className="nx-form-group">
            <label className="nx-label">
              <span className="nx-label__text">Test Configuration</span>
              <span className="nx-sub-label">Send a test email to verify the configuration.</span>
              <NxTextInput { ...testEmailState }
                           id="email-config-test-email-recipient"
                           onChange={setTestEmail}
                           onFocus={evt => { evt.target.select(); }}
                           className="nx-text-input--long"
                           autoComplete="new-password" />
            </label>
          </div>
          <div className="nx-btn-bar">
            <NxTooltip title={sendTestEmailTooltipText || ''}>
              <NxButton type="button"
                        id="email-config-test-email-send"
                        onClick={sendTestEmailOnClickHandler}
                        className={classnames({disabled: !isSendTestEmailEnabled()})}>
                Send Test Email
              </NxButton>
            </NxTooltip>
          </div>
        </div>

        { testEmailSent && <NxInfoAlert>A test email has been sent. Please check your mailbox.</NxInfoAlert> }
        { testEmailError &&
          <LoadError titleMessage="Unabled to send test email." error={testEmailError} retryHandler={sendTestEmail} />
        }
      </section>

      {/* Buttons */}
      <footer className="nx-footer">
        { saveError &&
          <LoadError titleMessage="An error occurred while saving the configuration."
                     error={saveError}
                     retryHandler={onSubmit} />
        }
        { deleteError &&
          <LoadError titleMessage="An error occurred while deleting the configuration."
                     error={deleteError}
                     retryHandler={del} />
        }
        <div className="nx-btn-bar">
          <NxButton type="button"
                    id="email-config-delete"
                    onClick={() => setShowDeleteModal(true)}
                    disabled={!serverData}>
            <NxFontAwesomeIcon icon={faTrashAlt}/>
            <span>Delete Configuration</span>
          </NxButton>
          <NxButton type="button"
                    id="email-config-cancel"
                    onClick={resetForm}
                    disabled={!isDirty}>
            Cancel
          </NxButton>
          <NxTooltip title={isSaveTooltipHidden ? '' : saveButtonTooltipText}>
            <NxButton type="submit"
                      className={classnames({ disabled: !isSubmitEnabled })}
                      id="email-config-save"
                      variant="primary">
              Save
            </NxButton>
          </NxTooltip>
        </div>
      </footer>
      { showDeleteModal && modal }
    </Fragment>
  );

  return (
    <MaximizedContainer id="mail-config-page-container" className="nx-page-content">
      <main className="nx-page-main">
        <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
          <section id="email-configuration" className="nx-tile">
            <form className="nx-form" onSubmit={onSubmit}>
              <header className="nx-tile-header">
                <div className="nx-tile-header__title">
                  <h2 className="nx-h2">Email</h2>
                </div>
              </header>
              <p className="nx-p">
                To receive email notifications for events enter the details of your SMTP Server here.
                For further details see the{' '}
                <NxExternalLink href="http://links.sonatype.com/products/nxiq/doc/email-configuration">
                  documentation
                </NxExternalLink>.
              </p>
              {submitMaskState !== null && <NxSubmitMask success={submitMaskState} message={submitMaskMessage} />}
              {form}
            </form>
          </section>
        </LoadWrapper>
      </main>
    </MaximizedContainer>
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
  isAuthorized: PropTypes.bool.isRequired
};
