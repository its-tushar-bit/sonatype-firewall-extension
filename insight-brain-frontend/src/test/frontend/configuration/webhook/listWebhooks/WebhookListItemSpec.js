/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import WebhookListItem from '../../../../../main/frontend/configuration/webhook/listWebhooks/WebhookListItem';
import * as routerContext from '../../../../../main/frontend/react/RouterStateContext';

describe('WebhookListItem', () => {
  let getShallow, minProps, hrefSpy;

  beforeEach(() => {
    hrefSpy = jasmine.createSpy('href');

    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    minProps = {
      isAppWebhooksSupported: true,
      webhook: {
        id: '13231654',
        url: 'http://test1',
        secretKey: '#~FAKE~SECRET~KEY~#',
        description: 'webhook 1',
        eventTypes: ['Policy Management', 'Application Evaluation', 'License Override Management'],
      },
    };

    getShallow = enzymeUtils.getShallowComponent(WebhookListItem, minProps);
  });

  it('renders webhook description if available', () => {
    expect(getShallow().find('.nx-list__text')).toHaveText('webhook 1');
  });

  it('renders url if description is not available', () => {
    const component = getShallow({
      webhook: {
        id: 1,
        url: 'http://test1',
      },
    });

    expect(component.find('.nx-list__text')).toHaveText('http://test1');
  });

  it('renders webhook eventTypes as subtext', () => {
    const component = getShallow();

    expect(component.find('.nx-list__subtext')).toHaveText(
      'Policy Management, Application Evaluation, License Override Management'
    );
  });

  it('renders Application Evaluation eventType as disabled if isAppWebhooksSupported is false', () => {
    const subtext = getShallow({ isAppWebhooksSupported: false }).find('.nx-list__subtext');

    const disabledEventTypes = subtext.find('.iq-webhook-event--disabled');
    expect(disabledEventTypes.length).toBe(1);
    expect(disabledEventTypes.childAt(0)).toHaveText('Application Evaluation');
  });

  it('renders all eventTypes as enabled if isAppWebhooksSupported is true', () => {
    const subtext = getShallow().find('.nx-list__subtext');

    expect(subtext.find('.iq-webhook-event--disabled')).not.toExist();
  });

  it('does not render subtext if eventTypes is null', () => {
    const component = getShallow({
      webhook: {
        id: 1,
        url: 'http://test1',
        eventTypes: null,
      },
    });

    expect(component.find('.nx-list__subtext')).not.toExist();
  });

  it('does not render subtext if eventTypes is empty', () => {
    const component = getShallow({
      webhook: {
        id: 1,
        url: 'http://test1',
        eventTypes: [],
      },
    });

    expect(component.find('.nx-list__subtext')).not.toExist();
  });

  it('generates href link to edit webhook page with proper webhook id', () => {
    getShallow();
    expect(hrefSpy).toHaveBeenCalledWith('editWebhook', {
      webhookId: '13231654',
    });
  });
});
