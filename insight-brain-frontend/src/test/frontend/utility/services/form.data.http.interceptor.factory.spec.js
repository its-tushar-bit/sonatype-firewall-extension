import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('form.data.http.interceptor.factory.spec.js', function() {
  let originalFormData;

  beforeEach(angular.mock.module(utilityModule.name));

  beforeEach(inject(function($window) {
    originalFormData = $window.FormData;
    $window.FormData = angular.noop;
  }));

  afterEach(inject(function($window) {
    $window.FormData = originalFormData;
  }));

  it('augments form data http post requests', inject([
    'form.data.http.interceptor', function(interceptor) {
      var config = {
        method: 'POST',
        data: new FormData(),
        headers: {
          'Content-Type': 'foo'
        }
      };
      config = interceptor.request(config);

      expect(config.headers['Content-Type']).toBeUndefined();
      expect(config.transformRequest).toBe(angular.identity);
    }
  ]));

  it('does not augment non form data http requests', inject([
    'form.data.http.interceptor', function(interceptor) {
      var config = {
        method: 'POST',
        data: {},
        headers: {
          'Content-Type': 'foo'
        }
      };
      config = interceptor.request(config);

      expect(config.headers['Content-Type']).toBe('foo');
      expect(config.transformRequest).toBeUndefined();
    }
  ]));

  it('does not augment form data http put requests', inject([
    'form.data.http.interceptor', function(interceptor) {
      var config = {
        method: 'PUT',
        data: new FormData(),
        headers: {
          'Content-Type': 'foo'
        }
      };
      config = interceptor.request(config);

      expect(config.headers['Content-Type']).toBe('foo');
      expect(config.transformRequest).toBeUndefined();
    }
  ]));
});
