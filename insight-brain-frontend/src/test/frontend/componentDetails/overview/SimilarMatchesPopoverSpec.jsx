/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxList, NxTextLink } from '@sonatype/react-shared-components';

import ComponentDisplay from '../../../../main/frontend/ComponentDisplay/ReactComponentDisplay';
import SimilarMatchesPopover from '../../../../main/frontend/componentDetails/overview/SimilarMatchesPopoover/SimilarMatchesPopover';
import { IqPopover } from '../../../../main/frontend/react/IqPopover';

describe('SimilarMatchesPopover', () => {
  let minimalProps, getShallow;

  const bestMatch = {
    displayName: { parts: [{ field: 'MatchFieldName', value: 'MatchFieldValueBestMatch' }] },
  };
  const otherMatches = [
    {
      displayName: { parts: [{ field: 'MatchFieldName', value: 'MatchFieldValueOtherMatch1' }] },
    },
    {
      displayName: { parts: [{ field: 'MatchFieldName', value: 'MatchFieldValueOtherMatch2' }] },
    },
  ];

  beforeEach(() => {
    minimalProps = {
      similarMatches: [bestMatch, ...otherMatches],
      onClose: jasmine.createSpy('onClose'),
      showSimilarMatchesPopover: true,
    };

    getShallow = enzymeUtils.getShallowComponent(SimilarMatchesPopover, minimalProps);
  });

  it('does not render anything if showSimilarMatchesPopover is false', () => {
    const nonPopover = getShallow({ ...minimalProps, showSimilarMatchesPopover: false });
    expect(nonPopover.getElement()).toBeNull();
  });

  describe('renders a popover', () => {
    it('renders a large popover with the appropriate close handler', () => {
      const component = getShallow(),
        popover = component.find(IqPopover);

      expect(popover).toExist();
      expect(popover).toHaveProp('size', 'large');
      expect(popover).toHaveProp('onClose', minimalProps.onClose);
    });

    it('renders a text with a link to the component identification documentation', () => {
      const expectedLinkHref =
        'https://help.sonatype.com/iqserver/reporting/application-composition-report/component-identification';
      const component = getShallow(),
        popover = component.find(IqPopover),
        text = popover.find('p'),
        link = text.find(NxTextLink);

      expect(link).toExist();
      expect(link).toHaveProp('href', expectedLinkHref);
    });

    it('renders a single list for the best match when there is a single best match for the component', () => {
      const component = getShallow({ similarMatches: [bestMatch] }),
        popover = component.find(IqPopover),
        headers = popover.find('h3'),
        lists = popover.find(NxList);

      expect(headers.length).toBe(1);
      expect(lists.length).toBe(1);
      expect(headers.at(0)).toHaveText('Best Match');

      const listItems = lists.at(0).find(NxList.Item);
      expect(listItems.length).toBe(1);
      const listItemDisplay = listItems.at(0).find(ComponentDisplay);
      expect(listItemDisplay).toHaveProp('component', bestMatch);
    });

    it('renders an additional list to display all other matches apart from the best match for the component', () => {
      const component = getShallow(),
        popover = component.find(IqPopover),
        headers = popover.find('h3'),
        lists = popover.find(NxList);

      expect(headers.length).toBe(2);
      expect(lists.length).toBe(2);
      expect(headers.at(1)).toHaveText('Other Matches');

      const listItems = lists.at(1).find(NxList.Item);
      expect(listItems.length).toBe(2);
      expect(listItems.at(0).find(ComponentDisplay)).toHaveProp('component', otherMatches[0]);
      expect(listItems.at(1).find(ComponentDisplay)).toHaveProp('component', otherMatches[1]);
    });
  });
});
