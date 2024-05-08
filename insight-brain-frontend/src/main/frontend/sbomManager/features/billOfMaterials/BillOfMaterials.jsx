/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxButton, NxButtonBar, NxFontAwesomeIcon, NxH1, NxPageTitle } from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';

import LoadWrapper from 'MainRoot/react/LoadWrapper';
import ComponentsBillOfMaterialsTile from 'MainRoot/sbomManager/features/componentsTile/ComponentsBillOfMaterialsTile';
import SbomVersionDropdown from 'MainRoot/sbomManager/features/sbomVersionDropdown/SbomVersionDropdown';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectBillOfMaterialsPage } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import { formatDate } from 'MainRoot/util/dateUtils';

import { actions } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';

export default function BillOfMaterials() {
  const dispatch = useDispatch();
  const {
    loadingInternalAppId,
    loadingSbomVersions,
    loadingSbomMetadata,
    errorInternalAppId,
    errorSbomVersions,
    errorSbomMetadata,
    internalAppId,
    applicationName,
    sbomVersions,
    sbomMetadata,
  } = useSelector(selectBillOfMaterialsPage);

  const loadingProductFeatures = useSelector(selectLoadingFeatures);
  const errorProductFeatures = useSelector(selectLoadErrorFeatures);
  const errorNoSbomManagerEnabled = useSelector(selectNoSbomManagerEnabledError);

  const routerParams = useSelector(selectRouterCurrentParams);

  const publicAppId = routerParams.applicationPublicId;
  const currentSbomVersion = routerParams.versionId;

  const doLoad = () => {
    dispatch(actions.setPublicAppId(publicAppId));
    dispatch(actions.loadInternalAppId(publicAppId));
  };

  useEffect(() => {
    doLoad();
  }, []);

  useEffect(() => {
    if (internalAppId) {
      dispatch(actions.loadApplicationSbomVersions(internalAppId));
      dispatch(
        actions.loadSbomMetadata({
          internalAppId,
          version: currentSbomVersion,
        })
      );
    }
  }, [internalAppId]);

  const loadError =
    errorProductFeatures || errorNoSbomManagerEnabled || errorInternalAppId || errorSbomVersions || errorSbomMetadata;
  const isLoading = loadingProductFeatures || loadingInternalAppId || loadingSbomVersions || loadingSbomMetadata;

  return (
    <div id="sbom-manager-bom" className="sbom-manager-bill-of-materials-page">
      <LoadWrapper retryHandler={() => doLoad()} loading={isLoading} error={loadError}>
        <NxPageTitle>
          <NxH1>{applicationName}</NxH1>
          <NxButtonBar>
            {sbomVersions && (
              <SbomVersionDropdown
                publicAppId={publicAppId}
                sbomVersions={sbomVersions}
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
          <NxPageTitle.Description>
            <div className="sbom-manager-bill-of-materials-page__sub-header">
              <div
                className="sbom-manager-bill-of-materials-page__sub-header__item"
                data-testid="bill-of-materials-page-sbom-imported-date"
              >
                <strong>Imported:</strong>
                <span>{formatDate(sbomMetadata.createdAt)}</span>
              </div>
            </div>
          </NxPageTitle.Description>
        </NxPageTitle>
        <ComponentsBillOfMaterialsTile
          isInternalAppIdLoading={loadingInternalAppId}
          internalAppId={internalAppId}
          sbomVersion={currentSbomVersion}
        />
      </LoadWrapper>
    </div>
  );
}
