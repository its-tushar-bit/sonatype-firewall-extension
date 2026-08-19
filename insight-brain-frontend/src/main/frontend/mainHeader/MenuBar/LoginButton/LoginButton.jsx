/* eslint-disable no-unused-vars */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { faUserAlt } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';

const LoginButton = (props) => (
  <NxButton id="header-login-button" className="iq-login-button" variant="tertiary" {...props}>
    <NxFontAwesomeIcon icon={faUserAlt} />
    <span>Sign in</span>
  </NxButton>
);

export default LoginButton;
