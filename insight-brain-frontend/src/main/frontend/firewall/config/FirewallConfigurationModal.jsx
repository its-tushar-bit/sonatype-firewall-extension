/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {NxFieldset, NxForm, NxModal, NxToggle} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function FirewallConfigurationModal(props) {
  // Actions
  const {
    toggleAutoUnquarantineEnabled,
    saveConfiguration,
    loadConfiguration,
    closeConfigurationModal
  } = props;

  //viewState
  const {
    sumbitMaskSuccessState,
    saveConfigurationError,
    isDirty
  } = props;

  //formState
  const {
    autoUnquarantineEnabled
  } = props;

  //autoUnquarantineState
  const {
    loadedConfiguration,
    loadConfigurationError
  } = props;

  return (
    <NxModal id="firewall-configuration-modal" onClose={closeConfigurationModal}>
      <NxForm onSubmit={saveConfiguration}
              loadError={loadConfigurationError}
              loading={!loadedConfiguration}
              doLoad={loadConfiguration}
              submitMaskMessage="Saving…"
              submitError={saveConfigurationError}
              submitMaskState={sumbitMaskSuccessState}
              submitBtnText="Save Changes"
              validationErrors={isDirty ? undefined : 'There are no changes to save.'}
              onCancel={closeConfigurationModal}>
        <header className="nx-modal-header">
          <h2 className="nx-h2">Auto Release From Quarantine Configuration</h2>
        </header>
        <div className="nx-modal-content">
          <NxFieldset isRequired
                      label="Auto Unquarantine"
                      sublabel="When Auto Unquarantine is set to active, quarantined components will be automatically
                                released when they are confirmed not malicious.">
            <NxToggle id="auto-unquarantine-toggle"
                      inputId="auto-unquarantine-check"
                      onChange={toggleAutoUnquarantineEnabled}
                      isChecked={autoUnquarantineEnabled}>
              Release Integrity Policy
            </NxToggle>
          </NxFieldset>
        </div>
      </NxForm>
    </NxModal>
  );
}

FirewallConfigurationModal.propTypes = {
  loadedConfiguration: PropTypes.bool.isRequired,
  loadConfigurationError: PropTypes.object,
  loadConfiguration: PropTypes.func.isRequired,
  autoUnquarantineEnabled: PropTypes.bool.isRequired,
  sumbitMaskSuccessState: PropTypes.bool,
  saveConfiguration: PropTypes.func.isRequired,
  saveConfigurationError: PropTypes.string,
  toggleAutoUnquarantineEnabled: PropTypes.func.isRequired,
  closeConfigurationModal: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired
};
