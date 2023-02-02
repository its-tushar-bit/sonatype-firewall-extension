/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxFieldset, NxStatefulForm, NxModal, NxToggle } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { INTEGRITY_RATING_POLICY_TYPE_ID } from './firewallConfigurationModalReducer';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function FirewallConfigurationModal(props) {
  // Actions
  const { toggleAutoUnquarantineEnabled, saveConfiguration, loadConfiguration, closeConfigurationModal } = props;

  //viewState
  const { submitMaskSuccessState, saveConfigurationError, isDirty } = props;

  //formState
  const { conditionTypes } = props;

  //autoUnquarantineState
  const { loadedConfiguration, loadConfigurationError } = props;

  const getIntegrityRatingIndex = () => {
    return conditionTypes.findIndex((element) => element.id === INTEGRITY_RATING_POLICY_TYPE_ID);
  };

  return (
    <NxModal id="firewall-configuration-modal" onClose={closeConfigurationModal}>
      <NxStatefulForm
        onSubmit={saveConfiguration}
        loadError={loadConfigurationError}
        loading={!loadedConfiguration}
        doLoad={loadConfiguration}
        submitMaskMessage="Saving…"
        submitError={saveConfigurationError}
        submitMaskState={submitMaskSuccessState}
        submitBtnText="Save Changes"
        validationErrors={isDirty ? undefined : MSG_NO_CHANGES_TO_SAVE}
        onCancel={closeConfigurationModal}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Auto Release From Quarantine Configuration</h2>
        </header>
        <div className="nx-modal-content">
          <NxFieldset
            isRequired
            label="Auto Release From Quarantine"
            sublabel="When Auto Release from Quarantine is set to active, quarantined components will be
                      automatically released when they are confirmed not malicious."
          >
            <NxToggle
              id="auto-unquarantine-toggle-integrity-rating"
              className="nx-toggle--no-gap"
              onChange={() => toggleAutoUnquarantineEnabled(INTEGRITY_RATING_POLICY_TYPE_ID)}
              isChecked={conditionTypes[getIntegrityRatingIndex()].autoReleaseQuarantineEnabled}
            >
              {conditionTypes[getIntegrityRatingIndex()].name} policy condition type
            </NxToggle>
          </NxFieldset>
          <NxFieldset
            isRequired
            label="Auto Release Additional Policy Condition Types"
            sublabel="By default Auto Release from Quarantine will be ‘on’ for components with the policy
                      condition type Integrity Rating. All policy condition types can be turned to ‘on’ or ‘off’
                      individually."
          >
            <div id="auto-release-condition-toggles">
              {conditionTypes
                .filter((condition) => condition.id !== INTEGRITY_RATING_POLICY_TYPE_ID)
                .map((condition, index) => (
                  <NxToggle
                    onChange={() => toggleAutoUnquarantineEnabled(condition.id)}
                    isChecked={condition.autoReleaseQuarantineEnabled}
                    key={index}
                  >
                    {condition.name}
                  </NxToggle>
                ))}
            </div>
          </NxFieldset>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

FirewallConfigurationModal.propTypes = {
  loadedConfiguration: PropTypes.bool.isRequired,
  loadConfigurationError: PropTypes.object,
  loadConfiguration: PropTypes.func.isRequired,
  conditionTypes: PropTypes.array.isRequired,
  submitMaskSuccessState: PropTypes.bool,
  saveConfiguration: PropTypes.func.isRequired,
  saveConfigurationError: PropTypes.string,
  toggleAutoUnquarantineEnabled: PropTypes.func.isRequired,
  closeConfigurationModal: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
};
