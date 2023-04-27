/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as PropTypes from 'prop-types';
import React, { useEffect, useState, Fragment } from 'react';
import {
  NxFormGroup,
  NxTextInput,
  NxButton,
  NxStatefulForm,
  NxFontAwesomeIcon,
  NxModal,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';

import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import RoleEditorPermissionsList from './RoleEditorPermissionsList';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

function RoleEditor({
  setRoleName,
  setRoleDescription,
  formState,
  toggleValue,
  isDirty,
  save,
  updateMaskState,
  load,
  readonly,
  loadError,
  updateError,
  roles,
  loadRoles,
  router,
  deleteRole,
  deleteError,
  deleteMaskState,
  builtIn,
  loading,
  stateGo,
}) {
  const { name: roleName, description: roleDescription, permissionCategories } = formState;

  const [showModal, setShowModal] = useState(false);

  const {
    currentState: { name },
    currentParams: { roleId },
  } = router;

  const creationMode = name === 'addRole';

  useEffect(() => {
    load(creationMode ? null : roleId);

    if (!roles.length) {
      loadRoles();
    }
  }, []);

  function formValidatorErrors() {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    }

    if ((roleName.validationErrors || []).length || (roleDescription.validationErrors || []).length) {
      return 'Unable to submit: fields with invalid or missing data.';
    }

    return null;
  }

  function readOnlyValidatorErrors() {
    return builtIn ? 'This role cannot be edited.' : 'You have insufficient permissions to edit this role.';
  }

  function modalCloseHandler() {
    setShowModal(false);
  }

  return (
    <Fragment>
      <main className="nx-page-main" id="role-editor">
        <MenuBarBackButton stateName="rolesList" />
        <div className="nx-page-title" id="role-title">
          <h1 className="nx-h1">{creationMode ? 'Create' : 'Edit'} a Role</h1>
        </div>
        <section className="nx-tile">
          <NxStatefulForm
            loading={loading}
            onCancel={() => stateGo('rolesList')}
            submitBtnText="Save"
            validationErrors={readonly ? readOnlyValidatorErrors() : formValidatorErrors()}
            onSubmit={save}
            submitMaskState={updateMaskState}
            loadError={loadError}
            doLoad={load}
            submitError={updateError}
          >
            <header className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Role Details</h2>
              </div>
              {!creationMode && (
                <div className="nx-tile__actions">
                  <NxButton
                    type="button"
                    variant="tertiary"
                    disabled={readonly}
                    onClick={() => setShowModal(true)}
                    id="delete-role"
                  >
                    <NxFontAwesomeIcon icon={faTrashAlt} />
                    <span>Delete</span>
                  </NxButton>
                </div>
              )}
            </header>
            <div className="nx-tile-content">
              <NxFormGroup label="Role Name" isRequired>
                <NxTextInput
                  {...roleName}
                  onChange={(value) => setRoleName({ value, roles })}
                  validatable={true}
                  disabled={readonly}
                  id="roleName"
                />
              </NxFormGroup>

              <NxFormGroup label="Role Description" isRequired>
                <NxTextInput
                  {...roleDescription}
                  type="textarea"
                  className="nx-text-input--long"
                  onChange={(value) => setRoleDescription({ value })}
                  validatable={true}
                  disabled={readonly}
                  id="roleDescription"
                />
              </NxFormGroup>
              {permissionCategories.map(({ displayName, permissions }, index) => {
                return (
                  <section
                    className="nx-tile-subsection"
                    key={displayName}
                    id={'test-permission-category-' + displayName}
                  >
                    {index === 0 && <h2 className="nx-h2">Permissions</h2>}
                    <header className="nx-tile-subsection__header">
                      <h3 className="nx-h3">{displayName}</h3>
                    </header>
                    <RoleEditorPermissionsList
                      permissionsList={permissions}
                      displayName={displayName}
                      toggleValue={toggleValue}
                      readonly={readonly}
                    />
                  </section>
                );
              })}
            </div>
          </NxStatefulForm>
        </section>
      </main>
      {showModal && (
        <NxModal onClose={modalCloseHandler} variant="narrow">
          <NxStatefulForm
            className="nx-form"
            onSubmit={() => deleteRole(roleId)}
            onCancel={modalCloseHandler}
            submitBtnText="Delete"
            submitError={deleteError}
            submitMaskState={deleteMaskState}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Role</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                Clicking ‘delete’ will permanently remove this role from the system. Are you sure you want to delete{' '}
                {roleName.trimmedValue}?
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </Fragment>
  );
}

RoleEditor.propTypes = {
  setRoleName: PropTypes.func.isRequired,
  setRoleDescription: PropTypes.func.isRequired,
  formState: PropTypes.object.isRequired,
  toggleValue: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  save: PropTypes.func.isRequired,
  updateMaskState: PropTypes.bool,
  load: PropTypes.func.isRequired,
  readonly: PropTypes.bool,
  loadError: PropTypes.string,
  updateError: PropTypes.string,
  roles: PropTypes.array.isRequired,
  loadRoles: PropTypes.func.isRequired,
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }),
  router: PropTypes.shape({
    currentParams: PropTypes.object.isRequired,
    currentState: PropTypes.object.isRequired,
  }),
  deleteRole: PropTypes.func.isRequired,
  deleteError: PropTypes.string,
  deleteSuccess: PropTypes.bool,
  deleteSaving: PropTypes.bool,
  deleteMaskState: PropTypes.bool,
  builtIn: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  stateGo: PropTypes.func.isRequired,
};

export default RoleEditor;
