/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import NoticeTextsTile from '../../../main/frontend/legal/NoticeTextsTile';

describe('NoticeTextsTile component', function() {

  let getShallowComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        noticeFiles: [
          { content: 'notice content 1', relPath: 'path1/notice.txt' },
          { content: 'notice content 2', relPath: 'path2/notice.txt' }
        ]
      }
    }
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeTextsTile, minimalProps);
  });

  it('renders a header with label `Notice Texts`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Notice Texts');
  });

  it('renders the given notices', function() {
    const wrapper = getShallowComponent();
    let noticeDivs = wrapper.find('div.legal-file');
    expect(noticeDivs.length).toBe(2);
    expect(noticeDivs.at(0).find('span.legal-file-path')).toHaveText('path1/notice.txt');
    expect(noticeDivs.at(0).find('blockquote')).toHaveText('notice content 1');
    expect(noticeDivs.at(1).find('span.legal-file-path')).toHaveText('path2/notice.txt');
    expect(noticeDivs.at(1).find('blockquote')).toHaveText('notice content 2');
  });
});
