/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import IqOrgAppPicker from '../../../main/frontend/components/iqOrgAppPicker/IqOrgAppPicker';
import { NxCollapsibleMultiSelect, NxStatefulCollapsibleMultiSelect } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';

describe('IqOrgAppPicker', function () {
  let getShallowComponent,
    getMountedComponent,
    mountedComponent,
    mockOrganizations,
    mockApplications,
    mockSelectedOrganizations,
    mockSelectedApplications,
    onChangeSpy,
    wrapper,
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

  beforeEach(function () {
    onChangeSpy = jasmine.createSpy('onChange');

    minimalProps = {
      applications: mockApplications,
      ownersMap: mockOrganizations,
      selectedOrganizations: mockSelectedOrganizations,
      selectedApplications: mockSelectedApplications,
      onChange: onChangeSpy,
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(IqOrgAppPicker, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(IqOrgAppPicker, minimalProps);
    wrapper = getShallowComponent(minimalProps);
  });

  afterEach(function () {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('returns 2 NxStatefulTreeViewMultiSelect components and renders n-level orgs with padding', () => {
    mountedComponent = getMountedComponent(minimalProps);
    const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);

    expect(orgMultiSelect.find('.nx-checkbox').length).toBe(6);

    const firstChild = orgMultiSelect.find('.nx-checkbox').at(1);
    const secondChild = orgMultiSelect.find('.nx-checkbox').at(2);
    const thirdChild = orgMultiSelect.find('.nx-checkbox').at(3);
    const fourthChild = orgMultiSelect.find('.nx-checkbox').at(4);
    const fifthChild = orgMultiSelect.find('.nx-checkbox').at(5);

    expect(firstChild.find('.iq-filter-children-icon')).not.toExist();
    expect(secondChild.find('.iq-filter-children-icon')).not.toExist();
    expect(thirdChild.find('.iq-filter-children-icon')).toExist();
    expect(fourthChild.find('.iq-filter-children-icon')).toExist();
    expect(fifthChild.find('.iq-filter-children-icon')).toExist();

    expect(thirdChild.find('span').at(2)).toHaveStyle('paddingLeft', '0px');
    expect(fourthChild.find('span').at(2)).toHaveStyle('paddingLeft', '17px');
    expect(fifthChild.find('span').at(2)).toHaveStyle('paddingLeft', '34px');

    expect(wrapper.find(NxCollapsibleMultiSelect).length).toBe(1);
    expect(wrapper.find(NxStatefulCollapsibleMultiSelect).length).toBe(1);
  });

  it('sets selectedIds from passed-in selected prop', () => {
    const selectedOrganizations = new Set(['mock']);
    const selectedApplications = new Set(['mockApp']);

    const selectedIdsWrapper = getShallowComponent({
      ...minimalProps,
      selectedOrganizations,
      selectedApplications,
    });
    const orgMultiSelect = selectedIdsWrapper.find(NxCollapsibleMultiSelect).at(0);
    const appMultiSelect = selectedIdsWrapper.find(NxStatefulCollapsibleMultiSelect).at(0);
    expect(orgMultiSelect).toHaveProp('selectedIds', new Set(['mock']));
    expect(appMultiSelect).toHaveProp('selectedIds', new Set(['mockApp']));
  });

  describe('when all orgs are deselected (none)', function () {
    describe('when all orgs are selected (all)', function () {
      it('selects all apps', function () {
        mountedComponent = getMountedComponent(new Set(), new Set());

        const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);
        const orgAllNone = orgMultiSelect.find('.nx-checkbox__input').at(0);
        orgAllNone.simulate('change');

        expect(onChangeSpy).toHaveBeenCalledWith(
          new Set(['barOrg', 'bazOrg', 'bazOrg2', 'bazOrg3', 'fooOrg']),
          new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2'])
        );
      });

      it('deselects all apps when you use all/none to unselect all orgs', function () {
        const selectedOrganizations = new Set(['barOrg', 'bazOrg', 'bazOrg2', 'bazOrg3', 'fooOrg']);
        const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);
        const orgAllNone = orgMultiSelect.find('.nx-checkbox__input').at(0);
        orgAllNone.simulate('change');
        expect(onChangeSpy).toHaveBeenCalledWith(new Set(), new Set());
      });
    });
  });

  describe('when an org is selected', function () {
    it('selects related apps', function () {
      const selectedOrganizations = new Set();
      const selectedApplications = new Set(['fooApp2', 'barApp1']);
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);
      const fooOrg = orgMultiSelect.find('.nx-checkbox__input').at(1);
      fooOrg.simulate('change');

      const newSelectedOrganizations = new Set(['fooOrg']);
      const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);

      expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
    });
  });

  describe('when an org is toggled to be deselected', function () {
    describe('when all related apps are selected', function () {
      it('deselects related apps', function () {
        const selectedOrganizations = new Set(['barOrg']);
        const selectedApplications = new Set(['fooApp2', 'barApp1', 'barApp2']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);
        const fooOrg = orgMultiSelect.find('.nx-checkbox__input').at(1);
        fooOrg.simulate('change');

        const newSelectedOrganizations = new Set(['barOrg', 'fooOrg']);
        const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

        expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
      });
    });
  });

  describe('when an org is not selected and was not toggled', function () {
    // this is to fix CLM-8852
    describe('when all related apps are selected', function () {
      it('does not deselect related apps', function () {
        const selectedOrganizations = new Set();
        const selectedApplications = new Set(['fooApp1', 'fooApp2']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const newSelectedOrganizations = new Set(['fooOrg']);
        const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2']);

        const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);
        const barOrg = orgMultiSelect.find('.nx-checkbox__input').at(1);
        barOrg.simulate('change');

        expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
      });
    });
  });

  describe('changing applications', function () {
    it('does not select an org when all related apps are selected', function () {
      const selectedApplications = new Set(['fooApp1']);
      const selectedOrganizations = new Set();
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const newSelectedApplications = new Set(['fooApp1', 'fooApp2']);
      const expectedSelectedOrganizations = new Set();

      const appMultiSelect = mountedComponent.find(NxStatefulCollapsibleMultiSelect).at(0);
      const fooApp2 = appMultiSelect.find('.nx-checkbox__input').at(2);
      fooApp2.simulate('change');

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });

    it('deselects an org if not all related apps are selected', function () {
      const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      const selectedOrganizations = new Set(['fooOrg', 'barOrg']);
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const newSelectedApplications = new Set(['fooApp1', 'barApp1', 'barApp2']);
      const expectedSelectedOrganizations = new Set(['barOrg']);

      const appMultiSelect = mountedComponent.find(NxStatefulCollapsibleMultiSelect).at(0);
      const fooApp2 = appMultiSelect.find('.nx-checkbox__input').at(2);
      fooApp2.simulate('change');

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });
  });

  it('does not render synthetic orgs', function () {
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
    mountedComponent = getMountedComponent({
      ...minimalProps,
      ownersMap: ownersMapWithSyntheticOrgs,
    });
    const orgMultiSelect = mountedComponent.find(NxCollapsibleMultiSelect).at(0);

    expect(orgMultiSelect.find('.nx-checkbox').length).toBe(4);
    const firstChild = orgMultiSelect.find('.nx-checkbox').at(1);
    const secondChild = orgMultiSelect.find('.nx-checkbox').at(2);
    const thirdChild = orgMultiSelect.find('.nx-checkbox').at(3);
    expect(firstChild.find('.iq-filter-children-icon')).not.toExist();
    expect(secondChild.find('.iq-filter-children-icon')).toExist();
    expect(thirdChild.find('.iq-filter-children-icon')).toExist();
    expect(secondChild.find('span').at(2)).toHaveStyle('paddingLeft', '0px');
    expect(thirdChild.find('span').at(2)).toHaveStyle('paddingLeft', '17px');
  });
});
