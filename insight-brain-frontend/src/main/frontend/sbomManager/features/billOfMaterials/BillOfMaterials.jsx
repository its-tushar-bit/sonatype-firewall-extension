/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import {
  NxButtonBar,
  NxFontAwesomeIcon,
  NxH1,
  NxPageTitle,
  NxStatefulSegmentedButton,
  NxStatefulSubmitMask,
} from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';
import { toLower } from 'ramda';

import LoadWrapper from 'MainRoot/react/LoadWrapper';
import SbomVersionDropdown from 'MainRoot/sbomManager/features/sbomVersionDropdown/SbomVersionDropdown';
import SummaryTile from 'MainRoot/sbomManager/features/billOfMaterials/summaryTile/SummaryTile';
import BillOfMaterialsComponentsTile from 'MainRoot/sbomManager/features/billOfMaterialsComponentsTile/BillOfMaterialsComponentsTile';
import ExportAugmentedSbomModal from './exportAugmentedSbomModal/ExportAugmentedSbomModal';

import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectBillOfMaterialsPage } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import {
  actions,
  EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_EXPORTING_MESSAGE,
  EXPORT_SBOM_FILE_FORMAT,
  EXPORT_SBOM_SPECIFICATION,
  EXPORT_SBOM_STATE,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { formatDate } from 'MainRoot/util/dateUtils';

import './billOfMaterials.scss';

export default function BillOfMaterials() {
  const dispatch = useDispatch();
  const {
    loadingInternalAppId,
    loadingSbomVersions,
    loadingSbomMetadata,
    loadingSbomSummary,

    errorInternalAppId,
    errorSbomVersions,
    errorSbomMetadata,
    errorSbomSummary,

    internalAppId,
    applicationName,
    sbomVersions,
    sbomMetadata,
    componentSummary,
    vulnerabilitiesSummary,
    annotatedVulnerabilitesPercentage,
    exportAndDownloadSbomSubmitMask: exportAndDownloadSbomSubmitMaskState,
  } = useSelector(selectBillOfMaterialsPage);

  const loadingProductFeatures = useSelector(selectLoadingFeatures);
  const errorProductFeatures = useSelector(selectLoadErrorFeatures);
  const errorNoSbomManagerEnabled = useSelector(selectNoSbomManagerEnabledError);

  const routerParams = useSelector(selectRouterCurrentParams);

  const publicAppId = routerParams.applicationPublicId;
  const currentSbomVersion = routerParams.versionId;

  const showExportAugmentedSbomModal = () => dispatch(actions.setShowExportAugmentedSbomModal(true));
  const exportAndDownloadSbom = (options) => dispatch(actions.exportAndDownloadSbom(options));

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
      const params = {
        internalAppId,
        version: currentSbomVersion,
      };
      dispatch(actions.loadSbomMetadata(params));
      dispatch(actions.loadSbomSummary(params));
    }
  }, [internalAppId]);

  const loadError =
    errorProductFeatures ||
    errorNoSbomManagerEnabled ||
    errorInternalAppId ||
    errorSbomVersions ||
    errorSbomMetadata ||
    errorSbomSummary;
  const isLoading =
    loadingProductFeatures || loadingInternalAppId || loadingSbomVersions || loadingSbomMetadata || loadingSbomSummary;

  const downloadLatestSbomFile = () =>
    exportAndDownloadSbom({
      state: EXPORT_SBOM_STATE.current,
      specification: EXPORT_SBOM_SPECIFICATION[toLower(sbomMetadata.specification)],
      fileFormat: EXPORT_SBOM_FILE_FORMAT[toLower(sbomMetadata.fileFormat)],
    });

  const downloadOriginalSbomFile = () =>
    window.open(getDownloadSbomFileUrl(internalAppId, currentSbomVersion), '_blank');

  const exportAndDownloadSbomSubmitMask = exportAndDownloadSbomSubmitMaskState.showSubmitMask ? (
    <NxStatefulSubmitMask
      success={exportAndDownloadSbomSubmitMaskState.success}
      successMessage={exportAndDownloadSbomSubmitMaskState.successMessage}
      message={EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_EXPORTING_MESSAGE}
    />
  ) : null;

  return (
    <div id="sbom-manager-bom" className="sbom-manager-bill-of-materials-page">
      {exportAndDownloadSbomSubmitMask}
      <ExportAugmentedSbomModal />
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
            <NxStatefulSegmentedButton
              variant="primary"
              onClick={downloadLatestSbomFile}
              buttonContent={
                <>
                  <NxFontAwesomeIcon icon={faDownload} />
                  <span>Export</span>
                </>
              }
            >
              <button className="nx-dropdown-button" onClick={downloadOriginalSbomFile}>
                Download Original SBOM
              </button>
              <button className="nx-dropdown-button" onClick={showExportAugmentedSbomModal}>
                Export Augmented
              </button>
            </NxStatefulSegmentedButton>
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
        <SummaryTile
          annotatedVulnerabilitesPercentage={annotatedVulnerabilitesPercentage}
          componentSummary={componentSummary}
          vulnerabilitiesSummary={vulnerabilitiesSummary}
        />
        <BillOfMaterialsComponentsTile internalAppId={internalAppId} />
      </LoadWrapper>
    </div>
  );
}
