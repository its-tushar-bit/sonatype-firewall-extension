/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH1, NxLoadError, NxLoadWrapper, NxP, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import {
  selectIsSbomManagerEnabled,
  selectLoadErrorFeatures,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function SbomManagerDashboard() {
  const isSbomManagerEnabled = useSelector(selectIsSbomManagerEnabled);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);

  const error = <NxLoadError error="The SBOM Manager license feature is not enabled." retryHandler={retryHandler} />;

  const dashboard = (
    <div>
      <NxPageTitle>
        <NxH1>Dashboard</NxH1>
      </NxPageTitle>
      <NxP>Content for Dashboard</NxP>
    </div>
  );

  function retryHandler() {}

  return (
    <NxPageMain id="sbom-manager-dashboard">
      <NxLoadWrapper retryHandler={() => {}} loading={isProductFeaturesLoading} error={errorLoadingProductFeatures}>
        {isSbomManagerEnabled ? dashboard : error}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
