/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup, NxErrorAlert } from '@sonatype/react-shared-components';

export const methods = ['NONE', 'SIMPLE', 'DIGESTMD5', 'CRAMMD5'];

export default function EditLdapAuth({
  authenticationMethod,
  saslRealm,
  systemUsername,
  systemPassword,
  setMethod,
  setSaslRealm,
  setUsername,
  setPassword,
  mustReenterPassword,
}) {
  const handleMethod = (e) => {
    setMethod(e.target.value);
  };

  const isNonDefaultMethod = authenticationMethod !== 'NONE';
  return (
    <section className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <h3 className="nx-h3">Authentication</h3>
      </header>
      <div className="nx-form-row">
        <NxFormGroup label="Method" isRequired>
          <select className="nx-form-select" id="method-selector" value={authenticationMethod} onChange={handleMethod}>
            {methods.map((method) => (
              <option key={method} value={method}>
                {method}
              </option>
            ))}
          </select>
        </NxFormGroup>
        {isNonDefaultMethod && (
          <NxFormGroup label="SASL Realm">
            <NxTextInput {...saslRealm} onChange={setSaslRealm} id="saslRealm" />
          </NxFormGroup>
        )}
      </div>
      {isNonDefaultMethod && (
        <Fragment>
          <div className="nx-form-row">
            <NxFormGroup label="Username" isRequired>
              <NxTextInput
                {...systemUsername}
                onChange={setUsername}
                validatable={true}
                id="username"
                aria-required={isNonDefaultMethod}
              />
            </NxFormGroup>
            <NxFormGroup label="Password" isRequired>
              <NxTextInput
                {...systemPassword}
                onChange={setPassword}
                validatable={true}
                id="password"
                type="password"
                autoComplete="new-password"
                aria-required={isNonDefaultMethod}
              />
            </NxFormGroup>
          </div>
          {mustReenterPassword && (
            <NxErrorAlert>
              The password must be given when updating the hostname or port for a connection that uses authentication.
            </NxErrorAlert>
          )}
        </Fragment>
      )}
    </section>
  );
}

const inputTypesProperties = {
  authenticationMethod: PropTypes.string.isRequired,
  saslRealm: PropTypes.object,
  systemUsername: PropTypes.object,
  systemPassword: PropTypes.object,
};

export const editLdapAuthActionTypes = {
  setMethod: PropTypes.func.isRequired,
  setSaslRealm: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
};

EditLdapAuth.propTypes = {
  ...inputTypesProperties,
  mustReenterPassword: PropTypes.bool.isRequired,
  ...editLdapAuthActionTypes,
};
