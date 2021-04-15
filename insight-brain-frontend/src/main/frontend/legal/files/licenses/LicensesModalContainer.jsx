/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import LicensesModal from './LicensesModal';
import {
  addLicense,
  cancelLicensesModal,
  saveLicenses,
  setLicenseContent,
  setLicensesScope,
  setLicenseStatus,
} from '../advancedLegalFileActions';
import {
  setObligationScope,
  setObligationStatus,
} from '../../obligation/advancedLegalObligationActions';

function mapStateToProps({ advancedLegal }) {
  return {
    scope:
      advancedLegal.component.component.licenseLegalData
        .componentLicensesScopeOwnerId,
    originalScope:
      advancedLegal.component.component.licenseLegalData
        .originalComponentLicensesScopeOwnerId,
    availableScopes: advancedLegal.availableScopes,
    licenses: advancedLegal.component.component.licenseLegalData.licenseFiles,
    error: advancedLegal.component.component.licenseLegalData.licensesError,
    submitMaskState:
      advancedLegal.component.component.licenseLegalData.saveLicensesSubmitMask,
    existingObligation: advancedLegal.component.component.licenseLegalData.obligations.find(
      (o) => o.name === 'Inclusion of License'
    ),
  };
}

const mapDispatchToProps = {
  cancelLicensesModal,
  setLicenseContent,
  setLicenseStatus,
  addLicense,
  setLicensesScope,
  saveLicenses,
  setObligationScope,
  setObligationStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(LicensesModal);
