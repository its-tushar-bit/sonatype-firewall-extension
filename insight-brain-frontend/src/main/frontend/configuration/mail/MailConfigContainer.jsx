/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { connect, useSelector, useDispatch } from 'react-redux';
import { pick } from 'ramda';

import { actions } from './mailConfigSlice';
import MailConfig from './MailConfig';
import {
  selectIsShowEmailStoppedEnabled,
  selectIsEmailConfigurationEnabled,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { setError } from 'MainRoot/session/appErrorSlice';

function mapStateToProps(state) {
  const mailConfig = state.mailConfig;
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
        'testEmailError',
        'serverData',
        'showDeleteModal',
        'testEmailSent',
        'fipsStatusLoading',
        'fipsStatusError',
      ],
      mailConfig
    ),
    hostnameState: mailConfig.formState.hostname,
    portState: mailConfig.formState.port,
    usernameState: mailConfig.formState.username,
    passwordState: mailConfig.formState.password,
    sslEnabledState: mailConfig.formState.sslEnabled,
    startTlsEnabledState: mailConfig.formState.startTlsEnabled,
    systemEmailState: mailConfig.formState.systemEmail,
    testEmailState: mailConfig.formState.testEmail,
    isEmailStopped: selectIsShowEmailStoppedEnabled(state),
    isFipsEnabled: mailConfig.isFipsEnabled,
  };
}

const ConnectedMailConfig = connect(mapStateToProps, actions)(MailConfig);

// CLM-38607: Gate mail config page behind email-configuration feature flag.
// When the feature is disabled, dispatches the same 'Unknown Address' error used by non-existent routes,
// which hides the ui-view and shows the standard error page via ng-if="error" in index.html.
export default function MailConfigContainer(props) {
  const isEmailConfigEnabled = useSelector(selectIsEmailConfigurationEnabled);
  const isFeaturesLoading = useSelector(selectLoadingFeatures);
  const dispatch = useDispatch();
  const shouldBlock = !isFeaturesLoading && !isEmailConfigEnabled;

  useEffect(() => {
    if (shouldBlock) {
      dispatch(setError('Unknown Address'));
    }
  }, [shouldBlock]);

  if (shouldBlock || isFeaturesLoading) {
    return null;
  }

  return <ConnectedMailConfig {...props} />;
}
