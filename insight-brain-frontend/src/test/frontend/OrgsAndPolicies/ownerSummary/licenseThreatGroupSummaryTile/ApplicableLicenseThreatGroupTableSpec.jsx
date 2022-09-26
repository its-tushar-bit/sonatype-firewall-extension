/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { verifyLicenseThreatGroupsTable } from './licenseThreatGroupTileTestingUtils';

import ApplicableLicenseThreatGroupTable from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/ApplicableLicenseThreatGroupTable';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';

describe('ApplicableLicenseThreatGroupTable', () => {
  let renderComponent, licenseThreatGroups, testData, goToEditLTGSpy;

  beforeEach(() => {
    goToEditLTGSpy = spyOn(actions, 'goToEditLTG').and.callThrough();
    licenseThreatGroups = [
      {
        id: '542783ebfbc54698962875340a4f805b',
        name: 'Banned',
        threatLevel: 10,
        licenses: [],
      },
      {
        id: '7c6ad1eeefa848f5ae434464f0132599',
        name: 'Commercial',
        threatLevel: 7,
        licenses: [],
      },
      {
        id: '7dea5f29e910404f86d76d32c0a31fdc',
        name: 'Liberal',
        threatLevel: 0,
        licenses: [],
      },
    ];
    renderComponent = (information) =>
      render(
        <ApplicableLicenseThreatGroupTable
          licenseThreatGroups={information.licenseThreatGroups}
          inherited={information.inherited}
          key={'ltg-group-' + information.ownerId}
        />
      );
  });

  it('renders table with empty message if owner has no ltgs', () => {
    testData = {
      ownerName: 'Root Organization',
      licenseThreatGroups: [],
      inherited: false,
    };
    renderComponent({ ...testData });
    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    verifyLicenseThreatGroupsTable(
      table,
      testData.ownerName,
      testData.licenseThreatGroups,
      testData.inherited,
      goToEditLTGSpy
    );
  });

  it('renders table with three clickable rows', () => {
    testData = {
      ownerName: 'Root Organization',
      licenseThreatGroups,
      inherited: false,
    };
    renderComponent({ ...testData });
    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    verifyLicenseThreatGroupsTable(
      table,
      testData.ownerName,
      testData.licenseThreatGroups,
      testData.inherited,
      goToEditLTGSpy
    );
  });

  it('renders table with three non clickable rows', () => {
    testData = {
      ownerName: 'Root Organization',
      licenseThreatGroups,
      inherited: true,
    };
    renderComponent({ ...testData });
    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    verifyLicenseThreatGroupsTable(
      table,
      testData.ownerName,
      testData.licenseThreatGroups,
      testData.inherited,
      goToEditLTGSpy
    );
  });
});
