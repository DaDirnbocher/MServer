/*
 * StringVariableTest.java
 *
 * Projekt    : MServer
 * erstellt am: 07.12.2017
 * Autor      : Sascha
 *
 */
package de.mediathekview.mserver.crawler.br.graphql.variables;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringVariableTest {

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testNormalString() {
      final StringVariable graphQLVariable =
              new StringVariable("broadcasterId", "av:http://ard.de/ontologies/ard#BR_Fernsehen");
      assertEquals(
              "\"broadcasterId\":\"av:http://ard.de/ontologies/ard#BR_Fernsehen\"",
              graphQLVariable.getJSONFromVariableOrDefaulNull());
  }

  @Test
  public void testStringWithQuotes() {
      final StringVariable graphQLVariable =
              new StringVariable("term", "\"Fit - auch ohne Sport!\". Wie das geht");
      assertEquals(
              "\"term\":\"\\\"Fit - auch ohne Sport!\\\". Wie das geht\"",
              graphQLVariable.getJSONFromVariableOrDefaulNull());
  }

  @Test
  public void testNullString() {
      final StringVariable graphQLVariable = new StringVariable("term", null);
    assertEquals("\"term\":null", graphQLVariable.getJSONFromVariableOrDefaulNull());
  }
}