/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import EditLicensesPopover from './EditLicensesPopover';
import { actions } from '../LicenseDetectionsTile/licenseDetectionsTileSlice';
import { selectShowEditLicensesPopover } from '../LicenseDetectionsTile/licenseDetectionsTileSelectors';

function mapStateToProps(state) {
  return {
    showEditLicensesPopover: selectShowEditLicensesPopover(state),
  };
}
const mapDispatchToProps = {
  onClose: actions.toggleShowEditLicensesPopover,
  resetFormFields: actions.resetEditLicensesFormFields,
};

export default connect(mapStateToProps, mapDispatchToProps)(EditLicensesPopover);
