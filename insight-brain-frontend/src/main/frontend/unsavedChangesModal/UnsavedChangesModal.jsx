/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function UnsavedChangesModal({ onContinue, onClose }) {
  return (
    <NxModal id="unsaved-modal" variant="narrow" onCancel={onClose}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Unsaved Changes</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert className="nx-alert--modifier">
          <span>The page may contain unsaved changes; continuing will discard them.</span>
        </NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton onClick={onClose} id="unsaved-changes-modal-cancel-button">
            Cancel
          </NxButton>
          <NxButton variant="primary" id="unsaved-changes-modal-continue-button" onClick={onContinue}>
            Continue
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

UnsavedChangesModal.propTypes = {
  onClose: PropTypes.func,
  onContinue: PropTypes.func,
};
