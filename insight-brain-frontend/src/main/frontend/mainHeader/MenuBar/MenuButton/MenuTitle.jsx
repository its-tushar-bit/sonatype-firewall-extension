/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

export const MenuTitle = ({ children }) => <h4 className="iq-dropdown-menu__title">{children}</h4>;

MenuTitle.propTypes = {
  children: PropTypes.node,
};
