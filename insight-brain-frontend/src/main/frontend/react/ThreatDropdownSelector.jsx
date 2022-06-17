/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  allThreatLevelNumbers,
  categoryByPolicyThreatLevel,
  NxDropdown,
  NxThreatIndicator,
  useToggle,
} from '@sonatype/react-shared-components';

import * as PropTypes from 'prop-types';
import { is, reverse } from 'ramda';
import { capitalize } from '../util/jsUtil';

export default function ThreatDropdownSelector({ threatLevel, onSelectThreatLevel, ...props }) {
  const renderThreatLevel = (level) =>
    is(Number, level) ? (
      <span>
        <NxThreatIndicator policyThreatLevel={level} /> {level} - {capitalize(categoryByPolicyThreatLevel[level])}
      </span>
    ) : (
      'Threat level'
    );

  const [isThreatDropdownOpen, toggleIsThreatDropdownOpen] = useToggle(false);

  return (
    <NxDropdown
      label={renderThreatLevel(threatLevel)}
      isOpen={isThreatDropdownOpen}
      onToggleCollapse={toggleIsThreatDropdownOpen}
      {...props}
    >
      {reverse(allThreatLevelNumbers).map((level) => {
        return (
          <button
            onClick={() => {
              onSelectThreatLevel(level);
              toggleIsThreatDropdownOpen();
            }}
            className="nx-dropdown-button"
            key={`${level}-dropdown-option`}
          >
            {renderThreatLevel(level)}
          </button>
        );
      })}
    </NxDropdown>
  );
}

ThreatDropdownSelector.defaultProps = {
  onSelect: () => {},
};

ThreatDropdownSelector.propTypes = {
  threatLevel: PropTypes.number,
  onSelectThreatLevel: PropTypes.func,
};
