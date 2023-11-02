/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { pick } from 'ramda';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  NxStatefulForm,
  NxButton,
  NxModal,
  NxTextInput,
  NxWarningAlert,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

export default function ProxyConfig({
  load,
  save,
  del,
  resetForm,
  setHostname,
  setPort,
  setUsername,
  setPassword,
  setExcludeHosts,
  loadLicenced,
  loading,
  submitMaskState,
  deleteMaskState,
  hasAllRequiredData,
  isDirty,
  isValid,
  loadError,
  saveError,
  deleteError,
  serverData,
  hostnameState,
  portState,
  usernameState,
  passwordState,
  excludeHostsState,
  mustReenterPassword,
  licensed,
  stateGo,
}) {
  useEffect(() => {
    init();
  }, []);

  function init() {
    load();
    loadLicenced();
  }

  const [showModal, setShowModal] = useState(false);

  function field(fieldState, onChange, placeholder, id, label, required = true, validatable = true) {
    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <NxFormGroup isRequired={required} label={label}>
        <NxTextInput
          {...fieldState}
          {...{ onChange, placeholder, id, validatable }}
          className="nx-text-input--long"
          autoComplete="new-password"
        />
      </NxFormGroup>
    );
  }

  function modalCloseHandler() {
    setShowModal(false);
  }

  const deleteModal = (
    <NxModal id="proxy-config-delete-modal" onClose={modalCloseHandler} variant="narrow">
      <NxStatefulForm
        className="nx-form"
        onSubmit={() => del(modalCloseHandler)}
        onCancel={modalCloseHandler}
        submitBtnText="OK"
        submitError={deleteError}
        submitMaskState={deleteMaskState}
        submitMaskMessage="Deleting…"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Proxy Configuration?</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>This will remove the configured proxy.</NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );

  const getValidationErrors = () => {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    }

    const isSubmitEnabled = hasAllRequiredData && isDirty && isValid && !mustReenterPassword;
    if (isSubmitEnabled) {
      return null;
    }

    if (!hasAllRequiredData || !isValid) {
      return 'Hostname and Port are required details.';
    } else {
      return 'Password must be provided when updating Hostname or Port.';
    }
  };

  return (
    <main id="proxy-config-container" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Proxy</h1>
      </div>
      <section id="proxy-configuration" className="nx-tile">
        <NxStatefulForm
          onSubmit={save}
          loadError={loadError}
          loading={loading}
          doLoad={init}
          submitMaskState={submitMaskState}
          submitMaskMessage="Saving…"
          submitBtnText="Save"
          submitError={saveError}
          validationErrors={getValidationErrors()}
          onCancel={resetForm}
          additionalFooterBtns={
            <Fragment>
              <NxButton
                type="button"
                id="proxy-config-delete"
                onClick={() => setShowModal(true)}
                disabled={!serverData}
              >
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Configuration</span>
              </NxButton>
            </Fragment>
          }
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Proxy</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <p className="nx-p">To use a Proxy Server for outbound requests, configure it here.</p>
            {/* This page is accessible without a license, so that users can configure their Proxy Servers */}
            {/* before attempting to install a license. If they are accessing this page without a license */}
            {/* most likely they want to navigate to license install page next. */}
            {!licensed && (
              <p id="proxy-config-product-license-navigation" className="nx-p">
                Continue installing your license{' '}
                <NxTextLink onClick={() => stateGo('productlicense')}>here.</NxTextLink>
              </p>
            )}
            {field(hostnameState, setHostname, 'proxy.server', 'proxy-config-hostname', 'Hostname')}
            {field(portState, setPort, '8080', 'proxy-config-port', 'Port')}
            {field(usernameState, setUsername, 'admin', 'proxy-config-username', 'Username', false, false)}
            <NxFormGroup
              className="nx-label"
              label="Password"
              sublabel={
                hasAllRequiredData && mustReenterPassword ? 'Must be re-entered when Hostname or Port is modified.' : ''
              }
            >
              <NxTextInput
                {...passwordState}
                id="proxy-config-password"
                onChange={setPassword}
                onFocus={(evt) => {
                  evt.target.select();
                }}
                className="nx-text-input--long"
                type="password"
                autoComplete="new-password"
              />
            </NxFormGroup>
            <NxFormGroup className="nx-label" label="Exclude Hosts" sublabel="Must be comma delimited.">
              <NxTextInput
                {...excludeHostsState}
                id="proxy-config-exclude-hosts"
                onChange={setExcludeHosts}
                className="nx-text-input--long"
                type="textarea"
              />
            </NxFormGroup>
          </div>
        </NxStatefulForm>
      </section>
      {showModal && deleteModal}
    </main>
  );
}

const textInputPropType = PropTypes.shape(pick(['value', 'isPristine', 'validationErrors'], NxTextInput.propTypes));

ProxyConfig.propTypes = {
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  del: PropTypes.func.isRequired,
  loadLicenced: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setPort: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setExcludeHosts: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
  deleteMaskState: PropTypes.bool,
  hostnameState: textInputPropType.isRequired,
  portState: textInputPropType.isRequired,
  usernameState: textInputPropType.isRequired,
  passwordState: textInputPropType.isRequired,
  excludeHostsState: textInputPropType.isRequired,
  hasAllRequiredData: PropTypes.bool.isRequired,
  isDirty: PropTypes.bool.isRequired,
  isValid: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  deleteError: PropTypes.string,
  serverData: PropTypes.any,
  mustReenterPassword: PropTypes.bool.isRequired,
  licensed: PropTypes.bool.isRequired,
  stateGo: PropTypes.func.isRequired,
};
