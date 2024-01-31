/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Children, forwardRef, useRef } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { NxButton, NxFontAwesomeIcon, NxH1, NxH2, NxH3 } from '@sonatype/react-shared-components';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';
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
  size: PropTypes.oneOf(['small', 'medium', 'large', 'extra-large']),
};

export const IqPopoverHeaderTitleText = ({ headerSize = 'h2', headerTitle }) => {
  const commonClass = 'iq-popover-header__title-text';

  switch (headerSize) {
    case 'h1':
      return <NxH1 className={commonClass}>{headerTitle}</NxH1>;
    case 'h3':
      return <NxH3 className={commonClass}>{headerTitle}</NxH3>;
    case 'h2':
    default:
      return <NxH2 className={commonClass}>{headerTitle}</NxH2>;
  }
};

IqPopoverHeaderTitleText.propTypes = {
  headerSize: PropTypes.oneOf(['h1', 'h2', 'h3']),
  headerTitle: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
};

export const IqPopoverHeader = (props) => {
  const {
    children,
    className,
    onClose,
    buttonId,
    headerSize,
    headerTitle,
    buttonClassnames,
    closeTitle,
    ...otherProps
  } = props;

  const btnClasses = cx('iq-popover-header__close-btn', buttonClassnames);
  const btnTitle = closeTitle || 'Close';

  return (
    <header className={cx('iq-popover__header', className)} {...otherProps}>
      <div className="iq-popover-header__title">
        <IqPopoverHeaderTitleText headerSize={headerSize} headerTitle={headerTitle} />
        <NxButton className={btnClasses} onClick={onClose} variant="icon-only" title={btnTitle} id={buttonId}>
          <NxFontAwesomeIcon icon={faArrowToRight} />
        </NxButton>
      </div>
      {children}
      <hr className="iq-popover__divider" />
    </header>
  );
};

IqPopoverHeader.propTypes = {
  ...IqPopoverHeaderTitleText.propTypes,
  children: PropTypes.node,
  onClose: PropTypes.func.isRequired,
  buttonId: PropTypes.string,
  buttonClassnames: PropTypes.string,
  closeTitle: PropTypes.string,
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
