/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxErrorAlert, NxH1, NxPageTitle } from '@sonatype/react-shared-components';
import React from 'react';

export const DEVELOPER_FEATURE_DISABLED_MESSAGE = 'Sonatype Developer is not enabled.';

export default function LicenseLockScreen() {
  return (
    <>
      <NxPageTitle>
        <NxH1>Sonatype Developer</NxH1>
      </NxPageTitle>

      <NxErrorAlert data-testid="iq-integrations__missing-license">{DEVELOPER_FEATURE_DISABLED_MESSAGE}</NxErrorAlert>
    </>
  );
}
