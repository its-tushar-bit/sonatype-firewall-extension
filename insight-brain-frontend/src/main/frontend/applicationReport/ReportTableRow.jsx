/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/pro-solid-svg-icons';

import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { selectIsAggregated, selectSelectedReport } from './applicationReportSelectors';
import { allPass, filter, includes, length, not, compose, pathOr, prop } from 'ramda';

import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';

const getInnerSourceParentsTooltipMessage = (component) => {
  const { innerSourceParentsDerivedComponentNames = [] } = component;
  const componentWord = innerSourceParentsDerivedComponentNames.length > 1 ? 'components' : 'component';
  return (
    <Fragment>
      This component was brought in by the following InnerSource {componentWord}:
      <ul>
        {innerSourceParentsDerivedComponentNames.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
    </Fragment>
  );
};

const DependencyIndicators = ({ component }) => {
  const { derivedDependencyType, isOnlyInnerSourceTransitiveDependency, innerSource } = component;

  if (derivedDependencyType === 'unknown') {
    return null;
  }

  const showInnerSourceIndicator = innerSource || isOnlyInnerSourceTransitiveDependency;
  const innerSourceDependencyIndicatorTooltipMessage =
    isOnlyInnerSourceTransitiveDependency && getInnerSourceParentsTooltipMessage(component);

  return (
    <Fragment>
      <DependencyIndicator type={derivedDependencyType} />
      {showInnerSourceIndicator && (
        <DependencyIndicator type="inner-source" tooltip={innerSourceDependencyIndicatorTooltipMessage} />
      )}
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
  const selectedReport = useSelector(selectSelectedReport);
  const isAggregated = useSelector(selectIsAggregated);

  const getTransitiveViolationsCount = () => {
    const transitiveComponentViolations = compose(
      length,
      filter(
        allPass([
          prop('policyThreatLevel'),
          compose(not, prop('waived')),
          compose(not, prop('grandfathered')),
          compose(includes(component.serializedComponentIdentifier), pathOr('', ['dependencyInfo', 'rootAncestors'])),
        ])
      )
    )(selectedReport.allEntries);
    return `${transitiveComponentViolations} transitive violation${transitiveComponentViolations === 1 ? '' : 's'}`;
  };
  return (
    <NxTableRow isClickable onClick={onClick}>
      <NxTableCell className="iq-app-report__threat-cell">
        <NxThreatIndicator policyThreatLevel={component.policyThreatLevel} />
        <span className="nx-threat-number">{component.policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-app-report__policy-name-cell">
        <NxOverflowTooltip>
          <div className="iq-app-report__policy-name-text">{component.policyName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-app-report__component-name-cell">
        {isAggregated && component.innerSource && (
          <span className="iq-transitive-violations-count iq-text-indicator iq-pull-right">
            {getTransitiveViolationsCount()}
          </span>
        )}
        {component.waived && (
          <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
            <span>Waived</span>
            <NxFontAwesomeIcon icon={faCheck} />
          </span>
        )}
        {component.grandfathered && (
          <span className="iq-text-indicator iq-text-indicator--legacy-violation iq-pull-right">
            <span>Legacy</span>
            <NxFontAwesomeIcon icon={faHistory} />
          </span>
        )}
        <div className="iq-app-report__truncate-wrapper">
          <DependencyIndicators component={component} />
          <ComponentDisplay component={component} truncate />
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
    componentIdentifier: PropTypes.object,
    isOnlyInnerSourceTransitiveDependency: PropTypes.bool,
    innerSource: PropTypes.bool,
  }),
  onClick: PropTypes.func.isRequired,
};
