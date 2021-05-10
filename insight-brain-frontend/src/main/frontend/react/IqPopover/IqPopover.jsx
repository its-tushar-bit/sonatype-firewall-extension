/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Children, forwardRef, useRef } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { groupBy } from 'ramda';
import useClickAway from '../useClickAway';
import useEscapeKeyStack from '../useEscapeKeyStack';

export const IqPopover = forwardRef(({ children, className, onClose = noop, size = 'small', ...props }, ref) => {
  const childNodes = groupChildrenByType(Children.toArray(children));
  const localRef = useRef(null);
  const clickAwayRef = ref || localRef;

  useClickAway(clickAwayRef, () => onClose());
  useEscapeKeyStack(true, () => onClose());

  return (
    <aside
      ref={clickAwayRef}
      className={cx('iq-popover', 'nx-viewport-sized', `iq-popover--${size}`, className)}
      {...props}
    >
      {childNodes.header}
      <div className="iq-popover__content nx-viewport-sized__scrollable">{childNodes.children}</div>
      {childNodes.footer}
    </aside>
  );
});

IqPopover.displayName = 'IqPopover';
IqPopover.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
  onClose: PropTypes.func,
  size: PropTypes.oneOf(['small', 'medium', 'large']),
};

export const IqPopoverHeader = ({ children, className, ...props }) => {
  return (
    <header className={cx('iq-popover__header', className)} {...props}>
      {children}
      <hr className="iq-popover__divider" />
    </header>
  );
};

IqPopoverHeader.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
};

export const IqPopoverFooter = ({ children, className, ...props }) => {
  return (
    <footer className={cx('iq-popover__footer', className)} {...props}>
      <hr className="iq-popover__divider" />
      {children}
    </footer>
  );
};

IqPopoverFooter.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
};

const groupChildrenByType = groupBy((child) =>
  child.type === IqPopoverHeader ? 'header' : child.type === IqPopoverFooter ? 'footer' : 'children'
);

const noop = () => {};

// default export
IqPopover.Header = IqPopoverHeader;
IqPopover.Footer = IqPopoverFooter;
export default IqPopover;
