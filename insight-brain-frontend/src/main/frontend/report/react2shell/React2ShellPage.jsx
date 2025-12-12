/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxPageMain } from '@sonatype/react-shared-components';
import React2ShellHeader from './React2ShellHeader';
import React2ShellAbout from './React2ShellAbout';

export default function React2ShellPage() {
  return (
    <NxPageMain className="iq-react2shell-page">
      <React2ShellHeader />
      <React2ShellAbout />
    </NxPageMain>
  );
}
