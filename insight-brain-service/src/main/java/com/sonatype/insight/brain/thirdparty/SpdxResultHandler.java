/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.model.ProjectScanItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.Read;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ModelObject;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.SpdxNoAssertionLicense;
import org.spdx.library.model.license.SpdxNoneLicense;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedThirdPartyIdentificationSource;

public class SpdxResultHandler
    extends SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(SpdxResultHandler.class);

  public SpdxResultHandler(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO)
  {
    super(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        multiLicenseDAO, thirdPartyVexDAO);
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      final ThirdPartyScanContent content,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (StringUtils.isNotBlank(content.getContent())) {
        SpdxDocument spdxDocument = parseSpdxContent(content);
        Bom targetBom = new Bom();
        List<ProjectScanItem> moduleDependencies = new ArrayList<>();
        log.info("Processing SPDX content for file: {}", content.getPath());
        processSpdxDocument(content.getPath(), spdxDocument, targetBom, thirdPartyFile, moduleDependencies);
        if (CollectionUtils.isEmpty(targetBom.getComponents())) {
          return new FilteredThirdPartyContent(content.getContent(), moduleDependencies);
        }
        else {
          return new FilteredThirdPartyContent(generateFilteredSbom(targetBom), moduleDependencies);
        }
      }
      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering SPDX file " + content.getPath(), e);
    }
  }

  private void processSpdxDocument(
      final String contentPath,
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final ThirdPartyFile thirdPartyFile,
      final List<ProjectScanItem> moduleDependencies) throws InvalidSPDXAnalysisException
  {
    String thirdPartyIdentificationSource =
        getTruncatedThirdPartyIdentificationSource(determineThirdPartyIdentificationSource(contentPath));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      String rootPackageId = collectFilteredMetadata(spdxDocument, targetBom);
      Map<String, String> componentRefs = new HashMap<>();
      processComponents(spdxDocument, targetBom, componentRefs, rootPackageId, thirdPartyIdentificationSource,
          thirdPartyFile, tx);
      tx.commit();
    }
    processDependencyGraph(spdxDocument, targetBom, moduleDependencies, thirdPartyFile);
  }

  private void processComponents(
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final Map<String, String> componentRefs,
      final String rootPackageId,
      final String thirdPartyIdentificationSource,
      final ThirdPartyFile thirdPartyFile,
      final TransactionContext tx) throws InvalidSPDXAnalysisException
  {
    List<? extends ModelObject> items = getSpdxPackages(spdxDocument);
    if (!items.isEmpty()) {
      Set<ComponentIdentifier> resolvedComponents = new HashSet<>();
      for (ModelObject item : items) {
        SpdxPackage spdxPackage = (SpdxPackage) item;
        processSpdxPackage(spdxPackage, thirdPartyFile.getId(), targetBom, thirdPartyIdentificationSource,
            resolvedComponents, componentRefs, rootPackageId, tx);
      }
    }
  }

  private List<? extends ModelObject> getSpdxPackages(final SpdxDocument spdxDocument)
      throws InvalidSPDXAnalysisException
  {
    return
        Read.getAllItems(spdxDocument.getModelStore(), spdxDocument.getDocumentUri(), SpdxConstants.CLASS_SPDX_PACKAGE)
            .collect(Collectors.toList());
  }

  private void processSpdxPackage(
      final SpdxPackage spdxPackage,
      final String thirdPartyFileId,
      final Bom targetBom,
      final String thirdPartyIdentificationSource,
      final Set<ComponentIdentifier> resolvedComponents,
      final Map<String, String> componentRefs,
      final String rootPackageId,
      final TransactionContext tx) throws InvalidSPDXAnalysisException
  {
    try {
      Pair<ComponentIdentifier, Component> resolvedComponent = getResolvedComponent(spdxPackage, rootPackageId);
      if (resolvedComponent != null) {
        getCpe(spdxPackage).ifPresent(cpe -> resolvedComponent.getRight().setCpe(cpe));
        getSwid(spdxPackage).ifPresent(swid -> resolvedComponent.getRight().setSwid(swid));
        ComponentIdentifier componentIdentifier = resolvedComponent.getLeft();
        if (componentIdentifier == null) {
          targetBom.addComponent(resolvedComponent.getRight());
        }
        else if (resolvedComponents.add(componentIdentifier)) {
          PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).ensureCompleteIdentifier();
          String coordinateId =
              saveComponent(thirdPartyFileId, thirdPartyIdentificationSource, spdxPackage, resolvedComponent, tx);
          if (StringUtils.isNotBlank(spdxPackage.getId())) {
            componentRefs.put(spdxPackage.getId(), coordinateId);
          }
          targetBom.addComponent(resolvedComponent.getRight());
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Component {} {} is missing coordinates. " + e.getMessage().replace(" for given format", ""),
          spdxPackage.getName(), spdxPackage.getVersionInfo().orElse(""), e);
    }
    catch (Exception e) {
      log.debug("Error processing component : {} {}", spdxPackage.getName(), spdxPackage.getVersionInfo().orElse(""),
          e);
    }
  }

  private String saveComponent(
      final String thirdPartyFileId,
      final String thirdPartyIdentificationSource,
      final SpdxPackage spdxPackage,
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final TransactionContext tx) throws InvalidSPDXAnalysisException, JsonProcessingException
  {
    Component component = resolvedComponent.getRight();
    ComponentIdentifier componentIdentifier;
    Optional<String> purlOptional = getPurl(spdxPackage);
    if (purlOptional.isPresent()) {
      PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(purlOptional.get());
      componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    }
    else {
      componentIdentifier = resolvedComponent.getLeft();
    }

    String hash = getOrCreateFakeHash(component, componentIdentifier);
    ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(hash, thirdPartyIdentificationSource,
        componentIdentifier.getFormat(), component.getName(), component.getVersion(), thirdPartyFileId);
    fileCoordinate.setPackageUrl(component.getPurl());
    if (component.getCpe() != null) {
      fileCoordinate.setCpe(component.getCpe());
    }
    if (component.getSwid() != null) {
      fileCoordinate.setSwid(ThirdPartyComponentDAO.MAPPER.writeValueAsString(component.getSwid()));
    }
    fileCoordinate.setIdentificationSources(SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE);
    thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
    saveLicenses(spdxPackage, fileCoordinate.getId(), component.getPurl(), tx);
    saveVulnerabilities(spdxPackage, fileCoordinate.getId(), component.getPurl(), tx);

    return fileCoordinate.getId();
  }

  private void saveVulnerabilities(
      final SpdxPackage spdxPackage,
      final String fileCoordinateId,
      final String packageUrl,
      final TransactionContext tx)
      throws InvalidSPDXAnalysisException
  {
    Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
    Set<String> processedVulnerabilityIds = new HashSet<>();
    for (ExternalRef externalRef : externalRefs) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        ThirdPartyCoordinateSecurity coordinateSecurity = parseVulnerability(externalRef, fileCoordinateId);
        if (coordinateSecurity != null) {
          if (processedVulnerabilityIds.add(coordinateSecurity.getRefId())) {
            thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
          }
          else {
            log.debug("Component with packageUrl {} has duplicate vulnerability with ID {}", packageUrl,
                coordinateSecurity.getRefId());
          }
        }
      }
    }
  }

  private static final Pattern CVE_LINK_PATTERN =
      Pattern.compile("https?://cve.mitre.org/cgi-bin/cvename.cgi\\?name=([^=]+)");

  private static final Pattern NVD_LINK_PATTERN = Pattern.compile("https?://nvd.nist.gov/vuln/detail/([^/]+)");

  private static final Pattern OSV_LINK_PATTERN = Pattern.compile("https?://osv.dev/vulnerability/([^/]+)");

  private static final Pattern SONATYPE_LINK_PATTERN = Pattern.compile("https?://.+/vln/(sonatype-[0-9-]+)");

  private ThirdPartyCoordinateSecurity parseVulnerability(
      final ExternalRef externalRef,
      final String fileCoordinateId)
      throws InvalidSPDXAnalysisException
  {
    String link = externalRef.getReferenceLocator();
    if (StringUtils.isBlank(link)) {
      return null;
    }
    Matcher matcher = CVE_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return createThirdPartyCoordinateSecurity(fileCoordinateId, matcher.group(1), link, "NVD");
    }
    matcher = NVD_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return createThirdPartyCoordinateSecurity(fileCoordinateId, matcher.group(1), link, "NVD");
    }
    matcher = OSV_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return createThirdPartyCoordinateSecurity(fileCoordinateId, matcher.group(1), link, "OSV");
    }
    matcher = SONATYPE_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return createThirdPartyCoordinateSecurity(fileCoordinateId, matcher.group(1), link, "SONATYPE");
    }
    return null;
  }

  private ThirdPartyCoordinateSecurity createThirdPartyCoordinateSecurity(
      final String fileCoordinateId,
      final String refId,
      final String link,
      final String source)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinateId, refId, null, link, 0.0f, null);
    coordinateSecurity.setVulnerabilitySource(source);
    coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
    return coordinateSecurity;
  }

  private void saveLicenses(
      final SpdxPackage spdxPackage,
      final String fileCoordinateId,
      final String packageUrl,
      final TransactionContext tx) throws InvalidSPDXAnalysisException
  {
    AnyLicenseInfo license = spdxPackage.getLicenseConcluded(); // preferred
    if (license instanceof SpdxNoAssertionLicense) {
      license = spdxPackage.getLicenseDeclared(); // fallback
    }
    if (license instanceof SpdxNoAssertionLicense) {
      log.debug("No licenses provided for Component with packageUrl {}", packageUrl);
    }
    else {
      if (license instanceof SpdxNoneLicense) {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
      else {
        Map<String, String> processedLicenses = new HashMap<>();
        spdxLicenseExpressionUtil.parseLicenses(license, processedLicenses, packageUrl);
        for (Entry<String, String> licenseEntry : processedLicenses.entrySet()) {
          ThirdPartyCoordinateLicense coordinateLicense =
              new ThirdPartyCoordinateLicense(fileCoordinateId, licenseEntry.getKey(), licenseEntry.getValue(), null);
          coordinateLicense.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateLicenseDAO.insert(tx, coordinateLicense);
        }
      }
    }
  }

  private Pair<ComponentIdentifier, Component> getResolvedComponent(
      final SpdxPackage spdxPackage,
      final String rootPackageId)
      throws InvalidSPDXAnalysisException, MalformedPackageURLException
  {
    Optional<String> purlOptional = getPurl(spdxPackage);
    try {
      if (purlOptional.isPresent()) {
        String packageUrl = purlOptional.get();
        PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(packageUrl);
        if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
          return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, false);
        }
        else {
          log.debug("PackageUrl is not valid {}", packageUrl);
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Invalid purl: {}", purlOptional.orElse(""));
    }
    Optional<String> cpeOptional = getCpe(spdxPackage);
    if (cpeOptional.isPresent()) {
      String cpe = cpeOptional.get();
      PackageUrlIdentifier packageUrlIdentifier = SbomIdentityUtils.buildPackageUrlFromCpe(cpe);
      if (packageUrlIdentifier != null) {
        return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, false);
      }
    }
    return processComponentFromHashOrCoordinates(spdxPackage, rootPackageId);
  }

  private Pair<ComponentIdentifier, Component> createComponent(
      final SpdxPackage spdxPackage,
      final PackageUrlIdentifier packageUrlIdentifier,
      final String rootPackageId,
      final boolean coordinates) throws InvalidSPDXAnalysisException
  {
    ComponentIdentifier componentIdentifier;
    Component component = new Component();
    component.setType(spdxPackage.getId().equals(rootPackageId) ? Type.APPLICATION : Type.LIBRARY);
    component.setBomRef(spdxPackage.getId());

    final Optional<String> sha1Optional = getChecksum(spdxPackage, ChecksumAlgorithm.SHA1);
    boolean hasHash = sha1Optional.isPresent();
    if (hasHash) {
      setHash(sha1Optional.get(), component);
    }
    if (!hasHash || !coordinates) {
      component.setPurl(ThirdPartyScanResultUtils.getTruncatedPurl(packageUrlIdentifier.getPackageUrl()));
    }
    componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    component.setName(packageUrlIdentifier.getName());
    component.setVersion(packageUrlIdentifier.getVersion());
    String namespace = packageUrlIdentifier.getNamespace();
    if (StringUtils.isNotBlank(namespace)) {
      component.setGroup(namespace);
    }
    // Process sha-256 only when BFS is enabled
    if (SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      getChecksum(spdxPackage, ChecksumAlgorithm.SHA256).ifPresent(
          v -> component.addHash(new Hash(Algorithm.SHA_256, v))
      );
    }
    return Pair.of(componentIdentifier, component);
  }

  /**
   * Collect the component hash (SHA1) and coordinates, if possible. The hash has priority, if exists.
   */
  private Pair<ComponentIdentifier, Component> processComponentFromHashOrCoordinates(
      final SpdxPackage spdxPackage,
      final String rootPackageId)
      throws InvalidSPDXAnalysisException, MalformedPackageURLException
  {
    boolean isRootPackage = spdxPackage.getId().equals(rootPackageId);

    // The hash has priority over coordinates
    Optional<String> sha1Optional = getChecksum(spdxPackage, ChecksumAlgorithm.SHA1);
    if (sha1Optional.isPresent()) {
      Component component = new Component();
      component.setType(isRootPackage ? Type.APPLICATION : Type.LIBRARY);
      component.setBomRef(spdxPackage.getId());
      spdxPackage.getName().ifPresent(component::setName);
      setHash(sha1Optional.get(), component);
      return Pair.of(null, component);
    }

    // try using the SPDX package name and version
    String name = spdxPackage.getName().orElse(MISSING_COMPONENT_NAME);
    String version = spdxPackage.getVersionInfo().orElse("");
    if (StringUtils.isNotBlank(version)) {
      PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(
          getPackageUrlFromCoordinates(name, version, isRootPackage));
      return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, true);
    }
    else {
      log.debug("Component with invalid information, name {}", name);
    }
    return null;
  }

  private String getPackageUrlFromCoordinates(String  name, String version, boolean isRootPackage)
      throws MalformedPackageURLException
  {
    String group = null;
    if (name.contains(":")) {
      // some SPDX generators set the name value as 'group:name' because there's no other placeholder for the group
      final String[] parts = name.split(":");
      if (parts.length == 2) {
        group = parts[0];
        name = parts[1];
      }
    }
    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(isRootPackage ? Type.APPLICATION.getTypeName() : Type.LIBRARY.getTypeName())
        .withName(name)
        .withVersion(version);
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    return packageURLBuilder.build().toString();
  }

  @VisibleForTesting
  void processDependencyGraph(
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (CollectionUtils.isNotEmpty(targetBom.getComponents())) {
        List<Dependency> bomDependencies = getDependencyList(spdxDocument);
        if (CollectionUtils.isNotEmpty(bomDependencies)) {
          Pair<Dependency, String> rootModuleAndRef =
              resolveRootModuleAndRef(bomDependencies, targetBom);
          if (rootModuleAndRef != null) {
            processValidDependencyGraph(rootModuleAndRef, thirdPartyFile, targetBom,
                moduleDependencies, bomDependencies);
          }
          else {
            log.debug("Unable to process dependency graph. The root component of the bom {} cannot be determined",
                thirdPartyFile.getFilename());
          }
        }
      }
    }
    catch (Exception e) {
      log.warn("There was an error processing dependency graph", e);
    }
  }

  private List<Dependency> getDependencyList(final SpdxDocument spdxDocument) throws InvalidSPDXAnalysisException {
    Map<String, Dependency> dependencyMap = new HashMap<>();

    // relationships are attached to packages in the SPDX object model
    List<? extends ModelObject> items = getSpdxPackages(spdxDocument);
    for (ModelObject item : items) {
      SpdxPackage spdxPackage = (SpdxPackage) item;
      Collection<Relationship> relationships = spdxPackage.getRelationships();
      for (Relationship relationship : relationships) {
        if (relationship.getRelationshipType() == RelationshipType.DESCRIBES ||
            !relationship.getRelatedSpdxElement().isPresent()) {
          continue;
        }
        String refId1 = spdxPackage.getId();
        String refId2 = relationship.getRelatedSpdxElement().get().getId();
        Dependency dependency1 = dependencyMap.computeIfAbsent(refId1, Dependency::new);
        Dependency dependency2 = dependencyMap.computeIfAbsent(refId2, Dependency::new);

        switch (relationship.getRelationshipType()) {
          case DEPENDS_ON:
            dependency1.addDependency(dependency2);
            break;
          case DEPENDENCY_OF:
          case BUILD_DEPENDENCY_OF:
          case DEV_DEPENDENCY_OF:
          case OPTIONAL_DEPENDENCY_OF:
          case PROVIDED_DEPENDENCY_OF:
          case RUNTIME_DEPENDENCY_OF:
          case TEST_DEPENDENCY_OF:
            dependency2.addDependency(dependency1);
            break;
          default:
        }
      }
    }
    return new ArrayList<>(dependencyMap.values());
  }

  /**
   * Collects metadata
   * @return the ID of the root package, if any; otherwise, it returns an empty string.
   */
  private String collectFilteredMetadata(final SpdxDocument spdxDocument, final Bom targetBom)
      throws InvalidSPDXAnalysisException
  {
    String rootElementId = "";
    Metadata metadata = new Metadata();

    final Collection<SpdxElement> describes = spdxDocument.getDocumentDescribes();
    if (!describes.isEmpty()) {
      final SpdxElement rootElement = describes.iterator().next();
      if (rootElement instanceof SpdxPackage) {
        SpdxPackage spdxPackage = (SpdxPackage) rootElement;
        rootElementId = spdxPackage.getId();
        Component component = new Component();
        component.setType(Type.APPLICATION);
        component.setBomRef(rootElementId);
        spdxPackage.getName().ifPresent(name -> {
          if (name.contains(":")) {
            final String[] parts = name.split(":");
            component.setGroup(parts[0]);
            component.setName(parts[1]);
          }
          else {
            component.setName(name);
          }
        });
        spdxPackage.getVersionInfo().ifPresent(component::setVersion);
        getPurl(spdxPackage).ifPresent(component::setPurl);
        metadata.setComponent(component);
      }
    }
    if (spdxDocument.getCreationInfo() != null) {
      final String created = spdxDocument.getCreationInfo().getCreated();
      if (StringUtils.isNotBlank(created)) {
        try {
          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
          metadata.setTimestamp(dateFormat.parse(created));
        }
        catch (ParseException e) {
          log.warn("Cannot parse creation date: " + created);
        }
      }
    }
    targetBom.setMetadata(metadata);
    return rootElementId;
  }

  private Optional<String> getPurl(final SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    final Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
    for (ExternalRef externalRef : externalRefs) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.PACKAGE_MANAGER &&
          externalRef.getReferenceType().getIndividualURI().endsWith("/purl")) {
        return Optional.of(externalRef.getReferenceLocator());
      }
    }
    return Optional.empty();
  }

  private Optional<String> getCpe(final SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    final Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
    for (ExternalRef externalRef : externalRefs) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        if (referenceType.endsWith("cpe23Type") || referenceType.endsWith("cpe22Type") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                externalRef.getReferenceLocator().startsWith("cpe"))) {
          return Optional.of(externalRef.getReferenceLocator());
        }
      }
    }
    return Optional.empty();
  }

  private static final String SWID_URI_PREFIX = "swid:";

  private Optional<Swid> getSwid(final SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        String referenceLocator = externalRef.getReferenceLocator();
        if (referenceType.endsWith("swid") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                referenceLocator.startsWith(SWID_URI_PREFIX))) {
          Swid swid = new Swid();
          String tagId = referenceLocator.startsWith(SWID_URI_PREFIX) ? referenceLocator.substring(
              SWID_URI_PREFIX.length()) : referenceLocator;
          swid.setTagId(tagId);
          return Optional.of(swid);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> getChecksum(final SpdxPackage spdxPackage, ChecksumAlgorithm algorithm)
      throws InvalidSPDXAnalysisException
  {
    final Collection<Checksum> checksums = spdxPackage.getChecksums();
    for (Checksum checksum : checksums) {
      if (checksum.getAlgorithm() == algorithm) {
        return Optional.of(checksum.getValue());
      }
    }
    return Optional.empty();
  }

  @VisibleForTesting
  @Override
  String determineThirdPartyIdentificationSource(final String contentPath) {
    String fileName = StringUtils.contains(contentPath, "/") ?
        StringUtils.substringAfterLast(contentPath, "/") : contentPath;
    String thirdPartyIdentificationSource = RegExUtils.removePattern(fileName, "\\.(?i)spdx\\.(xml|json)(?i)$");
    if (StringUtils.isBlank(thirdPartyIdentificationSource) ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.xml") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.json")) {
      return "Third-Party";
    }
    else {
      return thirdPartyIdentificationSource;
    }
  }

  private SpdxDocument parseSpdxContent(final ThirdPartyScanContent content)
      throws IOException, InvalidSPDXAnalysisException
  {
    String extension = FilenameUtils.getExtension(content.getPath());
    SbomFormat sbomFormat = SbomFormat.forString(extension.toLowerCase(Locale.ROOT));
    return ThirdPartyUtils.parseAndValidateSpdx(content.getContent(), sbomFormat);
  }
}
