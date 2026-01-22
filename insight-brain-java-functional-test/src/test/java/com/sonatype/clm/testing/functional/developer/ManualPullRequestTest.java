/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.developer;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO.ToVersionData;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CreatePRModal;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventOrchestrator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.gitlab.GitLabServer;
import com.sonatype.insight.test.gitlab.GitLabServerRule;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ManualPullRequestTest
    extends AbstractFunctionalTest
{
  @ClassRule
  public static GitLabServerRule rule = new GitLabServerRule();

  private static GitLabServer gitLabServer;

  private static GitLabApi gitLabApi;

  @BeforeClass
  public static void beforeClass() {
    gitLabServer = rule.getGitLabServer();
    gitLabApi = gitLabServer.getGitLabApi();
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path repoDir;

  private SourceControlEventOrchestrator sourceControlEventOrchestrator;

  @Before
  public void before() throws Exception {
    repoDir = temporaryFolder.newFolder().toPath();
    commitAndPushPom();
    setupPolicies();
    mockRemediationData();

    sourceControlEventOrchestrator = lookup(SourceControlEventOrchestrator.class);
    sourceControlEventOrchestrator.disableForTesting = false;
    // Reduce startup delay from 30s to 1s and interval from 15s to 2s for faster test execution
    sourceControlEventOrchestrator.setEventProcessingScheduleTimesForTesting(1, 2);
    sourceControlEventOrchestrator.register();
  }

  @After
  public void after() {
    sourceControlEventOrchestrator.deregister();
  }

  @Test
  public void testGitLabManualPullRequest() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    //add inner source data
    ComponentIdentifier innersourceDirectComponent =
        ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.3.1", "", "jar");
    PackageUrlIdentifier versionlessPurl = InnerSourceUtils.getVersionlessPackageUrl(innersourceDirectComponent);
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(versionlessPurl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.4.0", StageTypes.BUILD.getId());
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitLabServer.getProject().getWebUrl(),
        lookup(PasswordHandler.class).encryptPassword(gitLabServer.getAdminToken()),
        SourceControlProvider.GITLAB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);
    String scanId = "scanId";
    evaluateScan(application, scanId);
    refresh();

    // Open the priorities page
    refreshOrOpen(PrioritiesPage.url(application.getPublicId(), scanId));

    // Check our expected target row exists
    PrioritiesPage page = new PrioritiesPage();
    page.prioritiesTableCell(7, 0).shouldHave(text("8"));
    page.prioritiesTableCell(7, 1).shouldHave(text("Dapache-httpclient : commons-httpclient : 3.1"));
    page.prioritiesTableCell(7, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(7, 3).shouldHave(text("-"));
    page.prioritiesTableCell(7, 4).shouldHave(text("Upgrade to 3.2"));

    // Click the "Create PR" button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(7).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // Check the create PR modal has opened
    CreatePRModal createPRModal = new CreatePRModal();
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump commons-httpclient to 3.2"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("3.2"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("master"));

    // Click create
    createPRModal.createPullRequestModalCreateButton().shouldBe(visible, enabled).click();

    // Wait for the PR to be created
    await().atMost(10, TimeUnit.MINUTES).until(
        () -> !gitLabApi.getMergeRequestApi().getMergeRequests(gitLabServer.getProject().getId()).isEmpty()
    );

    // Check the PR is correct
    MergeRequest mergeRequest =
        gitLabApi.getMergeRequestApi().getMergeRequests(gitLabServer.getProject().getId()).get(0);
    assertThat(mergeRequest.getTitle()).isEqualTo("Bump commons-httpclient to 3.2");
  }

  private void commitAndPushPom() throws Exception {
    Path source = Paths.get(getClass().getResource("/ManualPullRequestTest/pom.xml").toURI());
    Path target = Files.copy(source, repoDir.resolve(source.getFileName()));
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      git.add().addFilepattern(target.getFileName().toString()).call();
      git.commit().setMessage("Added " + target.getFileName()).call();
      git.remoteAdd()
          .setName("origin")
          .setUri(new URIish(gitLabServer.getProject().getHttpUrlToRepo()))
          .call();
      git.push()
          .setRemote("origin")
          .setCredentialsProvider(
              new UsernamePasswordCredentialsProvider(gitLabServer.getAdminUsername(), gitLabServer.getAdminPassword()))
          .add("main")
          .call();
    }
  }

  private void setupPolicies() throws Exception {
    try (var referencePolicyStream = getClass().getResourceAsStream("/reference-policies-v3-with-build-fail.json")) {
      PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyStream, PolicyExportResult.class);
      lookup(PolicyImportExport.class).importOrganization(
          lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID), referencePolicies);
    }
  }

  private void evaluateScan(final Application app, final String scanId) throws Exception {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-with-dependency-tree", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator =
        new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
  }

  private void mockRemediationData() throws Exception {
    ComponentIdentifier componentFromReport =
        ComponentIdentifier.createMavenCoordinates("apache-httpclient", "commons-httpclient", "3.1", "", "jar");
    ComponentIdentifier componentNonFailing =
        ComponentIdentifier.createMavenCoordinates("apache-httpclient", "commons-httpclient", "3.2", "", "jar");

    ComponentDetails fromReport = createComponentDetailsForSecurityViolation(componentFromReport);
    ComponentDetails nonFailing = createComponentDetailsForNoViolation(componentNonFailing);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(List.of(fromReport, nonFailing));

    testCLMServer.getHdsServer().respondWith(detailsList).atUri("rest/ci/componentDetails/list");
    testCLMServer.getHdsServer().respondWith(List.of()).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
    testCLMServer.getHdsServer().respondWith(ComponentSummary.create(true)).atUri(
        UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier",
                URLEncoder.encode(new ObjectMapper().writeValueAsString(componentFromReport),
                    StandardCharsets.UTF_8))
            .build()
    );
    testCLMServer.getHdsServer().respondWith(new ComponentDependenciesDTO(Map.of(), Map.of()))
        .atUri("rest/component/dependencies");

    VersionScoringDTO versionScoringDTO = new VersionScoringDTO();
    versionScoringDTO.setComponentIdentifier(componentFromReport);
    versionScoringDTO.setVersionScore(0);
    versionScoringDTO.setMaxSeverity(5.0d);
    VersionScoringDTO.ToVersionData toVersionData = new ToVersionData();
    toVersionData.setBreakingChangeCount(0);
    versionScoringDTO.setToVersionsNonBreaking(Map.of("3.2", toVersionData));
    testCLMServer.getHdsServer().respondWith(new VersionScoringDTO[]{versionScoringDTO})
        .atUri("rest/component/version-scoring/list");
  }

  private ComponentDetails createComponentDetailsForNoViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setBreakingChangesCount(0);
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails
        .setSecurityVulnerabilities(List.of(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }
}
