/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import EditLicensesForm from './EditLicensesForm';
import { actions } from '../LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  selectEditLicensesForm,
  selectLicenseDetectionsTileDataSlice,
} from '../LicenseDetectionsTile/licenseDetectionsTileSelectors';
import { selectComponentIdentificationSource } from '../../componentDetailsSelectors';

function mapStateToProps(state) {
  const { status, comment, licenseIds, isDirty, scope, submitError, submitMaskState } = selectEditLicensesForm(state);
  const {
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    licenseOverride,
    selectableLicenses,
    allLicenses,
    hiddenObservedLicenses,
    supportAlpObservedLicenses,
    isAdvancedLegalPackSupported,
  } = selectLicenseDetectionsTileDataSlice(state);
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
    isAdvancedLegalPackSupported,
  };
}
const mapDispatchToProps = {
  onClose: actions.toggleShowEditLicensesPopover,
  resetFormFields: actions.resetEditLicensesFormFields,
  saveForm: actions.saveEditLicensesForm,
  deleteLicenseOverride: actions.deleteLicenseOverride,
  setLicenseStatus: actions.setLicenseFormStatus,
  setLicenseComment: actions.setLicenseFormComment,
  setLicenseScope: actions.setLicenseFormScope,
  setSelectedLicenses: actions.setLicenseFormLicenseIds,
  setShowUnsavedChangesModal: actions.setShowUnsavedChangesModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesForm);
