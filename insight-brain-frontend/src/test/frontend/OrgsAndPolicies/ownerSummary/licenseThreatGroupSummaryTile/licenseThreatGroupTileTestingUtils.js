/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fireEvent, screen, within } from 'TestRoot/SpecUtil';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { verifyThreatLevelIndicator, verifyHeaderCell } from '../utils/tileAndTableTestingUtils';

export const verifyLicenseThreatGroupsTable = (table, ownerName, licenseThreatGroups, inherited, goToEditLTGSpy) => {
  const groups = within(table).getAllByRole('rowgroup');
  expect(groups).toHaveSize(2);

  verifyTableHead(groups[0], inherited);
  verifyTableBody(groups[1], ownerName, licenseThreatGroups, inherited, goToEditLTGSpy);
};

const getNumberOfColumns = (inherited) => {
  return inherited ? 2 : 3; // threatLevel, ltg name and one clickable arrow if ltg is not inherited
};

const verifyTableHead = (thead, inherited) => {
  let rows, headers;
  const totalOfColumns = getNumberOfColumns(inherited);

  rows = within(thead).getAllByRole('row');
  expect(rows).toHaveSize(1);

  headers = within(rows[0]).getAllByRole('columnheader');
  expect(headers.length).toBe(totalOfColumns);
  verifyHeaderCell(headers[0], false, 'THREAT');
  verifyHeaderCell(headers[1], false, 'NAME');
  if (!inherited) {
    expect(within(rows[0]).getByRole('columnheader', { name: 'Select Row' })).toBeVisible();
  }
};

const verifyTableBody = (tbody, ownerName, licenseThreatGroups, inherited, goToEditLTGSpy) => {
  let rows, editButton;
  const totalOfColumns = getNumberOfColumns(inherited);

  rows = within(tbody).getAllByRole('row');

  if (isNilOrEmpty(licenseThreatGroups)) {
    const name = inherited ? ownerName : 'local';
    const emptyMessage = `No ${name} threat groups defined.`;
    expect(rows).toHaveSize(1);
    expect(screen.getByRole('cell', { name: emptyMessage })).toBeVisible();
  } else {
    expect(rows).toHaveSize(licenseThreatGroups.length);

    rows.forEach((row, index) => {
      let ltg = licenseThreatGroups[index];

      expect(within(row).getAllByRole('cell')).toHaveSize(totalOfColumns);
      verifyThreatLevelIndicator(row, ltg.threatLevel);
      expect(within(row).getByRole('cell', { name: ltg.name })).toBeVisible();
      if (!inherited) {
        editButton = within(row).getByRole('button', { name: `Edit ${ltg.name} License Threat Group` });
        expect(editButton).toBeVisible();
        fireEvent.click(editButton);
        expect(goToEditLTGSpy).toHaveBeenCalledWith(ltg.id);
      }
    });
  }
};
