/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import * as mailConfigActions from './mailConfigActions';
import MailConfig from './MailConfig';

function mapStateToProps({ mailConfig }) {
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
  };
}

export default connect(mapStateToProps, mailConfigActions)(MailConfig);
