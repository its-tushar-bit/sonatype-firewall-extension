/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import * as licenseObligationAttributionTileActions from './licenseObligationAttributionTileActions';
import LicenseObligationAttributionTile from './LicenseObligationAttributionTile';

function mapStateToProps({ licenseObligationAttributionTile }, ownProps) {
  return {
    ...pick([
      'attributionText',
      'obligationFulfilled',
      'scope'
    ], licenseObligationAttributionTile[ownProps.name])
  };
}

const mapDispatchToProps = { ...licenseObligationAttributionTileActions };

export default connect(mapStateToProps, mapDispatchToProps)(LicenseObligationAttributionTile);
