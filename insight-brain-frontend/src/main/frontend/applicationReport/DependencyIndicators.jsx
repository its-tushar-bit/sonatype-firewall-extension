/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
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

export const DependencyIndicators = ({ component }) => {
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
