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

export const MenuButton = ({ iconLabel, icon, iconSize, children, onChange = noop, closeOnClick = true, ...props }) => {
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

  return (
    <div {...props} className={classnames('iq-menu-button', props.className)} ref={menuRef}>
      <NxButton className="iq-menu-button__button" variant="icon-only" aria-label={iconLabel} onClick={onToggle}>
        <NxFontAwesomeIcon icon={icon} size={iconSize} />
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
  iconSize: PropTypes.oneOf(['xs', 'sm', 'lg', '2x', '3x', '4x', '5x', '6x', '7x', '8x', '9x', '10x']),
  onChange: PropTypes.func,
  closeOnClick: PropTypes.bool,
  className: PropTypes.string,
  children: PropTypes.node,
};
