/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  useToggle,
  NxForm,
  NxTextInput,
  NxFormGroup,
  NxButton,
  NxFontAwesomeIcon,
  NxModal,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { useRouterState } from '../../react/RouterStateContext';
import BackButton from '../../react/BackButton';

const getValidationMessage = ({ isDirty, validationError }) => {
  if (!isDirty) {
    return 'There are no changes to update';
  }

  return validationError;
};

export default function UserEdit({
  loading,
  loadError,
  submitMaskState,
  deleteMaskState,
  saveError,
  deleteError,
  isDirty,
  validationError,
  inputFields,
  loadUserById,
  update,
  deleteUser,
  username,
  setFirstName,
  setLastName,
  setEmail,
  resetForm,
  router,
  stateGo,
}) {
  const history = useRouterState();
  const { firstName, lastName, email } = inputFields;
  const [showModal, toggleShowModal] = useToggle(false);

  const {
    currentParams: { userId },
  } = router;

  useEffect(() => {
    loadUserById(userId);

    return () => {
      resetForm();
    };
  }, []);

  return (
    <Fragment>
      <main className="nx-page-main">
        <BackButton stateName="users" $state={history} />
        <div className="nx-page-title">
          <h1 className="nx-h1">Edit User</h1>
        </div>
        <section className="nx-tile">
          <NxForm
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
                <NxButton type="button" variant="tertiary" onClick={toggleShowModal} id="delete-user">
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
          </NxForm>
        </section>
      </main>
      {showModal && (
        <NxModal onClose={toggleShowModal} variant="narrow" id="delete-user-modal">
          <NxForm
            className="nx-form"
            onSubmit={() => deleteUser(userId)}
            submitMaskState={deleteMaskState}
            onCancel={toggleShowModal}
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
              <NxWarningAlert>
                You are about to permanently remove {username}. This action cannot be undone.
              </NxWarningAlert>
            </div>
          </NxForm>
        </NxModal>
      )}
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
  deleteError: PropTypes.string,
  isDirty: PropTypes.bool,
  validationError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  deleteMaskState: PropTypes.bool,
  update: PropTypes.func.isRequired,
  deleteUser: PropTypes.func.isRequired,
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
