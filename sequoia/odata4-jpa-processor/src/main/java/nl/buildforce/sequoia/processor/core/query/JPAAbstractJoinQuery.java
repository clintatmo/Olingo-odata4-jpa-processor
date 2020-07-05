package nl.buildforce.sequoia.processor.core.query;

import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAssociationPath;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPACollectionAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPADescriptionAttribute;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAElement;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAEntityType;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAPath;
import nl.buildforce.sequoia.metadata.core.edm.mapper.api.JPAStructuredType;
import nl.buildforce.sequoia.metadata.core.edm.mapper.exception.ODataJPAException;
import nl.buildforce.sequoia.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.processor.core.api.JPAODataCRUDContextAccess;
import nl.buildforce.sequoia.processor.core.api.JPAODataClaimProvider;
import nl.buildforce.sequoia.processor.core.api.JPAODataPage;
import nl.buildforce.sequoia.processor.core.api.JPAODataRequestContextAccess;
import nl.buildforce.sequoia.processor.core.exception.ODataJPAProcessorException;
import nl.buildforce.sequoia.processor.core.exception.ODataJPAQueryException;
import nl.buildforce.sequoia.processor.core.exception.ODataJPAQueryException.MessageKeys;
import nl.buildforce.sequoia.processor.core.filter.JPAFilterCompiler;
import nl.buildforce.sequoia.processor.core.filter.JPAFilterCrossCompiler;
import nl.buildforce.sequoia.processor.core.filter.JPAOperationConverter;
import nl.buildforce.sequoia.processor.core.processor.JPAODataRequestContextImpl;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import nl.buildforce.olingo.commons.api.edm.EdmType;
import nl.buildforce.olingo.commons.api.http.HttpStatusCode;
import nl.buildforce.olingo.server.api.OData;
import nl.buildforce.olingo.server.api.ODataApplicationException;
import nl.buildforce.olingo.server.api.uri.UriInfoResource;
import nl.buildforce.olingo.server.api.uri.UriParameter;
import nl.buildforce.olingo.server.api.uri.UriResource;
import nl.buildforce.olingo.server.api.uri.UriResourceComplexProperty;
import nl.buildforce.olingo.server.api.uri.UriResourceEntitySet;
import nl.buildforce.olingo.server.api.uri.UriResourceKind;
import nl.buildforce.olingo.server.api.uri.UriResourceNavigation;
import nl.buildforce.olingo.server.api.uri.UriResourcePartTyped;
import nl.buildforce.olingo.server.api.uri.UriResourcePrimitiveProperty;
import nl.buildforce.olingo.server.api.uri.UriResourceProperty;
import nl.buildforce.olingo.server.api.uri.queryoption.OrderByItem;
import nl.buildforce.olingo.server.api.uri.queryoption.OrderByOption;
import nl.buildforce.olingo.server.api.uri.queryoption.SelectItem;
import nl.buildforce.olingo.server.api.uri.queryoption.SelectOption;
import nl.buildforce.olingo.server.api.uri.queryoption.SkipOption;
import nl.buildforce.olingo.server.api.uri.queryoption.TopOption;
import nl.buildforce.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import nl.buildforce.olingo.server.api.uri.queryoption.expression.Member;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static nl.buildforce.sequoia.processor.core.exception.ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_NOT_ALLOWED_MEMBER;

public abstract class JPAAbstractJoinQuery extends JPAAbstractQuery implements JPAQuery {
  protected static final String ALIAS_SEPARATOR = ".";
  protected final UriInfoResource uriResource;
  protected final CriteriaQuery<Tuple> cq;
  protected Root<?> root;
  protected From<?, ?> target;
  protected final JPAODataCRUDContextAccess context;
  protected final JPAODataPage page;
  protected final List<JPANavigationPropertyInfo> navigationInfo;
  protected final JPANavigationPropertyInfo lastInfo;
  protected final JPAODataRequestContextAccess requestContext;

  public JPAAbstractJoinQuery(final OData odata, final JPAODataCRUDContextAccess sessionContext,
      final JPAEntityType jpaEntityType, final JPAODataRequestContextAccess requestContext,
      final Map<String, List<String>> requestHeaders, final List<JPANavigationPropertyInfo> navigationInfo)
      throws ODataJPAException {

    this(odata, sessionContext, jpaEntityType, requestContext.getUriInfo(), requestContext, requestHeaders,
        navigationInfo);
  }

  protected JPAAbstractJoinQuery(final OData odata, final JPAODataCRUDContextAccess sessionContext,
      final JPAEntityType jpaEntityType, final UriInfoResource uriInfo,
      final JPAODataRequestContextAccess requestContext, final Map<String, List<String>> requestHeaders,
      final List<JPANavigationPropertyInfo> navigationInfo) throws ODataJPAException {

    super(odata, sessionContext.getEdmProvider().getServiceDocument(), jpaEntityType, requestContext);
    this.requestContext = requestContext;
    this.locale = ExpressionUtil.determineLocale(requestHeaders);
    this.uriResource = uriInfo;
    this.cq = cb.createTupleQuery();
    this.context = sessionContext;
    this.page = requestContext.getPage();
    this.navigationInfo = navigationInfo;
    this.lastInfo = determineLastInfo(navigationInfo);
  }

