/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import LicenseFileDetailsContents from './LicenseFilesDetailsContents';
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
    loading: component.loading || availableScopes.loading || componentLicenseFileDetails.loadingLicenseDetails,
    error: component.error || availableScopes.error,
    availableScopes,
    componentLicenseFileDetails,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'licenseIndex', 'stageTypeId'], routerParams),
  };
}

const LicenseFilesDetailsContentsContainer = connect(mapStateToProps)(LicenseFileDetailsContents);
export default LicenseFilesDetailsContentsContainer;
