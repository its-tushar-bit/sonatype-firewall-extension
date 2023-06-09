/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxCodeSnippet, NxModal } from '@sonatype/react-shared-components';
import { MODAL_MODES } from './modalModes';

export default function CopyToClipboard({ username, newPassword, resetInitialNewPasswordValue, setMode }) {
  function onClose() {
    resetInitialNewPasswordValue();
    setMode(MODAL_MODES.DEFAULT);
  }

  return (
    <NxModal onClose={onClose} variant="narrow" className="iq-copy-password-modal" id="copy-password-modal">
      <NxStatefulForm className="nx-form" onSubmit={onClose} submitBtnText="Close">
        <header className="nx-modal-header">
          <h2 className="nx-h2">Reset Password</h2>
        </header>
        <div className="nx-modal-content">
          <NxCodeSnippet
            label="New Password"
            sublabel={`The new password for ${username} is below`}
            content={newPassword}
            inputProps={{ readOnly: true }}
          />
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

CopyToClipboard.propTypes = {
  setMode: PropTypes.func.isRequired,
  username: PropTypes.string.isRequired,
  newPassword: PropTypes.string,
  copy: PropTypes.func,
  resetInitialNewPasswordValue: PropTypes.func,
};
