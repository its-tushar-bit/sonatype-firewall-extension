import {toURIParams} from '../../../main/frontend/util/jsUtil';

describe('jsUtil', function() {
  describe('toURIParams', function() {
    it('encodes only defined parameters', function() {
      const params = {
        foo: null,
        'f o o': '?x=шеллы',
        baz: undefined,
        bar: '?x=test'
      };
      expect(toURIParams(params)).toEqual('f%20o%20o=%3Fx%3D%D1%88%D0%B5%D0%BB%D0%BB%D1%8B&bar=%3Fx%3Dtest');
    });
    it('handles empty object', function() {
      expect(toURIParams({})).toEqual('');
    });
  });
});
