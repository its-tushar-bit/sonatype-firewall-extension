/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { nLevelVerifyTableContent, verifyTableHead } from './licenseThreatGroupTileTestingUtils';
import { reject } from 'ramda';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { isEmptyNonLocal } from 'MainRoot/OrgsAndPolicies/utility/util';
import ApplicableLicenseThreatGroupTable from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/ApplicableLicenseThreatGroupTable';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  organizationWithoutLtgsByOwnerPayload,
  rootOrganizationLtgsByOwnerPayload,
  nLevelOrgWithInheritedLTGs,
  nLevelAppWithNoLTGs,
  nLevelAppWithLTGs,
} from './licenseThreatGroupSummaryTileMockData';

import 'TestRoot/SpecUtil';

describe('ApplicableLicenseThreatGroupTable', () => {
  let renderComponent, licenseThreatGroups, testData, goToEditLTGSpy;

  beforeEach(() => {
    goToEditLTGSpy = jest.spyOn(actions, 'goToEditLTG');
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
    renderComponent = (information) => render(<ApplicableLicenseThreatGroupTable applicableLTGs={information} />);
  });

  it('renders expected table headers', async () => {
    testData = [
      {
        inherited: false,
        licenseThreatGroups: [],
        ownerName: 'Root Organization',
      },
    ];

    renderComponent(testData);

    const tableSections = await screen.findAllByRole('rowgroup');
    const tableHeaders = tableSections[0];

    verifyTableHead(tableHeaders);
  });

  it('renders table with empty message if owner has no ltgs', () => {
    testData = [
      {
        inherited: false,
        licenseThreatGroups: [],
        ownerName: 'Root Organization',
      },
    ];

    renderComponent(testData);

    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    // get tbody elemnts
    const tableSections = screen.getAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, testData, goToEditLTGSpy);
  });

  it('renders table with three clickable rows', () => {
    testData = [
      {
        ownerName: 'Root Organization',
        licenseThreatGroups,
        inherited: true,
      },
    ];

    renderComponent(testData);
    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    // get tbody elemnts
    const tableSections = screen.getAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, testData, goToEditLTGSpy);
  });

  it('renders table with three non-clickable rows', () => {
    testData = [
      {
        ownerName: 'Root Organization',
        licenseThreatGroups,
        inherited: false,
      },
    ];
    renderComponent(testData);
    const table = screen.getByRole('table');
    expect(table).toBeVisible();
    // get tbody elemnts
    const tableSections = screen.getAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, testData, goToEditLTGSpy);
  });

  it('renders all correct subsection content when owner is root org', async () => {
    const ownersWithPolicies = rootOrganizationLtgsByOwnerPayload.ltgs.licenseThreatGroupsByOwner.filter(
      (owner) => !isNilOrEmpty(owner.licenseThreatGroups)
    );

    renderComponent(ownersWithPolicies);

    // get tbody elemnts
    const tableSections = await screen.findAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, ownersWithPolicies, goToEditLTGSpy);
  });

  it('renders all correct subsection content when owner is org with no license threat groups', async () => {
    const ownersWithPolicies = organizationWithoutLtgsByOwnerPayload.ltgs.licenseThreatGroupsByOwner;
    renderComponent(ownersWithPolicies);

    // get tbody elemnts
    const tableSections = await screen.findAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, ownersWithPolicies, goToEditLTGSpy);
  });

  it('renders all correct subsection content when owner is org with many inherited license threat groups', async () => {
    const ownersWithPolicies = reject(isEmptyNonLocal, nLevelOrgWithInheritedLTGs);
    renderComponent(ownersWithPolicies);

    // get tbody elemnts
    const tableSections = await screen.findAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, ownersWithPolicies, goToEditLTGSpy);
  });

  it('renders all correct subsection titles when owner is app with no license threat groups', async () => {
    const ownersWithPolicies = reject(isEmptyNonLocal, nLevelAppWithNoLTGs);
    renderComponent(ownersWithPolicies);

    // get tbody elemnts
    const tableSections = await screen.findAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, ownersWithPolicies, goToEditLTGSpy);
  });

  it('renders all correct subsection titles when owner is app with many license threat groups', async () => {
    const ownersWithPolicies = reject(isEmptyNonLocal, nLevelAppWithLTGs);
    renderComponent(ownersWithPolicies);

    // get tbody elemnts
    const tableSections = await screen.findAllByRole('rowgroup');
    const contentSections = tableSections.slice(1);

    nLevelVerifyTableContent(contentSections, ownersWithPolicies, goToEditLTGSpy);
  });
});
