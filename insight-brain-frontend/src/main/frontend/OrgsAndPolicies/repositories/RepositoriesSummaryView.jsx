/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxPageMain, NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import RepositoriesConfigurationTile from './RepositoriesConfigurationTile';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';

export default function RepositoriesSummaryView() {
  return (
    <NxPageMain id="repository-page">
      <NxPageTitle id="repositories-summary">
        <NxH1>Repositories</NxH1>
      </NxPageTitle>
      {/*Configuration / Access tabs to go here*/}
      <RepositoriesConfigurationTile />
      <PoliciesTile />
      <AccessTile />
    </NxPageMain>
  );
}
