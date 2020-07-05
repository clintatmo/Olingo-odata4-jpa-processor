/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.buildforce.olingo.commons.api.edm.provider;

import java.util.ArrayList;
import java.util.List;

import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;

/**
 * The type Csdl entity container.
 */
public class CsdlEntityContainer extends CsdlAbstractEdmItem implements CsdlNamed, CsdlAnnotatable {

  private String name;

  private FullQualifiedName extendsContainer;

  private List<CsdlEntitySet> entitySets = new ArrayList<>();

  private List<CsdlActionImport> actionImports = new ArrayList<>();

  private List<CsdlFunctionImport> functionImports = new ArrayList<>();

  private List<CsdlSingleton> singletons = new ArrayList<>();

  private List<CsdlAnnotation> annotations = new ArrayList<>();

  // Annotations
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   * @return the name
   */
  public CsdlEntityContainer setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets extends container.
   *
   * @return the extends container
   */
  public String getExtendsContainer() {
    if (extendsContainer != null) {
      return extendsContainer.getFullQualifiedNameAsString();
    }
    return null;
  }

  /**
   * Gets extends container fQN.
   *
   * @return the extends container fQN
   */
  public FullQualifiedName getExtendsContainerFQN() {
    return extendsContainer;
  }

  /**
   * Sets extends container.
   *
   * @param extendsContainer the extends container
   * @return the extends container
   */
  public CsdlEntityContainer setExtendsContainer(String extendsContainer) {
    this.extendsContainer = new FullQualifiedName(extendsContainer);
    return this;
  }

  /**
   * Gets entity sets.
   *
   * @return the entity sets
   */
  public List<CsdlEntitySet> getEntitySets() {
    return entitySets;
  }

  /**
   * Gets entity set.
   *
   * @param name the name
   * @return the entity set
   */
  public CsdlEntitySet getEntitySet(String name) {
    return getOneByName(name, getEntitySets());
  }

  /**
   * Sets entity sets.
   *
   * @param entitySets the entity sets
   * @return the entity sets
   */
  public CsdlEntityContainer setEntitySets(List<CsdlEntitySet> entitySets) {
    this.entitySets = entitySets;
    return this;
  }

  /**
   * Gets action imports.
   *
   * @return the action imports
   */
  public List<CsdlActionImport> getActionImports() {
    return actionImports;
  }

  /**
   * Gets the first action import with given name.
   *
   * @param name name.
   * @return action import.
   */
  public CsdlActionImport getActionImport(String name) {
    return getOneByName(name, getActionImports());
  }

  /**
   * Gets all action imports with given name.
   *
   * @param name name.
   * @return action imports.
   */
  public List<CsdlActionImport> getActionImports(String name) {
    return getAllByName(name, getActionImports());
  }

  /**
   * Sets action imports.
   *
   * @param actionImports the action imports
   * @return the action imports
   */
  public CsdlEntityContainer setActionImports(List<CsdlActionImport> actionImports) {
    this.actionImports = actionImports;
    return this;
  }

  /**
   * Gets function imports.
   *
   * @return the function imports
   */
  public List<CsdlFunctionImport> getFunctionImports() {
    return functionImports;
  }

  /**
   * Gets the first function import with given name.
   *
   * @param name name.
   * @return function import.
   */
  public CsdlFunctionImport getFunctionImport(String name) {
    return getOneByName(name, getFunctionImports());
  }

  /**
   * Gets all function imports with given name.
   *
   * @param name name.
   * @return function imports.
   */
  public List<CsdlFunctionImport> getFunctionImports(String name) {
    return getAllByName(name, getFunctionImports());
  }

  /**
   * Sets function imports.
   *
   * @param functionImports the function imports
   * @return the function imports
   */
  public CsdlEntityContainer setFunctionImports(List<CsdlFunctionImport> functionImports) {
    this.functionImports = functionImports;
    return this;
  }

  /**
   * Gets singletons.
   *
   * @return the singletons
   */
  public List<CsdlSingleton> getSingletons() {
    return singletons;
  }

  /**
   * Gets singleton.
   *
   * @param name the name
   * @return the singleton
   */
  public CsdlSingleton getSingleton(String name) {
    return getOneByName(name, getSingletons());
  }

  /**
   * Sets singletons.
   *
   * @param singletons the singletons
   * @return the singletons
   */
  public CsdlEntityContainer setSingletons(List<CsdlSingleton> singletons) {
    this.singletons = singletons;
    return this;
  }

  @Override
  public List<CsdlAnnotation> getAnnotations() {
    return annotations;
  }
  
  public CsdlEntityContainer setAnnotations(List<CsdlAnnotation> annotations) {
    this.annotations = annotations;
    return this;
  }

}
