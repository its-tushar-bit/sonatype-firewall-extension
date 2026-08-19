/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.VulnerabilityUrlBuilder;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Ancestors;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Scope;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Composition;
import org.cyclonedx.model.Composition.Aggregate;
import org.cyclonedx.model.Copyright;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Descendants;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalContact;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Variants;
import org.cyclonedx.model.license.Expression;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.IModelCopyManager;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.Checksum;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.ReferenceType;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.SpdxPackage.SpdxPackageBuilder;
import org.spdx.library.model.v2.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.library.model.v2.enumerations.Purpose;
import org.spdx.library.model.v2.enumerations.ReferenceCategory;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ConjunctiveLicenseSet;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.spdx.library.model.v2.license.InvalidLicenseStringException;
import org.spdx.core.DefaultStoreNotInitializedException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.ListedLicenses;
import org.spdx.library.model.v2.license.SpdxNoAssertionLicense;
import org.spdx.library.model.v2.license.SpdxNoneLicense;
import org.spdx.library.referencetype.ListedReferenceTypes;
import org.spdx.storage.IModelStore;
import org.spdx.storage.IModelStore.IdType;

@Named
@org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CycloneDxToSpdxExporter
    extends AbstractSpdxExporter
{
  private static final String REFERENCE_SITE_MAVEN_CENTRAL = "http://repo1.maven.org/maven2/";

  private static final String REFERENCE_SITE_NPM = "https://www.npmjs.com/";

  private static final String REFERENCE_SITE_NUGET = "https://www.nuget.org/";

  private static final String REFERENCE_SITE_BOWER = "http://bower.io/";

  private static final String NULL_SHA1_VALUE = "0000000000000000000000000000000000000000";

  private static final Map<String, ChecksumAlgorithm> CDX_ALGORITHM_TO_SPDX_ALGORITHM;

  static {
    Map<String, ChecksumAlgorithm> algToSpdx = new HashMap<>();
    algToSpdx.put("MD5", ChecksumAlgorithm.MD5);
    algToSpdx.put("SHA-1", ChecksumAlgorithm.SHA1);
    algToSpdx.put("SHA-256", ChecksumAlgorithm.SHA256);
    algToSpdx.put("SHA-384", ChecksumAlgorithm.SHA384);
    algToSpdx.put("SHA-512", ChecksumAlgorithm.SHA512);
    algToSpdx.put("SHA3-256", ChecksumAlgorithm.SHA3_256);
    algToSpdx.put("SHA3-384", ChecksumAlgorithm.SHA3_384);
    algToSpdx.put("SHA3-512", ChecksumAlgorithm.SHA3_512);
    algToSpdx.put("BLAKE2b-256", ChecksumAlgorithm.BLAKE2b_256);
    algToSpdx.put("BLAKE2b-384", ChecksumAlgorithm.BLAKE2b_384);
    algToSpdx.put("BLAKE2b-512", ChecksumAlgorithm.BLAKE2b_512);
    algToSpdx.put("BLAKE3", ChecksumAlgorithm.BLAKE3);
    CDX_ALGORITHM_TO_SPDX_ALGORITHM = Collections.unmodifiableMap(algToSpdx);
  }

  private static Map<Component.Type, Purpose> COMPONENT_TYPE_TO_PURPOSE;

  static {
    Map<Component.Type, Purpose> compPurpose = new HashMap<>();
    compPurpose.put(Component.Type.APPLICATION, Purpose.APPLICATION);
    compPurpose.put(Component.Type.CONTAINER, Purpose.CONTAINER);
    compPurpose.put(Component.Type.DEVICE, Purpose.DEVICE);
    compPurpose.put(Component.Type.FILE, Purpose.FILE);
    compPurpose.put(Component.Type.FIRMWARE, Purpose.FIRMWARE);
    compPurpose.put(Component.Type.FRAMEWORK, Purpose.FRAMEWORK);
    compPurpose.put(Component.Type.LIBRARY, Purpose.LIBRARY);
    compPurpose.put(Component.Type.OPERATING_SYSTEM, Purpose.OPERATING_SYSTEM);
    COMPONENT_TYPE_TO_PURPOSE = Collections.unmodifiableMap(compPurpose);
  }

  private Map<String, SpdxElement> componentIdToSpdxElement = new HashMap<>();

  private Map<String, AnyLicenseInfo> cdxLicenseIdToSpdxLicense = new HashMap<>();

  @Inject
  protected CycloneDxToSpdxExporter(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver,
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    super(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils,
        versionService, thirdPartyLicenseResolver, thirdPartyPersistenceService);
  }

  @Override
  public String export() {
    init();
    hasComponentRefs =
        thirdPartyFileCoordinateDAO.hasNonNullComponentRefs(exportParams.sbomMetadata.getThirdPartyFileId());
    try (InputStream gis = getOriginalSbomContent()) {
      Bom originalBom = SbomCycloneDxUtils.parseContentStreamNoValidation(gis);
      checkAndGenerateComponentMetadataIfMissing(originalBom);
      SpdxDocument originalDocument = spdxDocumentFromCycloneDxBom(originalBom, multiFormatStore);
      SpdxDocument newDocument = createNewDocumentFrom(originalDocument);
      return generateTargetSbomString(newDocument);
    }
    catch (IOException | ParseException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the original SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the translated SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
  }

  @Override
  public PdfData exportPdf() {
    throw new UnsupportedOperationException("PDF export not supported for SBOM exporter");
  }

  private void checkAndGenerateComponentMetadataIfMissing(Bom bom) {
    if (bom == null) {
      return;
    }
    if (bom.getMetadata() == null) {
      bom.setMetadata(new Metadata());
    }
    if (bom.getMetadata().getComponent() == null) {
      Component bomComponentInfo = createDefaultComponentDocumentDescribes();
      bom.getMetadata().setComponent(bomComponentInfo);
    }
  }

  private Component createDefaultComponentDocumentDescribes() {
    Component bomComponentInfo = new Component();
    bomComponentInfo.setType(Type.APPLICATION);
    bomComponentInfo.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata
        .getApplicationId()));
    return bomComponentInfo;
  }

  public SpdxDocument spdxDocumentFromCycloneDxBom(Bom baseBom, IModelStore spdxModelStore) {
    try {
      String documentUri = getBillOfMaterialsPath();
      // map CycloneDx contents to SPDX contents
      SpdxDocument newDocument = new SpdxDocument(spdxModelStore, documentUri, copyManager, true);
      setMetadata(newDocument);
      setComponents(baseBom, newDocument);
      setDescribes(baseBom, newDocument);
      setDependencies(baseBom.getDependencies());
      setVulnerabilities(baseBom);
      setCompositions(baseBom.getCompositions());
      return newDocument;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException("Unable to create SPDX document", e);
    }
  }

  private void setComponents(
      final Bom baseBom,
      final SpdxDocument newDocument) throws InvalidSPDXAnalysisException
  {
    List<Component> components = baseBom.getComponents();
    if (CollectionUtils.isNotEmpty(components)) {
      for (Component component : components) {
        bomComponentToSpdxElement(newDocument, component);
      }
    }
  }

  private void setDescribes(Bom baseBom, SpdxDocument newDocument) throws InvalidSPDXAnalysisException {
    if (baseBom.getMetadata() != null && baseBom.getMetadata().getComponent() != null) {
      SpdxElement describes = componentIdToSpdxElement.get(baseBom.getMetadata().getComponent().getBomRef());
      if (describes == null && baseBom.getMetadata().getComponent() != null) {
        bomComponentToSpdxElement(newDocument, baseBom.getMetadata().getComponent());
        describes = componentIdToSpdxElement.get(baseBom.getMetadata().getComponent().getBomRef());
        if (describes != null) {
          componentIdToSpdxElement.put(baseBom.getMetadata().getComponent().getBomRef(), describes);
          newDocument.getDocumentDescribes().add(describes);
        }
      }
    }
  }

  private void setCompositions(List<Composition> compositions) throws InvalidSPDXAnalysisException {
    if (CollectionUtils.isEmpty(compositions)) {
      return;
    }
    for (Composition composition : compositions) {
      Aggregate aggregate = composition.getAggregate();
      if (aggregate == null) {
        continue;
      }
      List<BomReference> assemblies = composition.getAssemblies();
      if (CollectionUtils.isNotEmpty(assemblies)) {
        addCommentToRelationships(assemblies, aggregate.toString(),
            RelationshipType.CONTAINS);
      }
      List<BomReference> dependencies = composition.getDependencies();
      if (CollectionUtils.isNotEmpty(dependencies)) {
        addCommentToRelationships(dependencies, aggregate.toString(),
            RelationshipType.DEPENDS_ON);
      }
    }
  }

  private void addCommentToRelationships(
      List<BomReference> bomRefs,
      String comment,
      @Nullable RelationshipType relationshipType) throws InvalidSPDXAnalysisException
  {
    for (BomReference assembly : bomRefs) {
      String bomRef = assembly.getRef();
      if (bomRef == null) {
        continue;
      }
      SpdxElement element = componentIdToSpdxElement.get(bomRef);
      if (element == null) {
        continue;
      }
      for (Relationship relationship : element.getRelationships()) {
        if (relationshipType != null) {
          if (relationshipType.equals(relationship.getRelationshipType())) {
            relationship.setComment(comment);
          }
        }
        else {
          relationship.setComment(comment);
        }
      }
    }
  }

  private void setDependencies(List<Dependency> dependencies) throws InvalidSPDXAnalysisException {
    if (CollectionUtils.isNotEmpty(dependencies)) {
      for (Dependency dependency : dependencies) {
        SpdxElement fromElement = componentIdToSpdxElement.get(dependency.getRef());
        if (fromElement == null) {
          continue;
        }
        List<Dependency> directDependencies = dependency.getDependencies();
        if (CollectionUtils.isNotEmpty(directDependencies)) {
          for (Dependency directDependency : directDependencies) {
            if (directDependency != null) {
              SpdxElement toElement = componentIdToSpdxElement.get(directDependency.getRef());
              if (toElement != null) {
                Relationship relationship =
                    fromElement.createRelationship(toElement, RelationshipType.DEPENDS_ON, null);
                Collection<Relationship> fromRelationships = fromElement.getRelationships();
                if (fromRelationships.stream()
                    .allMatch(
                        fromRelationship -> fromRelationship.compareTo(relationship) != 0))
                {
                  fromRelationships.add(relationship);
                }
              }
            }
          }
          setDependencies(directDependencies);
        }
      }
    }
  }

  private void addPedigreeRelationships(
      SpdxElement element,
      Pedigree pedigree) throws InvalidSPDXAnalysisException
  {
    Ancestors ancestors = pedigree.getAncestors();
    if (ancestors != null && ancestors.getComponents() != null) {
      for (Component ancestor : ancestors.getComponents()) {
        if (Scope.REQUIRED.equals(bomComponentToSpdxElement(element, ancestor))) {
          SpdxElement ancestorElement = componentIdToSpdxElement.get(ancestor.getBomRef());
          Relationship relationship = element.createRelationship(ancestorElement, RelationshipType.ANCESTOR_OF, null);
          element.addRelationship(relationship);
        }
      }
    }
    Descendants descendants = pedigree.getDescendants();
    if (descendants != null && ancestors.getComponents() != null) {
      for (Component descendant : descendants.getComponents()) {
        if (Scope.REQUIRED.equals(bomComponentToSpdxElement(element, descendant))) {
          SpdxElement descendantElement = componentIdToSpdxElement.get(descendant.getBomRef());
          Relationship relationship = element.createRelationship(descendantElement,
              RelationshipType.DESCENDANT_OF, null);
          element.addRelationship(relationship);
        }
      }
    }
    Variants variants = pedigree.getVariants();
    if (variants != null) {
      for (Component variant : variants.getComponents()) {
        if (Scope.REQUIRED.equals(bomComponentToSpdxElement(element, variant))) {
          SpdxElement variantElement = componentIdToSpdxElement.get(variant.getBomRef());
          Relationship relationship = element.createRelationship(variantElement,
              RelationshipType.VARIANT_OF, null);
          element.addRelationship(relationship);
        }
      }
    }
  }

  private Scope bomComponentToSpdxElement(
      SpdxElement newDocument,
      Component component) throws InvalidSPDXAnalysisException
  {
    Type componentType = component.getType();
    if (componentType == null) {
      return null;
    }
    SpdxElement element;
    String elementId = bomRefToSpdxId(component.getBomRef());
    if (elementId == null) {
      elementId = newDocument.getModelStore().getNextId(IdType.SpdxId);
    }

    if (hasComponentRefs) {
      String componentRef = SbomIdentityUtils.getComponentRef(component);
      spdxIdsToComponentRefs.put(elementId, componentRef);
    }

    String name = component.getName();
    if (name == null) {
      name = "UNKNOWN PACKAGE";
    }

    String group = component.getGroup();
    if (group != null && !group.isEmpty()) {
      name = group + ":" + name;
    }

    List<Hash> hashes = component.getHashes();
    Checksum sha1 = null;
    if (CollectionUtils.isNotEmpty(hashes)) {
      for (Hash hash : hashes) {
        if ("SHA-1".equals(hash.getAlgorithm())) {
          sha1 = newDocument.createChecksum(ChecksumAlgorithm.SHA1, hash.getValue());
          break;
        }
      }
    }
    if (sha1 == null) {
      sha1 = newDocument.createChecksum(ChecksumAlgorithm.SHA1, NULL_SHA1_VALUE);
    }
    String copyright = component.getCopyright();
    if (copyright == null) {
      copyright = SpdxConstantsCompatV2.NOASSERTION_VALUE;
    }

    if (Type.FILE.equals(componentType) && !containsPackageOnlyProperties(component)) {
      element = newDocument.createSpdxFile(elementId, name,
          new SpdxNoAssertionLicense(), new ArrayList<>(), copyright, sha1)
          .build();
      setFileProperties((SpdxFile) element, component);
    }
    else {
      SpdxPackageBuilder packageBuilder =
          newDocument.createPackage(elementId, name, new SpdxNoAssertionLicense(), copyright,
              new SpdxNoAssertionLicense())
              .setFilesAnalyzed(false);

      // Primary purpose is not supported by SPDX 2.2
      if (!SbomExportParams.ExportSpecification.SPDX_22.getVersion()
          .equals(exportParams.getExportSpecification().getVersion()))
      {
        packageBuilder.setPrimaryPurpose(COMPONENT_TYPE_TO_PURPOSE.get(componentType));
      }

      element = packageBuilder.build();
      setPackageProperties((SpdxPackage) element, component);
    }

    Pedigree pedigree = component.getPedigree();
    if (pedigree != null) {
      addPedigreeRelationships(element, pedigree);
    }
    Scope scope = component.getScope();
    if (scope == null) {
      scope = Scope.REQUIRED;
    }
    if (component.getBomRef() == null) {
      component.setBomRef(element.getId());
    }

    componentIdToSpdxElement.put(component.getBomRef(), element);
    return scope;
  }

  private void setPackageProperties(
      SpdxPackage spdxPackage,
      Component component) throws InvalidSPDXAnalysisException
  {
    if (component.getType() != null) {
      if (Type.FILE.equals(component.getType())) {
        spdxPackage.setPackageFileName(spdxPackage.getName().get());
        spdxPackage.setPackageVerificationCode(
            spdxPackage.createPackageVerificationCode(spdxPackage.getSha1(), new ArrayList<>()));
      }
    }
    spdxPackage.setLicenseDeclared(
        listToLicenseSet(spdxPackage, convertCycloneLicenseInfo(spdxPackage, component.getLicenses())));
    List<Hash> hashes = component.getHashes();
    if (CollectionUtils.isNotEmpty(hashes)) {
      for (Hash hash : hashes) {
        ChecksumAlgorithm algorithm = CDX_ALGORITHM_TO_SPDX_ALGORITHM.get(hash.getAlgorithm());
        if (algorithm != null) {
          try {
            spdxPackage.addChecksum(spdxPackage.createChecksum(algorithm, hash.getValue()));
          }
          catch (InvalidSPDXAnalysisException exception) {
            log.debug("Error creating the hash {}. {}", algorithm, exception.getMessage());
          }
        }
      }
    }
    String publisher = component.getPublisher();
    String author = component.getAuthor();
    if (publisher != null && !publisher.isEmpty()) {
      spdxPackage.setOriginator("Organization: " + publisher);
    }
    else if (author != null && !author.isEmpty()) {
      spdxPackage.setOriginator("Person: " + author);
    }
    List<Component> subComponents = component.getComponents();
    if (CollectionUtils.isNotEmpty(subComponents)) {
      for (Component subComponent : subComponents) {
        Scope scope = bomComponentToSpdxElement(spdxPackage, subComponent);
        SpdxElement subElement = componentIdToSpdxElement.get(subComponent.getBomRef());
        if (subElement != null) {
          if (Scope.REQUIRED.equals(scope)) {
            Relationship subRelationship = spdxPackage.createRelationship(
                subElement, RelationshipType.CONTAINS, null);
            spdxPackage.addRelationship(subRelationship);
          }
          else if (Scope.OPTIONAL.equals(scope)) {
            Relationship subRelationship = spdxPackage.createRelationship(
                spdxPackage, RelationshipType.OPTIONAL_COMPONENT_OF, null);
            subElement.addRelationship(subRelationship);
          }
        }
      }
    }
    String description = component.getDescription();
    if (description != null) {
      spdxPackage.setDescription(description);
    }
    List<ExternalReference> externalReferences = component.getExternalReferences();
    if (CollectionUtils.isNotEmpty(externalReferences)) {
      setExternalReferences(externalReferences, spdxPackage);
    }
    if (!spdxPackage.getDownloadLocation().isPresent()) {
      spdxPackage.setDownloadLocation(SpdxConstantsCompatV2.NOASSERTION_VALUE);
    }
    OrganizationalEntity supplier = component.getSupplier();
    if (supplier != null && !supplier.getName().isEmpty()) {
      spdxPackage.setSupplier(createSupplier(supplier));
    }
    String version = component.getVersion();
    if (version != null && !version.isEmpty()) {
      spdxPackage.setVersionInfo(version);
    }
    String purl = component.getPurl();
    if (purl != null && !purl.isEmpty()) {
      ExternalRef purlRef = spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
          ListedReferenceTypes.getListedReferenceTypes()
              .getListedReferenceTypeByName("purl"),
          purl, null);
      spdxPackage.addExternalRef(purlRef);
    }
    String cpe = component.getCpe();
    if (cpe != null && !cpe.isEmpty()) {
      ExternalRef cpeRef = spdxPackage.createExternalRef(ReferenceCategory.SECURITY,
          ListedReferenceTypes.getListedReferenceTypes()
              .getListedReferenceTypeByName(SbomSpdxUtils.getSpdxCpeVersion(cpe)),
          cpe, null);
      spdxPackage.addExternalRef(cpeRef);
    }
    Evidence evidence = component.getEvidence();
    if (evidence != null) {
      List<Copyright> copyrights = evidence.getCopyright();
      if (CollectionUtils.isNotEmpty(copyrights)) {
        for (Copyright copyright : copyrights) {
          String copyrightText = copyright.getText();
          if (copyrightText != null && !copyrightText.isEmpty()) {
            spdxPackage.getAttributionText().add(copyrightText);
          }
        }
      }
      if (evidence.getLicenses() != null) {
        AnyLicenseInfo spdxLicenseEvidence = licenseChoiceToSpdxLicense(spdxPackage, evidence.getLicenses());
        if (Objects.nonNull(spdxLicenseEvidence) && !(spdxLicenseEvidence instanceof SpdxNoAssertionLicense)) {
          spdxPackage.getAttributionText().add("Evidence license text for: " + spdxLicenseEvidence.toString());
        }
      }
    }
    if (component.getModified() != null && component.getModified()) {
      spdxPackage.setSourceInfo("This package has been modified");
    }
  }

  private static String createSupplier(final OrganizationalEntity supplier) {
    StringBuilder sb = new StringBuilder("Organization: ");
    sb.append(supplier.getName());
    List<OrganizationalContact> contacts = supplier.getContacts();
    String email = null;
    if (CollectionUtils.isNotEmpty(contacts)) {
      for (int i = 0; i < contacts.size(); i++) {
        OrganizationalContact contact = contacts.get(i);

        if (Objects.nonNull(contact.getEmail()) && !contact.getEmail().isEmpty()) {
          if (email == null) {
            email = contact.getEmail();
          }
        }
      }
    }
    if (email != null) {
      sb.append(" (");
      sb.append(email);
      sb.append(")");
    }
    return sb.toString();
  }

  private void setFileProperties(
      SpdxFile spdxFile,
      Component component) throws InvalidSPDXAnalysisException
  {
    spdxFile.getLicenseInfoFromFiles().addAll(convertCycloneLicenseInfo(spdxFile, component.getLicenses()));
    List<Hash> hashes = component.getHashes();
    if (CollectionUtils.isNotEmpty(hashes)) {
      for (Hash hash : hashes) {
        ChecksumAlgorithm algorithm = CDX_ALGORITHM_TO_SPDX_ALGORITHM.get(hash.getAlgorithm());
        if (!ChecksumAlgorithm.SHA1.equals(algorithm)) {
          spdxFile.addChecksum(spdxFile.createChecksum(algorithm, hash.getValue()));
        }
      }
    }
    String mimeType = component.getMimeType();
    if (mimeType != null) {
      FileType fileType = mimeToFileType(mimeType);
      if (fileType != null) {
        spdxFile.addFileType(fileType);
      }
    }
  }

  private boolean containsPackageOnlyProperties(Component component) {
    return (component.getAuthor() != null && !component.getAuthor()
        .isEmpty()) &&
        (component.getDescription() != null && !component.getDescription()
            .isEmpty())
        &&
        (component.getPublisher() != null && !component.getPublisher()
            .isEmpty())
        &&
        (component.getPurl() != null && !component.getPurl()
            .isEmpty())
        &&
        (component.getSupplier() != null && !component.getSupplier()
            .getName()
            .isEmpty())
        &&
        (component.getVersion() != null && !component.getVersion()
            .isEmpty())
        &&
        (component.getPurl() != null && !component.getPurl()
            .isEmpty())
        &&
        (component.getComponents() != null && !component.getComponents()
            .isEmpty())
        &&
        (component.getEvidence() != null) &&
        (component.getExternalReferences() != null && !component.getExternalReferences()
            .isEmpty());
  }

  private @Nullable FileType mimeToFileType(String mimeType) {
    String[] mimeParts = mimeType.toLowerCase()
        .trim()
        .split("/");
    if (mimeParts.length < 2) {
      return null;
    }
    switch (mimeParts[0]) {
      case "application":
        if (mimeParts[1].startsWith("spdx+")) {
          return FileType.SPDX;
        }
        else if (mimeParts[1].endsWith("+zip") || mimeParts[1].endsWith("+gzip") ||
            mimeParts[1].endsWith("+rar"))
        {
          return FileType.ARCHIVE;
        }
        else if (mimeParts[1].contains("x-bytecode")) {
          return FileType.BINARY;
        }
        switch (mimeParts[1]) {
          case "zip":
          case "gzip":
          case "rar":
          case "x-bzip":
          case "x-bzip2":
          case "vnd.rar":
          case "x-tar":
          case "x-7z-compressed":
            return FileType.ARCHIVE;
          case "java-archive":
            return FileType.BINARY;
          case "x-sh":
            return FileType.SOURCE;
          case "octet-stream":
            return FileType.BINARY;
          default:
            return FileType.APPLICATION;
        }
      case "audio":
        return FileType.AUDIO;
      case "font":
        return FileType.OTHER;
      case "example":
        return FileType.OTHER;
      case "image":
        return FileType.IMAGE;
      case "message":
        return FileType.OTHER;
      case "model":
        return FileType.OTHER;
      case "multipart":
        return FileType.ARCHIVE;
      case "text": {
        switch (mimeParts[1]) {
          case "text/javascript":
          case "x-csharp":
          case "x-java-source":
          case "x-c":
          case "x-script.phyton":
            return FileType.SOURCE;
          default:
            return FileType.TEXT;
        }
      }
      case "video":
        return FileType.VIDEO;
      default:
        return FileType.OTHER;
    }
  }

  private List<AnyLicenseInfo> convertCycloneLicenseInfo(
      SpdxElement parentElement,
      LicenseChoice licenseChoice)
  {
    List<AnyLicenseInfo> retval = new ArrayList<>();
    if (licenseChoice != null) {
      Expression expression = licenseChoice.getExpression();
      if (expression != null && StringUtils.isNotEmpty(expression.getValue())) {
        try {
          retval.add(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(expression.getValue(),
              parentElement.getModelStore(), parentElement.getDocumentUri(), parentElement.getCopyManager()));
        }
        catch (InvalidLicenseStringException | DefaultStoreNotInitializedException ex) {
          log.debug("Invalid license expression '" + expression + "'");
        }
      }
      List<License> licenses = licenseChoice.getLicenses();
      if (CollectionUtils.isNotEmpty(licenses)) {
        for (License lic : licenses) {
          try {
            retval.add(cdxLicenseToSpdxLicense(lic, parentElement.getModelStore(), parentElement.getDocumentUri(),
                parentElement.getCopyManager()));
          }
          catch (InvalidSPDXAnalysisException ex) {
            log.debug("Invalid CDX license '" + lic.getId() + "'");
          }
        }
      }
    }
    return retval;
  }

  private synchronized AnyLicenseInfo cdxLicenseToSpdxLicense(
      License cdxLicense,
      IModelStore modelStore,
      String documentUri,
      IModelCopyManager copyManager) throws InvalidSPDXAnalysisException
  {
    String id = cdxLicense.getId();
    if (id == null) {
      id = cdxLicense.getName();
      log.debug("Missing CycloneDX license ID for license name " + id);
    }
    if (ListedLicenses.getListedLicenses()
        .isSpdxListedLicenseId(id))
    {
      return ListedLicenses.getListedLicenses()
          .getListedLicenseByIdCompatV2(id);
    }
    if (!cdxLicenseIdToSpdxLicense.containsKey(id)) {
      // create the extracted license info
      ExtractedLicenseInfo eli =
          new ExtractedLicenseInfo(modelStore, documentUri, cdxLicenseIdToSpdxLicenseId(id), copyManager, true);
      AttachmentText attachmentText = cdxLicense.getAttachmentText();
      if (attachmentText != null) {
        String licenseText = attachmentText.getText();
        if (licenseText != null && !licenseText.isEmpty()) {
          eli.setExtractedText(licenseText);
        }
      }
      String url = cdxLicense.getUrl();
      if (url != null && !url.isEmpty()) {
        eli.getSeeAlso()
            .add(url);
      }
      String name = cdxLicense.getName();
      if (name != null && !name.isEmpty()) {
        eli.setName(name);
      }
      cdxLicenseIdToSpdxLicense.put(id, eli);
    }
    return cdxLicenseIdToSpdxLicense.get(id);
  }

  private String cdxLicenseIdToSpdxLicenseId(String id) {
    return SpdxConstantsCompatV2.NON_STD_LICENSE_ID_PRENUM + id.replaceAll(INVALID_REF_REGEX, "-");
  }

  private AnyLicenseInfo listToLicenseSet(
      SpdxElement parentElement,
      List<AnyLicenseInfo> licenses) throws InvalidSPDXAnalysisException
  {
    if (licenses.isEmpty()) {
      return new SpdxNoAssertionLicense();
    }
    if (licenses.size() == 1) {
      return licenses.get(0);
    }
    else {
      ConjunctiveLicenseSet retval = new ConjunctiveLicenseSet(parentElement.getModelStore(),
          parentElement.getDocumentUri(),
          parentElement.getModelStore()
              .getNextId(IdType.Anonymous),
          parentElement.getCopyManager(), true);
      retval.getMembers()
          .addAll(licenses);
      return retval;
    }
  }

  private @Nullable String bomRefToSpdxId(String bomRef) {
    if (Objects.isNull(bomRef)) {
      return null;
    }
    return SpdxConstantsCompatV2.SPDX_ELEMENT_REF_PRENUM + bomRef.replaceAll(INVALID_REF_REGEX, "-");
  }

  private void setExternalReferences(
      List<ExternalReference> externalReferences,
      SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException
  {
    if (CollectionUtils.isEmpty(externalReferences)) {
      return;
    }
    for (ExternalReference externalRef : externalReferences) {
      ExternalReference.Type type = externalRef.getType();
      String url = externalRef.getUrl();
      if (url == null || url.isEmpty() || type == null) {
        continue;
      }
      String comment = externalRef.getComment();
      switch (type) {
        case VCS:
          if (url.startsWith(REFERENCE_SITE_BOWER)) {
            spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
                ListedReferenceTypes.getListedReferenceTypes()
                    .getListedReferenceTypeByName("bower"),
                url, comment));
          }
          else if (url.startsWith(REFERENCE_SITE_MAVEN_CENTRAL)) {
            spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
                ListedReferenceTypes.getListedReferenceTypes()
                    .getListedReferenceTypeByName("maven-central"),
                url, comment));
          }
          else if (url.startsWith(REFERENCE_SITE_NPM)) {
            spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
                ListedReferenceTypes.getListedReferenceTypes()
                    .getListedReferenceTypeByName("npm"),
                url, comment));
          }
          else if (url.startsWith(REFERENCE_SITE_NUGET)) {
            spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
                ListedReferenceTypes.getListedReferenceTypes()
                    .getListedReferenceTypeByName("nuget"),
                url, comment));
          }
          else {
            spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
                new ReferenceType("http://cyclonedx.org/referenctype/other-package-manager"), url, comment));
          }
          break;
        case ISSUE_TRACKER:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/issue-tracker"), url, comment));
          break;
        case WEBSITE:
          if (spdxPackage.getHomepage()
              .isPresent())
          {
            log.debug("More than one home page in CycloneDX.  The following will be ignored: " + url);
          }
          else {
            spdxPackage.setHomepage(url);
          }
          break;
        case ADVISORIES:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.SECURITY,
              ListedReferenceTypes.getListedReferenceTypes()
                  .getListedReferenceTypeByName("advisory"),
              url, comment));
          break;
        case BOM:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/bom"), url, comment));
          break;
        case MAILING_LIST:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/mailing_list"), url, comment));
          break;
        case SOCIAL:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/social"), url, comment));
          break;
        case CHAT:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/chat"), url, comment));
          break;
        case DOCUMENTATION:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/documentation"), url, comment));
          break;
        case SUPPORT:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/support"), url, comment));
          break;
        case DISTRIBUTION:
          try {
            spdxPackage.setDownloadLocation(url);
          }
          catch (InvalidSPDXAnalysisException e) {
            log.debug(
                "downloadLocation cannot be set a non-url value found in 'externalReference' of type 'distribution': " +
                    url);
          }
          break;
        case LICENSE:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/license"), url, comment));
          break;
        case BUILD_META:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/buildmeta"), url, comment));
          break;
        case BUILD_SYSTEM:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/buildsystem"), url, comment));
          break;
        case OTHER:
        default:
          spdxPackage.addExternalRef(spdxPackage.createExternalRef(ReferenceCategory.OTHER,
              new ReferenceType("http://cyclonedx.org/referenctype/other"), url, comment));
          break;
      }
    }
  }

  private AnyLicenseInfo licenseChoiceToSpdxLicense(
      SpdxElement parent,
      LicenseChoice licenseChoice) throws InvalidSPDXAnalysisException
  {
    List<AnyLicenseInfo> licenses = convertCycloneLicenseInfo(parent, licenseChoice);
    if (licenses.size() == 1) {
      return licenses.get(0);
    }
    else if (licenses.isEmpty()) {
      return new SpdxNoneLicense();
    }
    else {
      return parent.createConjunctiveLicenseSet(licenses);
    }
  }

  private void setVulnerabilities(
      final Bom baseBom) throws InvalidSPDXAnalysisException
  {
    if (CollectionUtils.isNotEmpty(baseBom.getVulnerabilities())) {
      List<Vulnerability> bomVulnerabilitiesList = baseBom.getVulnerabilities();
      // Per-package locator tracker so duplicate alias URLs across multiple vulns on the
      // same component don't produce duplicate SPDX ExternalRefs.
      Map<SpdxPackage, Set<String>> emittedPerPackage = new HashMap<>();
      for (Vulnerability bomVulnerability : bomVulnerabilitiesList) {
        if (CollectionUtils.isNotEmpty(bomVulnerability.getAffects())) {
          List<Affect> affectsList = bomVulnerability.getAffects();
          for (Affect affected : affectsList) {
            SpdxPackage affectedPackage = (SpdxPackage) componentIdToSpdxElement.get(affected.getRef());
            String vulnerabilitySourceUrl = "Url-Not-Present";
            String vulnerabilitySourceName = "Name-Not-Present";
            if (bomVulnerability.getSource() != null) {
              if (bomVulnerability.getSource().getUrl() != null) {
                vulnerabilitySourceUrl = bomVulnerability.getSource().getUrl();
              }
              if (bomVulnerability.getSource().getName() != null) {
                vulnerabilitySourceName = bomVulnerability.getSource().getName();
              }
            }
            affectedPackage.addExternalRef(affectedPackage.createExternalRef(ReferenceCategory.SECURITY,
                ListedReferenceTypes.getListedReferenceTypes().getListedReferenceTypeByName("advisory"),
                vulnerabilitySourceUrl, "source: " + vulnerabilitySourceName));
            Set<String> emittedLocators = emittedPerPackage.computeIfAbsent(affectedPackage, p -> new HashSet<>());
            if (StringUtils.isNotBlank(vulnerabilitySourceUrl)) {
              emittedLocators.add(vulnerabilitySourceUrl.toLowerCase(Locale.ROOT));
            }
            if (bomVulnerability.getReferences() != null) {
              for (Vulnerability.Reference ref : bomVulnerability.getReferences()) {
                if (ref == null || ref.getSource() == null
                    || StringUtils.isBlank(ref.getSource().getUrl()))
                {
                  continue;
                }
                String refSourceName = ref.getSource().getName();
                if (bomVulnerability.getId() != null
                    && bomVulnerability.getId().equalsIgnoreCase(ref.getId())
                    && !VulnerabilityUrlBuilder.SONATYPE_GUIDE_SOURCE.equals(refSourceName))
                {
                  continue;
                }
                if (!emittedLocators.add(ref.getSource().getUrl().toLowerCase(Locale.ROOT))) {
                  continue;
                }
                String comment = VulnerabilityUrlBuilder.SONATYPE_GUIDE_SOURCE.equals(refSourceName)
                    ? VulnerabilityUrlBuilder.SONATYPE_GUIDE_SPDX_COMMENT
                    : "source: " + (StringUtils.isBlank(refSourceName) ? "UNKNOWN" : refSourceName);
                affectedPackage.addExternalRef(affectedPackage.createExternalRef(ReferenceCategory.SECURITY,
                    ListedReferenceTypes.getListedReferenceTypes().getListedReferenceTypeByName("advisory"),
                    ref.getSource().getUrl(), comment));
              }
            }
          }
        }
      }
    }
  }
}
