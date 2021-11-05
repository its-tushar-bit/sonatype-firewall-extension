/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import LicenseFilesDetailsList from './LicenseFilesDetailsList';
import { licenseDetailsStateName } from './common';

function mapStateToProps({ advancedLegal, componentLicenseFileDetails, router }) {
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
    componentLicenseFileDetails,
    loading: component.loading || availableScopes.loading || componentLicenseFileDetails.loadingLicenseDetails,
    error: component.error || availableScopes.error,
    ...pick(['component'], component),
    ...pick(['hash', 'componentIdentifier', 'ownerType', 'ownerId', 'stageTypeId', 'licenseIndex'], routerParams),
  };
}

const LicenseFilesDetailsListContainer = connect(mapStateToProps)(LicenseFilesDetailsList);
export default LicenseFilesDetailsListContainer;
LicenseFilesDetailsListContainer.propTypes = pick(['$state'], LicenseFilesDetailsList.propTypes);
