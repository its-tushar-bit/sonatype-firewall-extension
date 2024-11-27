/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'TestRoot/SpecUtil';
import IqOrgAppPicker from '../../../main/frontend/components/iqOrgAppPicker/IqOrgAppPicker';
import { mockAppsBig, mockAppsSmall } from './mockData.js';

describe('IqOrgAppPicker', function () {
  let mockOrganizations,
    mockApplications,
    mockSelectedOrganizations,
    mockSelectedApplications,
    onChangeSpy,
    minimalProps;

  mockOrganizations = {
    ROOT_ORGANIZATION_ID: {
      type: 'organization',
      id: 'ROOT_ORGANIZATION_ID',
      name: 'Root Organization',
      synthetic: true,
      parentOrganizationId: null,
      applicationIds: null,
      subOrgs: 12,
      totalApps: 5,
      organizationIds: ['fooOrg', 'barOrg'],
    },
    fooOrg: {
      type: 'organization',
      id: 'fooOrg',
      name: 'Foo Org',
      synthetic: false,
      parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      applicationIds: ['fooApp1', 'fooApp2'],
      subOrgs: 0,
      totalApps: 2,
      organizationIds: [],
    },
    barOrg: {
      type: 'organization',
      id: 'barOrg',
      name: 'Bar Org',
      synthetic: false,
      parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      applicationIds: ['barApp1', 'barApp2'],
      subOrgs: 1,
      totalApps: 2,
      organizationIds: ['bazOrg'],
    },
    bazOrg: {
      type: 'organization',
      id: 'bazOrg',
      name: 'Baz Org',
      synthetic: false,
      parentOrganizationId: 'barOrg',
      applicationIds: null,
      subOrgs: 1,
      totalApps: 0,
      organizationIds: ['bazOrg2'],
    },
    bazOrg2: {
      type: 'organization',
      id: 'bazOrg2',
      name: 'Baz Org 2',
      synthetic: false,
      parentOrganizationId: 'bazOrg',
      applicationIds: null,
      subOrgs: 1,
      totalApps: 0,
      organizationIds: ['bazOrg3'],
    },
    bazOrg3: {
      type: 'organization',
      id: 'bazOrg3',
      name: 'Baz Org 3',
      synthetic: false,
      parentOrganizationId: 'bazOrg2',
      applicationIds: null,
      subOrgs: 1,
      totalApps: 0,
      organizationIds: [],
    },
  };

  mockApplications = [
    { id: 'fooApp1', name: 'Foo App 1', organizationId: 'fooOrg' },
    { id: 'fooApp2', name: 'Foo App 2', organizationId: 'fooOrg' },
    { id: 'barApp1', name: 'Bar App 1', organizationId: 'barOrg' },
    { id: 'barApp2', name: 'Bar App 2', organizationId: 'barOrg' },
  ];

  mockSelectedOrganizations = new Set();
  mockSelectedApplications = new Set();

  function renderComponent(additionalProps) {
    return render(<IqOrgAppPicker {...minimalProps} {...additionalProps} />);
  }

  beforeEach(function () {
    onChangeSpy = jest.fn().mockName('onChange');

    minimalProps = {
      applications: mockApplications,
      ownersMap: mockOrganizations,
      selectedOrganizations: mockSelectedOrganizations,
      selectedApplications: mockSelectedApplications,
      onChange: onChangeSpy,
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
    };
  });

  it('returns a group with the organizations and one with the applications', async () => {
    const user = userEvent.setup();
    renderComponent();

    const [orgMultiSelect, appMultiSelect] = screen.getAllByRole('group');

    expect(orgMultiSelect).toBeInTheDocument();
    expect(appMultiSelect).toBeInTheDocument();

    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));
    await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

    const orgCheckboxes = within(orgMultiSelect).getAllByRole('menuitemcheckbox');
    const appCheckboxes = within(appMultiSelect).getAllByRole('menuitemcheckbox');

    expect(orgCheckboxes).toHaveLength(6);
    expect(appCheckboxes).toHaveLength(5);

    expect(orgCheckboxes[0]).toHaveAccessibleName('all/none');
    expect(orgCheckboxes[1]).toHaveAccessibleName('Foo Org');
    expect(orgCheckboxes[2]).toHaveAccessibleName('Bar Org');
    expect(orgCheckboxes[3]).toHaveAccessibleName('Baz Org');
    expect(orgCheckboxes[4]).toHaveAccessibleName('Baz Org 2');
    expect(orgCheckboxes[5]).toHaveAccessibleName('Baz Org 3');

    expect(appCheckboxes[0]).toHaveAccessibleName('all/none');
    expect(appCheckboxes[1]).toHaveAccessibleName('Foo App 1');
    expect(appCheckboxes[2]).toHaveAccessibleName('Foo App 2');
    expect(appCheckboxes[3]).toHaveAccessibleName('Bar App 1');
    expect(appCheckboxes[4]).toHaveAccessibleName('Bar App 2');
  });

  it('selects checkboxes per passed-in selected prop', async () => {
    const user = userEvent.setup();
    const selectedOrganizations = new Set(['fooOrg', 'barOrg']);
    const selectedApplications = new Set(['fooApp2']);

    renderComponent({
      selectedOrganizations,
      selectedApplications,
    });

    const [orgMultiSelect, appMultiSelect] = screen.getAllByRole('group');

    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));
    await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

    const checkedOrgCheckboxes = within(orgMultiSelect).getAllByRole('menuitemcheckbox', { checked: true });
    const checkedAppCheckboxes = within(appMultiSelect).getAllByRole('menuitemcheckbox', { checked: true });

    expect(checkedOrgCheckboxes).toHaveLength(2);
    expect(checkedOrgCheckboxes[0]).toHaveAccessibleName('Foo Org');
    expect(checkedOrgCheckboxes[1]).toHaveAccessibleName('Bar Org');
    expect(checkedAppCheckboxes).toHaveLength(1);
    expect(checkedAppCheckboxes[0]).toHaveAccessibleName('Foo App 2');
  });

  describe('org all/none checkbox', function () {
    it('selects all apps and orgs when toggled to checked', async function () {
      const user = userEvent.setup();
      renderComponent();

      const orgMultiSelect = screen.getAllByRole('group')[0];
      await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

      const orgAllNone = within(orgMultiSelect).getByRole('menuitemcheckbox', { name: /all\/none/ });
      await user.click(orgAllNone);

      expect(onChangeSpy).toHaveBeenCalledWith(
        new Set(['barOrg', 'bazOrg', 'bazOrg2', 'bazOrg3', 'fooOrg']),
        new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2'])
      );
    });

    it('deselects all apps and orgs when toggled to unchecked', async function () {
      const user = userEvent.setup();
      const selectedOrganizations = new Set(['barOrg', 'bazOrg', 'bazOrg2', 'bazOrg3', 'fooOrg']);
      const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      renderComponent({ selectedOrganizations, selectedApplications });

      const orgMultiSelect = screen.getAllByRole('group')[0];
      await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

      const orgAllNone = within(orgMultiSelect).getByRole('menuitemcheckbox', { name: /all\/none/ });
      await user.click(orgAllNone);

      expect(onChangeSpy).toHaveBeenCalledWith(new Set(), new Set());
    });
  });

  it('selects related apps when an org is toggled to checked', async function () {
    const user = userEvent.setup();
    const selectedOrganizations = new Set();
    const selectedApplications = new Set(['fooApp2', 'barApp1']);
    renderComponent({ selectedOrganizations, selectedApplications });

    const orgMultiSelect = screen.getAllByRole('group')[0];
    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

    const fooOrgCheckbox = within(orgMultiSelect).getByRole('menuitemcheckbox', { name: 'Foo Org' });
    await user.click(fooOrgCheckbox);

    const newSelectedOrganizations = new Set(['fooOrg']);
    const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);

    expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
  });

  it('deselects related apps when an org is toggled to unchecked', async function () {
    const user = userEvent.setup();
    const selectedOrganizations = new Set(['barOrg', 'fooOrg']);
    const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

    renderComponent({ selectedOrganizations, selectedApplications });

    const orgMultiSelect = screen.getAllByRole('group')[0];
    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

    const fooOrgCheckbox = within(orgMultiSelect).getByRole('menuitemcheckbox', { name: 'Foo Org' });
    await user.click(fooOrgCheckbox);

    const newSelectedOrganizations = new Set(['barOrg']);
    const expectedSelectedApplications = new Set(['barApp1', 'barApp2']);

    expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
  });

  it('does not deselect related apps when the org is toggled when apps were already selected', async function () {
    const user = userEvent.setup();
    const selectedOrganizations = new Set();
    const selectedApplications = new Set(['fooApp1', 'fooApp2']);

    renderComponent({ selectedOrganizations, selectedApplications });

    const orgMultiSelect = screen.getAllByRole('group')[0];
    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

    const fooOrgCheckbox = within(orgMultiSelect).getByRole('menuitemcheckbox', { name: 'Foo Org' });
    await user.click(fooOrgCheckbox);

    const newSelectedOrganizations = new Set(['fooOrg']);
    const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2']);

    expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
  });

  describe('changing applications', function () {
    it('does not select an org when all related apps are selected', async function () {
      const user = userEvent.setup();
      const selectedApplications = new Set(['fooApp1']);
      const selectedOrganizations = new Set();

      renderComponent({ selectedOrganizations, selectedApplications });

      const appMultiSelect = screen.getAllByRole('group')[1];
      await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

      const fooApp2Checkbox = within(appMultiSelect).getByRole('menuitemcheckbox', { name: /Foo App 2/ });
      await user.click(fooApp2Checkbox);

      const newSelectedApplications = new Set(['fooApp1', 'fooApp2']);
      const expectedSelectedOrganizations = new Set();

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });

    it('deselects an org if not all related apps are selected', async function () {
      const user = userEvent.setup();
      const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      const selectedOrganizations = new Set(['fooOrg', 'barOrg']);

      renderComponent({ selectedOrganizations, selectedApplications });

      const appMultiSelect = screen.getAllByRole('group')[1];
      await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

      const fooApp2Checkbox = within(appMultiSelect).getByRole('menuitemcheckbox', { name: /Foo App 2/ });
      await user.click(fooApp2Checkbox);

      const newSelectedApplications = new Set(['fooApp1', 'barApp1', 'barApp2']);
      const expectedSelectedOrganizations = new Set(['barOrg']);

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });
  });

  it('does not render synthetic orgs', async function () {
    const user = userEvent.setup();
    const ownersMapWithSyntheticOrgs = {
      ROOT_ORGANIZATION_ID: {
        type: 'organization',
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        synthetic: true,
        parentOrganizationId: null,
        applicationIds: null,
        subOrgs: 12,
        totalApps: 5,
        organizationIds: ['fooOrg', 'barOrg'],
      },
      fooOrg: {
        type: 'organization',
        id: 'fooOrg',
        name: 'Foo Org',
        synthetic: true,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        applicationIds: null,
        subOrgs: 0,
        totalApps: 2,
        organizationIds: [],
      },
      barOrg: {
        type: 'organization',
        id: 'barOrg',
        name: 'Bar Org',
        synthetic: true,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        applicationIds: ['barApp1', 'barApp2'],
        subOrgs: 1,
        totalApps: 2,
        organizationIds: ['bazOrg'],
      },
      bazOrg: {
        type: 'organization',
        id: 'bazOrg',
        name: 'Baz Org',
        synthetic: false,
        parentOrganizationId: 'barOrg',
        applicationIds: null,
        subOrgs: 1,
        totalApps: 0,
        organizationIds: ['bazOrg2'],
      },
      bazOrg2: {
        type: 'organization',
        id: 'bazOrg2',
        name: 'Baz Org 2',
        synthetic: false,
        parentOrganizationId: 'bazOrg',
        applicationIds: null,
        subOrgs: 1,
        totalApps: 0,
        organizationIds: ['bazOrg3'],
      },
      bazOrg3: {
        type: 'organization',
        id: 'bazOrg3',
        name: 'Baz Org 3',
        synthetic: true,
        parentOrganizationId: 'bazOrg2',
        applicationIds: null,
        subOrgs: 1,
        totalApps: 0,
        organizationIds: [],
      },
    };

    renderComponent({ ownersMap: ownersMapWithSyntheticOrgs });

    const orgMultiSelect = screen.getAllByRole('group')[0];
    await user.click(within(orgMultiSelect).getByRole('button', { name: /Organizations/ }));

    const orgCheckboxes = within(orgMultiSelect).getAllByRole('menuitemcheckbox');
    expect(orgCheckboxes).toHaveLength(4);
    expect(orgCheckboxes[0]).toHaveAccessibleName('all/none');
    expect(orgCheckboxes[1]).toHaveAccessibleName('Bar Org');
    expect(orgCheckboxes[2]).toHaveAccessibleName('Baz Org');
    expect(orgCheckboxes[3]).toHaveAccessibleName('Baz Org 2');
  });

  it('does not show a warning icon when apps is 500 or less', async function () {
    const user = userEvent.setup();
    const applications = mockAppsSmall;
    renderComponent({ applications });

    const appMultiSelect = screen.getAllByRole('group')[1];
    await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

    const applicationsCheckboxes = within(appMultiSelect).getAllByRole('menuitemcheckbox');
    expect(applications).toHaveLength(500);
    // there are 501 checkboxes because of the all/none checkbox
    expect(applicationsCheckboxes).toHaveLength(501);
    const warningIcon = screen.queryByTestId('iq-limited-apps-warning-icon');
    expect(warningIcon).not.toBeInTheDocument();
  });

  it('shows a warning icon when apps is 501 or more and only shows 500 apps', async function () {
    const user = userEvent.setup();
    const applications = mockAppsBig;
    renderComponent({ applications });

    const appMultiSelect = screen.getAllByRole('group')[1];
    await user.click(within(appMultiSelect).getByRole('button', { name: /Applications/ }));

    const applicationsCheckboxes = within(appMultiSelect).getAllByRole('menuitemcheckbox');
    expect(applications).toHaveLength(1000);
    // there are 501 checkboxes because of the all/none checkbox
    expect(applicationsCheckboxes).toHaveLength(501);
    const warningIcon = screen.queryByTestId('iq-limited-apps-warning-icon');
    expect(warningIcon).toBeVisible();
  });
});
