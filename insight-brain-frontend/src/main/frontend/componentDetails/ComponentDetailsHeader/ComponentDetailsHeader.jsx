/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { NxOverflowTooltip } from '@sonatype/react-shared-components';

export const ComponentDetailsHeader = ({ children, ...props }) => {
  return (
    <header {...props} className={cx('component-details-header', props.className)}>
      {children}
    </header>
  );
};

ComponentDetailsHeader.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
};

export const Title = ({ children, ...props }) => (
  <h1 {...props} className={cx('component-details-header__heading', props.className)}>
    <NxOverflowTooltip>
      <span className="truncate-ellipsis">{children}</span>
    </NxOverflowTooltip>
  </h1>
);

Title.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
};

ComponentDetailsHeader.Title = Title;
export default ComponentDetailsHeader;
