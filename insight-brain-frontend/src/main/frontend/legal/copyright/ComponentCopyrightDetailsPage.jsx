/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  availableScopesPropType,
  componentCopyrightDetailsPropType,
  componentPropType
} from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxBackButton } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import CopyrightList from './CopyrightList';
import CopyrightDetailsOverview from './CopyrightDetailsOverview';
import CopyrightFilesTile from './CopyrightFilesTile';
import { createSubtitle } from '../legalUtility';

export default function ComponentCopyrightDetailsPage(props) {
  const {
    loading,
    error,
    availableScopes,
    componentCopyrightDetails,
    component,
    ownerType,
    ownerId,
    hash,
    copyrightIndex,
    $state,

    loadComponentAndCopyrightDetails,
    loadFilePathsOnPageUpdate,
    loadCopyrightContexts,
    unloadCopyrightContexts
  } = props;

  function load() {
    loadComponentAndCopyrightDetails(ownerType, ownerId, hash, copyrightIndex);
  }

  useEffect(load, [ownerType, ownerId, hash]);

  const backUrl = () => {
    const state = ownerType === 'organization'
      ? 'organizationComponentLegalOverview'
      : 'applicationComponentLegalOverview';
    const params = {
      [ownerType === 'organization' ? 'organizationId' : 'applicationPublicId']: ownerId,
      'hash': hash
    };
    return $state.href($state.get(state), params);
  };

  return (
    <main className="nx-page-main nx-viewport-sized">
      <LoadWrapper loading={loading}
                   error={error}
                   retryHandler={load}>
        <NxBackButton href={backUrl()} targetPageTitle='Component Obligations'/>
        <div className="nx-page-title">
          <h1 className="nx-h1">
            Copyrights
          </h1>
          {createSubtitle(availableScopes)}
        </div>
        <div id="component-copyright-details-content" className="nx-viewport-sized__container">
          <CopyrightList
            component={component}
            copyrightIndex={copyrightIndex}
            ownerType={ownerType}
            ownerId={ownerId}
            hash={hash}
            $state={$state}
            componentCopyrightDetails={componentCopyrightDetails}/>
          <div id="component-copyright-details-right" className="nx-scrollable nx-viewport-sized__scrollable">
            <CopyrightDetailsOverview
              availableScopes={availableScopes}
              component={component}
              componentCopyrightDetails={componentCopyrightDetails}/>
            <CopyrightFilesTile
              selectedCopyright={componentCopyrightDetails.selectedCopyright}
              loadCopyrightContexts={loadCopyrightContexts}
              hideCopyrightContext={unloadCopyrightContexts}
              componentCopyrightDetails={componentCopyrightDetails}
              pageChange={loadFilePathsOnPageUpdate}/>
          </div>
        </div>
      </LoadWrapper>
    </main>
  );
}

ComponentCopyrightDetailsPage.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  copyrightIndex: PropTypes.string,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  availableScopes: availableScopesPropType,
  componentCopyrightDetails: componentCopyrightDetailsPropType,
  loadComponentAndCopyrightDetails: PropTypes.func.isRequired,
  $state: PropTypes.object.isRequired,
  loadFilePathsOnPageUpdate: PropTypes.func.isRequired,
  loadCopyrightContexts: PropTypes.func.isRequired,
  unloadCopyrightContexts: PropTypes.func.isRequired
};
