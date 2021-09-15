/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import CopyrightFilesTile from '../../../../main/frontend/legal/copyright/CopyrightFilesTile';
import { NxPagination, NxTreeView } from '@sonatype/react-shared-components';

describe('CopyrightFilesTile component', function () {
  let getShallowComponent;

  const minimalProps = {
    componentCopyrightDetails: {
      copyrightContexts: [
        {
          filePath: 'filePath1',
          contexts: ['Copyright 2021 the blah blah'],
        },
      ],
      copyrightFileCounts: {
        '12e02c424ef96230a8f79581b8ad8b16ccd95c5187667d40fba883c0d0d0f8bf': 49,
        '538ffcab74a6487adeb4c757ec05392f4a8c3c7d3d5b16ba3aa3a3fddf5b59d0': 9,
      },
      copyrightIndex: 1,
      loadingFilePaths: false,
      filePathsPage: 0,
      totalFileMatches: 3,
      filePaths: [
        {
          copyrightMatches: 1,
          filePath: 'filePath1',
        },
        {
          copyrightMatches: 1,
          filePath: 'filePath2',
        },
        {
          copyrightMatches: 1,
          filePath: 'filePath3',
        },
      ],
      selectedCopyright: {
        content: 'blah blah',
        id: null,
        originalContentHash: 'b89f37f278521fee5ccdae53806b717586402d9b0808d4cc87c15130d2b73487',
      },
      selectedFilePaths: ['filePath1'],
      status: 'enabled',
    },
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(CopyrightFilesTile, minimalProps);
  });

  it('renders a header with label `File Paths`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('File Paths');
  });

  it('renders a file paths header', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('p.nx-p')).toHaveText('Showing \n' + '          1  - 3 of \n' + '          3 file paths');
  });

  it('renders an empty message if there are no file paths', function () {
    const wrapper = getShallowComponent({
      componentCopyrightDetails: {
        copyrightContexts: [
          {
            filePath: 'filePath1',
            contexts: ['Copyright 2021 the blah blah'],
          },
        ],
        copyrightFileCounts: {
          '12e02c424ef96230a8f79581b8ad8b16ccd95c5187667d40fba883c0d0d0f8bf': 49,
          '538ffcab74a6487adeb4c757ec05392f4a8c3c7d3d5b16ba3aa3a3fddf5b59d0': 9,
        },
        copyrightIndex: 1,
        loadingFilePaths: false,
        filePathsPage: 0,
        totalFileMatches: 3,
        selectedCopyright: {
          content: 'Copyright 2002-2006 the original author or authors.',
          id: null,
          originalContentHash: 'b89f37f278521fee5ccdae53806b717586402d9b0808d4cc87c15130d2b73487',
        },
        selectedFilePaths: ['filePath1'],
        status: 'enabled',
      },
    });
    expect(wrapper.find('h2.nx-h2')).toHaveText('File Paths');
  });

  it('shows file path items', function () {
    const wrapper = getShallowComponent();
    const treeViews = wrapper.find(NxTreeView);
    expect(treeViews.length).toEqual(3);
    expect(treeViews.at(0)).toHaveProp('triggerTooltip', 'filePath1');
    expect(treeViews.at(0)).toHaveProp('isOpen', true);
    expect(treeViews.at(0).find('.nx-blockquote').html()).toEqual(
      `<blockquote class="nx-blockquote copyright-preformatted">Copyright 2021 the <mark class="copyright-highlight">blah blah</mark></blockquote>`
    );
    expect(treeViews.at(1)).toHaveProp('triggerTooltip', 'filePath2');
    expect(treeViews.at(1)).toHaveProp('isOpen', false);
    expect(treeViews.at(2)).toHaveProp('triggerTooltip', 'filePath3');
    expect(treeViews.at(2)).toHaveProp('isOpen', false);
  });

  it('shows correct pagination', function () {
    const wrapper = getShallowComponent({
      componentCopyrightDetails: {
        copyrightContexts: [
          {
            filePath: 'filePath1',
            contexts: ['Copyright 2021 the blah blah'],
          },
        ],
        copyrightFileCounts: {
          '12e02c424ef96230a8f79581b8ad8b16ccd95c5187667d40fba883c0d0d0f8bf': 49,
          '538ffcab74a6487adeb4c757ec05392f4a8c3c7d3d5b16ba3aa3a3fddf5b59d0': 9,
        },
        copyrightIndex: 1,
        loadingFilePaths: false,
        filePaths: [
          {
            copyrightMatches: 1,
            filePath: 'filePath1',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath2',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath3',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath4',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath5',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath6',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath7',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath8',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath9',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath10',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath11',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath12',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath13',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath14',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath15',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath16',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath17',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath18',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath19',
          },
          {
            copyrightMatches: 1,
            filePath: 'filePath20',
          },
        ],
        filePathsPage: 0,
        totalFileMatches: 20,
        selectedCopyright: {
          content: 'Copyright 2002-2006 the original author or authors.',
          id: null,
          originalContentHash: 'b89f37f278521fee5ccdae53806b717586402d9b0808d4cc87c15130d2b73487',
        },
        selectedFilePaths: ['filePath1'],
        status: 'enabled',
      },
    });
    const pagination = wrapper.find(NxPagination);
    expect(pagination.length).toEqual(1);
    expect(pagination).toHaveProp('pageCount', 2);
    expect(pagination).toHaveProp('currentPage', 0);
  });
});
