/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH2, NxTile } from '@sonatype/react-shared-components';

export default function Overview() {
  return (
    <div>
      <NxH2>Overview</NxH2>

      <NxTile>
        <NxTile.Content>Content...</NxTile.Content>
      </NxTile>
    </div>
  );
}
