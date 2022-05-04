/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSlice';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getApplicableLabelsUrl, getLabelsUrl, getDeleteLabelsUrl } from 'MainRoot/util/CLMLocation';

describe('orgsAndPoliciesLabelsActions', () => {
  const fn = () => {};
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadLabels', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
        applicationPublicId: 'application',
      });
    });

    it('loads all labels successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getLabelsUrl('application', 'application')]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(actions.loadLabels()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabels/pending',
          'orgsAndPoliciesLabels/loadLabels/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadLabels request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getLabelsUrl('application', 'application')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadLabels()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabels/pending',
          'orgsAndPoliciesLabels/loadLabels/rejected',
        ]);
        done();
      });
    });
  });

  describe('goToCreateLabel', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterSlice').and.returnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
        },
      });
    });

    it('redirects to proper create label path', (done) => {
      store.dispatch(actions.goToCreateLabel()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/goToCreateLabel/pending',
          '@@reduxUiRouter/stateGo',
          'orgsAndPoliciesLabels/goToCreateLabel/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.create-label',
          params: {
            organizationId: 'organizationId',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('goToEditLabel', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterSlice').and.returnValue({
        currentState: {
          name: 'organization.what',
        },
        currentParams: {
          organizationId: 'organizationId',
        },
      });
    });

    it('redirects to proper edit label path', (done) => {
      store.dispatch(actions.goToEditLabel('labelId')).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/goToEditLabel/pending',
          '@@reduxUiRouter/stateGo',
          'orgsAndPoliciesLabels/goToEditLabel/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.label',
          params: {
            organizationId: 'organizationId',
            labelId: 'labelId',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('loadApplicableLabelsByOwner', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
        applicationPublicId: 'application',
      });
    });

    it('loads all applicable labels successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: Promise.resolve({
            data: {
              labelsByOwner: [],
            },
          }),
        },
      });

      store.dispatch(actions.loadApplicableLabelsByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if getApplicableLabelsUrl request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicableLabelsByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadApplicableLabels', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
        applicationPublicId: 'application',
      });
    });

    it('loads applicable labels successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: Promise.resolve({
            data: {
              labelsByOwner: [
                {
                  ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                  ownerType: 'APPLICATION',
                  ownerName: 'appname',
                  labels: [
                    {
                      color: 'light-green',
                      description: null,
                      id: 'ae63051b2e304c3bbabf94c2443b03fb',
                      label: 'n3',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                      ownerType: 'APPLICATION',
                    },
                  ],
                },
                {
                  ownerId: '6b365e8a8000449aa924f194a7ed0d22',
                  ownerType: 'APPLICATION',
                  ownerName: 'appname2',
                  labels: [
                    {
                      color: 'dark-green',
                      description: null,
                      id: 'ae63051b2e304c3bbabf94c2443b03fa',
                      label: 'n4',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d22',
                      ownerType: 'APPLICATION',
                    },
                  ],
                },
              ],
            },
          }),
        },
      });

      store.dispatch(actions.loadApplicableLabels()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadApplicableLabels/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabels/fulfilled',
        ]);

        expect(actions[3].payload).toEqual([
          {
            ownerId: '6b365e8a8000449aa924f194a7ed0d21',
            ownerType: 'APPLICATION',
            ownerName: 'appname',
            labels: [
              {
                color: 'light-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fb',
                label: 'n3',
                ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                ownerType: 'APPLICATION',
              },
            ],
            inherited: false,
          },
          {
            ownerId: '6b365e8a8000449aa924f194a7ed0d22',
            ownerType: 'APPLICATION',
            ownerName: 'appname2',
            labels: [
              {
                color: 'dark-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fa',
                label: 'n4',
                ownerId: '6b365e8a8000449aa924f194a7ed0d22',
                ownerType: 'APPLICATION',
              },
            ],
            inherited: true,
          },
        ]);

        done();
      });
    });

    it('dispatches rejected action if getApplicableLabelsUrl request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicableLabels()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadApplicableLabels/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/rejected',
          'orgsAndPoliciesLabels/loadApplicableLabels/rejected',
        ]);
        expect(actions[3].payload).toBe('something went wrong');

        done();
      });
    });
  });

  describe('loadLabelsEditor', () => {
    let selectLabelsIsEditModeSpy, selectPrevOwnerTypeSpy, selectPrevOwnerIdSpy;
    beforeEach(() => {
      spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
        applicationPublicId: 'application',
        labelId: 'labelId',
      });
      selectLabelsIsEditModeSpy = spyOn(labelsSelectors, 'selectLabelsIsEditMode').and.returnValue(true);
      selectPrevOwnerTypeSpy = spyOn(labelsSelectors, 'selectPrevOwnerType').and.returnValue('ownerType');
      selectPrevOwnerIdSpy = spyOn(labelsSelectors, 'selectPrevOwnerId').and.returnValue('ownerId');
    });

    it('loads all labels and applicable labels successfully in edit mode if current ownerType and ownerId are different', (done) => {
      mockAxiosCalls({
        get: {
          [getLabelsUrl('application', 'application')]: Promise.resolve({
            data: [
              {
                color: 'light-red',
                description: null,
                id: 'labelId',
                label: 'n1',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
              {
                color: 'light-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fb',
                label: 'n3',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
            ],
          }),
          [getApplicableLabelsUrl('application', 'application')]: Promise.resolve({
            data: {
              labelsByOwner: [
                {
                  ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                  ownerType: 'APPLICATION',
                  ownerName: 'appname',
                  labels: [
                    {
                      color: 'light-red',
                      description: null,
                      id: 'labelId',
                      label: 'n1',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                    {
                      color: 'light-green',
                      description: null,
                      id: 'ae63051b2e304c3bbabf94c2443b03fb',
                      label: 'n3',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                  ],
                },
              ],
            },
          }),
        },
      });

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application');
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application/applicable');

        const actions = store.getActions();
        expect(actions.length).toBe(10);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/loadLabels/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/setCurrentOwnerProps',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadLabels/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'orgsAndPoliciesLabels/loadLabelsEditor/fulfilled',
        ]);

        expect(actions[9].payload).toEqual({
          siblings: [
            {
              color: 'light-red',
              description: null,
              id: 'labelId',
              label: 'n1',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          ],
          currentLabel: {
            color: 'light-red',
            description: null,
            id: 'labelId',
            label: 'n1',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
        });

        done();
      });
    });

    it('loads only all labels successfully in edit mode if current ownerType and ownerId are same as previous', (done) => {
      selectPrevOwnerTypeSpy.and.returnValue('application');
      selectPrevOwnerIdSpy.and.returnValue('application');
      mockAxiosCalls({
        get: {
          [getLabelsUrl('application', 'application')]: Promise.resolve({
            data: [
              {
                color: 'light-red',
                description: null,
                id: 'labelId',
                label: 'n1',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
              {
                color: 'light-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fb',
                label: 'n3',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application');

        const actions = store.getActions();
        expect(actions.length).toBe(7);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/loadLabels/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'orgsAndPoliciesLabels/loadLabels/fulfilled',
          'orgsAndPoliciesLabels/loadLabelsEditor/fulfilled',
        ]);

        expect(actions[6].payload).toEqual({
          siblings: null,
          currentLabel: {
            color: 'light-red',
            description: null,
            id: 'labelId',
            label: 'n1',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
        });

        done();
      });
    });

    it('loads all labels and applicable labels successfully in edit mode with no match', (done) => {
      mockAxiosCalls({
        get: {
          [getLabelsUrl('application', 'application')]: Promise.resolve({
            data: [
              {
                color: 'light-red',
                description: null,
                id: 'some-random-id',
                label: 'n1',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
              {
                color: 'light-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fb',
                label: 'n3',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerType: 'APPLICATION',
              },
            ],
          }),
          [getApplicableLabelsUrl('application', 'application')]: Promise.resolve({
            data: {
              labelsByOwner: [
                {
                  ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                  ownerType: 'APPLICATION',
                  ownerName: 'appname',
                  labels: [
                    {
                      color: 'light-red',
                      description: null,
                      id: 'labelId',
                      label: 'n1',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                    {
                      color: 'light-green',
                      description: null,
                      id: 'ae63051b2e304c3bbabf94c2443b03fb',
                      label: 'n3',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                  ],
                },
              ],
            },
          }),
        },
      });

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application');
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application/applicable');

        const actions = store.getActions();
        expect(actions.length).toBe(10);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/loadLabels/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/setCurrentOwnerProps',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadLabels/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'orgsAndPoliciesLabels/loadLabelsEditor/rejected',
        ]);

        expect(actions[9].payload).toBe('Unable to locate label.');

        done();
      });
    });

    it('loads applicable labels successfully in create mode if ownerType and ownerId are different', (done) => {
      selectLabelsIsEditModeSpy.and.returnValue(false);
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: Promise.resolve({
            data: {
              labelsByOwner: [
                {
                  ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                  ownerType: 'APPLICATION',
                  ownerName: 'appname',
                  labels: [
                    {
                      color: 'light-red',
                      description: null,
                      id: 'labelId',
                      label: 'n1',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                    {
                      color: 'light-green',
                      description: null,
                      id: 'ae63051b2e304c3bbabf94c2443b03fb',
                      label: 'n3',
                      ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                      ownerType: 'APPLICATION',
                    },
                  ],
                },
              ],
            },
          }),
        },
      });

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application/applicable');

        const actions = store.getActions();
        expect(actions.length).toBe(8);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/setCurrentOwnerProps',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/fulfilled',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'orgsAndPoliciesLabels/loadLabelsEditor/fulfilled',
        ]);

        expect(actions[7].payload).toEqual({
          siblings: [
            {
              color: 'light-red',
              description: null,
              id: 'labelId',
              label: 'n1',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          ],
          currentLabel: undefined,
        });

        done();
      });
    });

    it('does not load applicable labels in create mode if current ownerType and ownerId are same as previous', (done) => {
      selectLabelsIsEditModeSpy.and.returnValue(false);
      selectPrevOwnerTypeSpy.and.returnValue('application');
      selectPrevOwnerIdSpy.and.returnValue('application');

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(5);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'orgsAndPoliciesLabels/loadLabelsEditor/fulfilled',
        ]);
        expect(actions[4].payload).toEqual({
          siblings: null,
          currentLabel: undefined,
        });

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      selectLabelsIsEditModeSpy.and.returnValue(false);
      mockAxiosCalls({
        get: {
          [getApplicableLabelsUrl('application', 'application')]: () => Promise.reject('oops, rejected'),
        },
      });

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/labels/application/application/applicable');

        const actions = store.getActions();
        expect(actions.length).toBe(8);

        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/loadLabelsEditor/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'orgsAndPoliciesLabels/setCurrentOwnerProps',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/pending',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwner/rejected',
          'orgsAndPoliciesLabels/loadApplicableLabelsByOwnerIfNeeded/rejected',
          'orgsAndPoliciesLabels/loadLabelsEditor/rejected',
        ]);
        expect(actions[5].payload).toBe('oops, rejected');

        done();
      });
    });
  });

  describe('saveLabel', () => {
    let orgsAndPoliciesSelectorsSpy, selectLabelsIsEditModeSpy;
    beforeEach(() => {
      orgsAndPoliciesSelectorsSpy = spyOn(labelsSelectors, 'selectLabelsCurrentLabel').and.returnValue({
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });

      selectLabelsIsEditModeSpy = spyOn(labelsSelectors, 'selectLabelsIsEditMode').and.returnValue(true);

      spyOn(routerSelectors, 'selectRouterSlice').and.returnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationPublicId: 'application',
          labelId: 'labelId',
        },
      });
    });

    it('updates label successfully', (done) => {
      mockAxiosCalls({
        put: {
          [getLabelsUrl('application', 'application', 'ae63051b2e304c3bbabf94c2443b03fb')]: Promise.resolve({
            data: {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          }),
        },
      });

      store.dispatch(actions.saveLabel({ setPristine: fn })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/api/v2/labels/application/application', {
          color: 'light-green',
          description: null,
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        });

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/saveLabel/pending',
          'orgsAndPoliciesLabels/saveLabel/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          label: {
            color: 'light-green',
            description: null,
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
          isEditMode: true,
        });

        done();
      });
    });

    it('saves label successfully', (done) => {
      selectLabelsIsEditModeSpy.and.returnValue(false);
      orgsAndPoliciesSelectorsSpy.and.returnValue({
        color: 'light-green',
        description: null,
        id: 'newId',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });

      mockAxiosCalls({
        post: {
          [getLabelsUrl('application', 'application', 'newId')]: Promise.resolve({
            data: {
              color: 'light-green',
              description: null,
              id: 'newId',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          }),
        },
      });

      store.dispatch(actions.saveLabel({ setPristine: fn })).then(() => {
        expect(axios.post).toHaveBeenCalledTimes(1);
        expect(axios.post).toHaveBeenCalledWith('/api/v2/labels/application/application', {
          color: 'light-green',
          description: null,
          id: 'newId',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        });

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/saveLabel/pending',
          'orgsAndPoliciesLabels/saveLabel/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          label: {
            color: 'light-green',
            description: null,
            id: 'newId',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
          isEditMode: false,
        });

        done();
      });
    });

    it('dispatches rejected action if save request fails', (done) => {
      mockAxiosCalls({
        put: {
          [getLabelsUrl('application', 'application', 'ae63051b2e304c3bbabf94c2443b03fb')]: () =>
            Promise.reject('could not save label'),
        },
      });

      store.dispatch(actions.saveLabel({ setPristine: fn })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/saveLabel/pending',
          'orgsAndPoliciesLabels/saveLabel/rejected',
        ]);

        expect(actions[1].payload).toEqual('could not save label');

        done();
      });
    });
  });

  describe('removeLabel', () => {
    beforeEach(() => {
      spyOn(labelsSelectors, 'selectLabelsCurrentLabel').and.returnValue({
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });
      spyOn(routerSelectors, 'selectRouterSlice').and.returnValue({
        currentState: {
          name: 'application.what',
        },
        currentParams: {
          applicationPublicId: 'application',
        },
      });
    });

    it('removes label successfully', (done) => {
      mockAxiosCalls({
        del: {
          [getDeleteLabelsUrl('application', 'application', 'ae63051b2e304c3bbabf94c2443b03fb')]: Promise.resolve(),
        },
      });

      store.dispatch(actions.removeLabel(fn)).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/v2/labels/application/application/ae63051b2e304c3bbabf94c2443b03fb'
        );

        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/removeLabel/pending',
          'orgsAndPoliciesLabels/resetIsDirty',
          'orgsAndPoliciesLabels/goToCreateLabel/pending',
          '@@reduxUiRouter/stateGo',
          'orgsAndPoliciesLabels/goToCreateLabel/fulfilled',
          'orgsAndPoliciesLabels/removeLabel/fulfilled',
        ]);

        expect(actions[5].payload).toBe('ae63051b2e304c3bbabf94c2443b03fb');

        done();
      });
    });

    it('dispatches rejected action if remove request fails', (done) => {
      mockAxiosCalls({
        del: {
          [getDeleteLabelsUrl('application', 'application', 'ae63051b2e304c3bbabf94c2443b03fb')]: () =>
            Promise.reject('could not remove label'),
        },
      });

      store.dispatch(actions.removeLabel(fn)).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/v2/labels/application/application/ae63051b2e304c3bbabf94c2443b03fb'
        );

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPoliciesLabels/removeLabel/pending',
          'orgsAndPoliciesLabels/removeLabel/rejected',
        ]);

        expect(actions[1].payload).toBe('could not remove label');

        done();
      });
    });
  });
});
