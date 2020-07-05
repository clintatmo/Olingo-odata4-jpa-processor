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
package nl.buildforce.olingo.server.core.uri;

import nl.buildforce.olingo.commons.api.edm.EdmAction;
import nl.buildforce.olingo.commons.api.edm.EdmActionImport;
import nl.buildforce.olingo.commons.api.edm.EdmType;
import nl.buildforce.olingo.server.api.uri.UriResourceAction;
import nl.buildforce.olingo.server.api.uri.UriResourceKind;

/**
 * Implementation of the {@link UriResourceAction} interface. This class does not extend
 * {@link UriResourceTypedImpl UriResourceTypedImpl} since that would allow type
 * filters and subsequent path segments.
 */
public class UriResourceActionImpl extends UriResourceImpl implements UriResourceAction {

  private final EdmActionImport actionImport;
  private final EdmAction action;

  public UriResourceActionImpl(EdmActionImport actionImport) {
    super(UriResourceKind.action);
    this.actionImport = actionImport;
      action = actionImport.getUnboundAction();
  }

  public UriResourceActionImpl(EdmAction action) {
    super(UriResourceKind.action);
      actionImport = null;
    this.action = action;
  }

  @Override
  public EdmAction getAction() {
    return action;
  }

  @Override
  public EdmActionImport getActionImport() {
    return actionImport;
  }

  @Override
  public boolean isCollection() {
    return action.getReturnType() != null && action.getReturnType().isCollection();
  }

  @Override
  public EdmType getType() {
    return action.getReturnType() == null ? null : action.getReturnType().getType();
  }

  @Override
  public String getSegmentValue(boolean includeFilters) {
    return actionImport == null ? (action == null ? "" : action.getName()) : actionImport.getName();
  }

  @Override
  public String getSegmentValue() {
    return getSegmentValue(false);
  }

  @Override
  public String toString(boolean includeFilters) {
    return getSegmentValue(includeFilters);
  }
}