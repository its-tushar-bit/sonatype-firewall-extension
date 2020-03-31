/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getViolationDetailsUrl } from '../../../main/frontend/util/CLMLocation';
import { loadViolation, LOAD_VIOLATION_REQUESTED, LOAD_VIOLATION_FULFILLED, LOAD_VIOLATION_FAILED }
  from '../../../main/frontend/violation/violationPageActions';

describe('violationPageActions', function() {
  describe('loadViolation', function() {
    let store;

    beforeEach(function() {
      store = SpecUtil.mockReduxStore({});
    });

    it('dispatches LOAD_VIOLATION_REQUESTED immediately', function() {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());

      store.dispatch(loadViolation('foo'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(LOAD_VIOLATION_REQUESTED);
    });

    it('dispatches LOAD_VIOLATION_FULFILLED with response data', function(done) {
      const responseData = { foo: 'bar' };

      spyOn(axios, 'get').and.returnValue(Promise.resolve({ data: responseData }));

      store.dispatch(loadViolation()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FULFILLED);
        expect(store.getActions()[1].payload).toEqual({ foo: 'bar' });

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
    });

    it('dispatches LOAD_VIOLATION_FAILED when the response fails', function(done) {
      const responseError = 'errrr!';

      spyOn(axios, 'get').and.returnValue(Promise.reject(responseError));

      store.dispatch(loadViolation()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(LOAD_VIOLATION_FAILED);
        expect(store.getActions()[1].payload).toEqual(responseError);

        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(getViolationDetailsUrl());
    });
  });
});
