/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';

describe('ViolationDetailsSubtitle', function () {
  let getComponentNameMock,
    ViolationDetailsSubtitle,
    minimalProps,
    getShallowComponent;

  beforeEach(function () {
    getComponentNameMock = jasmine
      .createSpy('getComponentName')
      .and.returnValue('foo : bar');

    minimalProps = {
      organizationName: 'Org 1',
      applicationName: 'App 1',
    };

    ViolationDetailsSubtitle = require('inject-loader!../../../main/frontend/violation/ViolationDetailsSubtitle')(
      {
        '../util/componentNameUtils': {
          getComponentName: getComponentNameMock,
        },
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(
      ViolationDetailsSubtitle,
      minimalProps
    );
  });

  it('renders a nx-tile-header__subtitle', function () {
    expect(getShallowComponent()).toHaveClassName('nx-tile-header__subtitle');
  });

  describe('organization name part', function () {
    it('renders and icon and the org name as the first iq-violation-details__subtitle-part', function () {
      const part = getShallowComponent()
        .find('.iq-violation-details__subtitle-part')
        .at(0);

      expect(part).toExist();
      expect(part).toHaveText('<NxFontAwesomeIcon />Org 1');
    });
  });

  describe('application name part', function () {
    it('renders and icon and the app name as the second iq-violation-details__subtitle-part', function () {
      const part = getShallowComponent()
        .find('.iq-violation-details__subtitle-part')
        .at(1);

      expect(part).toExist();
      expect(part).toHaveText('<NxFontAwesomeIcon />App 1');
    });
  });

  describe('component name part', function () {
    it('renders and icon and the component name as the third iq-violation-details__subtitle-part', function () {
      const part = getShallowComponent()
        .find('.iq-violation-details__subtitle-part')
        .at(2);

      expect(part).toExist();
      expect(part).toHaveText('<NxFontAwesomeIcon />foo : bar');
    });

    it("passes the component's displayName, filename, and filenames to getComponentName", function () {
      const nameProps = {
        displayName: { foo: 'bar' },
        filename: 'bar.js',
        filenames: ['bar.js', 'Bar.js'],
      };

      getShallowComponent(nameProps);

      expect(getComponentNameMock).toHaveBeenCalledWith(
        jasmine.objectContaining(nameProps)
      );
    });
  });
});
