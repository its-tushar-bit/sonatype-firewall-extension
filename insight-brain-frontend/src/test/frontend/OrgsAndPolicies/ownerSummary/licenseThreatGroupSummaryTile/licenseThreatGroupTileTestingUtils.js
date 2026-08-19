/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fireEvent, within } from 'TestRoot/SpecUtil';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { verifyThreatLevelIndicator, verifyHeaderCell } from '../utils/tileAndTableTestingUtils';
import { sortByThreatLevel } from 'MainRoot/OrgsAndPolicies/utility/util';

export const verifyTableHead = (thead) => {
  let rows, headers;

  rows = within(thead).getAllByRole('row');
  expect(rows).toHaveLength(1);

  headers = within(rows[0]).getAllByRole('columnheader');
  expect(headers.length).toBe(3);
  verifyHeaderCell(headers[0], false, 'THREAT');
  verifyHeaderCell(headers[1], false, 'NAME');
  expect(within(rows[0]).getByRole('columnheader', { name: 'view threat group' })).toBeVisible();
};

export const nLevelVerifyTableContent = (tableSections, owners, goToEditLTGSpy) => {
  expect(tableSections.length).toBe(owners.length);

  for (const section of tableSections) {
    let allRows, contentRows, ltg, editButton;
    const index = tableSections.indexOf(section);
    const owner = owners[index];
    const licenseThreatGroups = sortByThreatLevel(owner.licenseThreatGroups);
    const cellsPerLTGRow = isNilOrEmpty(licenseThreatGroups) ? 1 : 3;
    const firstContentRow = within(section).getAllByRole('row')[1];
    const expectedEmptyMessage = owner.inherited
      ? `No ${owner.ownerName} threat groups defined`
      : 'No local threat groups defined';

    expect(within(firstContentRow).getAllByRole('cell')).toHaveLength(cellsPerLTGRow);

    // render correct title
    if (owner.inherited) {
      expect(within(section).getByText(`Inherited from ${owner.ownerName}`)).toBeVisible();
    } else {
      expect(within(section).getByText(`Local to ${owner.ownerName}`)).toBeVisible();
    }

    // if ltg count is 0, there should always be two rows and the empty message
    if (licenseThreatGroups.length === 0) {
      expect(within(section).getByText(expectedEmptyMessage)).toBeVisible();
      expect(within(section).getAllByRole('row')).toHaveLength(2);
    } else {
      allRows = within(section).getAllByRole('row');
      contentRows = allRows.slice(1);
      expect(allRows).toHaveLength(licenseThreatGroups.length + 1);
      expect(contentRows).toHaveLength(licenseThreatGroups.length);

      contentRows.forEach((row, index) => {
        ltg = licenseThreatGroups[index];

        verifyThreatLevelIndicator(row, ltg.threatLevel);
        expect(within(row).getByRole('cell', { name: ltg.name })).toBeVisible();

        if (!owner.inherited) {
          editButton = within(row).getByRole('button', { name: `Edit ${ltg.name} License Threat Group` });
          expect(editButton).toBeVisible();
          fireEvent.click(editButton);
          expect(goToEditLTGSpy).toHaveBeenCalledWith(ltg.id);
        }
      });
    }
  }
};
