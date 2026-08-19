/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxStatefulForm,
  NxTextInput,
  NxFormGroup,
  NxButton,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';
import { MODAL_MODES } from '../modals/modalModes';
import DeleteModal from '../modals/DeleteModal';
import ResetPasswordModal from '../modals/ResetPasswordModal';
import CopyToClipboard from '../modals/CopyToClipboard';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const getValidationMessage = ({ isDirty, validationError }) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_UPDATE;
  }

  return validationError;
};

export default function UserEdit({
  loading,
  loadError,
  submitMaskState,
  deleteMaskState,
  resetMaskState,
  newPassword,
  saveError,
  deleteError,
  resetError,
  isDirty,
  validationError,
  inputFields,
  loadUserById,
  update,
  deleteUser,
  resetPassword,
  resetInitialNewPasswordValue,
  username,
  setFirstName,
  setLastName,
  setEmail,
  resetForm,
  router,
  stateGo,
}) {
  const {
    currentParams: { userId },
  } = router;
  const { firstName, lastName, email } = inputFields;
  const [mode, setMode] = useState(MODAL_MODES.DEFAULT);

  useEffect(() => {
    loadUserById(userId);

    return () => {
      resetForm();
    };
  }, []);

  useEffect(() => {
    if (newPassword) {
      setMode(MODAL_MODES.COPY_TO_CLIPBOARD);
    }
  }, [newPassword]);

  function getModal() {
    switch (mode) {
      case MODAL_MODES.DELETE:
        return (
          <DeleteModal
            {...{ userId, username, deleteUser, deleteError, deleteMaskState }}
            onCancel={() => setMode(MODAL_MODES.DEFAULT)}
          />
        );
      case MODAL_MODES.RESET:
        return <ResetPasswordModal {...{ userId, username, resetPassword, resetError, resetMaskState, setMode }} />;
      case MODAL_MODES.COPY_TO_CLIPBOARD:
        return <CopyToClipboard {...{ username, newPassword, resetInitialNewPasswordValue, setMode }} />;
      default:
        return null;
    }
  }

  return (
    <Fragment>
      <main className="nx-page-main">
        <MenuBarBackButton stateName="users" />
        <div className="nx-page-title">
          <h1 className="nx-h1">Edit User</h1>
          <div className="nx-btn-bar">
            <NxButton type="button" variant="tertiary" onClick={() => setMode(MODAL_MODES.RESET)} id="reset-password">
              Reset Password
            </NxButton>
          </div>
        </div>
        <section className="nx-tile">
          <NxStatefulForm
            id="user-edit"
            autoComplete="off"
            onSubmit={update}
            loadError={loadError}
            loading={loading}
            doLoad={() => loadUserById(userId)}
            submitMaskMessage="Saving…"
            submitMaskState={submitMaskState}
            submitError={saveError}
            validationErrors={getValidationMessage({ isDirty, validationError })}
            submitBtnText="Update"
            onCancel={() => stateGo('users')}
          >
            <header className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">User Details</h2>
              </div>
              <div className="nx-tile__actions">
                <NxButton type="button" variant="tertiary" onClick={() => setMode(MODAL_MODES.DELETE)} id="delete-user">
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                  <span>Delete User</span>
                </NxButton>
              </div>
            </header>
            <div className="nx-tile-content">
              <div className="iq-input-group-wrapper">
                <NxFormGroup label="First Name" isRequired>
                  <NxTextInput
                    {...firstName}
                    onChange={setFirstName}
                    validatable={true}
                    className="nx-text-input"
                    id="firstName"
                    placeholder="Enter First Name"
                    aria-required={true}
                  />
                </NxFormGroup>
                <NxFormGroup label="Last Name" isRequired>
                  <NxTextInput
                    {...lastName}
                    onChange={setLastName}
                    validatable={true}
                    className="nx-text-input"
                    id="lastName"
                    placeholder="Enter Last Name"
                    aria-required={true}
                  />
                </NxFormGroup>
              </div>
              <NxFormGroup label="Email" isRequired>
                <NxTextInput
                  {...email}
                  onChange={setEmail}
                  validatable={true}
                  className="nx-text-input"
                  id="email"
                  placeholder="Enter Email"
                  aria-required={true}
                />
              </NxFormGroup>
            </div>
          </NxStatefulForm>
        </section>
      </main>
      {getModal()}
    </Fragment>
  );
}

const inputFieldsTypes = PropTypes.shape({
  firstName: PropTypes.object,
  lastName: PropTypes.object,
  email: PropTypes.object,
});

UserEdit.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  newPassword: PropTypes.string,
  deleteError: PropTypes.string,
  resetError: PropTypes.string,
  isDirty: PropTypes.bool,
  validationError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  deleteMaskState: PropTypes.bool,
  resetMaskState: PropTypes.bool,
  update: PropTypes.func.isRequired,
  deleteUser: PropTypes.func.isRequired,
  resetPassword: PropTypes.func.isRequired,
  resetInitialNewPasswordValue: PropTypes.func.isRequired,
  loadUserById: PropTypes.func.isRequired,
  username: PropTypes.string,
  setFirstName: PropTypes.func.isRequired,
  setLastName: PropTypes.func.isRequired,
  setEmail: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  inputFields: inputFieldsTypes,
  router: PropTypes.object,
  stateGo: PropTypes.func.isRequired,
};
