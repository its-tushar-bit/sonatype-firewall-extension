/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { actions } from '../../../../main/frontend/configuration/mail/mailConfigSlice';
import { getMailConfigUrl, getTestMailUrl } from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

const {
  save,
  del,
  sendTestEmail,
  resetForm,
  setHostname,
  setPort,
  setUsername,
  setPassword,
  setSslEnabled,
  setStartTlsEnabled,
  setSystemEmail,
  setTestEmail,
  setShowDeleteModal,
  submitMaskTimerDone,
} = actions;

describe('mailConfigSlice actions', function () {
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
      passwordIsIncluded: true,
    };

  let store, state;

  beforeEach(function () {
    state = {
      mailConfig: {
        formState: {
          startTlsEnabled: true,
          sslEnabled: false,
          hostname: {
            trimmedValue: 'foo',
          },
          username: {
            trimmedValue: 'test',
          },
          systemEmail: {
            trimmedValue: 'foo@bar.com',
          },
          port: {
            trimmedValue: 42,
          },
          password: {
            value: 'secret',
          },
          testEmail: {
            trimmedValue: 'test@test.com',
          },
        },
      },
    };

    jest.useFakeTimers();
    store = SpecUtil.mockReduxStore(state);
  });

  afterEach(() => jest.useRealTimers());

  describe('save', function () {
    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(mailConfigUrl, serverData);
    });

    it('immediately dispatches a mailConfig/save/pending action', function () {
      mockAxiosCalls({
        put: {
          [mailConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/save/pending');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [mailConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches mailConfig/save/fulfilled', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/save/fulfilled');
          expect(actions[1].payload).toEqual(serverData);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches mailConfig/submitMaskTimerDone after timeout', function (done) {
        store.dispatch(save()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('mailConfig/submitMaskTimerDone');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [mailConfigUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches mailConfig/save/rejected action', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/save/rejected');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('del', function () {
    afterEach(function () {
      expect(axios.delete).toHaveBeenCalledWith(mailConfigUrl);
    });

    it('immediately dispatches a mailConfig/delete/pending action', function () {
      mockAxiosCalls({
        del: {
          [mailConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(del());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/delete/pending');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful DELETE call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          del: {
            [mailConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches mailConfig/delete/fulfilled', function (done) {
        store.dispatch(del()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/delete/fulfilled');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches mailConfig/submitMaskTimerDone after timeout', function (done) {
        store.dispatch(del()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('mailConfig/submitMaskTimerDone');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed DELETE call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          del: {
            [mailConfigUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches mailConfig/delete/rejected action', function (done) {
        store.dispatch(del()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/delete/rejected');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('sendTestEmail', function () {
    const testMailUrl = getTestMailUrl('test@test.com');

    afterEach(function () {
      expect(axios.post).toHaveBeenCalledWith(testMailUrl, serverData);
    });

    it('immediately dispatches a mailConfig/sendTestEmail/pending action', function () {
      mockAxiosCalls({
        post: {
          [testMailUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(sendTestEmail());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/sendTestEmail/pending');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful POST call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          post: {
            [testMailUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches mailConfig/sendTestEmail/fulfilled', function (done) {
        store.dispatch(sendTestEmail()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/sendTestEmail/fulfilled');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches mailConfig/submitMaskTimerDone after timeout', function (done) {
        store.dispatch(sendTestEmail()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('mailConfig/submitMaskTimerDone');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed POST call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          post: {
            [testMailUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches mailConfig/sendTestEmail/rejected action', function (done) {
        store.dispatch(sendTestEmail()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('mailConfig/sendTestEmail/rejected');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('resetForm', function () {
    it('dispatches mailConfig/resetForm', function () {
      store.dispatch(resetForm());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/resetForm');
    });
  });

  describe('setHostname', function () {
    it('dispatches mailConfig/setHostname with the specified payload', function () {
      store.dispatch(setHostname('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setHostname');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setPort', function () {
    it('dispatches mailConfig/setPort with the specified payload', function () {
      store.dispatch(setPort('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setPort');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setUsername', function () {
    it('dispatches mailConfig/setUsername with the specified payload', function () {
      store.dispatch(setUsername('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setUsername');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setPassword', function () {
    it('dispatches mailConfig/setPassword with the specified payload', function () {
      store.dispatch(setPassword('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setPassword');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setSslEnabled', function () {
    it('dispatches mailConfig/setSslEnabled with the specified payload', function () {
      store.dispatch(setSslEnabled(true));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setSslEnabled');
      expect(actions[0].payload).toBe(true);
    });
  });

  describe('setStartTlsEnabled', function () {
    it('dispatches mailConfig/setStartTlsEnabled with the specified payload', function () {
      store.dispatch(setStartTlsEnabled(true));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setStartTlsEnabled');
      expect(actions[0].payload).toBe(true);
    });
  });

  describe('setSystemEmail', function () {
    it('dispatches mailConfig/setSystemEmail with the specified payload', function () {
      store.dispatch(setSystemEmail('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setSystemEmail');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setTestEmail', function () {
    it('dispatches mailConfig/setTestEmail with the specified payload', function () {
      store.dispatch(setTestEmail('foo'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setTestEmail');
      expect(actions[0].payload).toBe('foo');
    });
  });

  describe('setShowDeleteModal', function () {
    it('dispatches mailConfig/setShowDeleteModal with the specified payload', function () {
      store.dispatch(setShowDeleteModal(true));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/setShowDeleteModal');
      expect(actions[0].payload).toBe(true);
    });
  });

  describe('submitMaskTimerDone', function () {
    it('dispatches mailConfig/submitMaskTimerDone', function () {
      store.dispatch(submitMaskTimerDone());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('mailConfig/submitMaskTimerDone');
    });
  });
});
