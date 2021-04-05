/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {findIndex, propEq} from 'ramda';
import {NxFontAwesomeIcon} from '@sonatype/react-shared-components';
import {faGlobe, faSitemap, faTerminal} from '@fortawesome/free-solid-svg-icons';
import React from 'react';

export function isScopeOverride(originalOwnerId, ownerId, availableScopeValues) {
  const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
  const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
  return originalOwnerLevel > newOwnerLevel;
}

export function createSubtitle(availableScopes) {
  let availableScopeValuesReversed = availableScopes && availableScopes.values && [...availableScopes.values] || [];
  availableScopeValuesReversed.reverse();
  return (
    <div className="nx-page-title__description">
      {availableScopeValuesReversed.map((availableScope, index) => {
        const scopeIcon = availableScope.id === 'ROOT_ORGANIZATION_ID'
          ? faGlobe
          : availableScope.type === 'organization'
            ? faSitemap
            : faTerminal;
        return <span key={index} className="iq-violation-details__subtitle-part">
          <NxFontAwesomeIcon icon={scopeIcon}/>
          <span>{availableScope.name}</span>
        </span>;
      })}
    </div>
  );
}
