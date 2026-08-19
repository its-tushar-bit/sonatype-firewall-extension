/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef, useState, useEffect, useCallback } from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faFilter, faCaretDown, faCaretUp } from '@fortawesome/pro-solid-svg-icons';
import { firewallExpirationDates } from 'MainRoot/firewall/waivers/firewallWaiverExpirationFilterEntries';

export default function FirewallWaiverExpirationFilter({ selectedId, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  const [menuStyle, setMenuStyle] = useState({});
  const toggleRef = useRef(null);

  const selected = firewallExpirationDates.find(({ id }) => id === selectedId);
  const labelText = selected ? selected.name : 'expiration';

  const computePosition = useCallback(() => {
    if (toggleRef.current) {
      const rect = toggleRef.current.getBoundingClientRect();
      setMenuStyle({
        position: 'fixed',
        top: rect.bottom,
        left: rect.left,
        minWidth: rect.width,
        zIndex: 9999,
      });
    }
  }, []);

  const open = () => {
    computePosition();
    setIsOpen(true);
  };

  const close = useCallback(() => setIsOpen(false), []);

  const handleSelect = (id) => {
    onChange(id);
    close();
  };

  useEffect(() => {
    if (!isOpen) return;
    const onMouseDown = (e) => {
      if (toggleRef.current && !toggleRef.current.contains(e.target)) {
        close();
      }
    };
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [isOpen, close]);

  useEffect(() => {
    if (!isOpen) return;
    const reposition = () => computePosition();
    window.addEventListener('scroll', reposition, true);
    window.addEventListener('resize', reposition);
    return () => {
      window.removeEventListener('scroll', reposition, true);
      window.removeEventListener('resize', reposition);
    };
  }, [isOpen, computePosition]);

  return (
    <>
      <button
        ref={toggleRef}
        type="button"
        className={`nx-btn nx-btn--tertiary nx-dropdown__toggle${isOpen ? ' open' : ''}`}
        onClick={() => (isOpen ? close() : open())}
        aria-haspopup="true"
        aria-expanded={isOpen}
      >
        <span className="nx-dropdown__toggle-label">
          <NxFontAwesomeIcon icon={faFilter} />
          <span>{labelText}</span>
        </span>
        <NxFontAwesomeIcon className="nx-dropdown__toggle-caret" icon={isOpen ? faCaretUp : faCaretDown} size="lg" />
      </button>
      {isOpen &&
        ReactDOM.createPortal(
          <div className="nx-dropdown-menu" style={menuStyle}>
            {firewallExpirationDates.map(({ id, name }) => (
              <button key={id} className="nx-dropdown-button" onMouseDown={() => handleSelect(id)}>
                {name}
              </button>
            ))}
          </div>,
          document.body
        )}
    </>
  );
}

FirewallWaiverExpirationFilter.propTypes = {
  selectedId: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};
