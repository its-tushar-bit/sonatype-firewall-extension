/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxErrorAlert,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxSuccessAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

export default function CheckLogin({
  closeModal,
  username,
  password,
  setInputField,
  checkLogin,
  ldapId,
  checkLoginError,
  checkLoginSuccess,
  clearCheckLoginAlerts,
  resetCheckLoginModal,
}) {
  const formValidationError =
    username.validationErrors || password.validationErrors
      ? 'Unable to submit: fields with invalid or missing data.'
      : null;

  useEffect(() => {
    return () => {
      resetCheckLoginModal();
    };
  }, []);

  return (
    <NxModal onClose={closeModal} variant="narrow" onCancel={closeModal} autoComplete="off" id="ldap-check-login-modal">
      <NxStatefulForm
        onCancel={closeModal}
        submitBtnText="Check Login"
        onSubmit={() => checkLogin(ldapId)}
        validationErrors={formValidationError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Check Login</h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Username" isRequired>
            <NxTextInput
              {...username}
              onChange={(value) => setInputField({ field: 'username', value })}
              autoComplete="new-username"
              id="check-login-username"
            />
          </NxFormGroup>
          <NxFormGroup label="Password" isRequired>
            <NxTextInput
              {...password}
              onChange={(value) => setInputField({ field: 'password', value })}
              type="password"
              autoComplete="new-password"
              id="check-login-password"
            />
          </NxFormGroup>
          {checkLoginError && <NxErrorAlert onClose={clearCheckLoginAlerts}>{checkLoginError}</NxErrorAlert>}
          {checkLoginSuccess && <NxSuccessAlert onClose={clearCheckLoginAlerts}>Success!</NxSuccessAlert>}
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

CheckLogin.propTypes = {
  closeModal: PropTypes.func.isRequired,
  username: PropTypes.object.isRequired,
  password: PropTypes.object.isRequired,
  setInputField: PropTypes.func.isRequired,
  checkLogin: PropTypes.func.isRequired,
  clearCheckLoginAlerts: PropTypes.func.isRequired,
  ldapId: PropTypes.string.isRequired,
  checkLoginError: PropTypes.string,
  checkLoginSuccess: PropTypes.bool,
  resetCheckLoginModal: PropTypes.func.isRequired,
};