  @Override
  public AbstractQuery<?> getQuery() {
    return cq;
  }

  @Override
  public From<?, ?> getRoot() {
    return target;
  }

  /**
   * Applies the $skip and $top options of the OData request to the query. The values are defined as follows:
   * <ul>
   * <li> The $top system query option specifies a non-negative integer n that limits the number of items returned from
   * a collection.
   * <li> The $skip system query option specifies a non-negative integer n that excludes the first n items of the
   * queried collection from the result.
   * </ul>
   * These values can be restricted by a page provided by server driven paging<p>
   * For details see:
   * <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398306"
   * >OData Version 4.0 Part 1 - 11.2.5.3 System Query Option $top</a> and
   * <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata03/os/complete/part1-protocol/odata-v4.0-errata03-os-part1-protocol-complete.html#_Server-Driven_Paging"
   * >OData Version 4.0 Part 1 - 11.2.5.7 Server-Driven Paging</a>
   *
   * @throws ODataApplicationException
   */
  protected void addTopSkip(final TypedQuery<Tuple> tq) throws ODataApplicationException {
    /*
     * Where $top and $skip are used together, $skip MUST be applied before $top, regardless of the order in which they
     * appear in the request.
     * If no unique ordering is imposed through an $orderby query option, the service MUST impose a stable ordering
     * across requests that include $skip.
     *
     * URL example: http://localhost:8080/BuPa/BuPa.svc/Organizations?$count=true&$skip=5
     */

    final TopOption topOption = uriResource.getTopOption();
    if (topOption != null || page != null) {
      int topNumber = topOption != null ? topOption.getValue() : page.getTop();
      topNumber = topOption != null && page != null ? Math.min(topOption.getValue(), page.getTop())
          : topNumber;
      if (topNumber >= 0)
        tq.setMaxResults(topNumber);
      else
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
            HttpStatusCode.BAD_REQUEST, Integer.toString(topNumber), "$top");
    }

