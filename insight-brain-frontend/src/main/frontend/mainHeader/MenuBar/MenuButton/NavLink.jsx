/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { useRouterState } from '../../../react/RouterStateContext';
import { NxOverflowTooltip } from '@sonatype/react-shared-components';

export const NavLink = ({
  stateName,
  children,
  href,
  showIf = true,
  prefix = '',
  openInNewTab,
  disabled,
  ...props
}) => {
  const { href: hrefFromStateName, includes } = useRouterState();
  if (!showIf) {
    return null;
  }
  const resolvedPrefix = prefix && !prefix.endsWith('.') ? `${prefix}.` : prefix;
  const fullStateName = resolvedPrefix + stateName;
  const classes = classnames('nx-dropdown-link', props.className, {
    active: includes(fullStateName),
    disabled: disabled,
  });

  let linkHref = stateName ? hrefFromStateName(fullStateName) : href;
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
    <NxOverflowTooltip>
      <a {...props} {...openTabProps} className={classes} href={linkHref}>
        {children}
      </a>
    </NxOverflowTooltip>
  );
};

NavLink.propTypes = {
  children: PropTypes.node,
  stateName: PropTypes.string,
  href: PropTypes.string,
  className: PropTypes.string,
  showIf: PropTypes.bool,
  prefix: PropTypes.string,
  openInNewTab: PropTypes.bool,
  disabled: PropTypes.bool,
};

export default NavLink;
