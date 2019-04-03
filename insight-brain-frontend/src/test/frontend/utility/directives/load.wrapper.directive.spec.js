import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('load.wrapper.directive.spec.js', function() {
  var element,
      scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($compile, $rootScope) {
    scope = angular.extend($rootScope.$new(), {
      error: null,
      loading: false,
      reload: jasmine.createSpy(),
      canRetry: undefined
    });

    element = $compile('<div load-wrapper="error" loading="loading" reload="reload()" can-retry="canRetry">' +
        '<div id="content"></div></div>')(scope);
    scope.$digest();
  }));

  it('Directive shows loading circle when loading', function() {
    expect(element.find('i.fa-spin').length).toEqual(0);
    expect(element.find('.iq-alert').attr('class').split(' ')).toContain('ng-hide');
    expect(element.find('#content').length).toBe(1);

    scope.loading = true;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(1);
    expect(element.find('.iq-alert').attr('class').split(' ')).toContain('ng-hide');
    expect(element.find('#content').length).toBe(0);
  });

  it('Directive shows error when error and calls reload on click', function() {
    expect(element.find('i.fa-spin').length).toEqual(0);
    expect(element.find('#content').length).toBe(1);
    expect(element.find('.iq-alert').attr('class').split(' ')).toContain('ng-hide');

    scope.error = true;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(0);
    expect(element.find('#content').length).toBe(0);
    expect(element.find('.iq-alert').attr('class').split(' ')).not.toContain('ng-hide');

    expect(element.find('.btn.btn-error').length).toBe(1);
    element.find('.btn.btn-error').trigger('click');
    expect(scope.reload).toHaveBeenCalled();
  });

  it('Directive shows and calls retry button when canRetry is true', function() {
    scope.error = true;
    scope.canRetry = true;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(0);
    expect(element.find('#content').length).toBe(0);
    expect(element.find('.iq-alert').attr('class').split(' ')).not.toContain('ng-hide');

    expect(element.find('.btn.btn-error').length).toBe(1);
    element.find('.btn.btn-error').trigger('click');
    expect(scope.reload).toHaveBeenCalled();
  });

  it('Directive hides retry button when canRetry is false', function() {
    scope.error = true;
    scope.canRetry = false;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(0);
    expect(element.find('#content').length).toBe(0);
    expect(element.find('.iq-alert').attr('class').split(' ')).not.toContain('ng-hide');

    expect(element.find('.btn.btn-error').length).toBe(0);
  });
});
