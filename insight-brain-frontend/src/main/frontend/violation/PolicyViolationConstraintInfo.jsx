/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { map, head, isEmpty } from 'ramda';
import classnames from 'classnames';

export default function PolicyViolationConstraintInfo({
  constraintViolations,
  isFirewallContext,
  isFromPolicyViolations,
}) {
  if (isEmpty(constraintViolations)) {
    return null;
  }

  const { constraintName, reasons, conditions } = head(constraintViolations);

  return (
    <div id="policy-violation-constraint-info">
      <div className={classnames({ 'nx-tile-header': !isFromPolicyViolations })}>
        <div className="nx-tile-header__title">
          <h3 className="nx-h3">Policy Constraint</h3>
        </div>
      </div>
      <div className={classnames({ 'nx-tile-content': !isFromPolicyViolations })}>
        <h3 className="nx-h3">
          <span>{constraintName}</span> <span className="regular">is in violation for the following reason(s):</span>
        </h3>
        <div className="nx-list nx-list--bulleted nx-list--violation-reasons">
          <ul id="policy-violation-reasons">
            {isFirewallContext
              ? map(
                  ({ conditionReason }) => (
                    <li className="nx-list__item" key={conditionReason}>
                      {conditionReason}
                    </li>
                  ),
                  conditions
                )
              : map(
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

const conditionsPropType = PropTypes.shape({
  conditionReason: PropTypes.string.isRequired,
  conditionSummary: PropTypes.string.isRequired,
  conditionTriggerReference: PropTypes.shape({
    type: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
  conditionType: PropTypes.string.isRequired,
});

const constraintViolationPropType = PropTypes.shape({
  constraintName: PropTypes.string.isRequired,
  reasons: PropTypes.arrayOf(reasonPropType),
});

export const constraintViolationsPropType = PropTypes.arrayOf(constraintViolationPropType);

PolicyViolationConstraintInfo.propTypes = {
  constraintViolations: constraintViolationsPropType,
  isFirewallContext: PropTypes.bool,
  isFromPolicyViolations: PropTypes.bool,
  conditions: PropTypes.arrayOf(conditionsPropType),
};
