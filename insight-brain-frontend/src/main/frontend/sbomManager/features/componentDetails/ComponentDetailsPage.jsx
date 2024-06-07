/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useState } from 'react';
import { NxFontAwesomeIcon, NxLoadWrapper, NxPageMain, NxTag } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { ComponentDetailsHeader, ComponentDetailsTags, Title } from 'MainRoot/componentDetails/ComponentDetailsHeader';
import VulnerabilitiesTile from 'MainRoot/sbomManager/features/componentDetails/VulnerabilitiesTile';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import {
  selectComponentDetails,
  selectIsLoading,
  selectJustificationsReferenceData,
  selectLoadError,
  selectResponsesReferenceData,
  selectStatesReferenceData,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { actions as billOfMaterialsActions } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import ComponentDetailsDependencyTreeTile from 'MainRoot/sbomManager/features/componentDetails/dependecyTree/ComponentDetailsDependencyTreeTile';
import { selectInternalAppId } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import ComponentDetailsSbomInfo from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsSbomInfo';
import VulnerabilitiesSummary from 'MainRoot/sbomManager/features/componentDetails/VulnerabilitiesSummary';
import SbomVulnerabilityDetailsPopover from 'MainRoot/sbomManager/features/componentDetails/vulnerabilitiesDrawer/SbomVulnerabilityDetailsPopover';

import VexAnnotationDrawer from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';
import { isNil } from 'ramda';

export default function ComponentDetailsPage() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const isLoading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLoadError);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const componentDetails = useSelector(selectComponentDetails);
  const routerParams = useSelector(selectRouterCurrentParams);
  const justificationsOptions = useSelector(selectJustificationsReferenceData);
  const responsesOptions = useSelector(selectResponsesReferenceData);
  const analysisStatusesOptions = useSelector(selectStatesReferenceData);

  const uiRouterState = useRouterState();
  const { applicationPublicId, sbomVersion, componentHash } = routerParams;
  const billOfMaterialsState = 'sbomManager.management.view.bom';
  const billOfMaterialsHref = uiRouterState.href(billOfMaterialsState, {
    applicationPublicId,
    versionId: sbomVersion,
  });
  const internalAppId = useSelector(selectInternalAppId);
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const [isVexAnnotationPopoverOpen, setIsVexAnnotationPopoverOpen] = useState(false);

  const vulnerability = {};
  const [selectedVulnerability, setSelectedVulnerability] = useState(vulnerability);
  const load = () => dispatch(actions.loadComponentDetails({ internalAppId, sbomVersion, componentHash }));

  const loadSbomComponentVulnerabilities = (vulnerability) =>
    dispatch(
      actions.loadVulnerabilityDetails({
        vulnerability: { refId: vulnerability.issue },
        componentIdentifier: componentDetails?.componentIdentifier,
        extraParams: {
          ownerId: applicationPublicId,
          hash: componentDetails?.hash,
          isRepository: false,
        },
      })
    );

  const loadVexReferenceData = () => {
    dispatch(actions.getVulnerabilityAnalysisReferenceData());
  };

  const loadInternalAppId = () => dispatch(billOfMaterialsActions.loadInternalAppId(applicationPublicId));

  const initialize = () => {
    loadInternalAppId();
    loadVexReferenceData();
  };

  useEffect(() => {
    initialize();
  }, []);

  useEffect(() => {
    if (internalAppId) {
      load();
    }
  }, [internalAppId]);

  const copyToClipboard = async (value) => {
    try {
      await navigator.clipboard.writeText(value);
    } catch (err) {}
  };

  const closeVulnerabilityDetailsModal = () => setIsPopoverOpen(false);

  const openVulnerabilityDetailsModal = (vulnerability) => {
    setSelectedVulnerability(vulnerability);
    setIsPopoverOpen(true);
    setIsVexAnnotationPopoverOpen(false);
    loadSbomComponentVulnerabilities(vulnerability);
  };

  const closeVexAnnotationModal = () => setIsVexAnnotationPopoverOpen(false);

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

  return (
    <>
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
      <NxPageMain id="sbom-manager-component-details">
        <MenuBarBackButton
          text={`${applicationPublicId}:${sbomVersion}`}
          href={billOfMaterialsHref}
        ></MenuBarBackButton>
        <NxLoadWrapper
          retryHandler={load}
          loading={isProductFeaturesLoading || isLoading}
          error={errorLoadingProductFeatures || noSbomManagerEnabledError || loadError}
        >
          {componentDetails && (
            <div className="sbom-component-details">
              <ComponentDetailsHeader>
                <Title id="component-details-title">{componentDetails.displayName}</Title>
                <ComponentDetailsSbomInfo {...componentDetails.metadata} />
                <ComponentDetailsTags
                  dependencyType={componentDetails.dependencyType.toLowerCase()}
                  format={componentDetails.componentIdentifier?.format}
                  isInnerSource={componentDetails.isInnerSource}
                  labels={componentDetails.labels}
                />
                {componentDetails.packageUrl && (
                  <NxTag className="nx-tag sbom-nx-tag" color="sky">
                    {componentDetails.packageUrl}
                    {'  '}
                    <NxFontAwesomeIcon
                      className={'sbom-copy-icon'}
                      icon={faCopy}
                      onClick={() => copyToClipboard(componentDetails.packageUrl)}
                    />
                  </NxTag>
                )}
              </ComponentDetailsHeader>
              {componentDetails.vulnerabilitySummary && (
                <VulnerabilitiesSummary
                  vulnerabilitySummary={componentDetails.vulnerabilitySummary}
                ></VulnerabilitiesSummary>
              )}
              <VulnerabilitiesTile
                vulnerabilities={componentDetails?.disclosedVulnerabilities}
                openVulnerabilityDetailsModal={openVulnerabilityDetailsModal}
                openVexAnnotationModal={openVexAnnotationModal}
                analysisStatusesOptions={analysisStatusesOptions}
              ></VulnerabilitiesTile>
              <VulnerabilitiesTile
                vulnerabilities={componentDetails?.sonatypeIdentifiedVulnerabilities}
                isDisclosedVulnerabilities={false}
                openVulnerabilityDetailsModal={openVulnerabilityDetailsModal}
                openVexAnnotationModal={openVexAnnotationModal}
                analysisStatusesOptions={analysisStatusesOptions}
              ></VulnerabilitiesTile>
              <ComponentDetailsDependencyTreeTile
                componentDetails={componentDetails}
              ></ComponentDetailsDependencyTreeTile>
            </div>
          )}
        </NxLoadWrapper>
        <SbomVulnerabilityDetailsPopover
          toggleVulnerabilityPopoverWithEffects={closeVulnerabilityDetailsModal}
          showVulnerabilityDetailPopover={isPopoverOpen}
          vulnerabilityRefId={selectedVulnerability.issue}
          reloadFunction={() => loadSbomComponentVulnerabilities(selectedVulnerability)}
          componentName={componentDetails?.packageUrl}
        ></SbomVulnerabilityDetailsPopover>
      </NxPageMain>
    </>
  );
}
