/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../../../../main/frontend/components/module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('iq-back-button component', function () {
  var getVm, $state;

  beforeEach(
    angular.mock.module(componentsModule.name, legacyConfigurationModule.name)
  );
  beforeEach(inject(function ($componentController) {
    $state = jasmine.createSpyObj('$state', ['get']);
    $state.get.and.callFake(function (stateName) {
      switch (stateName) {
        case 'labs.successMetrics':
          return { data: { title: 'Success Metrics' } };

        case 'no-data':
          return {};

        case 'no-title':
          return { data: {} };
      }
    });

    getVm = function (bindings) {
      return $componentController('iqBackButton', { $state: $state }, bindings);
    };
  }));

  it('linkText supplied via argument is preferred', function () {
    expect(
      getVm({ stateName: 'labs.successMetrics', text: 'FOO' }).linkText
    ).toBe('FOO');
  });

  it('linkText is using page title for given state', function () {
    expect(getVm({ stateName: 'labs.successMetrics' }).linkText).toBe(
      'Back to Success Metrics'
    );
  });

  it('throws error if provided state does not exist', function () {
    function initWithBadState() {
      getVm({ stateName: 'bad.state' });
    }
    expect(initWithBadState).toThrowError(
      'Failed to display iq-back-button, provided state does not exist: bad.state'
    );
  });

  it('linkText is initialized with "Back" if provided state has no "data" property', function () {
    expect(getVm({ stateName: 'no-data' }).linkText).toBe('Back');
  });

  it('linkText is initialized with "Back" if provided state has no "data.title" property', function () {
    expect(getVm({ stateName: 'no-title' }).linkText).toBe('Back');
  });
});
