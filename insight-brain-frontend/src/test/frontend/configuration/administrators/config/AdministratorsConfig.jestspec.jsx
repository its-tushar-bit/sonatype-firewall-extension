/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AdministratorsConfig from 'MainRoot/configuration/administrators/config/AdministratorsConfig';
import * as administratorsSelectors from 'MainRoot/configuration/administrators/administratorsSelectors';
import { render, screen, within } from 'TestRoot/SpecUtil';

describe('AdministratorsConfig', () => {
  let renderComponent, selectIsLoadingSpy, selectLoadErrorSpy, selectMembersByRoleSpy;
  const membersByRoleMock = [
    {
      roleId: '1',
      roleName: 'Policy Administrator',
      roleDescription: 'Manages all organizations, applications, policies, and policy violations.',
      membersByOwner: [
        {
          ownerId: 'global',
          ownerName: 'Global',
          ownerType: 'global',
          members: [
            {
              type: 'USER',
              internalName: 'admin',
              displayName: 'Admin BuiltIn',
              email: 'admin@localhost',
              realm: 'IQ Server',
            },
          ],
        },
      ],
    },
    {
      roleId: '2',
      roleName: 'System Administrator',
      roleDescription: 'Manages system configuration and users.',
      membersByOwner: [
        {
          ownerId: 'global',
          ownerName: 'Global',
          ownerType: 'global',
          members: [
            {
              type: 'USER',
              internalName: 'admin',
              displayName: 'Admin BuiltIn',
              email: 'admin@localhost',
              realm: 'IQ Server',
            },
          ],
        },
      ],
    },
  ];

  beforeEach(() => {
    selectIsLoadingSpy = jest.spyOn(administratorsSelectors, 'selectIsLoading').mockReturnValue(false);
    selectLoadErrorSpy = jest.spyOn(administratorsSelectors, 'selectLoadError').mockReturnValue(null);
    selectMembersByRoleSpy = jest.spyOn(administratorsSelectors, 'selectMembersByRole').mockReturnValue([]);

    renderComponent = () => render(<AdministratorsConfig />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Administrators')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectIsLoadingSpy.mockReturnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders an empty table', () => {
    renderComponent();

    const rows = screen.getAllByRole('row');
    expect(within(rows[1]).getByText('No data found.')).toBeVisible();
  });

  it('renders a table with header and 2 data rows', () => {
    selectMembersByRoleSpy.mockReturnValue(membersByRoleMock);
    renderComponent();

    const rows = screen.getAllByRole('row');

    const headerRow = rows[0];

    expect(within(headerRow).getByText('Role')).toBeVisible();
    expect(within(headerRow).getByText('Members')).toBeVisible();

    expect(within(rows[1]).getByText('Policy Administrator')).toBeVisible();
    expect(within(rows[1]).getByText('Admin BuiltIn')).toBeVisible();

    expect(within(rows[2]).getByText('System Administrator')).toBeVisible();
    expect(within(rows[2]).getByText('Admin BuiltIn')).toBeVisible();
  });

  it('shows error message on error', () => {
    selectLoadErrorSpy.mockReturnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});
