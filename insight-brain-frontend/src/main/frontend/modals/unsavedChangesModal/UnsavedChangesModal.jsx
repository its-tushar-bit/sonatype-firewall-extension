/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxButtonBar, NxFooter, NxH2, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { selectUnsavedChangesModalSlice } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import * as PropTypes from 'prop-types';

export default function UnsavedChangesModal({ onContinue, onClose }) {
  const { open } = useSelector(selectUnsavedChangesModalSlice);
  const dispatch = useDispatch();

  const handleClose =
    onClose ??
    (() => {
      dispatch(actions.cancelAndClose());
    });

  const handleContinue =
    onContinue ??
    (() => {
      dispatch(actions.continueAndClose());
    });

  // In some places we create our own instance of UnsavedChangesModal with custom onContinue and onClose functions,
  // and in these cases we want the modal to be shown
  const isOpen = open || onContinue != null || onClose != null;

  return (
    isOpen && (
      <NxModal id="unsaved-modal" variant="narrow" onCancel={handleClose}>
        <NxModal.Header>
          <NxH2>Unsaved Changes</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert className="nx-alert--modifier">
            <span>The page may contain unsaved changes; continuing will discard them.</span>
          </NxWarningAlert>
        </NxModal.Content>
        <NxFooter>
          <NxButtonBar>
            <NxButton onClick={handleClose} id="unsaved-changes-modal-cancel-button">
              Cancel
            </NxButton>
            <NxButton variant="primary" id="unsaved-changes-modal-continue-button" onClick={handleContinue}>
              Continue
            </NxButton>
          </NxButtonBar>
        </NxFooter>
      </NxModal>
    )
  );
}

UnsavedChangesModal.propTypes = {
  onClose: PropTypes.func,
  onContinue: PropTypes.func,
};
