package org.petitparser.parser;

import java.util.List;

import org.petitparser.Chars;
import org.petitparser.buffer.Token;
import org.petitparser.context.Context;
import org.petitparser.context.Result;
import org.petitparser.utils.Functions;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * An abstract parser that forms the root of all parsers in this package.
 *
 * @author Lukas Renggli (renggli@gmail.com)
 */
public abstract class Parser implements Cloneable {

  /**
   * Apply the parser on the given {@code context}.
   */
  public abstract Result parse(Context context);

  /**
   * Returns a new parser that is simply wrapped.
   *
   * @see DelegateParser
   */
  public Parser wrapped() {
    return new DelegateParser(this);
  }

  /**
   * Returns a parser that points to the receiver, but can be changed to point
   * to something else at a later point in time.
   *
   * @see SetableParser
   */
  public SetableParser setable() {
    return new SetableParser(this);
  }

  /**
   * Returns a new parser that flattens to a {@link String}.
   *
   * @see FlattenParser
   */
  public Parser flatten() {
    return new FlattenParser(this);
  }

  /**
   * Returns a new parser that creates a {@link Token}.
   *
   * @see TokenParser
   */
  public Parser token() {
    return new TokenParser(this);
  }

  /**
   * Returns a new parser that consumes whitespace before and after the
   * receiving parser.
   *
   * @see TrimmingParser
   */
  public Parser trim() {
    return trim(Chars.whitespace());
  }

  /**
   * Returns a new parser that consumes and ignores the {@code trimmer}
   * repeatedly before and after the receiving parser.
   *
   * @see TrimmingParser
   */
  public Parser trim(Parser trimmer) {
    return new TrimmingParser(this, trimmer);
  }

  /**
   * Returns a new parser (logical and-predicate) that succeeds whenever the
   * receiver does, but never consumes input.
   *
   * @see AndParser
   */
  public Parser and() {
    return new AndParser(this);
  }

  /**
   * Returns a new parser (logical not-predicate) that succeeds whenever the
   * receiver fails, but never consumes input.
   *
   * @see NotParser
   */
  public Parser not() {
    return not(null);
  }

  /**
  * Returns a new parser (logical not-predicate) that succeeds whenever the
  * receiver fails, but never consumes input.
  *
  * @see NotParser
  */
 public Parser not(String message) {
   return new NotParser(this, message);
 }

  /**
   * Returns a new parser that consumes any input character but the receiver.
   */
  public Parser negate() {
    return negate(null);
  }

  /**
   * Returns a new parser that consumes any input character but the receiver.
   */
  public Parser negate(String message) {
    Parser sequence = this.not(message).seq(Chars.any());
    return sequence.map(Functions.lastOfList());
  }

  /**
   * Returns a new parser that parses the receiver, if possible.
   *
   * @see OptionalParser
   */
  public Parser optional() {
    return new OptionalParser(this);
  }

  /**
   * Returns a new parser that parses the receiver zero or more times.
   *
   * @see RepeatingParser
   */
  public Parser star() {
    return repeat(0, Integer.MAX_VALUE);
  }

  /**
   * Returns a new parser that parses the receiver one or more times.
   *
   * @see RepeatingParser
   */
  public Parser plus() {
    return repeat(1, Integer.MAX_VALUE);
  }

  /**
   * Returns a new parser that parses the receiver exactly {@code count} times.
   *
   * @see RepeatingParser
   */
  public Parser times(int count) {
    return repeat(count, count);
  }

  /**
   * Returns a new parser that parses the receiver between {@code min} and
   * {@code max} times.
   *
   * @see RepeatingParser
   */
  public Parser repeat(int min, int max) {
    return new RepeatingParser(this, min, max);
  }

  /**
   * Returns a new parser that parses the receiver one or more times, separated
   * by a {@code separator}.
   */
  public Parser separatedBy(Parser separator) {
    return new SequenceParser(this, new SequenceParser(separator, this).star())
        .map(new Function<List<List<List<Object>>>, List<Object>>() {
          @Override
          public List<Object> apply(List<List<List<Object>>> input) {
            List<Object> result = Lists.newArrayList();
            result.add(input.get(0));
            for (List<Object> list : input.get(1)) {
              result.addAll(list);
            }
            return result;
          }
        });
  }

  /**
   * Returns a new parser that parses the receiver one or more times, separated
   * and possibly ended by a {@code separator}."
   */
  public Parser delimitedBy(Parser separator) {
    return separatedBy(separator)
        .seq(separator.optional())
        .map(new Function<List<List<Object>>, List<Object>>() {
          @Override
          public List<Object> apply(List<List<Object>> input) {
            List<Object> result = Lists.newArrayList(input.get(0));
            if (input.get(1) != null) {
              result.add(input.get(1));
            }
            return result;
          }
        });
  }

  /**
   * Returns a new parser that parses the receiver, if that fails try with the
   * following parsers.
   *
   * @see ChoiceParser
   */
  public Parser or(Parser... parsers) {
    Parser[] array = new Parser[1 + parsers.length];
    array[0] = this;
    System.arraycopy(parsers, 0, array, 1, parsers.length);
    return new ChoiceParser(array);
  }

  /**
   * Returns a new parser that first parses the receiver and then the argument.
   *
   * @see SequenceParser
   */
  public Parser seq(Parser... parsers) {
    Parser[] array = new Parser[1 + parsers.length];
    array[0] = this;
    System.arraycopy(parsers, 0, array, 1, parsers.length);
    return new SequenceParser(array);
  }

  /**
   * Returns a new parser that performs the given function on success.
   *
   * @see ActionParser
   */
  public <T, R> Parser map(Function<T, R> function) {
    return new ActionParser<T, R>(this, function);
  }

  /**
   * Returns a parser that transform a successful parse result by returning
   * the element at {@code index} of a list. A negative index can be used to
   * access the elements from the back of the list.
   *
   * @see Functions#nthOfList(int)
   */
  public Parser pick(int index) {
    return this.map(Functions.nthOfList(index));
  }

  /**
   * Returns a parser that transforms a successful parse result by returning
   * the permuted elements at {@code indexes} of a list. Negative indexes can
   * be used to access the elements from the back of the list.
   *
   * @see Functions#permutationOfList(int...)
   */
  public Parser permute(int... indexes) {
    return this.map(Functions.permutationOfList(indexes));
  }

  /**
   * Returns a new parser that succeeds at the end of the input and return the
   * result of the receiver.
   *
   * @see EndOfInputParser
   */
  public Parser end() {
    return end("end of input expected");
  }

  /**
   * Returns a new parser that succeeds at the end of the input and return the
   * result of the receiver.
   *
   * @see EndOfInputParser
   */
  public Parser end(String message) {
    return new EndOfInputParser(this, message);
  }

  /**
   * Clones this parser.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * Replaces the referring parser {@code source} with {@code target}. Does
   * nothing if the parser does not exist.
   */
  public void replace(Parser source, Parser target) {
    // no referring parsers
  }

  /**
   * Returns a list of directly referring parsers.
   */
  public List<Parser> getChildren() {
    return Lists.newArrayList();
  }

  public Parser between(Parser front, Parser end) {
	  return seq(front, this, end);
  }
}
