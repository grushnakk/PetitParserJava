package org.petitparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.petitparser.Parsing.parse;

import org.petitparser.context.ParseError;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

public class Assertions {

  public static <T> void assertSuccess(Parser parser, String input, T result) {
    assertSuccess(parser, input, result, input.length());
  }

  public static <T> void assertSuccess(Parser parser, String input,
      T expected, int position) {
    Result result = parse(parser, input);
    assertNotNull(result.toString());
    assertTrue("Expected parse success", result.isSuccess());
    assertEquals("Position", position, result.getPosition());
    assertEquals("Result", expected, result.get());
    assertNull("No message expected", result.getMessage());
  }

  public static <T> void assertFailure(Parser parser, String input) {
    assertFailure(parser, input, 0);
  }

  public static <T> void assertFailure(Parser parser, String input,
      int position) {
    assertFailure(parser, input, position, null);
  }

  public static <T> void assertFailure(Parser parser, String input,
      String message) {
    assertFailure(parser, input, 0, message);
  }

  public static <T> void assertFailure(Parser parser, String input,
      int position, String message) {
    Result result = parse(parser, input);
    assertNotNull(result.toString());
    assertTrue("Expected parse failure", result.isFailure());
    assertEquals("Position", position, result.getPosition());
    if (message == null) {
      assertNotNull("Message expected", result.getMessage());
    } else {
      assertEquals("Message", message, result.getMessage());
    }
    try {
      result.get();
    } catch (ParseError e) {
      assertEquals(result, e.getContext());
      assertEquals(result.getMessage(), e.getMessage());
      return;
    }
    fail("Result#get() did not throw a ParseError");
  }

}
