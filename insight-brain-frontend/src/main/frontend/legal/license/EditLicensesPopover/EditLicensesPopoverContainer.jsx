/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import EditLicensesPopover from './EditLicensesPopover';
import { setShowLicensesModal } from 'MainRoot/legal/files/advancedLegalFileActions';
import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  selectShowEditLicensesPopover,
  selectEditLicensesFormIsDirty,
  selectIsUnsavedChangesModalActive,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import { setLicenseFormResetFormFields } from 'MainRoot/legal/advancedLegalActions';

function mapStateToProps(state) {
  return {
    showEditLicensesPopover: selectShowEditLicensesPopover(state),
    isDirty: selectEditLicensesFormIsDirty(state),
    showUnsavedChangesModal: selectIsUnsavedChangesModalActive(state),
  };
}
const mapDispatchToProps = {
  onClose: setShowLicensesModal,
  setShowUnsavedChangesModal: actions.setShowUnsavedChangesModal,
  resetFormFields: setLicenseFormResetFormFields,
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesPopover);
