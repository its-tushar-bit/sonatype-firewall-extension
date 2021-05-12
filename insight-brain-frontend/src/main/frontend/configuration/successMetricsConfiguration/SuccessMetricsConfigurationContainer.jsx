/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as successMetricsConfigurationActions from './successMetricsConfigurationActions';
import SuccessMetricsConfiguration from './SuccessMetricsConfiguration';

function mapStateToProps({ successMetricsConfiguration }) {
  return {
    ...successMetricsConfiguration.formState,
    ...successMetricsConfiguration.viewState,
  };
}

export default connect(mapStateToProps, successMetricsConfigurationActions)(SuccessMetricsConfiguration);
