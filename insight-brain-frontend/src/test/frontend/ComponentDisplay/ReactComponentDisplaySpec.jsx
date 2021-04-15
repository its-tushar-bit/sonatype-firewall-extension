/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';

import ComponentDisplay from '../../../main/frontend/ComponentDisplay/ReactComponentDisplay';
import { NxOverflowTooltip } from '@sonatype/react-shared-components';

describe('ComponentDisplay (React)', function () {
  const minimalProps = {
    component: {},
  };

  const getShallowComponent = enzymeUtils.getShallowComponent(
    ComponentDisplay,
    minimalProps
  );

  it('renders a Tooltip on the component', function () {
    expect(getShallowComponent()).toMatchSelector(NxOverflowTooltip);
  });

  it('adds the truncate-ellipsis class to the div iff the truncate prop is set', function () {
    expect(getShallowComponent().find('div')).not.toHaveClassName(
      'truncate-ellipsis'
    );
    expect(
      getShallowComponent({ truncate: false }).find('div')
    ).not.toHaveClassName('truncate-ellipsis');
    expect(getShallowComponent({ truncate: true }).find('div')).toHaveClassName(
      'truncate-ellipsis'
    );
  });

  it('renders an em child with "Unknown" if no displayName or filename is set', function () {
    expect(getShallowComponent().find('em')).toHaveText('Unknown');
    expect(getShallowComponent().find('span')).not.toExist();

    expect(
      getShallowComponent({
        component: { displayName: null, filename: null, filenames: null },
      }).find('em')
    ).toHaveText('Unknown');
    expect(
      getShallowComponent({
        component: { displayName: null, filename: null, filenames: null },
      }).find('span')
    ).not.toExist();
  });

  it('renders an em child with text derived from the filenames if they are present', function () {
    const componentWithFilenames = { filenames: ['foo.js', 'bar/baz.js'] };

    expect(
      getShallowComponent({ component: componentWithFilenames }).find('em')
    ).toHaveText('foo.js, bar/baz.js');
    expect(
      getShallowComponent({ component: componentWithFilenames }).find('span')
    ).not.toExist();
  });

  it('renders an em child with text derived from the filename (singular) if it is present', function () {
    const componentWithFilename = { filename: 'asdf.js' },
      componentWithFilenameAndFilenames = {
        ...componentWithFilename,
        filenames: ['foo.js', 'bar/baz.js'],
      };

    [componentWithFilename, componentWithFilenameAndFilenames].map(
      (component) => {
        expect(getShallowComponent({ component }).find('em')).toHaveText(
          'asdf.js'
        );
        expect(getShallowComponent({ component }).find('span')).not.toExist();
      }
    );
  });

  it('renders an em child with text derived from a displayName consisting only of a Filename part', function () {
    const componentWithDisplayName = {
        displayName: { parts: [{ field: 'Filename', value: 'display.js' }] },
      },
      componentWithDisplayNameAndFilename = {
        ...componentWithDisplayName,
        filename: 'asdf.js',
      },
      componentWithDisplaynameAndFilenameAndFilenames = {
        ...componentWithDisplayNameAndFilename,
        filenames: ['foo.js', 'bar/baz.js'],
      };

    [
      componentWithDisplayName,
      componentWithDisplayNameAndFilename,
      componentWithDisplaynameAndFilenameAndFilenames,
    ].map((component) => {
      expect(getShallowComponent({ component }).find('em')).toHaveText(
        'display.js'
      );
      expect(getShallowComponent({ component }).find('span')).not.toExist();
    });
  });

  it('renders a span child with text derived from a displayName that does not represent a filename', function () {
    const componentWithDisplayName = {
        displayName: {
          parts: [
            { field: 'groupId', value: 'org.slf4j' },
            { value: ' : ' },
            { field: 'artifactId', value: 'slf4j-log4j12' },
            { value: ' ~ ' }, // not realistic syntax, but just to check that it is in fact concatenating these values
            { field: 'version', value: '1' },
          ],
        },
      },
      componentWithDisplayNameAndFilename = {
        ...componentWithDisplayName,
        filename: 'asdf.js',
      },
      componentWithDisplaynameAndFilenameAndFilenames = {
        ...componentWithDisplayNameAndFilename,
        filenames: ['foo.js', 'bar/baz.js'],
      };

    [
      componentWithDisplayName,
      componentWithDisplayNameAndFilename,
      componentWithDisplaynameAndFilenameAndFilenames,
    ].map((component) => {
      expect(getShallowComponent({ component }).find('span')).toHaveText(
        'org.slf4j : slf4j-log4j12 ~ 1'
      );
      expect(getShallowComponent({ component }).find('em')).not.toExist();
    });
  });
});
