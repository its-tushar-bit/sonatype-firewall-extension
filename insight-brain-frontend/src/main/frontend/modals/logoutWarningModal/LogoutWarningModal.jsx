/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxButtonBar, NxFooter, NxH2, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectSessionTimeoutMilliseconds } from 'MainRoot/user/userSessionSelectors';
import { selectLogoutWarningModalSlice } from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSelectors';
import { actions } from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSlice';

export default function LogoutWarningModal() {
  const { open, secondsLeft } = useSelector(selectLogoutWarningModalSlice);
  const sessionTimeoutMilliseconds = useSelector(selectSessionTimeoutMilliseconds);
  const dispatch = useDispatch();
  const close = () => dispatch(actions.close());

  const sessionTimeoutText =
    typeof sessionTimeoutMilliseconds === 'number'
      ? `${Math.floor(sessionTimeoutMilliseconds / 1000 / 60)} minutes of inactivity`
      : 'inactivity';

  return (
    open && (
      <NxModal id="logout-warning-modal" aria-labelledby="logout-warning-modal-header">
        <NxModal.Header>
          <NxH2 id="logout-warning-modal-header">Session Timeout Warning</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            Due to {sessionTimeoutText} you will be logged out in {Math.max(secondsLeft, 0)} seconds.
          </NxWarningAlert>
        </NxModal.Content>
        <NxFooter>
          <NxButtonBar>
            <NxButton variant="primary" onClick={close} id="logout-warning-modal-extend-btn">
              Keep me signed in
            </NxButton>
          </NxButtonBar>
        </NxFooter>
      </NxModal>
    )
  );
}
