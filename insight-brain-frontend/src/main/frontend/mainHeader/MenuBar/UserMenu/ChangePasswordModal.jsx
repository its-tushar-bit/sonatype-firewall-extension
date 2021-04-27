/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import PropTypes from 'prop-types';

import {
  NxModal,
  NxForm,
  NxFormGroup,
  NxTextInput,
  NxErrorAlert,
  combineValidationErrors,
} from '@sonatype/react-shared-components';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

const setTextInput = (setter, validator) => (value) => {
  setter(userInput(validator, value));
};

const useFormState = (onChangePassword) => {
  const [originalPasswordState, setOriginalPasswordState] = useState(initialState(''));
  let [newPasswordState, setNewPasswordState] = useState(initialState(''));
  let [confirmPasswordState, setConfirmPasswordState] = useState(initialState(''));

  const handleSubmit = () => {
    onChangePassword({
      oldPassword: originalPasswordState.value,
      newPassword: newPasswordState.value,
    });
  };

  const matches = (valueToMatch) => (value) =>
    !valueToMatch || value === valueToMatch ? null : 'New Password and Confirmation must match';
  const matchesConfirmation = matches(confirmPasswordState.value);
  const matchesNewPassword = matches(newPasswordState.value);

  const passwordMisatchMessage =
    matchesConfirmation(newPasswordState.value) || matchesConfirmation(confirmPasswordState.value);

  const allFieldsHaveValues =
    originalPasswordState.value && newPasswordState.value && confirmPasswordState.value
      ? null
      : 'Required Fields Missing';

  const validationErrors = combineValidationErrors(allFieldsHaveValues, passwordMisatchMessage);

  // hack needed to keep validation in sync between confirmation and current password
  if (!passwordMisatchMessage) {
    newPasswordState = { ...newPasswordState, validationErrors: null };
    confirmPasswordState = { ...confirmPasswordState, validationErrors: null };
  }

  return {
    originalPasswordState,
    setOriginalPasswordState,
    newPasswordState,
    setNewPasswordState,
    confirmPasswordState,
    setConfirmPasswordState,
    matchesConfirmation,
    matchesNewPassword,
    handleSubmit,
    validationErrors,
  };
};

const submitMaskMap = {
  pending: false,
  success: true,
  failure: null,
  idle: null,
};

export const ChangePasswordModal = ({ onClose, onChangePassword, changePasswordError, changePasswordStatus }) => {
  const {
    originalPasswordState,
    setOriginalPasswordState,
    newPasswordState,
    setNewPasswordState,
    confirmPasswordState,
    setConfirmPasswordState,
    matchesConfirmation,
    matchesNewPassword,
    handleSubmit,
    validationErrors,
  } = useFormState(onChangePassword);
  useEscapeKeyStack(true, onClose);

  return (
    <NxModal id="change-password-modal" onClose={onClose}>
      <NxForm
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
              {...originalPasswordState}
              id="original-password"
              type="password"
              name="original-password"
              onChange={setTextInput(setOriginalPasswordState)}
            />
          </NxFormGroup>
          <hr />
          <NxFormGroup label="New Password" isRequired>
            <NxTextInput
              {...newPasswordState}
              id="new-password"
              type="password"
              name="new-password"
              onChange={setTextInput(setNewPasswordState, matchesConfirmation)}
              validatable
            />
          </NxFormGroup>
          <NxFormGroup label="Confirm New Password" isRequired>
            <NxTextInput
              {...confirmPasswordState}
              id="confirm-password"
              type="password"
              name="confirm-password"
              onChange={setTextInput(setConfirmPasswordState, matchesNewPassword)}
              validatable
            />
          </NxFormGroup>
          {changePasswordError && <NxErrorAlert id="change-password-error">{changePasswordError}</NxErrorAlert>}
        </div>
      </NxForm>
    </NxModal>
  );
};

ChangePasswordModal.propTypes = {
  onClose: PropTypes.func,
  onChangePassword: PropTypes.func,
  changePasswordError: PropTypes.string,
  changePasswordStatus: PropTypes.oneOf(['idle', 'pending', 'success', 'failure']),
};
export default ChangePasswordModal;
