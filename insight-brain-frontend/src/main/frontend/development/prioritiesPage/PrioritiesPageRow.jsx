/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTable, NxTag, NxThreatIndicator } from '@sonatype/react-shared-components';
import { DependencyIndicators } from 'MainRoot/applicationReport/DependencyIndicators';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function PrioritiesPageRow({ component, onClick, priority }) {
  const { policyThreatLevel, policyName, constraints, actions } = component;
  const policyAction = !isNilOrEmpty(actions) && actions[0].actionType; //to be changed later
  return (
    <NxTable.Row isClickable onClick={onClick}>
      <NxTable.Cell className="iq-priorities-page-priority">{priority + 1}</NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-components">
          <div className="iq-priorities-page-components__component">
            <DependencyIndicators component={component} />

            <ComponentDisplay component={component} />
          </div>
          <div className="iq-priorities-page-components__detail">
            <NxTag className="iq-priorities-page-components__detail-tag">Security-Reachable</NxTag>
          </div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-policy-details">
          <div className="iq-priorities-page-policy-details__desc">
            <NxThreatIndicator
              className="iq-priorities-page-policy-details__desc-threat-indicator"
              policyThreatLevel={policyThreatLevel}
            />
            <span className="iq-priorities-page-policy-details__desc-threat">{policyThreatLevel}</span>
            <span className={`iq-priorities-page-policy-details__desc-policy-action ${policyAction}`}>
              {policyAction}
            </span>
          </div>
          <div className="iq-priorities-page-policy-details__constraint">
            {constraints && constraints[0].constraintName}
          </div>
          <div className="iq-priorities-page-policy-details__policy">{policyName}</div>
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
