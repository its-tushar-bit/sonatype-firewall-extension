/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ComponentFormatTag from '../../../../main/frontend/react/tag/ComponentFormatTag';

describe('ComponentFormatTag', function () {
  let getShallow;

  beforeEach(function () {
    getShallow = enzymeUtils.getShallowComponent(ComponentFormatTag);
  });

  it('displays an icon next to the tag if the format has an icon image available', function () {
    const component = getShallow({ name: 'maven' });
    const icon = component.find('img');

    expect(icon).toExist();
  });

  it('does not display an icon if the format does not have an icon image available', function () {
    const component = getShallow({ name: 'unavailableFormat' });
    const icon = component.find('img');

    expect(icon).not.toExist();
  });

  describe('name mapping', function () {
    it('properly capitalizes the name of the a-name ecosystem', function () {
      const component = getShallow({ name: 'a-name' });
      const text = component.find('span');

      expect(text).toHaveText('A-Name');
    });

    it('properly capitalizes the name of the Alpine ecosystem', function () {
      const component = getShallow({ name: 'alpine' });
      const text = component.find('span');

      expect(text).toHaveText('Alpine');
    });

    it('properly capitalizes the name of the Cargo ecosystem', function () {
      const component = getShallow({ name: 'cargo' });
      const text = component.find('span');

      expect(text).toHaveText('Cargo');
    });

    it('properly capitalizes the name of the Cocoapods ecosystem', function () {
      const component = getShallow({ name: 'cocoapods' });
      const text = component.find('span');

      expect(text).toHaveText('CocoaPods');
    });

    it('properly capitalizes the name of the Composer ecosystem', function () {
      const component = getShallow({ name: 'composer' });
      const text = component.find('span');

      expect(text).toHaveText('Composer');
    });

    it('properly capitalizes the name of the Conan ecosystem', function () {
      const component = getShallow({ name: 'conan' });
      const text = component.find('span');

      expect(text).toHaveText('Conan');
    });

    it('properly capitalizes the name of the Conda ecosystem', function () {
      const component = getShallow({ name: 'conda' });
      const text = component.find('span');

      expect(text).toHaveText('Conda');
    });

    it('properly capitalizes the name of the Cran ecosystem', function () {
      const component = getShallow({ name: 'cran' });
      const text = component.find('span');

      expect(text).toHaveText('CRAN');
    });

    it('properly capitalizes the name of the Debian ecosystem', function () {
      const component = getShallow({ name: 'deb' });
      const text = component.find('span');

      expect(text).toHaveText('Debian');
    });

    it('properly capitalizes the name of the Drupal ecosystem', function () {
      const component = getShallow({ name: 'drupal' });
      const text = component.find('span');

      expect(text).toHaveText('Drupal');
    });

    it('properly capitalizes the name of the Golang ecosystem', function () {
      const component = getShallow({ name: 'golang' });
      const text = component.find('span');

      expect(text).toHaveText('Go');
    });

    it('properly capitalizes the name of the Maven ecosystem', function () {
      const component = getShallow({ name: 'maven' });
      const text = component.find('span');

      expect(text).toHaveText('Maven');
    });

    it('properly capitalizes the name of the NPM ecosystem', function () {
      const component = getShallow({ name: 'npm' });
      const text = component.find('span');

      expect(text).toHaveText('npm');
    });

    it('properly capitalizes the name of the Nuget ecosystem', function () {
      const component = getShallow({ name: 'nuget' });
      const text = component.find('span');

      expect(text).toHaveText('NuGet');
    });

    it('properly capitalizes the name of the PYPI ecosystem', function () {
      const component = getShallow({ name: 'pypi' });
      const text = component.find('span');

      expect(text).toHaveText('PyPI');
    });

    it('properly capitalizes the name of the RPM ecosystem', function () {
      const component = getShallow({ name: 'rpm' });
      const text = component.find('span');

      expect(text).toHaveText('RPM');
    });

    it('properly capitalizes the name of the RubyGems ecosystem', function () {
      const component = getShallow({ name: 'gem' });
      const text = component.find('span');

      expect(text).toHaveText('RubyGems');
    });

    it('properly capitalizes the name of the Swift ecosystem', function () {
      const component = getShallow({ name: 'swift' });
      const text = component.find('span');

      expect(text).toHaveText('Swift');
    });
  });
});
