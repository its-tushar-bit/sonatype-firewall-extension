/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as reduxUtil from '../../../main/frontend/util/reduxUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('reduxUtil', function () {
  describe('noPayloadActionCreator', function () {
    it('returns a no-arg function that returns an object with the specified type property', function () {
      const actionCreator = reduxUtil.noPayloadActionCreator('FOO');

      expect(actionCreator).toEqual(expect.any(Function));
      expect(actionCreator()).toEqual({ type: 'FOO' });
    });
  });

  describe('payloadParamActionCreator', function () {
    it('returns a function that takes a single parameter which becomes the payload of its returned action', function () {
      const actionCreator = reduxUtil.payloadParamActionCreator('BAR');

      expect(actionCreator).toEqual(expect.any(Function));
      expect(actionCreator(12345)).toEqual({ type: 'BAR', payload: 12345 });
    });
  });

  describe('mappedPayloadParamActionCreator', function () {
    it(
      'returns a function that takes a single parameter which is then run through the specified mapper function to' +
        'produce the payload of the returned action',
      function () {
        const actionCreator = reduxUtil.mappedPayloadParamActionCreator('BAZ', (str) => str.trim());

        expect(actionCreator).toEqual(expect.any(Function));
        expect(actionCreator('   abcdef\t')).toEqual({
          type: 'BAZ',
          payload: 'abcdef',
        });
      }
    );
  });
  describe('startSaveMaskSuccessTimer', function () {
    it('calls(dispatches) a function(promise) after certain timeout', function (done) {
      jest.useFakeTimers();

      const dispatch = (func) => func;
      let value = jest.fn().mockName('whatAmI');

      const action = () => value("I'm tester");

      reduxUtil.startSaveMaskSuccessTimer(dispatch, action);
      expect(value).not.toHaveBeenCalled();
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      expect(value).toHaveBeenCalledTimes(1);
      expect(value).toHaveBeenCalledWith("I'm tester");

      jest.useRealTimers();
      done();
    });

    it('calls(dispatches) a function as promise', function (done) {
      const dispatch = (func) => func;
      let value = jest.fn().mockName('whatAmI');

      const action = () => value("I'm tester");

      reduxUtil.startSaveMaskSuccessTimer(dispatch, action).then(() => {
        expect(value).toHaveBeenCalled();
        expect(value).toHaveBeenCalledTimes(1);
        expect(value).toHaveBeenCalledWith("I'm tester");
        done();
      });
    });
  });
});
