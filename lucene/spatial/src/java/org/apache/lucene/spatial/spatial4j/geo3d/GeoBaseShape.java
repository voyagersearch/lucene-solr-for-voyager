package org.apache.lucene.spatial.spatial4j.geo3d;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * All bounding box shapes can derive from this base class, which furnishes
 * some common code
 *
 * @lucene.internal
 */
public abstract class GeoBaseShape {

  protected final PlanetModel planetModel;
  
  public GeoBaseShape(final PlanetModel planetModel) {
    this.planetModel = planetModel;
  }
  
  @Override
  public int hashCode() {
    return planetModel.hashCode();
  }
  
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof GeoBaseShape))
      return false;
    return planetModel.equals(((GeoBaseShape)o).planetModel);
  }
}


