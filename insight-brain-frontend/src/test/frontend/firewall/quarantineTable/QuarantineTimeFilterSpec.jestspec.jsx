/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, fireEvent } from 'TestRoot/SpecUtil';
import QuarantineTimeFilter from 'MainRoot/firewall/quarantineTable/QuarantineTimeFilter';
import constants from 'MainRoot/firewall/quarantineTable/QuarantineTimeConstants';

const { ONE_DAY, SEVEN_DAYS, THIRTY_DAYS, NINETY_DAYS, ONE_YEAR, ALL_TIME } = constants.QUARANTINE_TIME;

describe('QuarantineTimeFilter', () => {
  let setQuarantineGridQuarantineTimeFilter;

  beforeEach(() => {
    setQuarantineGridQuarantineTimeFilter = jest.fn();
  });

  it('renders with default label', () => {
    const { container } = renderComponent(null);

    const defaultLabel = container.querySelector('.nx-dropdown');
    expect(defaultLabel).toHaveTextContent('Filter');
  });

  it('renders with correct label based on filterQuarantineTime prop', () => {
    const { container } = renderComponent(ONE_DAY.VALUE);

    const label = container.querySelector('.nx-dropdown');
    expect(label).toHaveTextContent(ONE_DAY.LABEL);
  });

  it('toggles dropdown on click', () => {
    const { container } = renderComponent(null);

    const dropdownToggle = container.querySelector('.nx-dropdown__toggle');
    fireEvent.click(dropdownToggle);
    expect(container.querySelector('.nx-dropdown-button')).toBeInTheDocument();

    fireEvent.click(dropdownToggle);
    expect(container.querySelector('.nx-dropdown-button')).not.toBeInTheDocument();
  });

  it('calls setQuarantineGridQuarantineTimeFilter with correct value on option click', () => {
    const { container } = renderComponent(null);

    testSelectedLabel(ONE_DAY, container);
    testSelectedLabel(SEVEN_DAYS, container);
    testSelectedLabel(THIRTY_DAYS, container);
    testSelectedLabel(NINETY_DAYS, container);
    testSelectedLabel(ONE_YEAR, container);
    testSelectedLabel(ALL_TIME, container);
  });

  function renderComponent(filterQuarantineTime) {
    return render(
      <QuarantineTimeFilter
        filterQuarantineTime={filterQuarantineTime}
        setQuarantineGridQuarantineTimeFilter={setQuarantineGridQuarantineTimeFilter}
      />
    );
  }

  function testSelectedLabel(quarantineTime, container) {
    const dropdownToggle = container.querySelector('.nx-dropdown__toggle');
    fireEvent.click(dropdownToggle);
    const option = findButtonByText(quarantineTime.LABEL, container);
    fireEvent.click(option);
    expect(setQuarantineGridQuarantineTimeFilter).toHaveBeenCalledWith(quarantineTime.VALUE);
  }

  function findButtonByText(text, container) {
    return Array.from(container.querySelectorAll('.nx-dropdown-button')).find((button) => button.textContent === text);
  }
});
