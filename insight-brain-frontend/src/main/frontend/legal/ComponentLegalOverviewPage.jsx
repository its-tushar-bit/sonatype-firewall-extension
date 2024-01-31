/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxWarningAlert, useToggle } from '@sonatype/react-shared-components';
import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
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
import { TEXT_BASED_OBLIGATIONS, SUPPORTED_COMPONENTS_ECOSYSTEM } from './advancedLegalConstants';
import LicenseObligationsTileContainer from './obligation/LicenseObligationsTileContainer';
import NoticeTextsTile from './files/notices/NoticeTextsTile';
import { createSubtitle, formatLicenseMeta } from './legalUtility';
import LicenseFilesTile from './files/licenses/LicenseFilesTile';
import OriginalSourcesTile from 'MainRoot/legal/originalSources/OriginalSourcesTile';

export default function ComponentLegalOverviewPage(props) {
  const {
    component,
    licenseLegalMetadata,
    obligations,
    loading,
    licenseFiles,
    noticeFiles,
    sourceLinks,
    error,
    organizationId,
    applicationPublicId,
    stageTypeId,
    hash,
    componentIdentifier,
    repositoryId,
    availableScopes,
    showEditCopyrightOverrideModal,
    showNoticesModal,
    showLicenseFilesModal,
    showLicensesModal,
    showOriginalSourcesModal,
    ecosystem,
    $state,
    prevState,
    prevParams,
    tabId,
    scanId,

    //actions
    setDisplayCopyrightOverrideModal,
    loadAvailableScopes,
    loadComponent,
    loadComponentByComponentIdentifier,
    setShowNoticesModal,
    setShowLicenseFilesModal,
    setShowLicensesModal,
    setDisplayOriginalSourcesOverrideModal,
  } = props;
  const effectiveLicenses = formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata);

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
    } else if (componentIdentifier) {
      if (repositoryId) {
        loadComponentByComponentIdentifier(componentIdentifier, {
          repositoryId,
        });
        loadAvailableScopes('organization', 'ROOT_ORGANIZATION_ID');
      } else if (applicationPublicId) {
        loadComponentByComponentIdentifier(componentIdentifier, {
          orgOrApp: 'application',
          ownerId: applicationPublicId,
        });
        loadAvailableScopes('application', applicationPublicId);
      } else {
        loadComponentByComponentIdentifier(componentIdentifier, {
          orgOrApp: 'organization',
          ownerId: organizationId || 'ROOT_ORGANIZATION_ID',
        });
        loadAvailableScopes('organization', organizationId || 'ROOT_ORGANIZATION_ID');
      }
    }
  }

  useEffect(load, [hash]);

  const ownerType = applicationPublicId ? 'application' : 'organization';
  const ownerId = applicationPublicId || organizationId || 'ROOT_ORGANIZATION_ID';

  const isTextBasedObligation = (licenseObligation) => {
    return TEXT_BASED_OBLIGATIONS.indexOf(licenseObligation.name) >= 0;
  };

  const createLicenseObligationAttributionTileContainer = (licenseObligation, index) => (
    <LicenseObligationAttributionTileContainer key={index} name={licenseObligation.name} />
  );

  const [isEcosystemSupportWarningOpen, dismissEcosystemSupportWarning] = useToggle(true);

  const isComponentEcosystemSupported = () =>
    SUPPORTED_COMPONENTS_ECOSYSTEM.find((supportedEcosystem) => supportedEcosystem === ecosystem);

  const getDefaultBackButtonUrl = () =>
    applicationPublicId && stageTypeId
      ? $state.href($state.get('legal.applicationDetails'), {
          applicationPublicId: applicationPublicId,
          stageTypeId: stageTypeId,
        })
      : $state.href($state.get(prevState.name), { ...prevParams });

  const backHref =
    tabId === 'legal' && scanId
      ? $state.href('applicationReport.componentDetails.legal', {
          publicId: applicationPublicId,
          scanId,
          hash,
        })
      : getDefaultBackButtonUrl();

  return (
    <main className="nx-page-main nx-viewport-sized iq-component-legal-overview-page">
      <div className="nx-viewport-sized__scrollable nx-scrollable iq-component-legal-overview-page__content">
        <LoadWrapper loading={loading} error={error} retryHandler={load}>
          <MenuBarBackButton href={backHref} text="Back" />
          {component && (
            <React.Fragment>
              <div className="nx-page-title">
                <h1 className="nx-h1">{component.displayName}</h1>
                {createSubtitle(availableScopes)}
              </div>
              {!isComponentEcosystemSupported() && isEcosystemSupportWarningOpen && (
                <NxWarningAlert onClose={dismissEcosystemSupportWarning}>
                  {"This component's ecosystem is not currently supported by the Advanced Legal Pack."}
                </NxWarningAlert>
              )}
            </React.Fragment>
          )}
          {component && (
            <div id="component-legal-overview-details">
              <ComponentOverviewTile applicationPublicId={applicationPublicId} component={component} $state={$state} />
              <LicenseObligationsTileContainer
                ownerType={ownerType}
                ownerId={ownerId}
                hash={hash}
                stageTypeId={stageTypeId}
                componentIdentifier={componentIdentifier}
                $state={$state}
                effectiveLicenses={effectiveLicenses}
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
                    componentIdentifier={componentIdentifier}
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
                    componentIdentifier={componentIdentifier}
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
                      componentIdentifier,
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
                      componentIdentifier,
                    }}
                  />
                  <OriginalSourcesTile
                    sourceLinks={sourceLinks}
                    showOriginalSourcesModal={showOriginalSourcesModal}
                    setDisplayOriginalSourcesOverrideModal={setDisplayOriginalSourcesOverrideModal}
                  />
                  {obligations.filter(isTextBasedObligation).map(createLicenseObligationAttributionTileContainer)}
                  <LicenseObligationAttributionTileContainer name={null} />
                </div>
              </section>
            </div>
          )}
        </LoadWrapper>
      </div>
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
  componentIdentifier: PropTypes.string,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  obligations: licenseObligationsPropType,
  noticeFiles: legalFilesPropType,
  sourceLinks: PropTypes.any,
  licenseFiles: legalFilesPropType,
  loadComponent: PropTypes.func,
  loadComponentByComponentIdentifier: PropTypes.func,
  loadAvailableScopes: PropTypes.func,
  repositoryId: PropTypes.string,
  availableScopes: availableScopesPropType,
  showEditCopyrightOverrideModal: PropTypes.bool.isRequired,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
  setShowNoticesModal: PropTypes.func.isRequired,
  showNoticesModal: PropTypes.bool.isRequired,
  setShowLicenseFilesModal: PropTypes.func.isRequired,
  showLicenseFilesModal: PropTypes.bool.isRequired,
  setShowLicensesModal: PropTypes.func.isRequired,
  showLicensesModal: PropTypes.bool.isRequired,
  setDisplayOriginalSourcesOverrideModal: PropTypes.func.isRequired,
  showOriginalSourcesModal: PropTypes.bool.isRequired,
  ecosystem: PropTypes.string,
  $state: PropTypes.shape({
    href: PropTypes.func,
    get: PropTypes.func,
  }),
  prevState: PropTypes.shape({
    name: PropTypes.string,
  }),
  prevParams: PropTypes.object,
  tabId: PropTypes.string,
  scanId: PropTypes.string,
};