    final SkipOption skipOption = uriResource.getSkipOption();
    if (skipOption != null || page != null) {
      int skipNumber = skipOption != null ? skipOption.getValue() : page.getSkip();
      skipNumber = skipOption != null && page != null ? Math.max(skipOption.getValue(), page.getSkip()) : skipNumber;
      if (skipNumber >= 0)
        tq.setFirstResult(skipNumber);
      else
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
            HttpStatusCode.BAD_REQUEST, Integer.toString(skipNumber), "$skip");
    }
  }

  protected List<JPAPath> buildEntityPathList(final JPAEntityType jpaEntity) throws ODataApplicationException {

    try {
      return jpaEntity.getPathList();
    } catch (ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
  }

  protected final void buildSelectionAddNavigationAndSelect(final UriInfoResource uriResource,
      final Set<JPAPath> jpaPathList, final SelectOption select) throws ODataApplicationException,
      ODataJPAModelException {

    final boolean targetIsCollection = determineTargetIsCollection(uriResource);
    final String pathPrefix = Util.determinePropertyNavigationPrefix(uriResource.getUriResourceParts());

    if (Util.VALUE_RESOURCE.equals(pathPrefix))
      jpaPathList.addAll(buildPathValue(jpaEntity));
    else if (select == null || select.getSelectItems().isEmpty() || select.getSelectItems().get(0).isStar()) {
      if (pathPrefix == null || pathPrefix.isEmpty())
        copyNonCollectionProperties(jpaPathList, buildEntityPathList(jpaEntity));
      else {
        expandPath(jpaEntity, jpaPathList, pathPrefix, targetIsCollection);
      }
    } else {
      convertSelectIntoPath(select, jpaPathList, targetIsCollection, pathPrefix);
    }
  }

  /**
   * Creates the path to all properties that need to be selected from the database. A Property can be included for the
   * following reasons:
   * <ul>
   * <li>It is a key in order to be able to build the links</li>
   * <li>It is part of the $select system query option</li>
   * <li>It is the result of a navigation, which my be restricted by a $select</li>
   * <li>If is required to link $expand with result with the parent result</li>
   * <li>A stream is requested and the property contains the mime type</>
   * </ul>
   * Not included are collection properties.
   * @param uriResource
   * @return
   * @throws ODataApplicationException
   */
  protected Set<JPAPath> buildSelectionPathList(final UriInfoResource uriResource)
      throws ODataApplicationException {
    // TODO It is also possible to request all actions or functions available for each returned entity:
    // http://host/service/Products?$select=DemoService.*

    final Set<JPAPath> jpaPathList = new HashSet<>();
    final SelectOption select = uriResource.getSelectOption();
    try {
      buildSelectionAddNavigationAndSelect(uriResource, jpaPathList, select);
      buildSelectionAddMimeType(jpaEntity, jpaPathList);
      buildSelectionAddKeys(jpaEntity, jpaPathList);
      buildSelectionAddExpandSelection(uriResource, jpaPathList);
      buildSelectionAddETag(jpaEntity, jpaPathList);
    } catch (ODataJPAModelException e) {
      throw new ODataApplicationException(e.getLocalizedMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR
          .getStatusCode(), ODataJPAModelException.getLocales().nextElement(), e);
    }
    return jpaPathList;
  }

  /**
   *
   * @param orderByTarget
   * @param selectionPath List of the requested fields that of type description
   * @param query
   * @param lastInfo
   * @return
   * @throws ODataApplicationException
   * @throws JPANoSelectionException
   */
  protected Map<String, From<?, ?>> createFromClause(final List<JPAAssociationPath> orderByTarget,
      final Collection<JPAPath> selectionPath, final CriteriaQuery<?> query, final JPANavigationPropertyInfo lastInfo)
      throws ODataApplicationException, JPANoSelectionException {

    final HashMap<String, From<?, ?>> joinTables = new HashMap<>();
    // 1. Create navigation joins
    createFromClauseRoot(query, joinTables);
    target = root;
    createFromClauseNavigationJoins(joinTables);
    createFromClauseCollectionsJoins(joinTables);
    // 2. OrderBy navigation property
    createFromClauseOrderBy(orderByTarget, joinTables);
    // 3. Description Join determine
    createFromClauseDescriptionFields(selectionPath, joinTables);
    // 4. Collection Attribute Joins
    generateCollectionAttributeJoin(joinTables, selectionPath, lastInfo);

    return joinTables;
  }

  protected final Expression<Boolean> createKeyWhere(
      final List<JPANavigationPropertyInfo> info) throws ODataApplicationException {

    jakarta.persistence.criteria.Expression<Boolean> whereCondition = null;
    // Given key: Organizations('1')/Roles(...)
    for (JPANavigationPropertyInfo naviInfo : info) {
      if (naviInfo.getKeyPredicates() != null) {
        final JPAEntityType et = naviInfo.getEntityType();

        final From<?, ?> f = naviInfo.getFromClause();
        final List<UriParameter> keyPredicates = naviInfo.getKeyPredicates();
        whereCondition = createWhereByKey(f, whereCondition, keyPredicates, et);
      }
    }
    return whereCondition;
  }

  /**
   * If asc or desc is not specified, the service MUST order by the specified property in ascending order.
   * See: <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398305"
   * >OData Version 4.0 Part 1 - 11.2.5.2 System Query Option $orderby</a> <p>
   *
   */
  protected List<Order> createOrderByList(Map<String, From<?, ?>> joinTables, OrderByOption orderByOption)
      throws ODataApplicationException {
    // .../Organizations?$orderby=Address/Country --> one item, two resourcePaths
    // [...ComplexProperty,...PrimitiveProperty]
    // .../Organizations?$orderby=Roles/$count --> one item, two resourcePaths [...NavigationProperty,...Count]
    // .../Organizations?$orderby=Roles/$count desc,Address/Country asc -->two items
    //
    // SQL example to order by number of entities of the
    // SELECT t0."BusinessPartnerID",COUNT(t1."BusinessPartnerID")
    // FROM {oj "OLINGO"."nl.buildforce.olingo::BusinessPartner" t0
    // LEFT OUTER JOIN "OLINGO"."nl.buildforce.olingo::BusinessPartnerRole" t1
    // ON (t1."BusinessPartnerID" = t0."BusinessPartnerID")} //NOSONAR
    // WHERE (t0."Type" = ?)
    // GROUP BY t0."BusinessPartnerID"
    // ORDER BY COUNT(t1."BusinessPartnerID") DESC

    // TODO Functions and orderBy: Part 1 - 11.5.3.1 Invoking a Function

    // final int handle = debugger.startRuntimeMeasurement(this, "createOrderByList");
    final List<Order> orders = new ArrayList<>();
    if (orderByOption != null) {
      try {
        for (final OrderByItem orderByItem : orderByOption.getOrders()) {
          final nl.buildforce.olingo.server.api.uri.queryoption.expression.Expression expression = orderByItem.getExpression();
          if (expression instanceof Member) {
            final UriInfoResource resourcePath = ((Member) expression).getResourcePath();
            JPAStructuredType type = jpaEntity;
            Path<?> p = target;
            StringBuilder externalPath = new StringBuilder();
            for (final UriResource uriResourceItem : resourcePath.getUriResourceParts()) {
              if (uriResourceItem instanceof UriResourcePrimitiveProperty
                  && !((UriResourceProperty) uriResourceItem).isCollection()) {
                p = p.get(type.getAttribute((UriResourceProperty) uriResourceItem).getInternalName());
                final JPAPath path = type.getPath(((UriResourceProperty) uriResourceItem).getProperty().getName());
                if (!path.isPartOfGroups(groups)) {
                  throw new ODataJPAQueryException(QUERY_PREPARATION_NOT_ALLOWED_MEMBER, HttpStatusCode.FORBIDDEN,
                      path.getAlias());
                }
                addOrderByExpression(orders, orderByItem, p);
              } else if (uriResourceItem instanceof UriResourceComplexProperty
                  && !((UriResourceProperty) uriResourceItem).isCollection()) {
                final JPAAttribute attribute = type.getAttribute((UriResourceProperty) uriResourceItem);
                addPathElement(externalPath, attribute);
                p = p.get(attribute.getInternalName());
                type = attribute.getStructuredType();
              } else if (uriResourceItem instanceof UriResourceNavigation
                  || (uriResourceItem instanceof UriResourceProperty
                      && ((UriResourceProperty) uriResourceItem).isCollection())) {

                if (uriResourceItem instanceof UriResourceNavigation)
                  externalPath.append(((UriResourceNavigation) uriResourceItem).getProperty().getName());
                else
                  externalPath.append(((UriResourceProperty) uriResourceItem).getProperty().getName());
                From<?, ?> join = joinTables.get(externalPath.toString());
                addOrderByExpression(orders, orderByItem, cb.count(join));
              }
            }
          }
        }
      } catch (ODataJPAModelException e) {
        // debugger.stopRuntimeMeasurement(handle);
        throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
      }
    }
      // Ensure results get ordered by primary key. By this it is ensured that the results will match the sub-select
      // results for $expand with $skip and $top

    // debugger.stopRuntimeMeasurement(handle);
    return orders;
  }

  protected jakarta.persistence.criteria.Expression<Boolean> createProtectionWhere(
      final Optional<JPAODataClaimProvider> claimsProvider) throws ODataJPAQueryException {

    jakarta.persistence.criteria.Expression<Boolean> restriction = null;
    for (final JPANavigationPropertyInfo navi : navigationInfo) { // for all participating entity types/tables
      final JPAEntityType et = navi.getEntityType();
      final From<?, ?> from = navi.getFromClause();
      restriction = addWhereClause(restriction, createProtectionWhereForEntityType(claimsProvider, et, from));
    }
    return restriction;
  }

  /**
   * The value of the $select query option is a comma-separated list of <b>properties</b>, qualified action names,
   * qualified function names, the <b>star operator (*)</b>, or the star operator prefixed with the namespace or alias
   * of the schema in order to specify all operations defined in the schema. See:
   * <a
   * href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398297"
   * >OData Version 4.0 Part 1 - 11.2.4.1 System Query Option $select</a> <p>
   * See also:
   * <a
   * href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398163"
   * >OData Version 4.0 Part 2 - 5.1.3 System Query Option $select</a>
   *

   * @param joinTables
   * @param requestedProperties
   * @param target
   * @param groups
   * @return
   * @throws ODataApplicationException
   */
  protected List<Selection<?>> createSelectClause(final Map<String, From<?, ?>> joinTables, // NOSONAR
      final Collection<JPAPath> requestedProperties, final From<?, ?> target, final List<String> groups)
      throws ODataApplicationException { // NOSONAR Allow subclasses to throw an exception

    // final int handle = debugger.startRuntimeMeasurement(this, "createSelectClause");
    final List<Selection<?>> selections = new ArrayList<>();

    // Build select clause
    for (final JPAPath jpaPath : requestedProperties) {
      if (jpaPath.isPartOfGroups(groups)) {
        final Path<?> p = ExpressionUtil.convertToCriteriaPath(joinTables, target, jpaPath.getPath());
        p.alias(jpaPath.getAlias());
        selections.add(p);
      }
    }
    // debugger.stopRuntimeMeasurement(handle);
    return selections;
  }

  protected jakarta.persistence.criteria.Expression<Boolean> createWhere(final UriInfoResource uriInfo,
      final List<JPANavigationPropertyInfo> navigationInfo) throws ODataApplicationException {

    // final int handle = debugger.startRuntimeMeasurement(this, "createWhere");
    jakarta.persistence.criteria.Expression<Boolean> whereCondition;
    // Given keys: Organizations('1')/Roles(...)
    try {
      whereCondition = createKeyWhere(navigationInfo);
    } catch (ODataApplicationException e) {
      // debugger.stopRuntimeMeasurement(handle);
      throw e;
    }

    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398094
    // https://tools.oasis-open.org/version-control/browse/wsvn/odata/trunk/spec/ABNF/odata-abnf-construction-rules.txt
    try {
      whereCondition = addWhereClause(whereCondition, navigationInfo.get(navigationInfo.size() - 1).getFilterCompiler()
          .compile());
    } catch (ExpressionVisitException e) {
      // debugger.stopRuntimeMeasurement(handle);
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.BAD_REQUEST, e);
    }

    if (uriInfo.getSearchOption() != null && uriInfo.getSearchOption().getSearchExpression() != null)
      whereCondition = addWhereClause(whereCondition,
          context.getDatabaseProcessor().createSearchWhereClause(cb, this.cq, target, jpaEntity, uriInfo
              .getSearchOption()));

    // debugger.stopRuntimeMeasurement(handle);
    return whereCondition;
  }

  protected JPAAssociationPath determineAssociation(final UriResourcePartTyped naviStart,
      final StringBuilder associationName) throws ODataApplicationException {

    JPAEntityType naviStartType;
    try {
      if (naviStart instanceof UriResourceEntitySet)
        naviStartType = sd.getEntity(naviStart.getType());
      else
        naviStartType = sd.getEntity(((UriResourceNavigation) naviStart).getProperty().getType());
      return naviStartType.getAssociationPath(associationName.toString());
    } catch (ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
  }

  protected JPANavigationPropertyInfo determineLastInfo(List<JPANavigationPropertyInfo> naviInfo) {
    return naviInfo.isEmpty() ? null : naviInfo.get(naviInfo.size() - 1);
  }

  protected final boolean determineTargetIsCollection(final UriInfoResource uriResource) {

    final UriResource last = !uriResource.getUriResourceParts().isEmpty() ? uriResource.getUriResourceParts().get(
        uriResource.getUriResourceParts().size() - 1) : null;
    return (last instanceof UriResourceProperty && ((UriResourceProperty) last).isCollection());
  }

  protected void expandPath(final JPAEntityType jpaEntity, final Collection<JPAPath> jpaPathList,
      final String selectItem, final boolean targetIsCollection) throws ODataJPAModelException, ODataJPAQueryException {

    final JPAPath selectItemPath = jpaEntity.getPath(selectItem);
    if (selectItemPath == null)
      throw new ODataJPAQueryException(MessageKeys.QUERY_PREPARATION_INVALID_SELECTION_PATH,
          HttpStatusCode.BAD_REQUEST);
    if (selectItemPath.getLeaf().isComplex()) {
      // Complex Type
      final List<JPAPath> c = jpaEntity.searchChildPath(selectItemPath);
      if (targetIsCollection)
        jpaPathList.addAll(c);
      else
        copyNonCollectionProperties(jpaPathList, c);
    } else if (!selectItemPath.getLeaf().isCollection() || targetIsCollection) {// Primitive Type
      jpaPathList.add(selectItemPath);
    }
  }

  protected List<JPAPath> extractDescriptionAttributes(final Collection<JPAPath> jpaPathList) {

    final List<JPAPath> result = new ArrayList<>();
    for (final JPAPath p : jpaPathList)
      if (p.getLeaf() instanceof JPADescriptionAttribute)
        result.add(p);
    return result;
  }

  /*
   * Create the join condition for a collection property. This attribute can be part of structure type, therefore the
   * path to the collection property needs to be traversed
   */
  protected void generateCollectionAttributeJoin(final Map<String, From<?, ?>> joinTables,
      final Collection<JPAPath> jpaPathList, final JPANavigationPropertyInfo lastInfo) throws JPANoSelectionException,
      ODataJPAProcessorException {

    for (JPAPath path : jpaPathList) {
      // 1. check if path contains collection attribute
      final JPAElement collection = findCollection(lastInfo, path);
      // 2. Check if join exists and create join if not
      addCollection(joinTables, path, collection);
    }
  }

  @Override
  protected Locale getLocale() {
    return locale;
  }

  @Override
  JPAODataCRUDContextAccess getContext() {
    return context;
  }

  private void addCollection(final Map<String, From<?, ?>> joinTables, final JPAPath path,
      final JPAElement collection) {

    if (collection != null && !joinTables.containsKey(collection.getExternalName())) {
      From<?, ?> f = target;
      for (JPAElement element : path.getPath()) {
        f = f.join(element.getInternalName());
        if (element instanceof JPACollectionAttribute) {
          break;
        }
      }
      joinTables.put(collection.getExternalName(), f);
    }
  }

  private void addOrderByExpression(final List<Order> orders, final OrderByItem orderByItem,
      jakarta.persistence.criteria.Expression<?> expression) {

    if (orderByItem.isDescending())
      orders.add(cb.desc(expression));
    else
      orders.add(cb.asc(expression));
  }

  private void addPathElement(StringBuilder externalPath, JPAAttribute attribute) {
    externalPath.append(attribute.getExternalName());
    externalPath.append(JPAPath.PATH_SEPARATOR);

  }

  // Only for streams e.g. .../OrganizationImages('9')/$value
  private List<JPAPath> buildPathValue(final JPAEntityType jpaEntity)
      throws ODataApplicationException {

    List<JPAPath> jpaPathList = new ArrayList<>();
    try {
      // Stream value
      jpaPathList.add(jpaEntity.getStreamAttributePath());
      jpaPathList.addAll(jpaEntity.getKeyPath());

    } catch (ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
    return jpaPathList;
  }

  /**
   * In order to be able to link the result of a expand query with the super-ordinate query it is necessary to ensure
   * that the join columns are selected.<br>
   * The same columns are required for the count query, for select as well as order by.
   * @param uriResource
   * @param jpaPathList
   * @throws ODataApplicationException
   * @throws ODataJPAQueryException
   */
  private void buildSelectionAddExpandSelection(final UriInfoResource uriResource, Collection<JPAPath> jpaPathList)
      throws ODataApplicationException {

    final Map<JPAExpandItem, JPAAssociationPath> associationPathList = Util.determineAssociations(sd, uriResource
        .getUriResourceParts(), uriResource.getExpandOption());
    if (!associationPathList.isEmpty()) {
      final List<JPAPath> tmpPathList = new ArrayList<>(jpaPathList);
      final List<JPAPath> addPathList = new ArrayList<>();

      Collections.sort(tmpPathList);
      for (final Entry<JPAExpandItem, JPAAssociationPath> item : associationPathList.entrySet()) {
        final JPAAssociationPath associationPath = item.getValue();
        try {
          for (final JPAPath joinItem : associationPath.getLeftColumnsList()) {
            final int pathIndex = Collections.binarySearch(tmpPathList, joinItem);
            final int insertIndex = Collections.binarySearch(addPathList, joinItem);
            if (pathIndex < 0 && insertIndex < 0)
              addPathList.add(Math.abs(insertIndex) - 1, joinItem);
          }
        } catch (ODataJPAModelException e) {
          throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
        }
      }
      jpaPathList.addAll(addPathList);
    }
  }

  private void buildSelectionAddETag(final JPAEntityType jpaEntity, final Collection<JPAPath> jpaPathList)
      throws ODataJPAModelException {
    if (jpaEntity.hasEtag())
      jpaPathList.add(jpaEntity.getEtagPath());

  }

  private void buildSelectionAddKeys(final JPAEntityType jpaEntity, final Collection<JPAPath> jpaPathList)
      throws ODataJPAModelException {

    final List<? extends JPAAttribute> jpaKeyList = new ArrayList<>(jpaEntity.getKey());

    for (JPAPath selectItemPath : jpaPathList) {
      for (int i = 0; i < jpaKeyList.size(); i++) {
        JPAAttribute key = jpaKeyList.get(i);
        if (key.getExternalFQN().equals(selectItemPath.getLeaf().getExternalFQN()))
          jpaKeyList.remove(i);
      }
      if (jpaKeyList.isEmpty())
        break;
    }
    for (final JPAAttribute key : jpaKeyList) {
      jpaPathList.add(jpaEntity.getPath(key.getExternalName()));
    }
  }

  private void buildSelectionAddMimeType(final JPAEntityType jpaEntity, final Collection<JPAPath> jpaPathList)
      throws ODataJPAModelException {

    if (jpaEntity.hasStream()) {
      final JPAPath mimeTypeAttribute = jpaEntity.getContentTypeAttributePath();
      if (mimeTypeAttribute != null) {
        jpaPathList.add(mimeTypeAttribute);
      }
    }
  }

  private boolean checkCollectionIsPartOfGroup(final String collectionPath) throws ODataJPAProcessorException {

    try {
      final JPAPath path = jpaEntity.getPath(collectionPath);
      return path.isPartOfGroups(groups);
    } catch (ODataJPAModelException e) {
      throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  private void convertSelectIntoPath(final SelectOption select, final Collection<JPAPath> jpaPathList,
      final boolean targetIsCollection, final String pathPrefix) throws ODataJPAModelException, ODataJPAQueryException {

    for (SelectItem sItem : select.getSelectItems()) {
      String pathItem = sItem.getResourcePath().getUriResourceParts().stream().map(UriResource::getSegmentValue).collect(Collectors.joining(JPAPath.PATH_SEPARATOR));
      expandPath(jpaEntity, jpaPathList, pathPrefix.isEmpty() ? pathItem : pathPrefix + "/" + pathItem,
          targetIsCollection);
    }
  }

  /**
   * Skips all those properties that are or belong to a collection property. E.g
   * (Organization)Comment or (Person)InhouseAddress/Room
   * @param jpaPathList
   * @param c
   */
  private void copyNonCollectionProperties(final Collection<JPAPath> jpaPathList, final List<JPAPath> c) {
    for (JPAPath p : c) {
      boolean skip = false;
      for (JPAElement pathElement : p.getPath()) {
        if (pathElement instanceof JPAAttribute && ((JPAAttribute) pathElement).isCollection()) {
          skip = true;
          break;
        }
      }
      if (!skip)
        jpaPathList.add(p);
    }
  }

  private void createFromClauseCollectionsJoins(final HashMap<String, From<?, ?>> joinTables)
      throws ODataJPAQueryException {

    try {
      if (lastInfo.getAssociationPath() != null
          && lastInfo.getAssociationPath().getLeaf() instanceof JPACollectionAttribute
          && !uriResource.getUriResourceParts().isEmpty()
          && uriResource.getUriResourceParts().get(uriResource.getUriResourceParts().size() - 1)
              .getKind() == UriResourceKind.complexProperty) {
        Path<?> p = target;
        JPAElement element = null;
        for (JPAElement pathElement : lastInfo.getAssociationPath().getPath()) {
          p = p.get(pathElement.getInternalName());
          element = pathElement;
        }
        joinTables.put(lastInfo.getAssociationPath().getAlias(), (From<?, ?>) p);
        final JPAEntityType targetEt = (JPAEntityType) ((JPAAssociationAttribute) element).getTargetEntity();
        final JPAOperationConverter converter = new JPAOperationConverter(cb, context.getOperationConverter());
        final JPAODataRequestContextAccess subContext = new JPAODataRequestContextImpl(uriResource, requestContext);
        lastInfo.setFilterCompiler(new JPAFilterCrossCompiler(odata, sd, targetEt, converter, this, (From<?, ?>) p,
            lastInfo.getAssociationPath(), subContext));
      } else {
        final JPAOperationConverter converter = new JPAOperationConverter(cb, context.getOperationConverter());
        final JPAODataRequestContextAccess subContext = new JPAODataRequestContextImpl(uriResource, requestContext);
        lastInfo.setFilterCompiler(new JPAFilterCrossCompiler(odata, sd, jpaEntity, converter, this, lastInfo
            .getAssociationPath(), subContext));
      }
    } catch (ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.BAD_REQUEST, e);
    }
    lastInfo.setFromClause(target);
  }

  private void createFromClauseDescriptionFields(final Collection<JPAPath> selectionPath,
      final HashMap<String, From<?, ?>> joinTables) throws ODataApplicationException {
    final List<JPAPath> descriptionFields = extractDescriptionAttributes(selectionPath);
    for (JPANavigationPropertyInfo info : this.navigationInfo) {
      if (info.getFilterCompiler() != null) {
        generateDescriptionJoin(joinTables,
            determineAllDescriptionPath(info.getFromClause() == target ? descriptionFields : Collections.emptyList(),
                info.getFilterCompiler()), info.getFromClause());
      }
    }
  }

  /**
   * Completes NavigationInfo and add Joins for navigation parts e.g. from <code>../Organizations('3')/Roles</code>
   * @param joinTables
   */
  private void createFromClauseNavigationJoins(final HashMap<String, From<?, ?>> joinTables) {

    for (int i = 0; i < this.navigationInfo.size() - 1; i++) {
      final JPANavigationPropertyInfo naviInfo = this.navigationInfo.get(i);

      EdmType castType;
      if (naviInfo.getUriResource() instanceof UriResourceNavigation)
        castType = ((UriResourceNavigation) naviInfo.getUriResource()).getTypeFilterOnEntry();
      else
        castType = ((UriResourceEntitySet) naviInfo.getUriResource()).getTypeFilterOnEntry();
      if (castType != null)
        target = (From<?, ?>) target.as(sd.getEntity(castType.getFullQualifiedName()).getTypeClass());
      naviInfo.setFromClause(target);
      if (naviInfo.getUriInfo() != null && naviInfo.getUriInfo().getFilterOption() != null) {
        final JPAOperationConverter converter = new JPAOperationConverter(cb, context.getOperationConverter());
        final JPAODataRequestContextAccess subContext = new JPAODataRequestContextImpl(naviInfo.getUriInfo(),
            requestContext);
        naviInfo.setFilterCompiler(new JPAFilterCrossCompiler(odata, sd, naviInfo.getEntityType(), converter, this,
            naviInfo.getFromClause(), null, subContext));
      }
      target = createJoinFromPath(naviInfo.getAssociationPath().getAlias(), naviInfo.getAssociationPath().getPath(),
          target, JoinType.INNER);
      joinTables.put(naviInfo.getAssociationPath().getAlias(), target);
    }
  }

  /**
   * Add from clause that is needed for orderby clauses that are not part of the navigation part e.g.
   * <code>"Organizations?$orderby=Roles/$count desc,Address/Region desc"</code>
   * @param orderByTarget
   * @param joinTables
   */
  private void createFromClauseOrderBy(final List<JPAAssociationPath> orderByTarget,
      final HashMap<String, From<?, ?>> joinTables) {
    for (final JPAAssociationPath orderBy : orderByTarget) {
      From<?, ?> join = target;
      for (JPAElement o : orderBy.getPath())
        join = join.join(o.getInternalName(), JoinType.LEFT);
      // Take on condition from JPA metadata; no explicit on
      joinTables.put(orderBy.getAlias(), join);
    }
  }

  /**
   * Start point of a Join Query e.g. triggered by <code>../Organizations</code> or
   * <code>../Organizations('3')/Roles</code>
   * @param query
   * @param joinTables
   */
  private void createFromClauseRoot(final CriteriaQuery<?> query, final HashMap<String, From<?, ?>> joinTables) {
    final JPAEntityType sourceEt = this.navigationInfo.get(0).getEntityType();
    this.root = query.from(sourceEt.getTypeClass());
    joinTables.put(sourceEt.getExternalFQN().getFullQualifiedNameAsString(), root);
  }

  private Set<JPAPath> determineAllDescriptionPath(List<JPAPath> descriptionFields, JPAFilterCompiler filter)
      throws ODataApplicationException {

    Set<JPAPath> allPath = new HashSet<>(descriptionFields);
    for (JPAPath path : filter.getMember()) {
      if (path.getLeaf() instanceof JPADescriptionAttribute)
        allPath.add(path);
    }
    return allPath;
  }

  private JPAElement findCollection(final JPANavigationPropertyInfo lastInfo, final JPAPath path)
      throws ODataJPAProcessorException, JPANoSelectionException {

    JPAElement collection = null;
    final StringBuilder collectionPath = new StringBuilder();
    for (JPAElement element : path.getPath()) {
      collectionPath.append(element.getExternalName());
      if (element instanceof JPACollectionAttribute) {
        if (checkCollectionIsPartOfGroup(collectionPath.toString())) {
          collection = element;
        } else if (lastInfo.getAssociationPath() != null
            && (lastInfo.getAssociationPath().getLeaf() instanceof JPACollectionAttribute)) {
          throw new JPANoSelectionException();
        }
        break;
      }
      collectionPath.append(JPAPath.PATH_SEPARATOR);
    }
    return collection;
  }

  protected <Y extends Comparable<? super Y>> jakarta.persistence.criteria.Expression<Boolean> createBoundary(
      final List<JPANavigationPropertyInfo> info, final Optional<JPAKeyBoundary> keyBoundary)
      throws ODataJPAQueryException {

    if (keyBoundary.isPresent()) {
      // Given key: Organizations('1')/Roles(...)
      // First is the root
      final JPANavigationPropertyInfo naviInfo = info.get(keyBoundary.get().getNoHops() - 1);
      try {
        final JPAEntityType et = naviInfo.getEntityType();
        final From<?, ?> f = naviInfo.getFromClause();

        if (keyBoundary.get().getKeyBoundary().hasUpperBoundary()) {
          return createBoundaryWithUpper(et, f, keyBoundary.get().getKeyBoundary());
        } else {
          return createBoundaryEquals(et, f, keyBoundary.get().getKeyBoundary());
        }
      } catch (ODataJPAModelException e) {
        throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private <Y extends Comparable<? super Y>> jakarta.persistence.criteria.Expression<Boolean> createBoundaryWithUpper(
      final JPAEntityType et,
      final From<?, ?> f, final JPAKeyPair jpaKeyPair)
      throws ODataJPAModelException {

    final List<JPAAttribute> keyElements = et.getKey();
    jakarta.persistence.criteria.Expression<Boolean> lowerExpression = null;
    jakarta.persistence.criteria.Expression<Boolean> upperExpression = null;
    for (int primaryIndex = 0; primaryIndex < keyElements.size(); primaryIndex++) {
      for (int secondaryIndex = primaryIndex; secondaryIndex < keyElements.size(); secondaryIndex++) {
        final JPAAttribute keyElement = keyElements.get(secondaryIndex);
        final Path<Y> keyPath = (Path<Y>) ExpressionUtil.convertToCriteriaPath(f,
            et.getPath(keyElement.getExternalName()).getPath());
        final Y lowerBoundary = jpaKeyPair.getMinElement(keyElement);
        final Y upperBoundary = jpaKeyPair.getMaxElement(keyElement);
        if (secondaryIndex == primaryIndex) {
          if (primaryIndex == 0) {
            lowerExpression = cb.greaterThanOrEqualTo(keyPath, lowerBoundary);
            upperExpression = cb.lessThanOrEqualTo(keyPath, upperBoundary);
          } else {
            lowerExpression = cb.or(lowerExpression, cb.greaterThan(keyPath, lowerBoundary));
            upperExpression = cb.or(upperExpression, cb.lessThan(keyPath, upperBoundary));
          }
        } else {
          lowerExpression = cb.and(lowerExpression, cb.equal(keyPath, lowerBoundary));
          upperExpression = cb.and(upperExpression, cb.equal(keyPath, upperBoundary));
        }
      }

    }
    return cb.and(lowerExpression, upperExpression);
  }

  @SuppressWarnings("unchecked")
  private <Y extends Comparable<? super Y>> jakarta.persistence.criteria.Expression<Boolean> createBoundaryEquals(
      final JPAEntityType et, final From<?, ?> f, final JPAKeyPair jpaKeyPair) throws ODataJPAModelException {

    jakarta.persistence.criteria.Expression<Boolean> whereCondition = null;
    for (final JPAAttribute keyElement : et.getKey()) {
      final Path<Y> keyPath = (Path<Y>) ExpressionUtil.convertToCriteriaPath(f, et.getPath(keyElement.getExternalName())
          .getPath());
      final Expression<Boolean> eqFragment = cb.equal(keyPath, jpaKeyPair.getMin().get(
          keyElement));
      if (whereCondition == null)
        whereCondition = eqFragment;
      else
        whereCondition = cb.and(whereCondition, eqFragment);
    }
    return whereCondition;
  }

}