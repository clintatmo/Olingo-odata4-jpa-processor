/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.commons.core.edm.annotation;

import nl.buildforce.olingo.commons.api.edm.Edm;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmAnd;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmAnnotationPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmApply;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmCast;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmCollection;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmDynamicExpression;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmEq;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmGe;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmGt;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmIf;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmIsOf;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLabeledElement;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLabeledElementReference;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLe;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLt;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNavigationPropertyPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNe;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNot;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNull;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmOr;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPropertyPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPropertyValue;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmRecord;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmUrlRef;

public abstract class AbstractEdmDynamicExpression extends AbstractEdmExpression implements EdmDynamicExpression {

  public AbstractEdmDynamicExpression(Edm edm, String name) {
    super(edm, name);
  }

  @Override
  public boolean isNot() {
    return this instanceof EdmNot;
  }

  @Override
  public EdmNot asNot() {
    return isNot() ? (EdmNot) this : null;
  }

  @Override
  public boolean isAnd() {
    return this instanceof EdmAnd;
  }

  @Override
  public EdmAnd asAnd() {
    return isAnd() ? (EdmAnd) this : null;
  }

  @Override
  public boolean isOr() {
    return this instanceof EdmOr;
  }

  @Override
  public EdmOr asOr() {
    return isOr() ? (EdmOr) this : null;
  }

  @Override
  public boolean isEq() {
    return this instanceof EdmEq;
  }

  @Override
  public EdmEq asEq() {
    return isEq() ? (EdmEq) this : null;
  }

  @Override
  public boolean isNe() {
    return this instanceof EdmNe;
  }

  @Override
  public EdmNe asNe() {
    return isNe() ? (EdmNe) this : null;
  }

  @Override
  public boolean isGt() {
    return this instanceof EdmGt;
  }

  @Override
  public EdmGt asGt() {
    return isGt() ? (EdmGt) this : null;
  }

  @Override
  public boolean isGe() {
    return this instanceof EdmGe;
  }

  @Override
  public EdmGe asGe() {
    return isGe() ? (EdmGe) this : null;
  }

  @Override
  public boolean isLt() {
    return this instanceof EdmLt;
  }

  @Override
  public EdmLt asLt() {
    return isLt() ? (EdmLt) this : null;
  }

  @Override
  public boolean isLe() {
    return this instanceof EdmLe;
  }

  @Override
  public EdmLe asLe() {
    return isLe() ? (EdmLe) this : null;
  }

  @Override
  public boolean isAnnotationPath() {
    return this instanceof EdmAnnotationPath;
  }

  @Override
  public EdmAnnotationPath asAnnotationPath() {
    return isAnnotationPath() ? (EdmAnnotationPath) this : null;
  }

  @Override
  public boolean isApply() {
    return this instanceof EdmApply;
  }

  @Override
  public EdmApply asApply() {
    return isApply() ? (EdmApply) this : null;
  }

  @Override
  public boolean isCast() {
    return this instanceof EdmCast;
  }

  @Override
  public EdmCast asCast() {
    return isCast() ? (EdmCast) this : null;
  }

  @Override
  public boolean isCollection() {
    return this instanceof EdmCollection;
  }

  @Override
  public EdmCollection asCollection() {
    return isCollection() ? (EdmCollection) this : null;
  }

  @Override
  public boolean isIf() {
    return this instanceof EdmIf;
  }

  @Override
  public EdmIf asIf() {
    return isIf() ? (EdmIf) this : null;
  }

  @Override
  public boolean isIsOf() {
    return this instanceof EdmIsOf;
  }

  @Override
  public EdmIsOf asIsOf() {
    return isIsOf() ? (EdmIsOf) this : null;
  }

  @Override
  public boolean isLabeledElement() {
    return this instanceof EdmLabeledElement;
  }

  @Override
  public EdmLabeledElement asLabeledElement() {
    return isLabeledElement() ? (EdmLabeledElement) this : null;
  }

  @Override
  public boolean isLabeledElementReference() {
    return this instanceof EdmLabeledElementReference;
  }

  @Override
  public EdmLabeledElementReference asLabeledElementReference() {
    return isLabeledElementReference() ? (EdmLabeledElementReference) this : null;
  }

  @Override
  public boolean isNull() {
    return this instanceof EdmNull;
  }

  @Override
  public EdmNull asNull() {
    return isNull() ? (EdmNull) this : null;
  }

  @Override
  public boolean isNavigationPropertyPath() {
    return this instanceof EdmNavigationPropertyPath;
  }

  @Override
  public EdmNavigationPropertyPath asNavigationPropertyPath() {
    return isNavigationPropertyPath() ? (EdmNavigationPropertyPath) this : null;
  }

  @Override
  public boolean isPath() {
    return this instanceof EdmPath;
  }

  @Override
  public EdmPath asPath() {
    return isPath() ? (EdmPath) this : null;
  }

  @Override
  public boolean isPropertyPath() {
    return this instanceof EdmPropertyPath;
  }

  @Override
  public EdmPropertyPath asPropertyPath() {
    return isPropertyPath() ? (EdmPropertyPath) this : null;
  }

  @Override
  public boolean isPropertyValue() {
    return this instanceof EdmPropertyValue;
  }

  @Override
  public EdmPropertyValue asPropertyValue() {
    return isPropertyValue() ? (EdmPropertyValue) this : null;
  }

  @Override
  public boolean isRecord() {
    return this instanceof EdmRecord;
  }

  @Override
  public EdmRecord asRecord() {
    return isRecord() ? (EdmRecord) this : null;
  }

  @Override
  public boolean isUrlRef() {
    return this instanceof EdmUrlRef;
  }

  @Override
  public EdmUrlRef asUrlRef() {
    return isUrlRef() ? (EdmUrlRef) this : null;
  }

}