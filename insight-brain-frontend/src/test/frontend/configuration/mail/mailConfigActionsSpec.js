/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { save, del, sendTestEmail } from '../../../../main/frontend/configuration/mail/mailConfigActions';
import { getMailConfigUrl, getTestMailUrl } from '../../../../main/frontend/util/CLMLocation';

describe('mailConfigActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      mailConfigUrl = getMailConfigUrl(),
      serverData = {
        startTlsEnabled: true,
        sslEnabled: false,
        hostname: 'foo',
        username: 'test',
        systemEmail: 'foo@bar.com',
        port: 42,
        password: 'secret',
        passwordIsIncluded: true
      };

  let store, state;

  beforeEach(function() {
    state = {
      mailConfig: {
        formState: {
          startTlsEnabled: true,
          sslEnabled: false,
          hostname: {
            trimmedValue: 'foo'
          },
          username: {
            trimmedValue: 'test'
          },
          systemEmail: {
            trimmedValue: 'foo@bar.com'
          },
          port: {
            trimmedValue: 42
          },
          password: {
            value: 'secret'
          },
          testEmail: {
            trimmedValue: 'test@test.com'
          }
        }
      }
    };

    store = SpecUtil.mockReduxStore(state);
  });

  describe('save', function() {

    afterEach(function() {
      expect(axios.put).toHaveBeenCalledWith(mailConfigUrl, serverData);
    });

    it('immediately dispatches a MAIL_CONFIG_SAVE_REQUESTED action', function() {
      mockAxiosCalls({
        put: {
          [mailConfigUrl]: Promise.resolve({})
        }
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('MAIL_CONFIG_SAVE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          put: {
            [mailConfigUrl]: Promise.resolve({})
          }
        });
      });

      it('dispatches MAIL_CONFIG_SAVE_FULFILLED', function(done) {

        store.dispatch(save())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_SAVE_FULFILLED');
              expect(actions[1].payload).toEqual(serverData);
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', function(done) {

        store.dispatch(save())
            .then(() => {

              setTimeout(function() {
                actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE');

                done();
              }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          put: {
            [mailConfigUrl]: Promise.reject('error!')
          }
        });
      });

      it('dispatches MAIL_CONFIG_SAVE_FAILED action', function(done) {
        store.dispatch(save())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_SAVE_FAILED');
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('del', function() {

    afterEach(function() {
      expect(axios.delete).toHaveBeenCalledWith(mailConfigUrl);
    });

    it('immediately dispatches a MAIL_CONFIG_DELETE_REQUESTED action', function() {
      mockAxiosCalls({
        del: {
          [mailConfigUrl]: Promise.resolve({})
        }
      });

      store.dispatch(del());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('MAIL_CONFIG_DELETE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful DELETE call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          del: {
            [mailConfigUrl]: Promise.resolve({})
          }
        });
      });

      it('dispatches MAIL_CONFIG_DELETE_FULFILLED', function(done) {

        store.dispatch(del())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_DELETE_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', function(done) {

        store.dispatch(del())
            .then(() => {
              setTimeout(function() {
                actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE');

                done();
              }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed DELETE call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          del: {
            [mailConfigUrl]: Promise.reject('error!')
          }
        });
      });

      it('dispatches MAIL_CONFIG_DELETE_FAILED action', function(done) {
        store.dispatch(del())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_DELETE_FAILED');
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('sendTestEmail', function() {

    const testMailUrl = getTestMailUrl('test@test.com');

    afterEach(function() {
      expect(axios.post).toHaveBeenCalledWith(testMailUrl, serverData);
    });

    it('immediately dispatches a MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED action', function() {
      mockAxiosCalls({
        post: {
          [testMailUrl]: Promise.resolve({})
        }
      });

      store.dispatch(sendTestEmail());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('MAIL_CONFIG_SEND_TEST_MAIL_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful POST call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          post: {
            [testMailUrl]: Promise.resolve({})
          }
        });
      });

      it('dispatches MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED', function(done) {

        store.dispatch(sendTestEmail())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_SEND_TEST_MAIL_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE after timeout', function(done) {

        store.dispatch(sendTestEmail())
            .then(() => {
              setTimeout(function() {
                actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('MAIL_CONFIG_SUBMIT_MASK_TIMER_DONE');

                done();
              }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed POST call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          post: {
            [testMailUrl]: Promise.reject('error!')
          }
        });
      });

      it('dispatches MAIL_CONFIG_SEND_TEST_MAIL_FAILED action', function(done) {
        store.dispatch(sendTestEmail())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('MAIL_CONFIG_SEND_TEST_MAIL_FAILED');
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
