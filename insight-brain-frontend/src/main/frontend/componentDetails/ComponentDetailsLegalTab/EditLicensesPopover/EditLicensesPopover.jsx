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

export default function EditLicensesPopover(props) {
  const { onClose, resetFormFields, showEditLicensesPopover } = props;

  if (!showEditLicensesPopover) {
    return null;
  }

  const handleOnClose = () => {
    onClose();
    resetFormFields();
  };

  return (
    <IqPopover size="extra-large" onClose={handleOnClose} id="edit-licenses-popover">
      <IqPopover.Header
        id="edit-licenses-popover-header"
        className="edit-licenses-popover-header"
        buttonId="edit-licenses-popover-close-btn"
        onClose={handleOnClose}
        headerTitle="Edit Licenses"
      />
      <EditLicensesFormContainer />
    </IqPopover>
  );
}

EditLicensesPopover.propTypes = {
  showEditLicensesPopover: PropTypes.bool.isRequired,
  ...pick(['onClose', 'resetFormFields'], EditLicensesForm.propTypes),
};
