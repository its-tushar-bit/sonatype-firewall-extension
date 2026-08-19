/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PagedNamingEnumerationTest
{

  @Mock
  private LdapContext ctx;

  @Mock
  private NamingEnumeration<SearchResult> results;

  @Mock
  private SearchResult result;

  private final String baseDN = "baseDN";

  private final String filter = "filter";

  private final SearchControls searchControls = new SearchControls();

  private final byte pageSize = 17;

  private final byte resultSize = 0;

  private final byte cookie = 13;

  private byte[] berEncoded(byte results, Byte cookie) {
    if (cookie == null) {
      return new byte[]{48, 5, 2, 1, results, 4, 0};
    }
    else {
      return new byte[]{48, 6, 2, 1, results, 4, 1, cookie};
    }
  }

  private void assertRequestControls(Control[] requestControls, boolean critical, byte pageSize, Byte cookie) {
    assertThat(requestControls).hasSize(1);
    assertThat(requestControls[0]).isInstanceOf(PagedResultsControl.class);
    PagedResultsControl control = (PagedResultsControl) requestControls[0];
    assertThat(control.isCritical()).isEqualTo(critical);
    assertThat(control.getEncodedValue()).isEqualTo(berEncoded(pageSize, cookie));
  }

  private Control[] newResponseControls(Byte cookie) throws Exception {
    return new Control[]{new PagedResultsResponseControl("id", false, berEncoded(resultSize, cookie))};
  }

  @Test
  public void testConstructor_StartsPagedSearch() throws Exception {
    new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    ArgumentCaptor<Control[]> requestControlsArg = ArgumentCaptor.forClass(Control[].class);
    verify(ctx).setRequestControls(requestControlsArg.capture());
    assertRequestControls(requestControlsArg.getValue(), false, pageSize, null);
    verify(ctx).search(baseDN, filter, searchControls);
  }

  @Test
  public void testConstructor_RestoresOriginalRequestControlsUponException() throws Exception {
    Control[] requestControls = {};
    when(ctx.getRequestControls()).thenReturn(requestControls);
    when(ctx.search(baseDN, filter, searchControls)).thenThrow(NamingException.class);
    assertThatThrownBy(() -> new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize))
        .isInstanceOf(NamingException.class);
    verify(ctx).setRequestControls(same(requestControls));
  }

  @Test
  public void testClose_ClosesResultEnumeration() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize).close();
    verify(results).close();
  }

  @Test
  public void testClose_ClosesCookie() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenAnswer((Answer<Boolean>) invocation -> {
      when(ctx.getResponseControls()).thenReturn(newResponseControls(cookie));
      return false;
    }).thenReturn(true);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    en.hasMore();
    verify(results, times(2)).hasMore();
    verify(ctx, times(2)).search(baseDN, filter, searchControls);
    en.close();
    ArgumentCaptor<Control[]> requestControlsArg = ArgumentCaptor.forClass(Control[].class);
    verify(ctx, times(4)).setRequestControls(requestControlsArg.capture());
    assertRequestControls(requestControlsArg.getAllValues().get(2), false, (byte) 0, cookie);
    verify(ctx, times(3)).search(baseDN, filter, searchControls);
  }

  @Test
  public void testClose_RestoresOriginalRequestControls() throws Exception {
    Control[] requestControls = {};
    when(ctx.getRequestControls()).thenReturn(requestControls);
    new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize).close();
    verify(ctx).setRequestControls(same(requestControls));
  }

  @Test
  public void testHasMoreElements_EatsNamingExceptionAsPerApi() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenThrow(NamingException.class);
    assertThat(new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize).hasMoreElements()).isFalse();
  }

  @Test
  public void testNextElement_SearchIncomplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenReturn(true);
    when(results.next()).thenReturn(result);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    assertThat(en.nextElement()).isEqualTo(result);
  }

  @Test
  public void testNextElement_SearchComplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenReturn(false);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    assertThatThrownBy(en::nextElement).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void testHasMore_PageIncomplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenReturn(true);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    assertThat(en.hasMore()).isTrue();
    verify(ctx).search(baseDN, filter, searchControls);
  }

  @Test
  public void testHasMore_PageComplete_SearchIncomplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenAnswer((Answer<Boolean>) invocation -> {
      when(ctx.getResponseControls()).thenReturn(newResponseControls(cookie));
      return false;
    }).thenReturn(true);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    verify(ctx).search(baseDN, filter, searchControls);
    assertThat(en.hasMore()).isTrue();
    ArgumentCaptor<Control[]> requestControlsArg = ArgumentCaptor.forClass(Control[].class);
    verify(ctx, times(2)).setRequestControls(requestControlsArg.capture());
    assertRequestControls(requestControlsArg.getAllValues().get(1), true, pageSize, cookie);
    verify(ctx, times(2)).search(baseDN, filter, searchControls);
  }

  @Test
  public void testHasMore_PageComplete_SearchComplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenAnswer((Answer<Boolean>) invocation -> {
      when(ctx.getResponseControls()).thenReturn(newResponseControls(null));
      return false;
    });
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    verify(ctx).search(baseDN, filter, searchControls);
    assertThat(en.hasMore()).isFalse();
    verify(ctx).search(baseDN, filter, searchControls);
  }

  @Test
  public void testNext_SearchIncomplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenReturn(true);
    when(results.next()).thenReturn(result);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    assertThat(en.next()).isEqualTo(result);
  }

  @Test
  public void testNext_SearchComplete() throws Exception {
    when(ctx.search(baseDN, filter, searchControls)).thenReturn(results);
    when(results.hasMore()).thenReturn(false);
    PagedNamingEnumeration en = new PagedNamingEnumeration(ctx, baseDN, filter, searchControls, pageSize);
    assertThatThrownBy(en::next).isInstanceOf(NoSuchElementException.class);
  }
}
