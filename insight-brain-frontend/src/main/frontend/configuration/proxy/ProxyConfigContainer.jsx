/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import * as proxyConfigActions from './proxyConfigActions';
import ProxyConfig from './ProxyConfig';
import { stateGo } from '../../reduxUiRouter/routerActions';

function mapStateToProps({ proxyConfig }) {
  return {
    ...pick(
      [
        'loading',
        'submitMaskState',
        'submitMaskMessage',
        'deleteMaskState',
        'hasAllRequiredData',
        'isDirty',
        'isValid',
        'mustReenterPassword',
        'loadError',
        'saveError',
        'deleteError',
        'serverData',
        'showDeleteModal',
        'licensed',
      ],
      proxyConfig
    ),
    hostnameState: proxyConfig.formState.hostname,
    portState: proxyConfig.formState.port,
    usernameState: proxyConfig.formState.username,
    passwordState: proxyConfig.formState.password,
    excludeHostsState: proxyConfig.formState.excludeHosts,
  };
}

export default connect(mapStateToProps, { ...proxyConfigActions, stateGo })(ProxyConfig);
