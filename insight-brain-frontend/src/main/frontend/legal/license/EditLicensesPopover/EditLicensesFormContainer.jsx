/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { saveLicenses, deleteLicenses, setShowLicensesModal } from 'MainRoot/legal/files/advancedLegalFileActions';
import { selectComponentIdentificationSource } from 'MainRoot/componentDetails/componentDetailsSelectors';
import EditLicensesForm from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesForm';
import {
  setLicenseFormComment,
  setLicenseFormLicenseIds,
  setLicenseFormResetFormFields,
  setLicenseFormScope,
  setLicenseFormStatus,
  setShowUnsavedChangesModal,
} from 'MainRoot/legal/advancedLegalActions';

function mapStateToProps(state) {
  const {
    status,
    comment,
    licenseIds,
    isDirty,
    scope,
    submitError,
    submitMaskState,
  } = state.advancedLegal.editLicensesForm;
  const {
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    licenseOverride,
    selectableLicenses,
    allLicenses,
    hiddenObservedLicenses,
    supportAlpObservedLicenses,
  } = state.advancedLegal.multiLicenses;

  const identificationSource = selectComponentIdentificationSource(state);

  return {
    status,
    comment,
    licenseIds,
    isDirty,
    scope,
    allLicenses,
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    selectableLicenses,
    availableLicenseScopes: licenseOverride,
    submitMaskState,
    submitError,
    identificationSource,
    hiddenObservedLicenses,
    supportAlpObservedLicenses,
    isAdvancedLegalPackSupported: true,
  };
}

const onClose = () => setShowLicensesModal(false);
const mapDispatchToProps = {
  onClose,
  resetFormFields: setLicenseFormResetFormFields,
  saveForm: saveLicenses,
  deleteLicenseOverride: deleteLicenses,
  setLicenseStatus: setLicenseFormStatus,
  setLicenseComment: setLicenseFormComment,
  setLicenseScope: setLicenseFormScope,
  setSelectedLicenses: setLicenseFormLicenseIds,
  setShowUnsavedChangesModal: setShowUnsavedChangesModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesForm);
