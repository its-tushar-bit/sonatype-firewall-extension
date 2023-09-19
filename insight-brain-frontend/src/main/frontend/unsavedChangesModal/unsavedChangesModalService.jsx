/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ReactDOM from 'react-dom';
import React from 'react';

import UnsavedChangesModal from './UnsavedChangesModal';

export default function unsavedChangesModalService($q) {
  let modalPromise = null;

  function resetIsShowing() {
    modalPromise = null;
  }

  return {
    open(ModalClass = UnsavedChangesModal) {
      if (modalPromise) {
        return modalPromise;
      }

      const unsavedChangesDiv = document.createElement('div');
      unsavedChangesDiv.setAttribute('id', 'unsaved-changes-modal-wrapper');
      document.body.appendChild(unsavedChangesDiv);

      const deferred = $q.defer();

      const onContinue = () => {
        cleanupModal();
        resetIsShowing();
        deferred.resolve();
      };

      const onClose = () => {
        cleanupModal();
        resetIsShowing();
        deferred.reject();
      };

      const cleanupModal = () => {
        ReactDOM.unmountComponentAtNode(unsavedChangesDiv);
        document.body.removeChild(unsavedChangesDiv);
      };

      ReactDOM.render(<ModalClass onContinue={onContinue} onClose={onClose} />, unsavedChangesDiv);
      modalPromise = deferred.promise;

      return modalPromise;
    },
  };
}

unsavedChangesModalService.$inject = ['$q'];
