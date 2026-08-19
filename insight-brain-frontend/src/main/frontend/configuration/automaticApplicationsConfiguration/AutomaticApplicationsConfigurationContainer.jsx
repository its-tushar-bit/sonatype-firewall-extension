/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as automaticApplicationsConfigurationActions from './automaticApplicationsConfigurationActions';
import AutomaticApplicationsConfiguration from './AutomaticApplicationsConfiguration.jsx';

function mapStateToProps({ automaticApplicationsConfiguration }) {
  return {
    ...automaticApplicationsConfiguration,
  };
}

export default connect(mapStateToProps, automaticApplicationsConfigurationActions)(AutomaticApplicationsConfiguration);
