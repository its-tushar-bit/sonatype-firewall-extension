/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup, NxToggle } from '@sonatype/react-shared-components';

export const groupTypes = ['NONE', 'STATIC', 'DYNAMIC'];

export default function EditLdapGroupElementMapping({
  groupMappingType,
  groupBaseDN,
  groupSubtree,
  groupObjectClass,
  groupIDAttribute,
  groupMemberAttribute,
  groupMemberFormat,
  userMemberOfGroupAttribute,
  dynamicGroupSearchEnabled,
  setGroupMappingType,
  setGroupBaseDN,
  setGroupSubtree,
  setGroupObjectClass,
  setGroupIDAttribute,
  setGroupMemberAttribute,
  setGroupMemberFormat,
  setUserMemberOfGroupAttribute,
  setDynamicGroupSearch,
}) {
  const handleGroupType = (e) => {
    setGroupMappingType(e.target.value);
  };

  const isStaticGroupType = groupMappingType === 'STATIC';
  const isDynamicGroupType = groupMappingType === 'DYNAMIC';

  return (
    <section className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <h3 className="nx-h3">Group Element Mapping</h3>
      </header>
      <NxFormGroup label="Group Type" isRequired>
        <select
          className="nx-form-select"
          id="ldap-group-mapping-type"
          value={groupMappingType}
          onChange={handleGroupType}
        >
          {groupTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </NxFormGroup>
      {isStaticGroupType && (
        <Fragment>
          <div className="nx-form-row">
            <NxFormGroup label="Base DN">
              <NxTextInput {...groupBaseDN} onChange={setGroupBaseDN} id="ldap-group-base-dn" maxLength="255" />
            </NxFormGroup>
            <NxToggle
              id="ldap-group-subtree"
              className="nx-toggle--no-gap"
              onChange={setGroupSubtree}
              isChecked={groupSubtree}
            >
              Include Group Subtree
            </NxToggle>
          </div>
          <div className="nx-form-row">
            <NxFormGroup label="Object Class" isRequired={isStaticGroupType}>
              <NxTextInput
                {...groupObjectClass}
                onChange={setGroupObjectClass}
                id="ldap-group-object-class"
                validatable={true}
                aria-required={isStaticGroupType}
                maxLength="255"
              />
            </NxFormGroup>
            <NxFormGroup label="Group ID Attribute" isRequired={isStaticGroupType}>
              <NxTextInput
                {...groupIDAttribute}
                onChange={setGroupIDAttribute}
                id="ldap-group-id-attribute"
                validatable={true}
                aria-required={isStaticGroupType}
                maxLength="255"
              />
            </NxFormGroup>
          </div>
          <div className="nx-form-row">
            <NxFormGroup label="Group Member Attribute" isRequired={isStaticGroupType}>
              <NxTextInput
                {...groupMemberAttribute}
                onChange={setGroupMemberAttribute}
                id="ldap-group-member-attribute"
                validatable={true}
                aria-required={isStaticGroupType}
                maxLength="255"
              />
            </NxFormGroup>
            <NxFormGroup label="Group Member Format" isRequired={isStaticGroupType}>
              <NxTextInput
                {...groupMemberFormat}
                onChange={setGroupMemberFormat}
                id="ldap-group-member-format"
                validatable={true}
                aria-required={isStaticGroupType}
                maxLength="255"
              />
            </NxFormGroup>
          </div>
        </Fragment>
      )}
      {isDynamicGroupType && (
        <Fragment>
          <NxFormGroup label="Member of Attribute" isRequired={isDynamicGroupType}>
            <NxTextInput
              {...userMemberOfGroupAttribute}
              onChange={setUserMemberOfGroupAttribute}
              id="ldap-user-member-of-group-attribute"
              validatable={true}
              aria-required={isDynamicGroupType}
              maxLength="255"
            />
          </NxFormGroup>
          <NxToggle
            id="ldap-dynamic-group-search-enabled"
            className="nx-toggle--no-gap"
            onChange={setDynamicGroupSearch}
            isChecked={dynamicGroupSearchEnabled}
          >
            Group Search
            <p className="iq-ldap-group-search-toggle">
              Disabling group search may improve performance, but groups will not appear in search results.
            </p>
          </NxToggle>
        </Fragment>
      )}
    </section>
  );
}

const inputTypesProperties = {
  groupMappingType: PropTypes.string,
  groupBaseDN: PropTypes.object,
  groupSubtree: PropTypes.bool,
  groupObjectClass: PropTypes.object,
  groupIDAttribute: PropTypes.object,
  groupMemberAttribute: PropTypes.object,
  groupMemberFormat: PropTypes.object,
  userMemberOfGroupAttribute: PropTypes.object,
  dynamicGroupSearchEnabled: PropTypes.bool,
};

export const editLdapGroupElementMappingActionTypes = {
  setGroupMappingType: PropTypes.func.isRequired,
  setGroupBaseDN: PropTypes.func.isRequired,
  setGroupSubtree: PropTypes.func.isRequired,
  setGroupObjectClass: PropTypes.func.isRequired,
  setGroupIDAttribute: PropTypes.func.isRequired,
  setGroupMemberAttribute: PropTypes.func.isRequired,
  setGroupMemberFormat: PropTypes.func.isRequired,
  setUserMemberOfGroupAttribute: PropTypes.func.isRequired,
  setDynamicGroupSearch: PropTypes.func.isRequired,
};

EditLdapGroupElementMapping.propTypes = {
  ...inputTypesProperties,
  ...editLdapGroupElementMappingActionTypes,
};
