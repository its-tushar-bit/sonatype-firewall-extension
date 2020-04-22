/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { map } from 'ramda';
import { NxVulnerabilityDetails } from '@sonatype/react-shared-components';
import LoadWrapper from '../react/LoadWrapper';

export default function PolicyViolationInfoTile(props) {
  const { violationDetails, vulnerabilityDetails, vulnerabilityDetailsError, vulnerabilityDetailsLoading } = props,
      { constraintViolations } = violationDetails,
      { constraintName, reasons } = constraintViolations[0];

  return (
    <div id="policy-violation-info-tile" className="nx-tile">
      <div className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">
            Policy Constraint - {constraintName}
          </h2>
        </div>
      </div>
      <div className="nx-tile-content">
        <div className="nx-list nx-list--bulleted">
          <h4 className="nx-list__title nx-h4">is in violation for the following reason(s)</h4>
          <ul id="policy-violation-reasons">
            {map(({reason}) => <li className="nx-list__item" key={reason}>{reason}</li>, reasons)}
          </ul>
        </div>
        <LoadWrapper error={vulnerabilityDetailsError} loading={vulnerabilityDetailsLoading}>
          {vulnerabilityDetails && <NxVulnerabilityDetails vulnerabilityDetails={vulnerabilityDetails}/>}
        </LoadWrapper>
      </div>
    </div>
  );
}

const reasonPropType = PropTypes.shape({
  reason: PropTypes.string.isRequired,
  reference: PropTypes.shape({
    type: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired
  })
});

const constraintViolationPropType = PropTypes.shape({
  constraintName: PropTypes.string.isRequired,
  reasons: PropTypes.arrayOf(reasonPropType).isRequired
});

export const constraintViolationsPropType = PropTypes.arrayOf(constraintViolationPropType);

PolicyViolationInfoTile.propTypes = {
  violationDetails: PropTypes.shape({
    constraintViolations: constraintViolationsPropType.isRequired
  }),
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error,
  vulnerabilityDetails: PropTypes.oneOfType([
    NxVulnerabilityDetails.propTypes.vulnerabilityDetails, PropTypes.oneOf([null])
  ])
};
