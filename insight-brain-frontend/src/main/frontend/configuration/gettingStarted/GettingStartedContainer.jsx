/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as GettingStartedActions from './gettingStartedActions';
import GettingStarted from './GettingStarted';
import { selectTenantMode } from '../../productFeatures/productFeaturesSelectors';

function mapStateToProps(state) {
  const { gettingStarted, router } = state,
    tenantMode = selectTenantMode(state);

  return {
    tenantMode,
    ...gettingStarted,
    ...router,
  };
}

export default connect(mapStateToProps, GettingStartedActions)(GettingStarted);
