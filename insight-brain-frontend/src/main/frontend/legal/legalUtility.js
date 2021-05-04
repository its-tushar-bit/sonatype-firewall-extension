/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { findIndex, propEq } from 'ramda';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faGlobe, faSitemap, faTerminal } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { isNilOrEmpty } from '../util/jsUtil';
import { NO_LICENSE_THREAT_GROUP_ASSIGNED } from './advancedLegalConstants';

export function isScopeOverride(originalOwnerId, ownerId, availableScopeValues) {
  const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
  const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
  return originalOwnerLevel > newOwnerLevel;
}

export function createSubtitle(availableScopes) {
  let availableScopeValuesReversed = (availableScopes && availableScopes.values && [...availableScopes.values]) || [];
  availableScopeValuesReversed.reverse();
  return (
    <div className="nx-page-title__description">
      {availableScopeValuesReversed.map((availableScope, index) => {
        const scopeIcon =
          availableScope.id === 'ROOT_ORGANIZATION_ID'
            ? faGlobe
            : availableScope.type === 'organization'
            ? faSitemap
            : faTerminal;
        return (
          <span key={index} className="iq-violation-details__subtitle-part">
            <NxFontAwesomeIcon icon={scopeIcon} />
            <span>{availableScope.name}</span>
          </span>
        );
      })}
    </div>
  );
}

export function backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash) {
  let state =
    ownerType === 'organization' ? 'legal.organizationComponentOverview' : 'legal.applicationComponentOverview';
  const params = {
    [ownerType === 'organization' ? 'organizationId' : 'applicationPublicId']: ownerId,
    hash: hash,
  };
  if (stageTypeId && ownerType === 'application') {
    state = 'legal.applicationStageTypeComponentOverview';
    params.stageTypeId = stageTypeId;
  }
  return $state.href($state.get(state), params);
}

export function getLicenseThreatGroupsFromLicense(license) {
  return isNilOrEmpty(license.licenseThreatGroups)
    ? [{ licenseThreatGroupName: NO_LICENSE_THREAT_GROUP_ASSIGNED }]
    : license.licenseThreatGroups;
}
