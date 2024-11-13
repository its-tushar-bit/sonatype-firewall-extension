/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { dec } from 'ramda';
import { NxButton, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectSessionTimeoutMilliseconds } from 'MainRoot/user/userSelectors';

export default function LogoutWarningModal({ onClick, startingCount }) {
  const [secondsLeft, setSecondsLeft] = useState(startingCount);
  const sessionTimeoutMilliseconds = useSelector(selectSessionTimeoutMilliseconds);

  useEffect(() => {
    const intervalId = setInterval(() => {
      setSecondsLeft(dec);
    }, 1000);

    return () => {
      clearInterval(intervalId);
    };
  }, []);

  const sessionTimeoutText =
    typeof sessionTimeoutMilliseconds === 'number'
      ? `${Math.floor(sessionTimeoutMilliseconds / 1000 / 60)} minutes of inactivity`
      : 'inactivity';

  return (
    <NxModal id="logout-warning-modal">
      <header className="nx-modal-header">
        <h2 className="nx-h2">Session Timeout Warning</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert>
          Due to {sessionTimeoutText} you will be logged out in {Math.max(secondsLeft, 0)} seconds.
        </NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton variant="primary" onClick={onClick} id="logout-warning-modal-extend-btn">
            Keep me signed in
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

LogoutWarningModal.propTypes = {
  onClick: PropTypes.func,
  startingCount: PropTypes.number.isRequired,
};
