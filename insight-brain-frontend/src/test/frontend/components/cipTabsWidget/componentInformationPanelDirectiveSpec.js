/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipTabsWidgetModule from '../../../../main/frontend/components/cipTabsWidget/module';

describe('componentInformationPanelDirective', function () {
  let vm, scope, $compile, SelectedComponent;

  beforeEach(angular.mock.module(cipTabsWidgetModule.name));

  beforeEach(inject(function (_$compile_, $rootScope, _SelectedComponent_) {
    $compile = _$compile_;
    SelectedComponent = _SelectedComponent_;
    scope = $rootScope.$new();
    scope.tabs = [
      {
        title: 'Component Info',
      },
      {
        title: 'Policy',
      },
      {
        title: 'Licenses',
      },
      {
        title: 'Vulnerabilities',
      },
      {
        title: 'Labels',
      },
    ];

    const el = angular.element(
      '<component-information-panel tabs="tabs"></component-information-panel>'
    );
    $compile(el)(scope);
    vm = el.isolateScope().vm;
  }));

  it('handles SelectedComponent state change', function () {
    const component = { hash: 'abcd' };

    expect(vm.selectedTab).toBeFalsy();
    expect(vm.showCIP).toBeFalsy();

    SelectedComponent.toggle(component);
    scope.$digest();

    expect(vm.selectedTab).toEqual(scope.tabs[0]);
    expect(vm.showCIP).toBeTruthy();

    SelectedComponent.toggle(component);
    scope.$digest();

    expect(vm.selectedTab).toBeFalsy();
    expect(vm.showCIP).toBeFalsy();
  });

  describe('hide', function () {
    it('toggles selectedComponent', function () {
      spyOn(SelectedComponent, 'toggle');

      vm.hide();
      expect(SelectedComponent.toggle).toHaveBeenCalledWith();
    });
  });

  describe('tabShown', function () {
    it('returns true if tab is configured to be matchedOnly and component is matched', function () {
      const component = { hash: 'abcd' };
      const tab = { title: 'foo', matchedOnly: true };

      SelectedComponent.toggle(component);
      expect(vm.tabShown(tab)).toBe(true);
    });

    it('returns false if tab is configured to be matchedOnly and component is not matched', function () {
      const component = { hash: 'abcd', matchState: 'unknown' };
      const tab = { title: 'foo', matchedOnly: true };

      SelectedComponent.toggle(component);
      expect(vm.tabShown(tab)).toBe(false);
    });

    it('returns true if tab is not configured to be matchedOnly and component is not matched', function () {
      const component = { hash: 'abcd', matchState: 'unknown' };
      const tab = { title: 'foo' };

      SelectedComponent.toggle(component);
      expect(vm.tabShown(tab)).toBe(true);
    });

    it('returns true if tab is not configured to be matchedOnly and component is matched', function () {
      const component = { hash: 'abcd' };
      const tab = { title: 'foo' };

      SelectedComponent.toggle(component);
      expect(vm.tabShown(tab)).toBe(true);
    });
  });

  describe('hide-close-button attribute', function () {
    it('hides close button when provided', function () {
      const el = angular.element(
        '<component-information-panel hide-close-button tabs="tabs"></component-information-panel>'
      );
      $compile(el)(scope);
      expect(el.isolateScope().vm.showCloseButton).toBe(false);
    });

    it('shows close button when not provided', function () {
      expect(vm.showCloseButton).toBe(true);
    });
  });
});
