/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';

export const IqPageFooter = ({ children, ...props }) => {
  return (
    <footer {...props} className={cx('iq-page-footer', props.className)}>
      {children}
    </footer>
  );
};

IqPageFooter.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
};
