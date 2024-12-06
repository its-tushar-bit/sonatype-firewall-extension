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
  NxPageMain,
  NxPageTitle,
  NxStatefulSegmentedButton,
  NxStatefulSubmitMask,
  NxTextLink,
  NxTooltip,
  NxWarningAlert,
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
import InvalidSbomIndicator from '../InvalidSbomIndicator';

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
import { invalidSbomMessageDetails } from '../messages';

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
    validationErrorAlertDismissed,
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
  const dismissSbomInvalidAlert = () => dispatch(actions.dismissSbomInvalidAlert());

  const pdfUrl = getSbomDownloadPdfUrl(publicAppId, currentSbomVersion);

  const { isValid } = sbomMetadata;
  const showSbomInvalidAlert = !isValid && !validationErrorAlertDismissed;

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

  const exportButtonTooltip =
    sbomMetadata.fileFormat &&
    sbomMetadata.specification &&
    `Export the current version of SBOM in ${sbomMetadata.specification} and ${toUpper(
      sbomMetadata.fileFormat
    )} format.`;

  const exportOriginalButtonTooltip = 'Export the original imported SBOM.';

  const segmentButtonContent = isValid ? (
    <NxTooltip title={exportButtonTooltip}>
      <span>
        <NxFontAwesomeIcon icon={faDownload} />
        <span>Export SBOM</span>
      </span>
    </NxTooltip>
  ) : (
    <NxTooltip title={exportOriginalButtonTooltip}>
      <span>
        <NxFontAwesomeIcon icon={faDownload} />
        <span>Export Original SBOM</span>
      </span>
    </NxTooltip>
  );

  const firstChildButton = isValid ? (
    <button className="nx-dropdown-button" onClick={downloadOriginalSbomFile}>
      <NxTooltip title={exportOriginalButtonTooltip}>
        <span>Export Original SBOM</span>
      </NxTooltip>
    </button>
  ) : (
    <button className="nx-dropdown-button disabled" onClick={downloadLatestSbomFile} disabled={true}>
      <NxTooltip title={exportButtonTooltip}>
        <span>Export SBOM</span>
      </NxTooltip>
    </button>
  );

  return (
    <>
      <MenuBarStatefulBreadcrumb />
      <NxPageMain id="sbom-manager-bom" className="sbom-manager-bill-of-materials-page">
        {exportAndDownloadSbomSubmitMask}
        <SbomAdditionalExportOptionsModal />
        <LoadWrapper retryHandler={() => doLoad()} loading={isLoading} error={loadError}>
          {showSbomInvalidAlert && (
            <NxWarningAlert
              id="invalid-sbom-alert"
              role="status"
              aria-labelledby="invalid-sbom-alert-header"
              onClose={dismissSbomInvalidAlert}
            >
              <strong
                id="invalid-sbom-alert-header"
                className="sbom-manager-bill-of-materials-page__invalid-sbom-alert-header"
              >
                Invalid SBOM Detected
              </strong>
              {invalidSbomMessageDetails}
            </NxWarningAlert>
          )}
          <NxPageTitle>
            <NxH1>
              <span>{applicationName}</span>
              {!isValid && validationErrorAlertDismissed && <InvalidSbomIndicator />}
            </NxH1>
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
                onClick={isValid ? downloadLatestSbomFile : downloadOriginalSbomFile}
                buttonContent={segmentButtonContent}
              >
                {firstChildButton}
                <button
                  className={`nx-dropdown-button ${isValid ? '' : 'disabled'}`}
                  onClick={showSbomAdditionalExportOptionsModal}
                  disabled={!isValid}
                >
                  <NxTooltip title="Export SBOM with customized options.">
                    <span>Additional Export Options</span>
                  </NxTooltip>
                </button>
                <NxTextLink className="nx-dropdown-button" href={pdfUrl} disabled={!isValid}>
                  <NxFontAwesomeIcon icon={faFilePdf} />
                  <span>Export PDF</span>
                </NxTextLink>
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
          <BillOfMaterialsComponentsTile />
        </LoadWrapper>
      </NxPageMain>
    </>
  );
}
