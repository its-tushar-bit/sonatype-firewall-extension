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

function mapStateToProps(state) {
  const { status, comment, isDirty, scope, submitError, submitMaskState } = selectEditLicensesForm(state);
  const {
    declaredlicenses,
    effectiveLicenses,
    observedlicenses,
    licenseOverride,
  } = selectLicenseDetectionsTileDataSlice(state);

  return {
    status,
    comment,
    isDirty,
    scope,
    declaredlicenses,
    effectiveLicenses,
    observedlicenses,
    availableLicenseScopes: licenseOverride,
    submitMaskState,
    submitError,
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
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesForm);
