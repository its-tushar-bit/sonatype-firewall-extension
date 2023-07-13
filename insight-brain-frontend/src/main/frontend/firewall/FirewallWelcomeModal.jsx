/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxModal } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function FirewallWelcomeModal(props) {
  const { close } = props;

  return (
    <NxModal id="firewall-welcome-modal" onClose={close}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Welcome to Sonatype Repository Firewall!</h2>
      </header>
      <div className="nx-modal-content">
        Firewall configuration will run in the background and populate data related to all your enabled repositories.
        The time taken to complete this process depends on the number of enabled repositories and size of individual
        repositories.
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton id="firewall-welcome-modal-close-btn" onClick={() => close()}>
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

FirewallWelcomeModal.propTypes = {
  close: PropTypes.func.isRequired,
};
