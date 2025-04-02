/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxDropdown,
  useToggle,
} from '@sonatype/react-shared-components';

import * as PropTypes from 'prop-types';
import { reverse } from 'ramda';
import cx from 'classnames';

export default function AutoWaiverScopeDropdownSelector({ scope, onSelectScope, className, ...props }) {
  const scopes =  ['any', 'all'];

  const renderScope = (scope) =>
      <span>
        {scope}
      </span>;

  const [isScopeDropdownOpen, toggleIsScopeDropdownOpen] = useToggle(false);

  const classnames = cx('iq-threat-dropdown-selector', className);

  return (
    <NxDropdown
      {...props}
      label={renderScope(scope)}
      isOpen={isScopeDropdownOpen}
      onToggleCollapse={toggleIsScopeDropdownOpen}
      className={classnames}
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
  );
}

AutoWaiverScopeDropdownSelector.defaultProps = {
  onSelectScope: () => {},
};

AutoWaiverScopeDropdownSelector.propTypes = {
  scope: PropTypes.string,
  className: PropTypes.string,
  onSelectScope: PropTypes.func,
};
