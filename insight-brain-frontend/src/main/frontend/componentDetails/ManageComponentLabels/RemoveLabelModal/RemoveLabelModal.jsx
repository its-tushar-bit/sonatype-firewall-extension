/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxModal, NxButton, NxLoadError } from '@sonatype/react-shared-components';

const REMOVE_LABEL_ERR = 'An error occurred removing label.';

export default function RemoveLabelModal({
  removeLabel,
  toggleShowRemoveLabelModal,
  selectedLabelDetails,
  showRemoveLabelModal,
  removeLabelError,
}) {
  const removeHandler = () => {
    removeLabel(selectedLabelDetails);
  };

  return (
    showRemoveLabelModal && (
      <NxModal variant="narrow" onCancel={toggleShowRemoveLabelModal} aria-labelledby="iq-remove-label">
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="iq-remove-label">
            <span>Remove Label</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <p className="nx-p">Are you sure you want to remove this label?</p>
        </div>
        <footer className="nx-footer">
          {removeLabelError && (
            <NxLoadError error={removeLabelError} titleMessage={REMOVE_LABEL_ERR} retryHandler={removeHandler} />
          )}
          <div className="nx-btn-bar">
            <NxButton onClick={toggleShowRemoveLabelModal}>Cancel</NxButton>
            {!removeLabelError && (
              <NxButton variant="primary" onClick={removeHandler}>
                Remove
              </NxButton>
            )}
          </div>
        </footer>
      </NxModal>
    )
  );
}

RemoveLabelModal.propTypes = {
  showRemoveLabelModal: PropTypes.bool.isRequired,
  removeLabel: PropTypes.func.isRequired,
  selectedLabelDetails: PropTypes.object.isRequired,
  toggleShowRemoveLabelModal: PropTypes.func.isRequired,
  removeLabelError: PropTypes.string,
};
