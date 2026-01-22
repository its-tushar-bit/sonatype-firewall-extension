/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.PackageURLBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class DocumentBuilderHelper
{
  private static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX_EVAL = "AdvancedSearch.createSearchIndex.eval";

  private static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX_COMPONENT =
      "AdvancedSearch.createSearchIndex.component";

  private static final int EVAL_THREADS_MIN = 1;

  private static final int EVAL_THREADS_MAX = Integer.MAX_VALUE;

  private static final int EVAL_THREADS_DEFAULT = 8;

  private static final int COMPONENT_THREADS_MIN = 1;

  private static final int COMPONENT_THREADS_MAX = Integer.MAX_VALUE;

  private static final int COMPONENT_THREADS_DEFAULT = 8;

  private static final Logger log = LoggerFactory.getLogger(DocumentBuilderHelper.class);

  private final LabelDAO labelDAO;

  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final TagDAO tagDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ReportService reportService;

  private final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  private final TenantReference<TenantThreadPoolExecutor> evalExecutors;

  private final TenantReference<TenantThreadPoolExecutor> componentExecutors;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public DocumentBuilderHelper(
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final TagDAO tagDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      final ComponentLoaderFactory componentLoaderFactory,
      final ReportService reportService,
      final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher,
      final ShutdownHandler shutdownHandler)
  {
    this.labelDAO = labelDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.tagDAO = tagDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
    this.componentLoaderFactory = componentLoaderFactory;
    this.reportService = reportService;
    this.vulnerabilityDescriptionFetcher = vulnerabilityDescriptionFetcher;
    this.evalExecutors = new TenantReference<>();
    this.componentExecutors = new TenantReference<>();
    this.shutdownHandler = shutdownHandler;
  }

  // Visible for testing
  public ExecutorService getEvalExecutor() {
    return evalExecutors.computeIfAbsent(tenant -> {
      int evalThreadCount = DefaultExecutorThreadPools.getThreadCount(
          EVAL_THREADS_MIN,
          EVAL_THREADS_MAX,
          EVAL_THREADS_DEFAULT,
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX_EVAL
      );
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          evalThreadCount,
          evalThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("DocumentBuilderHelper-eval-%d").build(),
          new AbortPolicy(),
          "advanced_search_indexing_eval",
          getClass().getSimpleName()
      );
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  // Visible for testing
  public ExecutorService getComponentExecutor() {
    return componentExecutors.computeIfAbsent(tenant -> {
      int componentThreadCount = DefaultExecutorThreadPools.getThreadCount(
          COMPONENT_THREADS_MIN,
          COMPONENT_THREADS_MAX,
          COMPONENT_THREADS_DEFAULT,
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX_COMPONENT
      );
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          componentThreadCount,
          componentThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("DocumentBuilderHelper-component-%d").build(),
          new AbortPolicy(),
          "advanced_search_indexing_component",
          getClass().getSimpleName()
      );
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  public List<Document> buildOrganizationDocs(
      IndexingContext indexingContext,
      Collection<Organization> organizations)
  {
    return organizations.stream().map(org -> buildDocument(indexingContext, org)).collect(toList());
  }

  public Document buildDocument(
      @SuppressWarnings("unused") IndexingContext indexingContext,
      Organization organization)
  {
    return new DocumentBuilder(ItemType.ORGANIZATION)
        .setOwner(organization)
        .build();
  }

  public List<Document> buildApplicationDocs(
      IndexingContext indexingContext,
      Collection<Application> applications)
  {
    return applications.stream().map(app -> buildDocument(indexingContext, app)).collect(toList());
  }

  public Document buildDocument(IndexingContext indexingContext, Application application) {
    return new DocumentBuilder(ItemType.APPLICATION)
        .setOwner(application)
        .setOwner(indexingContext.getOwner(application.getOrganizationId()))
        .build();
  }

  public List<Document> buildTagDocs(IndexingContext indexingContext) {
    return tagDAO.getAll().stream().map(tag -> buildDocument(indexingContext, tag)).collect(toList());
  }

  public Document buildDocument(IndexingContext indexingContext, Tag tag) {
    return new DocumentBuilder(ItemType.APPLICATION_CATEGORY) //
        .setApplicationCategoryId(tag.getId()) //
        .setApplicationCategoryName(tag.getName()) //
        .setApplicationCategoryColor(tag.getColor()) //
        .setApplicationCategoryDescription(tag.getDescription()) //
        .setOwner(indexingContext.getOwner(tag.getOrganizationId())) //
        .build();
  }

  public List<Document> buildLabelDocs(IndexingContext indexingContext) {
    return labelDAO.getAll().stream().map(label -> buildDocument(indexingContext, label)).collect(toList());
  }

  public Document buildDocument(IndexingContext indexingContext, Label label) {
    return new DocumentBuilder(ItemType.COMPONENT_LABEL) //
        .setComponentLabelId(label.getId()) //
        .setComponentLabelName(label.getLabel()) //
        .setComponentLabelColor(label.getColor()) //
        .setComponentLabelDescription(label.getDescription()) //
        .setOwner(indexingContext.getOwner(label.getOwnerId())) //
        .build();
  }

  public List<Document> buildPolicyDocs(IndexingContext indexingContext) {
    return policyDAO.getAll().stream().map(policy -> buildDocument(indexingContext, policy)).collect(toList());
  }

  public Document buildDocument(IndexingContext indexingContext, Policy policy) {
    return new DocumentBuilder(ItemType.POLICY) //
        .setPolicyId(policy.getId()) //
        .setPolicyName(policy.getName()) //
        .setPolicyThreatCategory(policy.getThreatCategory()) //
        .setPolicyThreatLevel(policy.getThreatLevel()) //
        .setOwner(indexingContext.getOwner(policy.getOwnerId())) //
        .build();
  }

  public List<Document> buildSbomDocs(IndexingContext indexingContext) {
    return thirdPartySbomMetadataDAO.getAll().stream()
        .map(sbomMetadata -> buildDocument(indexingContext, sbomMetadata))
        .collect(toList());
  }

  public Document buildDocument(IndexingContext indexingContext, ThirdPartySbomMetadata sbomMetadata) {
    Owner owner = indexingContext.getOwner(sbomMetadata.getApplicationId());
    if (!(owner instanceof Application)) {
      throw new IllegalStateException("ThirdPartySbomMetadata " + sbomMetadata.getId() + " has owner that is not " +
          "of type Application: " + owner);
    }

    Application application = (Application) owner;
    Organization org = (Organization) indexingContext.getOwner(application.getOrganizationId());

    return new DocumentBuilder(ItemType.SBOM_METADATA)
        .setOwner(application)
        .setOwner(org)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .build();
  }

  public List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application)
  {
    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    return buildApplicationSVDocs(indexingContext, organization, application,
        ImmutableMap.of(organization, parentOrganizations));
  }

  public List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {
    return StageTypes.getAll().stream()
        .map(stageType -> CompletableFuture.supplyAsync(
            () -> buildApplicationStageSVDocs(
                indexingContext,
                organization,
                application,
                stageType,
                parentOrgsMap.get(organization)
            ),
            getEvalExecutor()
        ))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }
        ));
  }

  public List<Document> buildApplicationStageSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      Collection<Organization> parentOrganizations)
  {
    PolicyEvaluation latestPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageType.getId());
    if (latestPolicyEvaluation == null) {
      return Collections.emptyList();
    }
    String scanId = latestPolicyEvaluation.getScanId();
    ApplicationReport applicationReport = null;
    try {
      applicationReport = reportService.getReport(application.getId(), scanId);
      if (!applicationReport.exists()) {
        return Collections.emptyList();
      }
      ReportEntry licenseReportEntry = applicationReport.getEntry(LICENSES_JSON.getName());
      ReportEntry securityReportEntry = applicationReport.getEntry(SECURITY_JSON.getName());
      ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
      ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null ||
          dependenciesReportEntry == null) {
        return Collections.emptyList();
      }

      return componentLoaderFactory.createComponentLoader(application)
          .getAll(
              licenseReportEntry.buf,
              securityReportEntry.buf,
              bomReportEntry.buf,
              dependenciesReportEntry.buf
          )
          .stream()
          .map(component -> CompletableFuture.supplyAsync(
              () -> buildApplicationComponentVulnerabilityDocuments(
                  indexingContext,
                  organization,
                  parentOrganizations,
                  application,
                  stageType,
                  scanId,
                  component
              ),
              getComponentExecutor()
          ))
          .collect(Collectors.collectingAndThen(
              Collectors.toList(),
              futures -> {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();
              }
          ));
    }
    catch (IOException | NotFoundException e) {
      log.error(e.getMessage(), e);
    }
    catch (UncheckedIOException e) {
      log.error("Error parsing report files at {}",
          applicationReport == null ? "Unknown" : applicationReport.getLocation(), e);
    }
    return Collections.emptyList();
  }

  public List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    if (CollectionUtils.isNotEmpty(component.getSecurityVulnerabilities())) {
      return component.getSecurityVulnerabilities().stream()
          .map(vulnerability -> buildDocument(indexingContext, application, stageType, reportId, component,
              vulnerability, parentOrganizations))
          .collect(toList());
    }
    else if (component.getComponentIdentifier() != null) {
      return Collections.singletonList(
          buildDocument(organization, parentOrganizations, application, stageType, reportId, component));
    }
    else {
      return Collections.emptyList();
    }
  }

  public Document buildDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    return new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT) //
        .setOwner(application) //
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType) //
        .setReportId(reportId) //
        .setComponentHash(component.getHash()) //
        .setComponentFormat(component.getComponentIdentifier().getFormat()) //
        .setComponentCoordinates(component) //
        .setComponentName(component.getDisplayNameFromIdentifier()) //
        .setParentOrganizationNames(parentOrganizations) //
        .setParentOrganizationIds(parentOrganizations) //
        .build();
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      SecurityVulnerability vulnerability,
      Collection<Organization> parentOrganizations)
  {
    return new DocumentBuilder(ItemType.SECURITY_VULNERABILITY) //
        .setOwner(application) //
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organizationDAO.getById(application.getOrganizationId()).getName())
        .setPolicyEvaluationStage(stageType) //
        .setReportId(reportId) //
        .setComponentHash(component.getHash()) //
        .setComponentFormat(component.getComponentIdentifier().getFormat()) //
        .setComponentCoordinates(component) //
        .setComponentName(component.getDisplayNameFromIdentifier()) //
        .setVulnerabilityId(vulnerability.getRefId()) //
        .setVulnerabilitySeverity(vulnerability.getSeverity()) //
        .setVulnerabilityStatus(vulnerability.getStatus().getName()) //
        .setVulnerabilityDescription(getDescription(indexingContext.getVulnDescByVulnId(), vulnerability)) //
        .setParentOrganizationNames(parentOrganizations) //
        .setParentOrganizationIds(parentOrganizations) //
        .build();
  }

  private String getDescription(
      final Map<String, String> vulnDescByVulnId,
      final SecurityVulnerability vulnerability)
  {
    if (IdentificationSource.SONATYPE_IAC.getId().equals(vulnerability.getSource())
        || "Sonatype-C".equals(vulnerability.getSource())) {
      return getThirdPartyVulnerabilityDescription(vulnDescByVulnId, vulnerability);
    }
    try {
      return vulnDescByVulnId.computeIfAbsent(vulnerability.getRefId(),
          vulnerabilityDescriptionFetcher::getVulnerabilityDescription);
    }
    catch (NotFoundException notFoundException) {
      log.warn(notFoundException.getMessage());
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return "";
  }

  private String getThirdPartyVulnerabilityDescription(
      final Map<String, String> vulnDescByVulnId,
      final SecurityVulnerability vulnerability)
  {
    if (vulnDescByVulnId.get(vulnerability.getRefId()) != null) {
      return vulnDescByVulnId.get(vulnerability.getRefId());
    }

    ThirdPartyVulnerability thirdPartyVulnerability = thirdPartyVulnerabilityDAO.getByRefId(vulnerability.getRefId());
    if (thirdPartyVulnerability == null || thirdPartyVulnerability.getDescription() == null) {
      log.warn("Description not found for vulnerability with refid: {}", vulnerability.getRefId());
      return "";
    }
    else {
      String description = thirdPartyVulnerability.getDescription();
      vulnDescByVulnId.put(vulnerability.getRefId(), description);
      return description;
    }
  }

  public List<Document> buildSbomSVDocs(
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {
    return thirdPartySbomMetadataDAO.getByApplicationId(application.getId())
        .stream()
        .map(sbomMetadata -> CompletableFuture.supplyAsync(
            () -> buildSbomVersionSVDocs(
                organization,
                application,
                sbomMetadata,
                parentOrgsMap.get(organization)
            ),
            getEvalExecutor()
        ))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }
        ));
  }

  public List<Document> buildSbomVersionSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations)
  {
    return thirdPartyFileCoordinateDAO.getBySbomMetadataId(sbomMetadata.getId())
        .stream()
        .map(fileCoord -> CompletableFuture.supplyAsync(
            () -> buildSbomFileCoordinateSVDocs(
                organization,
                application,
                sbomMetadata,
                parentOrganizations,
                fileCoord
            ),
            getComponentExecutor()
        ))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }
        ));
  }

  public List<Document> buildSbomFileCoordinateSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations,
      ThirdPartyFileCoordinate thirdPartyFileCoord)
  {
    List<ThirdPartyCoordinateSecurity> vulns = thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(
        Collections.singletonList(thirdPartyFileCoord.getId())
    );

    if (CollectionUtils.isNotEmpty(vulns)) {
      return vulns.stream()
          .map(vuln -> buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, vuln,
              parentOrganizations))
          .collect(toList());
    }
    else if (thirdPartyFileCoord.getPackageUrl() != null) {
      return Collections.singletonList(
          buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, parentOrganizations));
    }
    else {
      return Collections.emptyList();
    }
  }

  public Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      Collection<Organization> parentOrganizations)
  {
    DocumentBuilder documentBuilder = new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT);
    ComponentIdentifier componentIdentifier = tryConvert(thirdPartyFileCoord);
    if (componentIdentifier != null) {
      documentBuilder
          .setComponentFormat(componentIdentifier.getFormat())
          .setComponentCoordinates(componentIdentifier)
          .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    }
    return documentBuilder
        .setOwner(application)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setComponentHash(thirdPartyFileCoord.getHash())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .build();
  }

  public Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      Collection<Organization> parentOrganizations)
  {
    DocumentBuilder documentBuilder = new DocumentBuilder(ItemType.SECURITY_VULNERABILITY);
    ComponentIdentifier componentIdentifier = tryConvert(thirdPartyFileCoord);
    if (componentIdentifier != null) {
      documentBuilder
          .setComponentFormat(componentIdentifier.getFormat())
          .setComponentCoordinates(componentIdentifier)
          .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    }
    return documentBuilder
        .setOwner(application)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setComponentHash(thirdPartyFileCoord.getHash())
        .setComponentCoordinates(componentIdentifier)
        .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString())
        .setVulnerabilityId(thirdPartyCoordinateSecurity.getRefId())
        .setVulnerabilitySeverity(
            BigDecimal.valueOf(thirdPartyCoordinateSecurity.getSeverity()).setScale(2, RoundingMode.UNNECESSARY)
                .floatValue())
        .setVulnerabilityDescription(thirdPartyCoordinateSecurity.getDescription())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .build();
  }

  private ComponentIdentifier tryConvert(ThirdPartyFileCoordinate thirdPartyFileCoordinate) {
    PackageUrlIdentifier packageUrlIdentifier = null;

    // First try the pURL, if it exists
    if (thirdPartyFileCoordinate.getPackageUrl() != null) {
      try {
        packageUrlIdentifier =
            new PackageUrlIdentifier(thirdPartyFileCoordinate.getPackageUrl());
      }
      catch (Exception e) {
        log.error("Unable to create PackageUrlIdentifier from ThirdPartyFileCoordinate with id: '{}', and pURL: '{}'.",
            thirdPartyFileCoordinate.getId(), thirdPartyFileCoordinate.getPackageUrl());
      }
    }

    // Second, try the ThirdPartyFileCoordinate format, name, and version (which should always exist)
    if (packageUrlIdentifier == null) {
      try {
        packageUrlIdentifier = new PackageUrlIdentifier(PackageURLBuilder.aPackageURL()
            .withType(thirdPartyFileCoordinate.getFormat())
            .withName(thirdPartyFileCoordinate.getName())
            .withVersion(thirdPartyFileCoordinate.getVersion()).build()
            .canonicalize());
      }
      catch (Exception e) {
        log.warn("Unable to create PackageUrlIdentifier from ThirdPartyFileCoordinate with " +
                "id: '{}', format: '{}', name: '{}', and version: '{}'.", thirdPartyFileCoordinate.getId(),
            thirdPartyFileCoordinate.getFormat(), thirdPartyFileCoordinate.getName(),
            thirdPartyFileCoordinate.getVersion());
      }
    }

    // If one of the above worked, try to convert it to a component identifier and return it
    if (packageUrlIdentifier != null) {
      try {
        return packageUrlIdentifier.toComponentIdentifier();
      }
      catch (Exception e) {
        log.error("Unable to convert PackageUrlIdentifier from ThirdPartyFileCoordinate with id: " +
                "'{}', and pURL: '{}' to ComponentIdentifier.", thirdPartyFileCoordinate.getId(),
            packageUrlIdentifier.getPackageUrl());
      }
    }

    // If none of the above worked, log and return null
    log.error("Unable to create ComponentIdentifier from ThirdPartyFileCoordinate with id: " +
            "'{}', pURL: '{}', format: '{}', name: '{}', version: '{}'.",
        thirdPartyFileCoordinate.getId(), thirdPartyFileCoordinate.getPackageUrl(),
        thirdPartyFileCoordinate.getFormat(), thirdPartyFileCoordinate.getName(),
        thirdPartyFileCoordinate.getVersion());

    return null;
  }
}
