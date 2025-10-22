/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxCheckbox,
  NxOverflowTooltip,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { DependencyIndicators } from 'MainRoot/applicationReport/DependencyIndicators';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function BulkWaiveTableRow({
  component,
  condition,
  onClick,
  onCheckboxClick,
  isChecked,
  isCdpBulkWaive,
}) {
  return (
    <NxTableRow isClickable onClick={onClick}>
      <NxTableCell className="iq-bulk-waive__toggle-cell">
        <NxCheckbox isChecked={isChecked} onClick={onCheckboxClick} />
      </NxTableCell>
      <NxTableCell className="iq-bulk-waive__threat-cell">
        <NxThreatIndicator policyThreatLevel={component.policyThreatLevel} />
        <span className="nx-threat-number">{component.policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-bulk-waive__policy-name-cell">
        <NxOverflowTooltip>
          <div className="iq-bulk-waive__policy-name-text">{component.policyName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      {isCdpBulkWaive ? (
        <NxTableCell className="iq-bulk-waive__constraint-name-cell">
          {isNilOrEmpty(component.constraints) ? '' : component.constraints[0].constraintName}
        </NxTableCell>
      ) : (
        <NxTableCell className="iq-bulk-waive__component-name-cell">
          <div className="iq-bulk-waive__truncate-wrapper">
            <DependencyIndicators component={component} />
            <ComponentDisplay component={component} truncate />
          </div>
        </NxTableCell>
      )}
      <NxTableCell className="iq-bulk-waive__condition-name-cell">
        <p>{condition}</p>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

BulkWaiveTableRow.propTypes = {
  component: PropTypes.shape({
    derivedComponentName: PropTypes.string,
    policyName: PropTypes.string,
    hash: PropTypes.string,
    derivedDependencyType: PropTypes.string,
    waived: PropTypes.bool,
    legacyViolation: PropTypes.bool,
    policyThreatLevel: PropTypes.number,
    componentIdentifier: PropTypes.object,
    isOnlyInnerSourceTransitiveDependency: PropTypes.bool,
    innerSource: PropTypes.bool,
    waivedViolations: PropTypes.number,
    waivedWithAutoWaiver: PropTypes.bool,
    serializedComponentIdentifier: PropTypes.string,
    constraints: PropTypes.arrayOf(
      PropTypes.shape({
        constraintName: PropTypes.string,
      })
    ),
  }),
  condition: PropTypes.string,
  onClick: PropTypes.func.isRequired,
  onCheckboxClick: PropTypes.func.isRequired,
  isChecked: PropTypes.bool,
  isCdpBulkWaive: PropTypes.bool,
};
