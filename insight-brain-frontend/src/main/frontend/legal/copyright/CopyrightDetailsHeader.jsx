/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { availableScopesPropType } from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxBackButton } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { createSubtitle } from '../legalUtility';

export default function CopyrightDetailsHeader(props) {
  const {
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    copyrightIndex,
    $state,

    loadComponentAndCopyrightDetails,
  } = props;

  const backUrl = () => {
    const state =
      ownerType === 'organization'
        ? 'organizationComponentLegalOverview'
        : 'applicationComponentLegalOverview';
    const params = {
      [ownerType === 'organization'
        ? 'organizationId'
        : 'applicationPublicId']: ownerId,
      hash: hash,
    };
    return $state.href($state.get(state), params);
  };

  function load() {
    loadComponentAndCopyrightDetails(ownerType, ownerId, hash, copyrightIndex);
  }

  useEffect(load, [ownerType, ownerId, hash, copyrightIndex]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <NxBackButton href={backUrl()} targetPageTitle="Component Obligations" />
      <div className="nx-page-title">
        <h1 className="nx-h1">Copyrights</h1>
        {createSubtitle(availableScopes)}
      </div>
    </LoadWrapper>
  );
}

CopyrightDetailsHeader.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  copyrightIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,

  loadComponentAndCopyrightDetails: PropTypes.func.isRequired,
};
