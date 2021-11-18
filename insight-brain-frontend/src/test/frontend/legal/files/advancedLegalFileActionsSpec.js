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
  getLicensesWithSyntheticFilterUrl,
  getOwnerHierarchyUrl,
  getSaveLegalFileUrl,
} from '../../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
} from '../../../../main/frontend/legal/advancedLegalActions';

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
      jasmine.clock().install();

      store.dispatch(loadLicenseModalInformation({ ownerType, ownerId, componentIdentifier })).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jasmine.clock().uninstall();

        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledWith(
          '/rest/licenseOverride/application/ownerId?componentIdentifier=componentIdentifier123'
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
    const closeModalFn = jasmine.createSpy();

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

      jasmine.clock().install();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED actions on success for hash', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getLicenseOverrideUrl(ownerType, ownerId)]: Promise.resolve({ data: 'postData' }),
        },
        get: {
          [getOwnerHierarchyUrl('application', 'app')]: Promise.resolve({ data: 'getData' }),
          [getLicenseLegalComponentUrl('application', 'app', hash)]: Promise.resolve({ data: 'getData2' }),
        },
      });
      store.dispatch(saveLicenses({ ownerType, ownerId, postBody, hash, closeModalFn })).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseOverride/application/ownerId', postBody);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/licenseLegalMetadata/application/app/component?hash=hash123');
        expect(axios.get).toHaveBeenCalledWith('/rest/owner/application/app/hierarchy');
        expect(actions.length).toBe(4);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED actions on success for ComponentIdentifier', function (done) {
      const componentIdentifier = 'componentIdentifier-123';
      initialState.advancedLegal.availableScopes = {
        values: [
          {
            id: 'ROOT_ORGANIZATION_ID',
            publicId: 'ROOT_ORGANIZATION_ID',
            type: 'organization',
          },
        ],
      };
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getLicenseOverrideUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'postData' }),
        },
        get: {
          [getOwnerHierarchyUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'getData' }),
          [getLicenseLegalComponentByComponentIdentifierUrl(componentIdentifier)]: Promise.resolve({
            data: 'getData2',
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
          jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith('/rest/licenseOverride/organization/ROOT_ORGANIZATION_ID', postBody);
          expect(axios.get).toHaveBeenCalledWith(
            '/api/v2/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component?componentIdentifier=componentIdentifier-123'
          );
          expect(axios.get).toHaveBeenCalledWith('/rest/owner/organization/ROOT_ORGANIZATION_ID/hierarchy');
          expect(actions.length).toBe(4);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED);
          expect(actions[2].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
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
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseOverride/application/ownerId', postBody);
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
        jasmine.clock().install();

        store.dispatch(saveNotices({ isNoticesDirty: true })).then(() => {
          jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jasmine.clock().uninstall();

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
        jasmine.clock().install();

        store.dispatch(saveLicenseFiles({ isLicensesDirty: true })).then(() => {
          jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jasmine.clock().uninstall();

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
