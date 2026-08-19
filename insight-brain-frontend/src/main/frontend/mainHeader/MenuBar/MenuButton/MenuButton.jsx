/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import useClickAway from '../../../react/useClickAway';
export * from './MenuTitle';
export * from './NavLink';

const noop = () => {};

export const MenuButton = ({
  iconLabel,
  icon,
  largeIcon,
  children,
  onChange = noop,
  closeOnClick = true,
  ...props
}) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const closeMenu = () => {
    setIsMenuOpen(false);
    onChange(false);
  };
  const onToggle = () => {
    setIsMenuOpen((isOpen) => !isOpen);
    onChange(!isMenuOpen);
  };
  const handleBubbledClick = () => {
    if (closeOnClick) {
      closeMenu();
    }
  };

  const menuRef = useRef();
  useClickAway(menuRef, closeMenu);

  const btnClasses = classnames('iq-menu-button__button', { 'iq-menu-button__button--large-icon': largeIcon });

  return (
    <div {...props} className={classnames('iq-menu-button', props.className)} ref={menuRef}>
      <NxButton className={btnClasses} variant="icon-only" title={iconLabel} onClick={onToggle}>
        <NxFontAwesomeIcon icon={icon} size={largeIcon ? '2x' : undefined} />
      </NxButton>
      {isMenuOpen && (
        <div className="iq-dropdown-menu" onClick={handleBubbledClick}>
          {children}
        </div>
      )}
    </div>
  );
};

MenuButton.propTypes = {
  iconLabel: PropTypes.string,
  icon: PropTypes.oneOfType([PropTypes.object, PropTypes.array, PropTypes.string]),
  largeIcon: PropTypes.bool,
  onChange: PropTypes.func,
  closeOnClick: PropTypes.bool,
  className: PropTypes.string,
  children: PropTypes.node,
};
