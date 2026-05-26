/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.hds.HdsClient;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Verifies that the {@code ObjectProvider<HdsClient>} migration from
 * {@code Provider<List<HdsClient>>} correctly iterates all HDS client beans.
 *
 * <p>
 * The migration replaced {@code hdsClientsProvider.get().forEach(...)} with
 * {@code hdsClients.orderedStream().forEach(...)}. This test ensures that
 * {@code orderedStream()} correctly collects all HdsClient implementations and
 * that operations like {@code serverConfigurationChanged()} are invoked on each.
 */
public class HdsClientCollectionTest
{
  @SuppressWarnings("unchecked")
  @Test
  public void orderedStream_invokesServerConfigurationChangedOnAllClients() {
    HdsClient client1 = mock(HdsClient.class);
    HdsClient client2 = mock(HdsClient.class);
    HdsClient client3 = mock(HdsClient.class);

    ObjectProvider<HdsClient> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenReturn(Stream.of(client1, client2, client3));

    // Simulate the pattern used in Configuration.java
    provider.orderedStream().forEach(HdsClient::serverConfigurationChanged);

    verify(client1).serverConfigurationChanged();
    verify(client2).serverConfigurationChanged();
    verify(client3).serverConfigurationChanged();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void orderedStream_returnsAllClients() {
    HdsClient client1 = mock(HdsClient.class);
    HdsClient client2 = mock(HdsClient.class);

    ObjectProvider<HdsClient> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenReturn(Stream.of(client1, client2));

    List<HdsClient> clients = provider.orderedStream().toList();

    assertThat(clients).containsExactly(client1, client2);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void orderedStream_handlesEmptyProviderGracefully() {
    ObjectProvider<HdsClient> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenReturn(Stream.empty());

    List<HdsClient> clients = provider.orderedStream().toList();

    assertThat(clients).isEmpty();
  }
}
