/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxFontAwesomeIcon, NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/pro-solid-svg-icons';

import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';

const DependencyIndicators = ({ component }) => {
  const { derivedDependencyType, isOnlyInnerSourceTransitiveDependency, innerSource } = component;

  if (derivedDependencyType === 'unknown') {
    return null;
  }

  const showInnerSourceIndicator = innerSource || isOnlyInnerSourceTransitiveDependency;
  return (
    <Fragment>
      <DependencyIndicator type={derivedDependencyType} />
      {showInnerSourceIndicator && <DependencyIndicator type="inner-source" />}
    </Fragment>
  );
};

DependencyIndicators.propTypes = {
  component: PropTypes.shape({
    derivedDependencyType: PropTypes.string,
    isOnlyInnerSourceTransitiveDependency: PropTypes.bool,
    innerSource: PropTypes.bool,
  }),
};

export default function ReportTableRow({ onClick, component }) {
  return (
    <NxTableRow isClickable onClick={onClick}>
      <NxTableCell className="iq-app-report__threat-cell">
        <NxThreatIndicator policyThreatLevel={component.policyThreatLevel} />
        <span className="nx-threat-number">{component.policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-app-report__policy-name-cell">
        <span>{component.policyName}</span>
      </NxTableCell>
      <NxTableCell className="iq-app-report__component-name-cell">
        <div className="nx-truncate-ellipsis">
          {component.waived && (
            <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
              <span>Waived</span>
              <NxFontAwesomeIcon icon={faCheck} />
            </span>
          )}
          {component.grandfathered && (
            <span className="iq-text-indicator iq-text-indicator--grandfathered iq-pull-right">
              <span>Grandfathered</span>
              <NxFontAwesomeIcon icon={faHistory} />
            </span>
          )}
          <DependencyIndicators component={component} />
          <ComponentDisplay component={component} />
        </div>
      </NxTableCell>
      <NxTableCell chevron />
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
    policyThreatLevel: PropTypes.number,
    isOnlyInnerSourceTransitiveDependency: PropTypes.bool,
    innerSource: PropTypes.bool,
  }),
  onClick: PropTypes.func.isRequired,
};
