/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxToggle, NxButton, NxTextInput, NxFormGroup } from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const notValidErrorMessage = 'Notice Text cannot be blank';

const getValidationErrors = ({ message, enabled, isDirty }) => {
  if (!message.trimmedValue.length && enabled) {
    return notValidErrorMessage;
  }
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_UPDATE;
  }

  return null;
};

export default function SystemNoticeConfiguration(props) {
  const { load, update, toggleIsEnabled, setMessage, resetForm } = props;
  const { loading, isDirty, loadError, updateError, submitMaskState } = props;
  const { enabled, message } = props;

  useEffect(() => {
    load();
  }, []);

  return (
    <main className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">System Notice</h1>
      </div>
      <section id="system-notice-configuration" className="nx-tile">
        <NxStatefulForm
          onSubmit={update}
          loadError={loadError}
          loading={loading}
          doLoad={load}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Update"
          validationErrors={getValidationErrors(props)}
          additionalFooterBtns={
            <NxButton type="button" id="system-notice-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure System Notice</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <p id="system-notice-explanation" className="nx-p">
              Here you can enter a notice that will be displayed in a prominent manner on all pages of IQ Server, as
              well as the login prompt. The notice will be visible to all users and cannot be dismissed. You might want
              to use this feature for company policy or legal compliance reasons. The &apos;Notice Display&apos; toggle
              allows you to hide or show the notice.
            </p>
            <NxFormGroup label="Notice Text" isRequired>
              <NxTextInput
                type="textarea"
                className="nx-text-input--long"
                id="system-notice-text"
                aria-required={true}
                onChange={setMessage}
                maxLength="500"
                rows="10"
                {...message}
              />
            </NxFormGroup>
            <NxToggle
              id="system-notice-display-toggle-checkbox"
              className="nx-toggle--no-gap"
              onChange={toggleIsEnabled}
              isChecked={enabled}
            >
              Enable Notice Display
            </NxToggle>
          </div>
        </NxStatefulForm>
      </section>
    </main>
  );
}

SystemNoticeConfiguration.propTypes = {
  load: PropTypes.func.isRequired,
  update: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  enabled: PropTypes.bool.isRequired,
  message: PropTypes.shape({
    value: PropTypes.string,
    trimmedValue: PropTypes.string,
    isPristine: PropTypes.bool,
  }),
  toggleIsEnabled: PropTypes.func.isRequired,
  setMessage: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  updateError: PropTypes.string,
  submitMaskState: PropTypes.bool,
};
