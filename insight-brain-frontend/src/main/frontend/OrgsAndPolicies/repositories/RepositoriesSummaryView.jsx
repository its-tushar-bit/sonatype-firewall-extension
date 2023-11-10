/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxPageMain, NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import RepositoriesConfigurationTile from './RepositoriesConfigurationTile';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import NamespaceConfusionProtectionTile from './namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { useDispatch } from 'react-redux';
import RepositoriesPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesPills';

export default function RepositoriesSummaryView() {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(actions.checkEditIqPermission());
  }, []);

  return (
    <NxPageMain id="repository-page">
      <header>
        <NxPageTitle id="repositories-summary">
          <NxH1>All Repositories</NxH1>
          <RepositoriesPills />
        </NxPageTitle>
      </header>

      {/*Configuration / Access tabs to go here*/}
      <div
        className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
        id="repositories-summary-sections"
      >
        <div id="scrollable-content">
          <RepositoriesConfigurationTile />
          <PoliciesTile />
          <NamespaceConfusionProtectionTile />
          <AccessTile />
        </div>
      </div>
    </NxPageMain>
  );
}
