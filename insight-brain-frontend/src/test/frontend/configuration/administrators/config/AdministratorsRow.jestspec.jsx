/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AdministratorsRow from 'MainRoot/configuration/administrators/config/AdministratorsRow';
import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';

describe('AdministratorsRow', () => {
  let renderComponent, onClickSpy;

  beforeEach(() => {
    onClickSpy = jest.fn();
    const minimalProps = {
      onClick: onClickSpy,
      role: {
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
    };

    renderComponent = () => render(<AdministratorsRow {...minimalProps} />);
  });

  it('renders row with role and members', () => {
    renderComponent();

    const cells = screen.getAllByRole('cell');

    expect(within(cells[0]).getByText('Policy Administrator')).toBeVisible();
    expect(within(cells[1]).getByText('Admin BuiltIn')).toBeVisible();
  });

  it('calls onClick handler', () => {
    renderComponent();
    const clickable = screen.getByRole('row');
    fireEvent.click(clickable);

    expect(onClickSpy).toHaveBeenCalledTimes(1);
  });
});
