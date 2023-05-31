/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { last } from 'ramda';
import { NxTile } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { steps } from './firewallOnboardingUtils';

const step = last(steps);

export default function FirewallConfigurationOverview() {
  return (
    <NxTile>
      <NxTile.Content>Firewall configuration overview</NxTile.Content>
      <ActionsFooter currentStep={step} />
    </NxTile>
  );
}
