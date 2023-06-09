/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { MODAL_MODES } from './modalModes';

export default function ResetPasswordModal({ userId, username, resetPassword, resetError, resetMaskState, setMode }) {
  return (
    <NxModal onClose={() => setMode(MODAL_MODES.DEFAULT)} variant="narrow" id="reset-password-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={() => resetPassword(userId, username)}
        submitMaskState={resetMaskState}
        onCancel={() => setMode(MODAL_MODES.DEFAULT)}
        submitBtnText="Reset"
        submitError={resetError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Reset Password</h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            Are you sure you want to reset the password for {username}? This action cannot be undone.
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

ResetPasswordModal.propTypes = {
  setMode: PropTypes.func.isRequired,
  userId: PropTypes.string,
  username: PropTypes.string.isRequired,
  resetPassword: PropTypes.func.isRequired,
  resetError: PropTypes.string,
  resetMaskState: PropTypes.bool,
};
