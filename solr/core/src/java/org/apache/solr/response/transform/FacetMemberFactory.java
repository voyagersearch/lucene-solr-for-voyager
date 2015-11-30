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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;

/**
 *
 */
public class FacetMemberFactory extends TransformerFactory
{
  public static final String KEYKEY = "FacetMemberKEY";
  
  @Override
  public DocTransformer create(String field, SolrParams params, SolrQueryRequest req) {
    
    Map<String, DocSet> members = new ConcurrentHashMap<String,DocSet>();
    req.getContext().put(KEYKEY, members);
    
    return new FacetMemberAugmenter( field, members );
  }
  
  public static Map<String, DocSet> getFacetMemberCache(SolrQueryRequest req) {
    return (Map<String, DocSet>)req.getContext().get(KEYKEY);
  }
}

class FacetMemberAugmenter extends DocTransformer
{
  final String name;
  final Map<String, DocSet> members;
  int offset = 0;

  public FacetMemberAugmenter( String name, Map<String, DocSet> members )
  {
    this.name = name;
    this.members = members;
  }

  @Override
  public String getName()
  {
    return name;
  }

//  @Override
//  public void setContext( ResultContext context ) {
//
//    context.getRequest().getSearcher().getIndexReader();
//    
//    super.setContext(context);
//    try {
//      IndexReader reader = qparser.getReq().getSearcher().getIndexReader();
//      readerContexts = reader.leaves();
//      docValuesArr = new FunctionValues[readerContexts.size()];
//
//      
//      searcher = qparser.getReq().getSearcher();
//      fcontext = ValueSource.newContext(searcher);
//      this.valueSource.createWeight(fcontext, searcher);
//    } catch (IOException e) {
//      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
//    }
//  }

  @Override
  public void transform(SolrDocument doc, int docid, float score) {
    ArrayList<String> memberof = new ArrayList<String>(members.size());
    for(Map.Entry<String,DocSet> entries : members.entrySet()) {
      if(entries.getValue().exists(offset+docid)) {
        memberof.add(entries.getKey());
      }
    }
    doc.setField(name, memberof);    
  }
}

