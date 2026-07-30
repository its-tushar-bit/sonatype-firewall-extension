/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { includes } from 'ramda';
import {
  useToggle,
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxInfoAlert,
  NxModal,
  NxTextInput,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { isNilOrEmpty } from '../../../util/jsUtil';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';
import { validateUrlIsHttp } from 'MainRoot/configuration/webhook/webhookActions';

function EditWebhook({
  isLoading,
  isDirty,
  isUrlHTTP,
  tenantMode,
  loadError,
  saveError,
  updateMaskState,
  deleteMaskState,
  availableEventTypes,
  selectedEventTypes,
  loadWebhookPage,
  toggleEventType,
  setUrl,
  setDescription,
  setSecretKey,
  saveWebhook,
  deleteWebhook,
  deleteError,
  stateGo,
  isAppWebhooksSupported,
  inputFields,
  router,
}) {
  const {
    currentParams: { webhookId },
  } = router;
  const isSelfHosted = tenantMode !== 'multi-tenant';

  const createMode = isNilOrEmpty(webhookId);

  useEffect(() => {
    loadWebhookPage(webhookId);
  }, []);

  const [showModal, toggleShowModal] = useToggle(false);
  const [showHttpUrlWarningModal, toggleShowHttpUrlWarningModal] = useToggle(false);

  const { url, description, secretKey } = inputFields;

  // Derive Firewall context from product features set by fetchProductFeatures.
  // State-name detection (startsWith 'firewall.') fails for NOUX routes where
  // the state is 'editWebhook', not 'firewall.editWebhook'.
  const isFirewallContext = !isAppWebhooksSupported;

  // Helper function to normalize event type for comparison
  // Converts Firewall display names back to stored names for matching
  function normalizeEventTypeForComparison(eventType) {
    if (isFirewallContext) {
      if (eventType === 'Container Evaluation') {
        return 'Application Evaluation';
      }
      if (eventType === 'Organization and Repository Management') {
        return 'Organization and Application Management';
      }
    }
    return eventType;
  }

  function renderCheckbox(eventType) {
    const id = eventType.split(' ').join('-');
    // Normalize the available event type (display name) to stored name for comparison
    // selectedEventTypes already contains stored names from the database, so don't normalize them
    const normalizedEventType = normalizeEventTypeForComparison(eventType);
    const isSelected = includes(normalizedEventType, selectedEventTypes);

    return (
      <NxCheckbox key={id} checkboxId={id} isChecked={isSelected} onChange={() => toggleEventType(normalizedEventType)}>
        {eventType}
      </NxCheckbox>
    );
  }

  const getValidation = () => {
    if (!isDirty && !createMode) {
      return MSG_NO_CHANGES_TO_UPDATE;
    }

    return url.trimmedValue ? url.validationErrors : 'Webhook URL is a required field';
  };

  const onSubmitForm = () => {
    if (isSelfHosted && validateUrlIsHttp(url.trimmedValue)) {
      toggleShowHttpUrlWarningModal();
      return false;
    } else {
      return saveWebhook();
    }
  };

  const subLabelUrlInputGroupFormElement = (
    <>
      <span>to send the POST request</span>
      {isUrlHTTP && isSelfHosted && (
        <NxInfoAlert id="editor-webhook-url-http-alert">
          HTTPS is recommended because it is more secure than HTTP
        </NxInfoAlert>
      )}
    </>
  );

  return (
    <Fragment>
      <main className="nx-page-main" id="webhook-editor">
        <MenuBarBackButton stateName="listWebhooks" />
        <div className="nx-page-title">
          <h1 className="nx-h1">{createMode ? 'Create' : 'Edit'} Webhook</h1>
        </div>
        <section className="nx-tile">
          <NxStatefulForm
            onSubmit={onSubmitForm}
            submitBtnText={createMode ? 'Create' : 'Update'}
            submitMaskState={updateMaskState}
            submitError={saveError}
            validationErrors={getValidation()}
            onCancel={() => stateGo('listWebhooks')}
            loadError={loadError}
            loading={isLoading}
            doLoad={() => loadWebhookPage(webhookId)}
            id="webhook-form"
          >
            <div className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Webhook Details</h2>
              </div>
              {!createMode && (
                <div className="nx-tile__actions">
                  <NxButton type="button" variant="tertiary" onClick={toggleShowModal} id="delete-webhook-button">
                    <NxFontAwesomeIcon icon={faTrashAlt} />
                    <span>Delete Webhook</span>
                  </NxButton>
                </div>
              )}
            </div>
            <div className="nx-tile-content">
              <NxFormGroup label="Webhook URL" sublabel={subLabelUrlInputGroupFormElement} isRequired={true}>
                <NxTextInput
                  {...url}
                  onChange={setUrl}
                  validatable={true}
                  id="editor-webhook-url"
                  className="nx-text-input--long"
                  inputAttributes={{ maxLength: '2048', autoFocus: true }}
                />
              </NxFormGroup>
              <NxFormGroup label="Webhook description" sublabel="a description for your webhook used in the UI">
                <NxTextInput
                  {...description}
                  onChange={setDescription}
                  id="editor-webhook-description"
                  className="nx-text-input--long"
                  inputAttributes={{ maxLength: '2048', autoComplete: 'new-password' }}
                />
              </NxFormGroup>
              <NxFormGroup label="Secret Key" sublabel="used for the HMAC payload digest">
                <NxTextInput
                  {...secretKey}
                  onChange={setSecretKey}
                  id="editor-webhook-secret-key"
                  className="nx-text-input--long"
                  type="password"
                  inputAttributes={{ maxLength: '512', autoComplete: 'new-password' }}
                />
              </NxFormGroup>
              <NxFieldset id="event-types" label="Event Types" sublabel="which trigger this Webhook">
                {availableEventTypes.sort().map(renderCheckbox)}
              </NxFieldset>
            </div>
          </NxStatefulForm>
        </section>
      </main>
      {showModal && (
        <NxModal onClose={toggleShowModal} variant="narrow" id="delete-modal">
          <NxStatefulForm
            className="nx-form"
            onSubmit={() => deleteWebhook(webhookId)}
            submitMaskState={deleteMaskState}
            onCancel={toggleShowModal}
            submitBtnText="Continue"
            submitError={deleteError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Webhook</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                You are about to permanently remove webhook for {url.value}. This action cannot be undone.
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
      {showHttpUrlWarningModal && (
        <NxModal onClose={toggleShowHttpUrlWarningModal} variant="narrow" id="http-url-warning-modal">
          <NxStatefulForm
            id="http-url-warning-modal-form"
            className="nx-form"
            onSubmit={saveWebhook}
            submitMaskState={updateMaskState}
            onCancel={toggleShowHttpUrlWarningModal}
            submitBtnText="Continue"
            submitError={saveError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">
                <span>HTTP Warning</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                Using HTTP URLS for webhooks is less secure than HTTPS. Would you like to continue?
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </Fragment>
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
  isDirty: PropTypes.bool,
  isUrlHTTP: PropTypes.bool,
  tenantMode: PropTypes.string,
  isAppWebhooksSupported: PropTypes.bool,
  updateMaskState: PropTypes.bool,
  deleteMaskState: PropTypes.bool,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  deleteError: PropTypes.string,
  availableEventTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedEventTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
  loadWebhookPage: PropTypes.func.isRequired,
  toggleEventType: PropTypes.func.isRequired,
  setUrl: PropTypes.func.isRequired,
  setDescription: PropTypes.func.isRequired,
  setSecretKey: PropTypes.func.isRequired,
  saveWebhook: PropTypes.func.isRequired,
  deleteWebhook: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  inputFields: PropTypes.shape({
    url: userInputPropType,
    description: userInputPropType,
    secretKey: userInputPropType,
  }),
  router: PropTypes.shape({
    currentParams: PropTypes.oneOfType([
      PropTypes.shape({
        webhookId: PropTypes.string,
      }),
      PropTypes.object,
    ]),
  }),
};
