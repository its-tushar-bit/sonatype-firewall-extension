/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as ltgSelectors from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import 'TestRoot/SpecUtil';

import {
  getAllLicensesUrl,
  getLicenseGroupsUrl,
  getApplicableLicenseGroupsUrl,
  getDeleteLicenseGroupUrl,
  getLicenseGroupLicensesUrl,
} from 'MainRoot/util/CLMLocation';

describe('licenseThreatGroupActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  const licenseThreatGroupsByOwner = [
    {
      ownerId: '48951e9ed78946a6a5308420b5b533a8',
      ownerName: 'Development Inc',
      ownerType: 'organization',
      licenseThreatGroups: [
        {
          id: 'c1411f13f1e045959895a6b8686cd2df',
          name: 'Development Inc LTG1',
          threatLevel: '10',
          licenses: [
            {
              id: '33312eb53be24f199d55acee1db74621',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SAP-TOU',
            },
            {
              id: '71b5f83927944b56bfc6ab203232cf8b',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SATA',
            },
          ],
        },
        {
          id: 'a4a90b7407f44335a8a862973b96deb3',
          name: 'll',
          threatLevel: 5,
          licenses: [],
        },
      ],
    },
    {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      licenseThreatGroups: [
        {
          id: '4da8a978f07249289a690a47898eaa68',
          name: 'Banned',
          threatLevel: 10,
          licenses: [],
        },
      ],
    },
  ];
  const licenses = [
    {
      id: '0BSD',
      shortDisplayName: '0BSD',
      longDisplayName: 'BSD Zero Clause License',
    },
    {
      id: '10tec-Company-License-Agreement',
      shortDisplayName: '10tec-Company-License-Agreement',
      longDisplayName: '10tec Company License Agreement',
    },
    {
      id: '2KSYS-EULA',
      shortDisplayName: '2KSYS-EULA',
      longDisplayName: '2KSYS End User License Agreement',
    },
    {
      id: 'SAP-TOU',
      shortDisplayName: 'SAP-TOU',
      longDisplayName: 'SAP-TOU',
    },
    {
      id: 'SATA',
      shortDisplayName: 'SATA',
      longDisplayName: 'SATA',
    },
    {
      id: 'SautinSoft-Document-.Net-LA',
      shortDisplayName: 'SautinSoft-Document-.Net-LA',
      longDisplayName: 'SautinSoft-Document-.Net-LA',
    },
    {
      id: 'SautinSoft-Excel-to-PDF-.Net-LA',
      shortDisplayName: 'SautinSoft-Excel-to-PDF-.Net-LA',
      longDisplayName: 'SautinSoft-Excel-to-PDF-.Net-LA',
    },
  ];
  const licenseThreatGroup = {
    id: 'ltgId',
    name: 'new LTG',
    threatLevel: 8,
    licenses: [],
  };
  const nextLicenseThreatGroup = {
    id: 'nextLtgId',
    name: 'new LTG 2',
    threatLevel: 4,
    licenses: [],
  };

  beforeEach(() => {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'applicationId',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadLicenseThreatGroups', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'applicationId',
      });
    });

    it('loadLicenseThreatGroups successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroups()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroups/pending',
          'licenseThreatGroup/loadLicenseThreatGroups/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLicenseThreatGroups request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getLicenseGroupsUrl('application', 'applicationId')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroups()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroups/pending',
          'licenseThreatGroup/loadLicenseThreatGroups/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadApplicableLicenseThreatGroups', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'applicationId',
      });
    });

    it('loadApplicableLicenseThreatGroups successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadApplicableLicenseThreatGroups()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadApplicableLicenseThreatGroups request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicableLicenseThreatGroups()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/rejected',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadLicensesByLicenseThreatGroup', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'applicationId',
      });
    });

    it('loadLicensesByLicenseThreatGroup successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLicenseGroupsUrl('application', 'applicationId', 'ltgId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadLicensesByLicenseThreatGroup({ licenseThreatGroupId: 'ltgId' })).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicensesByLicenseThreatGroup/pending',
          'licenseThreatGroup/loadLicensesByLicenseThreatGroup/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLicensesByLicenseThreatGroup request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLicenseGroupsUrl('application', 'applicationId', 'ltgId')]: () =>
            Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadLicensesByLicenseThreatGroup({ licenseThreatGroupId: 'ltgId' })).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicensesByLicenseThreatGroup/pending',
          'licenseThreatGroup/loadLicensesByLicenseThreatGroup/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadAllLicenses', () => {
    it('loadAllLicenses successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: () => Promise.resolve({ data: licenses }),
        },
      });

      store.dispatch(actions.loadAllLicenses()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadAllLicenses/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadAllLicenses request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadAllLicenses()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadAllLicenses/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadLicenseThreatGroupEditor', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
          licenseThreatGroupId: 'licenseThreatGroupId',
        },
      });
    });

    it('loadLicenseThreatGroupEditor successfully, and calls loadAllLicenses ', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: Promise.resolve({
            data: licenses,
          }),
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadAllLicenses/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/fulfilled',
        ]);
        done();
      });
    });

    it('loadLicenseThreatGroupEditor successfully, and does not call loadAllLicenses ', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue(licenses);
      mockAxiosCalls({
        get: {
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(7);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLicenseThreatGroupEditor - getAllLicenses request fails', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: () => Promise.reject('something went wrong'),
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadAllLicenses/rejected',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/rejected',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLicenseThreatGroupEditor - getApplicableLicenseThreatGroups request fails', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: Promise.resolve({ data: licenses }),
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadAllLicenses/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/rejected',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/rejected',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/rejected',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLicenseThreatGroupEditor - both requests fails', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: () => Promise.reject('something went wrong'),
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadAllLicenses/rejected',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/rejected',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/rejected',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/rejected',
        ]);
        done();
      });
    });

    it('dispatches rejected action if current LTG is null while on edit mode', (done) => {
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupId').mockReturnValue('');
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(true);
      mockAxiosCalls({
        get: {
          [getAllLicensesUrl()]: Promise.resolve({
            data: licenses,
          }),
          [getApplicableLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              licenseThreatGroupsByOwner: licenseThreatGroupsByOwner,
            },
          }),
        },
      });

      store.dispatch(actions.loadLicenseThreatGroupEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/loadAllLicenses/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/pending',
          'licenseThreatGroup/loadAllLicenses/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroupsByOwner/fulfilled',
          'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
          'licenseThreatGroup/loadLicenseThreatGroupEditor/rejected',
        ]);
        done();
      });
    });
  });

  describe('goToCreateLTG', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
        },
      });
    });

    it('redirects to proper create LTG path', (done) => {
      store.dispatch(actions.goToCreateLTG()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/goToCreateLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToCreateLTG/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.create-license-threat-group',
          params: {
            organizationId: 'organizationId',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('goToEditLTG', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
          licenseThreatGroupId: 'e9ef670402be4cb49ec18ddea7a94a23',
        },
      });
    });

    it('redirects to proper edit LTG path', (done) => {
      store.dispatch(actions.goToEditLTG('e9ef670402be4cb49ec18ddea7a94a23')).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/goToEditLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToEditLTG/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.edit-license-threat-group',
          params: {
            organizationId: 'organizationId',
            licenseThreatGroupId: 'e9ef670402be4cb49ec18ddea7a94a23',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('goToNextLTG', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
          licenseThreatGroupId: 'e9ef670402be4cb49ec18ddea7a94a23',
        },
      });

      jest.spyOn(ltgSelectors, 'selectNextLicenseThreatGroup').mockReturnValue({
        id: 'e9ef670402be4cb49ec18ddea7a94a23',
      });
    });

    it('redirects to proper edit LTG path', (done) => {
      store.dispatch(actions.goToNextLTG()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/goToNextLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToNextLTG/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.edit-license-threat-group',
          params: {
            organizationId: 'organizationId',
            licenseThreatGroupId: 'e9ef670402be4cb49ec18ddea7a94a23',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('goToViewApplication', () => {
    it('redirects to organization management view', (done) => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
        },
      });

      store.dispatch(actions.goToViewManagement()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/goToViewManagementFromLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToViewManagementFromLTG/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.view.organization',
          params: {
            organizationId: 'organizationId',
          },
          options: undefined,
        });

        done();
      });
    });

    it('redirects to application management view', (done) => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationPublicId: 'applicationId',
        },
      });

      store.dispatch(actions.goToViewManagement()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/goToViewManagementFromLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToViewManagementFromLTG/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.view.application',
          params: {
            applicationPublicId: 'applicationId',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('removeLicenseThreatGroup', () => {
    beforeEach(() => {
      jest.spyOn(ltgSelectors, 'selectCurrentLicenseThreatGroup').mockReturnValue(licenseThreatGroup);
    });

    it('removeLicenseThreatGroup is successfully and goes to application management', (done) => {
      jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationPublicId: 'applicationId',
          licenseThreatGroupId: licenseThreatGroup.id,
        },
      });
      jest.spyOn(ltgSelectors, 'selectNextLicenseThreatGroup').mockReturnValue(null);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseGroupUrl('application', 'applicationId', licenseThreatGroup.id)]: Promise.resolve(),
        },
      });
      store.dispatch(actions.removeLicenseThreatGroup()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId/ltgId');

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/deleteLicenseThreatGroup/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/goToViewManagementFromLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToViewManagementFromLTG/fulfilled',
          'licenseThreatGroup/deleteLicenseThreatGroup/fulfilled',
        ]);

        expect(actions[5].payload).toEqual({ id: licenseThreatGroup.id });

        done();
      });
    });

    it('removeLicenseThreatGroup is successfully and goes to create LTG', (done) => {
      jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
          licenseThreatGroupId: licenseThreatGroup.id,
        },
      });
      jest.spyOn(orgAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
        ownerId: 'organizationId',
        ownerType: 'organization',
      });
      jest.spyOn(ltgSelectors, 'selectNextLicenseThreatGroup').mockReturnValue(null);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseGroupUrl('organization', 'organizationId', 'ltgId')]: Promise.resolve(),
        },
      });
      store.dispatch(actions.removeLicenseThreatGroup()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/licenseThreatGroup/organization/organizationId/ltgId');

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/deleteLicenseThreatGroup/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/goToCreateLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToCreateLTG/fulfilled',
          'licenseThreatGroup/deleteLicenseThreatGroup/fulfilled',
        ]);

        expect(actions[5].payload).toEqual({ id: licenseThreatGroup.id });

        done();
      });
    });

    it('removeLicenseThreatGroup is successfully and goes to next LTG', (done) => {
      jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationId: 'applicationId',
          applicationPublicId: 'applicationId',
          licenseThreatGroupId: licenseThreatGroup.id,
        },
      });
      jest.spyOn(ltgSelectors, 'selectNextLicenseThreatGroup').mockReturnValue(nextLicenseThreatGroup);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseGroupUrl('application', 'applicationId', 'ltgId')]: Promise.resolve(),
        },
      });
      store.dispatch(actions.removeLicenseThreatGroup()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId/ltgId');

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/deleteLicenseThreatGroup/pending',
          'licenseThreatGroup/resetIsDirty',
          'licenseThreatGroup/goToNextLTG/pending',
          '@@reduxUiRouter/stateGo',
          'licenseThreatGroup/goToNextLTG/fulfilled',
          'licenseThreatGroup/deleteLicenseThreatGroup/fulfilled',
        ]);

        expect(actions[5].payload).toEqual({ id: licenseThreatGroup.id });

        done();
      });
    });

    it('dispatches rejected action if remove request fails', (done) => {
      jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(false);
      jest.spyOn(ltgSelectors, 'selectNextLicenseThreatGroup').mockReturnValue(null);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseGroupUrl('application', 'applicationId', 'ltgId')]: () =>
            Promise.reject('Could not remove LTG'),
        },
      });

      store.dispatch(actions.removeLicenseThreatGroup()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId/ltgId');

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/deleteLicenseThreatGroup/pending',
          'licenseThreatGroup/deleteLicenseThreatGroup/rejected',
        ]);
        expect(actions[1].payload).toBe('Could not remove LTG');

        done();
      });
    });
  });

  describe('saveLicenseThreatGroup', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationId: 'applicationId',
          applicationPublicId: 'applicationId',
        },
      });
    });

    it('Create a new LTG successfully', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(false);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: null,
        name: {
          trimmedValue: 'new LTG',
        },
        threatLevel: 8,
        licenseIds: ['0BSD'],
      });
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue(licenses);
      mockAxiosCalls({
        post: {
          [getLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              id: 'ltgId',
              ownerId: 'applicationId',
              name: 'new LTG',
              threatLevel: 8,
            },
          }),
        },
        put: {
          [getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')]: Promise.resolve({
            data: [
              {
                id: 'fc3850958ac84028bd96cc79b064ed5e',
                ownerId: 'applicationId',
                licenseThreatGroupId: 'ltgId',
                licenseId: '0BSD',
              },
            ],
          }),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.post).toHaveBeenCalledTimes(1);
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
        });

        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroupLicense/application/applicationId/ltgId', [
          '0BSD',
        ]);

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/fulfilled',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/pending',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/fulfilled',
          'licenseThreatGroup/saveLicenseThreatGroup/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if creation of new LTG fails', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(false);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: null,
        name: {
          trimmedValue: 'new LTG',
        },
        licenseIds: ['0BSD'],
        threatLevel: 8,
      });
      mockAxiosCalls({
        post: {
          [getLicenseGroupsUrl('application', 'applicationId', 'ltgId')]: () => Promise.reject('Could not remove LTG'),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.post).toHaveBeenCalledTimes(1);
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
        });

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/rejected',
          'licenseThreatGroup/saveLicenseThreatGroup/rejected',
        ]);
        done();
      });
    });

    it('dispatches rejected action if adding licenses fails when creating new LTG', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(false);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: null,
        name: {
          trimmedValue: 'new LTG',
        },
        licenseIds: ['0BSD'],
        threatLevel: 8,
      });
      mockAxiosCalls({
        post: {
          [getLicenseGroupsUrl('application', 'applicationId', 'ltgId')]: Promise.resolve({
            data: {
              id: 'ltgId',
              ownerId: 'applicationId',
              name: 'new LTG',
              threatLevel: 8,
            },
          }),
        },
        put: {
          [getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')]: () =>
            Promise.reject('Could not remove LTG'),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.post).toHaveBeenCalledTimes(1);
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
        });

        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroupLicense/application/applicationId/ltgId', [
          '0BSD',
        ]);

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/fulfilled',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/pending',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/rejected',
          'licenseThreatGroup/saveLicenseThreatGroup/rejected',
        ]);
        done();
      });
    });

    it('Update LTG successfully', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(true);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: 'ltgId',
        name: {
          trimmedValue: 'new LTG',
        },
        licenseIds: ['0BSD', '10tec-Company-License-Agreement'],
        threatLevel: 8,
      });
      jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue(licenses);
      mockAxiosCalls({
        put: {
          [getLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              id: 'ltgId',
              ownerId: 'applicationId',
              name: 'new LTG',
              threatLevel: 8,
            },
          }),
          [getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')]: Promise.resolve({
            data: [
              {
                id: 'fc3850958ac84028bd96cc79b064ed5e',
                ownerId: 'applicationId',
                licenseThreatGroupId: 'ltgId',
                licenseId: '0BSD',
              },
              {
                id: 'fc3850958ac84109876abf79b064ed5e',
                ownerId: 'applicationId',
                licenseThreatGroupId: 'ltgId',
                licenseId: '10tec-Company-License-Agreement',
              },
            ],
          }),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
          id: 'ltgId',
          ownerId: 'applicationId',
        });
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroupLicense/application/applicationId/ltgId', [
          '0BSD',
          '10tec-Company-License-Agreement',
        ]);

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/fulfilled',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/pending',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/fulfilled',
          'licenseThreatGroup/saveLicenseThreatGroup/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if update of LTG fails', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(true);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: 'ltgId',
        name: {
          trimmedValue: 'new LTG',
        },
        licenseIds: ['0BSD'],
        threatLevel: 8,
      });

      mockAxiosCalls({
        put: {
          [getLicenseGroupsUrl('application', 'applicationId', 'ltgId')]: () => Promise.reject('Could not remove LTG'),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
          id: 'ltgId',
          ownerId: 'applicationId',
        });

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/rejected',
          'licenseThreatGroup/saveLicenseThreatGroup/rejected',
        ]);
        done();
      });
    });

    it('dispatches rejected action if adding licenses fails when updating LTG', (done) => {
      jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode').mockReturnValue(true);
      jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
        id: 'ltgId',
        name: {
          trimmedValue: 'new LTG',
        },
        licenseIds: ['0BSD', '10tec-Company-License-Agreement'],
        threatLevel: 8,
      });
      mockAxiosCalls({
        put: {
          [getLicenseGroupsUrl('application', 'applicationId')]: Promise.resolve({
            data: {
              id: 'ltgId',
              ownerId: 'applicationId',
              name: 'new LTG',
              threatLevel: 8,
            },
          }),
          [getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')]: () =>
            Promise.reject('Could not remove LTG'),
        },
      });
      store.dispatch(actions.saveLicenseThreatGroup()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroup/application/applicationId', {
          name: 'new LTG',
          threatLevel: 8,
          id: 'ltgId',
          ownerId: 'applicationId',
        });
        expect(axios.put).toHaveBeenCalledWith('/rest/licenseThreatGroupLicense/application/applicationId/ltgId', [
          '0BSD',
          '10tec-Company-License-Agreement',
        ]);

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'licenseThreatGroup/saveLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/pending',
          'licenseThreatGroup/createUpdateLicenseThreatGroup/fulfilled',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/pending',
          'licenseThreatGroup/updateLicenseThreatGroupLicenses/rejected',
          'licenseThreatGroup/saveLicenseThreatGroup/rejected',
        ]);
        done();
      });
    });
  });
});
