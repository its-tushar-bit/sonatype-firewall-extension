/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import EditLicensesForm from './EditLicensesForm';
import IqPopover from '../../../react/IqPopover';
import EditLicensesFormContainer from './EditLicensesFormContainer';
import { pick } from 'ramda';
import UnsavedChangesModal from 'MainRoot/unsavedChangesModal/UnsavedChangesModal';

export default function EditLicensesPopover(props) {
  const {
    onClose,
    resetFormFields,
    showEditLicensesPopover,
    isDirty,
    showUnsavedChangesModal,
    setShowUnsavedChangesModal,
  } = props;

  if (!showEditLicensesPopover) {
    return null;
  }

  const handleOnClose = () => {
    onClose();
    resetFormFields();
  };

  const openUnsavedChangesModal = () => {
    if (isDirty) {
      setShowUnsavedChangesModal(true);
    } else {
      handleOnClose();
    }
  };

  return (
    <IqPopover size="extra-large" onClose={openUnsavedChangesModal} id="edit-licenses-popover">
      <IqPopover.Header
        id="edit-licenses-popover-header"
        className="edit-licenses-popover-header"
        buttonId="edit-licenses-popover-close-btn"
        onClose={openUnsavedChangesModal}
        headerTitle="Edit Licenses"
      />
      {showUnsavedChangesModal && (
        <UnsavedChangesModal onContinue={handleOnClose} onClose={() => setShowUnsavedChangesModal(false)} />
      )}
      <EditLicensesFormContainer />
    </IqPopover>
  );
}

EditLicensesPopover.propTypes = {
  showEditLicensesPopover: PropTypes.bool.isRequired,
  unsavedChangesModalService: PropTypes.object,
  showUnsavedChangesModal: PropTypes.bool,
  setShowUnsavedChangesModal: PropTypes.func,
  isDirty: PropTypes.bool,
  ...pick(['onClose', 'resetFormFields'], EditLicensesForm.propTypes),
};
