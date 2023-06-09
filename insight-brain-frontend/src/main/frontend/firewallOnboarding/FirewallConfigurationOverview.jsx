/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { last } from 'ramda';
import { useSelector } from 'react-redux';
import { NxH2, NxTile } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { steps } from './firewallOnboardingUtils';
import { selectTotalEnabledRepositoriesByTypeAndProp } from './firewallOnboardingSelectors';
import logo from '../img/inspect_and_complete_page_image.svg';

const step = last(steps);

export default function FirewallConfigurationOverview() {
  const totalEnabledProxyRepositories = useSelector((state) =>
    selectTotalEnabledRepositoriesByTypeAndProp(state, 'proxy', 'quarantineEnabled')
  );

  return (
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Congratulations, you’re all set!</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        Once you launch Firewall, <b>malicious blocking</b> will be enabled for{' '}
        <b data-testid="proxy-repositories-count">{totalEnabledProxyRepositories}</b> proxy repositories.
        <div className="logo-container">
          <img src={logo} alt="Inspect and complete logo" />
        </div>
      </NxTile.Content>
      <ActionsFooter currentStep={step} />
    </NxTile>
  );
}
