/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxDropdown, NxTooltip, useToggle } from '@sonatype/react-shared-components';

import * as PropTypes from 'prop-types';
import { reverse } from 'ramda';
import cx from 'classnames';

export default function AutoWaiverScopeDropdownSelector({
  scope,
  onSelectScope = () => {},
  className,
  disabled,
  ...props
}) {
  const scopes = ['any', 'all'];

  const renderScope = (scope) => (disabled ? <span>any/all</span> : <span>{scope}</span>);

  const [isScopeDropdownOpen, toggleIsScopeDropdownOpen] = useToggle(false);

  const classnames = cx('iq-threat-dropdown-selector', { disabled }, className);

  useEffect(() => {
    if (disabled && scope === 'all') {
      onSelectScope('any');
    }
  }, [scope, disabled]);

  return (
    <NxTooltip title={disabled ? 'Select both conditions below to enable this option' : ''}>
      <NxDropdown
        {...props}
        label={renderScope(scope)}
        isOpen={isScopeDropdownOpen}
        onToggleCollapse={toggleIsScopeDropdownOpen}
        className={classnames}
        disabled={disabled}
      >
        {reverse(scopes).map((scope) => {
          return (
            <button
              onClick={() => {
                onSelectScope(scope);
                toggleIsScopeDropdownOpen();
              }}
              type="button"
              className="nx-dropdown-button"
              key={`${scope}-dropdown-option`}
            >
              {renderScope(scope)}
            </button>
          );
        })}
      </NxDropdown>
    </NxTooltip>
  );
}

AutoWaiverScopeDropdownSelector.propTypes = {
  scope: PropTypes.string,
  className: PropTypes.string,
  onSelectScope: PropTypes.func,
  disabled: PropTypes.bool,
};
