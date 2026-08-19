/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxToggle } from '@sonatype/react-shared-components';
import React from 'react';
import * as PropTypes from 'prop-types';
import { startsWith } from 'ramda';

function RoleEditorPermissionsList({ permissionsList, displayName: displayNameProp, toggleValue, readonly }) {
  function permissionMapper(category, { allowed, displayName, description, id }) {
    return (
      <NxToggle
        className="nx-toggle--no-gap"
        isChecked={allowed}
        onChange={() => toggleValue({ category, id })}
        disabled={readonly}
        key={id}
      >
        {displayName} {capitalizeIQ(description)}
      </NxToggle>
    );
  }

  const capitalizeIQ = (permissionDescription) => {
    return startsWith('IQ', permissionDescription)
      ? permissionDescription.slice(0, 2) + permissionDescription.slice(2).toLowerCase()
      : permissionDescription.toLowerCase();
  };
  const permissionsLength = permissionsList.length;
  const firstColumnSize = Math.ceil(permissionsLength / 2);
  const mappedPermissions = permissionsList.map((permission) => permissionMapper(displayNameProp, permission));
  const firstColumn = mappedPermissions.slice(0, firstColumnSize);
  const secondColumn = mappedPermissions.slice(firstColumnSize);

  return (
    <div className={'iq-role-editor-permission-group test-permissions-' + displayNameProp}>
      <div className="iq-role-editor-permission-group__col">{firstColumn}</div>
      <div className="iq-role-editor-permission-group__col">{secondColumn}</div>
    </div>
  );
}

RoleEditorPermissionsList.propTypes = {
  permissionsList: PropTypes.array.isRequired,
  displayName: PropTypes.string.isRequired,
  toggleValue: PropTypes.func.isRequired,
  readonly: PropTypes.bool.isRequired,
};

export default RoleEditorPermissionsList;
