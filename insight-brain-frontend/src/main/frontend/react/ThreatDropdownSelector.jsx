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
import cx from 'classnames';
import { capitalize } from 'MainRoot/util/jsUtil';

export default function ThreatDropdownSelector({ threatLevel, onSelectThreatLevel, className, ...props }) {
  const renderThreatLevel = (level) =>
    is(Number, level) ? (
      <>
        <NxThreatIndicator policyThreatLevel={level} />
        <span>
          {level} - {capitalize(categoryByPolicyThreatLevel[level])}
        </span>
      </>
    ) : (
      'Threat level'
    );

  const [isThreatDropdownOpen, toggleIsThreatDropdownOpen] = useToggle(false);

  const classnames = cx('iq-threat-dropdown-selector', className);

  return (
    <NxDropdown
      {...props}
      label={renderThreatLevel(threatLevel)}
      isOpen={isThreatDropdownOpen}
      onToggleCollapse={toggleIsThreatDropdownOpen}
      className={classnames}
    >
      {reverse(allThreatLevelNumbers).map((level) => {
        return (
          <button
            onClick={() => {
              onSelectThreatLevel(level);
              toggleIsThreatDropdownOpen();
            }}
            type="button"
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
  onSelectThreatLevel: () => {},
};

ThreatDropdownSelector.propTypes = {
  threatLevel: PropTypes.number,
  className: PropTypes.string,
  onSelectThreatLevel: PropTypes.func,
};
