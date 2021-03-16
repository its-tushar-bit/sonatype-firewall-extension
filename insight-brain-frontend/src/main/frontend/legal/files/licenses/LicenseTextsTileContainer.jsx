/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { setShowLicensesModal } from '../advancedLegalFileActions';

import LicensesTextsTile from './LicenseTextsTile';

function mapStateToProps({ advancedLegal }) {
  return {
    licenseFiles: advancedLegal.component.component.licenseLegalData.licenseFiles,
    showLicensesModal: advancedLegal.component.component.licenseLegalData.showLicensesModal
  };
}

const mapDispatchToProps = {
  setShowLicensesModal
};

export default connect(mapStateToProps, mapDispatchToProps)(LicensesTextsTile);
