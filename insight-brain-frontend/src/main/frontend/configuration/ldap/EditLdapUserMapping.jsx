/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxSuccessAlert } from '@sonatype/react-shared-components';
import EditLdapUserElementMapping, {
  editLdapUserElementMappingActionTypes,
} from './userAndGroupSettings/EditLdapUserElementMapping';
import EditLdapGroupElementMapping, {
  editLdapGroupElementMappingActionTypes,
} from './userAndGroupSettings/EditLdapGroupElementMapping';
import LdapUserMapping from './userAndGroupSettings/LdapUserMapping';

export default function EditLdapUserMapping(props) {
  const {
    inputFields,
    setUserBaseDN,
    setUserSubtree,
    setUserObjectClass,
    setUserFilter,
    setUserIDAttribute,
    setUserRealNameAttribute,
    setUserEmailAttribute,
    setUserPasswordAttribute,
    setGroupMappingType,
    setGroupBaseDN,
    setGroupSubtree,
    setGroupObjectClass,
    setGroupIDAttribute,
    setGroupMemberAttribute,
    setGroupMemberFormat,
    setUserMemberOfGroupAttribute,
    setDynamicGroupSearch,
    successMessage,
    resetAlertMessages,
    loadUserMapping,
    toggleUserMappingModalIsOpen,
    toggleUserMappingSortOrder,
    userMapping: { isLdapUserMappingModalOpen, userList, loadError, sortAscending },
  } = props;
  const {
    userBaseDN,
    userSubtree,
    userObjectClass,
    userFilter,
    userIDAttribute,
    userRealNameAttribute,
    userEmailAttribute,
    userPasswordAttribute,
    groupMappingType,
    groupBaseDN,
    groupSubtree,
    groupObjectClass,
    groupIDAttribute,
    groupMemberAttribute,
    groupMemberFormat,
    userMemberOfGroupAttribute,
    dynamicGroupSearchEnabled,
  } = inputFields;

  return (
    <Fragment>
      <EditLdapUserElementMapping
        userBaseDN={userBaseDN}
        userSubtree={userSubtree}
        userObjectClass={userObjectClass}
        userFilter={userFilter}
        userIDAttribute={userIDAttribute}
        userRealNameAttribute={userRealNameAttribute}
        userEmailAttribute={userEmailAttribute}
        userPasswordAttribute={userPasswordAttribute}
        setUserBaseDN={setUserBaseDN}
        setUserSubtree={setUserSubtree}
        setUserObjectClass={setUserObjectClass}
        setUserFilter={setUserFilter}
        setUserIDAttribute={setUserIDAttribute}
        setUserRealNameAttribute={setUserRealNameAttribute}
        setUserEmailAttribute={setUserEmailAttribute}
        setUserPasswordAttribute={setUserPasswordAttribute}
      />
      <EditLdapGroupElementMapping
        groupMappingType={groupMappingType}
        groupBaseDN={groupBaseDN}
        groupSubtree={groupSubtree}
        groupObjectClass={groupObjectClass}
        groupIDAttribute={groupIDAttribute}
        groupMemberAttribute={groupMemberAttribute}
        groupMemberFormat={groupMemberFormat}
        userMemberOfGroupAttribute={userMemberOfGroupAttribute}
        dynamicGroupSearchEnabled={dynamicGroupSearchEnabled}
        setGroupMappingType={setGroupMappingType}
        setGroupBaseDN={setGroupBaseDN}
        setGroupSubtree={setGroupSubtree}
        setGroupObjectClass={setGroupObjectClass}
        setGroupIDAttribute={setGroupIDAttribute}
        setGroupMemberAttribute={setGroupMemberAttribute}
        setGroupMemberFormat={setGroupMemberFormat}
        setUserMemberOfGroupAttribute={setUserMemberOfGroupAttribute}
        setDynamicGroupSearch={setDynamicGroupSearch}
      />
      {successMessage && <NxSuccessAlert onClose={resetAlertMessages}>{successMessage}</NxSuccessAlert>}
      {isLdapUserMappingModalOpen && (
        <LdapUserMapping
          toggleUserMappingModalIsOpen={toggleUserMappingModalIsOpen}
          loadUserMapping={loadUserMapping}
          userList={userList}
          loadError={loadError}
          sortAscending={sortAscending}
          toggleUserMappingSortOrder={toggleUserMappingSortOrder}
        />
      )}
    </Fragment>
  );
}

EditLdapUserMapping.propTypes = {
  inputFields: PropTypes.object,
  successMessage: PropTypes.string,
  resetAlertMessages: PropTypes.func.isRequired,
  userMapping: PropTypes.shape({
    isLdapUserMappingModalOpen: PropTypes.bool.isRequired,
    userList: PropTypes.array,
    loadError: PropTypes.string,
    sortAscending: PropTypes.bool.isRequired,
  }).isRequired,
  loadUserMapping: PropTypes.func.isRequired,
  toggleUserMappingModalIsOpen: PropTypes.func.isRequired,
  toggleUserMappingSortOrder: PropTypes.func.isRequired,
  ...editLdapUserElementMappingActionTypes,
  ...editLdapGroupElementMappingActionTypes,
};
