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
export default function ComponentDisplay({ component, truncate, matcherStrategy }) {
  const textTag = isFilenameOrUnknown(component) ? 'em' : 'span',
    divClass = classnames('iq-component-display', {
      'truncate-ellipsis': truncate,
    });

  const componentName =
    matcherStrategy === waiverMatcherStrategy.ALL_VERSIONS
      ? `${getComponentNameWithoutVersion(component)} (all versions)`
      : getComponentName(component);

  return (
    <NxOverflowTooltip>
      <div className={divClass}>
        {React.createElement(textTag, { className: 'iq-component-display-text' }, componentName)}
      </div>
    </NxOverflowTooltip>
  );
}

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
};
