/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';

export default function ConfirmationModal(props) {
  const {
    id,
    cancelHandler,
    titleContent,
    confirmationMessage,
    closeHandler,
    confirmationHandler,
    confirmationButtonText,
  } = props;

  return (
    <NxModal variant="narrow" id={id} onCancel={cancelHandler} aria-labelledby="modal-narrow-header">
      <header className="nx-modal-header">
        <h2 className="nx-h2" id="modal-narrow-header">
          {titleContent}
        </h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert>{confirmationMessage}</NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton type="button" onClick={closeHandler}>
            Close
          </NxButton>
          <NxButton type="button" variant="primary" onClick={confirmationHandler}>
            {confirmationButtonText}
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

ConfirmationModal.propTypes = {
  id: PropTypes.string,
  cancelHandler: PropTypes.func,
  titleContent: PropTypes.element.isRequired,
  confirmationMessage: PropTypes.string.isRequired,
  closeHandler: PropTypes.func.isRequired,
  confirmationHandler: PropTypes.func.isRequired,
  confirmationButtonText: PropTypes.string,
};
