/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import AllLicenseObligationsModal from './AllLicenseObligationsModal';
import { cancelAllObligationsModal, saveAllObligations } from './advancedLegalObligationActions';

function mapStateToProps({ advancedLegal }) {
  return {
    availableScopes: advancedLegal.availableScopes,
    submitMask: advancedLegal.component.component.licenseLegalData.saveAllObligationsSubmitMask,
    errorMessage: advancedLegal.component.component.licenseLegalData.saveAllObligationsError,
  };
}

const mapDispatchToProps = {
  cancelAllObligationsModal,
  saveAllObligations,
};

export default connect(mapStateToProps, mapDispatchToProps)(AllLicenseObligationsModal);
