/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClientStub;

import jakarta.ws.rs.core.Response;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Construction-time smoke test: the default stubs must construct cleanly under DI and the dispatcher must
 * be assemblable from them with no extra wiring. Catches accidental @Inject signature drift between
 * {@link ResultsService} and the default stub beans.
 *
 * <p>
 * Also verifies the stubs are fail-fast at request time (rather than silently returning empty), which
 * prevents an operator from serving empty Global Search responses when no real implementation is wired.
 */
public class ResultsServiceWiringTest
{
  @Test
  public void defaultStubBeans_assembleResultsServiceCleanly() {
    GlobalSearchResultsIqLocalClientStub iq = new GlobalSearchResultsIqLocalClientStub();
    GlobalSearchResultsCatalogClientStub catalog = new GlobalSearchResultsCatalogClientStub();
    ResultsService service = new ResultsService(iq, catalog);
    assertThat(service).isNotNull();
  }

  @Test
  public void iqLocalStub_throwsNotConfiguredOnAnyRequest() {
    GlobalSearchResultsIqLocalClientStub stub = new GlobalSearchResultsIqLocalClientStub();
    for (Tab tab : Tab.values()) {
      if (tab == Tab.ALL) {
        continue;
      }
      ResultsRequest req = new ResultsRequest("q", tab, 1, 25, null, null);
      assertThatThrownBy(() -> stub.searchNative(req))
          .isInstanceOf(GlobalSearchNotConfiguredException.class)
          .hasMessageContaining("Global Search requires a real GlobalSearchResultsIqLocalClient bean");
    }
  }

  @Test
  public void stubMapper_returns503ForNotConfiguredException() {
    GlobalSearchResultsIqLocalClientStubMapper mapper = new GlobalSearchResultsIqLocalClientStubMapper();
    Response response =
        mapper.toResponse(new GlobalSearchNotConfiguredException(GlobalSearchResultsIqLocalClientStub.MESSAGE));
    assertThat(response.getStatus()).isEqualTo(503);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getEntity();
    assertThat(body).containsEntry("code", "GLOBAL_SEARCH_NOT_CONFIGURED");
  }

  @Test
  public void catalogStub_isDisabledAndReturnsEmpty() {
    GlobalSearchResultsCatalogClientStub stub = new GlobalSearchResultsCatalogClientStub();
    assertThat(stub.isEnabled()).isFalse();
    ResultsRequest req = new ResultsRequest("q", Tab.COMPONENT, 1, 25, null, null);
    assertThat(stub.searchResults(req)).isEmpty();
  }
}
