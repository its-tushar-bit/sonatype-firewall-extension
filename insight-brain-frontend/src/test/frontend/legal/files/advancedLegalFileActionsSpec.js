/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  saveNotices
} from '../../../../main/frontend/legal/files/advancedLegalFileActions';
import { getSaveLegalFileUrl, getLegalFileUrl } from '../../../../main/frontend/util/CLMLocation';

describe('advancedLegalFileActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('saveNotices', function() {
    let store,
        initialState;

    const createState = (componentLegalFileId,
                         originalComponentLegalFileScopeOwnerId,
                         componentLegalFileScopeOwnerId,
                         noticeFiles) => {
      return {
        advancedLegal: {
          component: {
            component: {
              componentIdentifier: 'componentIdentifier',
              licenseLegalData: {
                componentLegalFileId: componentLegalFileId,
                originalComponentLegalFileScopeOwnerId: originalComponentLegalFileScopeOwnerId,
                componentLegalFileScopeOwnerId: componentLegalFileScopeOwnerId,
                noticeFiles: noticeFiles
              }
            }
          },
          availableScopes: {
            values: [
              { id: 'appId', publicId: 'app', type: 'application' },
              { id: 'orgId', publicId: 'orgId', type: 'organization' },
              { id: 'ROOT_ORGANIZATION_ID', publicId: 'ROOT_ORGANIZATION_ID', type: 'organization' }
            ]
          }
        }
      };
    };

    beforeEach(function() {
      initialState = createState(null, 'ROOT_ORGANIZATION_ID', 'ROOT_ORGANIZATION_ID', []);
    });

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED action', function() {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveNotices());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED and ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE actions on' +
        ' success', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({ data: 'postData' })
        },
        get: {
          [getLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID', 'componentIdentifier')]: Promise.resolve(
              { data: 'getData' })
        }
      });
      const expectedPostBody = {
        id: null,
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: []
      };
      store.dispatch(saveNotices()).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
              expectedPostBody);
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
          expect(actions[1].payload).toEqual('getData');
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('sends the correct payload for saving at the same or higher scope', function(done) {
      initialState = createState('componentLegalFileId', 'appId', 'appId', [
        { id: 'id1', originalContentHash: 'originalContentHash1', content: 'content1', status: 'enabled' },
        { id: null, originalContentHash: null, content: 'content2', status: 'disabled' }
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({ data: 'postData' })
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier')]: Promise.resolve({ data: 'getData' })
        }
      });
      const expectedPostBody = {
        id: 'componentLegalFileId',
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: 'id1',
            legalFileType: 'notice',
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled'
          },
          {
            id: null,
            legalFileType: 'notice',
            originalContentHash: null,
            content: 'content2',
            status: 'disabled'
          }
        ]
      };
      store.dispatch(saveNotices()).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/application/app/component/legalFile', expectedPostBody);
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('sends the correct payload for saving at a lower scope', function(done) {
      initialState = createState('componentLegalFileId', 'orgId', 'appId', [
        { id: 'id1', originalContentHash: 'originalContentHash1', content: 'content1', status: 'enabled' },
        { id: null, originalContentHash: null, content: 'content2', status: 'disabled' }
      ]);
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('application', 'app')]: Promise.resolve({ data: 'postData' })
        },
        get: {
          [getLegalFileUrl('application', 'app', 'componentIdentifier')]: Promise.resolve({ data: 'getData' })
        }
      });
      const expectedPostBody = {
        id: null,
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: [
          {
            id: null,
            legalFileType: 'notice',
            originalContentHash: 'originalContentHash1',
            content: 'content1',
            status: 'enabled'
          },
          {
            id: null,
            legalFileType: 'notice',
            originalContentHash: null,
            content: 'content2',
            status: 'disabled'
          }
        ]
      };
      store.dispatch(saveNotices()).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/application/app/component/legalFile', expectedPostBody);
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED);
        expect(actions[1].payload).toEqual('getData');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_NOTICES_FAILED action on save failure', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error')
        }
      });
      const expectedPostBody = {
        id: null,
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: []
      };
      store.dispatch(saveNotices()).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
            expectedPostBody);
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_FAILED);
        expect(actions[1].payload).toEqual('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED);
    });

    it('dispatches ADVANCED_LEGAL_SAVE_NOTICES_FAILED action on get failure', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve()
        },
        get: {
          [getLegalFileUrl('organization', 'ROOT_ORGANIZATION_ID', 'componentIdentifier')]: Promise.reject('error')
        }
      });
      const expectedPostBody = {
        id: null,
        componentIdentifier: 'componentIdentifier',
        legalFileOverrides: []
      };
      store.dispatch(saveNotices()).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/legalFile',
            expectedPostBody);
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
});
