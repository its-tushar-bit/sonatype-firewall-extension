/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton } from '@sonatype/react-shared-components';
import ComponentOverviewTile from './ComponentOverviewTile';
import LicenseDetailsTile from './LicenseDetailsTile';
import CopyrightStatementsTile from './copyright/CopyrightStatementsTile';
import LicenseObligationAttributionTileContainer from './LicenseObligationAttributionTileContainer';
import LoadWrapper from '../react/LoadWrapper';
import {
  availableScopesPropType,
  componentPropType,
  legalFilesPropType,
  licenseLegalMetadataPropType,
  licenseObligationsPropType,
} from './advancedLegalPropTypes';
import { TEXT_BASED_OBLIGATIONS } from './advancedLegalConstants';
import LicenseObligationsTileContainer from './obligation/LicenseObligationsTileContainer';
import NoticeTextsTile from './files/notices/NoticeTextsTile';
import { createSubtitle, formatLicenseMeta } from './legalUtility';
import LicenseFilesTile from './files/licenses/LicenseFilesTile';

export default function ComponentLegalOverviewPage(props) {
  const {
    component,
    licenseLegalMetadata,
    obligations,
    loading,
    licenseFiles,
    noticeFiles,
    error,
    organizationId,
    applicationPublicId,
    stageTypeId,
    hash,
    availableScopes,
    showEditCopyrightOverrideModal,
    showNoticesModal,
    showLicenseFilesModal,
    showLicensesModal,
    $state,

    //actions
    setDisplayCopyrightOverrideModal,
    loadAvailableScopes,
    loadComponent,
    setShowNoticesModal,
    setShowLicenseFilesModal,
    setShowLicensesModal,
  } = props;

  function load() {
    if (hash) {
      if (organizationId) {
        loadComponent('organization', organizationId, hash);
        loadAvailableScopes('organization', organizationId);
      } else if (applicationPublicId) {
        loadComponent('application', applicationPublicId, hash);
        loadAvailableScopes('application', applicationPublicId);
      } else {
        loadComponent('organization', 'ROOT_ORGANIZATION_ID', hash);
        loadAvailableScopes('organization', 'ROOT_ORGANIZATION_ID');
      }
    }
  }

  useEffect(load, [hash]);

  const ownerType = applicationPublicId ? 'application' : 'organization';
  const ownerId = applicationPublicId || organizationId || 'ROOT_ORGANIZATION_ID';

  const licenses = formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata);

  const isTextBasedObligation = (licenseObligation) => {
    return TEXT_BASED_OBLIGATIONS.indexOf(licenseObligation.name) >= 0;
  };

  const createLicenseObligationAttributionTileContainer = (licenseObligation, index) => (
    <LicenseObligationAttributionTileContainer key={index} name={licenseObligation.name} />
  );

  const backHref =
    applicationPublicId && stageTypeId
      ? $state.href($state.get('legal.applicationDetails'), {
          applicationPublicId: applicationPublicId,
          stageTypeId: stageTypeId,
        })
      : $state.href($state.get('legal.dashboard'));

  return (
    <main className="nx-page-main">
      <LoadWrapper loading={loading} error={error} retryHandler={load}>
        <NxBackButton href={backHref} text="Back" />
        {component && (
          <div className="nx-page-title">
            <h1 className="nx-h1">{component.displayName}</h1>
            {createSubtitle(availableScopes)}
          </div>
        )}
        {component && (
          <div id="component-legal-overview-details">
            <ComponentOverviewTile
              applicationPublicId={applicationPublicId}
              component={component}
              licenses={licenses}
              $state={$state}
            />
            <LicenseObligationsTileContainer
              ownerType={ownerType}
              ownerId={ownerId}
              hash={hash}
              stageTypeId={stageTypeId}
              $state={$state}
            />
            <section id="attribution-summary-tile" className="nx-tile">
              <header className="nx-tile-header">
                <div className="nx-tile-header__title">
                  <h2 className="nx-h2">Attribution Summary</h2>
                </div>
              </header>
              <div className="nx-tile-content nx-tile-content--accordion-container">
                <LicenseDetailsTile
                  component={component}
                  licenseLegalMetadata={licenseLegalMetadata}
                  ownerType={ownerType}
                  ownerId={ownerId}
                  hash={hash}
                  stageTypeId={stageTypeId}
                  $state={$state}
                  showLicensesModal={showLicensesModal}
                  setShowLicensesModal={setShowLicensesModal}
                />
                <CopyrightStatementsTile
                  component={component}
                  availableScopes={availableScopes}
                  ownerType={ownerType}
                  ownerId={ownerId}
                  hash={hash}
                  stageTypeId={stageTypeId}
                  $state={$state}
                  showEditCopyrightOverrideModal={showEditCopyrightOverrideModal}
                  setDisplayCopyrightOverrideModal={setDisplayCopyrightOverrideModal}
                />
                <NoticeTextsTile
                  {...{
                    noticeFiles,
                    setShowNoticesModal,
                    showNoticesModal,
                    stageTypeId,
                    $state,
                    component,
                    availableScopes,
                    ownerType,
                    ownerId,
                    hash,
                  }}
                />
                <LicenseFilesTile
                  {...{
                    licenseFiles,
                    setShowLicenseFilesModal,
                    showLicenseFilesModal,
                    stageTypeId,
                    $state,
                    component,
                    availableScopes,
                    ownerType,
                    ownerId,
                    hash,
                  }}
                />
                {obligations.filter(isTextBasedObligation).map(createLicenseObligationAttributionTileContainer)}
                <LicenseObligationAttributionTileContainer name={null} />
              </div>
            </section>
          </div>
        )}
      </LoadWrapper>
    </main>
  );
}

ComponentLegalOverviewPage.propTypes = {
  component: componentPropType,
  loading: PropTypes.bool,
  error: PropTypes.string,
  organizationId: PropTypes.string,
  applicationPublicId: PropTypes.string,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  obligations: licenseObligationsPropType,
  noticeFiles: legalFilesPropType,
  licenseFiles: legalFilesPropType,
  loadComponent: PropTypes.func,
  loadAvailableScopes: PropTypes.func,
  availableScopes: availableScopesPropType,
  showEditCopyrightOverrideModal: PropTypes.bool.isRequired,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
  setShowNoticesModal: PropTypes.func.isRequired,
  showNoticesModal: PropTypes.bool.isRequired,
  setShowLicenseFilesModal: PropTypes.func.isRequired,
  showLicenseFilesModal: PropTypes.bool.isRequired,
  setShowLicensesModal: PropTypes.func.isRequired,
  showLicensesModal: PropTypes.bool.isRequired,
  $state: PropTypes.object.isRequired,
};
