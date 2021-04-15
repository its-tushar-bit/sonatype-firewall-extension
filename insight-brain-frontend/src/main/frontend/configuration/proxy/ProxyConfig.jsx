/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, Fragment } from 'react';
import { pick } from 'ramda';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  NxButton,
  NxModal,
  NxTextInput,
  NxTooltip,
  NxWarningAlert,
  NxSubmitMask,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import LoadError from '../../react/LoadError';

const authErrorMessage = `It appears you do not have permission to access this page.
    If you believe this to be incorrect, please contact your administrator.`;

export default function ProxyConfig(props) {
  const {
      load,
      save,
      del,
      resetForm,
      setHostname,
      setPort,
      setUsername,
      setPassword,
      setShowDeleteModal,
      setExcludeHosts,
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
      serverData,
      showDeleteModal,
      hostnameState,
      portState,
      usernameState,
      passwordState,
      excludeHostsState,
      mustReenterPassword,
      isAuthorized,
      licensed,
      $state,
    } = props,
    isSubmitEnabled = hasAllRequiredData && isDirty && isValid && !mustReenterPassword,
    productLicenseUrl = $state.href($state.get('productlicense')),
    loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Fetch Proxy Configuration when page is opened
  useEffect(() => {
    load();
  }, []);

  function onSubmit(evt) {
    if (evt) {
      evt.preventDefault();
    }

    if (isSubmitEnabled) {
      save();
    }
  }

  function field(fieldState, onChange, placeholder, id, label, optional = false, validatable = true) {
    const labelClasses = classnames('nx-label', {
      'nx-label--optional': optional,
    });

    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <div className="nx-form-group">
        <label className={labelClasses}>
          <span className="nx-label__text">{label}</span>
          <NxTextInput
            {...fieldState}
            {...{ onChange, placeholder, id, validatable }}
            className="nx-text-input--long"
            autoComplete="new-password"
          />
        </label>
      </div>
    );
  }

  const deleteModal = (
    <NxModal id="proxy-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Delete Proxy Configuration?</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert>This will remove the configured proxy.</NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton
            type="button"
            id="proxy-config-delete-cancel"
            onClick={() => setShowDeleteModal(false)}
            className="nx-btn"
          >
            Cancel
          </NxButton>
          <NxButton type="button" id="proxy-config-delete-ok" onClick={del} className="nx-btn nx-btn--primary">
            OK
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );

  const tooltipText =
    !isDirty || isSubmitEnabled
      ? ''
      : !hasAllRequiredData || !isValid
      ? 'Hostname and Port are required details.'
      : 'Password must be provided when updating Hostname or Port.';

  const form = (
    <Fragment>
      {/* Input Fields */}
      {field(hostnameState, setHostname, 'proxy.server', 'proxy-config-hostname', 'Hostname')}
      {field(portState, setPort, '8080', 'proxy-config-port', 'Port')}
      {field(usernameState, setUsername, 'admin', 'proxy-config-username', 'Username', true, false)}
      <div className="nx-form-group">
        <label className="nx-label nx-label--optional">
          <span className="nx-label__text">Password</span>
          {hasAllRequiredData && mustReenterPassword && (
            <span className="nx-sub-label">Must be re-entered when Hostname or Port is modified.</span>
          )}
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
        </label>
      </div>
      <div className="nx-form-group">
        <label className="nx-label nx-label--optional">
          <span className="nx-label__text">Exclude Hosts</span>
          <span className="nx-sub-label">Must be comma delimited.</span>
          <NxTextInput
            {...excludeHostsState}
            id="proxy-config-exclude-hosts"
            onChange={setExcludeHosts}
            className="nx-text-input--long"
            type="textarea"
          />
        </label>
      </div>
      {/* Buttons */}
      <footer className="nx-footer">
        {saveError && (
          <LoadError
            titleMessage="An error occurred while saving the configuration."
            error={saveError}
            retryHandler={onSubmit}
          />
        )}
        {deleteError && (
          <LoadError
            titleMessage="An error occurred while deleting the configuration."
            error={deleteError}
            retryHandler={del}
          />
        )}
        <div className="nx-btn-bar">
          <NxButton
            type="button"
            id="proxy-config-delete"
            onClick={() => setShowDeleteModal(true)}
            disabled={!serverData}
          >
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Configuration</span>
          </NxButton>
          <NxButton type="button" id="proxy-config-cancel" onClick={resetForm} disabled={!isDirty}>
            Cancel
          </NxButton>
          <NxTooltip id="save-button-tooltip" title={tooltipText}>
            <NxButton
              type="submit"
              className={classnames({ disabled: !isSubmitEnabled })}
              id="proxy-config-save"
              variant="primary"
            >
              Save
            </NxButton>
          </NxTooltip>
        </div>
      </footer>
      {showDeleteModal && deleteModal}
    </Fragment>
  );

  return (
    <main id="proxy-config-container" className="nx-page-main">
      <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <form className="nx-form" onSubmit={onSubmit}>
          <section id="proxy-configuration" className="nx-tile">
            <header className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Proxy</h2>
              </div>
            </header>
            <div className="nx-tile-content">
              <p className="nx-p">To use a Proxy Server for outbound requests, configure it here.</p>
              {/* This page is accessible without a license, so that users can configure their Proxy Servers */}
              {/* before attempting to install a license. If they are accessing this page without a license */}
              {/* most likely they want to navigate to license install page next. */}
              {!licensed && (
                <p id="proxy-config-product-license-navigation" className="nx-p">
                  Continue installing your license <a href={productLicenseUrl}>here.</a>
                </p>
              )}
              {submitMaskState !== null && <NxSubmitMask success={submitMaskState} message={submitMaskMessage} />}
              {form}
            </div>
          </section>
        </form>
      </LoadWrapper>
    </main>
  );
}

const textInputPropType = PropTypes.shape(pick(['value', 'isPristine', 'validationErrors'], NxTextInput.propTypes));

ProxyConfig.propTypes = {
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  del: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setPort: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setExcludeHosts: PropTypes.func.isRequired,
  setShowDeleteModal: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
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
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  isAuthorized: PropTypes.bool.isRequired,
  licensed: PropTypes.bool.isRequired,
  $state: PropTypes.object.isRequired,
};
