/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { flatten } from 'ramda';

import ViolationExclamation from '../../react/ViolationExclamation';
import { NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';

const ACTION_ICON_CATEGORY = {
  fail: 'critical',
  warn: 'severe',
};

export default function PolicyViolationsTableRow({ violation }) {
  const { policyThreatLevel, policyName, constraints, actions } = violation;
  const [firstConstraint] = constraints;
  const reasons = flatten(
    constraints.map((constraint) => constraint.conditions.map((condition) => condition.conditionReason))
  );

  const renderActionsAsList = (actions = []) => {
    if (actions.length === 0) {
      return null;
    }

    return (
      <ul>
        {actions.map((action) => {
          return (
            <li key={action.actionType}>
              <ViolationExclamation threatLevelCategory={ACTION_ICON_CATEGORY[action.actionType]} />
              <span>{action.actionSummary}</span>
            </li>
          );
        })}
      </ul>
    );
  };

  return (
    <NxTableRow>
      <NxTableCell>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="policy-name-and-action">
        <span>{policyName}</span>
        {renderActionsAsList(actions)}
      </NxTableCell>
      <NxTableCell>{firstConstraint ? firstConstraint.constraintName : null}</NxTableCell>
      <NxTableCell>
        {reasons &&
          reasons.map((reason, index) => {
            return <p key={index}>{reason}</p>;
          })}
      </NxTableCell>
    </NxTableRow>
  );
}

const violationPropTypes = {
  policyViolationId: PropTypes.string.isRequired,
  policyThreatLevel: PropTypes.number.isRequired,
  policyName: PropTypes.string.isRequired,
  actions: PropTypes.arrayOf(
    PropTypes.shape({
      actionType: PropTypes.string.isRequired,
      actionSummary: PropTypes.string.isRequired,
    })
  ),
  constraints: PropTypes.arrayOf(
    PropTypes.shape({
      constraintName: PropTypes.string,
      conditions: PropTypes.arrayOf(
        PropTypes.shape({
          conditionReason: PropTypes.string,
        })
      ),
    })
  ),
};

PolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
};
