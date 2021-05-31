/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';

import SystemNotice from '../../../main/frontend/systemNotice/SystemNotice';

describe('SystemNotice', function () {
  let mockLoadSystemNotice, getShallowComponent, minimalProps;

  beforeEach(function () {
    mockLoadSystemNotice = jasmine.createSpy('loadConfiguration');

    minimalProps = {
      loadSystemNotice: mockLoadSystemNotice,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SystemNotice, minimalProps);
  });

  describe('on render', function () {
    it('renders the message passed as a prop', function () {
      const component = getShallowComponent({ message: 'system notice' });

      expect(component).toExist();
      expect(component.text()).toBe('system notice');
    });

    it('does not show a component if message is empty', function () {
      const component = getShallowComponent({ message: null });

      expect(component).toBeEmptyRender();
    });
  });

  describe('on initial load', function () {
    it('calls loadConfiguration', function () {
      const getMountedComponent = enzymeUtils.getMountedComponent(SystemNotice, minimalProps);
      const mountedComponent = getMountedComponent({ message: 'text' });

      expect(mockLoadSystemNotice).toHaveBeenCalled();
      mountedComponent.unmount();
    });
  });
});
