/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ReactDOM from 'react-dom';
import React from 'react';
import axios from 'axios';
import { getSessionUrl } from '../../../util/CLMLocation';

import { showNotification } from '../notificationService';
import LogoutWarningModal from './LogoutWarningModal';
import { Provider } from 'react-redux';

export default function logoutWarningModalService($ngRedux) {
  let modalPromise = null;

  function resetIsShowing() {
    modalPromise = null;
  }

  function open(secondsLeft, productEdition) {
    if (modalPromise) {
      return modalPromise;
    }

    const logoutWarningDiv = document.createElement('div');
    logoutWarningDiv.setAttribute('id', 'logout-warning-container');
    document.body.appendChild(logoutWarningDiv);

    let resolve;
    modalPromise = new Promise((r) => {
      resolve = r;
    });

    const fireSimpleRequest = () => {
      return axios.get(getSessionUrl()).then(resolve);
    };

    const cleanupModal = () => {
      ReactDOM.unmountComponentAtNode(logoutWarningDiv);
      document.body.removeChild(logoutWarningDiv);
    };

    const onExtendClick = () => {
      cleanupModal();
      resetIsShowing();
      fireSimpleRequest();
    };

    ReactDOM.render(
      <Provider store={$ngRedux}>
        <LogoutWarningModal onClick={onExtendClick} startingCount={secondsLeft} />
      </Provider>,
      logoutWarningDiv
    );
    showNotification('Session Timeout Warning', {
      body: `Your ${productEdition} session will expire in 2 minutes due to inactivity.`,
    });

    return modalPromise;
  }

  return { open };
}

logoutWarningModalService.$inject = ['$ngRedux'];
