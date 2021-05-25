package nl.buildforce.sequoia.metadata.core.edm.mapper.impl;

import nl.buildforce.sequoia.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.metadata.core.edm.mapper.extension.ODataAction;
import nl.buildforce.sequoia.metadata.core.edm.mapper.testobjects.ExampleJavaOneAction;
import nl.buildforce.sequoia.metadata.core.edm.mapper.testobjects.ExampleJavaTwoActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections8.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestIntermediateActionFactory extends TestMappingRoot {
  private TestHelper helper;

  private Reflections reflections;
  private IntermediateActionFactory cut;
  private Set<Class<? extends ODataAction>> javaActions;

  @BeforeEach
  public void setUp() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);

    reflections = mock(Reflections.class);
    cut = new IntermediateActionFactory();
    javaActions = new HashSet<>();
    when(reflections.getSubTypesOf(ODataAction.class)).thenReturn(javaActions);
  }

  @Test
  public void checkReturnEmptyMapIfReflectionsNull() throws ODataJPAModelException {
    assertNotNull(cut.create(new JPADefaultEdmNameBuilder(PUNIT_NAME), null, helper.schema));
  }

  @Test
  public void checkReturnEmptyMapIfNoJavaFunctionsFound() throws ODataJPAModelException {
    assertNotNull(cut.create(new JPADefaultEdmNameBuilder(PUNIT_NAME), reflections, helper.schema));
  }

  @Test
  public void checkReturnMapWithOneIfOneJavaFunctionsFound() throws ODataJPAModelException {
    javaActions.add(ExampleJavaOneAction.class);
    Map<? extends String, ? extends IntermediateJavaAction> act = cut.create(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(1, act.size());
  }

  @Test
  public void checkReturnMapWithTwoIfTwoJavaFunctionsFound() throws ODataJPAModelException {
    javaActions.add(ExampleJavaTwoActions.class);
    Map<? extends String, ? extends IntermediateJavaAction> act = cut.create(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(2, act.size());
  }

  @Test
  public void checkReturnMapWithWithJavaFunctionsFromTwoClassesFound() throws ODataJPAModelException {
    javaActions.add(ExampleJavaOneAction.class);
    javaActions.add(ExampleJavaTwoActions.class);
    Map<? extends String, ? extends IntermediateJavaAction> act = cut.create(new JPADefaultEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(3, act.size());
  }

}