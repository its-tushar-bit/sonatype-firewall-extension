/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH1, NxLoadWrapper, NxP, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function SbomManagerDashboard() {
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);

  function retryHandler() {}

  return (
    <NxPageMain id="sbom-manager-dashboard">
      <NxLoadWrapper
        retryHandler={retryHandler}
        loading={isProductFeaturesLoading}
        error={errorLoadingProductFeatures || noSbomManagerEnabledError}
      >
        <div>
          <NxPageTitle>
            <NxH1>Dashboard</NxH1>
          </NxPageTitle>
          <NxP>Content for Dashboard</NxP>
        </div>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
