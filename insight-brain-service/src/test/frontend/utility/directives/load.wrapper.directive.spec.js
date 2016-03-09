describe('load.wrapper.directive.spec.js', function() {
  var element,
      scope;

  beforeEach(module('utility.directives'));

  beforeEach(inject(function($compile, $rootScope) {
    scope = angular.extend($rootScope.$new(), {
      error: null,
      loading: false,
      reload: jasmine.createSpy()
    });

    element = $compile('<div load-wrapper="error" loading="loading" reload="reload()"><div id="content"></div></div>')(scope);
    scope.$digest();
  }));

  it('Directive shows loading circle when loading', function() {
    expect(element.find('i.fa-spin').length).toEqual(0);
    expect(element.find('.clm-alert').attr('class').split(' ')).toContain('ng-hide');
    expect(element.find('#content').length).toBe(1);

    scope.loading = true;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(1);
    expect(element.find('.clm-alert').attr('class').split(' ')).toContain('ng-hide');
    expect(element.find('#content').length).toBe(0);
  });

  it('Directive shows error when error and calls reload on click', function() {
    expect(element.find('i.fa-spin').length).toEqual(0);
    expect(element.find('#content').length).toBe(1);
    expect(element.find('.clm-alert').attr('class').split(' ')).toContain('ng-hide');

    scope.error = true;
    scope.$digest();

    expect(element.find('i.fa-spin').length).toBe(0);
    expect(element.find('#content').length).toBe(0);
    expect(element.find('.clm-alert').attr('class').split(' ')).not.toContain('ng-hide');

    element.find('button.btn-error').trigger('click');
    expect(scope.reload).toHaveBeenCalled();
  });
});
