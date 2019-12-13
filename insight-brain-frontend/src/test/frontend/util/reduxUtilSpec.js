/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as reduxUtil from '../../../main/frontend/util/reduxUtil';

describe('reduxUtil', function() {
  describe('noPayloadActionCreator', function() {
    it('returns a no-arg function that returns an object with the specified type property', function() {
      const actionCreator = reduxUtil.noPayloadActionCreator('FOO');

      expect(actionCreator).toEqual(jasmine.any(Function));
      expect(actionCreator()).toEqual({ type: 'FOO' });
    });
  });

  describe('payloadParamActionCreator', function() {
    it('returns a function that takes a single parameter which becomes the payload of its returned action', function() {
      const actionCreator = reduxUtil.payloadParamActionCreator('BAR');

      expect(actionCreator).toEqual(jasmine.any(Function));
      expect(actionCreator(12345)).toEqual({ type: 'BAR', payload: 12345 });
    });
  });

  describe('mappedPayloadParamActionCreator', function() {
    it('returns a function that takes a single parameter which is then run through the specified mapper function to' +
        'produce the payload of the returned action', function() {
      const actionCreator = reduxUtil.mappedPayloadParamActionCreator('BAZ', str => str.trim());

      expect(actionCreator).toEqual(jasmine.any(Function));
      expect(actionCreator('   abcdef\t')).toEqual({ type: 'BAZ', payload: 'abcdef' });
    });
  });
});
