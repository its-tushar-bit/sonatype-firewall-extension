/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { includes } from 'ramda';
import {
  NxCheckbox,
  NxFieldset,
  NxForm,
  NxFormGroup,
  NxInfoAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';
import BackButton from '../../../react/BackButton';
import { useRouterState } from '../../../react/RouterStateContext';

function EditWebhook({
  isLoading,
  loadError,
  saveError,
  submitMaskState,
  availableEventTypes,
  selectedEventTypes,
  loadWebhookData,
  toggleEventType,
  setUrl,
  setDescription,
  setSecretKey,
  saveWebhook,
  stateGo,
  isAppWebhooksSupported,
  inputFields,
}) {
  useEffect(() => {
    loadWebhookData();
  }, []);

  const uiRouterState = useRouterState();

  const { url, description, secretKey } = inputFields;

  function renderCheckbox(eventType) {
    const id = eventType.split(' ').join('-');
    const isSelected = includes(eventType, selectedEventTypes);
    const isDisabled = eventType === 'Application Evaluation' && !isAppWebhooksSupported;

    return (
      <NxCheckbox
        key={id}
        checkboxId={id}
        isChecked={isSelected}
        onChange={() => toggleEventType(eventType)}
        disabled={isDisabled}
      >
        {eventType}
      </NxCheckbox>
    );
  }

  const formValidationMessage = url.trimmedValue ? url.validationErrors : 'Webhook URL is a required field';

  return (
    <main className="nx-page-main" id="webhook-editor">
      <BackButton stateName="webhooks.list" $state={uiRouterState} />
      <section className="nx-tile">
        <NxForm
          onSubmit={saveWebhook}
          submitBtnText="Create"
          submitMaskState={submitMaskState}
          submitError={saveError}
          validationErrors={formValidationMessage}
          onCancel={() => stateGo('webhooks.list')}
          loadError={loadError}
          loading={isLoading}
          doLoad={loadWebhookData}
        >
          <div className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Create Webhook</h2>
            </div>
          </div>
          <div className="nx-tile-content">
            <NxFormGroup label="Webhook URL" sublabel="to send the POST request" isRequired={true}>
              <NxTextInput
                {...url}
                onChange={setUrl}
                validatable={true}
                id="editor-webhook-url"
                className="nx-text-input--long"
                maxLength="2048"
                autoFocus
              />
            </NxFormGroup>
            <NxFormGroup label="Webhook description" sublabel="a description for your webhook used in the UI">
              <NxTextInput
                {...description}
                onChange={setDescription}
                id="editor-webhook-description"
                className="nx-text-input--long"
                maxLength="2048"
              />
            </NxFormGroup>
            <NxFormGroup label="Secret Key" sublabel="used for the HMAC payload digest">
              <NxTextInput
                {...secretKey}
                onChange={setSecretKey}
                id="editor-webhook-secret-key"
                className="nx-text-input--long"
                maxLength="512"
                type="password"
              />
            </NxFormGroup>
            {!isAppWebhooksSupported && (
              <NxInfoAlert id="application-evaluation-disabled-message">
                Webhooks with Application Evaluation event types are not supported by your license.
              </NxInfoAlert>
            )}
            <NxFieldset id="event-types" label="Event Types" sublabel="which trigger this Webhook">
              {availableEventTypes.map(renderCheckbox)}
            </NxFieldset>
          </div>
        </NxForm>
      </section>
    </main>
  );
}

export default EditWebhook;

const userInputPropType = PropTypes.shape({
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string]),
});

EditWebhook.propTypes = {
  isLoading: PropTypes.bool,
  isAppWebhooksSupported: PropTypes.bool,
  submitMaskState: PropTypes.bool,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  availableEventTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedEventTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
  loadWebhookData: PropTypes.func.isRequired,
  toggleEventType: PropTypes.func.isRequired,
  setUrl: PropTypes.func.isRequired,
  setDescription: PropTypes.func.isRequired,
  setSecretKey: PropTypes.func.isRequired,
  saveWebhook: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  inputFields: PropTypes.shape({
    url: userInputPropType,
    description: userInputPropType,
    secretKey: userInputPropType,
  }),
};
