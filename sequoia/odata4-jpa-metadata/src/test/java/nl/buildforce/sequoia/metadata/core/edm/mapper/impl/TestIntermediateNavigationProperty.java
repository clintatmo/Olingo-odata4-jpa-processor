package nl.buildforce.sequoia.metadata.core.edm.mapper.impl;

import nl.buildforce.sequoia.metadata.api.JPAEdmMetadataPostProcessor;
import nl.buildforce.sequoia.metadata.core.edm.annotation.EdmEnumeration;
import nl.buildforce.sequoia.metadata.core.edm.annotation.EdmProtectedBy;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAOnConditionItem;
import nl.buildforce.sequoia.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.metadata.core.edm.mapper.extension.IntermediateEntityTypeAccess;
import nl.buildforce.sequoia.metadata.core.edm.mapper.extension.IntermediateNavigationPropertyAccess;
import nl.buildforce.sequoia.metadata.core.edm.mapper.extension.IntermediatePropertyAccess;
import nl.buildforce.sequoia.metadata.core.edm.mapper.extension.IntermediateReferenceList;
import nl.buildforce.sequoia.processor.core.testmodel.AbcClassification;
import nl.buildforce.sequoia.processor.core.testmodel.AdministrativeDivision;
import nl.buildforce.sequoia.processor.core.testmodel.BusinessPartner;
import nl.buildforce.sequoia.processor.core.testmodel.BusinessPartnerRole;
import nl.buildforce.sequoia.processor.core.testmodel.DummyToBeIgnored;
import nl.buildforce.sequoia.processor.core.testmodel.JoinSource;
import nl.buildforce.sequoia.processor.core.testmodel.Organization;
import nl.buildforce.sequoia.processor.core.testmodel.Person;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlOnDelete;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlOnDeleteAction;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlReferentialConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.reflections8.Reflections;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class TestIntermediateNavigationProperty extends TestMappingRoot {
  private IntermediateSchema schema;
  private TestHelper helper;
  private JPAEdmMetadataPostProcessor processor;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    final Reflections r = mock(Reflections.class);
    when(r.getTypesAnnotatedWith(EdmEnumeration.class)).thenReturn(new HashSet<>(Collections.singletonList(AbcClassification.class)));

    schema = new IntermediateSchema(new JPADefaultEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    processor = mock(JPAEdmMetadataPostProcessor.class);
  }

  @Test
  public void checkNaviPropertyCanBeCreated() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME), schema.getStructuredType(jpaAttribute),
        jpaAttribute, schema);
  }

  @Test
  public void checkGetName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("Roles", property.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkGetEdmType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals(PUNIT_NAME + ".BusinessPartnerRole", property.getEdmItem().getType(), "Wrong name");
  }

  @Test
  public void checkGetIgnoreFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetIgnoreTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(DummyToBeIgnored.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);
    assertTrue(property.ignore());
  }

  @Test
  public void checkGetPropertyFacetsNullableTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetPropertyOnDelete() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals(CsdlOnDeleteAction.Cascade, property.getEdmItem().getOnDelete().getAction());
  }

  @Test
  public void checkGetPropertyFacetsNullableFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);

    assertFalse(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetPropertyFacetsCollectionTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetPropertyFacetsCollectionFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);

    assertFalse(property.getEdmItem().isCollection());
  }

  @Test
  public void checkGetJoinColumnsSize1BP() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals(1, property.getJoinColumns().size());
  }

  @Test
  public void checkGetPartnerAdmin_Parent() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(AdministrativeDivision.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "parent");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Children", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerAdmin_Children() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(AdministrativeDivision.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "children");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Parent", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerBP_Roles() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("BusinessPartner", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerRole_BP() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Roles", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetJoinColumnFilledCompletely() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);

    IntermediateJoinColumn act = property.getJoinColumns().get(0);
    assertEquals("\"BusinessPartnerID\"", act.getName());
    assertEquals("\"ID\"", act.getReferencedColumnName());
  }

  @Test
  public void checkGetJoinColumnFilledCompletelyInvert() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);

    IntermediateJoinColumn act = property.getJoinColumns().get(0);
    assertEquals("\"BusinessPartnerID\"", act.getName());
    assertEquals("\"ID\"", act.getReferencedColumnName());
  }

  @Test
  public void checkGetJoinColumnsSize1Roles() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals(1, property.getJoinColumns().size());
  }

  @Test
  public void checkGetJoinColumnsSize2() throws ODataJPAModelException {
    EmbeddableType<?> et = helper.getEmbeddableType("PostalAddressData");
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "administrativeDivision");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getComplexType(et.getJavaType()), jpaAttribute, schema);
    List<IntermediateJoinColumn> columns = property.getJoinColumns();
    assertEquals(3, columns.size());
  }

  @Test
  public void checkGetReferentialConstraintSize() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);
    assertEquals(1, property.getProperty().getReferentialConstraints().size());
  }

  @Test
  public void checkGetReferentialConstraintBuPaRole() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    for (CsdlReferentialConstraint c : constraints) {
      assertEquals("ID", c.getProperty());
      assertEquals("BusinessPartnerID", c.getReferencedProperty());
    }
  }

  @Test
  public void checkGetReferentialConstraintRoleBuPa() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    for (CsdlReferentialConstraint c : constraints) {
      assertEquals("BusinessPartnerID", c.getProperty());
      assertEquals("ID", c.getReferencedProperty());
    }
  }

  @Test
  public void checkGetReferentialConstraintViaEmbeddedId() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "allDescriptions");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(AdministrativeDivision.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    assertEquals(3, constraints.size());
    for (CsdlReferentialConstraint c : constraints) {
      assertEquals(c.getReferencedProperty(), c.getProperty());
    }
  }

  @Test
  public void checkPostProcessorCalled() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(processor);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(
        BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    property.getEdmItem();
    verify(processor, atLeastOnce()).processNavigationProperty(property, BUPA_CANONICAL_NAME);
  }

  @Test
  public void checkPostProcessorNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("RoleAssignment", property.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    JPAAssociationAttribute property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);

    assertEquals("RoleAssignment", property.getExternalName(), "Wrong name");
  }

  @Test
  public void checkPostProcessorSetOnDelete() throws ODataJPAModelException {
    PostProcessorOneDelete pPDouble = new PostProcessorOneDelete();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "children");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(AdministrativeDivision.class), jpaAttribute, schema);

    assertEquals(CsdlOnDeleteAction.None, property.getProperty().getOnDelete().getAction());
  }

  @Test
  public void checkGetJoinTable() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNotNull(property.getJoinTable());
  }

  @Test
  public void checkGetJoinTableName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("\"SupportRelationship\"", property.getJoinTable().getTableName());
  }

  @Test
  public void checkGetNullIfNoJoinTableGiven() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "parent");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNull(property.getJoinTable());
  }

  @Test
  public void checkGetJoinTableJoinColumns() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
  }

  @Test
  public void checkGetJoinTableEntityType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNotNull(property.getJoinTable().getEntityType());
  }

  @Test
  public void checkGetJoinTableJoinColumnsNotMapped() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(JoinSource.class),
        "oneToMany");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(JoinSource.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
    assertNotNull(property.getJoinTable());
    IntermediateJoinTable act = (IntermediateJoinTable) property.getJoinTable();
    for (JPAOnConditionItem item : act.getJoinColumns()) {
      assertNotNull(item.getLeftPath());
      assertNotNull(item.getRightPath());
    }
  }

  @Test
  public void checkGetJoinTableJoinColumnsMapped() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Organization.class),
        "supportEngineers");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
  }

  @Test
  public void checkGetConverterReturnsNull() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertNull(property.getConverter());
  }

  @Test
  public void checkGetEdmTypeReturnsNull() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertNull(property.getEdmType());
  }

  @Test
  public void checkHasProtectionReturnsFalse() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertFalse(property.hasProtection());
  }

  @Test
  public void checkIsAssociationReturnsTrue() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertTrue(property.isAssociation());
  }

  @Test
  public void checkIsComplexReturnsFalse() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertFalse(property.isComplex());
  }

  @Test
  public void checkIsEnumReturnsFalse() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertFalse(property.isEnum());
  }

  @Test
  public void checkIsKeyReturnsFalse() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertFalse(property.isKey());
  }

  @Test
  public void checkIsSearchableReturnsFalse() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertFalse(property.isSearchable());
  }

  @Test
  public void checkGetProtectionPathReturnsEmptyList() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertTrue(property.getProtectionPath("Bla").isEmpty());
  }

  @Test
  public void checkGetProtectionClaimNamesReturnsEmptySet() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertTrue(property.getProtectionClaimNames().isEmpty());
  }

  @Test
  public void checkGetType() throws ODataJPAModelException {
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPADefaultEdmNameBuilder(
        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
    assertEquals(BusinessPartner.class, property.getType());
  }

//  @Test
//  public void checkSetAnnotations() throws ODataJPAModelException {
//    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(
//        PUNIT_NAME), schema.getEntityType(JoinSource.class), createDummyAttribute(), schema);
//    property.getAnnotations(edmAnnotations, member, internalName, property);
//  }

  private Attribute<?, ?> createDummyAttribute() {
    final Attribute<?, ?> jpaAttribute = mock(Attribute.class);
    final ManagedType<?> mgrType = mock(ManagedType.class);
    final Member member = mock(Member.class, withSettings().extraInterfaces(AnnotatedElement.class));
    when(jpaAttribute.getName()).thenReturn("willi");
    when(jpaAttribute.isCollection()).thenReturn(false);
    when(jpaAttribute.getJavaType()).thenAnswer((Answer<Class<?>>) invocation -> BusinessPartner.class);
    when(jpaAttribute.getDeclaringType()).thenAnswer((Answer<ManagedType<?>>) invocation -> mgrType);
    when(mgrType.getJavaType()).thenAnswer((Answer<Class<?>>) invocation -> BusinessPartner.class);
    when(jpaAttribute.getJavaMember()).thenReturn(member);
    when(((AnnotatedElement) member).getAnnotation(EdmProtectedBy.class)).thenReturn(null);
    return jpaAttribute;
  }

  private static class PostProcessorSetName extends JPAEdmMetadataPostProcessor {
    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
                                          String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(
          BUPA_CANONICAL_NAME)) {
        if (property.getInternalName().equals("roles")) {
          property.setExternalName("RoleAssignment");
        }
      }
    }

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {

    }

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

  }

  private static class PostProcessorOneDelete extends JPAEdmMetadataPostProcessor {
    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(ADMIN_CANONICAL_NAME)) {
        if (property.getInternalName().equals("children")) {
          CsdlOnDelete oD = new CsdlOnDelete();
          oD.setAction(CsdlOnDeleteAction.None);
          property.setOnDelete(oD);
        }
      }
    }

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

  }

}