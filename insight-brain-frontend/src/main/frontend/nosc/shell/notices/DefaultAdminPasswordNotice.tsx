/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { selectIsDefaultUser, selectShouldDisplayPasswordWarning } from 'MainRoot/user/userSessionSelectors';
import { NoticeBanner } from './NoticeBanner';

/**
 * Nexus One port of Classic's `ChangeDefaultAdminPasswordNotice`.
 *
 * Copy decision: neither Preview's user menu (`PreviewUserMenu.tsx`) nor its
 * Settings hub (`change-password` row is still a Coming Soon stub) has a
 * change-password mechanism, so there's no in-Preview link/CTA to offer.
 * The copy instead points the reader to Classic's user menu, where the
 * mechanism does exist today.
 */
export function DefaultAdminPasswordNotice(): JSX.Element | null {
  const shouldDisplayWarning = useSelector(selectShouldDisplayPasswordWarning);
  const isDefaultUser = useSelector(selectIsDefaultUser);

  if (!shouldDisplayWarning) {
    return null;
  }

  return (
    <NoticeBanner testId="nosc-default-admin-password-notice">
      <strong>Change Administrator Password. </strong>
      {isDefaultUser ? (
        <span>
          For security reasons, please change the default administrator password. Switch to Classic UI and use the
          user menu (top right) to update it.
        </span>
      ) : (
        <span>
          The &quot;admin&quot; user has the default password set. Sign in as that account, then switch to Classic
          UI and use the user menu (top right) to change it.
        </span>
      )}
    </NoticeBanner>
  );
}
