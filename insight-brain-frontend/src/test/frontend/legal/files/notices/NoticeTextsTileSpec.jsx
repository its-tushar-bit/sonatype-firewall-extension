/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import NoticeTextsTile from '../../../../../main/frontend/legal/files/notices/NoticeTextsTile';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';

describe('NoticeTextsTile', function () {
  let getShallowComponent, minimalProps, setShowNoticesModalSpy;

  beforeEach(function () {
    setShowNoticesModalSpy = jasmine.createSpy('setShowNoticesModalSpy');
    minimalProps = {
      setShowNoticesModal: setShowNoticesModalSpy,
      noticeFiles: [
        {
          originalStatus: 'enabled',
          originalContent: 'notice content 1',
          content: 'notice content 1',
          relPath: 'path1/notice.txt',
        },
        {
          originalStatus: 'enabled',
          originalContent: 'notice content 2',
          content: 'notice content 2',
        },
        {
          originalStatus: 'disabled',
          originalContent: 'notice content 3',
          content: 'notice content 3',
        },
      ],
      showNoticesModal: false,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(
      NoticeTextsTile,
      minimalProps
    );
  });

  it('renders a header with label `Notice Texts`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Notice Texts');
  });

  it('renders the given notices', function () {
    const wrapper = getShallowComponent();
    const notices = wrapper.find('.legal-file');
    expect(notices.length).toBe(2);
    expect(notices.at(0).find('.legal-file-path')).toHaveText(
      'path1/notice.txt'
    );
    expect(notices.at(0).find('blockquote')).toHaveText('notice content 1');
    expect(notices.at(1).find('.legal-file-path')).toHaveText('');
    expect(notices.at(1).find('blockquote')).toHaveText('notice content 2');
  });

  it('renders none found if there are no notices', function () {
    const wrapper = getShallowComponent({ noticeFiles: [] });
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
  });

  it('renders an add button if there are no notices', function () {
    const wrapper = getShallowComponent({ noticeFiles: [] });
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPlus);
    expect(button.find('span').at(0)).toHaveText('Add');
  });

  it('renders an edit button if there is at least one notice', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPen);
    expect(button.find('span').at(0)).toHaveText('Edit');
  });

  it('shows the notices modal when clicking the add/edit button', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    button.simulate('click');
    expect(setShowNoticesModalSpy).toHaveBeenCalledWith(true);
  });
});
