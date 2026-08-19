/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  ORIGINAL_SOURCES_OVERRIDE_FAILED,
  ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED,
  ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED,
  ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE,
  saveOriginalSourcesOverride,
} from '../../../../main/frontend/legal/originalSources/originalSourcesFormActions';
import {
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
} from '../../../../main/frontend/legal/obligation/advancedLegalObligationActions.js';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getComponentCopyrightOverrideUrl,
  getSaveComponentOriginalSourcesOverrideUrl,
  getComponentObligationUrl,
  getSaveComponentObligationUrl,
  getLicenseLegalComponentUrl,
} from '../../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED,
  ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED,
} from 'MainRoot/legal/advancedLegalActions';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import 'TestRoot/SpecUtil';

describe('originalSourcesFormActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  beforeEach(function () {
    jest.spyOn(routerSelectors, 'selectOwnerInfo').mockReturnValue({
      ownerType: 'organization',
      ownerId: 'org',
    });
  });

  describe('save original sources override', function () {
    let store;
    let initialState = {
      router: {
        currentParams: {
          hash: 'componentHash',
        },
      },
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            hash: 'componentHash',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId',
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Required Disclosure of Original Source Code with Distribution',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            { id: 'org', publicId: 'org', type: 'organization' },
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
          ],
        },
      },
    };

    let sourceLinks = [
      {
        id: '1',
        content: 'Source 1',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'Source 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('immediately dispatches a ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(
        saveOriginalSourcesOverride({
          sources: sourceLinks,
          areSourcesDirty: true,
          isObligationDirty: false,
        })
      );

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    });

    it('does not dispatch anything when not dirty', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(
        saveOriginalSourcesOverride({
          sources: sourceLinks,
          areSourcesDirty: false,
          isObligationDirty: false,
        })
      );

      const actions = store.getActions();
      expect(actions.length).toBe(0);
    });

    it(
      'dispatches ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED & ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE when ' +
        'saveOriginalSourcesOverride succeeds',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          componentIdentifier: 'componentIdentifier',
          packageUrl: undefined,
          sourceLinkOverrides: [
            {
              id: '1',
              content: 'Source 1',
              originalContentHash: 'originalContentHash1',
              status: 'enabled',
            },
            {
              id: '2',
              content: 'Source 2',
              originalContentHash: 'originalContentHash2',
              status: 'disabled',
            },
          ],
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
              data: {
                data: 'dataPOST',
              },
            }),
            [getSaveComponentObligationUrl('organization', 'org')]: Promise.resolve({
              data: {
                data: 'dataPOST2',
              },
            }),
          },
          get: {
            [getComponentCopyrightOverrideUrl('organization', 'org', 'componentIdentifier')]: Promise.resolve({
              data: {
                componentCopyrightDTO: {
                  data: 'dataGET',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
                ownerId: 'realOwner',
              },
            }),
            [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
              data: { component: { componentIdentifier: 'componentIdentifier' } },
            }),
            [getComponentObligationUrl(
              'organization',
              'org',
              'componentIdentifier',
              'Inclusion of Copyright'
            )]: Promise.resolve({
              data: {
                id: 'id',
                comment: 'comment',
                status: 'OPEN',
                ownerId: 'realOwner',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            }),
          },
        });
        jest.useFakeTimers();

        store
          .dispatch(
            saveOriginalSourcesOverride({
              sources: sourceLinks,
              existingObligation: {
                name: 'Required Disclosure of Original Source Code with Distribution',
                status: 'FULFILLED',
              },
              areSourcesDirty: true,
              isObligationDirty: false,
            })
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            jest.useRealTimers();

            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/sourceLink',
              expectedPostBody
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/v2/licenseLegalMetadata/organization/org/component?hash=componentHash'
            );
            expect(actions.length).toBe(6);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
            expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
            expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
            expect(actions[4].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
            expect(actions[5].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE);
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
      }
    );

    it('does not save obligation when it is missing', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        packageUrl: undefined,
        componentIdentifier: 'componentIdentifier',
        sourceLinkOverrides: [
          {
            id: '1',
            content: 'Source 1',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Source 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl('organization', 'org', 'componentIdentifier')]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'realOwner',
            },
          }),
          [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
        },
      });
      jest.useFakeTimers();

      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources: sourceLinks,
            areSourcesDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/sourceLink',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/v2/licenseLegalMetadata/organization/org/component?hash=componentHash'
          );
          expect(actions.length).toBe(6);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
          expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
          expect(actions[4].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
          expect(actions[5].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE);
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    });

    it('dispatches an ORIGINAL_SOURCES_OVERRIDE_FAILED action when the API fails to save', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        packageUrl: undefined,
        componentIdentifier: 'componentIdentifier',
        sourceLinkOverrides: [
          {
            id: '1',
            content: 'Source 1',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Source 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: () => Promise.reject('error'),
        },
      });

      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources: sourceLinks,
            areSourcesDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/sourceLink',
            expectedPostBody
          );
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe(ORIGINAL_SOURCES_OVERRIDE_FAILED);
          expect(actions[2].payload).toBe('error');
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    });
  });

  describe('save original source override with obligation at different scope', function () {
    let store;
    let initialState = {
      router: {
        currentParams: {
          hash: 'componentHash',
        },
      },
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Required Disclosure of Original Source Code with Distribution',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'org',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
            { id: 'org', publicId: 'org', type: 'organization' },
            { id: 'app', publicId: 'app', type: 'application' },
          ],
        },
      },
    };

    let sourceLinks = [
      {
        id: '1',
        content: 'Source 1',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'Source 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('component obligation exists at appScope, change to root scope', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        componentIdentifier: 'componentIdentifier',
        packageUrl: undefined,
        sourceLinkOverrides: [
          {
            id: '1',
            content: 'Source 1',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Source 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };

      assertExpectedHighScopeCalls('org', 'organization', expectedPostBody, done);
    });

    function assertExpectedHighScopeCalls(persistedAtScope, orgOrApp, expectedPostBody, done) {
      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl(orgOrApp, persistedAtScope)]: Promise.resolve({
            data: {
              data: 'dataPOST',
            },
          }),
          [getSaveComponentObligationUrl('organization', persistedAtScope)]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getLicenseLegalComponentUrl(orgOrApp, persistedAtScope, 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getComponentObligationUrl(
            orgOrApp,
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Required Disclosure of Original Source Code with Distribution'
          )]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'comment',
              status: 'OPEN',
              ownerId: 'realOwner',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources: sourceLinks,
            existingObligation: {
              name: 'Required Disclosure of Original Source Code with Distribution',
              status: 'FULFILLED',
            },
            areSourcesDirty: true,
            isObligationDirty: true,
          })
        )
        .then(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + persistedAtScope + '/component/sourceLink',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/v2/licenseLegalMetadata/' + orgOrApp + '/' + persistedAtScope + '/component?hash=componentHash'
          );
          expect(actions.length).toBe(8);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
          expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
          expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
          expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
          expect(actions[6].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[7].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED);
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    }
  });

  describe('save original source override and obligation', function () {
    let store;
    const initialState = {
      router: {
        currentParams: {
          hash: 'componentHash',
        },
      },
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Required Disclosure of Original Source Code with Distribution',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
          ],
        },
      },
    };
    const sources = [
      {
        id: '1',
        content: 'source 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'source 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('dispatches the expected actions on success', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        componentIdentifier: 'componentIdentifier',
        packageUrl: undefined,
        sourceLinkOverrides: sources,
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST1',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier'
          )]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET1',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'ROOT_ORGANIZATION_ID',
            },
          }),
          [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getComponentObligationUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Required Disclosure of Original Source Code with Distribution'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Required Disclosure of Original Source Code with Distribution',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources,
            existingObligation: {
              name: 'Required Disclosure of Original Source Code with Distribution',
              status: 'FLAGGED',
            },
            areSourcesDirty: true,
            isObligationDirty: true,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/sourceLink',
              expectedPostBody
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/v2/licenseLegalMetadata/organization/org/component?hash=componentHash'
            );
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              {
                id: 'd387da0b87a9428fbc352f437c8294cf',
                componentIdentifier: 'componentIdentifier',
                name: 'Required Disclosure of Original Source Code with Distribution',
                comment: 'comment',
                status: 'FLAGGED',
              }
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID' +
                '/component/obligation?componentIdentifier=%22componentIdentifier%22' +
                '&obligationName=Required%20Disclosure%20of%20Original%20Source%20Code%20with%20Distribution'
            );
            expect(actions.length).toBe(9);
            expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
            expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
            expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
            expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
            expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
            expect(actions[6].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
            expect(actions[6].payload).toEqual({
              name: 'Required Disclosure of Original Source Code with Distribution',
              value: {
                id: 'd387da0b87a9428fbc352f437c8294cf',
                comment: 'comment',
                ownerId: 'ROOT_ORGANIZATION_ID',
                status: 'FLAGGED',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            });
            expect(actions[7].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED);
            expect(actions[8].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    });
  });

  describe('save obligation', function () {
    let store;
    const initialState = {
      router: {
        currentParams: {
          hash: 'componentHash',
        },
      },
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Required Disclosure of Original Source Code with Distribution',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
          ],
        },
      },
    };
    const sources = [
      {
        id: '1',
        content: 'source 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'source 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('dispatches the expected actions on success', function (done) {
      store = SpecUtil.mockReduxStore(initialState);

      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST1',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getComponentObligationUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Required Disclosure of Original Source Code with Distribution'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Required Disclosure of Original Source Code with Distribution',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources,
            existingObligation: {
              name: 'Required Disclosure of Original Source Code with Distribution',
              status: 'FLAGGED',
            },
            areSourcesDirty: false,
            isObligationDirty: true,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();

            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              {
                id: 'd387da0b87a9428fbc352f437c8294cf',
                componentIdentifier: 'componentIdentifier',
                name: 'Required Disclosure of Original Source Code with Distribution',
                comment: 'comment',
                status: 'FLAGGED',
              }
            );

            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID' +
                '/component/obligation?componentIdentifier=%22componentIdentifier%22' +
                '&obligationName=Required%20Disclosure%20of%20Original%20Source%20Code%20with%20Distribution'
            );
            expect(actions.length).toBe(5);
            expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
            expect(actions[3].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED);
            expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
    });

    it('dispatches the expected actions on failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);

      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST1',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error'),
        },
        get: {
          [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getComponentObligationUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Required Disclosure of Original Source Code with Distribution'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Required Disclosure of Original Source Code with Distribution',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources,
            existingObligation: {
              name: 'Required Disclosure of Original Source Code with Distribution',
              status: 'FLAGGED',
            },
            areSourcesDirty: false,
            isObligationDirty: true,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(4);
            expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
            expect(actions[3].type).toBe(ORIGINAL_SOURCES_OVERRIDE_FAILED);
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
    });

    it('dispatches the expected actions on failure (source + obligation)', function (done) {
      store = SpecUtil.mockReduxStore(initialState);

      mockAxiosCalls({
        post: {
          [getSaveComponentOriginalSourcesOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST1',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error'),
        },
        get: {
          [getLicenseLegalComponentUrl('organization', 'org', 'componentHash')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getComponentObligationUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Required Disclosure of Original Source Code with Distribution'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Required Disclosure of Original Source Code with Distribution',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveOriginalSourcesOverride({
            sources,
            existingObligation: {
              name: 'Required Disclosure of Original Source Code with Distribution',
              status: 'FLAGGED',
            },
            areSourcesDirty: true,
            isObligationDirty: true,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(8);
            expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
            expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
            expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
            expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
            expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_FULFILLED);
            expect(actions[6].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
            expect(actions[7].type).toBe(ORIGINAL_SOURCES_OVERRIDE_FAILED);
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED);
      expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
    });
  });
});
