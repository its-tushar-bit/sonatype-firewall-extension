/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useState, useRef } from 'react';
import {
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxPageMain,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
  NxTag,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
  selectIsSbomPoliciesSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { ComponentDetailsHeader, ComponentDetailsTags, Title } from 'MainRoot/componentDetails/ComponentDetailsHeader';
import VulnerabilitiesTile from 'MainRoot/sbomManager/features/componentDetails/VulnerabilitiesTile';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import {
  selectComponentDetails,
  selectComponentPagination,
  selectCopyError,
  selectCopyMaskState,
  selectDeleteError,
  selectDeleteMaskState,
  selectIsLoading,
  selectJustificationsReferenceData,
  selectLoadError,
  selectResponsesReferenceData,
  selectSbomComponentDetails,
  selectShowCopyModal,
  selectShowDeleteModal,
  selectStatesReferenceData,
  selectInternalAppId,
  selectComponentsCurrentPage,
  selectSelectedComponentIndex,
  selectComponentsPagesData,
  selectTotalNumberOfPages,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import ComponentDetailsSbomInfo from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsSbomInfo';
import ComponentSummary from 'MainRoot/sbomManager/features/componentDetails/ComponentSummary';
import SbomVulnerabilityDetailsPopover from 'MainRoot/sbomManager/features/componentDetails/vulnerabilitiesDrawer/SbomVulnerabilityDetailsPopover';

import PolicyViolationsTile from 'MainRoot/sbomManager/features/componentDetails/policyViolationsTile/PolicyViolationsTile';
import PolicyViolationDetailsDrawer from 'MainRoot/sbomManager/features/componentDetails//policyViolationDetailsDrawer/PolicyViolationDetailsDrawer';

import VexAnnotationDrawer, {
  pickFirstVexResponse,
} from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';
import { isNil } from 'ramda';
import DeleteAnnotationModal from 'MainRoot/sbomManager/features/componentDetails/DeleteAnnotationModal';
import CopyAnnotationModal from 'MainRoot/sbomManager/features/componentDetails/CopyAnnotationModal';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { ComponentDetailsFooter } from 'MainRoot/componentDetails/ComponentDetailsFooter';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function ComponentDetailsPage() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const isLoading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLoadError);
  const uiRouterStateService = useRouterState();
  const pagination = useSelector((state) => selectComponentPagination(state, uiRouterStateService));
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const componentDetails = useSelector(selectComponentDetails);
  const routerParams = useSelector(selectRouterCurrentParams);
  const justificationsOptions = useSelector(selectJustificationsReferenceData);
  const responsesOptions = useSelector(selectResponsesReferenceData);
  const analysisStatusesOptions = useSelector(selectStatesReferenceData);
  const {
    activeTabIndex,
    disclosedVulnerabilitiesSortConfiguration,
    additionalVulnerabilitiesSortConfiguration,
  } = useSelector(selectSbomComponentDetails);
  const showDeleteModal = useSelector(selectShowDeleteModal);
  const deleteError = useSelector(selectDeleteError);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const showCopyModal = useSelector(selectShowCopyModal);
  const copyError = useSelector(selectCopyError);
  const copyMaskState = useSelector(selectCopyMaskState);
  const isSbomPoliciesSupported = useSelector(selectIsSbomPoliciesSupported);
  const currentComponentsPage = useSelector(selectComponentsCurrentPage);
  const currentSelectedComponentIndex = useSelector(selectSelectedComponentIndex);
  const pagesData = useSelector(selectComponentsPagesData);
  const pagesCount = useSelector(selectTotalNumberOfPages);

  const { applicationPublicId, sbomVersion, componentHash } = routerParams;
  const isMounted = useRef(true);

  useEffect(
    () => () => {
      isMounted.current = false;
    },
    []
  );

  const internalAppId = useSelector(selectInternalAppId);
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const [isVexAnnotationPopoverOpen, setIsVexAnnotationPopoverOpen] = useState(false);
  const [isPurlCopied, setIsPurlCopied] = useState(false);

  const vulnerability = {};
  const [selectedVulnerability, setSelectedVulnerability] = useState(vulnerability);
  const load = () => dispatch(actions.loadComponentDetails({ internalAppId, sbomVersion, componentHash }));
  const setActiveTabIndex = (index) => dispatch(actions.setActiveTabIndex(index));

  const loadSbomComponentVulnerabilities = (vulnerability) =>
    dispatch(
      actions.loadVulnerabilityDetails({
        vulnerability: { refId: vulnerability.issue, source: vulnerability.identificationSources },
        componentIdentifier: componentDetails?.componentIdentifier,
        extraParams: {
          ownerId: applicationPublicId,
          hash: componentDetails?.hash,
          isRepository: false,
          scanId: componentDetails?.metadata?.scanId,
        },
      })
    );

  const deleteVexAnnotation = () => {
    dispatch(
      actions.deleteVexAnnotation({
        internalAppId,
        sbomVersion,
        vulnerabilityRefId: selectedVulnerability.issue,
        componentLocator: { hash: componentDetails?.hash, packageUrl: componentDetails?.packageUrl },
      })
    );
  };

  const copyVexAnnotation = () => {
    const copyRequestObject = {
      componentLocator: {
        hash: componentDetails?.hash,
      },
      vulnerabilityAnalysis: {
        state: selectedVulnerability.latestPreviousAnnotation.analysisStatus,
        justification: selectedVulnerability.latestPreviousAnnotation.justification,
        response: pickFirstVexResponse(selectedVulnerability.latestPreviousAnnotation.response),
        detail: selectedVulnerability.latestPreviousAnnotation.detail,
      },
    };

    const copyPayload = {
      internalAppId,
      sbomVersion,
      vulnerabilityRefId: selectedVulnerability.issue,
      vexAnnotationFormData: copyRequestObject,
    };

    dispatch(actions.copyVexAnnotation(copyPayload));
  };

  const loadVexReferenceData = () => {
    dispatch(actions.getVulnerabilityAnalysisReferenceData());
  };

  const loadStateForBreadcrum = () => dispatch(ownerSideNavActions.loadOwnerList());

  const loadInternalAppId = () => dispatch(actions.loadInternalAppId(applicationPublicId));

  const updateComponentCurrentPage = () => dispatch(actions.updateCurrentPage(componentHash));

  const cycleDisclosedVulnerabilitiesSortDirection = (sortBy) =>
    dispatch(actions.cycleDisclosedVulnerabilitiesSortDirection({ sortBy }));
  const cycleAdditionalVulnerabilitiesSortDirection = (sortBy) =>
    dispatch(actions.cycleAdditionalVulnerabilitiesSortDirection({ sortBy }));

  const handleComponentPageQuery = () => {
    const components = pagesData?.[currentComponentsPage];
    if (
      currentComponentsPage !== undefined &&
      currentSelectedComponentIndex !== undefined &&
      currentSelectedComponentIndex !== -1
    ) {
      const pageToQuery = getPageToQuery(components);
      if (pageToQuery !== null && !pagesData[pageToQuery]) {
        dispatch(actions.setComponentsNextPage(pageToQuery));
        dispatch(actions.loadComponents({ internalAppId, sbomVersion, pageToQuery }));
      }
    }
  };

  const getPageToQuery = (components) => {
    if (currentSelectedComponentIndex === components.length - 1 && currentComponentsPage < pagesCount - 1) {
      return currentComponentsPage + 1;
    }
    if (currentSelectedComponentIndex === 0 && currentComponentsPage > 0) {
      return currentComponentsPage - 1;
    }
    return null;
  };

  useEffect(() => {
    loadInternalAppId();
    loadVexReferenceData();
    loadStateForBreadcrum();
  }, []);

  useEffect(() => {
    if (internalAppId) {
      handleComponentPageQuery();
      updateComponentCurrentPage();
      load();
    }
  }, [internalAppId]);

  const copyToClipboard = async (value) => {
    try {
      await navigator.clipboard.writeText(value);
      if (isMounted.current) setIsPurlCopied(true);
      setTimeout(() => {
        if (isMounted.current) setIsPurlCopied(false);
      }, 2000);
    } catch (err) {}
  };

  const closeVulnerabilityDetailsModal = () => setIsPopoverOpen(false);

  const openVulnerabilityDetailsModal = (vulnerability) => {
    setSelectedVulnerability(vulnerability);
    setIsPopoverOpen(true);
    setIsVexAnnotationPopoverOpen(false);
    loadSbomComponentVulnerabilities(vulnerability);
  };

  const closeVexAnnotationModal = () => {
    if (isMounted.current) setIsVexAnnotationPopoverOpen(false);
  };

  const openVexAnnotationModal = (vulnerabilityRow) => {
    setSelectedVulnerability(vulnerabilityRow);
    setIsVexAnnotationPopoverOpen(true);
    setIsPopoverOpen(false);
  };

  const preSaveMaskActions = () => {
    load();
  };

  const postSaveMaskActions = () => {
    closeVexAnnotationModal();
  };

  const onLearnMoreClick = () => {
    closeVexAnnotationModal();
    openVulnerabilityDetailsModal({
      issue: selectedVulnerability?.issue,
    });
  };

  const openDeleteModal = (vulnerability) => {
    dispatch(actions.setShowDeleteModal(true));
    setSelectedVulnerability(vulnerability);
  };

  const cancelDeleteModal = () => {
    dispatch(actions.setShowDeleteModal(false));
    setSelectedVulnerability({});
  };

  const openCopyModal = (vulnerability) => {
    dispatch(actions.setShowCopyModal(true));
    setSelectedVulnerability(vulnerability);
  };

  const cancelCopyModal = () => {
    dispatch(actions.setShowCopyModal(false));
    setSelectedVulnerability({});
  };

  return (
    <>
      <PolicyViolationDetailsDrawer />
      {!isNil(selectedVulnerability) && (
        <VexAnnotationDrawer
          isDrawerOpen={isVexAnnotationPopoverOpen}
          {...selectedVulnerability}
          onClose={closeVexAnnotationModal}
          componentPurl={componentDetails?.packageUrl}
          componentHash={componentDetails?.hash}
          internalAppId={internalAppId}
          sbomVersion={sbomVersion}
          responsesOptions={responsesOptions}
          analysisStatusesOptions={analysisStatusesOptions}
          justificationsOptions={justificationsOptions}
          loadVexReferenceData={loadVexReferenceData}
          openVulnerabilityDetailsModal={openVulnerabilityDetailsModal}
          preSaveMaskActions={preSaveMaskActions}
          postSaveMaskActions={postSaveMaskActions}
          onLearnMoreClick={onLearnMoreClick}
        />
      )}
      <NxPageMain id="sbom-manager-component-details" className={'sbom-component-details nx-viewport-sized'}>
        <MenuBarStatefulBreadcrumb />
        <div className="nx-viewport-sized__scrollable nx-scrollable sbom-component-details-page__content">
          <NxLoadWrapper
            retryHandler={load}
            loading={isProductFeaturesLoading || isLoading}
            error={errorLoadingProductFeatures || noSbomManagerEnabledError || loadError}
          >
            {componentDetails && (
              <div>
                <ComponentDetailsHeader>
                  <Title id="component-details-title">{componentDetails.displayName}</Title>
                  <ComponentDetailsSbomInfo {...componentDetails.metadata} />
                  <ComponentDetailsTags
                    dependencyType={componentDetails.dependencyType.toLowerCase()}
                    format={componentDetails.componentIdentifier?.format}
                    isInnerSource={componentDetails.isInnerSource}
                    labels={componentDetails.labels}
                    filename={isNilOrEmpty(componentDetails?.filenames) ? '' : componentDetails?.filenames[0]}
                    matchState={componentDetails?.matchState?.toLowerCase()}
                  />
                  {componentDetails.packageUrl && (
                    <NxTag className="nx-tag sbom-nx-tag" color="sky">
                      <span className="purl-container">{componentDetails.packageUrl}</span>
                      {'  '}
                      <NxTooltip title={!isPurlCopied ? 'Copy PackageURL to clipboard' : 'Copied'}>
                        <span
                          className="copy-icon-container"
                          data-testid="copyIconContainer"
                          onClick={() => copyToClipboard(componentDetails.packageUrl)}
                        >
                          <NxFontAwesomeIcon
                            className={'sbom-copy-icon'}
                            icon={faCopy}
                            onClick={() => copyToClipboard(componentDetails.packageUrl)}
                          />
                        </span>
                      </NxTooltip>
                    </NxTag>
                  )}
                </ComponentDetailsHeader>
                {componentDetails.vulnerabilitySummary && (
                  <ComponentSummary
                    vulnerabilitySummary={componentDetails.vulnerabilitySummary}
                    policyViolationSummary={componentDetails.policyViolationSummary}
                    isSbomPoliciesSupported={isSbomPoliciesSupported}
                  ></ComponentSummary>
                )}
                <NxTabs activeTab={activeTabIndex} onTabSelect={setActiveTabIndex}>
                  <NxTabList>
                    <NxTab>Vulnerability</NxTab>
                    {isSbomPoliciesSupported && <NxTab>Policy Violations</NxTab>}
                  </NxTabList>
                  <NxTabPanel>
                    <VulnerabilitiesTile
                      tableUniqueIdentifier={'disclosedVulnerabilities'}
                      vulnerabilities={componentDetails?.disclosedVulnerabilities}
                      openVulnerabilityDetailsModal={openVulnerabilityDetailsModal}
                      openVexAnnotationModal={openVexAnnotationModal}
                      analysisStatusesOptions={analysisStatusesOptions}
                      justificationsOptions={justificationsOptions}
                      responsesOptions={responsesOptions}
                      sortConfiguration={disclosedVulnerabilitiesSortConfiguration}
                      toggleSortDirection={cycleDisclosedVulnerabilitiesSortDirection}
                      onDeleteOptionClick={openDeleteModal}
                      onCopyOptionClick={openCopyModal}
                    ></VulnerabilitiesTile>
                    <VulnerabilitiesTile
                      tableUniqueIdentifier={'sonatypeIdentifiedVulnerabilities'}
                      vulnerabilities={componentDetails?.sonatypeIdentifiedVulnerabilities}
                      isDisclosedVulnerabilities={false}
                      openVulnerabilityDetailsModal={openVulnerabilityDetailsModal}
                      openVexAnnotationModal={openVexAnnotationModal}
                      analysisStatusesOptions={analysisStatusesOptions}
                      justificationsOptions={justificationsOptions}
                      responsesOptions={responsesOptions}
                      sortConfiguration={additionalVulnerabilitiesSortConfiguration}
                      toggleSortDirection={cycleAdditionalVulnerabilitiesSortDirection}
                      onDeleteOptionClick={openDeleteModal}
                      onCopyOptionClick={openCopyModal}
                    ></VulnerabilitiesTile>
                  </NxTabPanel>
                  {isSbomPoliciesSupported && (
                    <NxTabPanel>
                      <PolicyViolationsTile applicationPublicId={applicationPublicId} sbomVersion={sbomVersion} />
                    </NxTabPanel>
                  )}
                </NxTabs>
              </div>
            )}
          </NxLoadWrapper>
        </div>
        <SbomVulnerabilityDetailsPopover
          toggleVulnerabilityPopoverWithEffects={closeVulnerabilityDetailsModal}
          showVulnerabilityDetailPopover={isPopoverOpen}
          vulnerabilityRefId={selectedVulnerability.issue}
          reloadFunction={() => loadSbomComponentVulnerabilities(selectedVulnerability)}
          componentName={componentDetails?.packageUrl}
        ></SbomVulnerabilityDetailsPopover>
        {showDeleteModal && (
          <DeleteAnnotationModal
            vulnerability={selectedVulnerability}
            deleteAnnotationFromTable={deleteVexAnnotation}
            deleteError={deleteError}
            deleteMaskState={deleteMaskState}
            onCancel={cancelDeleteModal}
          ></DeleteAnnotationModal>
        )}
        {showCopyModal && (
          <CopyAnnotationModal
            vulnerability={selectedVulnerability}
            copyAnnotationFromTable={copyVexAnnotation}
            copyError={copyError}
            copyMaskState={copyMaskState}
            onCancel={cancelCopyModal}
          ></CopyAnnotationModal>
        )}
        {!isLoading && <ComponentDetailsFooter {...pagination} />}
      </NxPageMain>
    </>
  );
}
