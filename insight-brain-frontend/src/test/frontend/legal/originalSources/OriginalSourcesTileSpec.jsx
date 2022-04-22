/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxAccordion, NxList, NxTextLink } from '@sonatype/react-shared-components';
import OriginalSourcesTile from 'MainRoot/legal/originalSources/OriginalSourcesTile';

describe('OriginalSourcesTile component', function () {
  let getShallowComponent, $state;

  const minimalProps = {
    sourceLinks: [
      {
        id: '9ebd06ff0e5746d0abfec3d47e062881',
        content: 'source1',
        status: 'enabled',
      },
      {
        id: '',
        content: 'source2',
        status: 'disabled',
      },
      {
        id: '',
        content: 'https://source3.com',
        status: 'enabled',
      },
    ],
    setDisplayOriginalSourcesOverrideModal: jasmine.createSpy(),
    showOriginalSourcesModal: false,
  };

  beforeEach(function () {
    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    minimalProps.$state = $state;

    getShallowComponent = enzymeUtils.getShallowComponent(OriginalSourcesTile, minimalProps);
  });

  it('renders a header with label `Original Source Code`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxAccordion.Title)).toHaveText('Original Source Code');
  });

  it('renders the given source links', function () {
    const wrapper = getShallowComponent();
    let sourceLinkSpans = wrapper.find(NxList.Text);
    expect(sourceLinkSpans.length).toBe(2);
    expect(sourceLinkSpans.at(0)).toHaveText('source1');
    expect(sourceLinkSpans.at(1)).toHaveText('https://source3.com');
  });

  it('renders the given source link hrefs if it starts with https', function () {
    const wrapper = getShallowComponent();
    let sourceLinks = wrapper.find(NxList.Text).find(NxTextLink);
    expect(sourceLinks.length).toBe(1);
    expect(sourceLinks.at(0)).toHaveProp('href', 'https://source3.com');
  });

  it('renders None found if there are no source links', function () {
    const wrapper = enzymeUtils.getShallowComponent(OriginalSourcesTile, {
      sourceLinks: [],
    })();
    const content = wrapper.find(NxAccordion).shallow();
    expect(content.find('.nx-accordion__content')).toHaveText('None found');
  });

  it('renders None found if all the licenses are disabled', function () {
    const wrapper = enzymeUtils.getShallowComponent(OriginalSourcesTile, {
      sourceLinks: [
        {
          id: '',
          content: 'Disabled Source Link',
          originalContentHash: '',
          status: 'disabled',
        },
      ],
    })();
    const content = wrapper.find(NxAccordion).shallow();
    expect(content.find('.nx-accordion__content')).toHaveText('None found');
  });
});
