/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxPageMain, NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { useDispatch } from 'react-redux';

export default function RepositoryManagerSummaryView() {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(actions.checkEditIqPermission());
  }, []);

  return (
    <NxPageMain id="repository-manager-page">
      <header>
        <NxPageTitle id="repository-manager-summary">
          <NxH1>Repository Manager Name</NxH1>
        </NxPageTitle>
      </header>
      <div
        className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
        id="repository-manager-summary-sections"
      >
        <div id="scrollable-content"></div>
      </div>
    </NxPageMain>
  );
}
