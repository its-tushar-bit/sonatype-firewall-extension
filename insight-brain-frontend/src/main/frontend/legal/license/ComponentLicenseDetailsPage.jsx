/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { availableScopesPropType, componentPropType, licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { backToComponentOverviewUrl, createSubtitle } from '../legalUtility';
import LicenseList from './LicenseList';
import ComponentLicenseOverviewTile from './ComponentLicenseOverviewTile';
import LicenseFullDetailsTile from './LicenseFullDetailsTile';
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import LicensesModalContainer from './LicensesModalContainer';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';

export default function ComponentLicenseDetailsPage(props) {
  const {
    setShowLicensesModal,
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    scanId,
    componentIdentifier,
    stageTypeId,
    licenseIndex,
    component,
    componentLicenseDetails,
    licenseLegalMetadata,
    $state,
    loadComponentAndLicenseDetails,
  } = props;

  function load() {
    loadComponentAndLicenseDetails(ownerType, ownerId, hash, licenseIndex, componentIdentifier);
  }

  const showLicensesModal = component && component.licenseLegalData.showLicensesModal;

  useEffect(load, [ownerType, ownerId, hash, licenseIndex, componentIdentifier]);

  return (
    <main className="nx-page-main nx-viewport-sized">
      {showLicensesModal && <LicensesModalContainer />}
      <LoadWrapper loading={loading} error={error} retryHandler={load}>
        <MenuBarBackButton
          href={backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash, componentIdentifier, scanId)}
          text="Back to Component Obligations"
        />
        <div className="nx-page-title">
          <h1 className="nx-h1">Licenses</h1>
          <div className="nx-btn-bar">
            <NxButton id="edit-licenses" variant="tertiary" onClick={() => setShowLicensesModal(true)}>
              <NxFontAwesomeIcon icon={faPen} />
              <span>Edit Licenses</span>
            </NxButton>
          </div>
          {createSubtitle(availableScopes, component)}
        </div>
        <ComponentLicenseOverviewTile component={component} licenseLegalMetadata={licenseLegalMetadata} />
        <div id="component-license-details-content" className="nx-viewport-sized__container">
          <LicenseList
            ownerType={ownerType}
            ownerId={ownerId}
            hash={hash}
            stageTypeId={stageTypeId}
            licenseLegalMetadata={licenseLegalMetadata}
            componentLicenseDetails={componentLicenseDetails}
            $state={$state}
          />
          <LicenseFullDetailsTile
            componentLicenseDetails={componentLicenseDetails}
            licenseLegalMetadata={licenseLegalMetadata}
          />
        </div>
      </LoadWrapper>
    </main>
  );
}

ComponentLicenseDetailsPage.propTypes = {
  setShowLicensesModal: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  error: PropTypes.string,
  availableScopes: availableScopesPropType,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  scanId: PropTypes.string,
  componentIdentifier: PropTypes.string,
  stageTypeId: PropTypes.string,
  licenseIndex: PropTypes.string,
  component: componentPropType,
  componentLicenseDetails: PropTypes.object,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  $state: PropTypes.object.isRequired,
  loadComponentAndLicenseDetails: PropTypes.func.isRequired,
};
