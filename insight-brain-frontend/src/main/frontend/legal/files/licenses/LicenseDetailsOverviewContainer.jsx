/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import LicenseFilesDetailsOverview from './LicenseFilesDetailsOverview';

function mapStateToProps({ advancedLegal, componentLicenseFileDetails }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};

  return {
    componentLicenseFileDetails,
    loading: component.loading || availableScopes.loading || componentLicenseFileDetails.loadingLicenseDetails,
    error: component.error || availableScopes.error,
    ...pick(['component'], component),
  };
}

const LicenseDetailsOverviewContainer = connect(mapStateToProps)(LicenseFilesDetailsOverview);
export default LicenseDetailsOverviewContainer;
