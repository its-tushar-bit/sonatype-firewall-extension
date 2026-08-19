/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm } from '@sonatype/react-shared-components';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import LdapServerNameForm from './LdapServerNameForm';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = ({ isDirty, validationError }) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function CreateLdap({
  loadAddPage,
  saveServerName,
  setServerName,
  resetForm,
  inputFields,
  loading,
  loadError,
  saveError,
  isDirty,
  validationError,
  saveMaskState,
  stateGo,
}) {
  const { serverName } = inputFields;

  useEffect(() => {
    loadAddPage();

    return () => {
      resetForm();
    };
  }, []);

  return (
    <main className="nx-page-main" id="ldap-create-server">
      <MenuBarBackButton stateName="ldap-list" />
      <div className="nx-page-title">
        <h1 className="nx-h1" id="user-title">
          Add a Server
        </h1>
      </div>
      <section className="nx-tile">
        <NxStatefulForm
          id="ldap-create"
          autoComplete="off"
          onSubmit={saveServerName}
          submitBtnText="Save"
          submitMaskMessage="Saving…"
          loadError={loadError}
          loading={loading}
          doLoad={loadAddPage}
          validationErrors={getValidationMessage({ isDirty, validationError })}
          submitMaskState={saveMaskState}
          submitError={saveError}
          onCancel={() => stateGo('ldap-list')}
        >
          <div className="nx-tile-content">
            <LdapServerNameForm serverName={serverName} setServerName={setServerName} autoFocus={true} />
          </div>
        </NxStatefulForm>
      </section>
    </main>
  );
}

const inputFieldsTypes = PropTypes.shape({
  serverName: PropTypes.object,
});

CreateLdap.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  saveError: PropTypes.string,
  isDirty: PropTypes.bool,
  validationError: PropTypes.string,
  saveMaskState: PropTypes.bool,
  loadAddPage: PropTypes.func.isRequired,
  saveServerName: PropTypes.func.isRequired,
  setServerName: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  inputFields: inputFieldsTypes,
  stateGo: PropTypes.func.isRequired,
};
