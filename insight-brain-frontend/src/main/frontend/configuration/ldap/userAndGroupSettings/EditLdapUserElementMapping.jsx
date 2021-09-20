/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup, NxToggle } from '@sonatype/react-shared-components';

export default function EditLdapUserElementMapping({
  userBaseDN,
  userSubtree,
  userObjectClass,
  userFilter,
  userIDAttribute,
  userRealNameAttribute,
  userEmailAttribute,
  userPasswordAttribute,
  setUserBaseDN,
  setUserSubtree,
  setUserObjectClass,
  setUserFilter,
  setUserIDAttribute,
  setUserRealNameAttribute,
  setUserEmailAttribute,
  setUserPasswordAttribute,
}) {
  return (
    <section className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <h3 className="nx-h3">User Element Mapping</h3>
      </header>
      <div className="nx-form-row">
        <NxFormGroup label="Base DN">
          <NxTextInput {...userBaseDN} onChange={setUserBaseDN} id="ldap-user-base-dn" maxLength="255" />
        </NxFormGroup>
        <NxToggle
          id="ldap-user-subtree"
          className="nx-toggle--no-gap"
          onChange={setUserSubtree}
          isChecked={userSubtree}
        >
          Include User Subtree
        </NxToggle>
      </div>
      <div className="nx-form-row">
        <NxFormGroup label="Object Class" isRequired>
          <NxTextInput
            {...userObjectClass}
            onChange={setUserObjectClass}
            id="ldap-user-object-class"
            validatable={true}
            aria-required={true}
            maxLength="255"
          />
        </NxFormGroup>
        <NxFormGroup label="User Filter">
          <NxTextInput {...userFilter} onChange={setUserFilter} id="ldap-user-filter" maxLength="255" />
        </NxFormGroup>
      </div>
      <div className="nx-form-row">
        <NxFormGroup label="Username Attribute" isRequired>
          <NxTextInput
            {...userIDAttribute}
            onChange={setUserIDAttribute}
            id="ldap-user-id-attribute"
            validatable={true}
            aria-required={true}
            maxLength="255"
          />
        </NxFormGroup>
        <NxFormGroup label="Real Name Attribute" isRequired>
          <NxTextInput
            {...userRealNameAttribute}
            onChange={setUserRealNameAttribute}
            id="ldap-user-real-name-attribute"
            validatable={true}
            aria-required={true}
            maxLength="255"
          />
        </NxFormGroup>
      </div>
      <div className="nx-form-row">
        <NxFormGroup label="E-mail Attribute" isRequired>
          <NxTextInput
            {...userEmailAttribute}
            onChange={setUserEmailAttribute}
            id="ldap-user-email-attribute"
            validatable={true}
            aria-required={true}
            maxLength="255"
          />
        </NxFormGroup>
        <NxFormGroup label="Password Attribute">
          <NxTextInput
            {...userPasswordAttribute}
            type="password"
            autoComplete="new-password"
            onChange={setUserPasswordAttribute}
            id="ldap-user-password-attribute"
            maxLength="255"
          />
        </NxFormGroup>
      </div>
    </section>
  );
}

const inputTypesProperties = {
  userBaseDN: PropTypes.object,
  userSubtree: PropTypes.bool,
  userObjectClass: PropTypes.object,
  userFilter: PropTypes.object,
  userIDAttribute: PropTypes.object,
  userRealNameAttribute: PropTypes.object,
  userEmailAttribute: PropTypes.object,
  userPasswordAttribute: PropTypes.object,
};

export const editLdapUserElementMappingActionTypes = {
  setUserBaseDN: PropTypes.func.isRequired,
  setUserSubtree: PropTypes.func.isRequired,
  setUserObjectClass: PropTypes.func.isRequired,
  setUserFilter: PropTypes.func.isRequired,
  setUserIDAttribute: PropTypes.func.isRequired,
  setUserRealNameAttribute: PropTypes.func.isRequired,
  setUserEmailAttribute: PropTypes.func.isRequired,
  setUserPasswordAttribute: PropTypes.func.isRequired,
};

EditLdapUserElementMapping.propTypes = {
  ...inputTypesProperties,
  ...editLdapUserElementMappingActionTypes,
};
