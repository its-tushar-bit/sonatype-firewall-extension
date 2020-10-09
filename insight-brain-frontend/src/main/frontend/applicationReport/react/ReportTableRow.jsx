/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxFontAwesomeIcon, NxTableCell, NxTableRow, NxThreatBar, NxTooltip } from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/pro-solid-svg-icons';
import ComponentDisplay from '../../ComponentDisplay/ReactComponentDisplay';
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function ReportTableRow(props) {

  const {
    component
  } = props;

  const dependencyTooltipTitle = component.derivedDependencyType === 'direct' ?
        'Direct Dependency' : 'Transitive Dependency',
      dependencyIndicatorClasses = classnames('iq-dependency-indicator', {
        'direct': component.derivedDependencyType === 'direct',
        'transitive': component.derivedDependencyType === 'transitive'
      }),
      dependencyIndicator = component.derivedDependencyType === 'direct' ? 'D' : 'T';

  return (
    <NxTableRow key={ component.hash }>
      <NxTableCell className="nx-cell nx-cell--threat-bar">
        <NxThreatBar policyThreatLevel={component.policyThreatLevel} />
        <span className="nx-threat-number">{component.policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell >
        <span>{component.policyName}</span>
      </NxTableCell>
      <NxTableCell>
        {component.waived &&
          <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
            <span>Waived</span>
            <NxFontAwesomeIcon icon={faCheck}/>
          </span>
          }
        {component.grandfathered &&
          <span className="iq-text-indicator iq-text-indicator--grandfathered iq-pull-right">
            <span>Grandfathered</span>
            <NxFontAwesomeIcon icon={faHistory}/>
          </span>
          }
        {component.derivedDependencyType !== 'unknown' &&
          <NxTooltip
              title={dependencyTooltipTitle}
              placement="top">
            <div className={dependencyIndicatorClasses}>
              <span>{dependencyIndicator}</span>
            </div>
          </NxTooltip>
        }
        <div className="iq-report-cell-component">
          <ComponentDisplay component={component} truncate={true}/>
        </div>
      </NxTableCell>
    </NxTableRow>
  );
}

ReportTableRow.propTypes = {
  component: PropTypes.shape({
    derivedComponentName: PropTypes.string,
    policyName: PropTypes.string,
    hash: PropTypes.string,
    derivedDependencyType: PropTypes.string,
    waived: PropTypes.bool,
    grandfathered: PropTypes.bool,
    policyThreatLevel: PropTypes.number
  })
};
