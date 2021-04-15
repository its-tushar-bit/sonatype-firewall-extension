/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { map } from 'ramda';

export default function PolicyViolationConstraintInfoTile(props) {
  const { constraintViolations } = props,
    { constraintName, reasons } = constraintViolations[0];

  return (
    <div id="policy-violation-constraint-info-tile" className="nx-tile">
      <div className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Policy Constraint</h2>
        </div>
      </div>
      <div className="nx-tile-content">
        <h3 className="nx-h3">
          <strong>{constraintName}</strong>{' '}
          <span className="regular">is in violation for the following reason(s):</span>
        </h3>
        <div className="nx-list nx-list--bulleted nx-list--violation-reasons">
          <ul id="policy-violation-reasons">
            {map(
              ({ reason }) => (
                <li className="nx-list__item" key={reason}>
                  {reason}
                </li>
              ),
              reasons
            )}
          </ul>
        </div>
      </div>
    </div>
  );
}

const reasonPropType = PropTypes.shape({
  reason: PropTypes.string.isRequired,
  reference: PropTypes.shape({
    type: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
});

const constraintViolationPropType = PropTypes.shape({
  constraintName: PropTypes.string.isRequired,
  reasons: PropTypes.arrayOf(reasonPropType).isRequired,
});

export const constraintViolationsPropType = PropTypes.arrayOf(constraintViolationPropType);

PolicyViolationConstraintInfoTile.propTypes = {
  constraintViolations: constraintViolationsPropType.isRequired,
};
