/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTable, NxTag, NxThreatIndicator } from '@sonatype/react-shared-components';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import PropTypes from 'prop-types';

const dependencyTypeMap = {
  Direct: 'direct',
  Transitive: 'transitive',
  'Inner Source': 'inner-source',
  Unknown: 'unknown',
};

export default function PrioritiesPageRow({ component, onClick }) {
  const {
    displayName,
    dependencyType,
    action,
    highestThreat,
    priority,
    highestThreatPolicyName,
    highestThreatPolicyConstraintName,
    securityReachable,
  } = component;

  const policyAction = action === 'none' ? null : action;

  return (
    <NxTable.Row isClickable onClick={onClick}>
      <NxTable.Cell className="iq-priorities-page-priority">{priority}</NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-components">
          <div className="iq-priorities-page-components__component">
            <span data-testid="dependency-type">
              <DependencyIndicator type={dependencyTypeMap[dependencyType]} />
            </span>
            {displayName}
          </div>
          <div className="iq-priorities-page-components__detail">
            {securityReachable ? (
              <NxTag className="iq-priorities-page-components__detail-tag">Security-Reachable</NxTag>
            ) : null}
          </div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-policy-details">
          <div className="iq-priorities-page-policy-details__desc">
            <NxThreatIndicator
              className="iq-priorities-page-policy-details__desc-threat-indicator"
              policyThreatLevel={highestThreat}
            />
            <span className="iq-priorities-page-policy-details__desc-threat">{highestThreat}</span>

            {policyAction && (
              <span className={`iq-priorities-page-policy-details__desc-policy-action ${policyAction}`}>
                {policyAction}
              </span>
            )}
          </div>
          <div className="iq-priorities-page-policy-details__constraint">{highestThreatPolicyConstraintName}</div>
          <div className="iq-priorities-page-policy-details__policy">{highestThreatPolicyName}</div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-remediation">
          <div className="iq-priorities-page-remediation__upgrade">Upgrade to 1.11.0</div>
          <div className="iq-priorities-page-remediation__upgrade-desc">
            Next version with no policy violations for this component and its dependencies
          </div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
}

PrioritiesPageRow.propTypes = {
  component: PropTypes.shape({
    displayName: PropTypes.string.isRequired,
    dependencyType: PropTypes.string.isRequired,
    action: PropTypes.string.isRequired,
    highestThreat: PropTypes.number.isRequired,
    priority: PropTypes.number.isRequired,
    highestThreatPolicyName: PropTypes.string,
    highestThreatPolicyConstraintName: PropTypes.string,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
};
