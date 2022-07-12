/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { flatten } from 'ramda';

import { NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';

export default function FirewallPolicyViolationsTableRow({ violation }) {
  const { threatLevel, policyName, componentFacts } = violation;
  const [constraintFacts] = componentFacts;
  const conditionFacts = [...constraintFacts.constraintFacts];
  const constraintNames = flatten(componentFacts.map((val) => val.constraintFacts.map((o) => o.constraintName)));
  const reasons = flatten(conditionFacts.map((val) => val.conditionFacts.map((text) => text.reason)));

  return (
    <NxTableRow className="iq-policy-violation-row">
      <NxTableCell>
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span className="nx-threat-number">{threatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__policy-name-and-action-cell">
        <span>{policyName}</span>
      </NxTableCell>
      <NxTableCell>
        {constraintNames?.map((constraint, index) => {
          return <p key={index}>{constraint}</p>;
        })}
      </NxTableCell>
      <NxTableCell>
        {reasons?.map((reason, index) => {
          return <p key={index}>{reason}</p>;
        })}
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell"></NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}
