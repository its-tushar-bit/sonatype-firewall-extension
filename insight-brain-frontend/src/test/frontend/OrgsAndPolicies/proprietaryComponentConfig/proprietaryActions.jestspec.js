/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import * as proprietarySelectors from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getProprietaryConfigUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('proprietaryActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'ownerId',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadProprietaryConfig', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'ownerId',
      });
    });

    it('load proprietary matchers successfully', (done) => {
      const proprietaryConfigByOwners = [
        {
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerName: 'dfgdf',
          ownerType: 'application',
          proprietaryConfig: {
            id: 'f977bcf69fcb464b84837f643d8f93b7',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            packages: ['first', 'second'],
            regexes: ['cuatro', 'cinco'],
          },
        },
        {
          ownerId: '982ed06c48264a82acf63c8a1220fd2c',
          ownerName: 'kmnll',
          ownerType: 'application',
          proprietaryConfig: {
            id: '67c61f8869614beb84f025c7136d9dda',
            ownerId: '982ed06c48264a82acf63c8a1220fd2c',
            packages: ['third'],
            regexes: [],
          },
        },
      ];

      mockAxiosCalls({
        get: {
          [getProprietaryConfigUrl('application', 'ownerId')]: Promise.resolve({
            data: {
              proprietaryConfigByOwners,
            },
          }),
        },
      });

      store.dispatch(actions.loadProprietaryConfig()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/proprietary/application/ownerId');
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'proprietary/loadProprietaryConfig/pending',
          'proprietary/loadProprietaryConfig/fulfilled',
        ]);
        expect(actions[1].payload).toEqual({
          proprietaryConfigs: proprietaryConfigByOwners,
          currentConfig: {
            id: 'f977bcf69fcb464b84837f643d8f93b7',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            packages: ['first', 'second'],
            regexes: ['cuatro', 'cinco'],
          },
          localMatchers: [
            {
              type: 'Package',
              matcher: 'first',
            },
            {
              type: 'Package',
              matcher: 'second',
            },
            {
              type: 'Regular Expression',
              matcher: 'cuatro',
            },
            {
              type: 'Regular Expression',
              matcher: 'cinco',
            },
          ],
        });

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getProprietaryConfigUrl('application', 'ownerId')]: () => Promise.reject('could not load matchers'),
        },
      });

      store.dispatch(actions.loadProprietaryConfig()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/proprietary/application/ownerId');
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'proprietary/loadProprietaryConfig/pending',
          'proprietary/loadProprietaryConfig/rejected',
        ]);
        expect(actions[1].payload).toBe('could not load matchers');

        done();
      });
    });
  });

  describe('saveProprietaryConfig', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'ownerId',
      });

      jest.spyOn(proprietarySelectors, 'selectCurrentConfigs').mockReturnValue({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro', 'cinco'],
      });
    });

    it('updates proprietary matchers successfully', (done) => {
      mockAxiosCalls({
        put: {
          [getProprietaryConfigUrl('application', 'ownerId')]: Promise.resolve({
            data: {
              id: 'f977bcf69fcb464b84837f643d8f93b7',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              packages: ['first', 'second'],
              regexes: ['cuatro', 'cinco'],
            },
          }),
        },
      });

      store.dispatch(actions.saveProprietaryConfig()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/proprietary/application/ownerId', {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro', 'cinco'],
        });

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'proprietary/saveProprietaryConfig/pending',
          'proprietary/saveProprietaryConfig/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro', 'cinco'],
        });

        done();
      });
    });

    it('dispatches rejected action if update request fails', (done) => {
      mockAxiosCalls({
        put: {
          [getProprietaryConfigUrl('application', 'ownerId')]: () => Promise.reject('could not update matchers'),
        },
      });

      store.dispatch(actions.saveProprietaryConfig()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'proprietary/saveProprietaryConfig/pending',
          'proprietary/saveProprietaryConfig/rejected',
        ]);

        expect(actions[1].payload).toEqual('could not update matchers');

        done();
      });
    });
  });
});
