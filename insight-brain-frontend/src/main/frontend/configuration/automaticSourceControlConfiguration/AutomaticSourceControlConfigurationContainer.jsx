/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import AutomaticSourceControlConfiguration from './AutomaticSourceControlConfiguration';
import * as automaticSourceControlConfigurationActions from './automaticSourceControlConfigurationActions';

function mapStateToProps({ automaticSourceControlConfiguration }) {
  return {
    ...automaticSourceControlConfiguration.formState,
    ...automaticSourceControlConfiguration.viewState,
  };
}

export default connect(
  mapStateToProps,
  automaticSourceControlConfigurationActions
)(AutomaticSourceControlConfiguration);
