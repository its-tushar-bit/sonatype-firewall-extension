/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import { loadComponentAndLicenseDetails } from './componentLicenseFilesDetailsActions';
import { setShowLicenseFilesModal } from '../advancedLegalFileActions';
import LicenseFilesDetailsHeader from './LicenseFilesDetailsHeader';
import { licenseDetailsStateName } from './common';

function mapStateToProps({ advancedLegal, router, componentLicenseFileDetails }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};

  let routerParams = router.currentParams;
  if (
    !router.currentState.name.endsWith(licenseDetailsStateName) &&
    router.prevState.name.endsWith(licenseDetailsStateName)
  ) {
    routerParams = router.prevParams;
  }
  return {
    loading: component.loading || availableScopes.loading || componentLicenseFileDetails.loadingLicenseDetails,
    error: component.error || availableScopes.error,
    showLicenseFilesModal: component.component ? component.component.licenseLegalData.showLicenseFilesModal : false,
    availableScopes,
    ...pick(['component'], component),
    ...pick(['hash', 'componentIdentifier', 'ownerType', 'ownerId', 'licenseIndex', 'stageTypeId'], routerParams),
  };
}

const mapDispatchToProps = {
  loadComponentAndLicenseDetails,
  setShowLicenseFilesModal,
};

const LicenseFilesDetailsHeaderContainer = connect(mapStateToProps, mapDispatchToProps)(LicenseFilesDetailsHeader);
export default LicenseFilesDetailsHeaderContainer;
