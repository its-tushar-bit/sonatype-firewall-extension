/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as systemNoticeConfigurationActions from './systemNoticeConfigurationActions';
import SystemNoticeConfiguration from './SystemNoticeConfiguration';

function mapStateToProps({ systemNoticeConfiguration }) {
  return {
    ...systemNoticeConfiguration.formState,
    ...systemNoticeConfiguration.viewState,
  };
}

export default connect(mapStateToProps, systemNoticeConfigurationActions)(SystemNoticeConfiguration);
