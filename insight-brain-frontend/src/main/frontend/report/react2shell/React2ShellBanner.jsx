/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxInfoAlert } from '@sonatype/react-shared-components';

export default function React2ShellBanner() {
  return (
    <NxInfoAlert className="iq-react2shell-banner-info">
      If your violating component count is lower than your affected component count, it means the impacted file
      isn&apos;t in your app, even though you use the affected component.
    </NxInfoAlert>
  );
}
