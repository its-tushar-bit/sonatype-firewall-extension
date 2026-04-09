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

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function BulkWaiveTableRow({
  component,
  condition,
  onClick,
  onCheckboxClick,
  isChecked,
  isCdpBulkWaive,
  checkboxId,
}) {
  return (
    <NxTableRow isClickable onClick={onClick}>
      <NxTableCell className="fw-bulk-waive__toggle-cell">
        <NxCheckbox checkboxId={checkboxId} isChecked={isChecked} onClick={onCheckboxClick} />
      </NxTableCell>
      <NxTableCell className="fw-bulk-waive__threat-cell">
        <NxThreatIndicator policyThreatLevel={component?.threatLevel ? component?.threatLevel : 0} />
        <span className="nx-threat-number">{component?.threatLevel ? component?.threatLevel : 0}</span>
      </NxTableCell>
      <NxTableCell className="fw-bulk-waive__policy-name-cell">
        <NxOverflowTooltip>
          <div className="fw-bulk-waive__policy-name-text">
            {component?.policyName ? component.policyName : 'No Violations'}
          </div>
        </NxOverflowTooltip>
      </NxTableCell>
      {isCdpBulkWaive ? (
        <NxTableCell className="fw-bulk-waive__constraint-name-cell">
          {isNilOrEmpty(component.constraints) ? '' : component.constraints[0].constraintName}
        </NxTableCell>
      ) : (
        <NxTableCell className="fw-bulk-waive__component-name-cell">
          <NxOverflowTooltip title={component.componentDisplayText || component.pathname}>
            <div className="nx-truncate-ellipsis">{component.componentDisplayText || component.pathname}</div>
          </NxOverflowTooltip>
        </NxTableCell>
      )}
      <NxTableCell className="fw-bulk-waive__condition-name-cell">
        <div className="fw-bulk-waive__condition-text">{condition}</div>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

BulkWaiveTableRow.propTypes = {
  component: PropTypes.shape({
    derivedComponentName: PropTypes.string,
    policyName: PropTypes.string,
    policyViolationId: PropTypes.string,
    derivedDependencyType: PropTypes.string,
    waived: PropTypes.bool,
    legacyViolation: PropTypes.bool,
    threatLevel: PropTypes.number,
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
  checkboxId: PropTypes.string,
};
