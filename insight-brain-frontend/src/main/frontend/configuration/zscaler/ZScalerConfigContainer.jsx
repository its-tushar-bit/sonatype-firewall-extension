/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { actions } from './zscalerConfigSlice';
import zscalerConfig from './ZScalerConfig';

function mapStateToProps(state) {
  const zscalerConfig = state.zscalerConfig;
  return {
    ...pick(
      [
        'loading',
        'submitMaskState',
        'submitMaskMessage',
        'hasAllRequiredData',
        'isDirty',
        'isValid',
        'mustReenterPassword',
        'loadError',
        'saveError',
        'deleteError',
        'testConfigError',
        'testConfigSuccess',
        'serverData',
        'showDeleteModal',
      ],
      zscalerConfig
    ),
    usernameState: zscalerConfig.formState.username,
    passwordState: zscalerConfig.formState.password,
    hostnameState: zscalerConfig.formState.hostname,
    apiKeyState: zscalerConfig.formState.apiKey,
    eulaState: zscalerConfig.formState.eula,
  };
}

export default connect(mapStateToProps, actions)(zscalerConfig);
