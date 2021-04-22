/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { availableScopesPropType, licenseLegalMetadataPropType, componentPropType } from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxBackButton } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { createSubtitle } from '../legalUtility';
import LicenseList from './LicenseList';
import ComponentLicenseOverviewTile from './ComponentLicenseOverviewTile';
import LicenseFullDetailsTile from './LicenseFullDetailsTile';

export default function ComponentLicenseDetailsPage(props) {
  const {
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    licenseIndex,
    component,
    componentLicenseDetails,
    licenseLegalMetadata,
    $state,
    loadComponentAndLicenseDetails,
  } = props;

  function load() {
    loadComponentAndLicenseDetails(ownerType, ownerId, hash, licenseIndex);
  }

  useEffect(load, [ownerType, ownerId, hash, licenseIndex]);

  const backUrl = () => {
    const state =
      ownerType === 'organization' ? 'organizationComponentLegalOverview' : 'applicationComponentLegalOverview';
    const params = {
      [ownerType === 'organization' ? 'organizationId' : 'applicationPublicId']: ownerId,
      hash,
    };
    return $state.href($state.get(state), params);
  };

  return (
    <main className="nx-page-main nx-viewport-sized">
      <LoadWrapper loading={loading} error={error} retryHandler={load}>
        <NxBackButton href={backUrl()} targetPageTitle="Component Obligations" />
        <div className="nx-page-title">
          <h1 className="nx-h1">Licenses</h1>
          {createSubtitle(availableScopes)}
        </div>
        <ComponentLicenseOverviewTile component={component} />
        <div id="component-license-details-content" className="nx-viewport-sized__container">
          <LicenseList
            ownerType={ownerType}
            ownerId={ownerId}
            hash={hash}
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
  loading: PropTypes.bool,
  error: PropTypes.string,
  availableScopes: availableScopesPropType,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  licenseIndex: PropTypes.string,
  component: componentPropType,
  componentLicenseDetails: PropTypes.object,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  $state: PropTypes.object.isRequired,
  loadComponentAndLicenseDetails: PropTypes.func.isRequired,
};
