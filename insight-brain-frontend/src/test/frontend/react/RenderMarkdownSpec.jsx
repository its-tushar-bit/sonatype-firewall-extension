/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, shallow, mount } from 'enzyme';
import RenderMarkdown from 'MainRoot/react/RenderMarkdown';

describe('RenderMarkdown', function () {
  it('passes className prop to ReactCommonmark', function () {
    const shallowRender = shallow(<RenderMarkdown className="test-class">content</RenderMarkdown>);
    expect(shallowRender).toHaveClassName('test-class');
  });

  it('handles single ticks', function () {
    const wrapper = render(<RenderMarkdown>code `example`</RenderMarkdown>);
    expect(wrapper.html()).toBe('<p>code <code>example</code></p>');
  });

  it('handles double ticks', function () {
    const wrapper = render(<RenderMarkdown>code ``example``</RenderMarkdown>);
    expect(wrapper.html()).toBe('<p>code <code>example</code></p>');
  });

  it('disables gfm when parsing markdown', function () {
    const wrapper = render(<RenderMarkdown>link: http://example.com</RenderMarkdown>);
    expect(wrapper.html()).toBe('<p>link: http://example.com</p>');
  });

  it('adds target and rel attributes to markdown links', function () {
    const wrapper = mount(<RenderMarkdown>[example](http://example.com)</RenderMarkdown>);
    expect(wrapper.find('a')).toHaveProp({ target: '_blank', rel: 'noreferrer' });
  });

  it('adds an icon to markdown links', function () {
    const wrapper = mount(<RenderMarkdown>[example](http://example.com)</RenderMarkdown>);
    expect(wrapper).toContainMatchingElement('.nx-icon');
  });

  it('escapes raw html in markdown', function () {
    const markdownString = 'foo <h1>bar</h1>';
    const wrapper = render(<RenderMarkdown>{markdownString}</RenderMarkdown>);
    expect(wrapper.html()).toBe('<p>foo <span>&lt;h1&gt;</span>bar<span>&lt;/h1&gt;</span></p>');
  });

  it('handles fenced code blocks', function () {
    const markdownString = '```\ncode block example\n```';
    const wrapper = render(<RenderMarkdown>{markdownString}</RenderMarkdown>);
    expect(wrapper.html()).toBe('<pre><code>code block example\n</code></pre>');
  });

  it('handles fenced code blocks within blockquote', function () {
    const markdownString = '> Some other text in the blockquote\n' + '> ```\n' + '> code block example\n' + '> ```';
    const wrapper = render(<RenderMarkdown>{markdownString}</RenderMarkdown>);
    expect(wrapper.html()).toBe(
      '<blockquote><p>Some other text in the blockquote</p><pre><code>code block example\n' +
        '</code></pre></blockquote>'
    );
  });

  it('handles soft break', function () {
    const markdownString = 'foo\nbar';
    const wrapper = render(<RenderMarkdown>{markdownString}</RenderMarkdown>);
    expect(wrapper.html()).toBe('<p>foo<br>bar</p>');
  });
});
