/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { faFile, faNetworkWired, faTerminal } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCube } from '@fortawesome/pro-solid-svg-icons';
import { scopePropType } from './transitiveViolationsPropTypes';
import { capitalize } from '../util/jsUtil';

export const TransitiveViolationsPageSubtitle = ({ availableScopes, componentName, stageTypeId }) => {
  let applicationName = undefined;
  let organizationName = undefined;
  if (availableScopes && availableScopes.length > 0) {
    const lastScope = availableScopes[0];
    applicationName = lastScope.type === 'application' ? lastScope.name : null;
    organizationName = applicationName ? availableScopes[1].name : lastScope.name;
  }

  if (!applicationName && !organizationName && !componentName && !stageTypeId) {
    return null;
  }

  return (
    <div className="nx-page-title__description component-details-header__reportinfo">
      {!!organizationName && (
        <span className="component-details-header__reportinfo-item">
          <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faNetworkWired} />
          <span>{organizationName}</span>
        </span>
      )}
      {!!applicationName && (
        <span className="component-details-header__reportinfo-item">
          <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faTerminal} />
          <span>{applicationName}</span>
        </span>
      )}
      {!!componentName && (
        <span className="component-details-header__reportinfo-item">
          <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faCube} />
          <span>{componentName}</span>
        </span>
      )}
      {!!stageTypeId && (
        <span className="component-details-header__reportinfo-item">
          <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faFile} />
          <span>{`Latest ${capitalize(stageTypeId)} Report${applicationName ? '' : 's'}`}</span>
        </span>
      )}
    </div>
  );
};

TransitiveViolationsPageSubtitle.propTypes = {
  availableScopes: PropTypes.arrayOf(scopePropType.isRequired),
  componentName: PropTypes.string,
  stageTypeId: PropTypes.string,
};

export default TransitiveViolationsPageSubtitle;
