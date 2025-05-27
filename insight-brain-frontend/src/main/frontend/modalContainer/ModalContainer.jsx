/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import LogoutWarningModal from 'MainRoot/modals/logoutWarningModal/LogoutWarningModal';
import ExternalLinkModal from 'MainRoot/modals/externalLinkModal/ExternalLinkModal';

export default function ModalContainer() {
  return (
    <>
      <LogoutWarningModal />
      <ExternalLinkModal />
    </>
  );
}
