/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import EditLicensesPopover from './EditLicensesPopover';
import { setShowLicensesModal } from 'MainRoot/legal/files/advancedLegalFileActions';
import { selectShowEditLicensesPopover } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import { setLicenseFormResetFormFields, setShowUnsavedChangesModal } from 'MainRoot/legal/advancedLegalActions';

function mapStateToProps(state) {
  const { isDirty, showUnsavedChangesModal } = state.advancedLegal.editLicensesForm;
  return {
    showEditLicensesPopover: selectShowEditLicensesPopover(state),
    isDirty,
    showUnsavedChangesModal,
  };
}
const mapDispatchToProps = {
  onClose: setShowLicensesModal,
  setShowUnsavedChangesModal: setShowUnsavedChangesModal,
  resetFormFields: setLicenseFormResetFormFields,
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesPopover);
