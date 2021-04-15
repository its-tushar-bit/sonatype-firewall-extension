/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import CopyrightStatementsTile from '../../../../main/frontend/legal/copyright/CopyrightStatementsTile';

describe('CopyrightStatementsTile component', function () {
  let getShallowComponent;

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
    $state: {
      get: () => '',
      href: () => '',
    },
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(CopyrightStatementsTile, minimalProps);
  });

  it('renders a header with label `Copyright Statements`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Copyright Statements');
  });

  it('renders the given copyright statements', function () {
    const wrapper = getShallowComponent();
    let copyrightSpans = wrapper.find('span.nx-list__text');
    expect(copyrightSpans.length).toBe(2);
    expect(copyrightSpans.at(0)).toHaveText('Copyright 2043');
    expect(copyrightSpans.at(1)).toHaveText('Copyright 0');
  });

  it('renders None found if there are no licenses', function () {
    const wrapper = enzymeUtils.getShallowComponent(CopyrightStatementsTile, {
      component: {
        licenseLegalData: {
          copyrights: [],
        },
      },
    })();
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
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
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None enabled');
  });
});
