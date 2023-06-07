/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxModal } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import { actions } from './firewallOnboardingSlice';
import { selectIncompleteConfigurationModal } from './firewallOnboardingSelectors';

export default function IncompleteConfigurationModal() {
  const { showModal, href } = useSelector(selectIncompleteConfigurationModal);

  const dispatch = useDispatch();
  const closeModal = () => dispatch(actions.closeIncompleteConfigurationModal());

  const handleExit = () => {
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
            malicious code or dependency confusion threats until Repository Firewall has been configured. If you exit,
            any changes you have made will be discarded. You can restart and complete the configuration process at a
            later time by reloading Repository Firewall.
          </p>
          <p>Would you like to continue configuring Repository Firewall?</p>
        </div>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <NxButton id="exit-configuration-button" onClick={handleExit}>
              Exit
            </NxButton>
            <NxButton id="continue-configuration-button" variant="primary" onClick={closeModal}>
              Continue
            </NxButton>
          </div>
        </footer>
      </NxModal>
    )
  );
}
