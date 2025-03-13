/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import CopyrightStatementsTile from '../../../../main/frontend/legal/copyright/CopyrightStatementsTile';
import { NxAccordion } from '@sonatype/react-shared-components';

describe('CopyrightStatementsTile component', function () {
  let getShallowComponent, $state;

  const minimalProps = {
    component: {
      licenseLegalData: {
        copyrights: [
          {
            id: '',
            content: 'Copyright 2043',
            originalContentHash: '',
            status: 'enabled',
          },
          {
            id: '',
            content: 'Disabled Copyright',
            originalContentHash: '',
            status: 'disabled',
          },
          {
            id: '',
            content: 'Copyright 0',
            originalContentHash: '',
            status: 'enabled',
          },
        ],
      },
    },
    hash: 'testHash',
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

    getShallowComponent = enzymeUtils.getShallowComponent(CopyrightStatementsTile, minimalProps);
  });

  it('renders a header with label `Copyright Notices`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxAccordion.Title)).toHaveText('Copyright Notices');
  });

  it('renders the given copyright statements', function () {
    const wrapper = getShallowComponent();
    let copyrightSpans = wrapper.find('span.nx-list__text');
    expect(copyrightSpans.length).toBe(2);
    expect(copyrightSpans.at(0)).toHaveText('Copyright 2043');
    expect(copyrightSpans.at(1)).toHaveText('Copyright 0');
  });

  it('renders the given copyright statements links by hash', function () {
    const testCopyrightLinks = (props, expectedHrefPrefix) => {
      const wrapper = getShallowComponent(props);
      let copyrightLinks = wrapper.find('a.nx-list__link');

      let copyrightLink = copyrightLinks.at(0);
      expect(copyrightLink).toHaveProp('href', `${expectedHrefPrefix}-{"hash":"testHash","copyrightIndex":0}`);

      copyrightLink = copyrightLinks.at(1);
      expect(copyrightLink).toHaveProp('href', `${expectedHrefPrefix}-{"hash":"testHash","copyrightIndex":2}`);
    };

    testCopyrightLinks(minimalProps, 'legal.componentCopyrightDetails.copyrightDetails');
    testCopyrightLinks(
      {
        ...minimalProps,
        isSbomManager: true,
      },
      'sbomManager.legal.componentCopyrightDetails.copyrightDetails'
    );
  });

  it('renders the given copyright statements links by component identifier', function () {
    const testCopyrightLinksByComponentIdentifier = (props, expectedHrefPrefix) => {
      const wrapper = getShallowComponent(props);
      let copyrightLinks = wrapper.find('a.nx-list__link');

      let copyrightLink = copyrightLinks.at(0);
      expect(copyrightLink).toHaveProp(
        'href',
        `${expectedHrefPrefix}-{"componentIdentifier":"testComponentIdentifier","copyrightIndex":0}`
      );

      copyrightLink = copyrightLinks.at(1);
      expect(copyrightLink).toHaveProp(
        'href',
        `${expectedHrefPrefix}-{"componentIdentifier":"testComponentIdentifier","copyrightIndex":2}`
      );
    };

    const props = {
      ...minimalProps,
      componentIdentifier: 'testComponentIdentifier',
      hash: undefined,
    };

    testCopyrightLinksByComponentIdentifier(
      props,
      'legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails'
    );

    testCopyrightLinksByComponentIdentifier(
      {
        ...props,
        isSbomManager: true,
      },
      'sbomManager.legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails'
    );
  });

  it('renders None found if there are no licenses', function () {
    const wrapper = enzymeUtils.getShallowComponent(CopyrightStatementsTile, {
      component: {
        licenseLegalData: {
          copyrights: [],
        },
      },
    })();
    const content = wrapper.find(NxAccordion).shallow();
    expect(content.find('.nx-accordion__content')).toHaveText('None found');
  });

  it('renders None enabled if all the licenses are disabled', function () {
    const wrapper = enzymeUtils.getShallowComponent(CopyrightStatementsTile, {
      component: {
        licenseLegalData: {
          copyrights: [
            {
              id: '',
              content: 'Disabled Copyright',
              originalContentHash: '',
              status: 'disabled',
            },
          ],
        },
      },
    })();
    const content = wrapper.find(NxAccordion).shallow();
    expect(content.find('.nx-accordion__content')).toHaveText('None enabled');
  });
});
