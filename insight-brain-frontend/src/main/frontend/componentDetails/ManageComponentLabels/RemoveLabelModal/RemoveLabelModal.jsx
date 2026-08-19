/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { NxModal, NxFontAwesomeIcon, NxStatefulForm, NxWarningAlert } from '@sonatype/react-shared-components';

const REMOVE_LABEL_ERR = 'An error occurred removing label.';

export default function RemoveLabelModal({
  removeLabel,
  toggleShowRemoveLabelModal,
  selectedLabelDetails,
  showRemoveLabelModal,
  removeLabelError,
  removeLabelMaskState,
}) {
  const removeHandler = () => {
    removeLabel(selectedLabelDetails);
  };

  return (
    showRemoveLabelModal && (
      <NxModal variant="narrow" onCancel={toggleShowRemoveLabelModal} aria-labelledby="iq-remove-label">
        <NxStatefulForm
          onSubmit={removeHandler}
          submitMaskState={removeLabelMaskState}
          submitMaskMessage="Removing label…"
          onCancel={toggleShowRemoveLabelModal}
          submitBtnText="Remove"
          submitError={removeLabelError}
          submitErrorTitleMessage={REMOVE_LABEL_ERR}
        >
          <header className="nx-modal-header">
            <h2 className="nx-h2" id="iq-remove-label">
              <NxFontAwesomeIcon icon={faTrashAlt} />
              <span>Remove Label</span>
            </h2>
          </header>
          <div className="nx-modal-content">
            <NxWarningAlert>Are you sure you want to remove this label?</NxWarningAlert>
          </div>
        </NxStatefulForm>
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
  removeLabelMaskState: PropTypes.bool,
};
