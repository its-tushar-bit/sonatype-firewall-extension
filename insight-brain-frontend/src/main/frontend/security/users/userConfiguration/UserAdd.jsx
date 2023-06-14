/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxTextInput, NxFormGroup, NxFormRow } from '@sonatype/react-shared-components';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = ({ isDirty, validationError }) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function UserAdd(props) {
  const {
    loading,
    loadError,
    submitMaskState,
    saveError,
    isDirty,
    validationError,
    inputFields,
    loadCreateUserPage,
    save,
    setFirstName,
    setLastName,
    setEmail,
    setUserName,
    setPassword,
    setMatchPassword,
    resetForm,
    stateGo,
    tenantMode,
  } = props;

  const { firstName, lastName, email, username, password, matchPassword } = inputFields,
    isSingleTenant = tenantMode !== 'multi-tenant',
    pageTitle = isSingleTenant ? 'Add New User' : 'Invite User';

  useEffect(() => {
    loadCreateUserPage();

    return () => {
      resetForm();
    };
  }, []);

  return (
    <main className="nx-page-main">
      <MenuBarBackButton stateName="users" />
      <div className="nx-page-title">
        <h1 className="nx-h1" id="user-title">
          {pageTitle}
        </h1>
      </div>
      <section className="nx-tile">
        <NxStatefulForm
          id="user-form"
          autoComplete="off"
          onSubmit={save}
          loadError={loadError}
          loading={loading}
          doLoad={loadCreateUserPage}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={saveError}
          validationErrors={getValidationMessage({ isDirty, validationError })}
          submitBtnText="Save"
          onCancel={() => stateGo('users')}
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">User Details</h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <NxFormRow>
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
            </NxFormRow>
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
            {isSingleTenant && (
              <>
                <NxFormGroup label="Username" isRequired>
                  <NxTextInput
                    {...username}
                    onChange={setUserName}
                    validatable={true}
                    className="nx-text-input"
                    id="username"
                    placeholder="Enter Username"
                    aria-required={true}
                    autoComplete="new-password"
                  />
                </NxFormGroup>
                <NxFormRow>
                  <NxFormGroup label="Password" isRequired>
                    <NxTextInput
                      {...password}
                      onChange={setPassword}
                      validatable={true}
                      className="nx-text-input"
                      id="password"
                      type="password"
                      placeholder="Enter Password"
                      aria-required={true}
                      autoComplete="new-password"
                    />
                  </NxFormGroup>
                  <NxFormGroup label="Validate Password" isRequired>
                    <NxTextInput
                      {...matchPassword}
                      validatable={true}
                      onChange={setMatchPassword}
                      className="nx-text-input"
                      type="password"
                      id="passwordValidate"
                      placeholder="Enter Password"
                      aria-required={true}
                    />
                  </NxFormGroup>
                </NxFormRow>
              </>
            )}
          </div>
        </NxStatefulForm>
      </section>
    </main>
  );
}

const inputFieldsTypes = PropTypes.shape({
  firstName: PropTypes.object,
  lastName: PropTypes.object,
  email: PropTypes.object,
  username: PropTypes.object,
  password: PropTypes.object,
  matchPassword: PropTypes.object,
});

UserAdd.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  isDirty: PropTypes.bool,
  validationError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  save: PropTypes.func.isRequired,
  loadCreateUserPage: PropTypes.func.isRequired,
  setFirstName: PropTypes.func.isRequired,
  setLastName: PropTypes.func.isRequired,
  setEmail: PropTypes.func.isRequired,
  setUserName: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setMatchPassword: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  inputFields: inputFieldsTypes,
  tenantMode: PropTypes.string,
};
