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
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faDownload, faFilePdf } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';
import { toLower, toUpper } from 'ramda';

import LoadWrapper from 'MainRoot/react/LoadWrapper';
import SbomVersionDropdown from 'MainRoot/sbomManager/features/sbomVersionDropdown/SbomVersionDropdown';
import SummaryTile from 'MainRoot/sbomManager/features/billOfMaterials/summaryTile/SummaryTile';
import BillOfMaterialsComponentsTile from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/BillOfMaterialsComponentsTile';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import SbomAdditionalExportOptionsModal from './sbomAdditionalExportOptionsModal/SbomAdditionalExportOptionsModal';

import { getDownloadSbomFileUrl, getSbomDownloadPdfUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
  selectIsSbomPoliciesSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectBillOfMaterialsPage } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import {
  actions,
  EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_EXPORTING_MESSAGE,
  EXPORT_SBOM_FILE_FORMAT,
  EXPORT_SBOM_SPECIFICATION,
  EXPORT_SBOM_STATE,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
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
    policyViolationSummary,
    annotatedVulnerabilitesPercentage,
    exportAndDownloadSbomSubmitMask: exportAndDownloadSbomSubmitMaskState,
  } = useSelector(selectBillOfMaterialsPage);

  const loadingProductFeatures = useSelector(selectLoadingFeatures);
  const errorProductFeatures = useSelector(selectLoadErrorFeatures);
  const errorNoSbomManagerEnabled = useSelector(selectNoSbomManagerEnabledError);
  const isSbomPoliciesSupported = useSelector(selectIsSbomPoliciesSupported);

  const routerParams = useSelector(selectRouterCurrentParams);

  const publicAppId = routerParams.applicationPublicId;
  const currentSbomVersion = routerParams.versionId;

  const showSbomAdditionalExportOptionsModal = () => dispatch(actions.setShowSbomAdditionalExportOptionsModal(true));
  const exportAndDownloadSbom = (options) => dispatch(actions.exportAndDownloadSbom(options));

  const pdfUrl = getSbomDownloadPdfUrl(publicAppId, currentSbomVersion);

  const doLoad = () => {
    dispatch(actions.setPublicAppId(publicAppId));
    dispatch(actions.loadInternalAppId(publicAppId));
    // State for the breadcrumb
    dispatch(ownerSideNavActions.loadOwnerList());
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
    <>
      <MenuBarStatefulBreadcrumb />
      <div id="sbom-manager-bom" className="sbom-manager-bill-of-materials-page">
        {exportAndDownloadSbomSubmitMask}
        <SbomAdditionalExportOptionsModal />
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
                className="sbom-manager-bill-of-materials-page__export-button"
                variant="primary"
                onClick={downloadLatestSbomFile}
                buttonContent={
                  <NxTooltip
                    title={
                      sbomMetadata.fileFormat &&
                      sbomMetadata.specification &&
                      `Export the current version of SBOM in ${sbomMetadata.specification} and ${toUpper(
                        sbomMetadata.fileFormat
                      )} format.`
                    }
                  >
                    <span>
                      <NxFontAwesomeIcon icon={faDownload} />
                      <span>Export SBOM</span>
                    </span>
                  </NxTooltip>
                }
              >
                <button className="nx-dropdown-button" onClick={downloadOriginalSbomFile}>
                  <NxTooltip title="Export the original imported SBOM.">
                    <span>Export Original SBOM</span>
                  </NxTooltip>
                </button>
                <button className="nx-dropdown-button" onClick={showSbomAdditionalExportOptionsModal}>
                  <NxTooltip title="Export SBOM with customized options.">
                    <span>Additional Export Options</span>
                  </NxTooltip>
                </button>
                <a className="nx-dropdown-button" href={pdfUrl}>
                  <NxFontAwesomeIcon icon={faFilePdf} />
                  <span>Export PDF</span>
                </a>
              </NxStatefulSegmentedButton>
            </NxButtonBar>
            <NxPageTitle.Description>
              <div className="sbom-manager-bill-of-materials-page__sub-header">
                <div
                  className="sbom-manager-bill-of-materials-page__sub-header__item"
                  data-testid="bill-of-materials-page-sbom-imported-date"
                >
                  <strong>Imported:</strong>
                  <span id="bill-of-materials-page-imported-date">{formatDate(sbomMetadata.createdAt)}</span>
                </div>
              </div>
            </NxPageTitle.Description>
          </NxPageTitle>
          <SummaryTile
            annotatedVulnerabilitesPercentage={annotatedVulnerabilitesPercentage}
            componentSummary={componentSummary}
            vulnerabilitiesSummary={vulnerabilitiesSummary}
            policyViolationSummary={policyViolationSummary}
            isSbomPoliciesSupported={isSbomPoliciesSupported}
          />
          <BillOfMaterialsComponentsTile internalAppId={internalAppId} />
        </LoadWrapper>
      </div>
    </>
  );
}
