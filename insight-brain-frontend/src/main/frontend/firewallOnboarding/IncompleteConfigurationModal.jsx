/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import { actions } from './firewallOnboardingSlice';
import { selectIncompleteConfigurationModal } from './firewallOnboardingSelectors';

export default function IncompleteConfigurationModal() {
  const { showModal, href } = useSelector(selectIncompleteConfigurationModal);

  const dispatch = useDispatch();
  const closeModal = () => dispatch(actions.closeIncompleteConfigurationModal());

  const exitOnboarding = () => {
    if (href) {
      location.href = href;
      closeModal();
    }
  };

  return (
    showModal && (
      <NxModal id="incomplete-configuration-modal" onClose={closeModal}>
        <header className="nx-modal-header">
          <h2 className="nx-h2">Repository Firewall has not been configured</h2>
        </header>
        <div className="nx-modal-content">
          <p>
            You have not completed the Repository Firewall configuration. Your environment will not be protected from
            malicious code or dependency confusion threats until Repository Firewall has been configured. You can
            restart and complete the configuration process at a later time by reloading Repository Firewall.
          </p>
          <NxWarningAlert>If you continue, any changes you have made will be discarded.</NxWarningAlert>
        </div>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <NxButton id="incomplete-configuration-modal-cancel-button" onClick={closeModal}>
              Cancel
            </NxButton>
            <NxButton id="incomplete-configuration-modal-continue-button" variant="primary" onClick={exitOnboarding}>
              Continue
            </NxButton>
          </div>
        </footer>
      </NxModal>
    )
  );
}
