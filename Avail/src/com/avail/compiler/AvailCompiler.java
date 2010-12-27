/**
 * compiler/AvailCompiler.java Copyright (c) 2010, Mark van Gulik. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.avail.compiler;

import static com.avail.descriptor.AvailObject.error;
import static com.avail.newcompiler.TokenDescriptor.TokenType.*;
import java.io.*;
import java.util.*;
import com.avail.AvailRuntime;
import com.avail.annotations.NotNull;
import com.avail.compiler.scanner.AvailScanner;
import com.avail.descriptor.*;
import com.avail.descriptor.TypeDescriptor.Types;
import com.avail.interpreter.*;
import com.avail.interpreter.levelTwo.L2Interpreter;
import com.avail.newcompiler.*;
import com.avail.newcompiler.TokenDescriptor.TokenType;
import com.avail.utility.*;

/**
 * I parse a source file to create a {@link ModuleDescriptor module}.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class AvailCompiler
{
	/**
	 * The {@linkplain L2Interpreter interpreter} to use when evaluating
	 * top-level expressions.
	 */
	final @NotNull
	L2Interpreter interpreter;

	private String source;
	List<AvailObject> tokens;
	int greatestGuess;
	final List<Generator<String>> greatExpectations =
		new ArrayList<Generator<String>>(10);
	AvailObject module;
	AvailCompilerFragmentCache fragmentCache;
	List<AvailObject> extendedModules;
	List<AvailObject> usedModules;
	List<AvailObject> exportedNames;
	private Continuation3<ModuleName, Long, Long> progressBlock;

	/**
	 * Construct a new {@link AvailCompiler} which will use the given
	 * {@link Interpreter} to evaluate expressions.
	 *
	 * @param interpreter
	 *            The interpreter to be used for evaluating expressions.
	 */
	public AvailCompiler (@NotNull final L2Interpreter interpreter)
	{
		this.interpreter = interpreter;
	}

	/**
	 * A stack of {@link Continuation0 continuations} that need to be explored
	 * at some point.
	 */
	final Deque<Continuation0> workStack =
		new ArrayDeque<Continuation0>();

	/**
	 * This is actually a two-argument continuation, but it has only a single
	 * type parameter because the first one is always the {@link ParserState}
	 * that indicates where the continuation should continue parsing.
	 *
	 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
	 * @param <AnswerType>
	 *            The type of the second parameter of the
	 *            {@link Con#value(ParserState, Object)} method.
	 */
	protected abstract class Con<AnswerType> implements
			Continuation2<ParserState, AnswerType>
	{
		/**
		 * A debugging description of this continuation.
		 */
		final String description;

		/**
		 * Construct a new {@link AvailCompiler.Con} with the provided
		 * description.
		 *
		 * @param description
		 *            The provided description.
		 */
		public Con (final String description)
		{
			this.description = description;
		}

		@Override
		public String toString ()
		{
			return "Con(" + description + ")";
		}

		@Override
		public abstract void value (ParserState state, AnswerType answer);
	}

	/**
	 * Execute the block, passing a continuation that it should run upon finding
	 * a local solution. If exactly one solution is found, unwrap the stack (but
	 * not the token stream position or scopeStack), and pass the result to the
	 * continuation. Otherwise report that an unambiguous statement was
	 * expected.
	 *
	 * @param start
	 *            Where to start parsing
	 * @param tryBlock
	 *            The block to attempt.
	 * @param continuation
	 *            What to do if exactly one result was produced.
	 */
	private void tryIfUnambiguousThen (
		final ParserState start,
		final Con<Con<AvailParseNode>> tryBlock,
		final Con<AvailParseNode> continuation)
	{
		final Mutable<Integer> count = new Mutable<Integer>(0);
		final Mutable<AvailParseNode> solution = new Mutable<AvailParseNode>();
		final Mutable<AvailParseNode> another = new Mutable<AvailParseNode>();
		final Mutable<ParserState> where = new Mutable<ParserState>();
		final Mutable<Boolean> markerFired = new Mutable<Boolean>(false);
		attempt(
			new Continuation0()
			{
				@Override
				public void value ()
				{
					markerFired.value = true;
				}
			},
			"Marker for try unambiguous",
			start.position);
		attempt(start, tryBlock, new Con<AvailParseNode>(
			"Unambiguous statement")
		{
			@Override
			public void value (
				final ParserState afterSolution,
				final AvailParseNode aSolution)
			{
				if (count.value == 0)
				{
					solution.value = aSolution;
					where.value = afterSolution;
				}
				else
				{
					if (aSolution == solution.value)
					{
						error("Same solution was presented twice!");
					}
					another.value = aSolution;
				}
				count.value++;
			}
		});
		while (!markerFired.value)
		{
			workStack.pop().value();
		}
		if (count.value > 1)
		{
			// Indicate the problem on the last token of the ambiguous
			// expression.
			ambiguousInterpretationsAnd(
				new ParserState(
					where.value.position - 1,
					where.value.scopeStack),
				solution.value,
				another.value);
			return;
		}
		if (count.value == 0)
		{
			return;
		}
		assert count.value == 1;
		// We found exactly one solution. Advance the token stream just past it,
		// and redo any side-effects to the scopeStack, then invoke the
		// continuation with the solution.

		attempt(where.value, continuation, solution.value);
	}

	/**
	 * {@link ParserState} instances are immutable and keep track of a current
	 * {@link #position} and {@link #scopeStack} during parsing.
	 *
	 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
	 */
	class ParserState
	{
		/**
		 * The position represented by this {@link ParserState}. In particular,
		 * it's the (zero-based) index of the current token.
		 */
		final int position;

		/**
		 * The {@link AvailCompilerScopeStack scope stack}. This is a
		 * non-destructive singly-linked list of bindings. They're searched
		 * sequentially to resolve variables, but that's not likely to ever be a
		 * bottleneck.
		 */
		final AvailCompilerScopeStack scopeStack;

		/**
		 * Construct a new immutable {@link ParserState}.
		 *
		 * @param position
		 *            The index of the current token.
		 * @param scopeStack
		 *            The {@link AvailCompilerScopeStack}.
		 */
		ParserState (
				final int position,
				final AvailCompilerScopeStack scopeStack)
		{
			assert scopeStack != null;

			this.position = position;
			this.scopeStack = scopeStack;
		}

		@Override
		public int hashCode ()
		{
			return position * 473897843 ^ scopeStack.hashCode();
		}

		@Override
		public boolean equals (final Object another)
		{
			if (!(another instanceof ParserState))
			{
				return false;
			}
			final ParserState anotherState = (ParserState) another;
			return position == anotherState.position
					&& scopeStack.equals(anotherState.scopeStack);
		}

		/**
		 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
		 */
		@Override
		public String toString ()
		{
			return String.format(
				"%s%n"
				+ "\tPOSITION=%d%n"
				+ "\tSCOPE_STACK = %s",
				getClass().getSimpleName(),
				position,
				scopeStack);
		}

		/**
		 * Determine if this state represents the end of the file. If so, one
		 * should not invoke {@link #peekToken()} or {@link #afterToken()}
		 * again.
		 *
		 * @return Whether this state represents the end of the file.
		 */
		boolean atEnd ()
		{
			return this.position == tokens.size() - 1;
		}

		/**
		 * Answer the token at the current position.
		 *
		 * @return The token.
		 */
		AvailObject peekToken ()
		{
			assert !atEnd();
			return tokens.get(position);
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param tokenType
		 *            The {@link TokenType type} of token to look for.
		 * @param string
		 *            The exact token content to look for.
		 * @return Whether the specified token was found.
		 */
		boolean peekToken (final TokenType tokenType, final String string)
		{
			final AvailObject token = peekToken();
			return token.tokenType() == tokenType
					&& token.string().asNativeString().equals(string);
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param tokenType
		 *            The {@link TokenType type} of token to look for.
		 * @param string
		 *            The exact token content to look for.
		 * @param expected
		 *            A generator of a string message to record if the specified
		 *            token is not present.
		 * @return Whether the specified token is present.
		 */
		boolean peekToken (
			final TokenType tokenType,
			final String string,
			final Generator<String> expected)
		{
			final AvailObject token = peekToken();
			final boolean found = token.tokenType() == tokenType
					&& token.string().asNativeString().equals(string);
			if (!found)
			{
				expected(expected);
			}
			return found;
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param tokenType
		 *            The {@link TokenType type} of token to look for.
		 * @param string
		 *            The exact token content to look for.
		 * @param expected
		 *            A message to record if the specified token is not present.
		 * @return Whether the specified token is present.
		 */
		boolean peekToken (
			final TokenType tokenType,
			final String string,
			final String expected)
		{
			return peekToken(tokenType, string, generate(expected));
		}

		/**
		 * Return a new {@link ParserState} like this one, but advanced by one
		 * token.
		 *
		 * @return A new parser state.
		 */
		ParserState afterToken ()
		{
			assert !atEnd();
			return new ParserState(position + 1, scopeStack);
		}

		/**
		 * Parse a string literal. Answer the {@link LiteralTokenDescriptor
		 * string literal token} if found, otherwise answer null.
		 *
		 * @return The actual {@link LiteralTokenDescriptor literal token} or
		 *         null.
		 */
		AvailObject peekStringLiteral ()
		{
			final AvailObject token = peekToken();
			if (token.traversed().descriptor() instanceof LiteralTokenDescriptor)
			{
				return token;
			}
			return null;
		}

		/**
		 * Return a new {@link ParserState} like this one, but with the given
		 * declaration added.
		 *
		 * @param declaration
		 *            The {@link AvailVariableDeclarationNode declaration} to
		 *            add to the resulting {@link AvailCompilerScopeStack scope
		 *            stack}.
		 * @return The new parser state including the declaration.
		 */
		ParserState withDeclaration (
			final AvailVariableDeclarationNode declaration)
		{
			return new ParserState(position, new AvailCompilerScopeStack(
				declaration,
				scopeStack));
		}

		/**
		 * Record an expectation at the current parse position. The expectations
		 * captured at the rightmost parse position constitute the error message
		 * in case the parse fails.
		 * <p>
		 * The expectation is a {@link Generator Generator<String>}, in case
		 * constructing a {@link String} would be prohibitive. There is also
		 * {@link #expected(String) another} version of this method that accepts
		 * a String directly.
		 *
		 * @param stringGenerator
		 *            The {@code Generator<String>} to capture.
		 */
		void expected (final Generator<String> stringGenerator)
		{
			// System.out.println(Integer.toString(position) + " expected " +
			// stringBlock.value());
			if (position == greatestGuess)
			{
				greatExpectations.add(stringGenerator);
			}
			if (position > greatestGuess)
			{
				greatestGuess = position;
				greatExpectations.clear();
				greatExpectations.add(stringGenerator);
			}
		}

		/**
		 * Record an indication of what was expected at this parse position.
		 *
		 * @param aString
		 *            The string to look up.
		 */
		void expected (final String aString)
		{
			expected(generate(aString));
		}

	}

	/**
	 * Parse one or more string literals separated by commas. This parse isn't
	 * backtracking like the rest of the grammar - it's greedy. It considers a
	 * comma followed by something other than a string literal to be an
	 * unrecoverable parsing error (not a backtrack).
	 *
	 * <p>
	 * Return the {@link ParserState} after the strings if successful, otherwise
	 * null. Populate the passed {@link List} with the
	 * {@link ByteStringDescriptor actual Avail strings}.
	 * </p>
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param stringTokens
	 *            The list of strings to populate.
	 * @return The parser state after the list of strings, or null if the list
	 *         of strings is malformed.
	 */
	ParserState parseStringLiterals (
		final ParserState start,
		final List<AvailObject> stringTokens)
	{
		assert stringTokens.isEmpty();

		AvailObject token = start.peekStringLiteral();
		if (token == null)
		{
			return start;
		}
		stringTokens.add(token.literal());
		ParserState state = start.afterToken();
		while (state.peekToken(OPERATOR, ","))
		{
			state = state.afterToken();
			token = state.peekStringLiteral();
			if (token == null)
			{
				state.expected("another string literal after comma");
				return null;
			}
			state = state.afterToken();
			stringTokens.add(token.literal());
		}
		return state;
	}

	/**
	 * Parse a statement. This is the boundary for the backtracking grammar. A
	 * statement must be unambiguous (in isolation) to be valid. The passed
	 * continuation will be invoked at most once, and only if the statement had
	 * a single interpretation.
	 *
	 * <p>
	 * The {@link #workStack} should have the same content before and after this
	 * method is invoked.
	 * </p>
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param outermost
	 *            Whether this statement is outermost in the module.
	 * @param canBeLabel
	 *            Whether this statement can be a label declaration.
	 * @param continuation
	 *            What to do with the unambiguous, parsed statement.
	 */
	private void parseStatementAsOutermostCanBeLabelThen (
		final ParserState start,
		final boolean outermost,
		final boolean canBeLabel,
		final Con<AvailParseNode> continuation)
	{
		assert !(outermost & canBeLabel);
		tryIfUnambiguousThen(
			start,
			new Con<Con<AvailParseNode>>("Detect ambiguity")
			{
				@Override
				public void value (
					final ParserState ignored,
					final Con<AvailParseNode> whenFoundStatement)
				{
					parseDeclarationThen(
						start,
						new Con<AvailVariableDeclarationNode>(
							"Semicolon after declaration")
						{
							@Override
							public void value (
								final ParserState afterDeclaration,
								final AvailVariableDeclarationNode declaration)
							{
								if (afterDeclaration.peekToken(
									END_OF_STATEMENT,
									";",
									"; to end declaration statement"))
								{
									ParserState afterSemicolon =
										afterDeclaration.afterToken();
									if (outermost)
									{
										afterSemicolon = new ParserState(
											afterSemicolon.position,
											new AvailCompilerScopeStack(
												null, null));
									}
									whenFoundStatement.value(
										afterSemicolon,
										declaration);
								}
							}
						});
					parseAssignmentThen(start, new Con<AvailAssignmentNode>(
						"Semicolon after assignment")
					{
						@Override
						public void value (
							final ParserState afterAssignment,
							final AvailAssignmentNode assignment)
						{
							if (afterAssignment.peekToken(
								END_OF_STATEMENT,
								";",
								"; to end assignment statement"))
							{
								whenFoundStatement.value(
									afterAssignment.afterToken(),
									assignment);
							}
						}
					});
					parseExpressionThen(start, new Con<AvailParseNode>(
						"Semicolon after expression")
					{
						@Override
						public void value (
							final ParserState afterExpression,
							final AvailParseNode expression)
						{
							if (!afterExpression.peekToken(
								END_OF_STATEMENT,
								";",
								"; to end statement"))
							{
								return;
							}
							if (!outermost
									|| expression.type().equals(
										Types.voidType.object()))
							{
								whenFoundStatement.value(
									afterExpression.afterToken(),
									expression);
							}
							else
							{
								afterExpression
										.expected("outer level statement to have void type");
							}
						}
					});
					if (canBeLabel)
					{
						parseLabelThen(start, new Con<AvailLabelNode>(
							"Semicolon after label")
						{
							@Override
							public void value (
								final ParserState afterDeclaration,
								final AvailLabelNode label)
							{
								if (afterDeclaration.peekToken(
									END_OF_STATEMENT,
									";",
									"; to end label statement"))
								{
									whenFoundStatement.value(
										afterDeclaration.afterToken(),
										label);
								}
							}
						});
					}
				}
			},
			continuation);
	}

	/**
	 * Parse a label declaration, then invoke the continuation.
	 *
	 * @param start
	 *            Where to start parsing
	 * @param continuation
	 *            What to do after parsing a label.
	 */
	void parseLabelThen (
		final ParserState start,
		final Con<AvailLabelNode> continuation)
	{
		if (!start.peekToken(
			OPERATOR,
			"$",
			"label statement starting with \"$\""))
		{
			return;
		}
		final ParserState atName = start.afterToken();
		final AvailObject token = atName.peekToken();
		if (token.tokenType() != KEYWORD)
		{
			atName.expected("name of label after $");
			return;
		}
		final ParserState atColon = atName.afterToken();
		if (!atColon.peekToken(
			OPERATOR,
			":",
			"colon for label's type declaration"))
		{
			return;
		}
		final ParserState afterColon = atColon.afterToken();
		attempt(
			new Continuation0()
			{
				@Override
				public void value ()
				{
					parseAndEvaluateExpressionYieldingInstanceOfThen(
						afterColon,
						Types.continuationType.object(),
						new Con<AvailObject>("Check label type expression")
						{
							@Override
							public void value (
								final ParserState afterExpression,
								final AvailObject contType)
							{
								final AvailLabelNode label = new AvailLabelNode();
								label.name(token);
								label.declaredType(contType);
								final ParserState afterDeclaration = afterExpression
										.withDeclaration(label);
								attempt(afterDeclaration, continuation, label);
							}
						});
				}
			},
			"Label type",
			afterColon.position);
	}

	/**
	 * Parse an expression whose type is (at least) someType. Evaluate the
	 * expression, yielding a type, and pass that to the continuation. Note that
	 * the longest syntactically correct and type correct expression is what
	 * gets used. It's an ambiguity error if two or more possible parses of this
	 * maximum length are possible.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param someType
	 *            The type that the expression must return.
	 * @param continuation
	 *            What to do with the result of expression evaluation.
	 */
	void parseAndEvaluateExpressionYieldingInstanceOfThen (
		final ParserState start,
		final AvailObject someType,
		final Con<AvailObject> continuation)
	{
		final ParserState startWithoutScope = new ParserState(
			start.position,
			new AvailCompilerScopeStack(null, null));
		parseExpressionThen(startWithoutScope, new Con<AvailParseNode>(
			"Evaluate expression")
		{
			@Override
			public void value (
				final ParserState afterExpression,
				final AvailParseNode expression)
			{
				if (expression.type().isSubtypeOf(someType))
				{
					// A unique, longest type-correct expression was found.
					final AvailObject value = evaluate(expression);
					if (value.isInstanceOfSubtypeOf(someType))
					{
						assert afterExpression.scopeStack == startWithoutScope.scopeStack
						: "Subexpression should not have been able to cause declaration";
						// Make sure we continue with the position after the expression,
						// but the scope stack we started with. That's because the
						// expression was parsed for execution, and as such was excluded
						// from seeing things that would be in scope for regular
						// subexpressions at this point.
						attempt(
							new ParserState(
								afterExpression.position,
								start.scopeStack),
							continuation,
							value);
					}
					else
					{
						afterExpression.expected(
							"expression to respect its own type declaration");
					}
				}
				else
				{
					afterExpression.expected(new Generator<String>()
					{
						@Override
						public String value ()
						{
							return "expression to have type "
									+ someType.toString();
						}
					});
				}
			}
		});
	}

	/**
	 * Parse a local variable declaration. These have one of three forms:
	 * <ul>
	 * <li>a simple declaration (var : type),</li>
	 * <li>an initializing declaration (var : type := value), or</li>
	 * <li>a constant declaration (var ::= expr).</li>
	 * </ul>
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the local variable declaration.
	 */
	void parseDeclarationThen (
		final ParserState start,
		final Con<AvailVariableDeclarationNode> continuation)
	{
		final AvailObject localName = start.peekToken();
		if (localName.tokenType() != KEYWORD)
		{
			start.expected("a variable or constant declaration");
			return;
		}
		final ParserState afterVar = start.afterToken();
		if (!afterVar.peekToken(
			OPERATOR,
			":",
			": or ::= for simple/constant/initializing declaration"))
		{
			return;
		}
		final ParserState afterFirstColon = afterVar.afterToken();
		if (afterFirstColon.peekToken(
			OPERATOR,
			":",
			"second colon for constant declaration (a ::= expr)"))
		{
			final ParserState afterSecondColon = afterFirstColon.afterToken();
			if (afterSecondColon.peekToken(
				OPERATOR,
				"=",
				"= part of ::= in constant declaration"))
			{
				final ParserState afterEquals = afterSecondColon.afterToken();
				parseExpressionThen(afterEquals,
					new Con<AvailParseNode>("Complete var ::= expr")
					{
						@Override
						public void value (
							final ParserState afterInitExpression,
							final AvailParseNode initExpression)
						{
							final AvailConstantDeclarationNode constantDeclaration = new AvailConstantDeclarationNode();
							constantDeclaration.name(localName);
							constantDeclaration.declaredType(initExpression.type());
							constantDeclaration
									.initializingExpression(initExpression);
							constantDeclaration.isArgument(false);
							attempt(
								afterInitExpression
										.withDeclaration(constantDeclaration),
								continuation,
								constantDeclaration);
						}
					});
			}
		}
		parseAndEvaluateExpressionYieldingInstanceOfThen(
			afterFirstColon,
			Types.type.object(),
			new Con<AvailObject>("Type expression of var : type")
			{
				@Override
				public void value (
					final ParserState afterType,
					final AvailObject type)
				{
					if (type.equals(Types.voidType.object())
							|| type.equals(Types.terminates.object()))
					{
						afterType.expected(
							"a type for the variable other than void or terminates");
						return;
					}
					// Try the simple declaration... var : type;
					final AvailVariableDeclarationNode simpleDeclaration =
						new AvailVariableDeclarationNode();
					simpleDeclaration.name(localName);
					simpleDeclaration.declaredType(type);
					simpleDeclaration.isArgument(false);
					attempt(
						afterType.withDeclaration(simpleDeclaration),
						continuation,
						simpleDeclaration);

					// Also try for var : type := init.
					if (!afterType.peekToken(
						OPERATOR,
						":",
						"Second colon of var : type := init"))
					{
						return;
					}
					final ParserState afterSecondColon = afterType.afterToken();
					if (!afterSecondColon.peekToken(
						OPERATOR,
						"=",
						"Equals sign in var : type := init"))
					{
						return;
					}
					final ParserState afterEquals =
						afterSecondColon.afterToken();

					parseExpressionThen(afterEquals,
						new Con<AvailParseNode>("After expr of var : type := expr")
						{
							@Override
							public void value (
								final ParserState afterInit,
								final AvailParseNode initExpr)
							{
								if (initExpr.type().isSubtypeOf(type))
								{
									final AvailInitializingDeclarationNode initializingDeclaration = new AvailInitializingDeclarationNode();
									initializingDeclaration.name(localName);
									initializingDeclaration.declaredType(type);
									initializingDeclaration
											.initializingExpression(initExpr);
									initializingDeclaration.isArgument(false);
									attempt(
										afterInit
												.withDeclaration(initializingDeclaration),
										continuation,
										initializingDeclaration);
								}
								else
								{
									afterInit.expected(
										"initializing expression's type to agree with declared type");
								}
							}
						});
				}
			});
	}

	/**
	 * Attempt the zero-argument continuation. The implementation is free to
	 * execute it now or to put it in a stack of continuations to run later, but
	 * they have to be run in the reverse order that they were pushed.
	 *
	 * @param continuation
	 *            What to do at some point in the future.
	 * @param description
	 *            Debugging information about what is to be parsed.
	 * @param position
	 *            Debugging information about where the parse is happening.
	 */
	void attempt (
		final Continuation0 continuation,
		final String description,
		final int position)
	{
//		for (int i = workStack.size(); i > 0; i--)
//		{
//			System.err.append("  ");
//		}
//		System.err.printf(
//			"Pushing (%d): +%s%n",
//			position,
//			description);
		workStack.push(
			new Continuation0()
			{
				@Override
				public void value ()
				{
//					for (int i = workStack.size(); i > 0; i--)
//					{
//						System.err.append("  ");
//					}
//					System.err.printf(
//						"Running (%d): -%s%n",
//						position,
//						description);
					continuation.value();
				}
			});
		if (workStack.size() > 1000)
		{
			throw new RuntimeException("Probable recursive parse error");
		}
		//TODO: This temporarily forces depth-first parsing.
		//workStack.pop().value();
	}

	/**
	 * Wrap the {@link Continuation1 continuation of one argument} inside a
	 * {@link Continuation0 continuation of zero arguments} and record that as
	 * per {@link #attempt(Continuation0, String, int)}.
	 *
	 * @param <ArgType>
	 *            The type of argument to the given continuation.
	 * @param here
	 *            Where to start parsing when the continuation runs.
	 * @param continuation
	 *            What to execute with the passed argument.
	 * @param argument
	 *            What to pass as an argument to the provided
	 *            {@code Continuation1 one-argument continuation}.
	 */
	<ArgType> void attempt (
		final ParserState here,
		final Con<ArgType> continuation,
		final ArgType argument)
	{
		attempt(
			new Continuation0()
			{
				@Override
				public void value ()
				{
//					for (int i = workStack.size(); i > 0; i--)
//					{
//						System.err.append("  ");
//					}
//					System.err.printf(
//						"Invoking %s with %s%n",
//						continuation.description,
//						argument.toString());
					continuation.value(here, argument);
				}
			},
			continuation.description,
			here.position);
	}

	/**
	 * Evaluate an {@link AvailParseNode} in the module's context; lexically
	 * enclosing variables are not considered in scope, but module variables and
	 * constants are in scope.
	 *
	 * @param expressionNode
	 *            An {@link AvailParseNode}.
	 * @return The result of generating a {@link ClosureDescriptor closure} from
	 *         the argument and evaluating it.
	 */
	AvailObject evaluate (final AvailParseNode expressionNode)
	{
		// Evaluate a parse tree node.

		AvailBlockNode block = new AvailBlockNode();
		block.arguments(new ArrayList<AvailVariableDeclarationNode>());
		block.primitive(0);
		List<AvailParseNode> statements;
		statements = new ArrayList<AvailParseNode>(1);
		statements.add(expressionNode);
		block.statements(statements);
		block.resultType(Types.voidType.object());
		block = (AvailBlockNode) block.validatedWithInterpreter(interpreter);
		final AvailCodeGenerator codeGenerator = new AvailCodeGenerator();
		final AvailObject compiledBlock = block.generateOn(codeGenerator);
		// The block is guaranteed context-free (because imported
		// variables/values are embedded directly as constants in the generated
		// code), so build a closure with no copied data.
		final AvailObject closure = ClosureDescriptor.create(
			compiledBlock,
			TupleDescriptor.empty());
		closure.makeImmutable();
		List<AvailObject> args;
		args = new ArrayList<AvailObject>();
		final AvailObject result = interpreter.runClosureArguments(
			closure,
			args);
		// System.out.println(Integer.toString(position) + " evaluated (" +
		// expressionNode.toString() + ") = " + result.toString());
		return result;
	}

	/**
	 * Evaluate a parse tree node. It's a top-level statement in a module.
	 * Declarations are handled differently - they cause a variable to be
	 * declared in the module's scope.
	 *
	 * @param expressionNode
	 *            The expression to compile and evaluate as a top-level
	 *            statement in the module.
	 */
	private void evaluateModuleStatement (final AvailParseNode expressionNode)
	{
		if (!expressionNode.isDeclaration())
		{
			evaluate(expressionNode);
			return;
		}
		// It's a declaration...
		final AvailVariableDeclarationNode declarationExpression = (AvailVariableDeclarationNode) expressionNode;
		final AvailObject name = declarationExpression.name().string();
		if (declarationExpression.isConstant())
		{
			final AvailInitializingDeclarationNode declaration = (AvailInitializingDeclarationNode) declarationExpression;
			final AvailObject val = evaluate(declaration.initializingExpression());
			module.constantBindings(module.constantBindings()
					.mapAtPuttingCanDestroy(name, val.makeImmutable(), true));
		}
		else
		{
			final AvailObject var = ContainerDescriptor
					.forInnerType(declarationExpression.declaredType());
			if (declarationExpression.isInitializing())
			{
				final AvailInitializingDeclarationNode declaration = (AvailInitializingDeclarationNode) declarationExpression;
				var.setValue(evaluate(declaration.initializingExpression()));
			}
			module.variableBindings(module.variableBindings()
					.mapAtPuttingCanDestroy(name, var.makeImmutable(), true));
		}
	}

	/**
	 * Tokenize the {@linkplain ModuleDescriptor module} specified by the
	 * fully-qualified {@linkplain ModuleName module name}.
	 *
	 * @param qualifiedName
	 *            A fully-qualified {@linkplain ModuleName module name}.
	 * @param stopAfterNamesToken
	 *            Stop scanning after encountering the <em>Names</em> token?
	 * @return The {@linkplain ResolvedModuleName resolved module name}.
	 * @throws AvailCompilerException
	 *             If tokenization failed for any reason.
	 */
	private @NotNull
	ResolvedModuleName tokenize (
		final @NotNull ModuleName qualifiedName,
		final boolean stopAfterNamesToken) throws AvailCompilerException
	{
		final ModuleNameResolver resolver = interpreter.runtime()
				.moduleNameResolver();
		final ResolvedModuleName resolvedName = resolver.resolve(qualifiedName);
		if (resolvedName == null)
		{
			throw new AvailCompilerException(
				qualifiedName,
				0,
				0,
				"Unable to resolve fully-qualified module name \""
						+ qualifiedName.qualifiedName()
						+ "\" to an existing file");
		}

		try
		{
			final StringBuilder sourceBuilder = new StringBuilder(4096);
			final char[] buffer = new char[4096];
			final Reader reader = new BufferedReader(new FileReader(
				resolvedName.fileReference()));
			int charsRead;
			while ((charsRead = reader.read(buffer)) > 0)
			{
				sourceBuilder.append(buffer, 0, charsRead);
			}
			source = sourceBuilder.toString();
		}
		catch (final IOException e)
		{
			throw new AvailCompilerException(
				qualifiedName,
				0,
				0,
				"Encountered an I/O exception while reading source module \""
						+ qualifiedName.qualifiedName() + "\" (resolved to \""
						+ resolvedName.fileReference().getAbsolutePath()
						+ "\")");
		}

		tokens = new AvailScanner().scanString(source, stopAfterNamesToken);
		return resolvedName;
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} and install it into the
	 * {@linkplain AvailRuntime runtime}.
	 *
	 * @param qualifiedName
	 *            The {@linkplain ModuleName qualified name} of the
	 *            {@linkplain ModuleDescriptor source module}.
	 * @param aBlock
	 *            A {@linkplain Continuation3 continuation} that accepts the
	 *            {@linkplain ModuleName name} of the
	 *            {@linkplain ModuleDescriptor module} undergoing
	 *            {@linkplain AvailCompiler compilation}, the position of the
	 *            ongoing parse (in bytes), and the size of the module (in
	 *            bytes).
	 * @throws AvailCompilerException
	 *             If compilation fails.
	 */
	public void parseModule (
		final @NotNull ModuleName qualifiedName,
		final @NotNull Continuation3<ModuleName, Long, Long> aBlock)
			throws AvailCompilerException
	{
		progressBlock = aBlock;
		greatestGuess = -1;
		greatExpectations.clear();
		final ResolvedModuleName resolvedName = tokenize(qualifiedName, false);

		startModuleTransaction();
		try
		{
			parseModule(resolvedName);
		}
		catch (final AvailCompilerException e)
		{
			rollbackModuleTransaction();
			throw e;
		}
		commitModuleTransaction();
	}

	/**
	 * Parse the {@linkplain ModuleDescriptor module} with the specified
	 * fully-qualified {@linkplain ModuleName module name} from the
	 * {@linkplain TokenDescriptor token} stream.
	 *
	 * @param qualifiedName
	 *            The {@linkplain ResolvedModuleName resolved name} of the
	 *            {@linkplain ModuleDescriptor source module}.
	 * @throws AvailCompilerException
	 *             If compilation fails.
	 */
	private void parseModule (final @NotNull ResolvedModuleName qualifiedName)
			throws AvailCompilerException
	{
		final AvailRuntime runtime = interpreter.runtime();
		final ModuleNameResolver resolver = runtime.moduleNameResolver();
		final long sourceLength = qualifiedName.fileReference().length();
		final Mutable<AvailParseNode> interpretation = new Mutable<AvailParseNode>();
		final Mutable<ParserState> state = new Mutable<ParserState>();
		interpreter.checkUnresolvedForwards();
		greatestGuess = 0;
		greatExpectations.clear();

		state.value = parseHeader(qualifiedName, false);
		if (state.value == null)
		{
			reportError(new ParserState(0, null), qualifiedName);
			assert false;
		}
		if (!state.value.atEnd())
		{
			progressBlock.value(qualifiedName, (long) state.value.peekToken()
					.start(), sourceLength);
		}
		for (final AvailObject modName : extendedModules)
		{
			assert modName.isString();
			final ModuleName ref = resolver.canonicalNameFor(qualifiedName
					.asSibling(modName.asNativeString()));
			final AvailObject availRef = ByteStringDescriptor.fromNativeString(ref
					.qualifiedName());
			if (!runtime.includesModuleNamed(availRef))
			{
				state.value.expected("module \"" + ref.qualifiedName()
						+ "\" to be loaded already");
				reportError(state.value, qualifiedName);
				assert false;
			}
			final AvailObject mod = runtime.moduleAt(availRef);
			final AvailObject modNames = mod.names().keysAsSet();
			for (final AvailObject strName : modNames)
			{
				final AvailObject trueNames = mod.names().mapAt(strName);
				for (final AvailObject trueName : trueNames)
				{
					module.atNameAdd(strName, trueName);
				}
			}
		}
		for (final AvailObject modName : usedModules)
		{
			assert modName.isString();
			final ModuleName ref = resolver.canonicalNameFor(qualifiedName
					.asSibling(modName.asNativeString()));
			final AvailObject availRef = ByteStringDescriptor.fromNativeString(ref
					.qualifiedName());
			if (!runtime.includesModuleNamed(availRef))
			{
				state.value.expected("module \"" + ref.qualifiedName()
						+ "\" to be loaded already");
				reportError(state.value, qualifiedName);
				assert false;
			}
			final AvailObject mod = runtime.moduleAt(availRef);
			final AvailObject modNames = mod.names().keysAsSet();
			for (final AvailObject strName : modNames)
			{
				final AvailObject trueNames = mod.names().mapAt(strName);
				for (final AvailObject trueName : trueNames)
				{
					module.atPrivateNameAdd(strName, trueName);
				}
			}
		}
		for (final AvailObject stringObject : exportedNames)
		{
			assert stringObject.isString();
			final AvailObject trueNameObject = CyclicTypeDescriptor
					.create(stringObject);
			module.atNameAdd(stringObject, trueNameObject);
			module.atNewNamePut(stringObject, trueNameObject);
		}
		module.buildFilteredBundleTreeFrom(interpreter.runtime()
				.rootBundleTree());
		fragmentCache = new AvailCompilerFragmentCache();
		while (!state.value.atEnd())
		{
			greatestGuess = 0;
			greatExpectations.clear();
			interpretation.value = null;
			parseStatementAsOutermostCanBeLabelThen(
				state.value,
				true,
				false,
				new Con<AvailParseNode>("Outermost statement")
				{
					@Override
					public void value (
						final ParserState afterStatement,
						final AvailParseNode stmt)
					{
						assert interpretation.value == null : "Statement parser was supposed to catch ambiguity";
						interpretation.value = stmt;
						state.value = afterStatement;
					}
				});
			while (!workStack.isEmpty())
			{
				workStack.pop().value();
			}

			if (interpretation.value == null)
			{
				reportError(state.value, qualifiedName);
				assert false;
			}
			// Clear the section of the fragment cache associated with the
			// (outermost) statement just parsed...
			privateClearFrags();
			// Now execute the statement so defining words have a chance to
			// run. This lets the defined words be used in subsequent code.
			// It's even callable in later statements and type expressions.
			evaluateModuleStatement(interpretation.value);
			if (!state.value.atEnd())
			{
				progressBlock.value(
					qualifiedName,
					(long) (tokens.get(state.value.position - 1).start() + 2),
					sourceLength);
			}
		}
		interpreter.checkUnresolvedForwards();
		assert state.value.atEnd();
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} header from the specified
	 * string. Populate {@link #extendedModules} and {@link #usedModules}.
	 *
	 * @param qualifiedName
	 *            The {@linkplain ModuleName qualified name} of the
	 *            {@linkplain ModuleDescriptor source module}.
	 * @throws AvailCompilerException
	 *             If compilation fails.
	 * @author Todd L Smith &lt;anarkul@gmail.com&gt;
	 */
	void parseModuleHeader (final @NotNull ModuleName qualifiedName)
			throws AvailCompilerException
	{
		progressBlock = null;
		greatestGuess = -1;
		greatExpectations.clear();
		final ResolvedModuleName resolvedName = tokenize(qualifiedName, true);
		if (parseHeader(resolvedName, true) == null)
		{
			reportError(new ParserState(0, null), resolvedName);
			assert false;
		}
	}

	/**
	 * Parse the header of the module from the token stream. If successful,
	 * return the {@link ParserState} just after the header, otherwise return
	 * null.
	 *
	 * <p>
	 * If the dependenciesOnly parameter is true, only parse the bare minimum
	 * needed to determine information about which modules are used by this one.
	 * </p>
	 *
	 * @param qualifiedName
	 *            The expected module name.
	 * @param dependenciesOnly
	 *            Whether to do the bare minimum parsing required to determine
	 *            the modules to which this one refers.
	 * @return The state of parsing just after the header, or null if it failed.
	 */
	private ParserState parseHeader (
		final @NotNull ResolvedModuleName qualifiedName,
		final boolean dependenciesOnly)
	{
		assert workStack.isEmpty();
		extendedModules = new ArrayList<AvailObject>();
		usedModules = new ArrayList<AvailObject>();
		exportedNames = new ArrayList<AvailObject>();
		ParserState state = new ParserState(0, new AvailCompilerScopeStack(
			null,
			null));

		if (!state.peekToken(KEYWORD, "Module", "initial Module keyword"))
		{
			return null;
		}
		state = state.afterToken();
		final AvailObject localNameToken = state.peekStringLiteral();
		if (localNameToken == null)
		{
			state.expected("module name");
			return null;
		}
		if (!dependenciesOnly)
		{
			final AvailObject localName = localNameToken.literal();
			if (!qualifiedName.localName().equals(localName.asNativeString()))
			{
				state.expected("declared local module name to agree with "
						+ "fully-qualified module name");
				return null;
			}
			module.name(ByteStringDescriptor.fromNativeString(qualifiedName
					.qualifiedName()));
		}
		state = state.afterToken();

		if (state.peekToken(KEYWORD, "Pragma"))
		{
			state = state.afterToken();
			final List<AvailObject> strings = new ArrayList<AvailObject>();
			state = parseStringLiterals(state, strings);
			if (state == null)
			{
				return null;
			}
			if (!dependenciesOnly)
			{
				for (int index = 0; index < strings.size(); index++)
				{
					final AvailObject pragmaString = strings.get(index);
					final String[] parts = pragmaString.asNativeString().split("=");
					assert parts.length == 2;
					final String pragmaKey = parts[0].trim();
					final String pragmaValue = parts[1].trim();
					if (!pragmaKey.matches("\\w+"))
					{
						final ParserState badStringState = new ParserState(
							state.position + (index - strings.size()) * 2 + 1,
							state.scopeStack);
						badStringState.expected("pragma key (" + pragmaKey
								+ ") must not contain internal whitespace");
						return null;
					}
					if (pragmaKey.equals("bootstrapDefiningMethod"))
					{
						interpreter.bootstrapDefiningMethod(pragmaValue);
					}
					else if (pragmaKey.equals("bootstrapSpecialObject"))
					{
						interpreter.bootstrapSpecialObject(pragmaValue);
					}
				}
			}
		}

		if (!state.peekToken(KEYWORD, "Extends", "Extends keyword"))
		{
			return null;
		}
		state = state.afterToken();
		state = parseStringLiterals(state, extendedModules);
		if (state == null)
		{
			return null;
		}

		if (!state.peekToken(KEYWORD, "Uses", "Uses keyword"))
		{
			return null;
		}
		state = state.afterToken();
		state = parseStringLiterals(state, usedModules);
		if (state == null)
		{
			return null;
		}

		if (!state.peekToken(KEYWORD, "Names", "Names keyword"))
		{
			return null;
		}
		state = state.afterToken();
		if (dependenciesOnly)
		{
			// We've parsed everything necessary for inter-module information.
			return state;
		}
		state = parseStringLiterals(state, exportedNames);
		if (state == null)
		{
			return null;
		}

		if (!state.peekToken(KEYWORD, "Body", "Body keyword"))
		{
			return null;
		}
		state = state.afterToken();

		assert workStack.isEmpty();
		return state;
	}

	/**
	 * Report an error by throwing an {@link AvailCompilerException}. The
	 * exception encapsulates the {@linkplain ModuleName module name} of the
	 * {@linkplain ModuleDescriptor module} undergoing compilation, the error
	 * string, and the text position. This position is the rightmost position
	 * encountered during the parse, and the error strings in
	 * {@link #greatExpectations} are the things that were expected but not
	 * found at that position. This seems to work very well in practice.
	 *
	 * @param state
	 *            Where the error occurred.
	 * @param qualifiedName
	 *            The {@linkplain ModuleName qualified name} of the
	 *            {@linkplain ModuleDescriptor source module}.
	 * @throws AvailCompilerException
	 *             Always thrown.
	 */
	private void reportError (
		final ParserState state,
		final @NotNull ModuleName qualifiedName) throws AvailCompilerException
	{
		final long charPos = tokens.get(greatestGuess).start();
		final String sourceUpToError = source.substring(0, (int) charPos);
		final int startOfPreviousLine = sourceUpToError.lastIndexOf('\n') + 1;
		final StringBuilder text = new StringBuilder(100);
		text.append('\n');
		int wedges = 3;
		for (int i = startOfPreviousLine; i < charPos; i++)
		{
			if (source.codePointAt(i) == '\t')
			{
				while (wedges > 0)
				{
					text.append('>');
					wedges--;
				}
				text.append('\t');
			}
			else
			{
				if (wedges > 0)
				{
					text.append('>');
					wedges--;
				}
				else
				{
					text.append(' ');
				}
			}
		}
		text.append("^-- Expected...");
		text.append("\n>>>---------------------------------------------------------------------");
		assert greatExpectations.size() > 0 : "Bug - empty expectation list";
		final Set<String> alreadySeen = new HashSet<String>(greatExpectations.size());
		for (final Generator<String> generator : greatExpectations)
		{
			final String str = generator.value();
			if (!alreadySeen.contains(str))
			{
				text.append("\n");
				alreadySeen.add(str);
				text.append(">>>\t");
				text.append(str.replace("\n", "\n>>>\t"));
			}
		}
		text.append("\n>>>---------------------------------------------------------------------");
		throw new AvailCompilerException(
			qualifiedName,
			charPos,
			source.indexOf('\n', (int) charPos),
			text.toString());
	}

	/**
	 * Parse more of a block's formal arguments from the token stream. A
	 * vertical bar is required after the arguments if there are any (which
	 * there are if we're here).
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param argsSoFar
	 *            The arguments that have been parsed so far.
	 * @param continuation
	 *            What to do with the list of arguments.
	 */
	void parseAdditionalBlockArgumentsAfterThen (
		final ParserState start,
		final List<AvailVariableDeclarationNode> argsSoFar,
		final Con<List<AvailVariableDeclarationNode>> continuation)
	{
		if (start.peekToken(OPERATOR, ",", "comma and more block arguments"))
		{
			parseBlockArgumentThen(
				start.afterToken(),
				new Con<AvailVariableDeclarationNode>(
					"Additional block argument")
				{
					@Override
					public void value (
						final ParserState afterArgument,
						final AvailVariableDeclarationNode arg)
					{
						final List<AvailVariableDeclarationNode> newArgsSoFar =
							new ArrayList<AvailVariableDeclarationNode>(
								argsSoFar);
						newArgsSoFar.add(arg);
						parseAdditionalBlockArgumentsAfterThen(
							afterArgument,
							Collections.unmodifiableList(newArgsSoFar),
							continuation);
					}
				});
		}

		if (start.peekToken(
			OPERATOR,
			"|",
			"command and more block arguments or a vertical bar"))
		{
			attempt(
				start.afterToken(),
				continuation,
				new ArrayList<AvailVariableDeclarationNode>(argsSoFar));
		}
	}

	/**
	 * Parse an assignment statement.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the parsed assignment statement.
	 */
	void parseAssignmentThen (
		final ParserState start,
		final Con<AvailAssignmentNode> continuation)
	{
		if (start.peekToken().tokenType() != KEYWORD)
		{
			// Don't suggest it's an assignment attempt with no evidence.
			return;
		}
		parseVariableUseWithExplanationThen(
			start,
			"for an assignment",
			new Con<AvailVariableUseNode>("Variable use for assignment")
			{
				@Override
				public void value (
					final ParserState afterVar,
					final AvailVariableUseNode varUse)
				{
					if (!afterVar.peekToken(OPERATOR, ":", ":= for assignment"))
					{
						return;
					}
					final ParserState afterColon = afterVar.afterToken();
					if (!afterColon.peekToken(
						OPERATOR,
						"=",
						"= part of := for assignment"))
					{
						return;
					}

					final ParserState afterEquals = afterColon.afterToken();
					final Mutable<AvailObject> varType =
						new Mutable<AvailObject>();
					final AvailVariableDeclarationNode declaration = varUse
							.associatedDeclaration();
					boolean ok = true;
					if (declaration == null)
					{
						ok = false;
						start.expected("variable to have been declared");
					}
					else if (declaration.isSyntheticVariableDeclaration())
					{
						varType.value = declaration.declaredType();
					}
					else
					{
						if (declaration.isArgument())
						{
							ok = false;
							start.expected(
								"assignment variable not to be an argument");
						}
						if (declaration.isLabel())
						{
							ok = false;
							start.expected(
								"assignment variable to be local, not label");
						}
						if (declaration.isConstant())
						{
							ok = false;
							start.expected(
								"assignment variable not to be constant");
						}
						varType.value = declaration.declaredType();
					}

					if (!ok)
					{
						return;
					}
					parseExpressionThen(afterEquals, new Con<AvailParseNode>(
						"Expression for right side of assignment")
					{
						@Override
						public void value (
							final ParserState afterExpr,
							final AvailParseNode expr)
						{
							if (afterExpr.peekToken().tokenType()
									!= END_OF_STATEMENT)
							{
								afterExpr.expected(
									"; to end assignment statement");
								return;
							}
							if (expr.type().isSubtypeOf(varType.value))
							{
								final AvailAssignmentNode assignment =
									new AvailAssignmentNode();
								assignment.variable(varUse);
								assignment.expression(expr);
								attempt(afterExpr, continuation, assignment);
							}
							else
							{
								afterExpr.expected(new Generator<String>()
								{
									@Override
									public String value ()
									{
										return "assignment expression's type ("
												+ expr.type().toString()
												+ ") to match variable type ("
												+ varType.value.toString()
												+ ")";
									}
								});
							}
						}
					});
				}
			});
	}

	/**
	 * Parse a block's formal arguments from the token stream. A vertical bar
	 * ("|") is required after the arguments if there are any.
	 *
	 * @param start
	 *            Where to parse.
	 * @param continuation
	 *            What to do with the list of block arguments.
	 */
	private void parseBlockArgumentsThen (
		final ParserState start,
		final Con<List<AvailVariableDeclarationNode>> continuation)
	{
		// Try it with no arguments.
		attempt(
			start,
			continuation,
			new ArrayList<AvailVariableDeclarationNode>());
		parseBlockArgumentThen(start, new Con<AvailVariableDeclarationNode>(
			"Block argument")
		{
			@Override
			public void value (
				final ParserState afterFirstArg,
				final AvailVariableDeclarationNode firstArg)
			{
				final List<AvailVariableDeclarationNode> argsList =
					new ArrayList<AvailVariableDeclarationNode>(1);
				argsList.add(firstArg);
				parseAdditionalBlockArgumentsAfterThen(
					afterFirstArg,
					Collections.unmodifiableList(argsList),
					continuation);
			}
		});
	}

	/**
	 * Parse a single block argument.
	 *
	 * @param start
	 *            Where to parse.
	 * @param continuation
	 *            What to do with the parsed block argument.
	 */
	void parseBlockArgumentThen (
		final ParserState start,
		final Con<AvailVariableDeclarationNode> continuation)
	{
		final AvailObject localName = start.peekToken();
		if (localName.tokenType() != KEYWORD)
		{
			start.expected(": then block argument type");
			return;
		}
		final ParserState afterArgName = start.afterToken();
		if (!afterArgName.peekToken(OPERATOR, ":", ": then argument type"))
		{
			return;
		}
		parseAndEvaluateExpressionYieldingInstanceOfThen(
			afterArgName.afterToken(),
			Types.type.object(),
			new Con<AvailObject>("Type of block argument")
			{
				@Override
				public void value (
					final ParserState afterArgType,
					final AvailObject type)
				{
					if (type.equals(Types.voidType.object()))
					{
						afterArgType
								.expected("a type for the argument other than void");
					}
					else if (type.equals(Types.terminates.object()))
					{
						afterArgType
								.expected("a type for the argument other than terminates");
					}
					else
					{
						final AvailVariableDeclarationNode decl = new AvailVariableDeclarationNode();
						decl.name(localName);
						decl.declaredType(type);
						decl.isArgument(true);
						attempt(
							afterArgType.withDeclaration(decl),
							continuation,
							decl);
					}
				}
			});
	}

	/**
	 * Parse a block (a closure).
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the parsed block.
	 */
	private void parseBlockThen (
		final ParserState start,
		final Con<AvailBlockNode> continuation)
	{
		if (!start.peekToken(OPERATOR, "["))
		{
			// Don't suggest a block was expected here unless at least the "["
			// was present.
			return;
		}
		final AvailCompilerScopeStack scopeOutsideBlock = start.scopeStack;
		parseBlockArgumentsThen(
			start.afterToken(),
			new Con<List<AvailVariableDeclarationNode>>("Block arguments")
			{
				@Override
				public void value (
					final ParserState afterArguments,
					final List<AvailVariableDeclarationNode> arguments)
				{
					parseOptionalPrimitiveForArgCountThen(
						afterArguments,
						arguments.size(),
						new Con<Short>("Optional primitive")
						{
							@Override
							public void value (
								final ParserState afterOptionalPrimitive,
								final Short primitive)
							{
								parseStatementsThen(
									afterOptionalPrimitive,
									new Con<List<AvailParseNode>>(
										"Block statements")
									{
										@Override
										public void value (
											final ParserState afterStatements,
											final List<AvailParseNode> statements)
										{
											finishBlockThen(
												afterStatements,
												arguments,
												primitive,
												statements,
												scopeOutsideBlock,
												continuation);
										}
									});
							}
						});
				}
			});
	}

	/**
	 * Finish parsing a block. We've just parsed the list of statements.
	 *
	 * @param afterStatements
	 *            Where to start parsing, now that the statements have all been
	 *            parsed.
	 * @param arguments
	 *            The list of block arguments.
	 * @param primitive
	 *            The primitive number
	 * @param statements
	 *            The list of statements.
	 * @param scopeOutsideBlock
	 *            The scope that existed before the block started to be parsed.
	 * @param continuation
	 *            What to do with the {@link AvailBlockNode}.
	 */
	void finishBlockThen (
		final ParserState afterStatements,
		final List<AvailVariableDeclarationNode> arguments,
		final short primitive,
		final List<AvailParseNode> statements,
		final AvailCompilerScopeStack scopeOutsideBlock,
		final Con<AvailBlockNode> continuation)
	{
		if (primitive != 0 && primitive != 256 && statements.isEmpty())
		{
			afterStatements.expected(
				"mandatory failure code for primitive method (except #256)");
			return;
		}
		if (!afterStatements.peekToken(
			OPERATOR,
			"]",
			"close bracket (']') to end block"))
		{
			return;
		}
		final ParserState afterClose = afterStatements.afterToken();

		final Mutable<AvailObject> lastStatementType = new Mutable<AvailObject>();
		if (statements.size() > 0)
		{
			final AvailParseNode lastStatement = statements.get(
				statements.size() - 1);
			if (lastStatement.isDeclaration() || lastStatement.isAssignment())
			{
				lastStatementType.value = Types.voidType.object();
			}
			else
			{
				lastStatementType.value = lastStatement.type();
			}
		}
		else
		{
			lastStatementType.value = Types.voidType.object();
		}
		final ParserState stateOutsideBlock = new ParserState(
			afterClose.position,
			scopeOutsideBlock);

		if (statements.isEmpty() && primitive != 0)
		{
			afterClose.expected(
				"return type declaration for primitive block with "
				+ "no statements");
		}
		else
		{
			boolean blockTypeGood = true;
			if (statements.size() > 0 && statements.get(0).isLabel())
			{
				final AvailLabelNode labelNode = (AvailLabelNode) statements.get(0);
				final AvailObject labelClosureType = labelNode.declaredType()
						.closureType();
				blockTypeGood = labelClosureType.numArgs() == arguments.size()
						&& labelClosureType.returnType().equals(
							lastStatementType.value);
				for (int i = 1; i <= arguments.size(); i++)
				{
					if (blockTypeGood
							&& !labelClosureType.argTypeAt(i).equals(
								arguments.get(i - 1).declaredType()))
					{
						blockTypeGood = false;
					}
				}
			}
			if (blockTypeGood)
			{
				final AvailBlockNode blockNode = new AvailBlockNode();
				blockNode.arguments(arguments);
				blockNode.primitive(primitive);
				blockNode.statements(statements);
				blockNode.resultType(lastStatementType.value);
				attempt(stateOutsideBlock, continuation, blockNode);
			}
			else
			{
				afterClose.expected(
					"block with label to have return type void "
					+ "(otherwise exiting would need to provide a value)");
			}
		}

		if (!stateOutsideBlock.peekToken(
			OPERATOR,
			":",
			"optional block return type declaration"))
		{
			return;
		}
		parseAndEvaluateExpressionYieldingInstanceOfThen(
			stateOutsideBlock.afterToken(),
			Types.type.object(),
			new Con<AvailObject>("Block return type declaration")
			{
				@Override
				public void value (
					final ParserState afterReturnType,
					final AvailObject returnType)
				{
					if (statements.isEmpty() && primitive != 0
							|| lastStatementType.value.isSubtypeOf(returnType))
					{
						boolean blockTypeGood = true;
						if (statements.size() > 0
								&& statements.get(0).isLabel())
						{
							final AvailLabelNode labelNode = (AvailLabelNode) statements
									.get(0);
							final AvailObject labelClosureType = labelNode
									.declaredType().closureType();
							blockTypeGood = labelClosureType.numArgs() == arguments
									.size()
									&& labelClosureType.returnType().equals(
										returnType);
							for (int i = 1; i <= arguments.size(); i++)
							{
								if (blockTypeGood
										&& !labelClosureType.argTypeAt(i)
												.equals(
													arguments.get(i - 1)
															.declaredType()))
								{
									blockTypeGood = true;
								}
							}
						}
						if (blockTypeGood)
						{
							final AvailBlockNode blockNode = new AvailBlockNode();
							blockNode.arguments(arguments);
							blockNode.primitive(primitive);
							blockNode.statements(statements);
							blockNode.resultType(returnType);
							attempt(afterReturnType, continuation, blockNode);
						}
						else
						{
							stateOutsideBlock.expected(
								"label's type to agree with block type");
						}
					}
					else
					{
						afterReturnType.expected(new Generator<String>()
						{
							@Override
							public String value ()
							{
								return "last statement's type \""
										+ lastStatementType.value.toString()
										+ "\" to agree with block's declared result type \""
										+ returnType.toString() + "\".";
							}
						});
					}
				}
			});
	}

	/**
	 * Parse an expression, without directly using the
	 * {@linkplain #fragmentCache}.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the expression.
	 */
	void parseExpressionUncachedThen (
		final ParserState start,
		final Con<AvailParseNode> continuation)
	{
		parseLeadingKeywordSendThen(start, new Con<AvailSendNode>(
			"Uncached leading keyword send")
		{
			@Override
			public void value (
				final ParserState afterSendNode,
				final AvailSendNode sendNode)
			{
				parseOptionalLeadingArgumentSendAfterThen(
					afterSendNode,
					sendNode,
					continuation);
			}
		});
		parseSimpleThen(start, new Con<AvailParseNode>(
			"Uncached simple expression")
		{
			@Override
			public void value (
				final ParserState afterSimple,
				final AvailParseNode simpleNode)
			{
				parseOptionalLeadingArgumentSendAfterThen(
					afterSimple,
					simpleNode,
					continuation);
			}
		});
		parseBlockThen(start, new Con<AvailBlockNode>(
			"Uncached block expression")
		{
			@Override
			public void value (
				final ParserState afterBlock,
				final AvailBlockNode blockNode)
			{
				parseOptionalLeadingArgumentSendAfterThen(
					afterBlock,
					blockNode,
					continuation);
			}
		});
	}

	/**
	 * Parse an expression. Backtracking will find all valid interpretations.
	 * Note that a list expression requires at least two terms to form a list
	 * node. This method is a key optimization point, so the fragmentCache is
	 * used to keep track of parsing solutions at this point, simply replaying
	 * them on subsequent parses, as long as the variable declarations up to
	 * that point were identical.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param originalContinuation
	 *            What to do with the expression.
	 */
	void parseExpressionThen (
		final ParserState start,
		final Con<AvailParseNode> originalContinuation)
	{
		if (!fragmentCache.hasComputedForState(start))
		{
			final Mutable<Boolean> markerFired = new Mutable<Boolean>(false);
			attempt(
				new Continuation0()
				{
					@Override
					public void value ()
					{
						markerFired.value = true;
					}
				},
				"Expression marker",
				start.position);
			fragmentCache.startComputingForState(start);
			final Con<AvailParseNode> justRecord = new Con<AvailParseNode>(
				"Expression")
			{
				@Override
				public void value (
					final ParserState afterExpr,
					final AvailParseNode expr)
				{
					fragmentCache.addSolution(
						start,
						new AvailCompilerCachedSolution(afterExpr, expr));
				}
			};
			attempt(
				new Continuation0()
				{
					@Override
					public void value ()
					{
						parseExpressionUncachedThen(start, justRecord);
					}
				},
				"Capture expression for caching",
				start.position);
			// Force the previous attempts to all complete.
			while (!markerFired.value)
			{
				workStack.pop().value();
			}
		}
		// Deja vu! We were asked to parse an expression starting at this point
		// before. Luckily we had the foresight to record what those resulting
		// expressions were (as well as the scopeStack just after parsing each).
		// Replay just these solutions to the passed continuation. This has the
		// effect of eliminating each 'local' misparsing exactly once. I'm not
		// sure what happens to the order of the algorithm, but it might go from
		// exponential to small polynomial.
		final List<AvailCompilerCachedSolution> solutions =
			fragmentCache.solutionsAt(start);
		for (final AvailCompilerCachedSolution solution : solutions)
		{
			attempt(
				solution.endState(),
				originalContinuation,
				solution.parseNode());
		}
	}

	/**
	 * We've parsed part of a send. Try to finish the job.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param bundleTree
	 *            The bundle tree used to parse at this position.
	 * @param firstArgOrNull
	 *            Either null or an argument that must be consumed before any
	 *            keywords (or completion of a send).
	 * @param initialTokenPosition
	 *            The parse position where the send node started to be
	 *            processed. Does not count the position of the first argument
	 *            if there are no leading keywords.
	 * @param argsSoFar
	 *            The collection of arguments parsed so far. I do not modify it.
	 * @param continuation
	 *            What to do with a fully parsed send node.
	 */
	void parseRestOfSendNode (
		final ParserState start,
		final AvailObject bundleTree,
		final AvailParseNode firstArgOrNull,
		final ParserState initialTokenPosition,
		final List<AvailParseNode> argsSoFar,
		final Con<AvailSendNode> continuation)
	{
		final AvailObject complete = bundleTree.complete();
		final AvailObject incomplete = bundleTree.incomplete();
		final AvailObject special = bundleTree.specialActions();
		final boolean anyComplete = complete.mapSize() > 0;
		final boolean anyIncomplete = incomplete.mapSize() > 0;
		final boolean anySpecial = special.mapSize() > 0;
		assert anyComplete || anyIncomplete || anySpecial
		: "Expected a nonempty list of possible messages";
		if (anyComplete && firstArgOrNull == null
				&& start.position != initialTokenPosition.position)
		{
			// There are complete messages, we didn't leave a leading argument
			// stranded, and we made progress in the file (i.e., the message
			// send does not consist of exactly zero tokens, nor does it consist
			// of a solitary underscore).
			complete.mapDo(new Continuation2<AvailObject, AvailObject>()
			{
				@Override
				public void value (
					final AvailObject message,
					final AvailObject bundle)
				{
					if (interpreter.runtime().hasMethodsAt(message))
					{
						final Mutable<Boolean> valid = new Mutable<Boolean>();
						final AvailObject impSet = interpreter.runtime()
								.methodsAt(message);
						valid.value = true;
						final List<AvailObject> argTypes =
							new ArrayList<AvailObject>(argsSoFar.size());
						for (final AvailParseNode arg : argsSoFar)
						{
							argTypes.add(arg.type());
						}
						final AvailObject returnType = interpreter
							.validateTypesOfMessageSendArgumentTypesIfFail(
								message,
								argTypes,
								new Continuation1<Generator<String>>()
								{
									@Override
									public void value (
										final Generator<String> errorGenerator)
									{
										valid.value = false;
										start.expected(errorGenerator);
									}
								});
						if (valid.value)
						{
							checkRestrictionsIfFail(
								bundle,
								argsSoFar,
								new Continuation1<Generator<String>>()
								{
									@Override
									public void value (
										final Generator<String> errorGenerator)
									{
										valid.value = false;
										start.expected(errorGenerator);
									}
								});
						}
						if (valid.value)
						{
							final String errorMessage = interpreter
								.validateRequiresClauses(
									bundle.message(),
									argTypes);
							if (errorMessage != null)
							{
								valid.value = false;
								start.expected(errorMessage);
							}
						}
						if (valid.value)
						{
							final AvailSendNode sendNode = new AvailSendNode();
							sendNode.message(bundle.message());
							sendNode.bundle(bundle);
							sendNode.implementationSet(impSet);
							sendNode.arguments(argsSoFar);
							sendNode.returnType(returnType);
							attempt(start, continuation, sendNode);
						}
					}
				}
			});
		}
		if (anyIncomplete && firstArgOrNull == null)
		{
			final AvailObject keywordToken = start.peekToken();
			if (keywordToken.tokenType() == KEYWORD
					|| keywordToken.tokenType() == OPERATOR)
			{
				final AvailObject keywordString = keywordToken.string();
				if (incomplete.hasKey(keywordString))
				{
					final AvailObject subtree = incomplete.mapAt(keywordString);
					attempt(
						new Continuation0()
						{
							@Override
							public void value ()
							{
								parseRestOfSendNode(
									start.afterToken(),
									subtree,
									null,
									initialTokenPosition,
									argsSoFar,
									continuation);
							}
						},
						"Continue send after keyword: "
							+ keywordString.asNativeString(),
						start.afterToken().position);
				}
				else
				{
					expectedKeywordsOf(start, incomplete);
				}
			}
			else
			{
				expectedKeywordsOf(start, incomplete);
			}
		}
		if (anySpecial)
		{
			special.mapDo(new Continuation2<AvailObject, AvailObject>()
			{
				@Override
				public void value (
					final AvailObject instructionObject,
					final AvailObject successorTrees)
				{
					attempt(
						new Continuation0()
						{
							@Override
							public void value ()
							{
								runParsingInstructionThen(
									start,
									instructionObject.extractInt(),
									firstArgOrNull,
									argsSoFar,
									initialTokenPosition,
									successorTrees,
									continuation);
							}
						},
						"Continue with instruction " + instructionObject,
						start.position);
				}
			});
		}
	}

	/**
	 * Execute one non-keyword parsing instruction, then run the continuation.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param instruction
	 *            The {@link MessageSplitter instruction} to execute.
	 * @param firstArgOrNull
	 *            Either the already-parsed first argument or null. If we're
	 *            looking for leading-argument message sends to wrap an
	 *            expression then this is not-null before the first argument
	 *            position is encountered, otherwise it's null and we should
	 *            reject attempts to start with an argument (before a keyword).
	 * @param argsSoFar
	 *            The message arguments that have been parsed so far.
	 * @param initialTokenPosition
	 *            The position at which parsing of this message started. If it
	 *            was parsed as a leading argument send (i.e., firtArgOrNull
	 *            started out non-null) then the position is of the token
	 *            following the first argument.
	 * @param successorTrees
	 *            The {@link MessageBundleTreeDescriptor bundle trees} at which
	 *            to continue parsing.
	 * @param continuation
	 *            What to do with a complete {@link AvailSendNode message send}.
	 */
	void runParsingInstructionThen (
		final ParserState start,
		final int instruction,
		final AvailParseNode firstArgOrNull,
		final List<AvailParseNode> argsSoFar,
		final ParserState initialTokenPosition,
		final AvailObject successorTrees,
		final Con<AvailSendNode> continuation)
	{
		switch (instruction)
		{
			case 0:
			{
				// Parse an argument and continue.
				assert successorTrees.tupleSize() == 1;
				parseSendArgumentWithExplanationThen(
					start,
					" (an argument of some message)",
					firstArgOrNull,
					initialTokenPosition,
					new Con<AvailParseNode>("Argument of message send")
					{
						@Override
						public void value (
							final ParserState afterArg,
							final AvailParseNode newArg)
						{
							final List<AvailParseNode> newArgsSoFar =
								new ArrayList<AvailParseNode>(argsSoFar);
							newArgsSoFar.add(newArg);
							attempt(
								new Continuation0()
								{
									@Override
									public void value ()
									{
										parseRestOfSendNode(
											afterArg,
											successorTrees.tupleAt(1),
											null,
											initialTokenPosition,
											Collections.unmodifiableList(
												newArgsSoFar),
											continuation);
									}
								},
								"Continue send after argument",
								afterArg.position);
						}
					});
				break;
			}
			case 1:
			{
				// Push an empty list node and continue.
				assert successorTrees.tupleSize() == 1;
				final List<AvailParseNode> newArgsSoFar =
					new ArrayList<AvailParseNode>(argsSoFar);
				final AvailTupleNode newTupleNode = new AvailTupleNode(
					Collections.<AvailParseNode> emptyList());
				newArgsSoFar.add(newTupleNode);
				attempt(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								start,
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								Collections.unmodifiableList(newArgsSoFar),
								continuation);
						}
					},
					"Continue send after push empty",
					start.position);
				break;
			}
			case 2:
			{
				// Append the item that's the last thing to the
				// list that's the second last thing. Pop both and
				// push the new list (the original list must not
				// change), then continue.
				assert successorTrees.tupleSize() == 1;
				final List<AvailParseNode> newArgsSoFar =
					new ArrayList<AvailParseNode>(argsSoFar);
				final AvailParseNode value = newArgsSoFar.remove(
					newArgsSoFar.size() - 1);
				final AvailParseNode oldNode = newArgsSoFar.remove(
					newArgsSoFar.size() - 1);
				AvailTupleNode tupleNode = (AvailTupleNode) oldNode;
				tupleNode = tupleNode.copyWith(value);
				newArgsSoFar.add(tupleNode);
				attempt(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								start,
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								Collections.unmodifiableList(newArgsSoFar),
								continuation);
						}
					},
					"Continue send after append",
					start.position);
				break;
			}
			case 3:
			{
				// Push current parse position.
				assert successorTrees.tupleSize() == 1;
				final List<AvailParseNode> newArgsSoFar =
					new ArrayList<AvailParseNode>(argsSoFar);
				newArgsSoFar.add(
					new AvailMarkerNode(
						IntegerDescriptor.objectFromInt(start.position)));
				attempt(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								start,
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								Collections.unmodifiableList(newArgsSoFar),
								continuation);
						}
					},
					"Continue send after push parse position",
					start.position);
				break;
			}
			case 4:
			{
				// Underpop saved parse position (from 2nd-to-top of stack).
				assert successorTrees.tupleSize() == 1;
				final List<AvailParseNode> newArgsSoFar =
					new ArrayList<AvailParseNode>(argsSoFar);
				final AvailParseNode marker =
					newArgsSoFar.remove(newArgsSoFar.size() - 2);
				assert marker instanceof AvailMarkerNode;
				attempt(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								start,
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								Collections.unmodifiableList(newArgsSoFar),
								continuation);
						}
					},
					"Continue send after underpop saved position",
					start.position);
				break;
			}
			case 5:
			{
				// Check parse progress (abort if parse position is still equal
				// to value at 2nd-to-top of stack).  Also update the entry to
				// be the new parse position.
				assert successorTrees.tupleSize() == 1;
				final AvailParseNode shouldBeMarker =
					argsSoFar.get(argsSoFar.size() - 2);
				final AvailMarkerNode marker = (AvailMarkerNode) shouldBeMarker;
				if (marker.markerValue().extractInt() == start.position)
				{
					return;
				}
				final List<AvailParseNode> newArgsSoFar =
					new ArrayList<AvailParseNode>(argsSoFar);
				newArgsSoFar.set(
					newArgsSoFar.size() - 2,
					new AvailMarkerNode(
						IntegerDescriptor.objectFromInt(start.position)));
				attempt(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								start,
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								Collections.unmodifiableList(newArgsSoFar),
								continuation);
						}
					},
					"Continue send after check parse progress",
					start.position);
				break;
			}
			case 6:
			{
				assert false : "Reserved parsing instruction";
				break;
			}
			case 7:
			{
				assert false : "Reserved parsing instruction";
				break;
			}
			default:
			{
				// Branch or jump, or else we shouldn't be here. Continue along
				// each available path.
				assert instruction >= 8 && (instruction & 7) <= 1;
				for (final AvailObject successorTree : successorTrees)
				{
					attempt(
						new Continuation0()
						{
							@Override
							public void value ()
							{
								parseRestOfSendNode(
									start,
									successorTree,
									firstArgOrNull,
									initialTokenPosition,
									Collections.unmodifiableList(argsSoFar),
									continuation);
							}
						},
						"Continue send after branch or jump",
						start.position);
				}
			}
		}
	}

	/**
	 * Make sure none of my arguments are message sends that have been
	 * disallowed in that position by a negative precedence declaration.
	 *
	 * @param bundle
	 *            The bundle for which a send node was just parsed. It contains
	 *            information about any negative precedence restrictions.
	 * @param arguments
	 *            The argument expressions for the send that was just parsed.
	 * @param ifFail
	 *            What to do when a negative precedence rule inhibits a parse.
	 */
	void checkRestrictionsIfFail (
		final AvailObject bundle,
		final List<AvailParseNode> arguments,
		final Continuation1<Generator<String>> ifFail)
	{
		for (int i = 1; i <= arguments.size(); i++)
		{
			final int index = i;
			final AvailParseNode argument = arguments.get(index - 1);
			if (argument.isSend())
			{
				final AvailSendNode innerSend = (AvailSendNode) argument;
				final AvailObject restrictions = bundle.restrictions().tupleAt(index);
				if (restrictions.hasElement(innerSend.message()))
				{
					ifFail.value(new Generator<String>()
					{
						@Override
						public String value ()
						{
							return "different nesting for argument #"
									+ Integer.toString(index) + " in "
									+ bundle.message().toString();
						}
					});
				}
			}
		}
	}

	/**
	 * Report that the parser was expecting one of several keywords. The
	 * keywords are keys of the {@link MapDescriptor map} argument
	 * {@code incomplete}.
	 *
	 * @param where
	 *            Where the keywords were expected.
	 * @param incomplete
	 *            A map of partially parsed keywords, where the keys are the
	 *            strings that were expected at this position.
	 */
	private void expectedKeywordsOf (
		final ParserState where,
		final AvailObject incomplete)
	{
		where.expected(new Generator<String>()
		{
			@Override
			public String value ()
			{
				final StringBuilder builder = new StringBuilder(200);
				builder.append("one of the following internal keywords: ");
				final List<String> sorted = new ArrayList<String>(incomplete
						.mapSize());
				incomplete.mapDo(new Continuation2<AvailObject, AvailObject>()
				{
					@Override
					public void value (
						final AvailObject key,
						final AvailObject value)
					{
						sorted.add(key.asNativeString());
					}
				});
				Collections.sort(sorted);
				if (incomplete.mapSize() > 5)
				{
					builder.append("\n\t");
				}
				for (final String s : sorted)
				{
					builder.append(s);
					builder.append("  ");
				}
				return builder.toString();
			}
		});
	}

	/**
	 * Parse a send node whose leading argument has already been parsed.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param leadingArgument
	 *            The argument that was already parsed.
	 * @param continuation
	 *            What to do after parsing a send node.
	 */
	void parseLeadingArgumentSendAfterThen (
		final ParserState start,
		final AvailParseNode leadingArgument,
		final Con<AvailSendNode> continuation)
	{
		parseRestOfSendNode(
			start,
			interpreter.rootBundleTree(),
			leadingArgument,
			start,
			Collections.<AvailParseNode> emptyList(),
			continuation);
	}

	/**
	 * Parse a send node. To prevent infinite left-recursion and false
	 * ambiguity, we only allow a send with a leading keyword to be parsed from
	 * here, since leading underscore sends are dealt with iteratively
	 * afterward.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do after parsing a complete send node.
	 */
	private void parseLeadingKeywordSendThen (
		final ParserState start,
		final Con<AvailSendNode> continuation)
	{
		parseRestOfSendNode(
			start,
			interpreter.rootBundleTree(),
			null,
			start,
			Collections.<AvailParseNode> emptyList(),
			continuation);
	}

	/**
	 * Parse an expression that isn't a list. Backtracking will find all valid
	 * interpretations.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param node
	 *            An expression that acts as the first argument for a potential
	 *            leading-argument message send, or possibly a chain of them.
	 * @param continuation
	 *            What to do with either the passed node, or the node wrapped in
	 *            suitable leading-argument message sends.
	 */
	void parseOptionalLeadingArgumentSendAfterThen (
		final ParserState start,
		final AvailParseNode node,
		final Con<AvailParseNode> continuation)
	{
		// It's optional, so try it with no wrapping.
		attempt(start, continuation, node);

		// Don't wrap it if its type is void.
		if (node.type().equals(Types.voidType.object()))
		{
			return;
		}

		// Try to wrap it in a leading-argument message send.
		parseOptionalSuperCastAfterErrorSuffixThen(
			start,
			node,
			" in case it's the first argument of a non-keyword-leading message",
			new Con<AvailParseNode>("Optional supercast")
			{
				@Override
				public void value (
					final ParserState afterCast,
					final AvailParseNode cast)
				{
					parseLeadingArgumentSendAfterThen(
						afterCast,
						cast,
						new Con<AvailSendNode>(
							"Leading argument send after optional supercast")
						{
							@Override
							public void value (
								final ParserState afterSend,
								final AvailSendNode leadingSend)
							{
								parseOptionalLeadingArgumentSendAfterThen(
									afterSend,
									leadingSend,
									continuation);
							}
						});
				}
			});
	}

	/**
	 * Parse the optional primitive declaration at the start of a block. Since
	 * it's optional, try the continuation with a zero argument without having
	 * parsed anything, then try to parse "Primitive N;" for some supported
	 * integer N.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param argCount
	 *            The number of arguments accepted by the block being parsed.
	 * @param continuation
	 *            What to do with the parsed primitive number.
	 */
	void parseOptionalPrimitiveForArgCountThen (
		final ParserState start,
		final int argCount,
		final Con<Short> continuation)
	{
		// Try it first without looking for the primitive declaration.
		attempt(start, continuation, (short) 0);

		// Now look for the declaration.
		if (!start.peekToken(
			KEYWORD,
			"Primitive",
			"optional primitive declaration"))
		{
			return;
		}
		final ParserState afterPrimitiveKeyword = start.afterToken();
		final AvailObject token = afterPrimitiveKeyword.peekToken();
		if (token.tokenType() != LITERAL
				|| !token.literal().isInstanceOfSubtypeOf(
					IntegerRangeTypeDescriptor.positiveShorts()))
		{
			afterPrimitiveKeyword.expected(new Generator<String>()
			{
				@Override
				public String value ()
				{
					return
						"A positive short "
						+ IntegerRangeTypeDescriptor.positiveShorts()
						+ " after the Primitive keyword";
				}
			});
			return;
		}
		final short primitive = (short) token.literal().extractInt();
		if (!interpreter.supportsPrimitive(primitive))
		{
			afterPrimitiveKeyword
					.expected("a supported primitive number, not #"
							+ Short.toString(primitive));
			return;
		}

		if (!interpreter.primitiveAcceptsThisManyArguments(primitive, argCount))
		{
			final Primitive prim = Primitive.byPrimitiveNumber(primitive);
			afterPrimitiveKeyword.expected(new Generator<String>()
			{
				@Override
				public String value ()
				{
					return "Primitive #" + Short.toString(primitive) + " ("
							+ prim.name() + ") to be passed "
							+ Integer.toString(prim.argCount())
							+ " arguments, not " + Integer.toString(argCount);
				}
			});
		}
		final ParserState afterPrimitiveNumber = afterPrimitiveKeyword
				.afterToken();
		if (!afterPrimitiveNumber.peekToken(
			END_OF_STATEMENT,
			";",
			"; after Primitive N declaration"))
		{
			return;
		}
		attempt(afterPrimitiveNumber.afterToken(), continuation, primitive);
	}

	/**
	 * An expression was parsed. Now parse the optional supercast clause that
	 * may follow it to make a supercast node.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param expr
	 *            The expression after which to look for a supercast clause.
	 * @param errorSuffix
	 *            A suffix for messages describing what was expected.
	 * @param continuation
	 *            What to do with the supercast node, if present, or just the
	 *            passed expression if not.
	 */
	void parseOptionalSuperCastAfterErrorSuffixThen (
		final ParserState start,
		final AvailParseNode expr,
		final String errorSuffix,
		final Con<? super AvailParseNode> continuation)
	{
		// Optional, so try it without a super cast.
		attempt(start, continuation, expr);

		if (!start.peekToken(OPERATOR, ":"))
		{
			return;
		}
		final ParserState afterColon = start.afterToken();
		if (!afterColon.peekToken(OPERATOR, ":"))
		{
			start.expected(new Generator<String>()
			{
				@Override
				public String value ()
				{
					return ":: to supercast an expression" + errorSuffix;
				}
			});
			return;
		}
		final ParserState afterSecondColon = afterColon.afterToken();
		attempt(
			new Continuation0()
			{
				@Override
				public void value ()
				{
					parseAndEvaluateExpressionYieldingInstanceOfThen(
						afterSecondColon,
						Types.type.object(),
						new Con<AvailObject>("Type expression of supercast")
						{
							@Override
							public void value (
								final ParserState afterType,
								final AvailObject type)
							{
								if (expr.type().isSubtypeOf(type))
								{
									final AvailSuperCastNode cast =
										new AvailSuperCastNode();
									cast.expression(expr);
									cast.type(type);
									attempt(afterType, continuation, cast);
								}
								else
								{
									afterType.expected(
										"supercast type to be supertype "
										+ "of expression's type.");
								}
							}
						});
				}
			},
			"Type expression in supercast",
			afterSecondColon.position);
	}

	/**
	 * Parse an argument to a message send. Backtracking will find all valid
	 * interpretations.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param explanation
	 *            A {@link String} indicating why it's parsing an argument.
	 * @param firstArgOrNull
	 *            Either a parse node to use as the argument, or null if we
	 *            should parse one now.
	 * @param initialTokenPosition
	 *            The position at which we started parsing the message send.
	 *            Does not include the first argument if there were no leading
	 *            keywords.
	 * @param continuation
	 *            What to do with the argument.
	 */
	void parseSendArgumentWithExplanationThen (
		final ParserState start,
		final String explanation,
		final AvailParseNode firstArgOrNull,
		final ParserState initialTokenPosition,
		final Con<AvailParseNode> continuation)
	{
		if (firstArgOrNull == null)
		{
			// There was no leading argument. If we haven't parsed any keywords
			// then don't allow this argument parse to happen, since we must be
			// trying to parse a leading-keyword message send.
			if (start.position != initialTokenPosition.position)
			{
				parseExpressionThen(start, new Con<AvailParseNode>(
					"Argument expression (irrespective of supercast)")
				{
					@Override
					public void value (
						final ParserState afterArgument,
						final AvailParseNode argument)
					{
						parseOptionalSuperCastAfterErrorSuffixThen(
							afterArgument,
							argument,
							explanation,
							continuation);
					}
				});
			}
		}
		else
		{
			// We're parsing a message send with a leading argument. There
			// should have been no way to parse any keywords or other arguments
			// yet, so make sure the position hasn't budged since we started.
			// Then use the provided first argument.
			parseOptionalSuperCastAfterErrorSuffixThen(
				initialTokenPosition,
				firstArgOrNull,
				explanation,
				continuation);
		}
	}

	/**
	 * Parse a variable, reference, or literal, then invoke the continuation.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the simple parse node.
	 */
	private void parseSimpleThen (
		final ParserState start,
		final Con<? super AvailParseNode> continuation)
	{
		// Try a variable use.
		parseVariableUseWithExplanationThen(start, "", continuation);

		// Try a literal.
		if (start.peekToken().tokenType() == LITERAL)
		{
			final AvailLiteralNode literalNode = new AvailLiteralNode();
			literalNode.token(start.peekToken());
			attempt(start.afterToken(), continuation, literalNode);
		}

		// Try a reference: &var.
		if (start.peekToken(OPERATOR, "&"))
		{
			final ParserState afterAmpersand = start.afterToken();
			parseVariableUseWithExplanationThen(
				afterAmpersand,
				"in reference expression",
				new Con<AvailVariableUseNode>("Variable for reference")
				{
					@Override
					public void value (
						final ParserState afterVar,
						final AvailVariableUseNode var)
					{
						final AvailVariableDeclarationNode declaration = var
								.associatedDeclaration();
						if (declaration == null)
						{
							afterAmpersand
									.expected("reference variable to have been declared");
						}
						else if (declaration.isConstant())
						{
							afterAmpersand
									.expected("reference variable not to be a constant");
						}
						else if (declaration.isArgument())
						{
							afterAmpersand
									.expected("reference variable not to be an argument");
						}
						else
						{
							final AvailReferenceNode referenceNode = new AvailReferenceNode();
							referenceNode.variable(var);
							attempt(afterVar, continuation, referenceNode);
						}
					}
				});
		}
		start.expected("simple expression");
	}

	/**
	 * Parse zero or more statements from the tokenStream. Parse as many
	 * statements as possible before invoking the continuation.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the list of statements.
	 */
	void parseStatementsThen (
		final ParserState start,
		final Con<List<AvailParseNode>> continuation)
	{
		parseMoreStatementsThen(
			start,
			Collections.<AvailParseNode>emptyList(),
			continuation);
	}

	/**
	 * Try the current list of statements but also try to parse more.
	 *
	 * @param start Where to parse.
	 * @param statements The preceding list of statements.
	 * @param continuation What to do with the resulting list of statements.
	 */
	void parseMoreStatementsThen (
		final ParserState start,
		final List <AvailParseNode> statements,
		final Con<List<AvailParseNode>> continuation)
	{
		// Try it with the current list of statements.
		attempt(start, continuation, statements);

		// See if more statements would be legal.
		if (statements.size() > 0)
		{
			final AvailParseNode lastStatement = statements.get(
				statements.size() - 1);
			if (!lastStatement.isAssignment()
					&& !lastStatement.isDeclaration()
					&& !lastStatement.isLabel())
			{
				if (lastStatement.type().equals(Types.terminates.object()))
				{
					start.expected(
						"end of statements since this one always terminates");
					return;
				}
				if (!lastStatement.type().equals(Types.voidType.object()))
				{
					start.expected(new Generator<String>()
					{
						@Override
						public String value ()
						{
							return "non-last statement \""
									+ lastStatement.toString()
									+ "\" to have type void, not \""
									+ lastStatement.type().toString()
									+ "\".";
						}
					});
				}
			}
		}
		start.expected("more statements");

		// Try for more statements.
		parseStatementAsOutermostCanBeLabelThen(
			start,
			false,
			statements.isEmpty(),
			new Con<AvailParseNode>("Another statement")
			{
				@Override
				public void value (
					final ParserState afterStatement,
					final AvailParseNode newStatement)
				{
					final List<AvailParseNode> newStatements =
						new ArrayList<AvailParseNode>(statements);
					newStatements.add(newStatement);
					parseMoreStatementsThen(
						afterStatement,
						newStatements,
						continuation);
				}
			});
	}

	/**
	 * Parse the use of a variable.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param explanation
	 *            The string explaining why we were parsing a use of a variable.
	 * @param continuation
	 *            What to do after parsing the variable use.
	 */
	private void parseVariableUseWithExplanationThen (
		final ParserState start,
		final String explanation,
		final Con<? super AvailVariableUseNode> continuation)
	{
		final AvailObject token = start.peekToken();
		if (token.tokenType() != KEYWORD)
		{
			return;
		}
		final ParserState afterVar = start.afterToken();
		// First check if it's in a block scope...
		final AvailVariableDeclarationNode localDecl = lookupDeclaration(
			start,
			token.string());
		if (localDecl != null)
		{
			final AvailVariableUseNode varUse = new AvailVariableUseNode();
			varUse.name(token);
			varUse.associatedDeclaration(localDecl);
			attempt(afterVar, continuation, varUse);
			// Variables in inner scopes HIDE module variables.
			return;
		}
		// Not in a block scope. See if it's a module variable or module
		// constant...
		final AvailObject varName = token.string();
		if (module.variableBindings().hasKey(varName))
		{
			final AvailObject variableObject = module.variableBindings().mapAt(
				varName);
			final AvailVariableSyntheticDeclarationNode moduleVarDecl =
				new AvailVariableSyntheticDeclarationNode();
			moduleVarDecl.name(token);
			moduleVarDecl.declaredType(variableObject.type().innerType());
			moduleVarDecl.isArgument(false);
			moduleVarDecl.availVariable(variableObject);
			final AvailVariableUseNode varUse = new AvailVariableUseNode();
			varUse.name(token);
			varUse.associatedDeclaration(moduleVarDecl);
			attempt(afterVar, continuation, varUse);
			return;
		}
		if (module.constantBindings().hasKey(varName))
		{
			final AvailObject valueObject = module.constantBindings().mapAt(
				varName);
			final AvailConstantSyntheticDeclarationNode moduleConstDecl =
				new AvailConstantSyntheticDeclarationNode();
			moduleConstDecl.name(token);
			moduleConstDecl.declaredType(valueObject.type());
			moduleConstDecl.isArgument(false);
			moduleConstDecl.availValue(valueObject);
			final AvailVariableUseNode varUse = new AvailVariableUseNode();
			varUse.name(token);
			varUse.associatedDeclaration(moduleConstDecl);
			attempt(afterVar, continuation, varUse);
			return;
		}
		start.expected(new Generator<String>()
		{
			@Override
			public String value ()
			{
				return "variable " + token.string()
						+ " to have been declared before use " + explanation;
			}
		});
	}

	/**
	 * A statement was parsed correctly in two different ways. There may be more
	 * ways, but we stop after two as it's already an error. Report the error.
	 *
	 * @param where
	 *            Where the expressions were parsed from.
	 * @param interpretation1
	 *            The first interpretation as a {@link AvailParseNode}.
	 * @param interpretation2
	 *            The second interpretation as a {@link AvailParseNode}.
	 */
	private void ambiguousInterpretationsAnd (
		final ParserState where,
		final AvailParseNode interpretation1,
		final AvailParseNode interpretation2)
	{
		where.expected(new Generator<String>()
		{
			@Override
			public String value ()
			{
				final StringBuilder builder = new StringBuilder(200);
				builder.append("unambiguous interpretation.  ");
				builder.append("Here are two possible parsings...\n");
				builder.append("\t");
				builder.append(interpretation1.toString());
				builder.append("\n\t");
				builder.append(interpretation2.toString());
				return builder.toString();
			}
		});
	}

	/**
	 * Clear the fragment cache.
	 */
	private void privateClearFrags ()
	{
		fragmentCache.clear();
	}

	/**
	 * Answer a {@linkplain Generator} that will produce the given string.
	 *
	 * @param string
	 *            The exact string to generate.
	 * @return A generator that produces the string that was provided.
	 */
	Generator<String> generate (final String string)
	{
		return new Generator<String>()
		{
			@Override
			public String value ()
			{
				return string;
			}
		};
	}

	/**
	 * Look up a local declaration that has the given name, or null if not
	 * found.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param name
	 *            The name of the variable declaration for which to look.
	 * @return The declaration or null.
	 */
	private AvailVariableDeclarationNode lookupDeclaration (
		final ParserState start,
		final AvailObject name)
	{
		AvailCompilerScopeStack scope = start.scopeStack;
		while (scope.name() != null)
		{
			if (scope.name().equals(name))
			{
				return scope.declaration();
			}
			scope = scope.next();
		}
		return null;
	}

	/**
	 * Start definition of a {@linkplain ModuleDescriptor module}. The entire
	 * definition can be rolled back because the {@linkplain Interpreter
	 * interpreter}'s context module will contain all methods and precedence
	 * rules defined between the transaction start and the rollback (or commit).
	 * Committing simply clears this information.
	 */
	private void startModuleTransaction ()
	{
		assert module == null;
		module = ModuleDescriptor.newModule();
		interpreter.setModule(module);
	}

	/**
	 * Rollback the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction() startModuleTransaction}.
	 */
	private void rollbackModuleTransaction ()
	{
		assert module != null;
		module.removeFrom(interpreter);
		module = null;
		interpreter.setModule(null);
	}

	/**
	 * Commit the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction() startModuleTransaction}.
	 * Simply clear the "{@linkplain #module module}" instance variable.
	 */
	private void commitModuleTransaction ()
	{
		assert module != null;
		interpreter.runtime().addModule(module);
		module.cleanUpAfterCompile();
		module = null;
		interpreter.setModule(null);
	}

}
