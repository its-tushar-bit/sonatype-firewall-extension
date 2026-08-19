/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import LogoutWarningModal from 'MainRoot/modals/logoutWarningModal/LogoutWarningModal';
import ExternalLinkModal from 'MainRoot/modals/externalLinkModal/ExternalLinkModal';
import UnsavedChangesModal from 'MainRoot/modals/unsavedChangesModal/UnsavedChangesModal';
import LoginModal from 'MainRoot/user/LoginModal/LoginModal';

export default function ModalContainer() {
  return (
    <>
      <LoginModal />
      <LogoutWarningModal />
      <ExternalLinkModal />
      {/* UnsavedChangesModal is also currently rendered in other places; ideally everywhere should use this one */}
      <UnsavedChangesModal />
    </>
  );
}
