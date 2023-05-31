/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTile } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { steps } from './firewallOnboardingUtils';

const [step] = steps;

export default function ProxyRepositoriesSelector() {
  return (
    <NxTile>
      <NxTile.Content>Proxy repositories selector</NxTile.Content>
      <ActionsFooter currentStep={step} />
    </NxTile>
  );
}
