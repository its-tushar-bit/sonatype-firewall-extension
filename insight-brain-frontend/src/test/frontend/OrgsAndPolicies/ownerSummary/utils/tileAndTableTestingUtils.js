/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { prop } from 'ramda';
import { within } from 'TestRoot/SpecUtil';

export function getNumberOfTables(fieldName, dataByOwner = []) {
  if (isNilOrEmpty(dataByOwner)) return 0;
  let numberOfTables = 1;
  dataByOwner.forEach((owner, index) => {
    const information = prop(fieldName, owner);
    if (index !== 0 && !isNilOrEmpty(information)) {
      numberOfTables++;
    }
  });
  return numberOfTables;
}

export const threatLevelToLabel = (threatLevel) => {
  switch (threatLevel) {
    case 10:
    case 9:
    case 8:
      return 'threat level critical';

    case 7:
    case 6:
    case 5:
    case 4:
      return 'threat level severe';

    case 3:
    case 2:
      return 'threat level moderate';

    case 1:
      return 'threat level low';

    case 0:
      return 'threat level none';

    default:
      return 'unspecified';
  }
};

export function verifyThreatLevelIndicator(row, threatLevel) {
  const threatLevelLabel = threatLevelToLabel(threatLevel);
  let threatLevelCell = within(row).getByRole('cell', { name: threatLevel });
  expect(threatLevelCell).toBeVisible();
  expect(within(threatLevelCell).getByLabelText(threatLevelLabel)).toBeVisible();
}

export function verifyHeaderCell(
  cell,
  sortingEnabled,
  text = '',
  isActiveSort = false,
  dir = '',
  stageShouldBeDisable = false
) {
  let ariaLabel = 'unsorted';
  let ariaSort = 'none';
  if (sortingEnabled) {
    if (isActiveSort) {
      ariaLabel = dir === 'asc' ? 'ascending' : 'descending';
      ariaSort = dir === 'asc' ? 'ascending' : 'descending';
    }

    expect(cell).toHaveAttribute('aria-sort', ariaSort);
    const sortingButton = within(cell).getByRole('button');
    expect(sortingButton).toBeVisible();
    expect(sortingButton).toHaveAttribute('aria-label', `${text} ${ariaLabel}`);
  }
  if (!isNilOrEmpty(text)) {
    expect(within(cell).getByText(text)).toBeVisible();
  }
  if (stageShouldBeDisable) {
    expect(cell).toHaveClass('policy-tile__cell--disabled');
  }
}
