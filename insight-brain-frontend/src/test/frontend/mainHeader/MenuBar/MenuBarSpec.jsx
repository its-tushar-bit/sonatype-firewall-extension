/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import MenuBar from '../../../../main/frontend/mainHeader/MenuBar/MenuBar';
import SystemPreferencesMenu from '../../../../main/frontend/mainHeader/MenuBar/SystemPreferencesMenu/SystemPreferencesMenu';
import HelpMenu from '../../../../main/frontend/mainHeader/MenuBar/HelpMenu/HelpMenu';

describe('MenuBar', function () {
  let getShallowComponent;

  beforeEach(() => {
    const minProps = { isLoggedIn: true };
    getShallowComponent = enzymeUtils.getShallowComponent(MenuBar, minProps);
  });

  it('should pass the majorMinorVersion prop on to the HelpMenu component', () => {
    const component = getShallowComponent({ majorMinorVersion: 'x.x.x' });
    expect(component.find(HelpMenu).prop('majorMinorVersion')).toBe('x.x.x');
  });

  it('should pass the permissions and props on to the SystemPreferencesMenu component', () => {
    const permissions = { SOME_PERMISSION: true };
    const isWebhooksSupported = true;
    const isSourceControlSupported = true;
    const component = getShallowComponent({ permissions, isWebhooksSupported, isSourceControlSupported });
    expect(component.find(SystemPreferencesMenu).prop('permissions')).toBe(permissions);
    expect(component.find(SystemPreferencesMenu).prop('isWebhooksSupported')).toBe(isWebhooksSupported);
    expect(component.find(SystemPreferencesMenu).prop('isSourceControlSupported')).toBe(isSourceControlSupported);
  });

  it('should only render the SystemPreferencesMenu component if there are ANY permissions passed', () => {
    const componentWithPermissions = getShallowComponent({ permissions: { SOME_PERMISSION: true } });
    expect(componentWithPermissions.find(SystemPreferencesMenu).length).toBe(1);

    const componentWithoutPermissions = getShallowComponent({ permissions: {} });
    expect(componentWithoutPermissions.find(SystemPreferencesMenu).length).toBe(0);
  });
});
