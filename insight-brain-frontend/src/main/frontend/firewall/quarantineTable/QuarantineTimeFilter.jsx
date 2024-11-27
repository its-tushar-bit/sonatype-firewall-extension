/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxDropdown, NxFontAwesomeIcon, useToggle } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import constants from './QuarantineTimeConstants';

const { ONE_DAY, SEVEN_DAYS, THIRTY_DAYS, NINETY_DAYS, ONE_YEAR, ALL_TIME } = constants.QUARANTINE_TIME;

export default function QuarantineTimeFilter({ filterQuarantineTime, setQuarantineGridQuarantineTimeFilter }) {
  const defaultLabelElement = (labelText) => (
    <>
      <NxFontAwesomeIcon icon={faFilter}></NxFontAwesomeIcon>
      <span>{labelText}</span>
    </>
  );

  const getLabel = (filterQuarantineTime) => {
    switch (filterQuarantineTime) {
      case ONE_DAY.VALUE:
        return defaultLabelElement(ONE_DAY.LABEL);
      case SEVEN_DAYS.VALUE:
        return defaultLabelElement(SEVEN_DAYS.LABEL);
      case THIRTY_DAYS.VALUE:
        return defaultLabelElement(THIRTY_DAYS.LABEL);
      case NINETY_DAYS.VALUE:
        return defaultLabelElement(NINETY_DAYS.LABEL);
      case ONE_YEAR.VALUE:
        return defaultLabelElement(ONE_YEAR.LABEL);
      case ALL_TIME.VALUE:
        return defaultLabelElement(ALL_TIME.LABEL);
      case null:
        return defaultLabelElement(constants.FILTER);
    }
  };

  const [isOpen, onToggleCollapse] = useToggle(false);

  return (
    <NxDropdown
      label={getLabel(filterQuarantineTime)}
      isOpen={isOpen}
      onToggleCollapse={onToggleCollapse}
      id="firewall-quarantine-table--select-quarantine-time"
    >
      {Object.values(constants.QUARANTINE_TIME).map((option) => (
        <button
          key={option.VALUE}
          onClick={() => setQuarantineGridQuarantineTimeFilter(option.VALUE)}
          className="nx-dropdown-button"
        >
          {option.LABEL}
        </button>
      ))}
    </NxDropdown>
  );
}

QuarantineTimeFilter.propTypes = {
  filterQuarantineTime: PropTypes.number,
  setQuarantineGridQuarantineTimeFilter: PropTypes.func.isRequired,
};
