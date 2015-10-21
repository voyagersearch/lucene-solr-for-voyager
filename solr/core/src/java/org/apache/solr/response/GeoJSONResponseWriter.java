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

package org.apache.solr.response;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.AbstractSpatialFieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.ReturnFields;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.io.ShapeWriter;
import com.spatial4j.core.io.SupportedFormats;
import com.spatial4j.core.shape.Shape;

/**
 * Extend the standard JSONResponseWriter to support GeoJSON.  This writes
 * a {@link SolrDocumentList} with a 'FeatureCollection'
 */
public class GeoJSONResponseWriter extends JSONResponseWriter {
  @Override
  public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    
    SpatialContext ctx = null;
    String geofield = req.getParams().get("geojson.field", "geometry");
    SchemaField sf = req.getSchema().getFieldOrNull(geofield);
    if(sf != null && sf.getType() instanceof AbstractSpatialFieldType) {
      AbstractSpatialFieldType sftype = (AbstractSpatialFieldType)sf.getType();
      ctx = sftype.getSpatialContext();
    }
    
    if(ctx==null) {
      try {
        Class.forName("com.vividsolutions.jts.geom.Geometry");
        ctx = JtsSpatialContext.GEO;
      }
      catch(Exception ex) {
        ctx = SpatialContext.GEO;
      }
    }
    JSONWriter w = new GeoJSONWriter(writer, req, rsp, 
        geofield,
        ctx.getFormats()); 
    
    try {
      w.writeResponse();
    } finally {
      w.close();
    }
  }
}

class GeoJSONWriter extends JSONWriter {
  
  final SupportedFormats formats;
  final ShapeWriter geowriter;
  final String geofield;
  
  public GeoJSONWriter(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp, 
      String geofield, SupportedFormats formats) {
    super(writer, req, rsp);
    this.geofield = geofield;
    this.formats = formats;
    this.geowriter = formats.getGeoJsonWriter();
  }

  
  @Override
  public void writeSolrDocument(String name, SolrDocument doc, ReturnFields returnFields, int idx) throws IOException {
    if( idx > 0 ) {
      writeArraySeparator();
    }

    indent();
    writeMapOpener(-1); 
    incLevel();

    writeKey("type", false);
    writeVal(null, "Feature");
    
    Object geo = doc.getFieldValue(geofield);
    if(geo != null) {
      Shape shape = null;
      String str = null;
      if(geo instanceof Shape) {
        shape = (Shape)geo;
      }
      else if(geo instanceof IndexableField) {
        str = ((IndexableField)geo).stringValue();
      }
      else {
        str = geo.toString();
      }
      if(str !=null) {
        // Assume it is well formed JSON
        if(str.startsWith("{") && str.endsWith("}")) {
          shape = null;
          writer.write(str); // directly write the JSON
        }
        else {
          // try reading all format types
          shape = formats.read(str); 
          str = null;
        }
      }
      
      writeMapSeparator();
      writeKey("geometry", false);
      indent();
      
      if(str!=null) {
        writer.write(str);
      }
      else if(shape!=null) {
        geowriter.write(writer, shape);
      }
      else {
        writeMapOpener(0);
        writeMapCloser();
      }
    }

    
    boolean first=true;
    for (String fname : doc.getFieldNames()) {
      if (fname.equals(geofield) || ((returnFields!= null && !returnFields.wantsField(fname)))) {
        continue;
      }

      if (first) {
        writeMapSeparator();
        writeKey("properties", false);
        indent();
        writeMapOpener(-1); 
        incLevel();
        
        first=false;
      }
      else {
        writeMapSeparator();
      }

      indent();
      writeKey(fname, true);
      Object val = doc.getFieldValue(fname);

      // SolrDocument will now have multiValued fields represented as a Collection,
      // even if only a single value is returned for this document.
      if (val instanceof List) {
        // shortcut this common case instead of going through writeVal again
        writeArray(name,((Iterable)val).iterator());
      } else {
        writeVal(fname, val);
      }
    }

    // GeoJSON does not really support nested FeatureCollections
    if(doc.hasChildDocuments()) {
      if(first == false) {
        writeMapSeparator();
        indent();
      }
      writeKey("_childDocuments_", true);
      writeArrayOpener(doc.getChildDocumentCount());
      List<SolrDocument> childDocs = doc.getChildDocuments();
      for(int i=0; i<childDocs.size(); i++) {
        writeSolrDocument(null, childDocs.get(i), null, i);
      }
      writeArrayCloser();
    }

    // check that we added any properties
    if(!first) {
      decLevel();
      writeMapCloser();
    }
    
    decLevel();
    writeMapCloser();
  }

  @Override
  public void writeStartDocumentList(String name, 
      long start, int size, long numFound, Float maxScore) throws IOException
  {
    writeMapOpener((maxScore==null) ? 3 : 4);
    incLevel();
    writeKey("type",false);
    writeStr(null, "FeatureCollection", false);
    writeMapSeparator();
    writeKey("numFound",false);
    writeLong(null,numFound);
    writeMapSeparator();
    writeKey("start",false);
    writeLong(null,start);

    if (maxScore!=null) {
      writeMapSeparator();
      writeKey("maxScore",false);
      writeFloat(null,maxScore);
    }
    writeMapSeparator();
    
    // if can we get bbox of all results, we should write it here
    
    // indent();
    writeKey("features",false);
    writeArrayOpener(size);

    incLevel();
  }

  @Override
  public void writeEndDocumentList() throws IOException
  {
    decLevel();
    writeArrayCloser();

    decLevel();
    indent();
    writeMapCloser();
  }
}
