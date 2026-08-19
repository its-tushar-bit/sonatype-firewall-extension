/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxFontAwesomeIcon, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

export default function LdapRemoveServerModal({ ldapId, removeMaskState, removeError, closeModal, removeServer }) {
  return (
    <NxModal onClose={closeModal} variant="narrow" id="delete-user-modal">
      <NxStatefulForm
        onSubmit={() => removeServer(ldapId)}
        submitMaskState={removeMaskState}
        onCancel={closeModal}
        submitBtnText="Delete"
        submitError={removeError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Remove Server</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            Clicking &apos;delete&apos; will permanently remove this server and all data associated with it, including
            all data associated with the LDAP users in this configuration. This action cannot be undone. Are you sure
            you want to delete this server?
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

export const ldapRemoveServerModalPropTypes = {
  ldapId: PropTypes.string,
  removeMaskState: PropTypes.bool,
  removeError: PropTypes.string,
  removeServer: PropTypes.func.isRequired,
};

LdapRemoveServerModal.propTypes = {
  closeModal: PropTypes.func.isRequired,
  ...ldapRemoveServerModalPropTypes,
};
