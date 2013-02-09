/**
 * AbstractAvailCompiler.java
 * Copyright © 1993-2013, Mark van Gulik and Todd L Smith. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
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

import static com.avail.compiler.AbstractAvailCompiler.ExpectedToken.*;
import static com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind.*;
import static com.avail.descriptor.TokenDescriptor.TokenType.*;
import static com.avail.descriptor.TupleDescriptor.toList;
import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.utility.PrefixSharingList.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import com.avail.*;
import com.avail.annotations.*;
import com.avail.builder.*;
import com.avail.compiler.scanning.*;
import com.avail.descriptor.*;
import com.avail.descriptor.TokenDescriptor.TokenType;
import com.avail.interpreter.*;
import com.avail.interpreter.primitive.P_352_RejectParsing;
import com.avail.serialization.*;
import com.avail.utility.*;

/**
 * The abstract compiler for Avail code.  Subclasses may wish to implement, oh,
 * say, a system version with a hard-coded basic syntax and a non-system version
 * with no hard-coded syntax but macro capability.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public abstract class AbstractAvailCompiler
{
	/**
	 * The {@link AvailRuntime} for the compiler. Since a compiler cannot
	 * migrate between two runtime environments, it is safe to cache it for
	 * efficient access.
	 */
	final AvailRuntime runtime = AvailRuntime.current();

	/**
	 * A module's header information.
	 */
	public static class ModuleHeader
	{
		/**
		 * The {@link ModuleName} of the module undergoing compilation.
		 */
		public final ResolvedModuleName moduleName;

		/**
		 * Whether this is the header of a system module.
		 */
		public boolean isSystemModule;

		/**
		 * The versions for which the module undergoing compilation guarantees
		 * support.
		 */
		public final List<AvailObject> versions =
			new ArrayList<AvailObject>();

		/**
		 * The {@linkplain ModuleDescriptor modules} extended by the module
		 * undergoing compilation. Each element is a {@linkplain TupleDescriptor
		 * 3-tuple} whose first element is a module {@linkplain StringDescriptor
		 * name}, whose second element is the {@linkplain SetDescriptor set} of
		 * {@linkplain MethodDefinitionDescriptor method} names to import
		 * (and re-export), and whose third element is the set of conformant
		 * versions.
		 */
		public final List<AvailObject> extendedModules =
			new ArrayList<AvailObject>();

		/**
		 * The {@linkplain ModuleDescriptor modules} used by the module
		 * undergoing compilation. Each element is a {@linkplain TupleDescriptor
		 * 3-tuple} whose first element is a module {@linkplain StringDescriptor
		 * name}, whose second element is the {@linkplain SetDescriptor set} of
		 * {@linkplain MethodDefinitionDescriptor method} names to import,
		 * and whose third element is the set of conformant versions.
		 */
		public final List<AvailObject> usedModules =
			new ArrayList<AvailObject>();

		/**
		 * The {@linkplain AtomDescriptor names} defined and exported by the
		 * {@linkplain ModuleDescriptor module} undergoing compilation.
		 */
		public final List<AvailObject> exportedNames =
			new ArrayList<AvailObject>();

		/**
		 * The {@linkplain String pragma strings}.
		 */
		public final List<AvailObject> pragmas =
			new ArrayList<AvailObject>();

		/**
		 * Construct a new {@link AbstractAvailCompiler.ModuleHeader}.
		 *
		 * @param moduleName
		 *        The {@link ResolvedModuleName resolved name} of the module.
		 */
		public ModuleHeader (final ResolvedModuleName moduleName)
		{
			this.moduleName = moduleName;
		}

		/**
		 * @param serializer
		 */
		public void serializeHeaderOn (final Serializer serializer)
		{
			serializer.serialize(
				StringDescriptor.from(moduleName.qualifiedName()));
			serializer.serialize(
				AtomDescriptor.objectFromBoolean(isSystemModule));
			serializer.serialize(
				TupleDescriptor.fromList(versions));
			serializer.serialize(
				TupleDescriptor.fromList(extendedModules));
			serializer.serialize(
				TupleDescriptor.fromList(usedModules));
			serializer.serialize(
				TupleDescriptor.fromList(exportedNames));
			serializer.serialize(
				TupleDescriptor.fromList(pragmas));
		}

		/**
		 * Extract the module's header information from the {@link
		 * Deserializer}.
		 *
		 * @param deserializer The source of the header information.
		 * @throws MalformedSerialStreamException if malformed.
		 */
		public void deserializeHeaderFrom (final Deserializer deserializer)
			throws MalformedSerialStreamException
		{
			AvailObject object = deserializer.deserialize();
			assert object != null;
			if (!object.asNativeString().equals(moduleName.qualifiedName()))
			{
				throw new RuntimeException("Incorrect module name");
			}
			object = deserializer.deserialize();
			assert object != null;
			isSystemModule = object.extractBoolean();
			object = deserializer.deserialize();
			assert object != null;
			versions.clear();
			versions.addAll(toList(object));
			object = deserializer.deserialize();
			assert object != null;
			extendedModules.clear();
			extendedModules.addAll(toList(object));
			object = deserializer.deserialize();
			assert object != null;
			usedModules.clear();
			usedModules.addAll(toList(object));
			object = deserializer.deserialize();
			assert object != null;
			exportedNames.clear();
			exportedNames.addAll(toList(object));
			object = deserializer.deserialize();
			assert object != null;
			pragmas.clear();
			pragmas.addAll(toList(object));
		}

		/**
		 * Update the given module to correspond with information that has been
		 * accumulated in this {@link ModuleHeader}.
		 *
		 * @param module The module to update.
		 * @param runtime The current {@link AvailRuntime}.
		 * @return An error message {@link String} if there was a problem, or
		 *         {@code null} if no problems were encountered.
		 */
		public @Nullable String applyToModule (
			final AvailObject module,
			final AvailRuntime runtime)
		{
			final ModuleNameResolver resolver = runtime.moduleNameResolver();
			module.versions(SetDescriptor.fromCollection(versions));
			for (final AvailObject modImport : extendedModules)
			{
				assert modImport.isTuple();
				assert modImport.tupleSize() == 3;

				final ResolvedModuleName ref = resolver.resolve(
					moduleName.asSibling(
						modImport.tupleAt(1).asNativeString()));
				assert ref != null;
				final AvailObject availRef = StringDescriptor.from(
					ref.qualifiedName());
				if (!runtime.includesModuleNamed(availRef))
				{
					return
						"module \"" + ref.qualifiedName()
						+ "\" to be loaded already";
				}

				final AvailObject mod = runtime.moduleAt(availRef);
				final AvailObject reqVersions = modImport.tupleAt(3);
				if (reqVersions.setSize() > 0)
				{
					final AvailObject modVersions = mod.versions();
					final AvailObject intersection =
						modVersions.setIntersectionCanDestroy(
							reqVersions, false);
					if (intersection.setSize() == 0)
					{
						return
							"version compatibility; module \"" + ref.localName()
							+ "\" guarantees versions " + modVersions
							+ " but current module requires " + reqVersions;
					}
				}

				final AvailObject modNames = modImport.tupleAt(2).setSize() > 0
					? modImport.tupleAt(2)
					: mod.names().keysAsSet();
				for (final AvailObject strName : modNames)
				{
					if (!mod.names().hasKey(strName))
					{
						return
							"module \"" + ref.qualifiedName()
							+ "\" to export " + strName;
					}
					final AvailObject trueNames = mod.names().mapAt(strName);
					for (final AvailObject trueName : trueNames)
					{
						module.atNameAdd(strName, trueName);
					}
				}
			}
			for (final AvailObject modImport : usedModules)
			{
				assert modImport.isTuple();
				assert modImport.tupleSize() == 3;

				final ResolvedModuleName ref = resolver.resolve(
					moduleName.asSibling(
						modImport.tupleAt(1).asNativeString()));
				assert ref != null;
				final AvailObject availRef = StringDescriptor.from(
					ref.qualifiedName());
				if (!runtime.includesModuleNamed(availRef))
				{
					return
						"module \"" + ref.qualifiedName()
						+ "\" to be loaded already";
				}

				final AvailObject mod = runtime.moduleAt(availRef);
				final AvailObject reqVersions = modImport.tupleAt(3);
				if (reqVersions.setSize() > 0)
				{
					final AvailObject modVersions = mod.versions();
					final AvailObject intersection =
						modVersions.setIntersectionCanDestroy(
							reqVersions, false);
					if (intersection.setSize() == 0)
					{
						return
							"version compatibility; module \"" + ref.localName()
							+ "\" guarantees versions " + modVersions
							+ " but current module requires " + reqVersions;
					}
				}

				final AvailObject modNames = modImport.tupleAt(2).setSize() > 0
					? modImport.tupleAt(2)
					: mod.names().keysAsSet();
				for (final AvailObject strName : modNames)
				{
					if (!mod.names().hasKey(strName))
					{
						return
							"module \"" + ref.qualifiedName()
							+ "\" to export " + strName;
					}
					final AvailObject trueNames = mod.names().mapAt(strName);
					for (final AvailObject trueName : trueNames)
					{
						module.atPrivateNameAdd(strName, trueName);
					}
				}
			}

			for (final AvailObject name : exportedNames)
			{
				assert name.isString();
				final AvailObject trueName = AtomDescriptor.create(
					name,
					module);
				module.atNewNamePut(name, trueName);
				module.atNameAdd(name, trueName);
			}

			return null;
		}
	}

	/**
	 * The header information for the current module being parsed.
	 */
	public final ModuleHeader moduleHeader;

	/**
	 * The Avail {@linkplain ModuleDescriptor module} undergoing compilation.
	 */
	AvailObject module = NilDescriptor.nil();

	/**
	 * Answer the {@linkplain ModuleDescriptor module} undergoing compilation by
	 * this {@linkplain AbstractAvailCompiler compiler}.
	 *
	 * @return A module.
	 */
	public AvailObject module ()
	{
		return module;
	}

	/**
	 * The {@linkplain AvailLoader loader} create and operated by this
	 * {@linkplain AbstractAvailCompiler compiler} to facilitate the loading of
	 * {@linkplain ModuleDescriptor modules}.
	 */
	public @Nullable AvailLoader loader;

	/**
	 * Answer the {@linkplain AvailLoader loader} create and operated by this
	 * {@linkplain AbstractAvailCompiler compiler} to facilitate the loading of
	 * {@linkplain ModuleDescriptor modules}.
	 *
	 * @return A loader.
	 */
	public @Nullable AvailLoader loader ()
	{
		return loader;
	}

	/**
	 * The source text of the Avail {@linkplain ModuleDescriptor module}
	 * undergoing compilation.
	 */
	@InnerAccess final String source;

	/**
	 * The complete {@linkplain List list} of {@linkplain TokenDescriptor
	 * tokens} parsed from the source text.
	 */
	final @InnerAccess List<AvailObject> tokens;

	/**
	 * The position of the rightmost {@linkplain TokenDescriptor token} reached
	 * by any parsing attempt.
	 */
	@InnerAccess int greatestGuess;

	/**
	 * The {@linkplain List list} of {@linkplain String} {@linkplain Generator
	 * generators} that describe what was expected (but not found) at the
	 * {@linkplain #greatestGuess rightmost reached position}.
	 */
	@InnerAccess final List<Generator<String>> greatExpectations =
		new ArrayList<Generator<String>>();

	/** The memoization of results of previous parsing attempts. */
	final @InnerAccess AvailCompilerFragmentCache fragmentCache =
		new AvailCompilerFragmentCache();

	/**
	 * Answer whether this is a {@linkplain AvailSystemCompiler system
	 * compiler}.  A system compiler is used for modules that start with the
	 * keyword "{@linkplain ExpectedToken#SYSTEM System}".  Such modules use a
	 * predefined syntax.
	 *
	 * @return Whether this is a system compiler.
	 */
	boolean isSystemCompiler ()
	{
		return false;
	}

	/**
	 * These are the tokens that are understood by the Avail compilers. Most of
	 * these tokens exist to support the {@linkplain AvailSystemCompiler
	 * system compiler}, though a few (related to module headers) are needed
	 * also by the {@linkplain AvailCompiler standard compiler}.
	 */
	public enum ExpectedToken
	{
		/** Module header token. Must be the first token of a system module. */
		SYSTEM("System", KEYWORD),

		/** Module header token: Precedes the name of the defined module. */
		MODULE("Module", KEYWORD),

		/**
		 * Module header token: Precedes the list of versions for which the
		 * defined module guarantees compatibility.
		 */
		VERSIONS("Versions", KEYWORD),

		/** Module header token: Precedes the list of pragma strings. */
		PRAGMA("Pragma", KEYWORD),

		/**
		 * Module header token: Precedes the list of imported modules whose
		 * (filtered) names should be re-exported to clients of the defined
		 * module.
		 */
		EXTENDS("Extends", KEYWORD),

		/**
		 * Module header token: Precedes the list of imported modules whose
		 * (filtered) names are imported only for the private use of the
		 * defined module.
		 */
		USES("Uses", KEYWORD),

		/**
		 * Module header token: Precedes the list of names exported for use by
		 * clients of the defined module.
		 */
		NAMES("Names", KEYWORD),

		/** Module header token: Precedes the contents of the defined module. */
		BODY("Body", KEYWORD),

		/** Leads a primitive binding. */
		PRIMITIVE("Primitive", KEYWORD),

		/** Leads a label. */
		DOLLAR_SIGN("$", OPERATOR),

		/** Leads a reference. */
		UP_ARROW("↑", OPERATOR),

		/** Module header token: Separates tokens. */
		COMMA(",", OPERATOR),

		/** Uses related to declaration and assignment. */
		COLON(":", OPERATOR),

		/** Uses related to declaration and assignment. */
		EQUALS("=", OPERATOR),

		/** Leads a lexical block. */
		OPEN_SQUARE("[", OPERATOR),

		/** Ends a lexical block. */
		CLOSE_SQUARE("]", OPERATOR),

		/** Leads a function body. */
		VERTICAL_BAR("|", OPERATOR),

		/** Leads an exception set. */
		CARET("^", OPERATOR),

		/** Module header token: Uses related to grouping. */
		OPEN_PARENTHESIS("(", OPERATOR),

		/** Module header token: Uses related to grouping. */
		CLOSE_PARENTHESIS(")", OPERATOR),

		/** End of statement. */
		SEMICOLON(";", OPERATOR);

		/** The {@linkplain String Java string} form of the lexeme. */
		private final String lexemeString;

		/**
		 * The {@linkplain StringDescriptor Avail string} form of the
		 * lexeme.
		 */
		private AvailObject lexeme;

		/** The {@linkplain TokenType token type}. */
		private final TokenType tokenType;

		/**
		 * Answer the {@linkplain StringDescriptor lexeme}.
		 *
		 * @return The lexeme.
		 */
		public AvailObject lexeme ()
		{
			assert lexeme != null;
			return lexeme;
		}

		/**
		 * Answer the {@linkplain TokenType token type}.
		 *
		 * @return The token type.
		 */
		TokenType tokenType ()
		{
			return tokenType;
		}

		/**
		 * Construct a new {@link ExpectedToken}.
		 *
		 * @param lexemeString
		 *        The {@linkplain StringDescriptor lexeme string}, i.e. the text
		 *        of the token.
		 * @param tokenType
		 *        The {@linkplain TokenType token type}.
		 */
		ExpectedToken (
			final String lexemeString,
			final TokenType tokenType)
		{
			this.lexemeString = lexemeString;
			this.tokenType = tokenType;
		}

		/**
		 * Release any AvailObjects held statically by this class.
		 */
		public static void clearWellKnownObjects ()
		{
			for (final ExpectedToken value : values())
			{
				value.lexeme = null;
			}
		}

		/**
		 * Create AvailObjects to hold statically by this class.
		 */
		public static void createWellKnownObjects ()
		{
			for (final ExpectedToken value : values())
			{
				assert value.lexeme == null;
				value.lexeme =
					StringDescriptor.from(value.lexemeString).makeShared();
			}
		}
	}

	/**
	 * Asynchronously construct a suitable {@linkplain AbstractAvailCompiler
	 * compiler} to parse the specified {@linkplain ModuleName module name}.
	 *
	 * @param resolvedName
	 *        The {@linkplain ResolvedModuleName resolved name} of the
	 *        {@linkplain ModuleDescriptor module} to compile.
	 * @param stopAfterBodyToken
	 *        Whether to stop parsing at the occurrence of the BODY token. This
	 *        is an optimization for faster build analysis.
	 * @param succeed
	 *        What to do with the resultant compiler in the event of success.
	 *        This is a continuation that accepts the new compiler.
	 * @param fail
	 *        What to do in the event of failure. This is a continuation that
	 *        accepts the {@linkplain Throwable throwable} responsible for
	 *        abnormal termination.
	 * @throws IOException
	 *         If the source module cannot be opened or read.
	 */
	public static void create (
			final ResolvedModuleName resolvedName,
			final boolean stopAfterBodyToken,
			final Continuation1<AbstractAvailCompiler> succeed,
			final Continuation1<Throwable> fail)
		throws IOException
	{
		extractSourceThen(
			resolvedName,
			new Continuation1<String>()
			{
				@Override
				public void value (final @Nullable String sourceText)
				{
					try
					{
						assert sourceText != null;
						final List<AvailObject> tokens = tokenize(
							sourceText,
							stopAfterBodyToken);
						AbstractAvailCompiler compiler;
						if (!tokens.isEmpty()
							&& tokens.get(0).string().equals(SYSTEM.lexeme()))
						{
							compiler = new AvailSystemCompiler(
								resolvedName,
								sourceText,
								tokens);
						}
						else
						{
							compiler = new AvailCompiler(
								resolvedName,
								sourceText,
								tokens);
						}
						succeed.value(compiler);
					}
					catch (final Throwable e)
					{
						fail.value(e);
					}
				}
			},
			fail);
	}

	/**
	 * Construct a new {@link AbstractAvailCompiler} which will use the given
	 * {@link Interpreter} to evaluate expressions.
	 *
	 * @param moduleName
	 *        The {@link ResolvedModuleName resolved name} of the module to
	 *        compile.
	 * @param source
	 *        The source code {@linkplain StringDescriptor string}.
	 * @param tokens
	 *        The list of {@linkplain TokenDescriptor tokens}.
	 */
	public AbstractAvailCompiler (
		final ResolvedModuleName moduleName,
		final String source,
		final List<AvailObject> tokens)
	{
		this.moduleHeader = new ModuleHeader(moduleName);
		this.source = source;
		this.tokens = tokens;
	}

	/**
	 * This is actually a two-argument continuation, but it has only a single
	 * type parameter because the first one is always the {@linkplain
	 * ParserState parser state} that indicates where the continuation should
	 * continue parsing.
	 *
	 * @param <AnswerType>
	 *        The type of the second parameter of the {@linkplain
	 *        Con#value(ParserState, Object)} method.
	 * @author Mark van Gulik &lt;mark@availlang.org&gt;
	 */
	abstract class Con<AnswerType>
	implements Continuation2<ParserState, AnswerType>
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
		Con (final String description)
		{
			this.description = description;
		}

		@Override
		public String toString ()
		{
			return "Con(" + description + ")";
		}

		@Override
		public abstract void value (
			@Nullable ParserState state,
			@Nullable AnswerType answer);
	}


	/**
	 * A {@link Runnable} which supports a natural ordering (via the {@link
	 * Comparable} interface) which will favor processing of the leftmost
	 * available tasks first.
	 *
	 * @author Mark van Gulik &lt;mark@availlang.org&gt;
	 */
	private static class ParsingTask
	extends AvailTask
	{
		/**
		 * The description associated with this task. Only used for debugging.
		 */
		final String description;

		/** The parsing state for this task will operate. */
		final ParserState state;

		/**
		 * Construct a new {@link AbstractAvailCompiler.ParsingTask}.
		 *
		 * @param description What this task will do.
		 * @param state The {@linkplain ParserState parser state} for this task.
		 * @param continuation What to do.
		 */
		public ParsingTask (
			final String description,
			final ParserState state,
			final Continuation0 continuation)
		{
			super(FiberDescriptor.compilerPriority, continuation);
			this.description = description;
			this.state = state;
		}

		@Override
		public String toString()
		{
			return description + "@pos(" + state.position + ")";
		}

		@Override
		public int compareTo (final @Nullable AvailTask o)
		{
			assert o != null;
			final int priorityDelta = priority - o.priority;
			if (priorityDelta != 0)
			{
				return priorityDelta;
			}
			if (o instanceof ParsingTask)
			{
				final ParsingTask task = (ParsingTask) o;
				return state.position - task.state.position;
			}
			return priorityDelta;
		}
	}

	/** The number of work units that have been queued. */
	long workUnitsQueued = 0;

	/** The number of work units that have been completed. */
	long workUnitsCompleted = 0;

	/**
	 * The {@linkplain Throwable throwable} (if any) responsible for an
	 * abnormal termination of compilation.
	 */
	@InnerAccess volatile Throwable terminator;

	/**
	 * Announce that compilation has failed because of the specified {@linkplain
	 * Throwable throwable}. Pending compiler tasks should exit immediately upon
	 * running, and no new compiler tasks should be queued. Notify all
	 * {@linkplain Thread threads} waiting on the {@linkplain
	 * AbstractAvailCompiler receiver}'s monitor.
	 *
	 * <p>
	 * Only the first call of this method has any effect.
	 * </p>
	 *
	 * @param throwable
	 *        The throwable responsible for termination of compilation.
	 */
	synchronized @InnerAccess void compilationFailed (final Throwable throwable)
	{
		if (terminator == null)
		{
			terminator = throwable;
			failureReporter.value();
		}
	}

	/** The output stream on which the serializer writes. */
	public final ByteArrayOutputStream serializerOutputStream =
		new ByteArrayOutputStream(1000);

	/**
	 * The serializer that captures the sequence of bytes representing the
	 * module during compilation.
	 */
	final Serializer serializer = new Serializer(serializerOutputStream);

	/**
	 * What to do when there are no more work units.
	 */
	@InnerAccess volatile Continuation0 noMoreWorkUnits = null;

	/**
	 * Execute {@code #tryBlock}, passing a {@linkplain
	 * AbstractAvailCompiler.Con continuation} that it should run upon finding
	 * exactly one local {@linkplain ParseNodeDescriptor solution}. Report
	 * ambiguity as an error.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param tryBlock
	 *        What to try. This is a continuation that accepts a continuation
	 *        that tracks completion of parsing.
	 * @param supplyAnswer
	 *        What to do if exactly one result was produced. This is a
	 *        continuation that accepts a solution.
	 */
	void tryIfUnambiguousThen (
		final ParserState start,
		final Con<Con<AvailObject>> tryBlock,
		final Con<AvailObject> supplyAnswer)
	{
		assert noMoreWorkUnits == null;
		// Augment the start position with a variant that incorporates the
		// solution-accepting continuation.
		final Mutable<Integer> count = new Mutable<Integer>(0);
		final Mutable<AvailObject> solution = new Mutable<AvailObject>();
		final Mutable<ParserState> afterStatement = new Mutable<ParserState>();
		noMoreWorkUnits = new Continuation0()
		{
			@Override
			public void value ()
			{
				synchronized (AbstractAvailCompiler.this)
				{
					assert workUnitsQueued == workUnitsCompleted;
				}
				// Ambiguity is detected and prevented during the parse, and
				// should never be identified here.
				assert count.value < 2;
				// If no solutions were found, then report an error.
				if (count.value == 0)
				{
					reportError();
					assert false;
				}
				// If a simple unambiguous solution was found, then answer
				// it forward to the continuation.
				if (count.value == 1)
				{
					assert solution.value != null;
					supplyAnswer.value(
						afterStatement.value, solution.value);
				}
			}
		};
		final ParserState realStart = new ParserState(
			start.position,
			start.scopeMap,
			start.innermostBlockArguments);
		attempt(
			realStart,
			tryBlock,
			new Con<AvailObject>("Record solution")
			{
				@Override
				public void value (
					final @Nullable ParserState afterSolution,
					final @Nullable AvailObject aSolution)
				{
					assert afterSolution != null;
					assert aSolution != null;
					if (count.value == 0)
					{
						afterStatement.value = afterSolution;
						solution.value = aSolution;
						count.value++;
					}
					else
					{
						// Indicate the problem on the last token of the
						// ambiguous expression.
						reportAmbiguousInterpretations(
							new ParserState(
								afterSolution.position - 1,
								afterSolution.scopeMap,
								afterSolution.innermostBlockArguments),
							solution.value,
							aSolution);
						assert false;
					}
				}
			});
	}

	/**
	 * {@link ParserState} instances are immutable and keep track of a current
	 * {@link #position} and {@link #scopeMap} during parsing.
	 *
	 * @author Mark van Gulik &lt;mark@availlang.org&gt;
	 */
	public class ParserState
	{
		/**
		 * The position represented by this {@link ParserState}. In particular,
		 * it's the (zero-based) index of the current token.
		 */
		final int position;

		/**
		 * A {@linkplain MapDescriptor map} from each name that is in scope to
		 * the {@linkplain DeclarationNodeDescriptor declaration} with that
		 * name. The names are {@linkplain StringDescriptor strings}.
		 *
		 * <p>
		 * The map should be immutable, as each new declaration must create a
		 * fresh map. Backtracking (or closing a block scope) causes some
		 * previous version of the map to be used, so its state must never be
		 * destroyed. Note that this is especially important since the parser
		 * is continuation-passing, potentially breadth-first, and thread-safe.
		 * </p>
		 */
		public final AvailObject scopeMap;

		/**
		 * The tuple of argument declarations of the innermost block being
		 * parsed. This gets populated by an occurrence of the {@link
		 * MessageSplitter.ArgumentsCheckpoint} parsing instruction, which is
		 * specified by a {@linkplain StringDescriptor#sectionSign() section
		 * sign} ("§") outside any repeating/variable-occurrence
		 * parsing groups and structures.
		 */
		public final AvailObject innermostBlockArguments;

		/**
		 * Construct a new immutable {@link ParserState}.
		 *
		 * @param position
		 *        The index of the current token.
		 * @param scopeMap
		 *        The {@link MapDescriptor map} of bindings.
		 * @param innermostBlockArguments
		 *        The {@link TupleDescriptor tuple} of arguments to be
		 *        checkpointed in this parser state.
		 */
		ParserState (
			final int position,
			final AvailObject scopeMap,
			final AvailObject innermostBlockArguments)
		{
			assert scopeMap != null;

			this.position = position;
			this.scopeMap = scopeMap.makeShared();
			this.innermostBlockArguments = innermostBlockArguments.makeShared();
		}

		@Override
		public int hashCode ()
		{
			return position * 473897843 ^ scopeMap.hashCode();
		}

		@Override
		public boolean equals (final @Nullable Object another)
		{
			if (!(another instanceof ParserState))
			{
				return false;
			}
			final ParserState anotherState = (ParserState) another;
			return position == anotherState.position
				&& scopeMap.equals(anotherState.scopeMap)
				&& innermostBlockArguments.equals(
					anotherState.innermostBlockArguments);
		}

		@Override
		public String toString ()
		{
			return String.format(
				"%s%n\tPOSITION = %d%n%s\tSCOPE_STACK = %s",
				getClass().getSimpleName(),
				position,
				innermostBlockArguments.equals(NilDescriptor.nil())
					? ""
					: ("\tINNERMOST_BLOCK_ARGUMENTS = "
						+ innermostBlockArguments),
				scopeMap);
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
		 * Answer the {@linkplain TokenDescriptor token} at the current
		 * position.
		 *
		 * @return The token.
		 */
		AvailObject peekToken ()
		{
			return tokens.get(position);
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param expectedToken
		 *        The {@linkplain ExpectedToken expected token} to look for.
		 * @return Whether the specified token was found.
		 */
		boolean peekToken (final ExpectedToken expectedToken)
		{
			if (atEnd())
			{
				return false;
			}
			final AvailObject token = peekToken();
			return token.tokenType() == expectedToken.tokenType()
				&& token.string().equals(expectedToken.lexeme());
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param expectedToken
		 *        The {@linkplain ExpectedToken expected token} to look for.
		 * @param expected
		 *        A {@linkplain Generator generator} of a message to record if
		 *        the specified token is not present.
		 * @return Whether the specified token is present.
		 */
		boolean peekToken (
			final ExpectedToken expectedToken,
			final Generator<String> expected)
		{
			final AvailObject token = peekToken();
			final boolean found = token.tokenType() == expectedToken.tokenType()
					&& token.string().equals(expectedToken.lexeme());
			if (!found)
			{
				expected(expected);
			}
			return found;
		}

		/**
		 * Answer whether the current token has the specified type and content.
		 *
		 * @param expectedToken
		 *        The {@linkplain ExpectedToken expected token} to look for.
		 * @param expected
		 *        A message to record if the specified token is not present.
		 * @return Whether the specified token is present.
		 */
		boolean peekToken (
			final ExpectedToken expectedToken,
			final String expected)
		{
			return peekToken(expectedToken, generate(expected));
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
			return new ParserState(
				position + 1,
				scopeMap,
				innermostBlockArguments);
		}

		/**
		 * Parse a string literal. Answer the {@linkplain LiteralTokenDescriptor
		 * string literal token} if found, otherwise answer {@code null}.
		 *
		 * @return The actual {@linkplain LiteralTokenDescriptor literal token}
		 *         or {@code null}.
		 */
		@Nullable AvailObject peekStringLiteral ()
		{
			final AvailObject token = peekToken();
			if (token.isInstanceOfKind(
				LiteralTokenTypeDescriptor.create(
					TupleTypeDescriptor.stringTupleType())))
			{
				return token;
			}
			return null;
		}

		/**
		 * Return a new {@linkplain ParserState parser state} like this one, but
		 * with the given declaration added.
		 *
		 * @param declaration
		 *        The {@linkplain DeclarationNodeDescriptor declaration} to add
		 *        to the map of visible bindings.
		 * @return The new parser state including the declaration.
		 */
		ParserState withDeclaration (final AvailObject declaration)
		{
			final AvailObject name = declaration.token().string();
			assert !scopeMap.hasKey(name);
			return new ParserState(
				position,
				scopeMap.mapAtPuttingCanDestroy(
					name,
					declaration,
					false),
				innermostBlockArguments);
		}

		/**
		 * Record an expectation at the current parse position. The expectations
		 * captured at the rightmost parse position constitute the error message
		 * in case the parse fails.
		 *
		 * <p>
		 * The expectation is a {@linkplain Generator Generator<String>}, in
		 * case constructing a {@link String} would be prohibitive. There is
		 * also {@link #expected(String) another} version of this method that
		 * accepts a String directly.
		 * </p>
		 *
		 * @param stringGenerator
		 *        The {@code Generator<String>} to capture.
		 */
		void expected (final Generator<String> stringGenerator)
		{
			synchronized (AbstractAvailCompiler.this)
			{
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
		}

		/**
		 * Record an indication of what was expected at this parse position.
		 *
		 * @param aString
		 *        The string to look up.
		 */
		void expected (final String aString)
		{
			expected(generate(aString));
		}

		/**
		 * Return the {@linkplain ModuleDescriptor module} under compilation for
		 * this {@linkplain ParserState}.
		 *
		 * @return The current module being compiled.
		 */
		public AvailObject currentModule ()
		{
			return AbstractAvailCompiler.this.module;
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
	 * {@linkplain StringDescriptor actual Avail strings}.
	 * </p>
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param stringTokens
	 *        The initially empty list of strings to populate.
	 * @return The parser state after the list of strings, or {@code null} if
	 *         the list of strings is malformed.
	 */
	private static @Nullable ParserState parseStringLiterals (
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
		while (state.peekToken(COMMA))
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
	 * Parse one or more {@linkplain ModuleDescriptor module} imports separated
	 * by commas. This parse isn't backtracking like the rest of the grammar -
	 * it's greedy. It considers any parse error to be unrecoverable (not a
	 * backtrack).
	 *
	 * <p>
	 * Return the {@link ParserState} after the imports if successful, otherwise
	 * {@code null}. Populate the passed {@linkplain List list} with {@linkplain
	 * TupleDescriptor 2-tuples}. Each tuple's first element is a module
	 * {@linkplain StringDescriptor name} and second element is the
	 * collection of {@linkplain MethodDefinitionDescriptor method} names to
	 * import.
	 * </p>
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param imports
	 *        The initially empty list of imports to populate.
	 * @return The parser state after the list of imports, or {@code null} if
	 *         the list of imports is malformed.
	 * @author Todd L Smith &lt;todd@availlang.org&gt;
	 */
	private static @Nullable ParserState parseImports (
		final ParserState start,
		final List<AvailObject> imports)
	{
		assert imports.isEmpty();

		ParserState state = start;
		do
		{
			final AvailObject token = state.peekStringLiteral();
			if (token == null)
			{
				state.expected("another module name after comma");
				return imports.isEmpty() ? state : null;
			}

			final AvailObject moduleName = token.literal();
			state = state.afterToken();

			final List<AvailObject> versions = new ArrayList<AvailObject>();
			if (state.peekToken(OPEN_PARENTHESIS))
			{
				state = state.afterToken();
				state = parseStringLiterals(state, versions);
				if (state == null)
				{
					return null;
				}
				if (!state.peekToken(
					CLOSE_PARENTHESIS,
					"a close parenthesis following acceptable versions"))
				{
					return null;
				}
				state = state.afterToken();
			}

			final List<AvailObject> names = new ArrayList<AvailObject>();
			if (state.peekToken(EQUALS))
			{
				state = state.afterToken();
				if (!state.peekToken(
					OPEN_PARENTHESIS,
					"an open parenthesis preceding import list"))
				{
					return null;
				}
				state = state.afterToken();
				state = parseStringLiterals(state, names);
				if (state == null)
				{
					return null;
				}
				if (!state.peekToken(
					CLOSE_PARENTHESIS,
					"a close parenthesis following import list"))
				{
					return null;
				}
				state = state.afterToken();
			}

			imports.add(TupleDescriptor.from(
				moduleName,
				TupleDescriptor.fromList(names).asSet(),
				TupleDescriptor.fromList(versions).asSet()));
			if (state.peekToken(COMMA))
			{
				state = state.afterToken();
			}
			else
			{
				return state;
			}
		}
		while (true);
	}

	/**
	 * Read the source string for the {@linkplain ModuleDescriptor module}
	 * specified by the fully-qualified {@linkplain ModuleName module name}.
	 *
	 * @param resolvedName
	 *        The {@linkplain ResolvedModuleName resolved name} of the module.
	 * @param continuation
	 *        What to do after the source module has been completely read.
	 *        Accepts the source text of the module.
	 * @param fail
	 *        What to do in the event of failure. This is a continuation that
	 *        accepts the {@linkplain Throwable throwable} responsible for
	 *        abnormal termination.
	 * @throws IOException
	 *         If the source module could not be opened or read for any reason.
	 */
	private static void extractSourceThen (
			final ResolvedModuleName resolvedName,
			final Continuation1<String> continuation,
			final Continuation1<Throwable> fail)
		throws IOException
	{
		final AvailRuntime runtime = AvailRuntime.current();
		final File ref = resolvedName.fileReference();
		final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		final ByteBuffer input = ByteBuffer.allocateDirect(4096);
		final CharBuffer output = CharBuffer.allocate(4096);
		final StringBuilder sourceBuilder = new StringBuilder(4096);
		final Mutable<Long> filePosition = new Mutable<Long>(0L);
		final AsynchronousFileChannel file;
		try
		{
			file = runtime.openFile(ref.toPath(), StandardOpenOption.READ);
		}
		catch (final IOException e)
		{
			fail.value(e);
			return;
		}
		final Mutable<CompletionHandler<Integer, Void>> handler =
			new Mutable<CompletionHandler<Integer,Void>>(null);
		handler.value =
			new CompletionHandler<Integer, Void>()
			{
				@Override
				public void completed (
					@Nullable final Integer bytesRead,
					@Nullable final Void nothing)
				{
					try
					{
						assert bytesRead != null;
						boolean moreInput = true;
						if (bytesRead == -1)
						{
							moreInput = false;
						}
						else
						{
							filePosition.value += bytesRead;
						}
						input.flip();
						final CoderResult result = decoder.decode(
							input, output, !moreInput);
						// If the decoder didn't consume all of the bytes, then
						// preserve the unconsumed bytes in the next buffer (for
						// decoding).
						if (input.hasRemaining())
						{
							final int delta = input.limit() - input.position();
							for (int i = 0; i < delta; i++)
							{
								final byte b = input.get(input.position() + i);
								input.put(i, b);
							}
							input.limit(input.capacity());
							input.position(delta);
						}
						else
						{
							input.clear();
						}
						// UTF-8 never compresses data, so the number of
						// characters encoded can be no greater than the number
						// of bytes encoded. The input buffer and the output
						// buffer are equally sized (in units), so an overflow
						// cannot occur.
						assert !result.isOverflow();
						if (result.isError())
						{
							result.throwException();
						}
						output.flip();
						sourceBuilder.append(output.toString());
						// If more input remains, then queue another read.
						if (moreInput)
						{
							output.clear();
							file.read(
								input,
								filePosition.value,
								null,
								handler.value);
						}
						// Otherwise, close the file channel and queue the
						// original continuation.
						else
						{
							decoder.flush(output);
							sourceBuilder.append(output.toString());
							file.close();
							runtime.execute(
								new AvailTask(
									FiberDescriptor.compilerPriority,
									new Continuation0()
									{
										@Override
										public void value ()
										{
											continuation.value(
												sourceBuilder.toString());
										}
									}));
						}
					}
					catch (final Throwable e)
					{
						fail.value(e);
					}
				}

				@Override
				public void failed (
					@Nullable final Throwable throwable,
					@Nullable final Void attachment)
				{
					fail.value(throwable);
				}
			};
		// Kick off the asynchronous read.
		file.read(input, 0L, null, handler.value);
	}

	/**
	 * Tokenize the {@linkplain ModuleDescriptor module} specified by the
	 * fully-qualified {@linkplain ModuleName module name}.
	 *
	 * @param source
	 *        The {@linkplain String string} containing the module's source
	 *        code.
	 * @param stopAfterBodyToken
	 *        Stop scanning after encountering the BODY token?
	 * @return The {@linkplain ResolvedModuleName resolved module name}.
	 * @throws AvailCompilerException
	 *         If tokenization failed for any reason.
	 */
	static List<AvailObject> tokenize (
			final String source,
			final boolean stopAfterBodyToken)
		throws AvailCompilerException
	{
		return new AvailScanner().scanString(source, stopAfterBodyToken);
	}

	/**
	 * Map the tree through the (destructive) transformation specified by
	 * aBlock, children before parents. The block takes three arguments: the
	 * node, its parent, and the list of enclosing block nodes. Answer the
	 * resulting tree.
	 *
	 * @param object
	 *        The current {@linkplain ParseNodeDescriptor parse node}.
	 * @param aBlock
	 *        What to do with each descendant.
	 * @param parentNode
	 *        This node's parent.
	 * @param outerNodes
	 *        The list of {@linkplain BlockNodeDescriptor blocks} surrounding
	 *        this node, from outermost to innermost.
	 * @param nodeMap
	 *        The {@link Map} from old {@linkplain ParseNodeDescriptor parse
	 *        nodes} to newly copied, mutable parse nodes.  This should ensure
	 *        the consistency of declaration references.
	 * @return A replacement for this node, possibly this node itself.
	 */
	static AvailObject treeMapWithParent (
		final AvailObject object,
		final Transformer3<
				AvailObject,
				AvailObject,
				List<AvailObject>,
				AvailObject>
			aBlock,
		final AvailObject parentNode,
		final List<AvailObject> outerNodes,
		final Map<AvailObject, AvailObject> nodeMap)
	{
		if (nodeMap.containsKey(object))
		{
			return object;
		}
		final AvailObject objectCopy = object.copyMutableParseNode();
		objectCopy.childrenMap(
			new Transformer1<AvailObject, AvailObject>()
			{
				@Override
				public AvailObject value (final @Nullable AvailObject child)
				{
					assert child != null;
					assert child.isInstanceOfKind(PARSE_NODE.mostGeneralType());
					return treeMapWithParent(
						child,
						aBlock,
						objectCopy,
						outerNodes,
						nodeMap);
				}
			});
		final AvailObject transformed = aBlock.value(
			objectCopy,
			parentNode,
			outerNodes);
		assert transformed != null;
		transformed.makeShared();
		nodeMap.put(object, transformed);
		return transformed;
	}

	/**
	 * Answer a {@linkplain Generator generator} that will produce the given
	 * string.
	 *
	 * @param string
	 *        The exact string to generate.
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
	 * Report an error by throwing an {@link AvailCompilerException}. The
	 * exception encapsulates the {@linkplain ResolvedModuleName module name} of
	 * the {@linkplain ModuleDescriptor module} undergoing compilation, the
	 * error string, and the text position. This position is the rightmost
	 * position encountered during the parse, and the error strings in {@link
	 * #greatExpectations} are the things that were expected but not found at
	 * that position. This seems to work very well in practice.
	 *
	 * @throws AvailCompilerException
	 *        Always thrown.
	 */
	void reportError () throws AvailCompilerException
	{
		final AvailObject token;
		final List<Generator<String>> expectations;
		synchronized (this)
		{
			token = tokens.get(greatestGuess);
			expectations = new ArrayList<Generator<String>>(greatExpectations);
		}
		reportError(token, "Expected...", expectations);
		assert false;
	}

	/** A bunch of dash characters, wide enough to catch the eye. */
	static final String rowOfDashes;

	static
	{
		final char[] chars = new char[70];
		Arrays.fill(chars, '-');
		rowOfDashes = new String(chars);
	}

	/**
	 * Report an error by throwing an {@link AvailCompilerException}. The
	 * exception encapsulates the {@linkplain ResolvedModuleName module name} of
	 * the {@linkplain ModuleDescriptor module} undergoing compilation, the
	 * error string, and the text position. This position is the rightmost
	 * position encountered during the parse, and the error strings in
	 * {@link #greatExpectations} are the things that were expected but not
	 * found at that position. This seems to work very well in practice.
	 *
	 * @param token
	 *        Where the error occurred.
	 * @param banner
	 *        The string that introduces the problem text.
	 * @param problems
	 *        A list of {@linkplain Generator generators} that may be
	 *        invoked to produce problem strings.
	 * @throws AvailCompilerException
	 *         Always thrown.
	 */
	void reportError (
			final AvailObject token,
			final String banner,
			final List<Generator<String>> problems)
		throws AvailCompilerException
	{
		assert problems.size() > 0 : "Bug - empty problem list";
		final long charPos = token.start();
		final String sourceUpToError = source.substring(0, (int) charPos);
		final int startOfPreviousLine = sourceUpToError.lastIndexOf('\n') + 1;
		final Formatter text = new Formatter();
		text.format("%n");
		int wedges = 3;
		for (int i = startOfPreviousLine; i < charPos; i++)
		{
			if (source.codePointAt(i) == '\t')
			{
				while (wedges > 0)
				{
					text.format(">");
					wedges--;
				}
				text.format("\t");
			}
			else
			{
				if (wedges > 0)
				{
					text.format(">");
					wedges--;
				}
				else
				{
					text.format(" ");
				}
			}
		}
		text.format("^-- %s", banner);
		text.format("%n>>>%s", rowOfDashes);
		final Set<String> alreadySeen = new HashSet<String>(problems.size());
		for (final Generator<String> generator : problems)
		{
			final String str = generator.value();
			if (!alreadySeen.contains(str))
			{
				alreadySeen.add(str);
				text.format("\n>>>\t%s", str.replace("\n", "\n>>>\t"));
			}
		}
		text.format(
			"%n(file=\"%s\", line=%d)",
			moduleHeader.moduleName.qualifiedName(),
			token.lineNumber());
		text.format("%n>>>%s", rowOfDashes);
		int endOfLine = source.indexOf('\n', (int) charPos);
		if (endOfLine == -1)
		{
			endOfLine = source.length();
		}
		final String textString = text.toString();
		text.close();
		throw new AvailCompilerException(
			moduleHeader.moduleName,
			charPos,
			endOfLine,
			textString);
	}

	/**
	 * A statement was parsed correctly in two different ways. There may be more
	 * ways, but we stop after two as it's already an error. Report the error.
	 *
	 * @param where
	 *        Where the expressions were parsed from.
	 * @param interpretation1
	 *        The first interpretation as a {@linkplain ParseNodeDescriptor
	 *        parse node}.
	 * @param interpretation2
	 *        The second interpretation as a {@linkplain ParseNodeDescriptor
	 *        parse node}.
	 */
	@InnerAccess void reportAmbiguousInterpretations (
		final ParserState where,
		final AvailObject interpretation1,
		final AvailObject interpretation2)
	{
		final Mutable<AvailObject> node1 =
			new Mutable<AvailObject>(interpretation1);
		final Mutable<AvailObject> node2 =
			new Mutable<AvailObject>(interpretation2);
		findParseTreeDiscriminants(node1, node2);
		where.expected(
			new Generator<String>()
			{
				@Override
				public String value ()
				{
					final StringBuilder builder = new StringBuilder(200);
					builder.append("unambiguous interpretation.  ");
					builder.append("Here are two possible parsings...\n");
					builder.append("\t");
					builder.append(node1.value.toString());
					builder.append("\n\t");
					builder.append(node2.value.toString());
					return builder.toString();
				}
			});
		reportError();
		assert false;
	}

	/**
	 * Given two unequal parse trees, find the smallest descendant nodes that
	 * still contain all the differences.  The given {@link Mutable} objects
	 * initially contain references to the root nodes, but are updated to refer
	 * to the most specific pair of nodes that contain all the differences.
	 *
	 * @param node1
	 *            A {@code Mutable} reference to a {@linkplain
	 *            ParseNodeDescriptor parse tree}.  Updated to hold the most
	 *            specific difference.
	 * @param node2
	 *            The {@code Mutable} reference to the other parse tree.
	 *            Updated to hold the most specific difference.
	 */
	private void findParseTreeDiscriminants (
		final Mutable<AvailObject> node1,
		final Mutable<AvailObject> node2)
	{
		while (true)
		{
			assert !node1.value.equals(node2.value);
			if (!node1.value.kind().parseNodeKind().equals(
				node2.value.kind().parseNodeKind()))
			{
				// The nodes are different kinds, so present them as what's
				// different.
				return;
			}
			if (node1.value.kind().parseNodeKindIsUnder(SEND_NODE)
				&& node1.value.method() != node2.value.method())
			{
				// They're sends of different messages, so don't go any deeper.
				return;
			}
			final List<AvailObject> parts1 = new ArrayList<AvailObject>();
			node1.value.childrenDo(new Continuation1<AvailObject>()
			{
				@Override
				public void value (final @Nullable AvailObject part)
				{
					parts1.add(part);
				}
			});
			final List<AvailObject> parts2 = new ArrayList<AvailObject>();
			node2.value.childrenDo(new Continuation1<AvailObject>()
				{
					@Override
					public void value (final @Nullable AvailObject part)
					{
						parts2.add(part);
					}
				});
			if (parts1.size() != parts2.size())
			{
				// Different structure at this level.
				return;
			}
			final List<Integer> differentIndices =
				new ArrayList<Integer>();
			for (int i = 0; i < parts1.size(); i++)
			{
				if (!parts1.get(i).equals(parts2.get(i)))
				{
					differentIndices.add(i);
				}
			}
			if (differentIndices.size() != 1)
			{
				// More than one part differs, so we can't drill deeper.
				return;
			}
			// Drill into the only part that differs.
			node1.value = parts1.get(differentIndices.get(0));
			node2.value = parts2.get(differentIndices.get(0));
		}
	}

	/**
	 * Attempt the zero-argument continuation. The implementation is free to
	 * execute it now or to put it in a bag of continuations to run later <em>in
	 * an arbitrary order</em>. There may be performance and/or scale benefits
	 * to processing entries in FIFO, LIFO, or some hybrid order, but the
	 * correctness is not affected by a choice of order.
	 *
	 * @param continuation
	 *        What to do at some point in the future.
	 */
	void eventuallyDo (final Continuation0 continuation)
	{
		runtime.execute(new AvailTask(
			FiberDescriptor.compilerPriority,
			new Continuation0()
			{
				@Override
				public void value ()
				{
					try
					{
						continuation.value();
					}
					catch (final Throwable e)
					{
						compilationFailed(e);
					}
				}
			}));
	}

	/**
	 * Start a work unit.
	 */
	protected synchronized void startWorkUnit ()
	{
		if (terminator != null)
		{
			// Don't add any new tasks if canceling.
			return;
		}
		workUnitsQueued++;
	}

	/**
	 * Construct and answer a {@linkplain Continuation1 continuation} that
	 * wraps the specified continuation in logic that will increment the
	 * {@linkplain #workUnitsCompleted count of completed work units} and
	 * potentially call the {@linkplain #noMoreWorkUnits unambiguous
	 * statement}.
	 *
	 * @param state
	 *        A parser state with an unambiguous statement continuation.
	 * @param continuation
	 *        What to do as a work unit.
	 * @return A new continuation. It accepts an argument of some kind, which
	 *         will be passed forward to the argument continuation.
	 */
	protected <ArgType> Continuation1<ArgType> workUnitCompletion (
		final ParserState state,
		final Continuation1<ArgType> continuation)
	{
		assert noMoreWorkUnits != null;
		return new Continuation1<ArgType>()
		{
			@Override
			public void value (final @Nullable ArgType value)
			{
				boolean quiescent = false;
				try
				{
					// Don't actually run tasks if canceling.
					if (terminator == null)
					{
						continuation.value(value);
					}
				}
				catch (final Throwable e)
				{
					compilationFailed(e);
					return;
				}
				finally
				{
					synchronized (AbstractAvailCompiler.this)
					{
						workUnitsCompleted++;
						if (workUnitsCompleted == workUnitsQueued)
						{
							quiescent = true;
						}
					}
				}
				try
				{
					if (quiescent)
					{
						final Continuation0 noMore = noMoreWorkUnits;
						noMoreWorkUnits = null;
						noMore.value();
					}
				}
				catch (final Throwable e)
				{
					compilationFailed(e);
				}
			}
		};
	}

	/**
	 * Eventually execute the specified {@linkplain Continuation0 continuation}
	 * as a {@linkplain AbstractAvailCompiler compiler} work unit.
	 *
	 * @param continuation
	 *        What to do at some point in the future.
	 * @param description
	 *        Debugging information about what is to be parsed.
	 * @param where
	 *        Where the parse is happening.
	 */
	void workUnitDo (
		final Continuation0 continuation,
		final String description,
		final ParserState where)
	{
		startWorkUnit();
		final Continuation1<AvailObject> workUnit = workUnitCompletion(
			where,
			new Continuation1<AvailObject>()
			{
				@Override
				public void value (final @Nullable AvailObject ignored)
				{
					continuation.value();
				}
			});
		runtime.execute(new ParsingTask(
			description,
			where,
			new Continuation0()
			{
				@Override
				public void value ()
				{
					workUnit.value(null);
				}
			}));
	}

	/**
	 * Wrap the {@linkplain Continuation1 continuation of one argument} inside a
	 * {@linkplain Continuation0 continuation of zero arguments} and record that
	 * as per {@linkplain #workUnitDo(Continuation0, String, ParserState)}.
	 *
	 * @param <ArgType>
	 *        The type of argument to the given continuation.
	 * @param here
	 *        Where to start parsing when the continuation runs.
	 * @param continuation
	 *        What to execute with the passed argument.
	 * @param argument
	 *        What to pass as an argument to the provided {linkplain
	 *        Continuation1 one-argument continuation}.
	 */
	<ArgType> void attempt (
		final ParserState here,
		final Con<ArgType> continuation,
		final ArgType argument)
	{
		workUnitDo(
			new Continuation0()
			{
				@Override
				public void value ()
				{
					continuation.value(here, argument);
				}
			},
			continuation.description,
			here);
	}

	/**
	 * Look up a local declaration that has the given name, or null if not
	 * found.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param name
	 *        The name of the variable declaration for which to look.
	 * @return The declaration or {@code null}.
	 */
	@Nullable AvailObject lookupLocalDeclaration (
		final ParserState start,
		final AvailObject name)
	{
		if (start.scopeMap.hasKey(name))
		{
			return start.scopeMap.mapAt(name);
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
	void startModuleTransaction ()
	{
		assert module.equalsNil();
		module = ModuleDescriptor.newModule(
			StringDescriptor.from(moduleHeader.moduleName.qualifiedName()));
		module.isSystemModule(isSystemCompiler());
		loader = new AvailLoader(module);
	}

	/**
	 * Rollback the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction()}.
	 */
	void rollbackModuleTransaction ()
	{
		assert module != null;
		module.removeFrom(loader);
		module = NilDescriptor.nil();
	}

	/**
	 * Commit the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction()}.
	 */
	void commitModuleTransaction ()
	{
		assert module != null;
		runtime.addModule(module);
		module.cleanUpAfterCompile();
	}

	/**
	 * Convert a {@link ParseNodeDescriptor parse node} into a zero-argument
	 * {@link FunctionDescriptor function}.
	 *
	 * @param expressionNode The parse tree to compile to a function.
	 * @param lineNumber The line number to attach to the new function.
	 * @return A zero-argument function.
	 */
	AvailObject createFunctionToRun (
		final AvailObject expressionNode,
		final int lineNumber)
	{
		final AvailObject block = BlockNodeDescriptor.newBlockNode(
			Collections.<AvailObject>emptyList(),
			(short) 0,
			Collections.singletonList(expressionNode),
			TOP.o(),
			SetDescriptor.empty(),
			lineNumber);
		BlockNodeDescriptor.recursivelyValidate(block);
		final AvailCodeGenerator codeGenerator = new AvailCodeGenerator(
			module);
		final AvailObject compiledBlock = block.generate(codeGenerator);
		// The block is guaranteed context-free (because imported
		// variables/values are embedded directly as constants in the generated
		// code), so build a function with no copied data.
		assert compiledBlock.numOuters() == 0;
		final AvailObject function = FunctionDescriptor.create(
			compiledBlock,
			TupleDescriptor.empty());
		function.makeImmutable();
		return function;
	}

	/**
	 * Evaluate the specified {@linkplain FunctionDescriptor function} in the
	 * module's context; lexically enclosing variables are not considered in
	 * scope, but module variables and constants are in scope.
	 *
	 * @param function
	 *        A function.
	 * @param args
	 *        The arguments to the function.
	 * @param shouldSerialize
	 *        {@code true} if the generated function should be serialized,
	 *        {@code false} otherwise.
	 * @param onSuccess
	 *        What to do with the result of the evaluation.
	 * @param onFailure
	 *        What to do with a terminal {@linkplain Throwable throwable}.
	 */
	protected void evaluateFunctionThen (
		final AvailObject function,
		final List<AvailObject> args,
		final boolean shouldSerialize,
		final Continuation1<AvailObject> onSuccess,
		final Continuation1<Throwable> onFailure)
	{
		synchronized (this)
		{
			if (shouldSerialize)
			{
				serializer.serialize(function);
			}
		}
		final AvailObject fiber = FiberDescriptor.newLoaderFiber(loader);
		fiber.resultContinuation(onSuccess);
		fiber.failureContinuation(onFailure);
		Interpreter.runOutermostFunction(fiber, function, args);
	}

	/**
	 * Generate a {@linkplain FunctionDescriptor function} from the specified
	 * {@linkplain ParseNodeDescriptor phrase} and evaluate it in the module's
	 * context; lexically enclosing variables are not considered in scope, but
	 * module variables and constants are in scope.
	 *
	 * @param expressionNode
	 *        A {@linkplain ParseNodeDescriptor parse node}.
	 * @param lineNumber
	 *        The line number on which the expression starts.
	 * @param shouldSerialize
	 *        {@code true} if the generated function should be serialized,
	 *        {@code false} otherwise.
	 * @param onSuccess
	 *        What to do with the result of the evaluation.
	 * @param onFailure
	 *        What to do with a terminal {@linkplain Throwable throwable}.
	 */
	protected void evaluatePhraseThen (
		final AvailObject expressionNode,
		final int lineNumber,
		final boolean shouldSerialize,
		final Continuation1<AvailObject> onSuccess,
		final Continuation1<Throwable> onFailure)
	{
		evaluateFunctionThen(
			createFunctionToRun(expressionNode, lineNumber),
			Collections.<AvailObject>emptyList(),
			shouldSerialize,
			onSuccess,
			onFailure);
	}

	/**
	 * Evaluate a parse tree node. It's a top-level statement in a module.
	 * Declarations are handled differently - they cause a variable to be
	 * declared in the module's scope.
	 *
	 * @param expressionOrMacro
	 *        The expression to compile and evaluate as a top-level statement in
	 *        the module.
	 * @param onSuccess
	 *        What to do after success. Note that the result of executing the
	 *        statement must be {@linkplain NilDescriptor#nil() nil}, so there
	 *        is no point accepting in the result. Hence the {@linkplain
	 *        Continuation0 nullary continuation}.
	 * @param onFailure
	 *        What to do with a terminal {@linkplain Throwable throwable}.
	 */
	void evaluateModuleStatementThen (
		final AvailObject expressionOrMacro,
		final Continuation0 onSuccess,
		final Continuation1<Throwable> onFailure)
	{
		final AvailObject expression = expressionOrMacro.stripMacro();
		if (!expression.isInstanceOfKind(DECLARATION_NODE.mostGeneralType()))
		{
			// Only record module statements that aren't declarations. Users of
			// the module don't care if a module variable or constant is only
			// reachable from the module's methods.
			evaluatePhraseThen(
				expression,
				0,
				true,
				new Continuation1<AvailObject>()
				{
					@Override
					public void value (final @Nullable AvailObject ignored)
					{
						onSuccess.value();
					}
				},
				onFailure);
			return;
		}
		// It's a declaration, but the parser couldn't previously tell that it
		// was at module scope.
		final AvailObject name = expression.token().string();
		switch (expression.declarationKind())
		{
			case LOCAL_CONSTANT:
			{
				evaluatePhraseThen(
					expression.initializationExpression(),
					expression.token().lineNumber(),
					false,
					new Continuation1<AvailObject>()
					{
						@Override
						public void value (final @Nullable AvailObject val)
						{
							assert val != null;
							final AvailObject var =
								VariableDescriptor.forInnerType(
									AbstractEnumerationTypeDescriptor
										.withInstance(val));
							module.addConstantBinding(name, var);
							// Create a module variable declaration (i.e.,
							// cheat) JUST for this initializing assignment.
							final AvailObject decl =
								DeclarationNodeDescriptor.newModuleVariable(
									expression.token(),
									var,
									expression.initializationExpression());
							final AvailObject assign =
								AssignmentNodeDescriptor.from(
									VariableUseNodeDescriptor.newUse(
										expression.token(), decl),
									LiteralNodeDescriptor.syntheticFrom(val),
									false);
							final AvailObject function = createFunctionToRun(
								assign,
								expression.token().lineNumber());
							synchronized (AbstractAvailCompiler.this)
							{
								serializer.serialize(function);
							}
							var.setValue(val);
							onSuccess.value();
						}
					},
					onFailure);
				break;
			}
			case LOCAL_VARIABLE:
			{
				final AvailObject var = VariableDescriptor.forInnerType(
					expression.declaredType());
				module.addVariableBinding(name, var);
				if (!expression.initializationExpression().equalsNil())
				{
					final AvailObject decl =
						DeclarationNodeDescriptor.newModuleVariable(
							expression.token(),
							var,
							expression.initializationExpression());
					final AvailObject assign = AssignmentNodeDescriptor.from(
						VariableUseNodeDescriptor.newUse(
							expression.token(),
							decl),
						expression.initializationExpression(),
						false);
					final AvailObject function = createFunctionToRun(
						assign,
						expression.token().lineNumber());
					synchronized (AbstractAvailCompiler.this)
					{
						serializer.serialize(function);
					}
					evaluatePhraseThen(
						expression.initializationExpression(),
						expression.token().lineNumber(),
						false,
						new Continuation1<AvailObject>()
						{
							@Override
							public void value (final @Nullable AvailObject val)
							{
								assert val != null;
								var.setValue(val);
								onSuccess.value();
							}
						},
						onFailure);
				}
				else
				{
					onSuccess.value();
				}
				break;
			}
			default:
				assert false
					: "Expected top-level declaration to be parsed as local";
		}
	}

	/**
	 * Report that the parser was expecting one of several keywords. The
	 * keywords are keys of the {@linkplain MapDescriptor map} argument
	 * {@code incomplete}.
	 *
	 * @param where
	 *        Where the keywords were expected.
	 * @param incomplete
	 *        A map of partially parsed keywords, where the keys are the strings
	 *        that were expected at this position.
	 * @param caseInsensitive
	 *        {@code true} if the parsed keywords are case-insensitive, {@code
	 *        false} otherwise.
	 */
	void expectedKeywordsOf (
		final ParserState where,
		final AvailObject incomplete,
		final boolean caseInsensitive)
	{
		where.expected(
			new Generator<String>()
			{
				@Override
				public String value ()
				{
					final StringBuilder builder = new StringBuilder(200);
					if (caseInsensitive)
					{
						builder.append(
							"one of the following case-insensitive internal "
							+ "keywords:");
					}
					else
					{
						builder.append(
							"one of the following internal keywords:");
					}
					final List<String> sorted = new ArrayList<String>(
						incomplete.mapSize());
					for (final MapDescriptor.Entry entry
						: incomplete.mapIterable())
					{
						sorted.add(entry.key.asNativeString());
					}
					Collections.sort(sorted);
					boolean startOfLine = true;
					builder.append("\n\t");
					final int leftColumn = 4 + 4; // ">>> " and a tab.
					int column = leftColumn;
					for (final String s : sorted)
					{
						if (!startOfLine)
						{
							builder.append("  ");
							column += 2;
						}
						startOfLine = false;
						final int lengthBefore = builder.length();
						builder.append(s);
						column += builder.length() - lengthBefore;
						if (column + 2 + s.length() > 80)
						{
							builder.append("\n\t");
							column = leftColumn;
							startOfLine = true;
						}
					}
					return builder.toString();
				}
			});
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
	protected void parseLeadingKeywordSendThen (
		final ParserState start,
		final Con<AvailObject> continuation)
	{
		parseRestOfSendNode(
			start,
			module.filteredBundleTree(),
			null,
			start,
			false,  // Nothing consumed yet.
			Collections.<AvailObject> emptyList(),
			continuation);
	}

	/**
	 * Parse a send node whose leading argument has already been parsed.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param leadingArgument
	 *            The argument that was already parsed.
	 * @param initialTokenPosition
	 *            Where the leading argument started.
	 * @param continuation
	 *            What to do after parsing a send node.
	 */
	void parseLeadingArgumentSendAfterThen (
		final ParserState start,
		final AvailObject leadingArgument,
		final ParserState initialTokenPosition,
		final Con<AvailObject> continuation)
	{
		assert start.position != initialTokenPosition.position;
		assert leadingArgument != null;
		parseRestOfSendNode(
			start,
			module.filteredBundleTree(),
			leadingArgument,
			initialTokenPosition,
			false,  // Leading argument does not yet count as something parsed.
			Collections.<AvailObject> emptyList(),
			continuation);
	}

	/**
	 * Parse an expression with an optional lead-argument message send around
	 * it. Backtracking will find all valid interpretations.
	 *
	 * @param startOfLeadingArgument
	 *            Where the leading argument started.
	 * @param afterLeadingArgument
	 *            Just after the leading argument.
	 * @param node
	 *            An expression that acts as the first argument for a potential
	 *            leading-argument message send, or possibly a chain of them.
	 * @param continuation
	 *            What to do with either the passed node, or the node wrapped in
	 *            suitable leading-argument message sends.
	 */
	void parseOptionalLeadingArgumentSendAfterThen (
		final ParserState startOfLeadingArgument,
		final ParserState afterLeadingArgument,
		final AvailObject node,
		final Con<AvailObject> continuation)
	{
		// It's optional, so try it with no wrapping.
		attempt(afterLeadingArgument, continuation, node);

		// Don't wrap it if its type is top.
		if (node.expressionType().equals(TOP.o()))
		{
			return;
		}

		// Try to wrap it in a leading-argument message send.
		attempt(
			afterLeadingArgument,
			new Con<AvailObject>("Possible leading argument send")
			{
				@Override
				public void value (
					final @Nullable ParserState afterLeadingArgument2,
					final @Nullable AvailObject node2)
				{
					assert afterLeadingArgument2 != null;
					assert node2 != null;
					parseLeadingArgumentSendAfterThen(
						afterLeadingArgument2,
						node2,
						startOfLeadingArgument,
						new Con<AvailObject>("Leading argument send")
						{
							@Override
							public void value (
								final @Nullable ParserState afterSend,
								final @Nullable AvailObject leadingSend)
							{
								assert afterSend != null;
								assert leadingSend != null;
								parseOptionalLeadingArgumentSendAfterThen(
									startOfLeadingArgument,
									afterSend,
									leadingSend,
									continuation);
							}
						});
				}
			},
			node);
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
	 * @param consumedAnything
	 *            Whether any actual tokens have been consumed so far for this
	 *            send node.  That includes any leading argument.
	 * @param initialTokenPosition
	 *            The parse position where the send node started to be
	 *            processed. Does not count the position of the first argument
	 *            if there are no leading keywords.
	 * @param argsSoFar
	 *            The list of arguments parsed so far. I do not modify it. This
	 *            is a stack of expressions that the parsing instructions will
	 *            assemble into a list that correlates with the top-level
	 *            non-backquoted underscores and guillemet groups in the message
	 *            name.
	 * @param continuation
	 *            What to do with a fully parsed send node.
	 */
	void parseRestOfSendNode (
		final ParserState start,
		final AvailObject bundleTree,
		final @Nullable AvailObject firstArgOrNull,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<AvailObject> argsSoFar,
		final Con<AvailObject> continuation)
	{
		bundleTree.expand();
		final AvailObject complete = bundleTree.lazyComplete();
		final AvailObject incomplete = bundleTree.lazyIncomplete();
		final AvailObject caseInsensitive =
			bundleTree.lazyIncompleteCaseInsensitive();
		final AvailObject actions = bundleTree.lazyActions();
		final AvailObject prefilter = bundleTree.lazyPrefilterMap();
		final boolean anyComplete = complete.mapSize() > 0;
		final boolean anyIncomplete = incomplete.mapSize() > 0;
		final boolean anyCaseInsensitive = caseInsensitive.mapSize() > 0;
		final boolean anyActions = actions.mapSize() > 0;
		final boolean anyPrefilter = prefilter.mapSize() > 0;

		if (!(anyComplete
			|| anyIncomplete
			|| anyCaseInsensitive
			|| anyActions
			|| anyPrefilter))
		{
			return;
		}
		if (anyComplete && consumedAnything && firstArgOrNull == null)
		{
			// There are complete messages, we didn't leave a leading argument
			// stranded, and we made progress in the file (i.e., the message
			// send does not consist of exactly zero tokens).  It *should* be
			// powerful enough to parse calls of "_" (i.e., the implicit
			// conversion operation), but only if there is a grammatical
			// restriction to prevent run-away left-recursion.  A type
			// restriction won't be checked soon enough to prevent the
			// recursion.
			for (final MapDescriptor.Entry entry : complete.mapIterable())
			{
				if (runtime.hasMethodsAt(entry.key))
				{
					completedSendNode(
						initialTokenPosition,
						start,
						argsSoFar,
						entry.value,
						continuation);
				}
			}
		}
		if (anyIncomplete
			&& firstArgOrNull == null
			&& !start.atEnd())
		{
			final AvailObject keywordToken = start.peekToken();
			if (keywordToken.tokenType() == KEYWORD
				|| keywordToken.tokenType() == OPERATOR)
			{
				final AvailObject keywordString = keywordToken.string();
				if (incomplete.hasKey(keywordString))
				{
					final AvailObject subtree = incomplete.mapAt(keywordString);
					workUnitDo(
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
									true,  // Just consumed a token
									argsSoFar,
									continuation);
							}
						},
						"Continue send after a keyword",
						start.afterToken());
				}
				else
				{
					expectedKeywordsOf(start, incomplete, false);
				}
			}
			else
			{
				expectedKeywordsOf(start, incomplete, false);
			}
		}
		if (anyCaseInsensitive
			&& firstArgOrNull == null
			&& !start.atEnd())
		{
			final AvailObject keywordToken = start.peekToken();
			if (keywordToken.tokenType() == KEYWORD
					|| keywordToken.tokenType() == OPERATOR)
			{
				final AvailObject keywordString =
					keywordToken.lowerCaseString();
				if (caseInsensitive.hasKey(keywordString))
				{
					final AvailObject subtree =
						caseInsensitive.mapAt(keywordString);
					workUnitDo(
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
									true,  // Just consumed a token.
									argsSoFar,
									continuation);
							}
						},
						"Continue send after a keyword",
						start.afterToken());
				}
				else
				{
					expectedKeywordsOf(start, caseInsensitive, true);
				}
			}
			else
			{
				expectedKeywordsOf(start, caseInsensitive, true);
			}
		}
		if (anyPrefilter)
		{
			final AvailObject latestArgument = last(argsSoFar);
			if (latestArgument.isInstanceOfKind(SEND_NODE.mostGeneralType()))
			{
				final AvailObject methodName = latestArgument.method().name();
				if (prefilter.hasKey(methodName))
				{
					parseRestOfSendNode(
						start,
						prefilter.mapAt(methodName),
						firstArgOrNull,
						initialTokenPosition,
						consumedAnything,
						argsSoFar,
						continuation);
					// Don't allow normal action processing, as it would ignore
					// the restriction which we've been so careful to prefilter.
					return;
				}
			}
		}
		if (anyActions)
		{
			for (final MapDescriptor.Entry entry : actions.mapIterable())
			{
				final AvailObject key = entry.key;
				final AvailObject value = entry.value;
				workUnitDo(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							runParsingInstructionThen(
								start,
								key.extractInt(),
								firstArgOrNull,
								argsSoFar,
								initialTokenPosition,
								consumedAnything,
								value,
								continuation);
						}
					},
					"Continue with an instruction",
					start);
			}
		}
	}

	/**
	 * Execute one non-keyword-parsing instruction, then run the continuation.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param instruction
	 *            The {@linkplain MessageSplitter instruction} to execute.
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
	 * @param consumedAnything
	 *            Whether any tokens or arguments have been consumed yet.
	 * @param successorTrees
	 *            The {@linkplain TupleDescriptor tuple} of {@linkplain
	 *            MessageBundleTreeDescriptor bundle trees} at which to continue
	 *            parsing.
	 * @param continuation
	 *            What to do with a complete {@linkplain SendNodeDescriptor
	 *            message send}.
	 */
	void runParsingInstructionThen (
		final ParserState start,
		final int instruction,
		final @Nullable AvailObject firstArgOrNull,
		final List<AvailObject> argsSoFar,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final AvailObject successorTrees,
		final Con<AvailObject> continuation)
	{
		final ParsingOperation op = ParsingOperation.decode(instruction);
//		System.out.format(
//			"OP=%s%s [%s ☞ %s]%n",
//			op.name(),
//			instruction < ParsingOperation.distinctInstructions
//				? ""
//				: " (operand=" + op.operand(instruction) + ")",
//			tokens.get(start.position - 1).string(),
//			tokens.get(start.position).string());
		switch (op)
		{
			case parseArgument:
			{
				// Parse an argument and continue.
				assert successorTrees.tupleSize() == 1;
				parseSendArgumentWithExplanationThen(
					start,
					" (an argument of some message)",
					firstArgOrNull,
					firstArgOrNull == null
						&& initialTokenPosition.position != start.position,
					new Con<AvailObject>("Argument of message send")
					{
						@Override
						public void value (
							final @Nullable ParserState afterArg,
							final @Nullable AvailObject newArg)
						{
							assert afterArg != null;
							assert newArg != null;
							final List<AvailObject> newArgsSoFar =
								append(argsSoFar, newArg);
							workUnitDo(
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
											/* Arg was consumed if it's not a
											 * leading argument... */
											firstArgOrNull == null,
											newArgsSoFar,
											continuation);
									}
								},
								"Continue send after argument",
								afterArg);
						}
					});
				break;
			}
			case newList:
			{
				// Push an empty list node and continue.
				assert successorTrees.tupleSize() == 1;
				final List<AvailObject> newArgsSoFar =
					append(argsSoFar, ListNodeDescriptor.empty());
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after push empty",
					start);
				break;
			}
			case appendArgument:
			{
				// Append the item that's the last thing to the list that's the
				// second last thing. Pop both and push the new list (the
				// original list must not change), then continue.
				assert successorTrees.tupleSize() == 1;
				final AvailObject value = last(argsSoFar);
				final List<AvailObject> poppedOnce = withoutLast(argsSoFar);
				final AvailObject oldNode = last(poppedOnce);
				final AvailObject listNode = oldNode.copyWith(value);
				final List<AvailObject> newArgsSoFar =
					append(withoutLast(poppedOnce), listNode);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after append",
					start);
				break;
			}
			case saveParsePosition:
			{
				// Push current parse position.
				assert successorTrees.tupleSize() == 1;
				final AvailObject marker = MarkerNodeDescriptor.create(
					IntegerDescriptor.fromInt(
						firstArgOrNull == null
							? start.position
							: initialTokenPosition.position));
				final List<AvailObject> newArgsSoFar =
					PrefixSharingList.append(argsSoFar, marker);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after push parse position",
					start);
				break;
			}
			case discardSavedParsePosition:
			{
				// Under-pop saved parse position (from 2nd-to-top of stack).
				assert successorTrees.tupleSize() == 1;
				final AvailObject oldTop = last(argsSoFar);
				final List<AvailObject> poppedOnce = withoutLast(argsSoFar);
				assert last(poppedOnce).isMarkerNode();
				final List<AvailObject> newArgsSoFar =
					append(withoutLast(poppedOnce), oldTop);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after underpop saved position",
					start);
				break;
			}
			case ensureParseProgress:
			{
				// Check parse progress (abort if parse position is still equal
				// to value at 2nd-to-top of stack). Also update the entry to
				// be the new parse position.
				assert successorTrees.tupleSize() == 1;
				final AvailObject top = last(argsSoFar);
				final List<AvailObject> poppedOnce = withoutLast(argsSoFar);
				final AvailObject oldMarker = last(poppedOnce);
				if (oldMarker.markerValue().extractInt() == start.position)
				{
					// No progress has been made.  Reject this path.
					return;
				}
				final AvailObject newMarker = MarkerNodeDescriptor.create(
					IntegerDescriptor.fromInt(start.position));
				final List<AvailObject> newArgsSoFar =
					append(
						append(withoutLast(poppedOnce), newMarker),
						top);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after check parse progress",
					start);
				break;
			}
			case parseRawToken:
				// Parse a raw token and continue.
				assert successorTrees.tupleSize() == 1;
				if (firstArgOrNull != null)
				{
					// Starting with a parseRawToken can't cause unbounded
					// left-recursion, so treat it more like reading an expected
					// token than like parseArgument.  Thus, if a firstArgument
					// has been provided (i.e., we're attempting to parse a
					// leading-argument message to wrap a leading expression),
					// then reject the parse.
					break;
				}
				final AvailObject newToken = parseRawTokenOrNull(start);
				if (newToken != null)
				{
					final ParserState afterToken = start.afterToken();
					final AvailObject syntheticToken =
						LiteralTokenDescriptor.create(
							newToken.string(),
							newToken.start(),
							newToken.lineNumber(),
							SYNTHETIC_LITERAL,
							newToken);
					final AvailObject literalNode =
						LiteralNodeDescriptor.fromToken(syntheticToken);
					final List<AvailObject> newArgsSoFar =
						append(argsSoFar, literalNode);
					workUnitDo(
						new Continuation0()
						{
							@Override
							public void value ()
							{
								parseRestOfSendNode(
									afterToken,
									successorTrees.tupleAt(1),
									null,
									initialTokenPosition,
									true,
									newArgsSoFar,
									continuation);
							}
						},
						"Continue send after raw token for ellipsis",
						afterToken);
				}
				break;
			case pop:
			{
				// Pop the parse stack.
				assert successorTrees.tupleSize() == 1;
				final List<AvailObject> newArgsSoFar = withoutLast(argsSoFar);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after pop",
					start);
				break;
			}
			case argumentsCheckpoint:
			{
				assert successorTrees.tupleSize() == 1;
				final AvailObject tupleOfArgsSoFar =
					TupleDescriptor.fromList(argsSoFar);
				workUnitDo(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseRestOfSendNode(
								new ParserState(
									start.position,
									start.scopeMap,
									tupleOfArgsSoFar),
								successorTrees.tupleAt(1),
								firstArgOrNull,
								initialTokenPosition,
								consumedAnything,
								argsSoFar,
								continuation);
						}
					},
					"Continue send after argumentsCheckpoint (§)",
					start);
				break;
			}
			case reserved9:
			case reserved10:
			case reserved11:
			case reserved12:
			case reserved13:
			case reserved14:
			case reserved15:
			{
				AvailObject.error("Invalid parse instruction: " + op);
				break;
			}
			case branch:
				// $FALL-THROUGH$
				// Fall through.  The successorTrees will be different
				// for the jump versus parallel-branch.
			case jump:
				for (int i = successorTrees.tupleSize(); i >= 1; i--)
				{
					final AvailObject successorTree = successorTrees.tupleAt(i);
					workUnitDo(
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
									consumedAnything,
									argsSoFar,
									continuation);
							}
						},
						"Continue send after branch or jump",
						start);
				}
				break;
			case parsePart:
				// $FALL-THROUGH$
			case parsePartCaseInsensitive:
				assert false
				: "parse-token instruction should not be dispatched";
				break;
			case checkArgument:
			{
				// CheckArgument.  An actual argument has just been parsed (and
				// pushed).  Make sure it satisfies any grammatical
				// restrictions.  The message bundle tree's lazy prefilter map
				// deals with that efficiently.
				assert successorTrees.tupleSize() == 1;
				assert firstArgOrNull == null;
				workUnitDo(
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
								consumedAnything,
								argsSoFar,
								continuation);
						}
					},
					"Continue send after checkArgument",
					start);
				break;
			}
			case convert:
			{
				// Convert the argument.
				assert successorTrees.tupleSize() == 1;
				final AvailObject target = last(argsSoFar);
				final AvailObject replacement;
				switch (op.conversionRule(instruction))
				{
					case noConversion:
						replacement = target;
						break;
					case listToSize:
					{
						final AvailObject expressions =
							target.expressionsTuple();
						final AvailObject count = IntegerDescriptor.fromInt(
							expressions.tupleSize());
						final AvailObject token =
							LiteralTokenDescriptor.create(
								StringDescriptor.from(count.toString()),
								initialTokenPosition.peekToken().start(),
								initialTokenPosition.peekToken().lineNumber(),
								LITERAL,
								count);
						final AvailObject literalNode =
							LiteralNodeDescriptor.fromToken(token);
						replacement = literalNode;
						break;
					}
					case listToNonemptiness:
					{
						final AvailObject expressions =
							target.expressionsTuple();
						final AvailObject nonempty =
							AtomDescriptor.objectFromBoolean(
								expressions.tupleSize() > 0);
						final AvailObject token =
							LiteralTokenDescriptor.create(
								StringDescriptor.from(nonempty.toString()),
								initialTokenPosition.peekToken().start(),
								initialTokenPosition.peekToken().lineNumber(),
								LITERAL,
								nonempty);
						final AvailObject literalNode =
							LiteralNodeDescriptor.fromToken(token);
						replacement = literalNode;
						break;
					}
					default:
					{
						replacement = target;
						assert false : "Conversion rule not handled";
						break;
					}
				}
				final List<AvailObject> newArgsSoFar =
					append(withoutLast(argsSoFar), replacement);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after conversion",
					start);
				break;
			}
			case pushIntegerLiteral:
			{
				final AvailObject integerValue = IntegerDescriptor.fromInt(
					op.integerToPush(instruction));
				final AvailObject token =
					LiteralTokenDescriptor.create(
						StringDescriptor.from(integerValue.toString()),
						initialTokenPosition.peekToken().start(),
						initialTokenPosition.peekToken().lineNumber(),
						LITERAL,
						integerValue);
				final AvailObject literalNode =
					LiteralNodeDescriptor.fromToken(token);
				final List<AvailObject> newArgsSoFar =
					append(argsSoFar, literalNode);
				workUnitDo(
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
								consumedAnything,
								newArgsSoFar,
								continuation);
						}
					},
					"Continue send after conversion",
					start);

				break;
			}
		}
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
	 * @param canReallyParse
	 *            Whether any tokens may be consumed.  This should be false
	 *            specifically when the leftmost argument of a leading-argument
	 *            message is being parsed.
	 * @param continuation
	 *            What to do with the argument.
	 */
	void parseSendArgumentWithExplanationThen (
		final ParserState start,
		final String explanation,
		final @Nullable AvailObject firstArgOrNull,
		final boolean canReallyParse,
		final Con<AvailObject> continuation)
	{
		if (firstArgOrNull == null)
		{
			// There was no leading argument, or it has already been accounted
			// for.  If we haven't actually consumed anything yet then don't
			// allow a *leading* argument to be parsed here.  That would lead
			// to ambiguous left-recursive parsing.
			if (canReallyParse)
			{
				parseExpressionThen(
					start,
					new Con<AvailObject>("Argument expression")
					{
						@Override
						public void value (
							final @Nullable ParserState afterArgument,
							final @Nullable AvailObject argument)
						{
							assert afterArgument != null;
							assert argument != null;
							attempt(afterArgument, continuation, argument);
						}
					});
			}
		}
		else
		{
			// We're parsing a message send with a leading argument, and that
			// argument was explicitly provided to the parser.  We should
			// consume the provided first argument now.
			assert !canReallyParse;
			attempt(start, continuation, firstArgOrNull);
		}
	}

	/**
	 * Check the proposed message send for validity. Use not only the applicable
	 * {@linkplain MethodDefinitionDescriptor method signatures}, but also any
	 * type restriction functions. The type restriction functions may choose to
	 * {@linkplain P_352_RejectParsing reject the parse}, indicating that the
	 * argument types are mutually incompatible.
	 *
	 * @param method
	 *        A method.
	 * @param argTypes
	 *        The argument types.
	 * @param state
	 *        The {@linkplain ParserState parser state} after the function
	 *        evaluates successfully.
	 * @param onSuccess
	 *        What to do with the strengthened return type.
	 * @param onFailure
	 *        What to do if validation fails.
	 */
	private void validateArgumentTypes (
		final AvailObject method,
		final List<AvailObject> argTypes,
		final ParserState state,
		final Continuation1<AvailObject> onSuccess,
		final Continuation1<Generator<String>> onFailure)
	{
		final Mutable<AvailObject> definitionsTuple =
			new Mutable<AvailObject>();
		final Mutable<AvailObject> restrictions = new Mutable<AvailObject>();
		method.lock(new Continuation0()
		{
			@Override
			public void value ()
			{
				definitionsTuple.value = method.definitionsTuple();
				restrictions.value = method.typeRestrictions();
			}
		});
		// Filter the definitions down to those that are locally most specific.
		// Fail if more than one survives.
		if (definitionsTuple.value.tupleSize() > 0
			&& !definitionsTuple.value.tupleAt(1).isMacroDefinition())
		{
			// This consists of method definitions.
			for (
				int index = 1, end = argTypes.size();
				index <= end;
				index++)
			{
				final int finalIndex = index;
				final AvailObject finalType =
					argTypes.get(finalIndex - 1).makeShared();
				if (finalType.equals(BottomTypeDescriptor.bottom())
					|| finalType.equals(TOP.o()))
				{
					onFailure.value(new Generator<String> ()
					{
						@Override
						public String value()
						{
							return "argument #"
								+ Integer.toString(finalIndex)
								+ " of message \""
								+ method.name().name().asNativeString()
								+ "\" to have a type other than "
								+ argTypes.get(finalIndex - 1);
						}
					});
					return;
				}
			}
		}
		// Find all method definitions that could match the argument types.
		final List<AvailObject> satisfyingDefinitions =
			method.filterByTypes(argTypes);
		if (satisfyingDefinitions.isEmpty())
		{
			onFailure.value(new Generator<String> ()
			{
				@Override
				public String value()
				{
					final List<AvailObject> functionTypes =
						new ArrayList<AvailObject>(2);
					for (final AvailObject imp : definitionsTuple.value)
					{
						functionTypes.add(imp.bodySignature());
					}
					final Formatter builder = new Formatter();
					final List<Integer> allFailedIndices =
						new ArrayList<Integer>(3);
					each_arg:
					for (int index = argTypes.size(); index >= 1; index--)
					{
						for (final AvailObject sig : functionTypes)
						{
							if (argTypes.get(index - 1).isSubtypeOf(
								sig.argsTupleType().typeAtIndex(index)))
							{
								continue each_arg;
							}
						}
						allFailedIndices.add(0, index);
					}
					builder.format(
						"arguments at indices %s of message %s to match a "
						+ "method definition.%n",
						allFailedIndices,
						method.name().name().asNativeString());
					builder.format(
						"\tI got:%n\t\t%s%n",
						argTypes);
					builder.format(
						"\tI expected%s:",
						functionTypes.size() > 1 ? " one of" : "");
					for (final AvailObject sig : functionTypes)
					{
						builder.format("%n\t\t%s", sig);
					}
					final String builderString = builder.toString();
					builder.close();
					return builderString;
				}
			});
			return;
		}
		// Compute the intersection of the return types of the possible callees.
		final Mutable<AvailObject> intersection = new Mutable<AvailObject>(
			satisfyingDefinitions.get(0).bodySignature().returnType());
		for (int i = 1, end = satisfyingDefinitions.size(); i < end; i++)
		{
			intersection.value = intersection.value.typeIntersection(
				satisfyingDefinitions.get(i).bodySignature().returnType());
		}
		// Determine which semantic restrictions are relevant.
		final List<AvailObject> restrictionsToTry =
			new ArrayList<AvailObject>(restrictions.value.tupleSize());
		for (int i = 1, end = restrictions.value.tupleSize(); i <= end; i++)
		{
			final AvailObject restriction = restrictions.value.tupleAt(i);
			if (restriction.kind().acceptsListOfArgValues(argTypes))
			{
				restrictionsToTry.add(restriction);
			}
		}
		// If there are no relevant semantic restrictions, then just invoke the
		// success continuation with the intersection and exit early.
		if (restrictionsToTry.isEmpty())
		{
			onSuccess.value(intersection.value);
			return;
		}
		// Run all relevant semantic restrictions, in parallel, computing the
		// type intersection of their results.
		final Mutable<Integer> outstanding = new Mutable<Integer>(
			restrictionsToTry.size());
		final Mutable<Boolean> anyFailures = new Mutable<Boolean>(false);
		final Continuation1<AvailObject> intersectAndDecrement =
			workUnitCompletion(
				state,
				new Continuation1<AvailObject>()
				{
					@Override
					public void value (
						final @Nullable AvailObject restrictionType)
					{
						assert restrictionType != null;
						synchronized (outstanding)
						{
							if (!anyFailures.value)
							{
								intersection.value =
									intersection.value.typeIntersection(
										restrictionType);
								outstanding.value--;
								if (outstanding.value == 0)
								{
									onSuccess.value(intersection.value);
								}
							}
						}
					}
				});
		final Continuation1<Throwable> failed =
			workUnitCompletion(
				state,
				new Continuation1<Throwable>()
				{
					@Override
					public void value (final @Nullable Throwable e)
					{
						assert e != null;
						final boolean alreadyFailed;
						synchronized (outstanding)
						{
							alreadyFailed = anyFailures.value;
							if (!alreadyFailed)
							{
								anyFailures.value = true;
							}
						}
						if (!alreadyFailed)
						{
							if (e instanceof AvailRejectedParseException)
							{
								final AvailRejectedParseException rej =
									(AvailRejectedParseException) e;
								final AvailObject problem =
									rej.rejectionString();
								onFailure.value(
									new Generator<String>()
									{
										@Override
										public String value ()
										{
											return
												problem.asNativeString()
												+ " (while parsing send of "
												+ method.name().name()
													.asNativeString()
												+ ")";
										}
									});
							}
							else
							{
								onFailure.value(
									new Generator<String>()
									{
										@Override
										public String value ()
										{
											return
												"semantic restriction not to "
												+ "raise an unhandled "
												+ "exception (while parsing "
												+ "send of "
												+ method.name().name()
													.asNativeString()
												+ "):\n\t"
												+ e.toString();
										}
									});
							}
						}
					}
				});
		for (final AvailObject restriction : restrictionsToTry)
		{
			startWorkUnit();
			evaluateFunctionThen(
				restriction,
				argTypes,
				false,
				intersectAndDecrement,
				failed);
		}
	}

	/**
	 * A complete {@linkplain SendNodeDescriptor send node} has been parsed.
	 * Create the send node and invoke the continuation.
	 *
	 * <p>
	 * If this is a macro, invoke the body immediately with the argument
	 * expressions to produce a parse node.
	 * </p>
	 *
	 * @param stateBeforeCall
	 *            The initial parsing state, prior to parsing the entire
	 *            message.
	 * @param stateAfterCall
	 *            The parsing state after the message.
	 * @param argumentExpressions
	 *            The {@linkplain ParseNodeDescriptor parse nodes} that will be
	 *            arguments of the new send node.
	 * @param bundle
	 *            The {@linkplain MessageBundleDescriptor message bundle} that
	 *            identifies the message to be sent.
	 * @param continuation
	 *            What to do with the resulting send node.
	 */
	void completedSendNode (
		final ParserState stateBeforeCall,
		final ParserState stateAfterCall,
		final List<AvailObject> argumentExpressions,
		final AvailObject bundle,
		final Con<AvailObject> continuation)
	{
		final Mutable<Boolean> valid = new Mutable<Boolean>(true);
		final AvailObject message = bundle.message();
		final AvailObject method = runtime.methodsAt(message);
		assert !method.equalsNil();
		final AvailObject definitionsTuple = method.definitionsTuple();
		assert definitionsTuple.tupleSize() > 0;

		if (definitionsTuple.tupleAt(1).isMacroDefinition())
		{
			// Macro definitions and non-macro definitions are not allowed to
			// mix within a method.
			completedSendNodeForMacro(
				stateBeforeCall,
				stateAfterCall,
				argumentExpressions,
				bundle,
				method,
				continuation);
			return;
		}
		// It invokes a method (not a macro).
		final List<AvailObject> argTypes =
			new ArrayList<AvailObject>(argumentExpressions.size());
		for (final AvailObject argumentExpression : argumentExpressions)
		{
			argTypes.add(argumentExpression.expressionType());
		}
		// Parsing a method send can't affect the scope.
		assert stateAfterCall.scopeMap == stateBeforeCall.scopeMap;
		final ParserState afterState = new ParserState(
			stateAfterCall.position,
			stateBeforeCall.scopeMap,
			stateBeforeCall.innermostBlockArguments);
		// Validate the method send before reifying a send phrase.
		validateArgumentTypes(
			method,
			argTypes,
			stateAfterCall,
			new Continuation1<AvailObject>()
			{
				@Override
				public void value (final @Nullable AvailObject returnType)
				{
					assert returnType != null;
					final AvailObject sendNode = SendNodeDescriptor.from(
						method,
						ListNodeDescriptor.newExpressions(
							TupleDescriptor.fromList(argumentExpressions)),
						returnType);
					attempt(afterState, continuation, sendNode);
				}
			},
			new Continuation1<Generator<String>>()
			{
				@Override
				public void value (
					final @Nullable Generator<String> errorGenerator)
				{
					assert errorGenerator != null;
					valid.value = false;
					stateAfterCall.expected(errorGenerator);
				}
			});
	}

	/**
	 * A macro invocation has just been parsed.  Run it now if macro execution
	 * is supported.
	 *
	 * @param stateBeforeCall
	 *            The initial parsing state, prior to parsing the entire
	 *            message.
	 * @param stateAfterCall
	 *            The parsing state after the message.
	 * @param argumentExpressions
	 *            The {@linkplain ParseNodeDescriptor parse nodes} that will be
	 *            arguments of the new send node.
	 * @param bundle
	 *            The {@linkplain MessageBundleDescriptor message bundle} that
	 *            identifies the message to be sent.
	 * @param method
	 *            The {@linkplain MethodDescriptor method}
	 *            that contains the macro signature to be invoked.
	 * @param continuation
	 *            What to do with the resulting send node.
	 */
	abstract void completedSendNodeForMacro (
		final ParserState stateBeforeCall,
		final ParserState stateAfterCall,
		final List<AvailObject> argumentExpressions,
		final AvailObject bundle,
		final AvailObject method,
		final Con<AvailObject> continuation);

	/**
	 * Create a bootstrap primitive method. Use the primitive's type declaration
	 * as the argument types. If the primitive is fallible then generate
	 * suitable primitive failure code (to invoke the {@link MethodDescriptor
	 * #vmCrashMethod}).
	 *
	 * @param methodName
	 *        The name of the primitive method being defined.
	 * @param primitiveNumber
	 *        The {@linkplain Primitive#primitiveNumber primitive number} of the
	 *        {@linkplain MethodDescriptor method} being defined.
	 * @param continuation
	 *        What to do after the operation completes.
	 */
	void bootstrapMethodThen (
		final String methodName,
		final int primitiveNumber,
		final Continuation0 continuation)
	{
		final AvailObject availName = StringDescriptor.from(methodName);
		final AvailObject nameLiteral =
			LiteralNodeDescriptor.syntheticFrom(availName);
		final AvailObject function =
			MethodDescriptor.newPrimitiveFunction(
				Primitive.byPrimitiveNumberOrFail(primitiveNumber));
		final AvailObject send = SendNodeDescriptor.from(
			MethodDescriptor.vmMethodDefinerMethod(),
			ListNodeDescriptor.newExpressions(TupleDescriptor.from(
				nameLiteral,
				LiteralNodeDescriptor.syntheticFrom(function))),
			TOP.o());
		evaluateModuleStatementThen(
			send,
			continuation,
			new Continuation1<Throwable>()
			{
				@Override
				public void value (final @Nullable Throwable killer)
				{
					assert killer != null;
					compilationFailed(killer);
				}
			});
	}

	/**
	 * Create a bootstrap primitive {@linkplain MacroDefinitionDescriptor
	 * macro}. Use the primitive's type declaration as the argument types. If
	 * the primitive is fallible then generate suitable primitive failure code
	 * (to invoke the {@link MethodDescriptor#vmCrashMethod}).
	 *
	 * @param macroName
	 *        The name of the primitive macro being defined.
	 * @param primitiveNumber
	 *        The {@linkplain Primitive#primitiveNumber primitive number} of the
	 *        macro being defined.
	 * @param continuation
	 *        What to do after the operation completes.
	 */
	void bootstrapMacroThen (
		final String macroName,
		final int primitiveNumber,
		final Continuation0 continuation)
	{
		final AvailObject availName = StringDescriptor.from(macroName);
		final AvailObject nameLiteral =
			LiteralNodeDescriptor.syntheticFrom(availName);
		final AvailObject function =
			MethodDescriptor.newPrimitiveFunction(
				Primitive.byPrimitiveNumberOrFail(primitiveNumber));
		final AvailObject send = SendNodeDescriptor.from(
			MethodDescriptor.vmMacroDefinerMethod(),
			ListNodeDescriptor.newExpressions(TupleDescriptor.from(
				nameLiteral,
				LiteralNodeDescriptor.syntheticFrom(function))),
			TOP.o());
		evaluateModuleStatementThen(
			send,
			continuation,
			new Continuation1<Throwable>()
			{
				@Override
				public void value (final @Nullable Throwable killer)
				{
					assert killer != null;
					compilationFailed(killer);
				}
			});
	}

	/**
	 * Serialize a function that will publish all atoms that are currently
	 * public in the module.
	 *
	 * @param isPublic
	 *        {@code true} if the atoms are public, {@code false} if they are
	 *        private.
	 */
	@InnerAccess void serializePublicationFunction (final boolean isPublic)
	{
		// Output a function that publishes the initial public set of atoms.
		final AvailObject sourceNames =
			isPublic ? module.names() : module.privateNames();
		AvailObject names = SetDescriptor.empty();
		for (final MapDescriptor.Entry entry : sourceNames.mapIterable())
		{
			names = names.setUnionCanDestroy(entry.value, false);
		}
		final AvailObject send = SendNodeDescriptor.from(
			MethodDescriptor.vmPublishAtomsMethod(),
			ListNodeDescriptor.newExpressions(
				TupleDescriptor.from(
					LiteralNodeDescriptor.syntheticFrom(names),
					LiteralNodeDescriptor.syntheticFrom(
						AtomDescriptor.objectFromBoolean(isPublic)))),
			TOP.o());
		final AvailObject function = createFunctionToRun(send, 0);
		function.makeImmutable();
		synchronized (this)
		{
			serializer.serialize(function);
		}
	}

	/**
	 * Apply any pragmas detected during the parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param continuation
	 *        What to do after the operation completes.
	 */
	private void applyPragmasThen (
		final ParserState state,
		final Continuation0 continuation)
	{
		// If there are no pragmas, then just invoke the success continuation
		// and return.
		final int count = moduleHeader.pragmas.size();
		if (count == 0)
		{
			continuation.value();
			return;
		}
		final Mutable<Integer> outstanding =
			new Mutable<Integer>(moduleHeader.pragmas.size());
		final Continuation0 wrapped =
			new Continuation0()
			{
				@Override
				public void value ()
				{
					synchronized (outstanding)
					{
						outstanding.value--;
						if (outstanding.value == 0)
						{
							continuation.value();
						}
					}
				}
			};
		for (final AvailObject pragmaString : moduleHeader.pragmas)
		{
			eventuallyDo(new Continuation0()
			{
				@Override
				public void value ()
				{
					final String nativeString =
						pragmaString.asNativeString();
					final String[] parts = nativeString.split("=", 3);
					assert parts.length == 3;
					final String pragmaKind = parts[0].trim();
					final String pragmaPrim = parts[1].trim();
					final String pragmaName = parts[2].trim();
					// TODO: [MvG] Move these into named constants.
					final boolean isMethod;
					if ((isMethod = pragmaKind.equals("method"))
						|| pragmaKind.equals("macro"))
					{
						final int primNum = Integer.valueOf(pragmaPrim);
						if (isMethod)
						{
							bootstrapMethodThen(pragmaName, primNum, wrapped);
						}
						else
						{
							bootstrapMacroThen(pragmaName, primNum, wrapped);
						}
					}
					else
					{
						state.expected(
							"pragma to have the form "
							+ "method=<digits>=name or macro=<digits>=name.");
						reportError();
						assert false;
					}
				}
			});
		}
	}

	/**
	 * Parse a {@linkplain ModuleHeader module header} from the {@linkplain
	 * TokenDescriptor token list} and apply any side-effects. Then
	 * {@linkplain #parseModuleBody(ParserState) parse a module body} and apply
	 * any side-effects.
	 */
	@InnerAccess void parseModuleCompletely ()
	{
		final ParserState afterHeader = parseModuleHeader(false);
		if (afterHeader == null)
		{
			reportError();
			assert false;
		}
		// Update the reporter. This condition just prevents
		// the reporter from being called twice at the end of a
		// file.
		else if (!afterHeader.atEnd())
		{
			final AvailObject token = afterHeader.peekToken();
			progressReporter.value(
				moduleHeader.moduleName,
				(long) token.lineNumber(),
				(long) token.start(),
				(long) source.length());
		}
		assert afterHeader != null;
		// Run any side-effects implied by this module header
		// against the module.
		final String errorString =
			moduleHeader.applyToModule(module, runtime);
		if (errorString != null)
		{
			afterHeader.expected(errorString);
			reportError();
			assert false;
		}
		synchronized (this)
		{
			serializer.serialize(
				AtomDescriptor.moduleHeaderSectionAtom());
			moduleHeader.serializeHeaderOn(serializer);
			serializer.serialize(
				AtomDescriptor.moduleBodySectionAtom());
		}
		applyPragmasThen(
			afterHeader,
			new Continuation0()
			{
				@Override
				public void value ()
				{
					module.buildFilteredBundleTreeFrom(
						runtime.rootBundleTree());
					// Parse the body of the module.
					if (!afterHeader.atEnd())
					{
						eventuallyDo(new Continuation0()
						{
							@Override
							public void value ()
							{
								parseModuleBody(afterHeader);
							}
						});
					}
					else
					{
						successReporter.value();
					}
				}
			});
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} from the {@linkplain
	 * TokenDescriptor token} list and install it into the {@linkplain
	 * AvailRuntime runtime}.
	 *
	 * @param afterHeader
	 *        The {@linkplain ParserState parse state} after parsing a
	 *        {@linkplain ModuleHeader module header}.
	 */
	@InnerAccess void parseModuleBody (final ParserState afterHeader)
	{
		final Mutable<Con<AvailObject>> parseOutermost =
			new Mutable<Con<AvailObject>>();
		parseOutermost.value = new Con<AvailObject>("Outermost statement")
		{
			@Override
			public void value (
				final @Nullable ParserState afterStatement,
				final @Nullable AvailObject unambiguousStatement)
			{
				assert afterStatement != null;
				assert unambiguousStatement != null;
				synchronized (AbstractAvailCompiler.this)
				{
					assert workUnitsQueued == workUnitsCompleted;
				}

				if (!unambiguousStatement.expressionType().equals(TOP.o()))
				{
					afterStatement.expected(
						"top-level statement to have type ⊤");
					reportError();
					assert false;
				}

				// Clear the section of the fragment cache associated with
				// the (outermost) statement just parsed...
				fragmentCache.clear();
				evaluateModuleStatementThen(
					unambiguousStatement,
					new Continuation0()
					{
						@Override
						public void value ()
						{
							// If this was not the last statement, then report
							// progress.
							if (!afterStatement.atEnd())
							{
								final AvailObject token =
									tokens.get(afterStatement.position - 1);
								progressReporter.value(
									moduleHeader.moduleName,
									(long) token.lineNumber(),
									(long) token.start() + 2,
									(long) source.length());
								eventuallyDo(new Continuation0()
								{
									@Override
									public void value ()
									{
										greatestGuess = 0;
										greatExpectations.clear();
										parseOutermostStatement(
											new ParserState(
												afterStatement.position,
												afterHeader.scopeMap,
												afterHeader
													.innermostBlockArguments),
											parseOutermost.value);
									}
								});
							}
							// Otherwise, make sure that all forwards were
							// resolved.
							else if (loader.pendingForwards.setSize() != 0)
							{
								@SuppressWarnings("resource")
								final Formatter formatter = new Formatter();
								formatter.format(
									"the following forwards to be resolved:");
								for (final AvailObject forward
									: loader.pendingForwards)
								{
									formatter.format("%n\t%s", forward);
								}
								afterStatement.expected(formatter.toString());
								reportError();
								assert false;
							}
							// Otherwise, report success.
							else
							{
								successReporter.value();
							}
						}
					},
					new Continuation1<Throwable>()
					{
						@Override
						public void value (final @Nullable Throwable killer)
						{
							assert killer != null;
							compilationFailed(killer);
						}
					});
			}
		};
		greatestGuess = 0;
		greatExpectations.clear();
		parseOutermostStatement(afterHeader, parseOutermost.value);
	}

	/**
	 * The {@linkplain Continuation4 continuation} that reports compilation
	 * progress at various checkpoints. It that accepts the {@linkplain
	 * ResolvedModuleName name} of the {@linkplain ModuleDescriptor module}
	 * undergoing {@linkplain AbstractAvailCompiler compilation}, the line
	 * number on which the last complete statement concluded, the position of
	 * the ongoing parse (in bytes), and the size of the module (in bytes).
	 */
	@InnerAccess Continuation4<ModuleName, Long, Long, Long> progressReporter;

	/**
	 * The {@linkplain Continuation1 continuation} that reports success of
	 * compilation.
	 */
	@InnerAccess Continuation0 successReporter;

	/**
	 * The {@linkplain Continuation0 continuation} that reports failure of
	 * compilation.
	 */
	@InnerAccess Continuation0 failureReporter;

	/**
	 * Parse a {@linkplain ModuleHeader module header} from the {@linkplain
	 * TokenDescriptor token} list.
	 *
	 * @return A module header.
	 * @throws AvailCompilerException
	 *         If the module header cannot be parsed.
	 */
	public ModuleHeader parseModuleHeader () throws AvailCompilerException
	{
		greatestGuess = -1;
		greatExpectations.clear();
		if (parseModuleHeader(true) == null)
		{
			reportError();
		}
		return moduleHeader;
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} from the {@linkplain
	 * TokenDescriptor token} list and install it into the {@linkplain
	 * AvailRuntime runtime}.
	 *
	 * @param reporter
	 *        How to report progress to the client who instigated compilation.
	 *        This {@linkplain Continuation4 continuation} that accepts the
	 *        {@linkplain ModuleName name} of the {@linkplain ModuleDescriptor
	 *        module} undergoing {@linkplain AbstractAvailCompiler compilation},
	 *        the line number on which the last complete statement concluded,
	 *        the position of the ongoing parse (in bytes), and the size of the
	 *        module (in bytes).
	 * @param succeed
	 *        What to do after compilation succeeds. This {@linkplain
	 *        Continuation1 continuation} is invoked with the completed module.
	 * @param fail
	 *        What to do after compilation fails. This {@linkplain Continuation1
	 *        continuation} is invoked with the terminating {@linkplain
	 *        Throwable throwable}.
	 */
	public void parseModule (
		final Continuation4<ModuleName, Long, Long, Long> reporter,
		final Continuation1<AvailObject> succeed,
		final Continuation1<Throwable> fail)
	{
		progressReporter = reporter;
		successReporter = new Continuation0()
		{
			@Override
			public void value ()
			{
				serializePublicationFunction(true);
				commitModuleTransaction();
				succeed.value(module);
			}
		};
		failureReporter = new Continuation0()
		{
			@Override
			public void value ()
			{
				rollbackModuleTransaction();
				module = NilDescriptor.nil();
				fail.value(terminator);
			}
		};
		startModuleTransaction();
		eventuallyDo(new Continuation0()
		{
			@Override
			public void value ()
			{
				parseModuleCompletely();
			}
		});
	}

	/**
	 * Parse a {@linkplain ModuleHeader module header} from the {@linkplain
	 * TokenDescriptor token} list. If successful, return the {@linkplain
	 * ParserState parser state} just after the header, otherwise return {@code
	 * null}.
	 *
	 * <p>If the {@code dependenciesOnly} parameter is true, only parse the bare
	 * minimum needed to determine information about which modules are used by
	 * this one.</p>
	 *
	 * @param dependenciesOnly
	 *        Whether to do the bare minimum parsing required to determine
	 *        the modules to which this one refers.
	 * @return The state of parsing just after the header, or {@code null} if it
	 *         failed.
	 */
	private @Nullable ParserState parseModuleHeader (
		final boolean dependenciesOnly)
	{
		// Create the initial parser state: no tokens have been seen, no names
		// are in scope, and we're not part-way through parsing a block.
		ParserState state = new ParserState(
			0,
			MapDescriptor.empty(),
			NilDescriptor.nil());

		// The module header must begin with either SYSTEM MODULE or MODULE,
		// followed by the local name of the module.
		if (isSystemCompiler())
		{
			if (!state.peekToken(SYSTEM, "System keyword"))
			{
				return null;
			}
			state = state.afterToken();
		}
		if (!state.peekToken(ExpectedToken.MODULE, "Module keyword"))
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
			if (!moduleHeader.moduleName.localName().equals(
				localName.asNativeString()))
			{
				state.expected("declared local module name to agree with "
						+ "fully-qualified module name");
				return null;
			}
		}
		state = state.afterToken();

		// Module header section tracking.
		final List<ExpectedToken> expected = new ArrayList<ExpectedToken>(
			Arrays.<ExpectedToken>asList(
				VERSIONS, EXTENDS, USES, NAMES, PRAGMA, BODY));
		final Set<AvailObject> seen = new HashSet<AvailObject>(6);
		final Generator<String> expectedMessage = new Generator<String>()
		{
			@Override
			public String value ()
			{
				final StringBuilder builder = new StringBuilder();
				builder.append(
					expected.size() == 1
					? "module header keyword "
					: "one of the following module header keywords: ");
				boolean first = true;
				for (final ExpectedToken token : expected)
				{
					if (!first)
					{
						builder.append(", ");
					}
					builder.append(token.lexeme().asNativeString());
					first = false;
				}
				return builder.toString();
			}
		};

		// Permit the other sections to appear optionally, singly, and in any
		// order. Parsing of the module header is complete when BODY has been
		// consumed.
		while (true)
		{
			final AvailObject token = state.peekToken();
			final AvailObject lexeme = token.string();
			int tokenIndex = 0;
			for (final ExpectedToken expectedToken : expected)
			{
				if (expectedToken.tokenType() == token.tokenType()
					&& expectedToken.lexeme().equals(lexeme))
				{
					break;
				}
				tokenIndex++;
			}
			// The token was not recognized as beginning a module section, so
			// record what was expected and fail the parse.
			if (tokenIndex == expected.size())
			{
				if (seen.contains(lexeme))
				{
					state.expected(
						lexeme.asNativeString()
						+ " keyword (and related section) to occur only once");
				}
				else
				{
					state.expected(expectedMessage);
				}
				return null;
			}
			expected.remove(tokenIndex);
			seen.add(lexeme);
			state = state.afterToken();
			// When BODY has been encountered, the parse of the module header is
			// complete.
			if (lexeme.equals(BODY.lexeme()))
			{
				return state;
			}
			// On VERSIONS, record the versions.
			else if (lexeme.equals(VERSIONS.lexeme()))
			{
				state = parseStringLiterals(state, moduleHeader.versions);
			}
			// On EXTENDS, record the imports.
			else if (lexeme.equals(EXTENDS.lexeme()))
			{
				state = parseImports(state, moduleHeader.extendedModules);
			}
			// On USES, record the imports.
			else if (lexeme.equals(USES.lexeme()))
			{
				state = parseImports(state, moduleHeader.usedModules);
			}
			// On NAMES, record the names.
			else if (lexeme.equals(NAMES.lexeme()))
			{
				state = parseStringLiterals(state, moduleHeader.exportedNames);
			}
			// On PRAGMA, record the pragma strings.
			else if (lexeme.equals(PRAGMA.lexeme()))
			{
				state = parseStringLiterals(state, moduleHeader.pragmas);
			}
			// If the parser state is now null, then fail the parse.
			if (state == null)
			{
				return null;
			}
		}
	}

	/**
	 * Parse an expression. Backtracking will find all valid interpretations.
	 * This method is a key optimization point, so the {@link #fragmentCache} is
	 * used to keep track of parsing solutions at this point, simply replaying
	 * them on subsequent parses, as long as the variable declarations up to
	 * that point were identical.
	 *
	 * <p>
	 * Additionally, the {@code fragmentCache} also keeps track of actions to
	 * perform when another solution is found at this position, so the solutions
	 * and actions can be added in arbitrary order while ensuring that each
	 * action gets a chance to try each solution.
	 * </p>
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param originalContinuation
	 *        What to do with the expression.
	 */
	void parseExpressionThen (
		final ParserState start,
		final Con<AvailObject> originalContinuation)
	{
		synchronized (fragmentCache)
		{
			// The first time we parse at this position the fragmentCache will
			// have no knowledge about it.
			if (!fragmentCache.hasStartedParsingAt(start))
			{
				fragmentCache.indicateParsingHasStartedAt(start);
				workUnitDo(
					new Continuation0()
					{
						@Override
						public void value ()
						{
							parseExpressionUncachedThen(
								start,
								new Con<AvailObject>("Uncached expression")
								{
									@Override
									public void value (
										final @Nullable ParserState afterExpr,
										final @Nullable AvailObject expr)
									{
										assert afterExpr != null;
										assert expr != null;
										synchronized (fragmentCache)
										{
											fragmentCache.addSolution(
												start,
												new AvailCompilerCachedSolution(
													afterExpr,
													expr));
										}
									}
								});
						}
					},
					"Capture expression for caching",
					start);
			}
			fragmentCache.addAction(start, originalContinuation);
		}
	}

	/**
	 * Parse an expression whose type is (at least) someType. There may be
	 * multiple expressions that start at the specified starting point.  Only
	 * evaluate expressions whose static type is as strong as the expected type.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param someType
	 *        The type that the expression must return.
	 * @param continuation
	 *        What to do with the result of expression evaluation.
	 */
	void parseAndEvaluateExpressionYieldingInstanceOfThen (
		final ParserState start,
		final AvailObject someType,
		final Con<AvailObject> continuation)
	{
		final ParserState startWithoutScope = new ParserState(
			start.position,
			MapDescriptor.empty(),
			NilDescriptor.nil());
		parseExpressionThen(
			startWithoutScope,
			new Con<AvailObject>("Evaluate expression")
			{
				@SuppressWarnings("null")
				@Override
				public void value (
					final @Nullable ParserState afterExpression,
					final @Nullable AvailObject expression)
				{
					if (expression.expressionType().isSubtypeOf(someType))
					{
						startWorkUnit();
						evaluatePhraseThen(
							expression,
							start.peekToken().lineNumber(),
							false,
							workUnitCompletion(
								afterExpression,
								new Continuation1<AvailObject>()
								{
									@Override
									public void value (
										final @Nullable AvailObject value)
									{
										assert value != null;
										if (value.isInstanceOf(someType))
										{
											assert afterExpression.scopeMap ==
													startWithoutScope.scopeMap
												: "Subexpression should not "
													+ "have been able to cause "
													+ "declaration";
											// Make sure we continue at the
											// position after the expression,
											// but using the scope stack we
											// started with. That's because the
											// expression was parsed for
											// execution, and as such was
											// excluded from seeing things that
											// would be in scope for regular
											// subexpressions at this point.
											attempt(
												new ParserState(
													afterExpression.position,
													start.scopeMap,
													start
														.innermostBlockArguments),
												continuation,
												value);
										}
										else
										{
											afterExpression.expected(
												"expression to respect "
												+ "its own type declaration");
										}
									}
							}),
							new Continuation1<Throwable>()
							{
								@Override
								public void value (
									final @Nullable Throwable killer)
								{
									assert killer != null;
									compilationFailed(killer);
								}
							});
					}
					else
					{
						afterExpression.expected(
							new Generator<String>()
							{
								@Override
								public String value ()
								{
									return
										"expression to have type "
										+ someType;
								}
							});
					}
				}
			});
	}

	/**
	 * Parse a top-level statement.  This is the <em>only</em> boundary for the
	 * backtracking grammar (it used to be that <em>all</em> statements had to
	 * be unambiguous, even those in blocks).  The passed continuation will be
	 * invoked at most once, and only if the top-level statement had a single
	 * interpretation.
	 *
	 * @param start
	 *            Where to start parsing a top-level statement.
	 * @param continuation
	 *            What to do with the (unambiguous) top-level statement.
	 */
	abstract void parseOutermostStatement (
		final ParserState start,
		final Con<AvailObject> continuation);

	/**
	 * Parse an expression, without directly using the
	 * {@linkplain #fragmentCache}.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the expression.
	 */
	abstract void parseExpressionUncachedThen (
		final ParserState start,
		final Con<AvailObject> continuation);

	/**
	 * Parse and return an occurrence of a raw keyword, literal, or operator
	 * token.  If no suitable token is present, answer null.  The caller is
	 * responsible for skipping the token if it was parsed.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @return
	 *            The token or {@code null}.
	 */
	protected @Nullable AvailObject parseRawTokenOrNull (
		final ParserState start)
	{
		final AvailObject token = start.peekToken();
		switch (token.tokenType())
		{
			case KEYWORD:
			case OPERATOR:
			case LITERAL:
				return token;
			default:
				return null;
		}
	}
}
