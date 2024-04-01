/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import ComponentsBillOfMaterialsTile from 'MainRoot/sbomManager/features/componentsTile/ComponentsBillOfMaterialsTile';

import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import {
  selectInternalApplicationId,
  selectInternalApplicationIdError,
  selectInternalApplicationIdIsLoading,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';

export default function BillOfMaterials() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const routerParams = useSelector(selectRouterCurrentParams);
  const internalAppId = useSelector(selectInternalApplicationId);
  const isInternalAppIdLoading = useSelector(selectInternalApplicationIdIsLoading);
  const internalAppIdError = useSelector(selectInternalApplicationIdError);

  const publicApplicationId = routerParams.applicationPublicId;
  const sbomVersion = routerParams.versionId;

  const doLoad = () => {
    dispatch(actions.setPublicAppId(publicApplicationId));
    dispatch(actions.loadInternalApplicationId(publicApplicationId));
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div id="sbom-manager-bom">
      <NxLoadWrapper
        retryHandler={() => {
          doLoad();
        }}
        loading={isProductFeaturesLoading || isInternalAppIdLoading}
        error={errorLoadingProductFeatures || noSbomManagerEnabledError || internalAppIdError}
      >
        <NxPageTitle>
          <NxH1>Bill Of Materials</NxH1>
          <NxButtonBar>
            <NxButton
              variant="primary"
              onClick={() => window.open(getDownloadSbomFileUrl(internalAppId, sbomVersion), '_blank')}
            >
              <NxFontAwesomeIcon icon={faDownload} />
              <span>Download</span>
            </NxButton>
          </NxButtonBar>
          <NxPageTitle.Tags>
            <NxThreatIndicator threatLevelCategory="critical" presentational />
            <span>Critical</span>
            <NxThreatIndicator threatLevelCategory="severe" presentational />
            <span>High</span>
            <NxThreatIndicator threatLevelCategory="moderate" presentational />
            <span>Medium</span>
            <NxThreatIndicator threatLevelCategory="low" presentational />
            <span>Low</span>
            <NxThreatIndicator threatLevelCategory="none" presentational />
            <span>None</span>
          </NxPageTitle.Tags>
        </NxPageTitle>
        <ComponentsBillOfMaterialsTile
          internalAppId={internalAppId}
          sbomVersion={sbomVersion}
          isInternalAppIdLoading={isInternalAppIdLoading}
        />
      </NxLoadWrapper>
    </div>
  );
}
