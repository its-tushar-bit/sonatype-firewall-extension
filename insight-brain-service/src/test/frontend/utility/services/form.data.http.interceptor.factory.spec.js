describe('form.data.http.interceptor.factory.spec.js', function() {
  beforeEach(module('utility'));

  beforeEach(inject(function($window) {
    $window.FormData = angular.noop;
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
