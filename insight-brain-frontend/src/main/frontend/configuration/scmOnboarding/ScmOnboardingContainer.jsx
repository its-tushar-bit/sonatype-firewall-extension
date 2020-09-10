/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';

import * as scmOnboardingActions from '../scmOnboarding/scmOnboardingActions';
import ScmOnboarding from '../scmOnboarding/ScmOnboarding';

function mapStateToProps({scmOnboarding}) {
  return {
    loading: scmOnboarding.loading,
    isManifestScanFeatureEnabled: scmOnboarding.isManifestScanFeatureEnabled
  };
}

export default connect(mapStateToProps, scmOnboardingActions)(ScmOnboarding);
