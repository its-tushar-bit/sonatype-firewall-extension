/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import FirewallOverviewComponentInformation from './FirewallOverviewComponentInformation';
import { selectFirewallComponentDetailsPage } from 'MainRoot/firewall/firewallSelectors';
import { loadComponentDetails } from 'MainRoot/firewall/firewallActions';

export default function FirewallOverviewComponentInformationTile() {
  const { isLoadingComponentDetails, componentDetailsError, componentDetails } = useSelector(
    selectFirewallComponentDetailsPage
  );

  return (
    <NxLoadWrapper
      loading={isLoadingComponentDetails}
      error={componentDetailsError}
      retryHandler={loadComponentDetails(componentDetails)}
    >
      <FirewallOverviewComponentInformation />
    </NxLoadWrapper>
  );
}
