/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { findIndex, propEq } from 'ramda';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faGlobe, faSitemap, faTerminal, faCube, faCaretSquareRight } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { isNilOrEmpty } from '../util/jsUtil';
import { NO_LICENSE_THREAT_GROUP_ASSIGNED } from './advancedLegalConstants';

export function isScopeOverride(originalOwnerId, ownerId, availableScopeValues) {
  const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
  const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
  return originalOwnerLevel > newOwnerLevel;
}

export function createSubtitle(availableScopes, component) {
  let availableScopeValuesReversed = (availableScopes && availableScopes.values && [...availableScopes.values]) || [];
  availableScopeValuesReversed.reverse();
  if (availableScopeValuesReversed.length > 1) {
    availableScopeValuesReversed = availableScopeValuesReversed.filter((obj) => obj.id !== 'ROOT_ORGANIZATION_ID');
  }
  if (component) {
    availableScopeValuesReversed.push({
      type: 'component',
      id: 'component',
      name: component.displayName,
    });
  }
  return (
    <div className="nx-page-title__description">
      {availableScopeValuesReversed.map((availableScope, index) => {
        return (
          <span key={index} className="iq-violation-details__subtitle-part">
            <NxFontAwesomeIcon icon={setScopeIcon(availableScope)} />
            <span>{availableScope.name}</span>
          </span>
        );
      })}
    </div>
  );
}

export function setScopeIcon(availableScope) {
  if (availableScope.id === 'ROOT_ORGANIZATION_ID') {
    return faGlobe;
  }
  switch (availableScope.type) {
    case 'organization':
      return faSitemap;
    case 'application':
      return faTerminal;
    case 'component':
      return faCube;
  }
  return faCaretSquareRight;
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

export function scopeName(scopeOwnerId, availableScopes) {
  const availableScopeValues = (availableScopes && availableScopes.values && [...availableScopes.values]) || [];
  const scopeIndex = findIndex(propEq('id', scopeOwnerId), availableScopeValues);
  return scopeIndex < 0 ? 'Root Organization' : availableScopeValues[scopeIndex].name;
}

export function ifExistsElseEmpty(element, func) {
  return element ? func() : '';
}

export function attributionStatus(item) {
  return ifExistsElseEmpty(item, () => (item.status === 'enabled' ? 'Included' : 'Excluded'));
}
export function legalSource(item) {
  return ifExistsElseEmpty(item, () => (item.originalContentHash ? 'Sonatype Scan' : 'Manually added'));
}

/**
 * Find the index of the single license in licenseMetadata.
 * If user clicked on a multi-license in the list we should select the first license in the multi.
 */
export function findSingleLicenseIndex(licenseId, licenseLegalMetadata) {
  const corrected = licenseLegalMetadata.findIndex((license) => !license.isMulti && license.licenseId === licenseId);
  if (corrected !== -1) {
    return corrected;
  }

  // Must be a multilicense
  return licenseLegalMetadata.findIndex((license) => !license.isMulti && licenseId.startsWith(license.licenseId));
}

/**
 * Given a component and the licenseLegalMetadata, returns an array of the license name and id from the component's
 * effective license IDs.
 *  example:
 * [{
 *    licenseId: id,
 *    licenseName: name
 *  }]
 */
export function getComponentEffectiveLicenseNamesAndIds(component, licenseLegalMetadata) {
  return component
    ? licenseLegalMetadata
        .filter((l) => component.licenseLegalData.effectiveLicenses.includes(l.licenseId))
        .map((l) => ({
          licenseId: l.licenseId,
          licenseName: l.licenseName,
        }))
    : [];
}
