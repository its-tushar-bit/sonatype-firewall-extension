/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  NxModal,
  NxStatefulForm,
  NxFormGroup,
  NxTextInput,
  NxErrorAlert,
  combineValidationErrors,
  hasValidationErrors,
} from '@sonatype/react-shared-components';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { validateNonEmpty } from '../../../util/validationUtil';
import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

export const MISSING_FIELDS_ERROR_MESSAGE = 'Required Fields Missing';
export const CONFIRMATION_MISMATCH_ERROR_MESSAGE = 'New Password and Confirmation must match';

export const ChangePasswordModal = ({ onClose, onChangePassword, changePasswordError, changePasswordStatus }) => {
  const [password, setPassword] = useState(initialState(''));
  const [newPassword, setNewPassword] = useState(initialState(''));
  const [confirmPassword, setConfirmPassword] = useState(initialState(''));

  const requiredFieldsError =
    password.value && newPassword.value && confirmPassword.value ? null : MISSING_FIELDS_ERROR_MESSAGE;
  const mismatchError = newPassword.value === confirmPassword.value ? null : CONFIRMATION_MISMATCH_ERROR_MESSAGE;

  const validationErrors = combineValidationErrors(requiredFieldsError, mismatchError);

  useEscapeKeyStack(true, onClose);

  const handleSubmit = () => {
    if (!hasValidationErrors(validationErrors)) {
      onChangePassword({
        oldPassword: password.value,
        newPassword: newPassword.value,
      });
    }
  };

  return (
    <NxModal id="change-password-modal" onClose={onClose}>
      <NxStatefulForm
        className="nx-form"
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitMaskState={submitMaskMap[changePasswordStatus]}
        submitMaskMessage="Changing password"
        validationErrors={validationErrors}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <span>Change Password</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Current Password" isRequired>
            <NxTextInput
              {...password}
              id="original-password"
              type="password"
              name="password"
              validatable
              onChange={(value) => setPassword(userInput(validateNonEmpty, value))}
            />
          </NxFormGroup>
          <hr />
          <NxFormGroup label="New Password" isRequired>
            <NxTextInput
              {...newPassword}
              id="new-password"
              type="password"
              name="new-password"
              validatable
              onChange={(value) => setNewPassword(userInput(validateNonEmpty, value))}
            />
          </NxFormGroup>
          <NxFormGroup label="Confirm New Password" isRequired>
            <NxTextInput
              {...confirmPassword}
              id="confirm-password"
              type="password"
              name="confirm-password"
              validatable
              validationErrors={validateNonEmpty(confirmPassword.value) || mismatchError}
              onChange={(value) => setConfirmPassword(userInput(null, value))}
            />
          </NxFormGroup>
          {changePasswordError && <NxErrorAlert id="change-password-error">{changePasswordError}</NxErrorAlert>}
        </div>
      </NxStatefulForm>
    </NxModal>
  );
};

ChangePasswordModal.propTypes = {
  onClose: PropTypes.func,
  onChangePassword: PropTypes.func,
  changePasswordError: PropTypes.string,
  changePasswordStatus: PropTypes.oneOf(['idle', 'pending', 'success', 'failure']),
};

const submitMaskMap = {
  pending: false,
  success: true,
  failure: null,
  idle: null,
};

export default ChangePasswordModal;
