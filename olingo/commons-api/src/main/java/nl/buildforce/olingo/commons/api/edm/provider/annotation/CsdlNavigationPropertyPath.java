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
package nl.buildforce.olingo.commons.api.edm.provider.annotation;

/**
 * The edm:NavigationPropertyPath expression provides a value for terms or term properties that specify the
 * built-in abstract type Edm.NavigationPropertyPath
 */
public class CsdlNavigationPropertyPath extends CsdlDynamicExpression {
  
  private String value;

  /**
   * Returns the navigation property path itself.
   *
   * @return navigation property
   */
  public String getValue() {
    return value;
  }

  public CsdlNavigationPropertyPath setValue(String value) {
    this.value = value;
    return this;
  }
  
  @Override
  public boolean equals (Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CsdlNavigationPropertyPath)) {
      return false;
    }
    CsdlNavigationPropertyPath csdlNavPropPath = (CsdlNavigationPropertyPath) obj;
    return (getValue() == null ? csdlNavPropPath.getValue() == null :
            getValue().equals(csdlNavPropPath.getValue()));
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

}