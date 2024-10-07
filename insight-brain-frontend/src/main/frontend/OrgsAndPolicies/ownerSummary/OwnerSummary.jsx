/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH1, NxPageTitle, NxLoadWrapper, NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faUser } from '@fortawesome/free-solid-svg-icons';
import { findIconDefinition } from '@fortawesome/fontawesome-svg-core';
import { getOwnerImageUrl } from 'MainRoot/utilAngular/CLMContextLocation';
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { selectLoadError as selectLoadSelectedOwnerError } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwner, selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRepositoryUrl, selectScmProviderIcon } from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import {
  selectNoSbomManagerEnabledError,
  selectIsSbomContinuousMonitoringUiEnabled,
  selectIsSbomPoliciesSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import OwnerSummaryPills from 'MainRoot/OrgsAndPolicies/OwnerSummaryPills/OwnerSummaryPills';
import ApplicationCategoriesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ApplicationCategoriesTile';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import LegacyViolationsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/LegacyViolationsTile';
import ContinuousMonitoringSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ContinuousMonitoringSummaryTile';
import ProprietaryComponentConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ProprietaryComponentConfigurationTile';
import LabelsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/labelsTile/LabelsTile';
import LicenseThreatGroupSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/LicenseThreatGroupSummaryTile';
import RetentionTile from 'MainRoot/OrgsAndPolicies/ownerSummary/retentionTile/RetentionTile';
import SourceControlTile from 'MainRoot/OrgsAndPolicies/ownerSummary/SourceControlTile';
import ArtifactoryRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ArtifactoryRepositoryTile';
import InnerSourceRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/InnerSourceRepositoryTile';
import SbomsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/SbomsTile';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import LegacyViolationModal from 'MainRoot/OrgsAndPolicies/legacyViolationModal/LegacyViolationModal';
import ChangeApplicationIdModal from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/ChangeApplicationIdModal';
import RevokeLegacyViolationModal from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/RevokeLegacyViolationModal';
import ImportPoliciesModal from 'MainRoot/OrgsAndPolicies/importPoliciesModal/ImportPoliciesModal';
import SelectContactModal from 'MainRoot/OrgsAndPolicies/selectContactModal/SelectContactModal';
import EvaluateApplicationModal from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/EvaluateApplicationModal';
import MoveOwnerModal from 'MainRoot/OrgsAndPolicies/moveOwner/MoveOwnerModal';
import { selectIsDisplayedOrganizationSynthetic } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import InsufficientPermissionOwnerHierarchyTree from 'MainRoot/OrgsAndPolicies/insufficientPermissionOwnerHierarchyTree/InsufficientPermissionOwnerHierarchyTree';
import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import WaiversConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/AutomatedWaiversTile';

function DefaultTiles() {
  return (
    <>
      <ApplicationCategoriesTile />
      <PoliciesTile />
      <LegacyViolationsTile />
      <ContinuousMonitoringSummaryTile />
      <ProprietaryComponentConfigurationTile />
      <LabelsTile />
      <LicenseThreatGroupSummaryTile />
      <RetentionTile />
      <SourceControlTile />
      <InnerSourceRepositoryTile />
      <WaiversConfigurationTile />
      <ArtifactoryRepositoryTile />
      <AccessTile />
    </>
  );
}

function SbomManagerTiles(isApp, isSbomContinuousMonitoringUiEnabled, isSbomPoliciesSupported) {
  return (
    <>
      {isApp && <SbomsTile />}
      {isSbomPoliciesSupported && <PoliciesTile />}
      {isSbomContinuousMonitoringUiEnabled && <ContinuousMonitoringSummaryTile />}
      <AccessTile />
    </>
  );
}

export default function OwnerSummary() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const owner = useSelector(selectSelectedOwner);
  const isApp = useSelector(selectIsApplication);
  const repositoryUrl = useSelector(selectRepositoryUrl);
  const scmProviderIcon = useSelector(selectScmProviderIcon);
  const entityId = useSelector(selectEntityId);
  const isSyntheticOrg = useSelector(selectIsDisplayedOrganizationSynthetic) && !isApp;
  const isSbomManager = useSelector(selectIsSbomManager);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const isSbomContinuousMonitoringUiEnabled = useSelector(selectIsSbomContinuousMonitoringUiEnabled);
  const isSbomPoliciesSupported = useSelector(selectIsSbomPoliciesSupported);
  const loadingOwnerSummaryAndEntityId = loading || !entityId || entityId !== owner[isApp ? 'publicId' : 'id'];
  const error = loadError || loadSelectedOwnerError || (isSbomManager && noSbomManagerEnabledError);

  const doLoad = () => dispatch(ownerSummaryActions.loadOwnerSummary());

  useEffect(() => {
    if (entityId) {
      doLoad();
    }
  }, [entityId]);

  return isSyntheticOrg ? (
    <InsufficientPermissionOwnerHierarchyTree />
  ) : (
    <NxLoadWrapper loading={loadingOwnerSummaryAndEntityId} error={error} retryHandler={doLoad}>
      <div id="owner-summary">
        <NxPageTitle className="iq-page-title">
          <NxH1>
            <span className="nx-icon">
              <img src={getOwnerImageUrl(owner)} />
            </span>
            <span>{owner.name}</span>
            {isApp && <span className="iq-owner-public-id"> ({owner.publicId})</span>}
          </NxH1>
          {owner.contact && (
            <NxPageTitle.Description>
              <NxFontAwesomeIcon icon={faUser} className="iq-owner-contact-icon" /> {owner.contact.displayName}
            </NxPageTitle.Description>
          )}
          <div className="nx-btn-bar" data-testid="owner-summary-action-dropdown-container">
            <ActionDropdown />
          </div>
        </NxPageTitle>
        {isApp && repositoryUrl && (
          <div className="page-repository-url nx-truncate-ellipsis">
            <NxFontAwesomeIcon icon={findIconDefinition({ prefix: 'fab', iconName: scmProviderIcon })} />
            <NxTextLink external href={repositoryUrl}>
              {repositoryUrl}
            </NxTextLink>
          </div>
        )}
        <OwnerSummaryPills />
      </div>
      <div
        className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
        id="owner-summary-sections"
      >
        {isSbomManager
          ? SbomManagerTiles(isApp, isSbomContinuousMonitoringUiEnabled, isSbomPoliciesSupported)
          : DefaultTiles()}
      </div>
      <DeleteOwnerModal />
      <LegacyViolationModal />
      <ChangeApplicationIdModal />
      <RevokeLegacyViolationModal />
      <ImportPoliciesModal />
      <ImportSbomModal />
      <MoveOwnerModal />
      <SelectContactModal />
      <EvaluateApplicationModal />
    </NxLoadWrapper>
  );
}
