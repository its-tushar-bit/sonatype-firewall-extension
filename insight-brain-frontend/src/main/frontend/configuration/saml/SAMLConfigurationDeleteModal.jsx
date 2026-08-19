/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxModal, NxFontAwesomeIcon, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrash } from '@fortawesome/pro-solid-svg-icons';

export default function SAMLConfigurationDeleteModal({ deleteConfiguration, toggleDeleteModal }) {
  const contentMessage =
    'Clicking "delete" will permanently remove this SAML configuration from the system. Are you sure you want to delete it?';

  return (
    <NxModal
      id="saml-configuration-delete-modal"
      variant="narrow"
      aria-labelledby="saml-configuration-delete-modal-heading"
      onCancel={toggleDeleteModal}
    >
      <header className="nx-modal-header">
        <h2 className="nx-h2" id="saml-configuration-delete-modal-heading">
          <NxFontAwesomeIcon icon={faTrash} />
          <span>Delete Configuration</span>
        </h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert>{contentMessage}</NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton onClick={toggleDeleteModal}>Cancel</NxButton>
          <NxButton variant="primary" onClick={deleteConfiguration}>
            Delete
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

SAMLConfigurationDeleteModal.propTypes = {
  deleteConfiguration: PropTypes.func.isRequired,
  toggleDeleteModal: PropTypes.func.isRequired,
};
