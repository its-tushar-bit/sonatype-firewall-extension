/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED,
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_LICENSES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  deleteLicenses,
  loadLicenseModalInformation,
  saveLicenseFiles,
  saveLicenses,
  saveNotices,
} from '../../../../main/frontend/legal/files/advancedLegalFileActions';
import {
  getLegalFileUrl,
  getLicenseLegalComponentUrl,
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseOverrideUrl,
  getLicenseOverrideLegalReviewerUrl,
  getLicensesWithSyntheticFilterUrl,
  getOwnerHierarchyLegalReviewerUrl,
  getSaveLegalFileUrl,
  getDeleteLicenseOverrideUrl,
} from '../../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED,
} from '../../../../main/frontend/legal/advancedLegalActions';

import 'TestRoot/SpecUtil';

describe('advancedLegalFileActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('loadLicenseModalInformation', function () {
    let store, initialState;
    const ownerType = 'application';
    const ownerId = 'ownerId';
    const componentIdentifier = 'componentIdentifier123';

    beforeEach(function () {
      initialState = {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {},
            },
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              {
                id: 'ROOT_ORGANIZATION_ID',
                publicId: 'ROOT_ORGANIZATION_ID',
                type: 'organization',
              },
            ],
          },
        },
      };
    });

    it('correctly loads the data', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const overrideData = { data: { licenseOverridesByOwner: 'testLicenseData' } };
      const licenseData = {
        data: [
          {
            id: 'id',
          },
        ],
      };
      mockAxiosCalls({
        get: {
          [getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier)]: Promise.resolve(overrideData),
          [getLicensesWithSyntheticFilterUrl()]: Promise.resolve(licenseData),
        },
      });
      jest.useFakeTimers();

      store.dispatch(loadLicenseModalInformation({ ownerType, ownerId, componentIdentifier })).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledWith(
          '/api/v2/licenseOverrides/application/ownerId?componentIdentifier=componentIdentifier123'
        );
        expect(axios.get).toHaveBeenCalledWith('/rest/license?filterSynthetic=true');
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED);
        expect(actions[0].payload).toEqual(['id']);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED);
        expect(actions[1].payload).toEqual('testLicenseData');
        done();
      });
    });
  });

  describe('saveLicenses', function () {
    let store, initialState;
    const ownerType = 'application';
    const ownerId = 'ownerId';
    const postBody = {};
    const hash = 'hash123';
    const closeModalFn = jest.fn();

    beforeEach(function () {
      initialState = {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {},
            },
          },
          editLicensesForm: {
            comment: { value: 'comment' },
            status: 'status',
            scope: { ownerType: 'application', ownerId: 'ownerId' },
            licenseIds: [],
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              {
                id: 'ROOT_ORGANIZATION_ID',
                publicId: 'ROOT_ORGANIZATION_ID',
                type: 'organization',
              },
            ],
          },
        },
        router: {
          currentParams: {
            hash,
          },
        },
      };

      jest.useFakeTimers();
    });

    afterEach(() => jest.useRealTimers());

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED actions on success for hash', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getLicenseOverrideUrl(ownerType, ownerId)]: Promise.resolve({ data: 'postData' }),
        },
        get: {
          [getOwnerHierarchyLegalReviewerUrl('application', 'app')]: Promise.resolve({ data: 'getData' }),
          [getLicenseLegalComponentUrl('application', 'app', hash)]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getLicensesWithSyntheticFilterUrl()]: Promise.resolve({ data: 'getData3' }),
          [getLicenseOverrideLegalReviewerUrl(ownerType, ownerId, 'componentIdentifier')]: Promise.resolve({
            data: 'getData4',
          }),
        },
      });
      store.dispatch(saveLicenses({ ownerType, ownerId, postBody, hash, closeModalFn })).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        const expectedPostBody = {
          id: null,
          licenseIds: [],
          componentIdentifier: 'componentIdentifier',
          status: 'status',
          comment: 'comment',
          ownerId,
        };
        expect(axios.post).toHaveBeenCalledWith('/api/v2/licenseOverrides/application/ownerId', expectedPostBody);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/licenseLegalMetadata/application/app/component?hash=hash123');
        expect(axios.get).toHaveBeenCalledWith('/rest/owner/application/app/hierarchy/legalReviewer');
        expect(actions.length).toBe(7);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
        expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
        expect(actions[4].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
        expect(actions[6].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED actions on success for ComponentIdentifier', function (done) {
      const componentIdentifier = 'componentIdentifier';
      initialState.advancedLegal.availableScopes = {
        values: [
          {
            id: 'ROOT_ORGANIZATION_ID',
            publicId: 'ROOT_ORGANIZATION_ID',
            type: 'organization',
          },
        ],
      };
      initialState.router.currentParams = {};
      (initialState.advancedLegal.editLicensesForm.scope = {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
      }),
        (store = SpecUtil.mockReduxStore(initialState));
      mockAxiosCalls({
        post: {
          [getLicenseOverrideUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'postData' }),
        },
        get: {
          [getOwnerHierarchyLegalReviewerUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: 'getData',
          }),
          [getLicenseLegalComponentByComponentIdentifierUrl('"componentIdentifier"')]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
        },
      });
      store
        .dispatch(
          saveLicenses({
            ownerType: 'organization',
            ownerId: 'ROOT_ORGANIZATION_ID',
            postBody,
            componentIdentifier,
            closeModalFn,
          })
        )
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          const expectedPostBody = {
            id: null,
            licenseIds: [],
            componentIdentifier: 'componentIdentifier',
            status: 'status',
            comment: 'comment',
            ownerId: 'ROOT_ORGANIZATION_ID',
          };
          expect(axios.post).toHaveBeenCalledWith(
            '/api/v2/licenseOverrides/organization/ROOT_ORGANIZATION_ID',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/v2/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component?componentIdentifier=%22componentIdentifier%22'
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/rest/owner/organization/ROOT_ORGANIZATION_ID/hierarchy/legalReviewer'
          );
          expect(actions.length).toBe(7);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
          expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
          expect(actions[4].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
          expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
          expect(actions[6].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_FAILED action on save failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getLicenseOverrideUrl('application', 'ownerId')]: () => Promise.reject('error'),
        },
      });
      store.dispatch(saveLicenses({ ownerType, ownerId, postBody, hash })).catch(() => {
        const actions = store.getActions();
        const expectedPostBody = {
          id: null,
          licenseIds: [],
          componentIdentifier: 'componentIdentifier',
          status: 'status',
          comment: 'comment',
          ownerId: 'ownerId',
        };
        expect(axios.post).toHaveBeenCalledWith('/api/v2/licenseOverrides/application/ownerId', expectedPostBody);
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });
  });

  describe('deleteLicenses', function () {
    let store, initialState;
    const ownerType = 'application';
    const ownerId = 'ownerId';
    const licenseOverrideId = 'licenseOverrideId';
    const hash = 'hash123';
    const closeModalFn = jest.fn();

    beforeEach(function () {
      initialState = {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {},
            },
          },
          editLicensesForm: {
            comment: { value: 'comment' },
            status: 'status',
            scope: { ownerType: 'application', ownerId: 'ownerId', licenseOverride: { id: licenseOverrideId } },
            licenseIds: [],
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              {
                id: 'ROOT_ORGANIZATION_ID',
                publicId: 'ROOT_ORGANIZATION_ID',
                type: 'organization',
              },
            ],
          },
        },
        router: {
          currentParams: {
            hash,
          },
        },
      };

      jest.useFakeTimers();
    });

    afterEach(() => jest.useRealTimers());

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED actions on success for hash', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)]: Promise.resolve({}),
        },
        get: {
          [getOwnerHierarchyLegalReviewerUrl('application', 'app')]: Promise.resolve({ data: 'getData' }),
          [getLicenseLegalComponentUrl('application', 'app', hash)]: Promise.resolve({
            data: { component: { componentIdentifier: 'componentIdentifier' } },
          }),
          [getLicensesWithSyntheticFilterUrl()]: Promise.resolve({ data: 'getData3' }),
          [getLicenseOverrideLegalReviewerUrl(ownerType, ownerId, 'componentIdentifier')]: Promise.resolve({
            data: 'getData4',
          }),
        },
      });
      store.dispatch(deleteLicenses({ ownerType, ownerId, licenseOverrideId, hash, closeModalFn })).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(axios.delete).toHaveBeenCalledWith('/api/v2/licenseOverrides/application/ownerId/licenseOverrideId');
        expect(axios.get).toHaveBeenCalledWith('/api/v2/licenseLegalMetadata/application/app/component?hash=hash123');
        expect(axios.get).toHaveBeenCalledWith('/rest/owner/application/app/hierarchy/legalReviewer');
        expect(actions.length).toBe(7);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
        expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
        expect(actions[4].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[5].type).toBe(ADVANCED_LEGAL_LOAD_MULTI_LICENSES_REQUESTED);
        expect(actions[6].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_FAILED action on save failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)]: Promise.reject('error'),
        },
      });
      store.dispatch(deleteLicenses({ ownerType, ownerId, hash })).catch(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith('/api/v2/licenseOverrides/application/ownerId/licenseOverrideId');
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });
  });

  describe('saveNotices', function () {
    let store, initialState;

    const createState = (
      componentNoticesId,
      originalComponentNoticesScopeOwnerId,
      componentNoticesScopeOwnerId,
      noticeFiles
    ) => {
      return {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {
                componentNoticesId,
                originalComponentNoticesScopeOwnerId,
                componentNoticesScopeOwnerId,
                noticeFiles,
              },
            },
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              {
                id: 'ROOT_ORGANIZATION_ID',
                publicId: 'ROOT_ORGANIZATION_ID',
                type: 'organization',
              },
            ],
          },
        },
      };
    };

    beforeEach(function () {
      initialState = createState(null, 'ROOT_ORGANIZATION_ID', 'ROOT_ORGANIZATION_ID', []);
    });

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveNotices({ isNoticesDirty: true }));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('does not dispatch anything when not dirty', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveNotices({ isNoticesDirty: false }));
      const actions = store.getActions();
      expect(actions.length).toBe(0);
    });

    it(
      'dispatches ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED and ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE actions on' +
        ' success',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        mockAxiosCalls({
          post: {
            [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'postData' }),
          },
          get: {
            [getLegalFileUrl('application', 'app', 'componentIdentifier', 'notice')]: Promise.resolve({
              data: 'getData',
            }),
          },
        });
        const expectedPostBody = {
          id: null,
          legalFileType: 'notice',
          componentIdentifier: 'componentIdentifier',
          legalFileOverrides: [],
        };
        jest.useFakeTimers();

        store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
              '?componentIdentifier=%22componentIdentifier%22&legalFileType=notice'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
          expect(actions[1].payload).toEqual('getData');
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE);
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
      }
    );

    it('sends the correct payload for saving at the same or higher scope', function (done) {
      initialState = createState('componentNoticesId', 'appId', 'appId', [
        {
          id: 'id1',
          originalContentHash: 'originalContentHash1',
          content: 'content1',
          status: 'enabled',
        },
        {
          id: null,
          originalContentHash: null,
          content: 'content2',
          status: 'disabled',
        },
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({
            data: 'postData',
          }),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'notice')]: Promise.resolve({
            data: 'getData',
          }),
        },
      });
      const expectedPostBody = {
        id: 'componentNoticesId',
        legalFileType: 'notice',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: 'id1',
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled',
          },
          {
            id: null,
            originalContentHash: null,
            content: 'content2',
            status: 'disabled',
          },
        ],
      };
      store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=notice'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('sends the correct payload for saving at a lower scope', function (done) {
      initialState = createState('componentNoticesId', 'orgId', 'appId', [
        {
          id: 'id1',
          originalContentHash: 'originalContentHash1',
          content: 'content1',
          status: 'enabled',
        },
        {
          id: null,
          originalContentHash: null,
          content: 'content2',
          status: 'disabled',
        },
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({
            data: 'postData',
          }),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'notice')]: Promise.resolve({
            data: 'getData',
          }),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'notice',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: null,
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled',
          },
          {
            id: null,
            originalContentHash: null,
            content: 'content2',
            status: 'disabled',
          },
        ],
      };
      store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=notice'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_NOTICES_FAILED action on save failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: () => Promise.reject('error'),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'notice',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [],
      };
      store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
          expectedPostBody
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_NOTICES_FAILED action on get failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'notice')]: () => Promise.reject('error'),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'notice',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [],
      };
      store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=notice'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });
  });

  describe('saveLicenseFiles', function () {
    let store, initialState;

    const createState = (
      componentLicensesId,
      originalComponentLicensesScopeOwnerId,
      componentLicensesScopeOwnerId,
      licenseFiles
    ) => {
      return {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {
                componentLicensesId,
                originalComponentLicensesScopeOwnerId,
                componentLicensesScopeOwnerId,
                licenseFiles,
              },
            },
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              {
                id: 'ROOT_ORGANIZATION_ID',
                publicId: 'ROOT_ORGANIZATION_ID',
                type: 'organization',
              },
            ],
          },
        },
      };
    };

    beforeEach(function () {
      initialState = createState(null, 'ROOT_ORGANIZATION_ID', 'ROOT_ORGANIZATION_ID', []);
    });

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveLicenseFiles({ isLicensesDirty: true }));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
    });

    it('does not dispatch anything when not dirty', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveLicenseFiles({ isLicensesDirty: false }));
      const actions = store.getActions();
      expect(actions.length).toBe(0);
    });

    it(
      'dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED and ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE actions on' +
        ' success',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        mockAxiosCalls({
          post: {
            [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'postData' }),
          },
          get: {
            [getLegalFileUrl('application', 'app', 'componentIdentifier', 'license')]: Promise.resolve({
              data: 'getData',
            }),
          },
        });
        const expectedPostBody = {
          id: null,
          legalFileType: 'license',
          componentIdentifier: 'componentIdentifier',
          legalFileOverrides: [],
        };
        jest.useFakeTimers();

        store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
              '?componentIdentifier=%22componentIdentifier%22&legalFileType=license'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED);
          expect(actions[1].payload).toEqual('getData');
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE);
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
      }
    );

    it('sends the correct payload for saving at the same or higher scope', function (done) {
      initialState = createState('componentLicensesId', 'appId', 'appId', [
        {
          id: 'id1',
          originalContentHash: 'originalContentHash1',
          content: 'content1',
          status: 'enabled',
        },
        {
          id: null,
          originalContentHash: null,
          content: 'content2',
          status: 'disabled',
        },
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({
            data: 'postData',
          }),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'license')]: Promise.resolve({
            data: 'getData',
          }),
        },
      });
      const expectedPostBody = {
        id: 'componentLicensesId',
        legalFileType: 'license',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: 'id1',
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled',
          },
          {
            id: null,
            originalContentHash: null,
            content: 'content2',
            status: 'disabled',
          },
        ],
      };
      store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=license'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
    });

    it('sends the correct payload for saving at a lower scope', function (done) {
      initialState = createState('componentLicensesId', 'orgId', 'appId', [
        {
          id: 'id1',
          originalContentHash: 'originalContentHash1',
          content: 'content1',
          status: 'enabled',
        },
        {
          id: null,
          originalContentHash: null,
          content: 'content2',
          status: 'disabled',
        },
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({
            data: 'postData',
          }),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'license')]: Promise.resolve({
            data: 'getData',
          }),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'license',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: null,
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled',
          },
          {
            id: null,
            originalContentHash: null,
            content: 'content2',
            status: 'disabled',
          },
        ],
      };
      store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=license'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_FAILED action on save failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: () => Promise.reject('error'),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'license',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [],
      };
      store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
          expectedPostBody
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_FAILED action on get failure', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(),
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier', 'license')]: () => Promise.reject('error'),
        },
      });
      const expectedPostBody = {
        id: null,
        legalFileType: 'license',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [],
      };
      store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/application/app/component/legalFile' +
            '?componentIdentifier=%22componentIdentifier%22&legalFileType=license'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED);
    });
  });
});
