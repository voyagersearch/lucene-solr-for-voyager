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
package org.apache.solr.response.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.JavaBinCodec.ObjectResolver;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.response.WriteableValue;

import com.google.common.base.Strings;

/**
 * @since solr 5.2
 */
public class RawValueTransformerFactory extends TransformerFactory
{
  protected Class<? extends QueryResponseWriter> rawWriterClass = JSONResponseWriter.class;

  @Override
  public void init(NamedList args) {
    defaultUserArgs = (String)args.get( "args" );
  }
  
  @Override
  public DocTransformer create(String display, SolrParams params, SolrQueryRequest req) {
    String field = params.get("f");
    if(Strings.isNullOrEmpty(field)) {
      field = display;
    }
    QueryResponseWriter writer = req.getCore().getQueryResponseWriter(req);
    if(rawWriterClass.isInstance(writer)) {
      return new RawTransformer( field, display );
    }
    if(field.equals(display)) {
      return null; // nothing
    }
    return new RenameFieldTransformer( field, display, false );
  }

  static class RawTransformer extends DocTransformer
  {
    final String field;
    final String display;

    public RawTransformer( String field, String display )
    {
      this.field = field;
      this.display = display;
    }

    @Override
    public String getName()
    {
      return display;
    }

    @Override
    public void transform(SolrDocument doc, int docid) {
      Object val = doc.remove(field);
      if(val==null) {
        return;
      }
      if(val instanceof Collection) {
        Collection current = (Collection)val;
        ArrayList<WriteableStringValue> vals = new ArrayList<RawValueTransformerFactory.WriteableStringValue>();
        for(Object v : current) {
          vals.add(new WriteableStringValue(v));
        }
        doc.setField(display, vals);
      }
      else {
        doc.setField(display, new WriteableStringValue(val));
      }
    }
  }
  
  public static class WriteableStringValue extends WriteableValue {
    public final Object val;
    
    public WriteableStringValue(Object val) {
      this.val = val;
    }
    
    @Override
    public void write(TextResponseWriter writer) throws IOException {
      String str = null;
      if(val instanceof IndexableField) { // delays holding it in memory
        str = ((IndexableField)val).stringValue();
      }
      else {
        str = val.toString();
      }
      writer.getWriter().write(str);
    }

    @Override
    public Object resolve(Object o, JavaBinCodec codec) throws IOException {
      ObjectResolver orig = codec.getResolver();
      if(orig != null) {
        codec.writeVal(orig.resolve(val, codec));
        return null;
      }
      return val.toString();
    }
  }
}



