package nl.buildforce.sequoia.processor.core.modify;

import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAssociationPath;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAttribute;
// import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPADescriptionAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAElement;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAEntityType;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAPath;
import nl.buildforce.sequoia.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.processor.core.converter.JPAExpandResult;
import nl.buildforce.sequoia.processor.core.converter.JPATuple;
import nl.buildforce.sequoia.processor.core.converter.JPATupleChildConverter;
import nl.buildforce.sequoia.processor.core.exception.ODataJPAProcessorException;
import nl.buildforce.sequoia.processor.core.query.ExpressionUtil;
import nl.buildforce.olingo.server.api.ODataApplicationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class JPACreateResult implements JPAExpandResult {

  protected final JPAEntityType et;
  protected final Map<JPAAssociationPath, JPAExpandResult> children;
  protected final List<JPAPath> pathList;
  protected final JPAConversionHelper helper;
  protected final Map<String, List<String>> requestHeaders;

  public JPACreateResult(final JPAEntityType et, final Map<String, List<String>> requestHeaders)
      throws ODataJPAModelException {

    this.et = et;
    this.helper = new JPAConversionHelper();
    children = new HashMap<>(0);
    this.pathList = et.getPathList();
    this.requestHeaders = requestHeaders;
  }

  @Override
  public void convert(final JPATupleChildConverter converter) throws ODataApplicationException {
    // No implementation required for CUD operations
  }

  @Override
  public JPAExpandResult getChild(JPAAssociationPath associationPath) {
    return children.get(associationPath);
  }

  @Override
  public Map<JPAAssociationPath, JPAExpandResult> getChildren() {
    return children;
  }

  @Override
  public Long getCount(final String key) {
    return null;
  }

  @Override
  public JPAEntityType getEntityType() {
    return et;
  }

  @Override
  public boolean hasCount() {
    return false;
  }

  protected void addValueToTuple(final JPATuple tuple, final JPAPath path, final int index, Object value)
      /*throws ODataJPAProcessorException*/ {
    /*if (path.getPath().get(index) instanceof JPADescriptionAttribute) {
      @SuppressWarnings("unchecked")
      Collection<Object> desc = (Collection<Object>) value;
      if (desc != null) {
        for (final Object entry : desc) {
          final Map<String, Object> descGetterMap = entryAsMap(entry);
          final JPADescriptionAttribute jpaAttribute = (JPADescriptionAttribute) path.getPath().get(index);
          final String providedLocale = determineLocale(descGetterMap, jpaAttribute);
          if (locale.getLanguage().equals(providedLocale)
              || locale.toString().equals(providedLocale)) {
            final Object description = descGetterMap.get(jpaAttribute.getDescriptionAttribute().getInternalName());
            tuple.addElement(path.getAlias(), path.getLeaf().getType(), description);
            break;
          }
        }
      } else {
        tuple.addElement(path.getAlias(), path.getLeaf().getType(), null);
      }
    } else*/ {
      tuple.addElement(path.getAlias(), path.getLeaf().getType(), value);
    }
  }

  protected void convertPathToTuple(final JPATuple tuple, final Map<String, Object> jpaEntity, final JPAPath path,
      final int index) throws ODataJPAProcessorException {

    final Object value = jpaEntity.get(path.getPath().get(index).getInternalName());
    if (path.getPath().size() == index + 1 || value == null) {
      addValueToTuple(tuple, path, index, value);
    } else {
      final Map<String, Object> embeddedGetterMap = entryAsMap(value);
      convertPathToTuple(tuple, embeddedGetterMap, path, index + 1);
    }
  }

  protected abstract String determineLocale(final Map<String, Object> descGetterMap,
      JPAPath localeAttribute, final int index) throws ODataJPAProcessorException;

  protected abstract Map<String, Object> entryAsMap(final Object entry) throws ODataJPAProcessorException;

  protected boolean notContainsCollection(final JPAPath path) {
    for (JPAElement e : path.getPath())
      if (e instanceof JPAAttribute && ((JPAAttribute) e).isCollection())
        return false;
    return true;
  }

/*  private String determineLocale(final Map<String, Object> descGetterMap,
      final JPADescriptionAttribute descAttribute) throws ODataJPAProcessorException {
    return determineLocale(descGetterMap, descAttribute.getLocaleFieldName(), 0);
  }*/
}