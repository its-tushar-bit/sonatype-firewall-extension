/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getApplicableLabelsUrl, getLabelsUrl, getDeleteLabelsUrl } from 'MainRoot/util/CLMLocation';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('labelsActions', () => {
  const fn = () => {};
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    jest.useFakeTimers();
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  afterEach(function () {
    jest.useRealTimers();
  });

  describe('loadLabels', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
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
        expect(actions).toHaveActionTypesInOrder(['labels/loadLabels/pending', 'labels/loadLabels/fulfilled']);
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
        expect(actions).toHaveActionTypesInOrder(['labels/loadLabels/pending', 'labels/loadLabels/rejected']);
        done();
      });
    });
  });

  describe('goToCreateLabel', () => {
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

    it('redirects to proper create label path', (done) => {
      store.dispatch(actions.goToCreateLabel()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'labels/goToCreateLabel/pending',
          '@@reduxUiRouter/stateGo',
          'labels/goToCreateLabel/fulfilled',
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

  describe('loadApplicableLabelsByOwner', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
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
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/fulfilled',
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
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadApplicableLabels', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
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
          'labels/loadApplicableLabels/pending',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/fulfilled',
          'labels/loadApplicableLabels/fulfilled',
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
          'labels/loadApplicableLabels/pending',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/rejected',
          'labels/loadApplicableLabels/rejected',
        ]);
        expect(actions[3].payload).toBe('something went wrong');

        done();
      });
    });
  });

  describe('loadLabelsEditor', () => {
    let selectLabelsIsEditModeSpy, selectPrevOwnerTypeSpy, selectPrevOwnerIdSpy;
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'application',
        labelId: 'labelId',
      });
      selectLabelsIsEditModeSpy = jest.spyOn(labelsSelectors, 'selectLabelsIsEditMode').mockReturnValue(true);
      selectPrevOwnerTypeSpy = jest.spyOn(labelsSelectors, 'selectPrevOwnerType').mockReturnValue('ownerType');
      selectPrevOwnerIdSpy = jest.spyOn(labelsSelectors, 'selectPrevOwnerId').mockReturnValue('ownerId');
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
          'labels/loadLabelsEditor/pending',
          'labels/loadLabels/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/setCurrentOwnerProps',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadLabels/fulfilled',
          'labels/loadApplicableLabelsByOwner/fulfilled',
          'labels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'labels/loadLabelsEditor/fulfilled',
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
      selectPrevOwnerTypeSpy.mockReturnValue('application');
      selectPrevOwnerIdSpy.mockReturnValue('application');
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
          'labels/loadLabelsEditor/pending',
          'labels/loadLabels/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'labels/loadLabels/fulfilled',
          'labels/loadLabelsEditor/fulfilled',
        ]);

        expect(actions[6].payload).toEqual({
          siblings: [],
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
          'labels/loadLabelsEditor/pending',
          'labels/loadLabels/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/setCurrentOwnerProps',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadLabels/fulfilled',
          'labels/loadApplicableLabelsByOwner/fulfilled',
          'labels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'labels/loadLabelsEditor/rejected',
        ]);

        expect(actions[9].payload).toBe('Unable to locate label.');

        done();
      });
    });

    it('loads applicable labels successfully in create mode if ownerType and ownerId are different', (done) => {
      selectLabelsIsEditModeSpy.mockReturnValue(false);
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
          'labels/loadLabelsEditor/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/setCurrentOwnerProps',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/fulfilled',
          'labels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'labels/loadLabelsEditor/fulfilled',
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
          currentLabel: { color: 'light-purple', description: '', label: '' },
        });

        done();
      });
    });

    it('does not load applicable labels in create mode if current ownerType and ownerId are same as previous', (done) => {
      selectLabelsIsEditModeSpy.mockReturnValue(false);
      selectPrevOwnerTypeSpy.mockReturnValue('application');
      selectPrevOwnerIdSpy.mockReturnValue('application');

      store.dispatch(actions.loadLabelsEditor()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(5);

        expect(actions).toHaveActionTypesInOrder([
          'labels/loadLabelsEditor/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/loadApplicableLabelsByOwnerIfNeeded/fulfilled',
          'labels/loadLabelsEditor/fulfilled',
        ]);
        expect(actions[4].payload).toEqual({
          siblings: [],
          currentLabel: { color: 'light-purple', description: '', label: '' },
        });

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      selectLabelsIsEditModeSpy.mockReturnValue(false);
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
          'labels/loadLabelsEditor/pending',
          'labels/resetIsDirty',
          'labels/loadApplicableLabelsByOwnerIfNeeded/pending',
          'labels/setCurrentOwnerProps',
          'labels/loadApplicableLabelsByOwner/pending',
          'labels/loadApplicableLabelsByOwner/rejected',
          'labels/loadApplicableLabelsByOwnerIfNeeded/rejected',
          'labels/loadLabelsEditor/rejected',
        ]);
        expect(actions[5].payload).toBe('oops, rejected');

        done();
      });
    });
  });

  describe('saveLabel', () => {
    let orgsAndPoliciesSelectorsSpy, selectLabelsIsEditModeSpy;
    beforeEach(() => {
      orgsAndPoliciesSelectorsSpy = jest.spyOn(labelsSelectors, 'selectLabelsCurrentLabel').mockReturnValue({
        color: 'light-green',
        description: rscInitialState(''),
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: rscInitialState('n3'),
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });

      selectLabelsIsEditModeSpy = jest.spyOn(labelsSelectors, 'selectLabelsIsEditMode').mockReturnValue(true);

      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
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
              description: rscInitialState(''),
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: rscInitialState('n3'),
              ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              ownerType: 'APPLICATION',
            },
          }),
        },
      });

      store.dispatch(actions.saveLabel()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/api/v2/labels/application/application', {
          color: 'light-green',
          description: '',
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        });

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['labels/saveLabel/pending', 'labels/saveLabel/fulfilled']);

        expect(actions[1].payload).toEqual({
          label: {
            color: 'light-green',
            description: rscInitialState(''),
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: rscInitialState('n3'),
            ownerId: '6b365e8a8000449aa924f194a7ed0d27',
            ownerType: 'APPLICATION',
          },
          isEditMode: true,
        });

        done();
      });
    });

    it('saves label successfully', (done) => {
      selectLabelsIsEditModeSpy.mockReturnValue(false);
      orgsAndPoliciesSelectorsSpy.mockReturnValue({
        color: 'light-green',
        description: rscInitialState(''),
        id: 'newId',
        label: rscInitialState('n3'),
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });

      mockAxiosCalls({
        post: {
          [getLabelsUrl('application', 'application', 'newId')]: Promise.resolve({
            data: {
              color: 'light-green',
              description: rscInitialState(''),
              id: 'newId',
              label: rscInitialState('n3'),
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
          description: '',
          id: 'newId',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          ownerType: 'APPLICATION',
        });

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['labels/saveLabel/pending', 'labels/saveLabel/fulfilled']);

        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'labels/saveLabel/pending',
          'labels/saveLabel/fulfilled',
          'labels/saveMaskTimerDone',
        ]);

        expect(actions[1].payload).toEqual({
          label: {
            color: 'light-green',
            description: rscInitialState(''),
            id: 'newId',
            label: rscInitialState('n3'),
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
        expect(actions).toHaveActionTypesInOrder(['labels/saveLabel/pending', 'labels/saveLabel/rejected']);

        expect(actions[1].payload).toEqual('could not save label');

        done();
      });
    });
  });

  describe('removeLabel', () => {
    beforeEach(() => {
      jest.spyOn(labelsSelectors, 'selectLabelsCurrentLabel').mockReturnValue({
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerType: 'APPLICATION',
      });
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
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
          'labels/removeLabel/pending',
          'labels/resetIsDirty',
          'labels/goToCreateLabel/pending',
          '@@reduxUiRouter/stateGo',
          'labels/goToCreateLabel/fulfilled',
          'labels/removeLabel/fulfilled',
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
        expect(actions).toHaveActionTypesInOrder(['labels/removeLabel/pending', 'labels/removeLabel/rejected']);

        expect(actions[1].payload).toBe('could not remove label');

        done();
      });
    });
  });
});
