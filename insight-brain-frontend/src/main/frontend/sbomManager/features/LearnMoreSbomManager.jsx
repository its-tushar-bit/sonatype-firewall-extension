/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxPageMain, NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
export default function LearnMoreSbomManager() {
  return (
    <NxPageMain>
      <NxInfoAlert>
        SBOM Manager is currently not enabled for your organization.
        <br />
        <NxTextLink href="http://links.sonatype.com/products/sbom-manager-learn-more" external>
          Learn more about SBOM Manager.
        </NxTextLink>
      </NxInfoAlert>
    </NxPageMain>
  );
}
