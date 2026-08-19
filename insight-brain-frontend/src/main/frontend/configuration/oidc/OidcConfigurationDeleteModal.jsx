/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { NxModal, NxStatefulForm, NxWarningAlert, NxFontAwesomeIcon } from '@sonatype/react-shared-components';

export default function OidcConfigurationDeleteModal({ deleteConfiguration, toggleDeleteModal }) {
  return (
    <NxModal
      id="oidc-config-delete-modal"
      onClose={toggleDeleteModal}
      variant="narrow"
      aria-labelledby="oidc-delete-label-modal"
    >
      <NxStatefulForm onSubmit={deleteConfiguration} onCancel={toggleDeleteModal} submitBtnText="OK">
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="oidc-delete-label-modal">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete OIDC Configuration?</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            This will remove the configured OIDC authentication. Users will need to authenticate using alternative
            methods after deletion.
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

OidcConfigurationDeleteModal.propTypes = {
  deleteConfiguration: PropTypes.func.isRequired,
  toggleDeleteModal: PropTypes.func.isRequired,
};
