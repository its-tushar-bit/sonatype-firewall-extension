/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxModal } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function InnerSourceProducerPermissionsModal({ applicationName, onClose }) {
  return (
    <NxModal id="innersource-producer-insufficient-permissions-modal" onClose={onClose}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Insufficient Permissions</h2>
      </header>
      <div className="nx-modal-content">
        Insufficient permissions to view the report for <strong>{applicationName}</strong>. Please contact your Policy
        Administrator or an Owner to request access.
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton id="innersource-producer-insufficient-permissions-modal-close" onClick={onClose}>
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

InnerSourceProducerPermissionsModal.propTypes = {
  applicationName: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
