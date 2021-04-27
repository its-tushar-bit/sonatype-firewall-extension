/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { useRouterState } from '../../../react/RouterStateContext';

export const NavLink = ({ stateName, children, href, showIf = true, openInNewTab, disabled, ...props }) => {
  const { href: hrefFromStateName, includes } = useRouterState();
  if (!showIf) {
    return null;
  }
  const classes = classnames('iq-menu-button__nav-link', props.className, {
    active: includes(stateName),
    disabled: disabled,
  });
  let linkHref = stateName ? hrefFromStateName(stateName) : href;
  if (disabled) {
    linkHref = undefined;
  }
  const openTabProps = openInNewTab
    ? {
        target: '_blank',
        rel: 'noreferrer',
      }
    : {};
  return (
    <a {...props} {...openTabProps} className={classes} href={linkHref}>
      {children}
    </a>
  );
};

NavLink.propTypes = {
  children: PropTypes.node,
  stateName: PropTypes.string,
  href: PropTypes.string,
  className: PropTypes.string,
  showIf: PropTypes.bool,
  openInNewTab: PropTypes.bool,
  disabled: PropTypes.bool,
};

export default NavLink;
