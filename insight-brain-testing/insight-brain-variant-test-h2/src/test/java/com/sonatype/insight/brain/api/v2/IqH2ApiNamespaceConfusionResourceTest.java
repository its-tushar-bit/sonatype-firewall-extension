/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiNamespaceConfusionResourceTest
{
  private IqTestContext ctx;

  private ProprietaryComponentNamePatternDAO proprietaryDao;

  private RepositoryDAO repositoryDao;

  @BeforeEach
  void setup() {
    proprietaryDao = ctx.lookup(ProprietaryComponentNamePatternDAO.class);
    repositoryDao = ctx.lookup(RepositoryDAO.class);
  }

  @Test
  void testAddProprietaryComponentNames_NamespaceIsAddedToCorrectFormat() throws Exception {
    HttpResponse response = buildCreateRequest("maven2", List.of("org.sonatype")).post();
    ctx.assertResponseStatus(204, response);

    Repository repository = repositoryDao.getByRepositoryManagerInstanceIdAndPublicId(
        ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_ROOT,
        ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PREFIX + "maven2");
    assertThat(repository).isNotNull();

    List<ProprietaryComponentNamePattern> namespacePatterns = proprietaryDao.getByFormat("maven");
    assertThat(namespacePatterns).hasSize(1);
    assertThat(namespacePatterns.get(0).getNamespacePattern()).isEqualTo("org.sonatype");
  }

  @Test
  void testAddProprietaryComponentNames_AddingNamespacesIsAdditive() throws Exception {
    buildCreateRequest("maven2", List.of("org.sonatype")).post();
    buildCreateRequest("maven2", List.of("com.sonatype")).post();

    List<ProprietaryComponentNamePattern> namespacePatterns = proprietaryDao.getByFormat("maven");
    assertThat(namespacePatterns).hasSize(2);
  }

  @Test
  void testAddProprietaryComponentNames_DifferentFormatsCreatesRepositories() throws Exception {
    buildCreateRequest("maven2", List.of("org.sonatype")).post();
    buildCreateRequest("npm", List.of("com.sonatype")).post();

    List<ProprietaryComponentNamePattern> mavenPatterns = proprietaryDao.getByFormat("maven");
    assertThat(mavenPatterns.get(0).getNamespacePattern()).isEqualTo("org.sonatype");

    List<ProprietaryComponentNamePattern> npmPatterns = proprietaryDao.getByFormat("npm");
    assertThat(npmPatterns.get(0).getNamespacePattern()).isEqualTo("com.sonatype");
  }

  @Test
  void testRemoveProprietaryComponentNames() throws Exception {
    buildCreateRequest("maven2", List.of("org.sonatype")).post();
    buildCreateRequest("npm", List.of("com.sonatype")).post();

    List<ProprietaryComponentNamePattern> namespacePatterns = proprietaryDao.getAll();
    assertThat(namespacePatterns).hasSize(2);

    buildDeleteRequest("maven2");

    namespacePatterns = proprietaryDao.getAll();
    assertThat(namespacePatterns).hasSize(1);
    assertThat(namespacePatterns.get(0).getNamespacePattern()).isEqualTo("com.sonatype");
  }

  private HttpRequest buildCreateRequest(String format, List<String> namespaces) throws Exception {
    String path = ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH.replace("{format}", format);
    return ctx.restRequest() //
        .path(path) //
        .body(makeRequestBody(namespaces));
  }

  private void buildDeleteRequest(String format) throws Exception {
    String path = ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH.replace("{format}", format);
    ctx.restRequest() //
        .path(path)
        .delete();
  }

  private String makeRequestBody(List<String> namespaces) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(namespaces);
  }
}
