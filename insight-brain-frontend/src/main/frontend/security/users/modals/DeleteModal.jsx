/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxFontAwesomeIcon, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

export default function DeleteModal({ userId, username, deleteUser, deleteError, deleteMaskState, onCancel }) {
  return (
    <NxModal onCancel={onCancel} variant="narrow" id="delete-user-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={() => deleteUser(userId)}
        submitMaskState={deleteMaskState}
        onCancel={onCancel}
        submitBtnText="Continue"
        submitError={deleteError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete User</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>You are about to permanently remove {username}. This action cannot be undone.</NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

DeleteModal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  userId: PropTypes.string,
  username: PropTypes.string.isRequired,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
  deleteUser: PropTypes.func.isRequired,
};
