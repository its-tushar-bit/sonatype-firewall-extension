/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { actions as zscalerConfigActions } from './zscalerConfigSlice';
import { actions as zscalerConfigLimitsActions } from './zscalerConfigLimitsSlice';
import zscalerConfig from './ZscalerConfig';

function mapStateToProps(state) {
  const zscalerConfig = state.zscalerConfig;
  const zscalerConfigLimits = state.zscalerConfigLimits;
  return {
    ...pick(
      [
        'loading',
        'submitMaskState',
        'submitMaskMessage',
        'hasAllRequiredData',
        'hasAllRequiredDataForTestConfig',
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
    configuredFormatState: zscalerConfig.formState.configuredFormatState,
    zscalerConfigLimitsState: zscalerConfigLimits,
  };
}

const mapDispatchToProps = {
  ...zscalerConfigActions,
  loadLimits: zscalerConfigLimitsActions.load,
};

export default connect(mapStateToProps, mapDispatchToProps)(zscalerConfig);
