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
  NxErrorAlert,
  NxModal,
  NxTextInput,
  NxTooltip,
  NxWarningAlert,
  NxStatefulSubmitMask,
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import MaximizedContainer from '../../react/MaximizedContainer';

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
        setExcludeHosts
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
        excludeHostsState,
        mustReenterPassword,
        isAuthorized,
        licensed,
        $state
      } = props,
      isSubmitEnabled = hasAllRequiredData && isDirty && isValid && !mustReenterPassword,
      productLicenseUrl = $state.href($state.get('productlicense'));

  // Fetch Proxy Configuration when page is opened
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

  const deleteModal = (
    <NxModal id="proxy-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Delete Proxy Configuration?</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert>This will remove the configured proxy.</NxWarningAlert>
      </div>
      <footer className="nx-modal-footer">
        <div className="nx-btn-bar">
          <NxButton type="button"
                    id="proxy-config-delete-ok"
                    onClick={del}
                    className="nx-btn nx-btn--primary">
            OK
          </NxButton>
          <NxButton type="button"
                    id="proxy-config-delete-cancel"
                    onClick={() => setShowDeleteModal(false)}
                    className="nx-btn">
            Cancel
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );

  const tooltipText = !hasAllRequiredData || !isValid ? 'Hostname and Port are required details.'
    : 'Password must be provided when updating Hostname or Port.';

  const saveButton =
    <NxButton type="submit"
              className={classnames({ disabled: !isSubmitEnabled })}
              id="proxy-config-save"
              variant="primary">
      Save
    </NxButton>;

  const isTooltipHidden = !isDirty || isSubmitEnabled;
  const form = (
    <form className="nx-form" onSubmit={onSubmit}>
      {/* Input Fields */}
      {field(hostnameState, setHostname, 'proxy.server', 'proxy-config-hostname', 'Hostname')}
      {field(portState, setPort, '8080', 'proxy-config-port', 'Port')}
      {field(usernameState, setUsername, 'admin', 'proxy-config-username', 'Username')}
      <div className="nx-form-group">
        <label className="nx-label">
          <span className="nx-label__text">Password</span>
          {
            hasAllRequiredData && mustReenterPassword &&
            <span className="nx-sub-label">Must be re-entered when Hostname or Port is modified.</span>
          }
          <NxTextInput { ...passwordState }
                       id="proxy-config-password"
                       onChange={setPassword}
                       onFocus={evt => { evt.target.select(); }}
                       className="nx-text-input nx-text-input--long"
                       type="password"
                       autoComplete="new-password" />
        </label>
      </div>
      <div className="nx-form-group">
        <label className="nx-label">
          <span className="nx-label__text">Exclude Hosts</span>
          <span className="nx-sub-label">Must be comma delimited.</span>
          <NxTextInput { ...excludeHostsState }
                       id="proxy-config-exclude-hosts"
                       onChange={setExcludeHosts}
                       className="nx-text-input nx-text-input--long"
                       type="textarea"/>
        </label>
      </div>
      {/* Messages */}
      { error && <NxErrorAlert>{error}</NxErrorAlert> }
      {/* Buttons */}
      <div className="iq-tile-footer">
        <div className="nx-btn-bar">
          {isTooltipHidden ? saveButton :
          <NxTooltip id="save-button-tooltip" title={tooltipText}>
            {saveButton}
          </NxTooltip>
          }
          <NxButton type="button"
                    id="proxy-config-cancel"
                    onClick={resetForm}
                    disabled={!isDirty}>
            Cancel
          </NxButton>
          <NxButton type="button"
                    id="proxy-config-delete"
                    onClick={() => setShowDeleteModal(true)}
                    disabled={!serverData}>
            <NxFontAwesomeIcon icon={faTrashAlt}/>
            <span>Delete Configuration</span>
          </NxButton>
          { showDeleteModal && deleteModal }
        </div>
      </div>
    </form>
  );

  return (
    <LoadWrapper loading={loading}>
      <MaximizedContainer id="proxy-config-container" className="iq-body-container iq-body-container--single-pane">
        <div id="proxy-configuration" className="iq-tile iq-tile--sys-prefs">
          {isAuthorized &&
            <Fragment>
              <div className="iq-tile-header">
                <div className="iq-tile-header__title">
                  <h2>Proxy</h2>
                </div>
              </div>
              <div>
                <p>
                  To use a Proxy Server for outbound requests, configure it here.
                </p>
                {/* This page is accessible without a license, so that users can configure their Proxy Servers */}
                {/* before attempting to install a license. If they are accessing this page without a license */}
                {/* most likely they want to navigate to license install page next. */}
                {!licensed &&
                  <p id="proxy-config-product-license-navigation">
                    Continue installing your license <a href={productLicenseUrl}>here.</a>
                  </p>
                }
                {submitMaskState !== null &&
                  <NxStatefulSubmitMask success={submitMaskState} message={submitMaskMessage}/>}
                {form}
              </div>
            </Fragment>
          }
          {!isAuthorized &&
            <NxErrorAlert id="proxy-config-insufficient-permissions-error">
              <strong>Error</strong> It appears you do not have permission to access this page.
              If you believe this to be incorrect please contact your administrator.
            </NxErrorAlert>
          }
        </div>
      </MaximizedContainer>
    </LoadWrapper>
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
  error: PropTypes.string,
  serverData: PropTypes.any,
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  isAuthorized: PropTypes.bool.isRequired,
  licensed: PropTypes.bool.isRequired,
  $state: PropTypes.object.isRequired
};
