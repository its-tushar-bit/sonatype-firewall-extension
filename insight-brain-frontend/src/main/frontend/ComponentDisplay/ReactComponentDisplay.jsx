/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxOverflowTooltip } from '@sonatype/react-shared-components';

import isFilenameOrUnknown from './isFilenameOrUnknown';
import { waiverMatcherStrategy } from '../util/waiverUtils';
import { getComponentName, getComponentNameWithoutVersion } from '../util/componentNameUtils';

/**
 * The React implementation of the component-display angular component
 */
export default function ComponentDisplay({
  component,
  truncate,
  matcherStrategy,
  displayTextIfUnknown,
  componentName: incomingComponentName,
  componentNameWithoutVersion: incomingComponentNameWithoutVersion,
}) {
  const textTag = isFilenameOrUnknown(component) ? 'em' : 'span',
    divClass = classnames('iq-component-display', {
      'truncate-ellipsis': truncate,
    });

  const componentName = getComponentNameToRender(
    component,
    matcherStrategy,
    incomingComponentNameWithoutVersion,
    incomingComponentName
  );

  return (
    <NxOverflowTooltip>
      <div className={divClass}>
        {React.createElement(
          textTag,
          { className: 'iq-component-display-text' },
          componentName === 'Unknown' && displayTextIfUnknown ? displayTextIfUnknown : componentName
        )}
      </div>
    </NxOverflowTooltip>
  );
}
// Because this component is being used by several tables across the project the component name can be passed in as a prop or calculated by other props
// This is why the data can be obtained from the component prop or from a direct props
const getComponentNameToRender = (component, matcherStrategy, componentNameWithoutVersion, componentName) => {
  const matcherStrategyWithFallback = component?.componentMatchStrategy ?? matcherStrategy;
  if (matcherStrategyWithFallback === waiverMatcherStrategy.ALL_VERSIONS) {
    return `${componentNameWithoutVersion || getComponentNameWithoutVersion(component)} (all versions)`;
  } else {
    return componentName || getComponentName(component);
  }
};

export const componentPropTypes = {
  filename: PropTypes.string,
  filenames: PropTypes.arrayOf(PropTypes.string),
  displayName: PropTypes.shape({
    parts: PropTypes.arrayOf(
      PropTypes.shape({
        value: PropTypes.string.isRequired,
        field: PropTypes.string,
      })
    ).isRequired,
  }),
};

ComponentDisplay.propTypes = {
  truncate: PropTypes.bool,
  component: PropTypes.shape(componentPropTypes),
  matcherStrategy: PropTypes.string,
  displayTextIfUnknown: PropTypes.string,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
};
