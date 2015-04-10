package org.apache.lucene.spatial.query;

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

import com.spatial4j.core.SpatialPredicate;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Rectangle;

import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.text.ParseException;

//Tests SpatialPredicate somewhat too
public class SpatialArgsParserTest extends LuceneTestCase {

  private SpatialContext ctx = SpatialContext.GEO;

  //The args parser is only dependent on the ctx for IO so I don't care to test
  // with other implementations.

  @Test
  public void testArgsParser() throws Exception {
    SpatialArgsParser parser = new SpatialArgsParser();

    String arg = SpatialPredicate.IsWithin + "(Envelope(-10, 10, 20, -20))";
    SpatialArgs out = parser.parse(arg, ctx);
    assertEquals(SpatialPredicate.IsWithin, out.getOperation());
    Rectangle bounds = (Rectangle) out.getShape();
    assertEquals(-10.0, bounds.getMinX(), 0D);
    assertEquals(10.0, bounds.getMaxX(), 0D);

    // Disjoint should not be scored
    arg = SpatialPredicate.IsDisjointTo + " (Envelope(-10,-20,20,10))";
    out = parser.parse(arg, ctx);
    assertEquals(SpatialPredicate.IsDisjointTo, out.getOperation());

    try {
      parser.parse(SpatialPredicate.IsDisjointTo + "[ ]", ctx);
      fail("spatial operations need args");
    }
    catch (Exception ex) {//expected
    }

    try {
      parser.parse("XXXX(Envelope(-10, 10, 20, -20))", ctx);
      fail("unknown operation!");
    }
    catch (Exception ex) {//expected
    }

    assertAlias(SpatialPredicate.IsWithin, "CoveredBy");
    assertAlias(SpatialPredicate.IsWithin, "COVEREDBY");
    assertAlias(SpatialPredicate.IsWithin, "coveredBy");
    assertAlias(SpatialPredicate.IsWithin, "Within");
    assertAlias(SpatialPredicate.IsEqualTo, "Equals");
    assertAlias(SpatialPredicate.IsDisjointTo, "disjoint");
    assertAlias(SpatialPredicate.Contains, "Covers");
  }

  private void assertAlias(SpatialPredicate op, final String name) throws ParseException {
    String arg;
    SpatialArgs out;
    arg = name + "(Point(0 0))";
    out = new SpatialArgsParser().parse(arg, ctx);
    assertEquals(op, out.getOperation());
  }

}
