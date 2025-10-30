/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { faWarning } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { selectIsDefaultUser, selectShouldDisplayNotice } from 'MainRoot/user/userSelectors';

export default function ChangeDefaultAdminPasswordNotice() {
  const isDefaultUser = useSelector(selectIsDefaultUser);
  const shouldDisplayNotice = useSelector(selectShouldDisplayNotice);

  if (!shouldDisplayNotice) {
    return null;
  }

  return (
    <div id="change-default-admin-password-notice" className="nx-system-notice nx-system-notice--alert">
      <FontAwesomeIcon icon={faWarning} />
      <strong> Change Administrator Password. </strong>
      {isDefaultUser ? (
        <span>For security reasons, please change your password by clicking the user menu in the header.</span>
      ) : (
        <span>
          The &quot;admin&quot; user has the default password set. Login under that username to change the password.
        </span>
      )}
    </div>
  );
}
