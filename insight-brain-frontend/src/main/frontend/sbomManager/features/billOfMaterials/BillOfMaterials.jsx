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
  NxPageTitle,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import ComponentsBillOfMaterialsTile from 'MainRoot/sbomManager/features/componentsTile/ComponentsBillOfMaterialsTile';
import SbomVersionDropdown from 'MainRoot/sbomManager/features/sbomVersionDropdown/SbomVersionDropdown';
import LoadWrapper from 'MainRoot/react/LoadWrapper';

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
  selectSbomVersions,
  selectErrorSbomVersions,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';

export default function BillOfMaterials() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const routerParams = useSelector(selectRouterCurrentParams);
  const isInternalAppIdLoading = useSelector(selectInternalApplicationIdIsLoading);
  const internalAppIdError = useSelector(selectInternalApplicationIdError);
  const internalAppId = useSelector(selectInternalApplicationId);

  const publicApplicationId = routerParams.applicationPublicId;
  const currentSbomVersion = routerParams.versionId;
  const allSbomVersions = useSelector(selectSbomVersions);
  const errorSbomVersionsDropdown = useSelector(selectErrorSbomVersions);

  const doLoad = () => {
    dispatch(actions.setPublicAppId(publicApplicationId));
    dispatch(actions.loadInternalApplicationId(publicApplicationId));
  };

  useEffect(() => {
    doLoad();
  }, []);

  useEffect(() => {
    if (internalAppId) dispatch(actions.loadApplicationSbomVersions(internalAppId));
  }, [internalAppId]);

  return (
    <div id="sbom-manager-bom">
      <LoadWrapper
        retryHandler={() => doLoad()}
        loading={isProductFeaturesLoading || isInternalAppIdLoading}
        error={
          errorLoadingProductFeatures || noSbomManagerEnabledError || internalAppIdError || errorSbomVersionsDropdown
        }
      >
        <NxPageTitle>
          <NxH1>Bill Of Materials</NxH1>
          <NxButtonBar>
            {allSbomVersions && (
              <SbomVersionDropdown
                publicApplicationId={publicApplicationId}
                allSbomVersions={allSbomVersions}
                currentSbomVersion={currentSbomVersion}
              />
            )}
            <NxButton
              variant="primary"
              onClick={() => window.open(getDownloadSbomFileUrl(internalAppId, currentSbomVersion), '_blank')}
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
          sbomVersion={currentSbomVersion}
          isInternalAppIdLoading={isInternalAppIdLoading}
        />
      </LoadWrapper>
    </div>
  );
}
