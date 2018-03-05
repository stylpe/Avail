/*
 * AvailCompiler.java
 * Copyright © 1993-2018, The Avail Foundation, LLC. All rights reserved.
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

import com.avail.AvailRuntime;
import com.avail.AvailThread;
import com.avail.annotations.InnerAccess;
import com.avail.builder.ModuleName;
import com.avail.builder.ResolvedModuleName;
import com.avail.compiler.problems.Problem;
import com.avail.compiler.problems.ProblemHandler;
import com.avail.compiler.scanning.LexingState;
import com.avail.compiler.splitter.MessageSplitter;
import com.avail.descriptor.*;
import com.avail.descriptor.FiberDescriptor.GeneralFlag;
import com.avail.descriptor.MapDescriptor.Entry;
import com.avail.descriptor.MethodDescriptor.SpecialMethodAtom;
import com.avail.descriptor.TokenDescriptor.TokenType;
import com.avail.dispatch.LookupTree;
import com.avail.exceptions.AvailAssertionFailedException;
import com.avail.exceptions.AvailEmergencyExitException;
import com.avail.exceptions.AvailErrorCode;
import com.avail.exceptions.MalformedPragmaException;
import com.avail.exceptions.MethodDefinitionException;
import com.avail.interpreter.AvailLoader;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.Primitive;
import com.avail.interpreter.primitive.phrases.P_RejectParsing;
import com.avail.io.TextInterface;
import com.avail.performance.Statistic;
import com.avail.performance.StatisticReport;
import com.avail.utility.Generator;
import com.avail.utility.Mutable;
import com.avail.utility.MutableInt;
import com.avail.utility.MutableLong;
import com.avail.utility.MutableOrNull;
import com.avail.utility.Pair;
import com.avail.utility.PrefixSharingList;
import com.avail.utility.evaluation.Continuation0;
import com.avail.utility.evaluation.Continuation1NotNull;
import com.avail.utility.evaluation.Continuation2;
import com.avail.utility.evaluation.Continuation3;
import com.avail.utility.evaluation.Describer;
import com.avail.utility.evaluation.FormattingDescriber;
import com.avail.utility.evaluation.SimpleDescriber;
import com.avail.utility.evaluation.Transformer3;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.avail.AvailRuntime.currentRuntime;
import static com.avail.compiler.ExpectedToken.*;
import static com.avail.compiler.ParsingOperation.*;
import static com.avail.compiler.problems.ProblemType.EXTERNAL;
import static com.avail.compiler.problems.ProblemType.PARSE;
import static com.avail.compiler.splitter.MessageSplitter.Metacharacter;
import static com.avail.descriptor.AbstractEnumerationTypeDescriptor
	.instanceTypeOrMetaOn;
import static com.avail.descriptor.AssignmentPhraseDescriptor.newAssignment;
import static com.avail.descriptor.AtomDescriptor.*;
import static com.avail.descriptor.AtomDescriptor.SpecialAtom.*;
import static com.avail.descriptor.DeclarationPhraseDescriptor
	.newModuleConstant;
import static com.avail.descriptor.DeclarationPhraseDescriptor
	.newModuleVariable;
import static com.avail.descriptor.FiberDescriptor.newLoaderFiber;
import static com.avail.descriptor.FunctionDescriptor.createFunctionForPhrase;
import static com.avail.descriptor.FunctionDescriptor.newPrimitiveFunction;
import static com.avail.descriptor.LexerDescriptor.lexerBodyFunctionType;
import static com.avail.descriptor.LexerDescriptor.lexerFilterFunctionType;
import static com.avail.descriptor.ListPhraseDescriptor.emptyListNode;
import static com.avail.descriptor.ListPhraseDescriptor.newListNode;
import static com.avail.descriptor.LiteralPhraseDescriptor.literalNodeFromToken;
import static com.avail.descriptor.LiteralPhraseDescriptor
	.syntheticLiteralNodeFor;
import static com.avail.descriptor.LiteralTokenDescriptor.literalToken;
import static com.avail.descriptor.MacroSubstitutionPhraseDescriptor
	.newMacroSubstitution;
import static com.avail.descriptor.MapDescriptor.emptyMap;
import static com.avail.descriptor.MapDescriptor.mapFromPairs;
import static com.avail.descriptor.MethodDescriptor.SpecialMethodAtom.*;
import static com.avail.descriptor.ModuleDescriptor.newModule;
import static com.avail.descriptor.NilDescriptor.nil;
import static com.avail.descriptor.ObjectTupleDescriptor.*;
import static com.avail.descriptor.ParsingPlanInProgressDescriptor
	.newPlanInProgress;
import static com.avail.descriptor.PhraseTypeDescriptor.PhraseKind.*;
import static com.avail.descriptor.SendPhraseDescriptor.newSendNode;
import static com.avail.descriptor.SetDescriptor.emptySet;
import static com.avail.descriptor.StringDescriptor.formatString;
import static com.avail.descriptor.StringDescriptor.stringFrom;
import static com.avail.descriptor.TokenDescriptor.TokenType.*;
import static com.avail.descriptor.TupleDescriptor.emptyTuple;
import static com.avail.descriptor.TupleDescriptor.toList;
import static com.avail.descriptor.TupleTypeDescriptor.stringType;
import static com.avail.descriptor.TypeDescriptor.Types.TOKEN;
import static com.avail.descriptor.TypeDescriptor.Types.TOP;
import static com.avail.descriptor.VariableSharedGlobalDescriptor.createGlobal;
import static com.avail.descriptor.VariableTypeDescriptor.variableTypeFor;
import static com.avail.descriptor.VariableUsePhraseDescriptor.newUse;
import static com.avail.exceptions.AvailErrorCode.E_AMBIGUOUS_METHOD_DEFINITION;
import static com.avail.exceptions.AvailErrorCode.E_NO_METHOD_DEFINITION;
import static com.avail.interpreter.AvailLoader.Phase.COMPILING;
import static com.avail.interpreter.AvailLoader.Phase.EXECUTING_FOR_COMPILE;
import static com.avail.interpreter.Interpreter.runOutermostFunction;
import static com.avail.interpreter.Interpreter.stringifyThen;
import static com.avail.interpreter.Primitive.primitiveByName;
import static com.avail.utility.Nulls.stripNull;
import static com.avail.utility.PrefixSharingList.append;
import static com.avail.utility.PrefixSharingList.last;
import static com.avail.utility.StackPrinter.trace;
import static com.avail.utility.Strings.increaseIndentation;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * The compiler for Avail code.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class AvailCompiler
{
	public final CompilationContext compilationContext;

	/**
	 * The {@link AvailRuntime} for the compiler. Since a compiler cannot
	 * migrate between two runtime environments, it is safe to cache it for
	 * efficient access.
	 */
	private final AvailRuntime runtime = currentRuntime();

	public A_String source ()
	{
		return compilationContext.source();
	}

	/**
	 * The {@linkplain AvailCompiler compiler} notifies a {@code
	 * CompilerProgressReporter} whenever a top-level statement is parsed
	 * unambiguously.
	 *
	 * <p>The {@link #value(Object, Object, Object)} method takes the module
	 * name, the module size in bytes, and the current parse position within the
	 * module.</p>
	 */
	@FunctionalInterface
	public interface CompilerProgressReporter
	extends Continuation3<ModuleName, Long, Long>
	{
		// nothing
	}

	/**
	 * Answer the {@linkplain ModuleHeader module header} for the current
	 * {@linkplain ModuleDescriptor module} being parsed.
	 *
	 * @return the moduleHeader
	 */
	private ModuleHeader moduleHeader ()
	{
		return stripNull(compilationContext.getModuleHeader());
	}

	/**
	 * Answer the fully-qualified name of the {@linkplain ModuleDescriptor
	 * module} undergoing compilation.
	 *
	 * @return The module name.
	 */
	private ModuleName moduleName ()
	{
		return new ModuleName(
			compilationContext.module().moduleName().asNativeString());
	}

	/** The memoization of results of previous parsing attempts. */
	private final AvailCompilerFragmentCache fragmentCache =
		new AvailCompilerFragmentCache();

	/**
	 * Asynchronously construct a suitable {@code AvailCompiler} to parse the
	 * specified {@linkplain ModuleName module name}.
	 *
	 * @param resolvedName
	 *        The {@linkplain ResolvedModuleName resolved name} of the
	 *        {@linkplain ModuleDescriptor module} to compile.
	 * @param textInterface
	 *        The {@linkplain TextInterface text interface} for any {@linkplain
	 *        A_Fiber fibers} started by the new compiler.
	 * @param pollForAbort
	 *        A zero-argument continuation to invoke.
	 * @param succeed
	 *        What to do with the resultant compiler in the event of success.
	 *        This is a continuation that accepts the new compiler.
	 * @param afterFail
	 *        What to do after a failure that the {@linkplain ProblemHandler
	 *        problem handler} does not choose to continue.
	 * @param problemHandler
	 *        A problem handler.
	 */
	public static void create (
		final ResolvedModuleName resolvedName,
		final TextInterface textInterface,
		final Generator<Boolean> pollForAbort,
		final CompilerProgressReporter reporter,
		final Continuation1NotNull<AvailCompiler> succeed,
		final Continuation0 afterFail,
		final ProblemHandler problemHandler)
	{
		extractSourceThen(
			resolvedName,
			sourceText -> succeed.value(
				new AvailCompiler(
					new ModuleHeader(resolvedName),
					newModule(stringFrom(resolvedName.qualifiedName())),
					stringFrom(sourceText),
					textInterface,
					pollForAbort,
					reporter,
					problemHandler)),
			afterFail,
			problemHandler);
	}

	/**
	 * Construct a new {@code AvailCompiler}.
	 *
	 * @param moduleHeader
	 *        The {@link ModuleHeader module header} of the module to compile.
	 *        May be null for synthetic modules (for entry points), or when
	 *        parsing the header.
	 * @param module
	 *        The current {@linkplain ModuleDescriptor module}.`
	 * @param source
	 *        The source {@link String}.
	 * @param textInterface
	 *        The {@linkplain TextInterface text interface} for any {@linkplain
	 *        A_Fiber fibers} started by this compiler.
	 * @param pollForAbort
	 *        How to quickly check if the client wants to abort compilation.
	 * @param progressReporter
	 *        How to report progress to the client who instigated compilation.
	 *        This {@linkplain CompilerProgressReporter continuation} that
	 *        accepts the {@linkplain ModuleName name} of the {@linkplain
	 *        ModuleDescriptor module} undergoing {@linkplain
	 *        AvailCompiler compilation}, the line number on which the
	 *        last complete statement concluded, the position of the ongoing
	 *        parse (in bytes), and the size of the module (in bytes).
	 * @param problemHandler
	 *        The {@link ProblemHandler} used for reporting compilation
	 *        problems.
	 */
	public AvailCompiler (
		final @Nullable ModuleHeader moduleHeader,
		final A_Module module,
		final A_String source,
		final TextInterface textInterface,
		final Generator<Boolean> pollForAbort,
		final CompilerProgressReporter progressReporter,
		final ProblemHandler problemHandler)
	{
		this.compilationContext = new CompilationContext(
			moduleHeader,
			module,
			source,
			textInterface,
			pollForAbort,
			progressReporter,
			problemHandler);
	}

	/**
	 * A list of subexpressions being parsed, represented by {@link
	 * A_BundleTree}s holding the positions within all outer send expressions.
	 */
	@InnerAccess static class PartialSubexpressionList
	{
		/** The {@link A_BundleTree} being parsed at this moment. */
		@InnerAccess final A_BundleTree bundleTree;

		/** The parent {@link PartialSubexpressionList} being parsed. */
		@InnerAccess final @Nullable PartialSubexpressionList parent;

		/** How many subexpressions deep that we're parsing. */
		@InnerAccess final int depth;

		/**
		 * Create a list like the receiver, but with a different {@link
		 * A_BundleTree}.
		 *
		 * @param newBundleTree
		 *        The new {@link A_BundleTree} to replace the one in the
		 *        receiver within the copy.
		 * @return A {@code PartialSubexpressionList} like the receiver, but
		 *         with a different message bundle tree.
		 */
		@InnerAccess PartialSubexpressionList advancedTo (
			final A_BundleTree newBundleTree)
		{
			return new PartialSubexpressionList(newBundleTree, parent);
		}

		/**
		 * Construct a new {@code PartialSubexpressionList}.
		 *
		 * @param bundleTree
		 *        The current {@link A_BundleTree} being parsed.
		 * @param parent
		 *        The enclosing partially-parsed super-expressions being parsed.
		 */
		@InnerAccess PartialSubexpressionList (
			final A_BundleTree bundleTree,
			final @Nullable PartialSubexpressionList parent)
		{
			this.bundleTree = bundleTree;
			this.parent = parent;
			this.depth = parent == null ? 1 : parent.depth + 1;
		}
	}

	/**
	 * Output a description of the layers of message sends that are being parsed
	 * at this point in history.
	 *
	 * @param partialSubexpressions
	 *        The {@link PartialSubexpressionList} that captured the nesting of
	 *        partially parsed superexpressions.
	 * @param builder
	 *        Where to describe the chain of superexpressions.
	 */
	private static void describeOn (
		final @Nullable PartialSubexpressionList partialSubexpressions,
		final StringBuilder builder)
	{
		@Nullable PartialSubexpressionList pointer = partialSubexpressions;
		if (pointer == null)
		{
			builder.append("\n\t(top level expression)");
			return;
		}
		final int maxDepth = 10;
		final int limit = max(pointer.depth - maxDepth, 0);
		while (pointer != null && pointer.depth >= limit)
		{
			builder.append("\n\t");
			builder.append(pointer.depth);
			builder.append(". ");
			final A_BundleTree bundleTree = pointer.bundleTree;
			// Reduce to the plans' unique bundles.
			final A_Map bundlesMap = bundleTree.allParsingPlansInProgress();
			final List<A_Bundle> bundles =
				toList(bundlesMap.keysAsSet().asTuple());
			bundles.sort((b1, b2) ->
			{
				assert b1 != null && b2 != null;
				return b1.message().atomName().asNativeString()
					.compareTo(b2.message().atomName().asNativeString());
			});
			boolean first = true;
			final int maxBundles = 3;
			for (final A_Bundle bundle :
				bundles.subList(0, min(bundles.size(), maxBundles)))
			{
				if (!first)
				{
					builder.append(", ");
				}
				final A_Map plans = bundlesMap.mapAt(bundle);
				// Pick an active plan arbitrarily for this bundle.
				final A_Set plansInProgress =
					plans.mapIterable().next().value();
				final A_ParsingPlanInProgress planInProgress =
					plansInProgress.iterator().next();
				// Adjust the pc to refer to the actual instruction that caused
				// the argument parse, not the successor instruction that was
				// captured.
				final A_ParsingPlanInProgress adjustedPlanInProgress =
					newPlanInProgress(
						planInProgress.parsingPlan(),
						planInProgress.parsingPc() - 1);
				builder.append(adjustedPlanInProgress.nameHighlightingPc());
				first = false;
			}
			if (bundles.size() > maxBundles)
			{
				builder.append("… (and ");
				builder.append(bundles.size() - maxBundles);
				builder.append(" others)");
			}
			pointer = pointer.parent;
		}
	}

	/**
	 * A simple static factory for constructing {@link Con}s.  Java is crummy at
	 * deducing parameter types for constructors, so this static method can be
	 * used to elide the CompilerSolution type.
	 *
	 * @param superexpressions
	 *        The {@link PartialSubexpressionList} that explains why this
	 *        continuation exists.
	 * @param continuation
	 *        The {@link Continuation1NotNull} to invoke.
	 * @return The new {@link Con}.
	 */
	static Con Con (
		final @Nullable PartialSubexpressionList superexpressions,
		final Continuation1NotNull<CompilerSolution> continuation)
	{
		return new Con(superexpressions, continuation);
	}

	/**
	 * Execute {@code #tryBlock}, passing a {@linkplain Con continuation} that
	 * it should run upon finding exactly one local {@linkplain CompilerSolution
	 * solution}.  Report ambiguity as an error.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param tryBlock
	 *        What to try. This is a continuation that accepts a continuation
	 *        that tracks completion of parsing.
	 * @param supplyAnswer
	 *        What to do if exactly one result was produced. This is a
	 *        continuation that accepts a solution.
	 * @param afterFail
	 *        What to do after a failure has been reported.
	 */
	private void tryIfUnambiguousThen (
		final ParserState start,
		final Continuation1NotNull<Con> tryBlock,
		final Con supplyAnswer,
		final Continuation0 afterFail)
	{
		assert compilationContext.getNoMoreWorkUnits() == null;
		// Augment the start position with a variant that incorporates the
		// solution-accepting continuation.
		final MutableInt count = new MutableInt(0);
		final MutableOrNull<A_Phrase> solution = new MutableOrNull<>();
		final MutableOrNull<ParserState> afterStatement = new MutableOrNull<>();
		compilationContext.setNoMoreWorkUnits(() ->
		{
			// The counters must be read in this order for correctness.
			final long completed = compilationContext.getWorkUnitsCompleted();
			assert completed == compilationContext.getWorkUnitsQueued();
			if (compilationContext.diagnostics.pollForAbort.value())
			{
				// We may have been asked to abort subtasks by a failure in
				// another module, so we can't trust the count of solutions.
				afterFail.value();
				return;
			}
			// Ambiguity is detected and reported during the parse, and
			// should never be identified here.
			if (count.value == 0)
			{
				// No solutions were found.  Report the problems.
				compilationContext.diagnostics.reportError(afterFail);
				return;
			}
			// If a simple unambiguous solution was found, then answer
			// it forward to the continuation.
			if (count.value == 1)
			{
				assert solution.value != null;
				supplyAnswer.value(
					new CompilerSolution(
						stripNull(afterStatement.value), solution.value));
			}
			// Otherwise an ambiguity was already reported when the second
			// solution arrived (and subsequent solutions may have arrived
			// and been ignored).  Do nothing.
		});
		final Con argument = Con(
			supplyAnswer.superexpressions,
			aSolution ->
			{
				synchronized (AvailCompiler.this)
				{
					// It may look like we could hoist all but the increment
					// and capture of the count out of the synchronized
					// section, but then there wouldn't be a write fence
					// after recording the first solution.
					count.value++;
					final int myCount = count.value;
					if (myCount == 1)
					{
						// Save the first solution to arrive.
						afterStatement.value = aSolution.endState();
						solution.value = aSolution.phrase();
						return;
					}
					if (myCount == 2)
					{
						// We are exactly the second solution to arrive and
						// to increment count.value.  Report the ambiguity
						// between the previously recorded solution and this
						// one.
						reportAmbiguousInterpretations(
							aSolution.endState(),
							solution.value(),
							aSolution.phrase(),
							afterFail);
						return;
					}
					// We're at a third or subsequent solution.  Ignore it
					// since we reported the ambiguity when the second
					// solution was reached.
					assert myCount > 2;
				}
			});
		start.workUnitDo(tryBlock, argument);
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
	 *        What to do in the event of a failure that the {@linkplain
	 *        ProblemHandler problem handler} does not wish to continue.
	 * @param problemHandler
	 *        A problem handler.
	 */
	private static void extractSourceThen (
		final ResolvedModuleName resolvedName,
		final Continuation1NotNull<String> continuation,
		final Continuation0 fail,
		final ProblemHandler problemHandler)
	{
		final AvailRuntime runtime = currentRuntime();
		final File ref = resolvedName.sourceReference();
		final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		final ByteBuffer input = ByteBuffer.allocateDirect(4096);
		final CharBuffer output = CharBuffer.allocate(4096);
		final MutableLong filePosition = new MutableLong(0L);
		final AsynchronousFileChannel file;
		try
		{
			file = runtime.openFile(
				ref.toPath(), EnumSet.of(StandardOpenOption.READ));
		}
		catch (final IOException e)
		{
			final Problem problem = new Problem(
				resolvedName,
				1,
				0,
				PARSE,
				"Unable to open source module \"{0}\" [{1}]: {2}",
				resolvedName,
				ref.getAbsolutePath(),
				e.getLocalizedMessage())
			{
				@Override
				public void abortCompilation ()
				{
					fail.value();
				}
			};
			problemHandler.handle(problem);
			return;
		}
		final MutableOrNull<CompletionHandler<Integer, Void>> handler =
			new MutableOrNull<>();
		final StringBuilder sourceBuilder = new StringBuilder(4096);
		handler.value = new CompletionHandler<Integer, Void>()
		{
			@Override
			public void completed (
				final @Nullable Integer bytesRead,
				final @Nullable Void nothing)
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
					// UTF-8 never compresses data, so the number of
					// characters encoded can be no greater than the number
					// of bytes encoded. The input buffer and the output
					// buffer are equally sized (in units), so an overflow
					// cannot occur.
					assert !result.isOverflow();
					assert !result.isError();
					// If the decoder didn't consume all of the bytes, then
					// preserve the unconsumed bytes in the next buffer (for
					// decoding).
					if (input.hasRemaining())
					{
						input.compact();
					}
					else
					{
						input.clear();
					}
					output.flip();
					sourceBuilder.append(output);
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
						sourceBuilder.append(output);
						file.close();
						runtime.execute(
							FiberDescriptor.compilerPriority,
							() -> continuation.value(sourceBuilder.toString()));
					}
				}
				catch (final IOException e)
				{
					final Problem problem = new Problem(
						resolvedName,
						1,
						0,
						PARSE,
						"Invalid UTF-8 encoding in source module "
						+ "\"{0}\": {1}\n{2}",
						resolvedName,
						e.getLocalizedMessage(),
						trace(e))
					{
						@Override
						public void abortCompilation ()
						{
							fail.value();
						}
					};
					problemHandler.handle(problem);
				}
			}

			@Override
			public void failed (
				final @Nullable Throwable e,
				final @Nullable Void attachment)
			{
				assert e != null;
				final Problem problem = new Problem(
					resolvedName,
					1,
					0,
					EXTERNAL,
					"Unable to read source module \"{0}\": {1}\n{2}",
					resolvedName,
					e.getLocalizedMessage(),
					trace(e))
				{
					@Override
					public void abortCompilation ()
					{
						fail.value();
					}
				};
				problemHandler.handle(problem);
			}
		};
		// Kick off the asynchronous read.
		file.read(input, 0L, null, handler.value);
	}

	/**
	 * Map the entire phrase through the (destructive) transformation specified
	 * by aBlock, children before parents. The block takes three arguments: the
	 * phrase, its parent, and the list of enclosing block phrases. Answer the
	 * recursively transformed phrase.
	 *
	 * @param object
	 *        The current {@linkplain PhraseDescriptor phrase}.
	 * @param transformer
	 *        What to do with each descendant.
	 * @param parentPhrase
	 *        This phrase's parent.
	 * @param outerPhrases
	 *        The list of {@linkplain BlockPhraseDescriptor blocks} surrounding
	 *        this phrase, from outermost to innermost.
	 * @param phraseMap
	 *        The {@link Map} from old {@linkplain PhraseDescriptor phrases} to
	 *        newly copied, mutable phrases.  This should ensure the consistency
	 *        of declaration references.
	 * @return A replacement for this phrase, possibly this phrase itself.
	 */
	private static A_Phrase treeMapWithParent (
		final A_Phrase object,
		final Transformer3<A_Phrase, A_Phrase, List<A_Phrase>, A_Phrase>
			transformer,
		final A_Phrase parentPhrase,
		final List<A_Phrase> outerPhrases,
		final Map<A_Phrase, A_Phrase> phraseMap)
	{
		if (phraseMap.containsKey(object))
		{
			return phraseMap.get(object);
		}
		final A_Phrase objectCopy = object.copyMutablePhrase();
		objectCopy.childrenMap(
			child ->
			{
				assert child != null;
				assert child.isInstanceOfKind(PARSE_PHRASE.mostGeneralType());
				return treeMapWithParent(
					child, transformer, objectCopy, outerPhrases, phraseMap);
			});
		final A_Phrase transformed = transformer.valueNotNull(
			objectCopy, parentPhrase, outerPhrases);
		transformed.makeShared();
		phraseMap.put(object, transformed);
		return transformed;
	}

	/**
	 * A statement was parsed correctly in two different ways. There may be more
	 * ways, but we stop after two as it's already an error. Report the error.
	 *
	 * @param where
	 *        Where the expressions were parsed from.
	 * @param interpretation1
	 *        The first interpretation as a {@linkplain PhraseDescriptor
	 *        phrase}.
	 * @param interpretation2
	 *        The second interpretation as a {@linkplain PhraseDescriptor
	 *        phrase}.
	 * @param afterFail
	 *        What to do after reporting the failure.
	 */
	private void reportAmbiguousInterpretations (
		final ParserState where,
		final A_Phrase interpretation1,
		final A_Phrase interpretation2,
		final Continuation0 afterFail)
	{
		final Mutable<A_Phrase> phrase1 = new Mutable<>(interpretation1);
		final Mutable<A_Phrase> phrase2 = new Mutable<>(interpretation2);
		findParseTreeDiscriminants(phrase1, phrase2);
		where.expected(
			continuation ->
			{
				final List<A_Phrase> phrases =
					asList(phrase1.value, phrase2.value);
				stringifyThen(
					runtime,
					compilationContext.getTextInterface(),
					phrases,
					phraseStrings ->
						continuation.value(
							"unambiguous interpretation.  "
								+ "Here are two possible parsings...\n\t"
								+ phraseStrings.get(0)
								+ "\n\t"
								+ phraseStrings.get(1)));
			});
		compilationContext.diagnostics.reportError(afterFail);
	}

	/**
	 * Given two unequal phrases, find the smallest descendant phrases that
	 * still contain all the differences.  The given {@link Mutable} objects
	 * initially contain references to the root phrases, but are updated to
	 * refer to the most specific pair of phrases that contain all the
	 * differences.
	 *
	 * @param phrase1
	 *        A {@code Mutable} reference to a {@linkplain PhraseDescriptor
	 *        phrase}.  Updated to hold the most specific difference.
	 * @param phrase2
	 *        The {@code Mutable} reference to the other phrase. Updated to hold
	 *        the most specific difference.
	 */
	private static void findParseTreeDiscriminants (
		final Mutable<A_Phrase> phrase1,
		final Mutable<A_Phrase> phrase2)
	{
		while (true)
		{
			assert !phrase1.value.equals(phrase2.value);
			if (!phrase1.value.phraseKind().equals(
				phrase2.value.phraseKind()))
			{
				// The phrases are different kinds, so present them as what's
				// different.
				return;
			}
			if (phrase1.value.isMacroSubstitutionNode()
				&& phrase2.value.isMacroSubstitutionNode())
			{
				if (phrase1.value.apparentSendName().equals(
					phrase2.value.apparentSendName()))
				{
					// Two occurrences of the same macro.  Drill into the
					// resulting phrases.
					phrase1.value = phrase1.value.outputPhrase();
					phrase2.value = phrase2.value.outputPhrase();
					continue;
				}
				// Otherwise the macros are different and we should stop.
				return;
			}
			if (phrase1.value.isMacroSubstitutionNode()
				|| phrase2.value.isMacroSubstitutionNode())
			{
				// They aren't both macros, but one is, so they're different.
				return;
			}
			if (phrase1.value.phraseKindIsUnder(SEND_PHRASE)
				&& !phrase1.value.bundle().equals(phrase2.value.bundle()))
			{
				// They're sends of different messages, so don't go any deeper.
				return;
			}
			final List<A_Phrase> parts1 = new ArrayList<>();
			phrase1.value.childrenDo(parts1::add);
			final List<A_Phrase> parts2 = new ArrayList<>();
			phrase2.value.childrenDo(parts2::add);
			final boolean isBlock =
				phrase1.value.phraseKindIsUnder(BLOCK_PHRASE);
			if (parts1.size() != parts2.size() && !isBlock)
			{
				// Different structure at this level.
				return;
			}
			final List<Integer> differentIndices = new ArrayList<>();
			for (int i = 0; i < min(parts1.size(), parts2.size()); i++)
			{
				if (!parts1.get(i).equals(parts2.get(i)))
				{
					differentIndices.add(i);
				}
			}
			if (isBlock)
			{
				if (differentIndices.size() == 0)
				{
					// Statement or argument lists are probably different sizes.
					// Use the block itself.
					return;
				}
				// Show the first argument or statement that differs.
				// Fall through.
			}
			else if (differentIndices.size() != 1)
			{
				// More than one part differs, so we can't drill deeper.
				return;
			}
			// Drill into the only part that differs.
			phrase1.value = parts1.get(differentIndices.get(0));
			phrase2.value = parts2.get(differentIndices.get(0));
		}
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
		final AvailLoader newLoader = new AvailLoader(
			compilationContext.module(),
			compilationContext.getTextInterface());
		newLoader.compilationContext(compilationContext);
		compilationContext.setLoader(newLoader);
	}

	/**
	 * Rollback the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction()}.
	 *
	 * @param afterRollback
	 *        What to do after rolling back.
	 */
	private void rollbackModuleTransaction (
		final Continuation0 afterRollback)
	{
		compilationContext
			.module()
			.removeFrom(compilationContext.loader(), afterRollback);
	}

	/**
	 * Commit the {@linkplain ModuleDescriptor module} that was defined since
	 * the most recent {@link #startModuleTransaction()}.
	 */
	private void commitModuleTransaction ()
	{
		runtime.addModule(compilationContext.module());
	}

	/**
	 * Evaluate the specified semantic restriction {@linkplain
	 * FunctionDescriptor function} in the module's context; lexically enclosing
	 * variables are not considered in scope, but module variables and constants
	 * are in scope.
	 *
	 * @param restriction
	 *        A {@linkplain SemanticRestrictionDescriptor semantic restriction}.
	 * @param args
	 *        The arguments to the function.
	 * @param onSuccess
	 *        What to do with the result of the evaluation.
	 * @param onFailure
	 *        What to do with a terminal {@link Throwable}.
	 */
	private void evaluateSemanticRestrictionFunctionThen (
		final A_SemanticRestriction restriction,
		final List<? extends A_BasicObject> args,
		final Continuation1NotNull<AvailObject> onSuccess,
		final Continuation1NotNull<Throwable> onFailure)
	{
		final A_Function function = restriction.function();
		final A_RawFunction code = function.code();
		final A_Module mod = code.module();
		final A_Fiber fiber = newLoaderFiber(
			function.kind().returnType(),
			compilationContext.loader(),
			() ->
				formatString("Semantic restriction %s, in %s:%d",
					restriction.definitionMethod().bundles()
						.iterator().next().message(),
					mod.equals(nil)
						? "no module"
						: mod.moduleName(), code.startingLineNumber()));
		fiber.setGeneralFlag(GeneralFlag.CAN_REJECT_PARSE);
		fiber.textInterface(compilationContext.getTextInterface());
		fiber.resultContinuation(onSuccess);
		fiber.failureContinuation(onFailure);
		runOutermostFunction(runtime, fiber, function, args);
	}

	/**
	 * Evaluate the specified macro {@linkplain FunctionDescriptor function} in
	 * the module's context; lexically enclosing variables are not considered in
	 * scope, but module variables and constants are in scope.
	 *
	 * @param macro
	 *        A {@linkplain MacroDefinitionDescriptor macro definition}.
	 * @param args
	 *        The argument phrases to supply the macro.
	 * @param clientParseData
	 *        The map to associate with the {@link
	 *        SpecialAtom#CLIENT_DATA_GLOBAL_KEY} atom in the fiber.
	 * @param clientParseDataOut
	 *        A {@link MutableOrNull} into which we will store an {@link A_Map}
	 *        when the fiber completes successfully.  The map will be the
	 *        content of the fiber variable holding the client data, extracted
	 *        just after the fiber completes.  If unsuccessful, don't assign to
	 *        the {@code MutableOrNull}.
	 * @param onSuccess
	 *        What to do with the result of the evaluation, a {@linkplain
	 *        A_Phrase phrase}.
	 * @param onFailure
	 *        What to do with a terminal {@link Throwable}.
	 */
	private void evaluateMacroFunctionThen (
		final A_Definition macro,
		final List<? extends A_Phrase> args,
		final A_Map clientParseData,
		final MutableOrNull<A_Map> clientParseDataOut,
		final Continuation1NotNull<AvailObject> onSuccess,
		final Continuation1NotNull<Throwable> onFailure)
	{
		final A_Function function = macro.bodyBlock();
		final A_RawFunction code = function.code();
		final A_Module mod = code.module();
		final A_Fiber fiber = newLoaderFiber(
			function.kind().returnType(),
			compilationContext.loader(),
			() -> formatString("Macro evaluation %s, in %s:%d",
				macro.definitionMethod().bundles()
					.iterator().next().message(),
				mod.equals(nil)
					? "no module"
					: mod.moduleName(), code.startingLineNumber()));
		fiber.setGeneralFlag(GeneralFlag.CAN_REJECT_PARSE);
		fiber.setGeneralFlag(GeneralFlag.IS_EVALUATING_MACRO);
		A_Map fiberGlobals = fiber.fiberGlobals();
		fiberGlobals = fiberGlobals.mapAtPuttingCanDestroy(
			CLIENT_DATA_GLOBAL_KEY.atom, clientParseData, true);
		fiber.fiberGlobals(fiberGlobals);
		fiber.textInterface(compilationContext.getTextInterface());
		fiber.resultContinuation(outputPhrase ->
		{
			clientParseDataOut.value = fiber
				.fiberGlobals()
				.mapAt(CLIENT_DATA_GLOBAL_KEY.atom);
			onSuccess.value(outputPhrase);
		});
		fiber.failureContinuation(onFailure);
		runOutermostFunction(runtime, fiber, function, args);
	}

	/**
	 * Evaluate a phrase. It's a top-level statement in a module. Declarations
	 * are handled differently - they cause a variable to be declared in the
	 * module's scope.
	 *
	 * @param startState
	 *        The start {@link ParserState}, for line number reporting.
	 * @param afterStatement
	 *        The {@link ParserState} just after the statement.
	 * @param expression
	 *        The expression to compile and evaluate as a top-level statement in
	 *        the module.
	 * @param declarationRemap
	 *        A {@link Map} holding the isomorphism between phrases and their
	 *        replacements.  This is especially useful for keeping track of how
	 *        to transform references to prior declarations that have been
	 *        transformed from local-scoped to module-scoped.
	 * @param onSuccess
	 *        What to do after success. Note that the result of executing the
	 *        statement must be {@linkplain NilDescriptor#nil nil}, so there
	 *        is no point in having the continuation accept this value, hence
	 *        the {@linkplain Continuation0 nullary continuation}.
	 * @param afterFail
	 *        What to do after execution of the top-level statement fails.
	 */
	private void evaluateModuleStatementThen (
		final ParserState startState,
		final ParserState afterStatement,
		final A_Phrase expression,
		final Map<A_Phrase, A_Phrase> declarationRemap,
		final Continuation0 onSuccess,
		final Continuation0 afterFail)
	{
		assert !expression.isMacroSubstitutionNode();
		// The mapping through declarationRemap has already taken place.
		final A_Phrase replacement = treeMapWithParent(
			expression,
			(phrase, parent, outerBlocks) -> phrase,
			nil,
			new ArrayList<>(),
			declarationRemap);

		final Continuation1NotNull<Throwable> phraseFailure =
			e ->
			{
				if (e instanceof AvailAssertionFailedException)
				{
					compilationContext.reportAssertionFailureProblem(
						startState.lineNumber(),
						startState.position(),
						(AvailAssertionFailedException) e);
				}
				else if (e instanceof AvailEmergencyExitException)
				{
					compilationContext.reportEmergencyExitProblem(
						startState.lineNumber(),
						startState.position(),
						(AvailEmergencyExitException) e);
				}
				else
				{
					compilationContext.reportExecutionProblem(
						startState.lineNumber(),
						startState.position(),
						e);
				}
				afterFail.value();
			};

		if (!replacement.phraseKindIsUnder(DECLARATION_PHRASE))
		{
			// Only record module statements that aren't declarations. Users of
			// the module don't care if a module variable or constant is only
			// reachable from the module's methods.
			compilationContext.evaluatePhraseThen(
				replacement,
				startState.lineNumber(),
				true,
				ignored -> onSuccess.value(),
				phraseFailure);
			return;
		}
		// It's a declaration, but the parser couldn't previously tell that it
		// was at module scope.  Serialize a function that will cause the
		// declaration to happen, so that references to the global
		// variable/constant from a subsequent module will be able to find it by
		// name.
		final A_Module module = compilationContext.module();
		final AvailLoader loader = compilationContext.loader();
		final A_String name = replacement.token().string();
		final @Nullable String shadowProblem =
			module.variableBindings().hasKey(name)
				? "module variable"
				: module.constantBindings().hasKey(name)
					? "module constant"
					: null;
		switch (replacement.declarationKind())
		{
			case LOCAL_CONSTANT:
			{
				if (shadowProblem != null)
				{
					afterStatement.expected(
						"new module constant "
						+ name
						+ " not to have same name as existing "
						+ shadowProblem);
					compilationContext.diagnostics.reportError(afterFail);
					return;
				}
				loader.startRecordingEffects();
				compilationContext.evaluatePhraseThen(
					replacement.initializationExpression(),
					replacement.token().lineNumber(),
					false,
					val ->
					{
						loader.stopRecordingEffects();
						final boolean canSummarize =
							loader.statementCanBeSummarized();
						final A_Type innerType = instanceTypeOrMetaOn(val);
						final A_Type varType = variableTypeFor(innerType);
						final A_Phrase creationSend = newSendNode(
							emptyTuple(),
							CREATE_MODULE_VARIABLE.bundle,
							newListNode(
								tuple(
									syntheticLiteralNodeFor(module),
									syntheticLiteralNodeFor(name),
									syntheticLiteralNodeFor(varType),
									syntheticLiteralNodeFor(trueObject()),
									syntheticLiteralNodeFor(
										objectFromBoolean(canSummarize)))),
							TOP.o());
						final A_Function creationFunction =
							createFunctionForPhrase(
								creationSend,
								module,
								replacement.token().lineNumber());
						// Force the declaration to be serialized.
						compilationContext.serializeWithoutSummary(
							creationFunction);
						final A_Variable var =
							createGlobal(varType, module, name, true);
						var.valueWasStablyComputed(canSummarize);
						module.addConstantBinding(name, var);
						// Update the map so that the local constant goes to
						// a module constant.  Then subsequent statements in
						// this sequence will transform uses of the constant
						// appropriately.
						final A_Phrase newConstant =
							newModuleConstant(
								replacement.token(),
								var,
								replacement.initializationExpression());
						declarationRemap.put(expression, newConstant);
						// Now create a module variable declaration (i.e.,
						// cheat) JUST for this initializing assignment.
						final A_Phrase newDeclaration =
							newModuleVariable(
								replacement.token(),
								var,
								nil,
								replacement.initializationExpression());
						final A_Phrase assign =
							newAssignment(
								newUse(replacement.token(), newDeclaration),
								syntheticLiteralNodeFor(val),
								expression.tokens(),
								false);
						final A_Function assignFunction =
							createFunctionForPhrase(
								assign,
								module,
								replacement.token().lineNumber());
						compilationContext.serializeWithoutSummary(
							assignFunction);
						var.setValue(val);
						onSuccess.value();
					},
					phraseFailure);
				break;
			}
			case LOCAL_VARIABLE:
			{
				if (shadowProblem != null)
				{
					afterStatement.expected(
						"new module variable "
						+ name
						+ " not to have same name as existing "
						+ shadowProblem);
					compilationContext.diagnostics.reportError(afterFail);
					return;
				}
				final A_Type varType = variableTypeFor(
					replacement.declaredType());
				final A_Phrase creationSend = newSendNode(
					emptyTuple(),
					CREATE_MODULE_VARIABLE.bundle,
					newListNode(
						tuple(
							syntheticLiteralNodeFor(module), syntheticLiteralNodeFor
								(name),
							syntheticLiteralNodeFor(varType),
							syntheticLiteralNodeFor(falseObject()),
							syntheticLiteralNodeFor(falseObject()))),
					TOP.o());
				final A_Function creationFunction =
					createFunctionForPhrase(
						creationSend,
						module,
						replacement.token().lineNumber());
				creationFunction.makeImmutable();
				// Force the declaration to be serialized.
				compilationContext.serializeWithoutSummary(creationFunction);
				final A_Variable var =
					createGlobal(
						varType, module, name, false);
				module.addVariableBinding(name, var);
				if (!replacement.initializationExpression().equalsNil())
				{
					final A_Phrase newDeclaration =
						newModuleVariable(
							replacement.token(),
							var,
							replacement.typeExpression(),
							replacement.initializationExpression());
					declarationRemap.put(expression, newDeclaration);
					final A_Phrase assign = newAssignment(
						newUse(
							replacement.token(),
							newDeclaration),
							replacement.initializationExpression(),
						tuple(expression.token()),
						false);
					final A_Function assignFunction =
						createFunctionForPhrase(
							assign, module, replacement.token().lineNumber());
					compilationContext.evaluatePhraseThen(
						replacement.initializationExpression(),
						replacement.token().lineNumber(),
						false,
						val ->
						{
							var.setValue(val);
							compilationContext.serializeWithoutSummary(
								assignFunction);
							onSuccess.value();
						},
						phraseFailure);
				}
				else
				{
					onSuccess.value();
				}
				break;
			}
			default:
				assert false
					: "Expected top-level declaration to have been "
						+ "parsed as local";
		}
	}

	/**
	 * Report that the parser was expecting one of several keywords. The
	 * keywords are keys of the {@linkplain MapDescriptor map} argument {@code
	 * incomplete}.
	 *
	 * @param where
	 *        Where the keywords were expected.
	 * @param incomplete
	 *        A map of partially parsed keywords, where the keys are the strings
	 *        that were expected at this position.
	 * @param caseInsensitive
	 *        {@code true} if the parsed keywords are case-insensitive, {@code
	 *        false} otherwise.
	 * @param excludedStrings
	 *        The {@link Set} of {@link A_String}s to omit from the message,
	 *        since they were the actual encountered tokens' texts.  Note that
	 *        this set may have multiple elements because multiple lexers may
	 *        have produced competing tokens at this position.
	 */
	private void expectedKeywordsOf (
		final ParserState where,
		final A_Map incomplete,
		final boolean caseInsensitive,
		final Set<A_String> excludedStrings)
	{
		where.expected(c ->
		{
			final StringBuilder builder = new StringBuilder(200);
			if (caseInsensitive)
			{
				builder.append("one of the following case-insensitive tokens:");
			}
			else
			{
				builder.append("one of the following tokens:");
			}
			final List<String> sorted = new ArrayList<>(incomplete.mapSize());
			final boolean detail = incomplete.mapSize() < 10;
			for (final Entry entry : incomplete.mapIterable())
			{
				final A_String availTokenString = entry.key();
				if (!excludedStrings.contains(availTokenString))
				{
					if (!detail)
					{
						sorted.add(availTokenString.asNativeString());
						continue;
					}
					// Collect the plans-in-progress and deduplicate
					// them by their string representation (including
					// the indicator at the current parsing location).
					// We can't just deduplicate by bundle, since the
					// current bundle tree might be eligible for
					// continued parsing at multiple positions.
					final Set<String> strings = new HashSet<>();
					final A_BundleTree nextTree = entry.value();
					for (final Entry successorBundleEntry :
						nextTree.allParsingPlansInProgress().mapIterable())
					{
						final A_Bundle bundle = successorBundleEntry.key();
						for (final Entry definitionEntry :
							successorBundleEntry.value().mapIterable())
						{
							for (final A_ParsingPlanInProgress inProgress
								: definitionEntry.value())
							{
								final A_ParsingPlanInProgress
									previousPlan =
										newPlanInProgress(
												inProgress.parsingPlan(),
												max(
													inProgress.parsingPc() - 1,
													1));
								final A_Module issuingModule =
									bundle.message().issuingModule();
								final String moduleName =
									issuingModule.equalsNil()
										? "(built-in)"
										: issuingModule.moduleName()
											.asNativeString();
								final String shortModuleName =
									moduleName.substring(
										moduleName.lastIndexOf('/') + 1);
								strings.add(
									previousPlan.nameHighlightingPc()
										+ " from "
										+ shortModuleName);
							}
						}
					}
					final List<String> sortedStrings = new ArrayList<>(strings);
					Collections.sort(sortedStrings);
					final StringBuilder buffer = new StringBuilder();
					buffer.append(availTokenString.asNativeString());
					buffer.append("  (");
					boolean first = true;
					for (final String progressString : sortedStrings)
					{
						if (!first)
						{
							buffer.append(", ");
						}
						buffer.append(progressString);
						first = false;
					}
					buffer.append(')');
					sorted.add(buffer.toString());
				}
			}
			Collections.sort(sorted);
			boolean startOfLine = true;
			final int leftColumn = 4 + 4; // ">>> " and a tab.
			int column = leftColumn;
			for (final String s : sorted)
			{
				if (startOfLine)
				{
					builder.append("\n\t");
					column = leftColumn;
				}
				else
				{
					builder.append("  ");
					column += 2;
				}
				startOfLine = false;
				final int lengthBefore = builder.length();
				builder.append(s);
				column += builder.length() - lengthBefore;
				if (detail || column + 2 + s.length() > 80)
				{
					startOfLine = true;
				}
			}
			compilationContext.eventuallyDo(
				where.lexingState,
				() -> c.value(builder.toString()));
		});
	}

	/**
	 * Pre-build the state of the initial parse stack.  Now that the top-most
	 * arguments get concatenated into a list, simply start with a list
	 * containing one empty list phrase.
	 */
	private static final List<A_Phrase> initialParseStack =
		Collections.singletonList(emptyListNode());

	/**
	 * Pre-build the state of the initial mark stack.  This stack keeps track of
	 * parsing positions to detect if progress has been made at certain points.
	 * This mechanism serves to prevent empty expressions from being considered
	 * an occurrence of a repeated or optional subexpression, even if it would
	 * otherwise be recognized as such.
	 */
	private static final List<Integer> initialMarkStack = emptyList();

	/**
	 * Parse a send phrase. To prevent infinite left-recursion and false
	 * ambiguity, we only allow a send with a leading keyword to be parsed from
	 * here, since leading underscore sends are dealt with iteratively
	 * afterward.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do after parsing a complete send phrase.
	 */
	private void parseLeadingKeywordSendThen (
		final ParserState start,
		final Con continuation)
	{
		A_Map clientMap = start.clientDataMap;
		// Start accumulating tokens related to this leading-keyword message
		// send at its first token.
		clientMap = clientMap.mapAtPuttingCanDestroy(
			ALL_TOKENS_KEY.atom, emptyTuple(), false);
		parseRestOfSendNode(
			start.withMap(clientMap),
			compilationContext.loader().rootBundleTree(),
			null,
			start,
			false,  // Nothing consumed yet.
			emptyList(),
			initialParseStack,
			initialMarkStack,
			Con(
				new PartialSubexpressionList(
					compilationContext.loader().rootBundleTree(),
					continuation.superexpressions),
				continuation));
	}

	/**
	 * Parse a send phrase whose leading argument has already been parsed.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param leadingArgument
	 *        The argument that was already parsed.
	 * @param initialTokenPosition
	 *        Where the leading argument started.
	 * @param continuation
	 *        What to do after parsing a send phrase.
	 */
	private void parseLeadingArgumentSendAfterThen (
		final ParserState start,
		final A_Phrase leadingArgument,
		final ParserState initialTokenPosition,
		final Con continuation)
	{
		assert start.lexingState != initialTokenPosition.lexingState;
		A_Map clientMap = start.clientDataMap;
		// Start accumulating tokens related to this leading-argument message
		// send after the leading argument.
		clientMap = clientMap.mapAtPuttingCanDestroy(
			ALL_TOKENS_KEY.atom, emptyTuple(), false);
		parseRestOfSendNode(
			start.withMap(clientMap),
			compilationContext.loader().rootBundleTree(),
			leadingArgument,
			initialTokenPosition,
			false,  // Leading argument does not yet count as something parsed.
			emptyList(),
			initialParseStack,
			initialMarkStack,
			Con(
				new PartialSubexpressionList(
					compilationContext.loader().rootBundleTree(),
					continuation.superexpressions),
				continuation));
	}

	/**
	 * Parse an expression with an optional leading-argument message send around
	 * it. Backtracking will find all valid interpretations.
	 *
	 * @param startOfLeadingArgument
	 *        Where the leading argument started.
	 * @param afterLeadingArgument
	 *        Just after the leading argument.
	 * @param phrase
	 *        An expression that acts as the first argument for a potential
	 *        leading-argument message send, or possibly a chain of them.
	 * @param continuation
	 *        What to do with either the passed phrase, or the phrase wrapped in
	 *        a leading-argument send.
	 */
	private void parseOptionalLeadingArgumentSendAfterThen (
		final ParserState startOfLeadingArgument,
		final ParserState afterLeadingArgument,
		final A_Phrase phrase,
		final Con continuation)
	{
		// It's optional, so try it with no wrapping.  We have to try this even
		// if it's a supercast, since we may be parsing an expression to be a
		// non-leading argument of some send.
		afterLeadingArgument.workUnitDo(
			continuation,
			new CompilerSolution(afterLeadingArgument, phrase));
		// Try to wrap it in a leading-argument message send.
		final Con con = Con(
			continuation.superexpressions,
			solution2 -> parseLeadingArgumentSendAfterThen(
				solution2.endState(),
				solution2.phrase(),
				startOfLeadingArgument,
				Con(
					continuation.superexpressions,
					solutionAfter -> parseOptionalLeadingArgumentSendAfterThen(
						startOfLeadingArgument,
						solutionAfter.endState(),
						solutionAfter.phrase(),
						continuation))));
		afterLeadingArgument.workUnitDo(
			con,
			new CompilerSolution(afterLeadingArgument, phrase));
	}

	/** Statistic for matching an exact token. */
	private static final Statistic matchTokenStat =
		new Statistic(
			"(Match particular token)",
			StatisticReport.RUNNING_PARSING_INSTRUCTIONS);

	/** Statistic for matching a token case-insensitively. */
	private static final Statistic matchTokenInsensitivelyStat =
		new Statistic(
			"(Match insensitive token)",
			StatisticReport.RUNNING_PARSING_INSTRUCTIONS);

	/** Statistic for type-checking an argument. */
	private static final Statistic typeCheckArgumentStat =
		new Statistic(
			"(type-check argument)",
			StatisticReport.RUNNING_PARSING_INSTRUCTIONS);

	/**
	 * We've parsed part of a send. Try to finish the job.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param bundleTreeArg
	 *        The bundle tree used to parse at this position.
	 * @param firstArgOrNull
	 *        Either null or an argument that must be consumed before any
	 *        keywords (or completion of a send).
	 * @param initialTokenPosition
	 *        The parse position where the send phrase started to be processed.
	 *        Does not count the position of the first argument if there are no
	 *        leading keywords.
	 * @param consumedAnything
	 *        Whether any actual tokens have been consumed so far for this send
	 *        phrase.  That includes any leading argument.
	 * @param consumedTokens
	 *        The immutable {@link List} of {@link A_Token}s that have been
	 *        consumed so far in this potential method/macro send.
	 * @param argsSoFar
	 *        The list of arguments parsed so far. I do not modify it. This is a
	 *        stack of expressions that the parsing instructions will assemble
	 *        into a list that correlates with the top-level non-backquoted
	 *        underscores and guillemet groups in the message name.
	 * @param marksSoFar
	 *        The stack of mark positions used to test if parsing certain
	 *        subexpressions makes progress.
	 * @param continuation
	 *        What to do with a fully parsed send phrase.
	 */
	private void parseRestOfSendNode (
		final ParserState start,
		final A_BundleTree bundleTreeArg,
		final @Nullable A_Phrase firstArgOrNull,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<A_Token> consumedTokens,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final Con continuation)
	{
		A_BundleTree bundleTree = bundleTreeArg;
		// If a bundle tree is marked as a source of a cycle, its latest
		// backward jump field is always the target.  Just continue processing
		// there and it'll never have to expand the current node.  However, it's
		// the expand() that might set up the cycle in the first place...
		bundleTree.expand(compilationContext.module());
		while (bundleTree.isSourceOfCycle())
		{
			// Jump to its (once-)equivalent ancestor.
			bundleTree = bundleTree.latestBackwardJump();
			// Give it a chance to find an equivalent ancestor of its own.
			bundleTree.expand(compilationContext.module());
			// Abort if the bundle trees have diverged.
			if (!bundleTree.allParsingPlansInProgress().equals(
				bundleTreeArg.allParsingPlansInProgress()))
			{
				// They've diverged.  Disconnect the backward link.
				bundleTreeArg.isSourceOfCycle(false);
				bundleTree = bundleTreeArg;
				break;
			}
		}

		boolean skipCheckArgumentAction = false;
		if (firstArgOrNull == null)
		{
			// A call site is only valid if at least one token has been parsed.
			if (consumedAnything)
			{
				final A_Set complete = bundleTree.lazyComplete();
				if (complete.setSize() > 0)
				{
					// There are complete messages, we didn't leave a leading
					// argument stranded, and we made progress in the file
					// (i.e., the message contains at least one token).
					assert marksSoFar.isEmpty();
					assert argsSoFar.size() == 1;
					final A_Phrase args = argsSoFar.get(0);
					for (final A_Bundle bundle : complete)
					{
						if (AvailRuntime.debugCompilerSteps)
						{
							System.out.println(
								"Completed send/macro: "
								+ bundle.message() + ' ' + args);
						}
						completedSendNode(
							initialTokenPosition,
							start,
							args,
							bundle,
							consumedTokens,
							continuation);
					}
				}
			}
			final A_Map incomplete = bundleTree.lazyIncomplete();
			if (incomplete.mapSize() > 0)
			{
				attemptToConsumeToken(
					start,
					initialTokenPosition,
					consumedAnything,
					consumedTokens,
					argsSoFar,
					marksSoFar,
					continuation,
					incomplete,
					false);
			}
			final A_Map caseInsensitive =
				bundleTree.lazyIncompleteCaseInsensitive();
			if (caseInsensitive.mapSize() > 0)
			{
				attemptToConsumeToken(
					start,
					initialTokenPosition,
					consumedAnything,
					consumedTokens,
					argsSoFar,
					marksSoFar,
					continuation,
					caseInsensitive,
					true);
			}
			final A_Map prefilter = bundleTree.lazyPrefilterMap();
			if (prefilter.mapSize() > 0)
			{
				final A_Phrase latestArgument = last(argsSoFar);
				if (latestArgument.isMacroSubstitutionNode()
					|| latestArgument
					.isInstanceOfKind(SEND_PHRASE.mostGeneralType()))
				{
					final A_Bundle argumentBundle =
						latestArgument.apparentSendName().bundleOrNil();
					assert !argumentBundle.equalsNil();
					if (prefilter.hasKey(argumentBundle))
					{
						final A_BundleTree successor =
							prefilter.mapAt(argumentBundle);
						if (AvailRuntime.debugCompilerSteps)
						{
							System.out.println(
								"Grammatical prefilter: " + argumentBundle
									+ " to " + successor);
						}
						eventuallyParseRestOfSendNode(
							start,
							successor,
							null,
							initialTokenPosition,
							consumedAnything,
							consumedTokens,
							argsSoFar,
							marksSoFar,
							continuation);
						// Don't allow any check-argument actions to be
						// processed normally, as it would ignore the
						// restriction which we'vembeen so careful to prefilter.
						skipCheckArgumentAction = true;
					}
					// The argument name was not in the prefilter map, so fall
					// through to allow normal action processing, including the
					// default check-argument action if it's present.
				}
			}
			final A_BasicObject typeFilterTreePojo =
				bundleTree.lazyTypeFilterTreePojo();
			if (!typeFilterTreePojo.equalsNil())
			{
				// Use the most recently pushed phrase's type to look up the
				// successor bundle tree.  This implements aggregated argument
				// type filtering.
				final A_Phrase latestPhrase = last(argsSoFar);
				final LookupTree<A_Tuple, A_BundleTree, A_BundleTree>
					typeFilterTree = typeFilterTreePojo.javaObjectNotNull();
				final long timeBefore = AvailRuntime.captureNanos();
				final A_BundleTree successor =
					MessageBundleTreeDescriptor.parserTypeChecker.lookupByValue(
						typeFilterTree,
						latestPhrase,
						bundleTree.latestBackwardJump());
				final long timeAfter = AvailRuntime.captureNanos();
				final AvailThread thread = (AvailThread) Thread.currentThread();
				typeCheckArgumentStat.record(
					timeAfter - timeBefore,
					thread.interpreter.interpreterIndex);
				if (AvailRuntime.debugCompilerSteps)
				{
					System.out.println(
						"Type filter: " + latestPhrase
							+ " -> " + successor);
				}
				// Don't complain if at least one plan was happy with the type
				// of the argument.  Otherwise list all argument type/plan
				// expectations as neatly as possible.
				if (successor.allParsingPlansInProgress().mapSize() == 0)
				{
					final A_BundleTree finalBundleTree = bundleTree;
					start.expected(
						continueWithDescription -> stringifyThen(
							runtime,
							compilationContext.getTextInterface(),
							latestPhrase.expressionType(),
							actualTypeString -> describeFailedTypeTestThen(
								actualTypeString,
								finalBundleTree,
								continueWithDescription)));
				}
				eventuallyParseRestOfSendNode(
					start,
					successor,
					null,
					initialTokenPosition,
					consumedAnything,
					consumedTokens,
					argsSoFar,
					marksSoFar,
					continuation);
				// Parse instruction optimization allows there to be some plans
				// that do a type filter here, but some that are able to
				// postpone it.  Therefore, also allow general actions to be
				// collected here by falling through.
			}
		}
		final A_Map actions = bundleTree.lazyActions();
		if (actions.mapSize() > 0)
		{
			for (final Entry entry : actions.mapIterable())
			{
				final int keyInt = entry.key().extractInt();
				final ParsingOperation op = decode(keyInt);
				if (skipCheckArgumentAction && op == CHECK_ARGUMENT)
				{
					// Skip this action, because the latest argument was a send
					// that had an entry in the prefilter map, so it has already
					// been dealt with.
					continue;
				}
				// Eliminate it before queueing a work unit if it shouldn't run
				// due to there being a first argument already pre-parsed.
				if (firstArgOrNull == null || op.canRunIfHasFirstArgument)
				{
					start.workUnitDo(
						value -> runParsingInstructionThen(
							start,
							keyInt,
							firstArgOrNull,
							argsSoFar,
							marksSoFar,
							initialTokenPosition,
							consumedAnything,
							consumedTokens,
							value,
							continuation),
						entry.value());
				}
			}
		}
	}

	/**
	 * Attempt to consume a token from the source.
	 *
	 * @param start
	 *        Where to start consuming the token.
	 * @param initialTokenPosition
	 *        Where the current potential send phrase started.
	 * @param consumedAnything
	 *        Whether any tokens have been consumed so far for this potential
	 *        send phrase.
	 * @param consumedTokens
	 *        An immutable {@link PrefixSharingList} of {@link A_Token}s that
	 *        have been consumed so far for te current potential send phrase.
	 *        The tokens are only those that match literal parts of the message
	 *        name or explicit token arguments.
	 * @param argsSoFar
	 *        The argument phrases that have been accumulated so far.
	 * @param marksSoFar
	 *        The mark stack.
	 * @param continuation
	 *        What to do when the current potential send phrase is complete.
	 * @param tokenMap
	 *        A map from string to message bundle tree, used for parsing tokens
	 *        when in this state.
	 */
	private void attemptToConsumeToken (
		final ParserState start,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<A_Token> consumedTokens,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final Con continuation,
		final A_Map tokenMap,
		final boolean caseInsensitive)
	{
		final AtomicBoolean ambiguousWhitespace = new AtomicBoolean(false);
		skipWhitespaceAndComments(
			start,
			afterWhiteSpaceStates ->
			{
				for (final ParserState afterWhiteSpace : afterWhiteSpaceStates)
				{
					afterWhiteSpace.lexingState.withTokensDo(tokens ->
					{
						// At least one of them must be a non-whitespace, but we
						// can completely ignore the whitespaces(/comments).
						boolean foundOne = false;
						boolean recognized = false;
						for (final A_Token token : tokens)
						{
							final TokenType tokenType = token.tokenType();
							if (tokenType == COMMENT || tokenType == WHITESPACE)
							{
								continue;
							}
							foundOne = true;
							final A_String string = caseInsensitive
								? token.lowerCaseString()
								: token.string();
							if (tokenType != KEYWORD && tokenType != OPERATOR)
							{
								continue;
							}
							if (!tokenMap.hasKey(string))
							{
								continue;
							}
							final long timeBefore = AvailRuntime.captureNanos();
							final A_BundleTree successor =
								tokenMap.mapAt(string);
							if (AvailRuntime.debugCompilerSteps)
							{
								System.out.println(
									format(
										"Matched %s token: %s @%d for %s",
										caseInsensitive ? "insensitive " : "",
										string,
										token.lineNumber(),
										successor));
							}
							recognized = true;
							// Record this token for the call site.
							A_Map clientMap = start.clientDataMap;
							clientMap = clientMap.mapAtPuttingCanDestroy(
								ALL_TOKENS_KEY.atom,
								clientMap
									.mapAt(ALL_TOKENS_KEY.atom)
									.appendCanDestroy(token, false),
								false);
							final ParserState afterToken = new ParserState(
								token.nextLexingStateIn(compilationContext),
								clientMap,
								start.capturedCommentTokens);
							eventuallyParseRestOfSendNode(
								afterToken,
								successor,
								null,
								initialTokenPosition,
								true,  // Just consumed a token.
								append(consumedTokens, token),
								argsSoFar,
								marksSoFar,
								continuation);
							final long timeAfter = AvailRuntime.captureNanos();
							final AvailThread thread =
								(AvailThread) Thread.currentThread();
							final Statistic stat = caseInsensitive
								? matchTokenInsensitivelyStat
								: matchTokenStat;
							stat.record(
								timeAfter - timeBefore,
								thread.interpreter.interpreterIndex);
						}
						assert foundOne;
						if (!recognized && consumedAnything)
						{
							final Set<A_String> strings =
								tokens.stream()
									.map(
										caseInsensitive
											? A_Token::lowerCaseString
											: A_Token::string)
									.collect(Collectors.toSet());
							expectedKeywordsOf(
								start, tokenMap, caseInsensitive, strings);
						}
					});
				}
			},
			ambiguousWhitespace);
	}

	/**
	 * Skip over whitespace and comment tokens, collecting the latter.  Produce
	 * a {@link List} of {@link ParserState}s corresponding to the possible
	 * positions after completely parsing runs of whitespaces and comments
	 * (i.e., the potential {@link A_Token}s that follows each such {@link
	 * ParserState} must include at least one token that isn't whitespace or a
	 * comment.  Invoke the continuation with this list of parser states.
	 *
	 * <p>Informally, it just skips as many whitespace and comment tokens as it
	 * can, but the nature of the ambiguous lexer makes this more subtle to
	 * express.</p>
	 *
	 * @param start
	 *        Where to start consuming the token.
	 * @param continuation
	 *        What to invoke with the collection of successor tokens.
	 * @param ambiguousWhitespace
	 *        A flag that is set if ambiguous whitespace is encountered.
	 */
	@InnerAccess void skipWhitespaceAndComments (
		final ParserState start,
		final Continuation1NotNull<List<ParserState>> continuation,
		final AtomicBoolean ambiguousWhitespace)
	{
		if (ambiguousWhitespace.get())
		{
			// Should probably be queued instead of called directly.
			continuation.value(emptyList());
			return;
		}
		start.lexingState.withTokensDo(tokens ->
		{
			final List<A_Token> toSkip = new ArrayList<>(1);
			final List<A_Token> toKeep = new ArrayList<>(1);
			for (final A_Token token : tokens)
			{
				final TokenType tokenType = token.tokenType();
				if (tokenType == COMMENT || tokenType == WHITESPACE)
				{
					if (!toSkip.isEmpty())
					{
						for (final A_Token previousToSkip : toSkip)
						{
							if (previousToSkip.string().equals(token.string()))
							{
								ambiguousWhitespace.set(true);
								if (tokenType == WHITESPACE
									&& token.string().tupleSize() < 50)
								{
									start.expected(
										"the whitespace " + token.string()
											+ " to be uniquely lexically"
											+ " scanned.  There are probably"
											+ " multiple conflicting lexers"
											+ " visible in this module.");
								}
								else if (tokenType == COMMENT
									&& token.string().tupleSize() < 100)
								{
									start.expected(
										"the comment " + token.string()
											+ " to be uniquely lexically"
											+ " scanned.  There are probably"
											+ " multiple conflicting lexers"
											+ " visible in this module.");
								}
								else
								{
									start.expected(
										"the comment or whitespace ("
											+ token.string().tupleSize()
											+ ") characters to be uniquely"
											+ " lexically scanned.  There are"
											+ " probably multiple conflicting"
											+ " lexers visible in this"
											+ " module.");
								}
								continuation.value(emptyList());
								return;
							}
						}
					}
					toSkip.add(token);
				}
				else
				{
					toKeep.add(token);
				}
			}
			if (toSkip.size() == 0)
			{
				if (toKeep.size() == 0)
				{
					start.expected(
						"a way to parse tokens here, but all lexers were "
							+ "unproductive.");
					continuation.value(emptyList());
				}
				else
				{
					// The common case where no interpretation is
					// whitespace/comment, but there's a non-whitespace token
					// (or end of file).  Allow parsing to continue right here.
					continuation.value(Collections.singletonList(start));
				}
				return;
			}
			if (toSkip.size() == 1 && toKeep.size() == 0)
			{
				// Common case of an unambiguous whitespace/comment token.
				final A_Token token = toSkip.get(0);
				skipWhitespaceAndComments(
					new ParserState(
						token.nextLexingStateIn(compilationContext),
						start.clientDataMap,
						captureIfComment(start.capturedCommentTokens, token)),
					continuation,
					ambiguousWhitespace);
				return;
			}
			// Rarer, more complicated cases with at least two interpretations,
			// at least one of which is whitespace/comment.
			final List<ParserState> result = new ArrayList<>(3);
			if (toKeep.size() > 0)
			{
				// There's at least one non-whitespace token present at start.
				result.add(start);
			}
			final MutableInt countdown = new MutableInt(toSkip.size());
			for (final A_Token tokenToSkip : toSkip)
			{
				// Common case of an unambiguous whitespace/comment token.
				final ParserState after = new ParserState(
					tokenToSkip.nextLexingStateIn(compilationContext),
					start.clientDataMap,
					captureIfComment(start.capturedCommentTokens, tokenToSkip));
				skipWhitespaceAndComments(
					after,
					partialList ->
					{
						synchronized (countdown)
						{
							result.addAll(partialList);
							countdown.value--;
							if (countdown.value == 0)
							{
								continuation.value(result);
							}
						}
					},
					ambiguousWhitespace);
			}
		});
	}

	/**
	 * Return a list with the given token appended to the comment tokens only if
	 * the new token is also a comment token.
	 *
	 * @param commentTokens
	 *        A {@link List} of comment {@link A_Token}s.
	 * @param token
	 *        An {@link A_Token} which might be a comment token.
	 * @return Either a new {@link List} with the comment token appended, or the
	 *         original, unmodified list.
	 */
	private static List<A_Token> captureIfComment (
		final List<A_Token> commentTokens,
		final A_Token token)
	{
		switch (token.tokenType())
		{
			case COMMENT:
				return append(commentTokens, token);
			case WHITESPACE:
				return commentTokens;
			default:
				throw new RuntimeException(
					"Expecting comment or whitespace token");
		}
	}

	/**
	 * A type test for a leaf argument of a potential method or macro invocation
	 * site has failed to produce any viable candidates.  Arrange to have a
	 * suitable diagnostic description of the problem produced, then passed to
	 * the given continuation.  This method may or may not return before the
	 * description has been constructed and passed to the continuation.
	 *
	 * @param actualTypeString
	 *        A String describing the actual type of the argument.
	 * @param bundleTree
	 *        The {@link A_BundleTree} at which parsing was foiled.  There may
	 *        be multiple potential methods and/or macros at this position, none
	 *        of which will have survived the type test.
	 * @param continuation
	 *        What to do once a description of the problem has been produced.
	 */
	private void describeFailedTypeTestThen (
		final String actualTypeString,
		final A_BundleTree bundleTree,
		final Continuation1NotNull<String> continuation)
	{
		// TODO(MvG) Present the full phrase type if it can be a macro argument.
		final Map<A_Type, Set<String>> definitionsByType = new HashMap<>();
		for (final Entry entry
			: bundleTree.allParsingPlansInProgress().mapIterable())
		{
			final A_Map submap = entry.value();
			for (final Entry subentry : submap.mapIterable())
			{
				final A_Set inProgressSet = subentry.value();
				for (final A_ParsingPlanInProgress planInProgress
					: inProgressSet)
				{
					final A_DefinitionParsingPlan plan =
						planInProgress.parsingPlan();
					final A_Tuple instructions = plan.parsingInstructions();
					final int instruction =
						instructions.tupleIntAt(planInProgress.parsingPc());
					final int typeIndex =
						TYPE_CHECK_ARGUMENT.typeCheckArgumentIndex(instruction);
					final A_Type argType =
						MessageSplitter.constantForIndex(typeIndex);
					Set<String> planStrings = definitionsByType.get(argType);
					if (planStrings == null)
					{
						planStrings = new HashSet<>();
						definitionsByType.put(
							argType.expressionType(), planStrings);
					}
					planStrings.add(planInProgress.nameHighlightingPc());
				}
			}
		}
		final List<A_Type> types = new ArrayList<>(definitionsByType.keySet());
		// Generate the type names in parallel.
		stringifyThen(
			runtime,
			compilationContext.getTextInterface(),
			types,
			typeNames ->
			{
				// Stitch the type names back onto the plan
				// strings, prior to sorting by type name.
				assert typeNames.size() == types.size();
				final List<Pair<String, List<String>>> pairs =
					new ArrayList<>();
				for (int i = 0; i < types.size(); i++)
				{
					final A_Type type = types.get(i);
					final List<String> planStrings =
						new ArrayList<>(definitionsByType.get(type));
					// Sort individual lists of plans.
					Collections.sort(planStrings);
					pairs.add(new Pair<>(typeNames.get(i), planStrings));
				}
				// Now sort by type names.
				pairs.sort(Comparator.comparing(Pair::first));
				// Print it all out.
				final StringBuilder builder = new StringBuilder(100);
				builder.append("phrase to have a type other than ");
				builder.append(actualTypeString);
				builder.append(".  Expecting:");
				for (final Pair<String, List<String>> pair : pairs)
				{
					builder.append("\n\t");
					builder.append(increaseIndentation(pair.first(), 2));
					for (final String planString : pair.second())
					{
						builder.append("\n\t\t");
						builder.append(planString);
					}
				}
				continuation.value(builder.toString());
			}
		);
	}

	/**
	 * Execute one non-keyword-parsing instruction, then run the continuation.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param instruction
	 *        An int encoding the {@linkplain ParsingOperation parsing
	 *        instruction} to execute.
	 * @param firstArgOrNull
	 *        Either the already-parsed first argument or null. If we're looking
	 *        for leading-argument message sends to wrap an expression then this
	 *        is not-null before the first argument position is encountered,
	 *        otherwise it's null and we should reject attempts to start with an
	 *        argument (before a keyword).
	 * @param argsSoFar
	 *        The message arguments that have been parsed so far.
	 * @param marksSoFar
	 *        The parsing markers that have been recorded so far.
	 * @param initialTokenPosition
	 *        The position at which parsing of this message started. If it was
	 *        parsed as a leading argument send (i.e., firstArgOrNull started
	 *        out non-null) then the position is of the token following the
	 *        first argument.
	 * @param consumedAnything
	 *        Whether any tokens or arguments have been consumed yet.
	 * @param consumedTokens
	 *        The immutable {@link List} of "static" {@link A_Token}s that have
	 *        been encountered and consumed for the current method or macro
	 *        invocation being parsed.  These are the tokens that correspond
	 *        with tokens that occur verbatim inside the name of the method or
	 *        macro.
	 * @param successorTrees
	 *        The {@linkplain TupleDescriptor tuple} of {@linkplain
	 *        MessageBundleTreeDescriptor bundle trees} at which to continue
	 *        parsing.
	 * @param continuation
	 *        What to do with a complete {@linkplain SendPhraseDescriptor
	 *        message send phrase}.
	 */
	private void runParsingInstructionThen (
		final ParserState start,
		final int instruction,
		final @Nullable A_Phrase firstArgOrNull,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<A_Token> consumedTokens,
		final A_Tuple successorTrees,
		final Con continuation)
	{
		final ParsingOperation op = decode(instruction);
		if (AvailRuntime.debugCompilerSteps)
		{
			if (op.ordinal() >= distinctInstructions)
			{
				System.out.println(
					"Instr @"
						+ start.shortString()
						+ ": "
						+ op.name()
						+ " ("
						+ operand(instruction)
						+ ") -> "
						+ successorTrees);
			}
			else
			{
				System.out.println(
					"Instr @"
						+ start.shortString()
						+ ": "
						+ op.name()
						+ " -> "
						+ successorTrees);
			}
		}

		final long timeBefore = AvailRuntime.captureNanos();
		op.execute(
			this,
			instruction,
			successorTrees,
			start,
			firstArgOrNull,
			argsSoFar,
			marksSoFar,
			initialTokenPosition,
			consumedAnything,
			consumedTokens,
			continuation);
		final long timeAfter = AvailRuntime.captureNanos();
		final AvailThread thread = (AvailThread) Thread.currentThread();
		op.parsingStatisticInNanoseconds.record(
			timeAfter - timeBefore,
			thread.interpreter.interpreterIndex);
	}

	/**
	 * Attempt the specified prefix function.  It may throw an {@link
	 * AvailRejectedParseException} if a specific parsing problem needs to be
	 * described.
	 *
	 * @param start
	 *        The {@link ParserState} at which the prefix function is being run.
	 * @param successorTree
	 *        The {@link A_BundleTree} with which to continue parsing.
	 * @param prefixFunction
	 *        The prefix {@link A_Function} to invoke.
	 * @param listOfArgs
	 *        The argument {@linkplain A_Phrase phrases} to pass to the prefix
	 *        function.
	 * @param firstArgOrNull
	 *        The leading argument if it has already been parsed but not
	 *        consumed.
	 * @param initialTokenPosition
	 *        The {@link ParserState} at which the current potential macro
	 *        invocation started.
	 * @param consumedAnything
	 *        Whether any tokens have been consumed so far at this macro site.
	 * @param argsSoFar
	 *        The stack of phrases.
	 * @param marksSoFar
	 *        The stack of markers that detect epsilon transitions
	 *        (subexpressions consisting of no tokens).
	 * @param continuation
	 *        What should eventually be done with the completed macro
	 *        invocation, should parsing ever get that far.
	 */
	void runPrefixFunctionThen (
		final ParserState start,
		final A_BundleTree successorTree,
		final A_Function prefixFunction,
		final List<AvailObject> listOfArgs,
		final @Nullable A_Phrase firstArgOrNull,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<A_Token> consumedTokens,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final Con continuation)
	{
		if (!prefixFunction.kind().acceptsListOfArgValues(listOfArgs))
		{
			return;
		}
		compilationContext.startWorkUnits(1);
		final A_Fiber fiber = newLoaderFiber(
			prefixFunction.kind().returnType(),
			compilationContext.loader(),
			() ->
			{
				final A_RawFunction code = prefixFunction.code();
				return
					formatString("Macro prefix %s, in %s:%d",
						code.methodName(), code.module().moduleName(),
						code.startingLineNumber());
			});
		fiber.setGeneralFlag(GeneralFlag.CAN_REJECT_PARSE);
		final A_Map withTokens = start.clientDataMap.mapAtPuttingCanDestroy(
			ALL_TOKENS_KEY.atom,
			tupleFromList(consumedTokens),
			false).makeImmutable();
		A_Map fiberGlobals = fiber.fiberGlobals();
		fiberGlobals = fiberGlobals.mapAtPuttingCanDestroy(
			CLIENT_DATA_GLOBAL_KEY.atom, withTokens, true);
		fiber.fiberGlobals(fiberGlobals);
		fiber.textInterface(compilationContext.getTextInterface());
		final AtomicBoolean hasRunEither = new AtomicBoolean(false);
		fiber.resultContinuation(compilationContext.workUnitCompletion(
			start.lexingState,
			hasRunEither,
			ignoredResult ->
			{
				// The prefix function ran successfully.
				final A_Map replacementClientDataMap =
					fiber.fiberGlobals().mapAt(CLIENT_DATA_GLOBAL_KEY.atom);
				eventuallyParseRestOfSendNode(
					start.withMap(replacementClientDataMap),
					successorTree,
					firstArgOrNull,
					initialTokenPosition,
					consumedAnything,
					consumedTokens,
					argsSoFar,
					marksSoFar,
					continuation);
			}));
		fiber.failureContinuation(compilationContext.workUnitCompletion(
			start.lexingState,
			hasRunEither,
			e ->
			{
				// The prefix function failed in some way.
				if (e instanceof AvailAcceptedParseException)
				{
					// Prefix functions are allowed to explicitly accept a
					// parse.
					final A_Map replacementClientDataMap =
						fiber.fiberGlobals().mapAt(CLIENT_DATA_GLOBAL_KEY.atom);
					eventuallyParseRestOfSendNode(
						start.withMap(replacementClientDataMap),
						successorTree,
						firstArgOrNull,
						initialTokenPosition,
						consumedAnything,
						consumedTokens,
						argsSoFar,
						marksSoFar,
						continuation);
				}
				if (e instanceof AvailRejectedParseException)
				{
					final AvailRejectedParseException stronger =
						(AvailRejectedParseException) e;
					start.expected(
						stronger.rejectionString().asNativeString());
				}
				else
				{
					start.expected(new FormattingDescriber(
						"prefix function not to have failed with:\n%s", e));
				}
			}));
		runOutermostFunction(
			runtime, fiber, prefixFunction, listOfArgs);
	}

	/**
	 * Check the proposed message send for validity. Use not only the applicable
	 * {@linkplain MethodDefinitionDescriptor method definitions}, but also any
	 * semantic restrictions. The semantic restrictions may choose to
	 * {@linkplain P_RejectParsing reject the parse}, indicating that the
	 * argument types are mutually incompatible.
	 *
	 * @param bundle
	 *        A {@linkplain MessageBundleDescriptor message bundle}.
	 * @param argTypes
	 *        The argument types.
	 * @param state
	 *        The {@linkplain ParserState parser state} after the function
	 *        evaluates successfully.
	 * @param macroOrNil
	 *        A {@link MacroDefinitionDescriptor macro definition} if this is
	 *        for a macro invocation, otherwise {@code nil}.
	 * @param originalOnSuccess
	 *        What to do with the strengthened return type.
	 * @param originalOnFailure
	 *        What to do if validation fails.
	 */
	private void validateArgumentTypes (
		final A_Bundle bundle,
		final List<? extends A_Type> argTypes,
		final A_Definition macroOrNil,
		final ParserState state,
		final Continuation1NotNull<A_Type> originalOnSuccess,
		final Continuation1NotNull<Describer> originalOnFailure)
	{
		final A_Method method = bundle.bundleMethod();
		final A_Tuple methodDefinitions = method.definitionsTuple();
		final A_Set restrictions = method.semanticRestrictions();
		// Filter the definitions down to those that are locally most specific.
		// Fail if more than one survives.
		compilationContext.startWorkUnits(1);
		final AtomicBoolean hasRunEither = new AtomicBoolean(false);
		final Continuation1NotNull<A_Type> onSuccess =
			compilationContext.workUnitCompletion(
				state.lexingState, hasRunEither, originalOnSuccess);
		final Continuation1NotNull<Describer> onFailure =
			compilationContext.workUnitCompletion(
				state.lexingState, hasRunEither, originalOnFailure);
		if (methodDefinitions.tupleSize() > 0)
		{
			// There are method definitions.
			for (
				int index = 1, end = argTypes.size();
				index <= end;
				index++)
			{
				final int finalIndex = index;
				final A_Type finalType = argTypes.get(finalIndex - 1);
				if (finalType.isBottom() || finalType.isTop())
				{
					onFailure.value(c -> stringifyThen(
						runtime,
						compilationContext.getTextInterface(),
						argTypes.get(finalIndex - 1),
						s -> c.value(format(
							"argument #%d of message %s "
							+ "to have a type other than %s",
							finalIndex,
							bundle.message().atomName(),
							s))));
					return;
				}
			}
		}
		// Find all method definitions that could match the argument types.
		// Only consider definitions that are defined in the current module or
		// an ancestor.
		final A_Set allAncestors = compilationContext.module().allAncestors();
		final List<A_Definition> filteredByTypes = macroOrNil.equalsNil()
			? method.filterByTypes(argTypes)
			: Collections.singletonList(macroOrNil);
		final List<A_Definition> satisfyingDefinitions = new ArrayList<>();
		for (final A_Definition definition : filteredByTypes)
		{
			final A_Module definitionModule = definition.definitionModule();
			if (definitionModule.equalsNil()
				|| allAncestors.hasElement(definitionModule))
			{
				satisfyingDefinitions.add(definition);
			}
		}
		if (satisfyingDefinitions.isEmpty())
		{
			onFailure.value(describeWhyDefinitionsAreInapplicable(
				bundle,
				argTypes,
				macroOrNil.equalsNil()
					? methodDefinitions
					: tuple(macroOrNil),
				allAncestors));
			return;
		}
		// Compute the intersection of the return types of the possible callees.
		// Macro bodies return phrases, but that's not what we want here.
		final Mutable<A_Type> intersection;
		if (macroOrNil.equalsNil())
		{
			intersection = new Mutable<>(
				satisfyingDefinitions.get(0).bodySignature().returnType());
			for (int i = 1, end = satisfyingDefinitions.size(); i < end; i++)
			{
				intersection.value = intersection.value.typeIntersection(
					satisfyingDefinitions.get(i).bodySignature().returnType());
			}
		}
		else
		{
			// The macro's semantic type (expressionType) is the authoritative
			// type to check against the macro body's actual return phrase's
			// semantic type.  Semantic restrictions may still narrow it below.
			intersection = new Mutable<>(
				macroOrNil.bodySignature().returnType().expressionType());
		}
		// Determine which semantic restrictions are relevant.
		final List<A_SemanticRestriction> restrictionsToTry =
			new ArrayList<>(restrictions.setSize());
		for (final A_SemanticRestriction restriction : restrictions)
		{
			final A_Module definitionModule = restriction.definitionModule();
			if (definitionModule.equalsNil()
				|| allAncestors.hasElement(restriction.definitionModule()))
			{
				if (restriction.function().kind().acceptsListOfArgValues(
					argTypes))
				{
					restrictionsToTry.add(restriction);
				}
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
		final MutableInt outstanding = new MutableInt(restrictionsToTry.size());
		final List<Describer> failureMessages = new ArrayList<>();
		final Continuation0 whenDone = () ->
		{
			assert outstanding.value == 0;
			if (failureMessages.isEmpty())
			{
				onSuccess.value(intersection.value);
				return;
			}
			onFailure.value(new Describer()
			{
				int index = 0;

				@Override
				public void describeThen (
					final Continuation1NotNull<String> continuation)
				{
					assert !failureMessages.isEmpty();
					final StringBuilder builder = new StringBuilder();
					final MutableOrNull<Continuation0> looper =
						new MutableOrNull<>();
					looper.value = () ->
						failureMessages.get(index).describeThen(
							string ->
							{
								if (index > 0)
								{
									builder.append("\n-------------------\n");
								}
								builder.append(string);
								index++;
								if (index < failureMessages.size())
								{
									looper.value().value();
								}
								else
								{
									continuation.value(builder.toString());
								}
							});
					looper.value().value();
				}
			});
		};
		final Continuation1NotNull<AvailObject> intersectAndDecrement =
			restrictionType ->
			{
				assert restrictionType.isType();
				synchronized (outstanding)
				{
					if (failureMessages.isEmpty())
					{
						intersection.value =
							intersection.value.typeIntersection(
								restrictionType);
					}
					outstanding.value--;
					if (outstanding.value == 0)
					{
						whenDone.value();
					}
				}
			};
		final Continuation1NotNull<Throwable> failAndDecrement =
			e ->
			{
				if (e instanceof AvailAcceptedParseException)
				{
					// This is really a success.
					intersectAndDecrement.value(TOP.o());
					return;
				}
				final Describer message;
				if (e instanceof AvailRejectedParseException)
				{
					final AvailRejectedParseException rej =
						(AvailRejectedParseException) e;
					message = c -> c.value(
						rej.rejectionString().asNativeString()
						+ " (while parsing send of "
						+ bundle.message().atomName()
							.asNativeString()
						+ ')');
				}
				else if (e instanceof FiberTerminationException)
				{
					message = c -> c.value(
						"semantic restriction not to raise an "
						+ "unhandled exception (while parsing "
						+ "send of "
						+ bundle.message().atomName()
							.asNativeString()
						+ "):\n\t"
						+ e);
				}
				else if (e instanceof AvailAssertionFailedException)
				{
					final AvailAssertionFailedException ex =
						(AvailAssertionFailedException) e;
					message = new SimpleDescriber(
						"assertion not to have failed "
						+ "(while parsing send of "
						+ bundle.message().atomName().asNativeString()
						+ "):\n\t"
						+ ex.assertionString().asNativeString());
				}
				else
				{
					message = new FormattingDescriber(
						"unexpected error: %s", e);
				}
				synchronized (outstanding)
				{
					failureMessages.add(message);
					outstanding.value--;
					if (outstanding.value == 0)
					{
						whenDone.value();
					}
				}
			};
		// Launch the semantic restriction in parallel.
		for (final A_SemanticRestriction restriction : restrictionsToTry)
		{
			evaluateSemanticRestrictionFunctionThen(
				restriction,
				argTypes,
				intersectAndDecrement,
				failAndDecrement);
		}
	}

	/**
	 * Given a collection of definitions, whether for methods or for macros, but
	 * not both, and given argument types (phrase types in the case of macros)
	 * for a call site, produce a reasonable explanation of why the definitions
	 * were all rejected.
	 *
	 * @param bundle
	 *        The target bundle for the call site.
	 * @param argTypes
	 *        The types of the arguments, or their phrase types if this is for a
	 *        macro lookup
	 * @param definitionsTuple
	 *        The method or macro (but not both) definitions that were visible
	 *        (defined in the current or an ancestor module) but not applicable.
	 * @param allAncestorModules
	 *        The {@linkplain A_Set set} containing the current {@linkplain
	 *        A_Module module} and its ancestors.
	 * @return
	 *        A {@link Describer} able to describe why none of the definitions
	 *        were applicable.
	 */
	private Describer describeWhyDefinitionsAreInapplicable (
		final A_Bundle bundle,
		final List<? extends A_Type> argTypes,
		final A_Tuple definitionsTuple,
		final A_Set allAncestorModules)
	{
		assert definitionsTuple.tupleSize() > 0;
		return c ->
		{
			final String kindOfDefinition =
				definitionsTuple.tupleAt(1).isMacroDefinition()
					? "macro"
					: "method";
			final List<A_Definition> allVisible = new ArrayList<>();
			for (final A_Definition def : definitionsTuple)
			{
				final A_Module definingModule = def.definitionModule();
				if (definingModule.equalsNil()
					|| allAncestorModules.hasElement(def.definitionModule()))
				{
					allVisible.add(def);
				}
			}
			final List<Integer> allFailedIndices = new ArrayList<>(3);
			each_arg:
			for (int i = 1, end = argTypes.size(); i <= end; i++)
			{
				for (final A_Definition definition : allVisible)
				{
					final A_Type sig = definition.bodySignature();
					if (argTypes.get(i - 1).isSubtypeOf(
						sig.argsTupleType().typeAtIndex(i)))
					{
						continue each_arg;
					}
				}
				allFailedIndices.add(i);
			}
			if (allFailedIndices.size() == 0)
			{
				// Each argument applied to at least one definition, so put
				// the blame on them all instead of none.
				for (int i = 1, end = argTypes.size(); i <= end; i++)
				{
					allFailedIndices.add(i);
				}
			}
			// Don't stringify all the argument types, just the failed ones.
			// And don't stringify the same value twice. Obviously side
			// effects in stringifiers won't work right here…
			final List<A_BasicObject> uniqueValues = new ArrayList<>();
			final Map<A_BasicObject, Integer> valuesToStringify =
				new HashMap<>();
			for (final int i : allFailedIndices)
			{
				final A_Type argType = argTypes.get(i - 1);
				if (!valuesToStringify.containsKey(argType))
				{
					valuesToStringify.put(argType, uniqueValues.size());
					uniqueValues.add(argType);
				}
				for (final A_Definition definition : allVisible)
				{
					final A_Type signatureArgumentsType =
						definition.bodySignature().argsTupleType();
					final A_Type sigType =
						signatureArgumentsType.typeAtIndex(i);
					if (!valuesToStringify.containsKey(sigType))
					{
						valuesToStringify.put(sigType, uniqueValues.size());
						uniqueValues.add(sigType);
					}
				}
			}
			stringifyThen(
				runtime,
				compilationContext.getTextInterface(),
				uniqueValues,
				strings ->
				{
					@SuppressWarnings({
						"resource",
						"IOResourceOpenedButNotSafelyClosed"
					})
					final Formatter builder = new Formatter();
					builder.format(
						"arguments at indices %s of message %s to "
						+ "match a visible %s definition:%n",
						allFailedIndices,
						bundle.message().atomName(),
						kindOfDefinition);
					builder.format("\tI got:%n");
					for (final int i : allFailedIndices)
					{
						final A_Type argType = argTypes.get(i - 1);
						final String s = strings.get(
							valuesToStringify.get(argType));
						builder.format("\t\t#%d = %s%n", i, s);
					}
					builder.format(
						"\tI expected%s:",
						allVisible.size() > 1 ? " one of" : "");
					for (final A_Definition definition : allVisible)
					{
						builder.format(
							"%n\t\tFrom module %s @ line #%s,",
							definition.definitionModuleName(),
							definition.isMethodDefinition()
								? definition.bodyBlock().code()
									.startingLineNumber()
								: "unknown");
						final A_Type signatureArgumentsType =
							definition.bodySignature().argsTupleType();
						for (final int i : allFailedIndices)
						{
							final A_Type sigType =
								signatureArgumentsType.typeAtIndex(i);
							final String s = strings.get(
								valuesToStringify.get(sigType));
							builder.format("%n\t\t\t#%d = %s", i, s);
						}
					}
					if (allVisible.isEmpty())
					{
						c.value(
							"[[[Internal problem - No visible implementations;"
								+ " should have been excluded.]]]\n"
								+ builder);
					}
					else
					{
						c.value(builder.toString());
					}
				});
		};
	}

	/**
	 * A complete {@linkplain SendPhraseDescriptor send phrase} has been parsed.
	 * Create the send phrase and invoke the continuation.
	 *
	 * <p>
	 * If this is a macro, invoke the body immediately with the argument
	 * expressions to produce a phrase.
	 * </p>
	 *
	 * @param stateBeforeCall
	 *        The initial parsing state, prior to parsing the entire message.
	 * @param stateAfterCall
	 *        The parsing state after the message.
	 * @param argumentsListNode
	 *        The {@linkplain ListPhraseDescriptor list phrase} that will hold
	 *        all the arguments of the new send phrase.
	 * @param bundle
	 *        The {@linkplain MessageBundleDescriptor message bundle} that
	 *        identifies the message to be sent.
	 * @param consumedTokens
	 *        The list of all tokens collected for this send phrase.  This
	 *        includes only those tokens that are operator or keyword tokens
	 *        that correspond with parts of the method name itself, not the
	 *        arguments.
	 * @param continuation
	 *        What to do with the resulting send phrase.
	 */
	private void completedSendNode (
		final ParserState stateBeforeCall,
		final ParserState stateAfterCall,
		final A_Phrase argumentsListNode,
		final A_Bundle bundle,
		final List<A_Token> consumedTokens,
		final Con continuation)
	{
		final Mutable<Boolean> valid = new Mutable<>(true);
		final A_Method method = bundle.bundleMethod();
		final A_Tuple macroDefinitionsTuple = method.macroDefinitionsTuple();
		final A_Tuple definitionsTuple = method.definitionsTuple();
		if (definitionsTuple.tupleSize() + macroDefinitionsTuple.tupleSize()
			== 0)
		{
			stateAfterCall.expected(
				"there to be a method or macro definition for "
				+ bundle.message()
				+ ", but there wasn't");
			return;
		}

		// An applicable macro definition (even if ambiguous) prevents this site
		// from being a method invocation.
		A_Definition macro = nil;
		if (macroDefinitionsTuple.tupleSize() > 0)
		{
			// Find all macro definitions that could match the argument phrases.
			// Only consider definitions that are defined in the current module
			// or an ancestor.
			final A_Set allAncestors =
				compilationContext.module().allAncestors();
			final List<A_Definition> visibleDefinitions =
				new ArrayList<>(macroDefinitionsTuple.tupleSize());
			for (final A_Definition definition : macroDefinitionsTuple)
			{
				final A_Module definitionModule = definition.definitionModule();
				if (definitionModule.equalsNil()
					|| allAncestors.hasElement(definitionModule))
				{
					visibleDefinitions.add(definition);
				}
			}
			@Nullable AvailErrorCode errorCode = null;
			if (visibleDefinitions.size() == macroDefinitionsTuple.tupleSize())
			{
				// All macro definitions are visible.  Use the lookup tree.
				try
				{
					macro = method.lookupMacroByPhraseTuple(
						argumentsListNode.expressionsTuple());
				}
				catch (final MethodDefinitionException e)
				{
					errorCode = e.errorCode();
				}
			}
			else
			{
				// Some of the macro definitions are not visible.  Search the
				// hard (but hopefully infrequent) way.
				final List<A_Type> phraseTypes =
					new ArrayList<>(method.numArgs());
				for (final A_Phrase argPhrase :
					argumentsListNode.expressionsTuple())
				{
					phraseTypes.add(
						instanceTypeOrMetaOn(argPhrase));
				}
				final List<A_Definition> filtered = new ArrayList<>();
				for (final A_Definition macroDefinition : visibleDefinitions)
				{
					if (macroDefinition.bodySignature().couldEverBeInvokedWith(
						phraseTypes))
					{
						filtered.add(macroDefinition);
					}
				}

				if (filtered.size() == 0)
				{
					// Nothing is visible.
					stateAfterCall.expected(
						"perhaps some definition of the macro "
						+ bundle.message()
						+ " to be visible");
					errorCode = E_NO_METHOD_DEFINITION;
					// Fall through.
				}
				else if (filtered.size() == 1)
				{
					macro = filtered.get(0);
				}
				else
				{
					// Find the most specific macro(s).
					// assert filtered.size() > 1;
					final List<A_Definition> mostSpecific = new ArrayList<>();
					for (final A_Definition candidate : filtered)
					{
						boolean isMostSpecific = true;
						for (final A_Definition other : filtered)
						{
							if (!candidate.equals(other))
							{
								if (candidate.bodySignature()
									.acceptsArgTypesFromFunctionType(
										other.bodySignature()))
								{
									isMostSpecific = false;
									break;
								}
							}
						}
						if (isMostSpecific)
						{
							mostSpecific.add(candidate);
						}
					}
					assert mostSpecific.size() >= 1;
					if (mostSpecific.size() == 1)
					{
						// There is one most-specific macro.
						macro = mostSpecific.get(0);
					}
					else
					{
						// There are multiple most-specific macros.
						errorCode = E_AMBIGUOUS_METHOD_DEFINITION;
					}
				}
			}

			if (macro.equalsNil())
			{
				// Failed lookup.
				if (errorCode != E_NO_METHOD_DEFINITION)
				{
					final AvailErrorCode finalErrorCode = stripNull(errorCode);
					stateAfterCall.expected(
						withString -> withString.value(
							finalErrorCode == E_AMBIGUOUS_METHOD_DEFINITION
								? "unambiguous definition of macro "
									+ bundle.message()
								: "successful macro lookup, not: "
									+ finalErrorCode.name()));
					// Don't try to treat it as a method invocation.
					return;
				}
				if (definitionsTuple.tupleSize() == 0)
				{
					// There are only macro definitions, but the arguments were
					// not the right types.
					final List<A_Type> phraseTypes =
						new ArrayList<>(method.numArgs());
					for (final A_Phrase argPhrase :
						argumentsListNode.expressionsTuple())
					{
						phraseTypes.add(instanceTypeOrMetaOn(argPhrase));
					}
					stateAfterCall.expected(
						describeWhyDefinitionsAreInapplicable(
							bundle,
							phraseTypes,
							macroDefinitionsTuple,
							allAncestors));
					// Don't report it as a failed method lookup, since there
					// were none.
					return;
				}
				// No macro definition matched, and there are method definitions
				// also possible, so fall through and treat it as a potential
				// method invocation site instead.
			}
			// Fall through to test semantic restrictions and run the macro if
			// one was found.
		}
		// It invokes a method (not a macro).  We compute the union of the
		// superUnionType() and the expressionType() for lookup, since if this
		// is a supercall we want to know what semantic restrictions and
		// function return types will be reached by the method definition(s)
		// actually being invoked.
		final A_Type argTupleType =
			argumentsListNode.superUnionType().typeUnion(
				argumentsListNode.expressionType());
		final int argCount = argumentsListNode.expressionsSize();
		final List<A_Type> argTypes = new ArrayList<>(argCount);
		for (int i = 1; i <= argCount; i++)
		{
			argTypes.add(argTupleType.typeAtIndex(i));
		}
		// Parsing a macro send must not affect the scope.
		final ParserState afterState =
			stateAfterCall.withMap(stateBeforeCall.clientDataMap);
		final A_Definition finalMacro = macro;
		// Validate the message send before reifying a send phrase.
		validateArgumentTypes(
			bundle,
			argTypes,
			finalMacro,
			stateAfterCall,
			expectedYieldType ->
			{
				if (finalMacro.equalsNil())
				{
					final A_Phrase sendNode = newSendNode(
						tupleFromList(consumedTokens),
						bundle,
						argumentsListNode,
						expectedYieldType);
					afterState.workUnitDo(
						continuation,
						new CompilerSolution(afterState, sendNode));
					return;
				}
				completedSendNodeForMacro(
					stateAfterCall,
					argumentsListNode,
					bundle,
					consumedTokens,
					finalMacro,
					expectedYieldType,
					Con(
						continuation.superexpressions,
						macroSolution ->
						{
							assert macroSolution.phrase()
								.isMacroSubstitutionNode();
							continuation.value(macroSolution);
						}));
			},
			errorGenerator ->
			{
				valid.value = false;
				stateAfterCall.expected(errorGenerator);
			});
	}

	/**
	 * Parse an argument to a message send. Backtracking will find all valid
	 * interpretations.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param kindOfArgument
	 *        A {@link String}, in the form of a noun phrase, saying the kind of
	 *        argument that is expected.
	 * @param firstArgOrNull
	 *        Either a phrase to use as the argument, or null if we should
	 *        parse one now.
	 * @param canReallyParse
	 *        Whether any tokens may be consumed.  This should be false
	 *        specifically when the leftmost argument of a leading-argument
	 *        message is being parsed.
	 * @param wrapInLiteral
	 *        Whether the argument should be wrapped inside a literal phrase.
	 *        This allows statements to be more easily processed by macros.
	 * @param continuation
	 *        What to do with the argument.
	 */
	void parseSendArgumentWithExplanationThen (
		final ParserState start,
		final String kindOfArgument,
		final @Nullable A_Phrase firstArgOrNull,
		final boolean canReallyParse,
		final boolean wrapInLiteral,
		final Con continuation)
	{
		if (firstArgOrNull != null)
		{
			// We're parsing a message send with a leading argument, and that
			// argument was explicitly provided to the parser.  We should
			// consume the provided first argument now.
			assert !canReallyParse;

			// wrapInLiteral allows us to accept anything, even expressions that
			// are ⊤- or ⊥-valued.
			if (wrapInLiteral)
			{
				start.workUnitDo(
					continuation,
					new CompilerSolution(start, wrapAsLiteral(firstArgOrNull)));
				return;
			}
			final A_Type expressionType = firstArgOrNull.expressionType();
			if (expressionType.isTop())
			{
				start.expected("leading argument not to be ⊤-valued.");
				return;
			}
			if (expressionType.isBottom())
			{
				start.expected("leading argument not to be ⊥-valued.");
				return;
			}
			start.workUnitDo(
				continuation, new CompilerSolution(start, firstArgOrNull));
			return;
		}
		// There was no leading argument, or it has already been accounted for.
		// If we haven't actually consumed anything yet then don't allow a
		// *leading* argument to be parsed here.  That would lead to ambiguous
		// left-recursive parsing.
		if (!canReallyParse)
		{
			return;
		}
		parseExpressionThen(
			start,
			Con(
				continuation.superexpressions,
				solution ->
				{
					// Only accept a ⊤-valued or ⊥-valued expression if
					// wrapInLiteral is true.
					final A_Phrase argument = solution.phrase();
					final ParserState afterArgument = solution.endState();
					if (!wrapInLiteral)
					{
						final A_Type type = argument.expressionType();
						final @Nullable String badTypeName =
							type.isTop()
								? "⊤"
								: type.isBottom() ? "⊥" : null;
						if (badTypeName != null)
						{
							final Describer describer = c ->
							{
								final StringBuilder b = new StringBuilder(100);
								b.append(kindOfArgument);
								b.append(" to have a type other than ");
								b.append(badTypeName);
								b.append(" in:");
								describeOn(continuation.superexpressions, b);
								c.value(b.toString());
							};
							afterArgument.expected(describer);
							return;
						}
					}
					final CompilerSolution argument1 =
						new CompilerSolution(
							afterArgument,
							wrapInLiteral ? wrapAsLiteral(argument) : argument);
					afterArgument.workUnitDo(continuation, argument1);
				}));
	}

	/**
	 * Transform the argument, a {@linkplain A_Phrase phrase}, into a {@link
	 * LiteralPhraseDescriptor literal phrase} whose value is the original
	 * phrase. If the given phrase is a {@linkplain
	 * MacroSubstitutionPhraseDescriptor macro substitution phrase} then extract
	 * its {@link A_Phrase#apparentSendName()}, strip off the macro
	 * substitution, wrap the resulting expression in a literal phrase, then
	 * re-apply the same apparentSendName to the new literal phrase to produce
	 * another macro substitution phrase.
	 *
	 * @param phrase
	 *        A phrase.
	 * @return A literal phrase that yields the given phrase as its value.
	 */
	private static A_Phrase wrapAsLiteral (
		final A_Phrase phrase)
	{
		if (phrase.isMacroSubstitutionNode())
		{
			return newMacroSubstitution(
				phrase.macroOriginalSendNode(),
				syntheticLiteralNodeFor(phrase));
		}
		return syntheticLiteralNodeFor(phrase);
	}

	/**
	 * Parse an argument in the top-most scope.  This is an important capability
	 * for parsing type expressions, and the macro facility may make good use
	 * of it for other purposes.
	 *
	 * @param start
	 *        The position at which parsing should occur.
	 * @param firstArgOrNull
	 *        An optional already parsed expression which, if present, must be
	 *        used as a leading argument.  If it's {@code null} then no leading
	 *        argument has been parsed, and a request to parse a leading
	 *        argument should simply produce no local solution.
	 * @param initialTokenPosition
	 *        The parse position where the send phrase started to be processed.
	 *        Does not count the position of the first argument if there are no
	 *        leading keywords.
	 * @param argsSoFar
	 *        The list of arguments parsed so far. I do not modify it. This is a
	 *        stack of expressions that the parsing instructions will assemble
	 *        into a list that correlates with the top-level non-backquoted
	 *        underscores and guillemet groups in the message name.
	 * @param marksSoFar
	 *        The stack of mark positions used to test if parsing certain
	 *        subexpressions makes progress.
	 * @param successorTrees
	 *        A {@linkplain TupleDescriptor tuple} of {@linkplain
	 *        MessageBundleTreeDescriptor message bundle trees} along which to
	 *        continue parsing if a local solution is found.
	 * @param continuation
	 *        What to do once we have a fully parsed send phrase (of which we
	 *        are currently parsing an argument).
	 */
	void parseArgumentInModuleScopeThen (
		final ParserState start,
		final @Nullable A_Phrase firstArgOrNull,
		final List<A_Token> consumedTokens,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final ParserState initialTokenPosition,
		final A_Tuple successorTrees,
		final Con continuation)
	{
		// Parse an argument in the outermost (module) scope and continue.
		assert successorTrees.tupleSize() == 1;
		final A_Map clientDataInGlobalScope =
			start.clientDataMap.mapAtPuttingCanDestroy(
				COMPILER_SCOPE_MAP_KEY.atom,
				emptyMap(),
				false);
		parseSendArgumentWithExplanationThen(
			start.withMap(clientDataInGlobalScope),
			"module-scoped argument",
			firstArgOrNull,
			firstArgOrNull == null
				&& initialTokenPosition.lexingState != start.lexingState,
			false,  // Static argument can't be top-valued
			Con(
				continuation.superexpressions,
				solution ->
				{
					final A_Phrase newArg = solution.phrase();
					final ParserState afterArg = solution.endState();
					if (newArg.hasSuperCast())
					{
						afterArg.expected(
							"global-scoped argument, not supercast");
						return;
					}
					//noinspection VariableNotUsedInsideIf
					if (firstArgOrNull != null)
					{
						// A leading argument was already supplied.  We
						// couldn't prevent it from referring to
						// variables that were in scope during its
						// parsing, but we can reject it if the leading
						// argument is supposed to be parsed in global
						// scope, which is the case here, and there are
						// references to local variables within the
						// argument's parse tree.
						final A_Set usedLocals =
							usesWhichLocalVariables(newArg);
						if (usedLocals.setSize() > 0)
						{
							// A leading argument was supplied which
							// used at least one local.  It shouldn't
							// have.
							afterArg.expected(c ->
							{
								final List<String> localNames =
									new ArrayList<>();
								for (final A_Phrase usedLocal : usedLocals)
								{
									final A_String name =
										usedLocal.token().string();
									localNames.add(name.asNativeString());
								}
								c.value(
									"a leading argument which "
									+ "was supposed to be parsed in "
									+ "module scope, but it referred to "
									+ "some local variables: "
									+ localNames);
							});
							return;
						}
					}
					final List<A_Phrase> newArgsSoFar =
						append(argsSoFar, newArg);
					eventuallyParseRestOfSendNode(
						afterArg.withMap(start.clientDataMap),
						successorTrees.tupleAt(1),
						null,
						initialTokenPosition,
						// The argument counts as something that was
						// consumed if it's not a leading argument...
						firstArgOrNull == null,
						consumedTokens,
						newArgsSoFar,
						marksSoFar,
						continuation);
				}));
	}

	/**
	 * A macro invocation has just been parsed.  Run its body now to produce a
	 * substitute phrase.
	 *
	 * @param stateAfterCall
	 *        The parsing state after the message.
	 * @param argumentsListNode
	 *        The {@linkplain ListPhraseDescriptor list phrase} that will hold
	 *        all the arguments of the new send phrase.
	 * @param bundle
	 *        The {@linkplain MessageBundleDescriptor message bundle} that
	 *        identifies the message to be sent.
	 * @param consumedTokens
	 *        The list of all tokens collected for this send phrase.  This
	 *        includes only those tokens that are operator or keyword tokens
	 *        that correspond with parts of the method name itself, not the
	 *        arguments.
	 * @param macroDefinitionToInvoke
	 *        The actual {@link MacroDefinitionDescriptor macro definition} to
	 *        invoke (statically).
	 * @param expectedYieldType
	 *        What semantic type the expression returned from the macro
	 *        invocation is expected to yield.  This will be narrowed further by
	 *        the actual phrase returned by the macro body, although if it's not
	 *        a send phrase then the resulting phrase is <em>checked</em>
	 *        against this expected yield type instead.
	 * @param continuation
	 *        What to do with the resulting send phrase solution.
	 */
	private void completedSendNodeForMacro (
		final ParserState stateAfterCall,
		final A_Phrase argumentsListNode,
		final A_Bundle bundle,
        final List<A_Token> consumedTokens,
		final A_Definition macroDefinitionToInvoke,
		final A_Type expectedYieldType,
		final Con continuation)
	{
		final A_Tuple argumentsTuple = argumentsListNode.expressionsTuple();
		final int argCount = argumentsTuple.tupleSize();
		// Strip off macro substitution wrappers from the arguments.  These
		// were preserved only long enough to test grammatical restrictions.
		final List<A_Phrase> argumentsList = new ArrayList<>(argCount);
		for (final A_Phrase argument : argumentsTuple)
		{
			argumentsList.add(argument);
		}
		// Capture all of the tokens that comprised the entire macro send.
		final A_Tuple constituentTokens = tupleFromList(consumedTokens);
		final A_Map withTokensAndBundle = stateAfterCall.clientDataMap
			.mapAtPuttingCanDestroy(
				ALL_TOKENS_KEY.atom, constituentTokens, false)
			.mapAtPuttingCanDestroy(MACRO_BUNDLE_KEY.atom, bundle, true)
			.makeShared();
		compilationContext.startWorkUnits(1);
		final MutableOrNull<A_Map> clientDataAfterRunning =
			new MutableOrNull<>();
		final AtomicBoolean hasRunEither = new AtomicBoolean(false);
		if (AvailRuntime.debugMacroExpansions)
		{
			System.out.println(
				"PRE-EVAL:"
					+ stateAfterCall.lineNumber()
					+ '('
					+ stateAfterCall.position()
					+ ") "
					+ macroDefinitionToInvoke
					+ ' '
					+ argumentsList);
		}
		evaluateMacroFunctionThen(
			macroDefinitionToInvoke,
			argumentsList,
			withTokensAndBundle,
			clientDataAfterRunning,
			compilationContext.workUnitCompletion(
				stateAfterCall.lexingState,
				hasRunEither,
				replacement ->
				{
					assert clientDataAfterRunning.value != null;
					// In theory a fiber can produce anything, although you
					// have to mess with continuations to get it wrong.
					if (!replacement.isInstanceOfKind(
						PARSE_PHRASE.mostGeneralType()))
					{
						stateAfterCall.expected(
							Collections.singletonList(replacement),
							list -> format(
								"Macro body for %s to have "
									+ "produced a phrase, not %s",
								bundle.message(),
								list.get(0)));
						return;
					}
					final A_Phrase adjustedReplacement;
					if (replacement.phraseKindIsUnder(SEND_PHRASE))
					{
						// Strengthen the send phrase produced by the macro.
						adjustedReplacement = newSendNode(
							replacement.tokens(),
							replacement.bundle(),
							replacement.argumentsListNode(),
							replacement.expressionType().typeIntersection(
								expectedYieldType));
					}
					else if (replacement.expressionType().isSubtypeOf(
						expectedYieldType))
					{
						// No adjustment necessary.
						adjustedReplacement = replacement;
					}
					else
					{
						// Not a send phrase, so it's impossible to
						// strengthen it to what the semantic
						// restrictions promised it should be.
						stateAfterCall.expected(
							"macro "
								+ bundle.message().atomName()
								+ " to produce either a send phrase to "
								+ "be strengthened, or a phrase that "
								+ "yields "
								+ expectedYieldType
								+ ", not "
								+ replacement);
						return;
					}
					// Continue after this macro invocation with whatever
					// client data was set up by the macro.
					final ParserState stateAfter = stateAfterCall.withMap(
						clientDataAfterRunning.value());
					final A_Phrase original = newSendNode(
						constituentTokens,
						bundle,
						argumentsListNode,
						macroDefinitionToInvoke.bodySignature().returnType());
					final A_Phrase substitution =
						newMacroSubstitution(original, adjustedReplacement);
					if (AvailRuntime.debugMacroExpansions)
					{
						System.out.println(
							":"
								+ stateAfter.lineNumber()
								+ '('
								+ stateAfter.position()
								+ ") "
								+ substitution);
					}
					stateAfter.workUnitDo(
						continuation,
						new CompilerSolution(stateAfter, substitution));
				}),
			compilationContext.workUnitCompletion(
				stateAfterCall.lexingState,
				hasRunEither,
				e ->
				{
					if (e instanceof AvailAcceptedParseException)
					{
						stateAfterCall.expected(
							"macro body to reject the parse or produce "
								+ "a replacement expression, not merely "
								+ "accept its phrases like a semantic "
								+ "restriction");
					}
					else if (e instanceof AvailRejectedParseException)
					{
						final AvailRejectedParseException rej =
							(AvailRejectedParseException) e;
						stateAfterCall.expected(
							rej.rejectionString().asNativeString());
					}
					else
					{
						stateAfterCall.expected(
							"evaluation of macro body not to raise an "
								+ "unhandled exception:\n\t"
								+ e);
					}
				}));
	}

	/**
	 * Check a property of the Avail virtual machine.
	 *
	 * @param pragmaToken
	 *        The string literal token specifying the pragma.
	 * @param state
	 *        The {@linkplain ParserState state} following a parse of the
	 *        {@linkplain ModuleHeader module header}.
	 * @param propertyName
	 *        The name of the property that is being checked.
	 * @param propertyValue
	 *        A value that should be checked, somehow, for conformance.
	 * @param success
	 *        What to do after the check completes successfully.
	 * @param failure
	 *        What to do after the check completes unsuccessfully.
	 * @throws MalformedPragmaException
	 *         If there's a problem with this check pragma.
	 */
	private static void pragmaCheckThen (
		final A_Token pragmaToken,
		final ParserState state,
		final String propertyName,
		final String propertyValue,
		final Continuation0 success,
		final Continuation0 failure)
	throws MalformedPragmaException
	{
		switch (propertyName)
		{
			case "version":
				// Split the versions at commas.
				final String[] versions = propertyValue.split(",");
				for (int i = 0; i < versions.length; i++)
				{
					versions[i] = versions[i].trim();
				}
				// Put the required versions into a set.
				A_Set requiredVersions = emptySet();
				for (final String version : versions)
				{
					requiredVersions =
						requiredVersions.setWithElementCanDestroy(
							stringFrom(version),
							true);
				}
				// Ask for the guaranteed versions.
				final A_Set activeVersions = AvailRuntime.activeVersions();
				// If the intersection of the sets is empty, then the module and
				// the virtual machine are incompatible.
				if (!requiredVersions.setIntersects(activeVersions))
				{
					throw new MalformedPragmaException(
						format(
							"Module and virtual machine are not compatible; "
								+ "the virtual machine guarantees versions %s, "
								+ "but the current module requires %s",
							activeVersions,
							requiredVersions));
				}
				break;
			default:
				final Set<String> viableAssertions = new HashSet<>();
				viableAssertions.add("version");
				throw new MalformedPragmaException(
					format(
						"Expected check pragma to assert one of the following "
							+ "properties: %s",
						viableAssertions));
		}
		success.value();
	}

	/**
	 * Create a bootstrap primitive method. Use the primitive's type declaration
	 * as the argument types.  If the primitive is fallible then generate
	 * suitable primitive failure code (to invoke the {@link MethodDescriptor
	 * #vmCrashAtom()}'s bundle).
	 *
	 * @param state
	 *        The {@linkplain ParserState state} following a parse of the
	 *        {@linkplain ModuleHeader module header}.
	 * @param token
	 *        A token with which to associate the definition of the function.
	 *        Since this is a bootstrap method, it's appropriate to use the
	 *        string token within the pragma for this purpose.
	 * @param methodName
	 *        The name of the primitive method being defined.
	 * @param primitiveName
	 *        The {@linkplain Primitive#name() primitive name} of the
	 *        {@linkplain MethodDescriptor method} being defined.
	 * @param success
	 *        What to do after the method is bootstrapped successfully.
	 * @param failure
	 *        What to do if the attempt to bootstrap the method fails.
	 */
	private void bootstrapMethodThen (
		final ParserState state,
		final A_Token token,
		final String methodName,
		final String primitiveName,
		final Continuation0 success,
		final Continuation0 failure)
	{
		final A_String availName = stringFrom(methodName);
		final A_Phrase nameLiteral = syntheticLiteralNodeFor(availName);
		final Primitive primitive = stripNull(primitiveByName(primitiveName));
		final A_Function function = newPrimitiveFunction(
			primitive,
			compilationContext.module(),
			token.lineNumber());
		final A_Phrase send = newSendNode(
			emptyTuple(),
			METHOD_DEFINER.bundle,
			newListNode(
				tuple(nameLiteral, syntheticLiteralNodeFor(function))),
			TOP.o());
		evaluateModuleStatementThen(
			state, state, send, new HashMap<>(), success, failure);
	}

	/**
	 * Create a bootstrap primitive {@linkplain MacroDefinitionDescriptor
	 * macro}. Use the primitive's type declaration as the argument types.  If
	 * the primitive is fallible then generate suitable primitive failure code
	 * (to invoke the {@link SpecialMethodAtom#CRASH}'s bundle).
	 *
	 * @param state
	 *        The {@linkplain ParserState state} following a parse of the
	 *        {@linkplain ModuleHeader module header}.
	 * @param token
	 *        A token with which to associate the definition of the function(s).
	 *        Since this is a bootstrap macro (and possibly prefix functions),
	 *        it's appropriate to use the string token within the pragma for
	 *        this purpose.
	 * @param macroName
	 *        The name of the primitive macro being defined.
	 * @param primitiveNames
	 *        The array of {@linkplain String}s that are bootstrap macro names.
	 *        These correspond to the occurrences of the {@linkplain
	 *        Metacharacter#SECTION_SIGN section sign} (§) in the macro
	 *        name, plus a final body for the complete macro.
	 * @param success
	 *        What to do after the macro is defined successfully.
	 * @param failure
	 *        What to do after compilation fails.
	 */
	private void bootstrapMacroThen (
		final ParserState state,
		final A_Token token,
		final String macroName,
		final String[] primitiveNames,
		final Continuation0 success,
		final Continuation0 failure)
	{
		assert primitiveNames.length > 0;
		final A_String availName = stringFrom(macroName);
		final AvailObject token1 = literalToken(
			stringFrom(availName.toString()),
			emptyTuple(),
			emptyTuple(),
			0,
			0,
			SYNTHETIC_LITERAL,
			availName);
		final A_Phrase nameLiteral = literalNodeFromToken(token1);
		final List<A_Phrase> functionLiterals = new ArrayList<>();
		try
		{
			for (final String primitiveName: primitiveNames)
			{
				final Primitive prim =
					stripNull(primitiveByName(primitiveName));
				functionLiterals.add(
					syntheticLiteralNodeFor(
						newPrimitiveFunction(
							prim,
							compilationContext.module(),
							token.lineNumber())));
			}
		}
		catch (final RuntimeException e)
		{
			compilationContext.reportInternalProblem(
				state.lineNumber(),
				state.position(),
				e);
			failure.value();
			return;
		}
		final A_Phrase bodyLiteral =
			functionLiterals.remove(functionLiterals.size() - 1);
		final A_Phrase send = newSendNode(
			emptyTuple(),
			MACRO_DEFINER.bundle,
			newListNode(
				tuple(nameLiteral, newListNode(
					tupleFromList(functionLiterals)),
					bodyLiteral)),
			TOP.o());
		evaluateModuleStatementThen(
			state, state, send, new HashMap<>(), success, failure);
	}

	/**
	 * Create a bootstrap primitive lexer. Validate the primitive's type
	 * declaration against what's needed for a lexer function.  If either
	 * primitive is fallible then generate suitable primitive failure code for
	 * it (to invoke the {@link MethodDescriptor #vmCrashAtom()}'s bundle).
	 *
	 * <p>The filter takes a character and answers a boolean indicating whether
	 * the lexer should be attempted when that character is next in the source
	 * file.</p>
	 *
	 * <p>The body takes a character (which has already passed the filter), the
	 * entire source string, and the one-based index of the current character in
	 * the string.  It returns nothing, but it invokes a success primitive for
	 * each successful lexing (passing a tuple of tokens and the character
	 * position after what was lexed), and/or invokes a failure primitive to
	 * give specific diagnostics about what went wrong.</p>
	 *
	 * @param state
	 *        The {@linkplain ParserState state} following a parse of the
	 *        {@linkplain ModuleHeader module header}.
	 * @param token
	 *        A token with which to associate the definition of the lexer
	 *        function.  Since this is a bootstrap lexer, it's appropriate to
	 *        use the string token within the pragma for this purpose.
	 * @param lexerAtom
	 *        The name (an {@link A_Atom atom}) of the lexer being defined.
	 * @param filterPrimitiveName
	 *        The {@linkplain Primitive#name() primitive name} of the filter
	 *        for the lexer being defined.
	 * @param bodyPrimitiveName
	 *        The {@linkplain Primitive#name() primitive name} of the body of
	 *        the lexer being defined.
	 * @param success
	 *        What to do after the method is bootstrapped successfully.
	 * @param failure
	 *        What to do if the attempt to bootstrap the method fails.
	 * @throws MalformedPragmaException
	 *         If the lexer pragma cannot be created.
	 */
	private void bootstrapLexerThen (
		final ParserState state,
		final A_Token token,
		final A_Atom lexerAtom,
		final String filterPrimitiveName,
		final String bodyPrimitiveName,
		final Continuation0 success,
		final Continuation0 failure)
	throws MalformedPragmaException
	{
		// Process the filter primitive.
		final @Nullable Primitive filterPrimitive =
			primitiveByName(filterPrimitiveName);
		if (filterPrimitive == null)
		{
			throw new MalformedPragmaException(
				"Unknown lexer filter primitive name ("
					+ filterPrimitiveName
					+ ')');
		}
		final A_Type filterFunctionType =
			filterPrimitive.blockTypeRestriction();
		if (!filterFunctionType.equals(
			lexerFilterFunctionType()))
		{
			throw new MalformedPragmaException(
				"Type signature for filter primitive is invalid for lexer. "
					+ "Primitive has "
					+ filterFunctionType
					+ " but a lexer filter needs "
					+ lexerFilterFunctionType());
		}
		final A_Function filterFunction =
			newPrimitiveFunction(
				filterPrimitive,
				compilationContext.module(),
				token.lineNumber());

		// Process the body primitive.
		final @Nullable Primitive bodyPrimitive =
			primitiveByName(bodyPrimitiveName);
		if (bodyPrimitive == null)
		{
			throw new MalformedPragmaException(
				"Unknown lexer body primitive name ("
					+ bodyPrimitiveName
					+ ')');
		}
		final A_Type bodyFunctionType = bodyPrimitive.blockTypeRestriction();
		if (!bodyFunctionType.equals(lexerBodyFunctionType()))
		{
			throw new MalformedPragmaException(
				"Type signature for body primitive is invalid for lexer. "
					+ "Primitive has "
					+ bodyFunctionType
					+ " but a lexer body needs "
					+ lexerBodyFunctionType());
		}
		final A_Function bodyFunction =
			newPrimitiveFunction(
				bodyPrimitive,
				compilationContext.module(),
				token.lineNumber());

		// Process the lexer name.
		final A_Phrase nameLiteral = syntheticLiteralNodeFor(lexerAtom);

		// Build a phrase to define the lexer.
		final A_Phrase send = newSendNode(
			emptyTuple(),
			LEXER_DEFINER.bundle,
			newListNode(
				tuple(
					nameLiteral,
					syntheticLiteralNodeFor(filterFunction),
					syntheticLiteralNodeFor(bodyFunction))),
			TOP.o());
		evaluateModuleStatementThen(
			new ParserState(
				token.nextLexingStateIn(compilationContext),
				emptyMap(),
				emptyList()),
			state,
			send,
			new HashMap<>(),
			success,
			failure);
	}

	/**
	 * Apply a {@link ExpectedToken#PRAGMA_CHECK check} pragma that was detected
	 * during parse of the {@linkplain ModuleHeader module header}.
	 *
	 * @param pragmaToken
	 *        The string literal token specifying the pragma.
	 * @param pragmaValue
	 *        The pragma {@link String} after {@code "check="}.
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param success
	 *        What to do after the pragmas have been applied successfully.
	 * @param failure
	 *        What to do if a problem is found with one of the pragma
	 *        definitions.
	 * @throws MalformedPragmaException if the pragma is malformed.
	 */
	private static void applyCheckPragmaThen (
		final A_Token pragmaToken,
		final String pragmaValue,
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	throws MalformedPragmaException
	{
		final String[] parts = pragmaValue.split("=", 2);
		if (parts.length != 2)
		{
			throw new MalformedPragmaException(
				"Should have the form 'check=<property>=<value>'.");
		}
		final String propertyName = parts[0].trim();
		final String propertyValue = parts[1].trim();
		pragmaCheckThen(
			pragmaToken, state, propertyName, propertyValue, success, failure);
	}

	/**
	 * Apply a method pragma detected during parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param pragmaValue
	 *        The pragma {@link String} after "method=".
	 * @throws MalformedPragmaException
	 *         If this method-pragma is malformed.
	 */
	private void applyMethodPragmaThen (
		final A_Token pragmaToken,
		final String pragmaValue,
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	throws MalformedPragmaException
	{
		final String[] parts = pragmaValue.split("=", 2);
		if (parts.length != 2)
		{
			throw new MalformedPragmaException(
				format(
					"Expected method pragma to have the form "
						+ "%s=primitiveName=name",
					PRAGMA_METHOD.lexemeJavaString));
		}
		final String primName = parts[0].trim();
		final String methodName = parts[1].trim();
		bootstrapMethodThen(
			state, pragmaToken, methodName, primName, success, failure);
	}

	/**
	 * Apply a macro pragma detected during parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param pragmaToken
	 *        The string literal token specifying the pragma.
	 * @param pragmaValue
	 *        The pragma {@link String} after "macro=".
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param success
	 *        What to do after the macro is defined successfully.
	 * @param failure
	 *        What to do if the attempt to define the macro fails.
	 */
	private void applyMacroPragmaThen (
		final A_Token pragmaToken,
		final String pragmaValue,
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	{
		final String[] parts = pragmaValue.split("=", 2);
		if (parts.length != 2)
		{
			throw new IllegalArgumentException();
		}
		final String pragmaPrim = parts[0].trim();
		final String macroName = parts[1].trim();
		final String[] primNameStrings = pragmaPrim.split(",");
		final String[] primNames = new String[primNameStrings.length];
		for (int i = 0; i < primNames.length; i++)
		{
			final String primName = primNameStrings[i];
			final @Nullable Primitive prim = primitiveByName(primName);
			if (prim == null)
			{
				compilationContext.diagnostics.reportError(
					pragmaToken.nextLexingStateIn(compilationContext),
					"Malformed pragma at %s on line %d:",
					format(
						"Expected macro pragma to reference "
							+ "a valid primitive, not %s",
						primName),
					failure);
				return;
			}
			primNames[i] = primName;
		}
		bootstrapMacroThen(
			state, pragmaToken, macroName, primNames, success, failure);
	}

	/**
	 * Apply a stringify pragma detected during parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param pragmaToken
	 *        The string literal token specifying the pragma.
	 * @param pragmaValue
	 *        The pragma {@link String} after "stringify=".
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param success
	 *        What to do after the stringification name is defined successfully.
	 * @param failure
	 *        What to do after stringification fails.
	 */
	private void applyStringifyPragmaThen (
		final A_Token pragmaToken,
		final String pragmaValue,
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	{
		final A_String availName = stringFrom(pragmaValue);
		final A_Set atoms =
			compilationContext.module().trueNamesForStringName(availName);
		if (atoms.setSize() == 0)
		{
			compilationContext.diagnostics.reportError(
				pragmaToken.nextLexingStateIn(compilationContext),
				"Problem in stringification macro at %s on line %d:",
				format(
					"stringification method \"%s\" should be introduced"
						+ " in this module",
					availName.asNativeString()),
				failure);
			return;
		}
		if (atoms.setSize() > 1)
		{
			compilationContext.diagnostics.reportError(
				pragmaToken.nextLexingStateIn(compilationContext),
				"Problem in stringification macro at %s on line %d:",
				format(
					"stringification method \"%s\" is ambiguous",
					availName.asNativeString()),
				failure);
			return;
		}
		final A_Atom atom = atoms.asTuple().tupleAt(1);
		final A_Phrase send = newSendNode(
			emptyTuple(),
			DECLARE_STRINGIFIER.bundle,
			newListNode(tuple(syntheticLiteralNodeFor(atom))),
			TOP.o());
		evaluateModuleStatementThen(
			state, state, send, new HashMap<>(), success, failure);
	}

	/**
	 * Apply a lexer definition pragma detected during parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param pragmaToken
	 *        The string literal token specifying the pragma.
	 * @param pragmaValue
	 *        The pragma {@link String} after "lexer=".
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param success
	 *        What to do after the lexer is defined successfully.
	 * @param failure
	 *        What to do if the attempt to define the lexer fails.
	 */
	private void applyLexerPragmaThen (
		final A_Token pragmaToken,
		final String pragmaValue,
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	{
		final String filterPrimitiveName;
		final String bodyPrimitiveName;
		final String lexerName;
		try
		{
			final String[] parts = pragmaValue.split("=", 2);
			if (parts.length != 2)
			{
				throw new IllegalArgumentException();
			}
			final String primNames = parts[0].trim();
			final String[] primParts = primNames.split(",", 2);
			if (primParts.length != 2)
			{
				throw new IllegalArgumentException();
			}
			filterPrimitiveName = primParts[0];
			bodyPrimitiveName = primParts[1];
			lexerName = parts[1].trim();
		}
		catch (final IllegalArgumentException e)
		{
			compilationContext.diagnostics.handleProblem(
				new Problem(
					moduleName(),
					pragmaToken.lineNumber(),
					pragmaToken.start(),
					PARSE,
					"Expected lexer pragma to have the form "
						+ "{0}=filterPrim,bodyPrim=lexerName",
					PRAGMA_LEXER.lexemeJavaString)
				{
					@Override
					public void abortCompilation ()
					{
						failure.value();
					}
				});
			return;
		}

		final A_String availName = stringFrom(lexerName);
		final A_Module module = state.lexingState.compilationContext.module();
		final A_Set atoms = module.trueNamesForStringName(availName);
		if (atoms.setSize() == 0)
		{
			compilationContext.diagnostics.reportError(
				pragmaToken.nextLexingStateIn(compilationContext),
				"Problem in lexer pragma at %s on line %d:",
				format(
					"lexer method %s should be introduced in this module",
					availName),
				failure);
			return;
		}
		if (atoms.setSize() > 1)
		{
			compilationContext.diagnostics.reportError(
				pragmaToken.nextLexingStateIn(compilationContext),
				"Problem in lexer pragma at %s on line %d:",
				format("lexer name %s is ambiguous", availName),
				failure);
			return;
		}
		final A_Atom lexerAtom = atoms.iterator().next();

		try
		{
			bootstrapLexerThen(
				state,
				pragmaToken,
				lexerAtom,
				filterPrimitiveName,
				bodyPrimitiveName,
				success,
				failure);
		}
		catch (final MalformedPragmaException e)
		{
			compilationContext.diagnostics.handleProblem(
				new Problem(
					moduleName(),
					pragmaToken.lineNumber(),
					pragmaToken.start(),
					PARSE,
					e.problem(),
					PRAGMA_LEXER.lexemeJavaString)
				{
					@Override
					public void abortCompilation ()
					{
						failure.value();
					}
				});
		}
	}

	/**
	 * Apply any pragmas detected during the parse of the {@linkplain
	 * ModuleHeader module header}.
	 *
	 * @param state
	 *        The {@linkplain ParserState parse state} following a parse of the
	 *        module header.
	 * @param success
	 *        What to do after the pragmas have been applied successfully.
	 * @param failure
	 *        What to do after a problem is found with one of the pragma
	 *        definitions.
	 */
	private void applyPragmasThen (
		final ParserState state,
		final Continuation0 success,
		final Continuation0 failure)
	{
		final Iterator<A_Token> iterator = moduleHeader().pragmas.iterator();
		final MutableOrNull<Continuation0> body = new MutableOrNull<>();
		final Continuation0 next =
			() -> state.lexingState.compilationContext.eventuallyDo(
				state.lexingState,
				body.value());
		body.value = () ->
		{
			if (!iterator.hasNext())
			{
				// Done with all the pragmas, if any.  Report any new
				// problems relative to the body section.
				recordExpectationsRelativeTo(state.position());
				success.value();
				return;
			}
			final A_Token pragmaToken = iterator.next();
			final A_String pragmaString = pragmaToken.literal();
			final String nativeString = pragmaString.asNativeString();
			final String[] pragmaParts = nativeString.split("=", 2);
			if (pragmaParts.length != 2)
			{
				compilationContext.diagnostics.reportError(
					pragmaToken.nextLexingStateIn(compilationContext),
					"Malformed pragma at %s on line %d:",
					"Pragma should have the form key=value",
					failure);
				return;
			}
			final String pragmaKind = pragmaParts[0].trim();
			final String pragmaValue = pragmaParts[1].trim();
			try
			{
				switch (pragmaKind)
				{
					case "check":
					{
						assert pragmaKind.equals(
							PRAGMA_CHECK.lexemeJavaString);
						applyCheckPragmaThen(
							pragmaToken, pragmaValue, state, next, failure);
						break;
					}
					case "method":
					{
						assert pragmaKind.equals(
							PRAGMA_METHOD.lexemeJavaString);
						applyMethodPragmaThen(
							pragmaToken, pragmaValue, state, next, failure);
						break;
					}
					case "macro":
					{
						assert pragmaKind.equals(
							PRAGMA_MACRO.lexemeJavaString);
						applyMacroPragmaThen(
							pragmaToken, pragmaValue, state, next, failure);
						break;
					}
					case "stringify":
					{
						assert pragmaKind.equals(
							PRAGMA_STRINGIFY.lexemeJavaString);
						applyStringifyPragmaThen(
							pragmaToken, pragmaValue, state, next, failure);
						break;
					}
					case "lexer":
					{
						assert pragmaKind.equals(
							PRAGMA_LEXER.lexemeJavaString);
						applyLexerPragmaThen(
							pragmaToken, pragmaValue, state, next, failure);
						break;
					}
					default:
						compilationContext.diagnostics.reportError(
							pragmaToken.nextLexingStateIn(compilationContext),
							"Malformed pragma at %s on line %d:",
							format(
								"Pragma key should be one of "
									+ "%s, %s, %s, %s, or %s",
								PRAGMA_CHECK.lexemeJavaString,
								PRAGMA_METHOD.lexemeJavaString,
								PRAGMA_MACRO.lexemeJavaString,
								PRAGMA_STRINGIFY.lexemeJavaString,
								PRAGMA_LEXER.lexemeJavaString),
							failure);
				}
			}
			catch (final MalformedPragmaException e)
			{
				compilationContext.diagnostics.reportError(
					pragmaToken.nextLexingStateIn(compilationContext),
					"Malformed pragma at %s on line %d:",
					format(
						"Malformed pragma: %s",
						e.problem()),
					failure);
			}
		};
		compilationContext.loader().setPhase(EXECUTING_FOR_COMPILE);
		next.value();
	}

	/**
	 * Parse a {@linkplain ModuleHeader module header} from the {@linkplain
	 * TokenDescriptor token list} and apply any side-effects. Then {@linkplain
	 * #parseAndExecuteOutermostStatements(ParserState, Continuation0) parse the
	 * module body} and apply any side-effects.  Finally, execute the
	 * {@link #compilationContext}'s successReporter.
	 *
	 * @param onFail
	 *        What to do if the module compilation fails.
	 */
	private void parseModuleCompletely (
		final Continuation0 onFail)
	{
		// TODO MvG - Some day load the header instead of parsing it again.
		parseModuleHeader(
			afterHeader ->
			{
				compilationContext.getProgressReporter().value(
					moduleName(),
					(long) source().tupleSize(),
					(long) afterHeader.position());
				// Run any side-effects implied by this module header against
				// the module.
				final @Nullable String errorString =
					moduleHeader().applyToModule(
						compilationContext.module(), runtime);
				if (errorString != null)
				{
					compilationContext.getProgressReporter().value(
						moduleName(),
						(long) source().tupleSize(),
						(long) source().tupleSize());
					afterHeader.expected(errorString);
					compilationContext.diagnostics.reportError(onFail);
					return;
				}
				compilationContext.loader().prepareForCompilingModuleBody();
				applyPragmasThen(
					afterHeader,
					() ->
						parseAndExecuteOutermostStatements(afterHeader, onFail),
					onFail);
			},
			onFail);
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} body from the {@linkplain
	 * TokenDescriptor token} list, execute it, and repeat if we're not at the
	 * end of the module.
	 *
	 * @param start
	 *        The {@linkplain ParserState parse state} after parsing a
	 *        {@linkplain ModuleHeader module header}.
	 * @param afterFail
	 *        What to do after compilation fails.
	 */
	private void parseAndExecuteOutermostStatements (
		final ParserState start,
		final Continuation0 afterFail)
	{
		compilationContext.loader().setPhase(COMPILING);
		final MutableOrNull<List<ParserState>> afterWhitespaceHolder =
			new MutableOrNull<>();
		compilationContext.setNoMoreWorkUnits(() ->
		{
			if (compilationContext.diagnostics.isShuttingDown)
			{
				// Some of the tasks may have been bypassed due to the pending
				// shutdown, so don't assume afterWhitespaceHolder.value has
				// been set.
				afterFail.value();
				return;
			}
			final List<ParserState> afterWhitespace =
				afterWhitespaceHolder.value();
			if (afterWhitespace.size() == 1)
			{
				parseOutermostStatementWithoutWhitespace(
					afterWhitespace.get(0), afterFail);
			}
			else if (afterWhitespace.isEmpty())
			{
				// It should have already reported a problem trying to lex.
				compilationContext.diagnostics.reportError(afterFail);
			}
			else
			{
				final ParserState earliest = Collections.min(
					afterWhitespace,
					Comparator.comparingInt(ParserState::position));
				earliest.expected(
					"unambiguous lexical scan of whitespace and comments "
						+ "between top-level statements");
				afterFail.value();
			}
		});
		skipWhitespaceAndComments(
			start,
			positions -> afterWhitespaceHolder.value = positions,
			new AtomicBoolean(false));
	}

	/**
	 * Having already skipped any whitespace and comments to get to a (unique)
	 * parse position that's not at the end, parse an outermost statement.
	 *
	 * @param start
	 *        Where to start parsing.  Cannot be at whitespace or a comment or
	 *        the end of the module.
	 * @param afterFail
	 *        What to do if the expression fails.
	 */
	private void parseOutermostStatementWithoutWhitespace (
		final ParserState start,
		final Continuation0 afterFail)
	{
		if (start.atEnd())
		{
			reachedEndOfModule(start, afterFail);
			return;
		}
		parseOutermostStatement(
			start,
			Con(
				null,
				solution ->
				{
					// The counters must be read in this order for correctness.
					assert compilationContext.getWorkUnitsCompleted()
						== compilationContext.getWorkUnitsQueued();

					// In case the top level statement is compound, process the
					// base statements individually.
					final ParserState afterStatement = solution.endState();
					final A_Phrase unambiguousStatement = solution.phrase();
					final List<A_Phrase> simpleStatements = new ArrayList<>();
					unambiguousStatement.statementsDo(
						simpleStatement ->
						{
							assert simpleStatement.phraseKindIsUnder(
								STATEMENT_PHRASE);
							simpleStatements.add(simpleStatement);
						});

					// For each top-level simple statement, (1) transform it to
					// have referenced previously transformed top-level
					// declarations mapped from local scope into global scope,
					// (2) if it's itself a declaration, transform it and record
					// the transformation for subsequent statements, and (3)
					// execute it.  The declarationRemap accumulates the
					// transformations.  Parts 2 and 3 actually happen together
					// so that module constants can have types as strong as the
					// actual values produced by running their initialization
					// expressions.
					final Map<A_Phrase, A_Phrase> declarationRemap =
						new HashMap<>();
					final Iterator<A_Phrase> simpleStatementIterator =
						simpleStatements.iterator();

					// What to do after running all these simple statements.
					final Continuation0 resumeParsing = () ->
					{
						compilationContext.clearLexingStates();
						// Report progress.
						compilationContext.getProgressReporter().value(
							moduleName(),
							(long) source().tupleSize(),
							(long) afterStatement.position());
						parseAndExecuteOutermostStatements(
							afterStatement.withMap(start.clientDataMap),
							afterFail);
					};

					// What to do after running a simple statement (or to get
					// the first one to run).
					final MutableOrNull<Continuation0> executeSimpleStatement =
						new MutableOrNull<>();
					executeSimpleStatement.value = () ->
					{
						if (!simpleStatementIterator.hasNext())
						{
							resumeParsing.value();
							return;
						}
						final A_Phrase statement =
							simpleStatementIterator.next();
						if (AvailLoader.debugLoadedStatements)
						{
							System.out.println(
								moduleName().qualifiedName()
									+ ':' + start.lineNumber()
									+ " Running statement:\n" + statement);
						}
						evaluateModuleStatementThen(
							start,
							afterStatement,
							statement,
							declarationRemap,
							executeSimpleStatement.value(),
							afterFail);
					};

					// Kick off execution of these simple statements.
					compilationContext.loader().setPhase(EXECUTING_FOR_COMPILE);
					executeSimpleStatement.value().value();
				}),
			afterFail);
	}

	/**
	 * We just reached the end of the module.
	 *
	 * @param afterModule
	 *        The position at the end of the module.
	 * @param afterFail
	 *        What to do if there's a failure.
	 */
	private void reachedEndOfModule (
		final ParserState afterModule,
		final Continuation0 afterFail)
	{
		final AvailLoader theLoader = compilationContext.loader();
		if (theLoader.pendingForwards.setSize() != 0)
		{
			@SuppressWarnings({
				"resource",
				"IOResourceOpenedButNotSafelyClosed"
			})
			final Formatter formatter = new Formatter();
			formatter.format("the following forwards to be resolved:");
			for (final A_BasicObject forward : theLoader.pendingForwards)
			{
				formatter.format("%n\t%s", forward);
			}
			afterModule.expected(formatter.toString());
			compilationContext.diagnostics.reportError(afterFail);
			return;
		}
		// Clear the section of the fragment cache
		// associated with the (outermost) statement
		// just parsed and executed...
		synchronized (fragmentCache)
		{
			fragmentCache.clear();
		}
		compilationContext.getSuccessReporter().value();
	}

	/**
	 * Clear any information about potential problems encountered during
	 * parsing.  Reset the problem information to record relative to the
	 * provided one-based source position.
	 *
	 * @param positionInSource
	 *        The earliest source position for which we should record problem
	 *        information.
	 */
	private synchronized void recordExpectationsRelativeTo (
		final int positionInSource)
	{
		compilationContext.diagnostics.startParsingAt(positionInSource);
	}

	/**
	 * Parse a {@linkplain ModuleDescriptor module} from the source and install
	 * it into the {@linkplain AvailRuntime runtime}.  This method generally
	 * returns long before the module has been parsed, but the {@link
	 * #compilationContext}'s {@link CompilationContext#successReporter} is
	 * invoked when the module has been fully parsed and installed.
	 *
	 * @param onSuccess
	 *        What to do when the entire module has been parsed successfully.
	 * @param afterFail
	 *        What to do after compilation fails.
	 */
	public synchronized void parseModule (
		final Continuation1NotNull<A_Module> onSuccess,
		final Continuation0 afterFail)
	{
		compilationContext.setSuccessReporter(() ->
		{
			serializePublicationFunction(true);
			serializePublicationFunction(false);
			commitModuleTransaction();
			onSuccess.value(compilationContext.module());
		});
		startModuleTransaction();
		parseModuleCompletely(() -> rollbackModuleTransaction(afterFail));
	}

	/**
	 * Parse a command, compiling it into the current {@linkplain
	 * ModuleDescriptor module}, from the {@linkplain
	 * TokenDescriptor token} list.
	 *
	 * @param succeed
	 *        What to do after compilation succeeds. This {@linkplain
	 *        Continuation2 continuation} is invoked with a {@linkplain List
	 *        list} of {@link A_Phrase phrases} that represent the possible
	 *        solutions of compiling the command and a {@linkplain
	 *        Continuation1NotNull continuation} that cleans up this compiler
	 *        and its module (and then continues with a post-cleanup {@linkplain
	 *        Continuation0 continuation}).
	 * @param afterFail
	 *        What to do after compilation fails.
	 */
	public synchronized void parseCommand (
		final Continuation2<List<A_Phrase>, Continuation1NotNull<Continuation0>>
			succeed,
		final Continuation0 afterFail)
	{
		assert compilationContext.getWorkUnitsCompleted() == 0
			&& compilationContext.getWorkUnitsQueued() == 0;
		// Start a module transaction, just to complete any necessary
		// initialization. We are going to rollback this transaction no matter
		// what happens.
		startModuleTransaction();
		final AvailLoader loader = compilationContext.loader();
		loader.prepareForCompilingModuleBody();
		final A_Map clientData = mapFromPairs(
			COMPILER_SCOPE_MAP_KEY.atom,
			emptyMap(),
			ALL_TOKENS_KEY.atom,
			emptyTuple());
		final ParserState start = new ParserState(
			new LexingState(compilationContext, 1, 1), clientData, emptyList());
		final List<A_Phrase> solutions = new ArrayList<>();
		compilationContext.setNoMoreWorkUnits(
			() ->
			{
				// The counters must be read in this order for correctness.
				assert compilationContext.getWorkUnitsCompleted()
					== compilationContext.getWorkUnitsQueued();
				// If no solutions were found, then report an error.
				if (solutions.isEmpty())
				{
					start.expected("an invocation of an entry point");
					compilationContext.diagnostics.reportError(
						() -> rollbackModuleTransaction(afterFail));
					return;
				}
				succeed.value(solutions, this::rollbackModuleTransaction);
			});
		recordExpectationsRelativeTo(1);
		skipWhitespaceAndComments(
			start,
			afterLeadingWhitespaceStates ->
			{
				// Rollback the module transaction no matter what happens.
				if (afterLeadingWhitespaceStates.size() == 0)
				{
					start.expected(
						"a way to lexically scan whitespace before the "
							+ "command");
					return;
				}
				if (afterLeadingWhitespaceStates.size() > 1)
				{
					start.expected(
						"an unambiguous way to lexically scan whitespace "
							+ "before the command");
					return;
				}
				parseExpressionThen(
					afterLeadingWhitespaceStates.get(0),
					Con(
						null,
						solution ->
						{
							final A_Phrase expression = solution.phrase();
							final ParserState afterExpression =
								solution.endState();
							if (expression.hasSuperCast())
							{
								afterExpression.expected(
									"a valid command, not a supercast");
								return;
							}
							skipWhitespaceAndComments(
								afterExpression,
								finalStates ->
								{
									if (finalStates.size() == 1)
									{
										final ParserState finalState =
											finalStates.get(0);
										if (finalState.atEnd())
										{
											synchronized (solutions)
											{
												solutions.add(expression);
											}
											return;
										}
										finalState.expected("end of command");
										// Fall-through.
									}
									// Otherwise, report a failure.
									if (finalStates.size() == 0)
									{
										afterExpression.expected(
											"a way to lexically scan "
												+ "whitespace after the "
												+ "command");
									}
									else if (finalStates.size() > 1)
									{
										final ParserState earliest =
											Collections.min(
												finalStates,
												Comparator.comparing(
													ParserState::position));
										earliest.expected(
											"an unambiguous way to lexically "
												+ "scan whitespace after the "
												+ "command");
									}
								},
								new AtomicBoolean(false));
						}));
			},
			new AtomicBoolean(false));
	}

	/**
	 * The given phrase must contain only subexpressions that are literal
	 * phrases or list phrases.  Convert the structure into a nested tuple of
	 * tokens.
	 *
	 * <p>The tokens are kept, rather than extracting the literal strings or
	 * integers, so that error reporting can refer to the token positions.</p>
	 *
	 * @param phrase
	 *        The root literal phrase or list phrase.
	 * @return The token of the literal phrase, or a tuple with the (recursive)
	 *         tuples of the list phrase's subexpressions' tokens.
	 */
	private static AvailObject convertHeaderPhraseToValue (
		final A_Phrase phrase)
	{
		switch (phrase.phraseKind())
		{
			case LITERAL_PHRASE:
			{
				return (AvailObject) phrase.token();
			}
			case LIST_PHRASE:
			case PERMUTED_LIST_PHRASE:
			{
				final A_Tuple expressions = phrase.expressionsTuple();
				return generateObjectTupleFrom(
					expressions.tupleSize(),
					index -> convertHeaderPhraseToValue(
						expressions.tupleAt(index))
				);
			}
			case MACRO_SUBSTITUTION_PHRASE:
			{
				//noinspection TailRecursion
				return convertHeaderPhraseToValue(phrase.stripMacro());
			}
			default:
			{
				throw new RuntimeException(
					"Unexpected phrase type in header: " +
					phrase.phraseKind().name());
			}
		}
	}

	/**
	 * Extract a {@link A_String string} from the given string literal {@link
	 * A_Token token}.
	 *
	 * @param token The string literal token.
	 * @return The token's string.
	 */
	private static A_String stringFromToken (final A_Token token)
	{
		assert token.isInstanceOfKind(TOKEN.o());
		final A_Token innerToken = token.literal();
		final A_String literal = innerToken.literal();
		assert literal.isInstanceOfKind(stringType());
		return literal;
	}

	/**
	 * Process a header that has just been parsed.
	 *
	 * @param headerPhrase
	 *        The invocation of {@link SpecialMethodAtom#MODULE_HEADER_METHOD}
	 *        that was just parsed.
	 * @param afterFail
	 *        What to invoke if a failure happens.
	 */
	private void processHeaderMacro (
		final A_Phrase headerPhrase,
		final ParserState stateAfterHeader,
		final Continuation0 afterFail)
	{
		final ModuleHeader header = moduleHeader();

		assert headerPhrase.phraseKindIsUnder(SEND_PHRASE);
		assert headerPhrase.apparentSendName().equals(
			MODULE_HEADER_METHOD.atom);
		final A_Tuple args =
			convertHeaderPhraseToValue(headerPhrase.argumentsListNode());
		assert args.tupleSize() == 6;
		final A_Token moduleNameToken = args.tupleAt(1);
		final A_Tuple optionalVersions = args.tupleAt(2);
		final A_Tuple allImports = args.tupleAt(3);
		final A_Tuple optionalNames = args.tupleAt(4);
		final A_Tuple optionalEntries = args.tupleAt(5);
		final A_Tuple optionalPragmas = args.tupleAt(6);

		// Module name section
		final A_String moduleName = stringFromToken(moduleNameToken);
		if (!moduleName.asNativeString().equals(moduleName().localName()))
		{
			moduleNameToken.nextLexingStateIn(compilationContext).expected(
				"declared local module name to agree with "
				+ "fully-qualified module name");
			afterFail.value();
			return;
		}

		// Module version section
		if (optionalVersions.tupleSize() > 0)
		{
			assert optionalVersions.tupleSize() == 1;
			for (final A_Token versionStringToken : optionalVersions.tupleAt(1))
			{
				final A_String versionString = stringFromToken(
					versionStringToken);
				if (header.versions.contains(versionString))
				{
					versionStringToken.nextLexingStateIn(compilationContext)
						.expected("version strings to be unique");
					afterFail.value();
					return;
				}
				header.versions.add(versionString);
			}
		}

		// Imports section (all Extends/Uses subsections)
		for (final A_Tuple importSection : allImports)
		{
			final A_Token importKindToken = importSection.tupleAt(1);
			assert importKindToken.isInstanceOfKind(TOKEN.o());
			final A_Number importKind = importKindToken.literal();
			assert importKind.isInt();
			final int importKindInt = importKind.extractInt();
			assert importKindInt >= 1 && importKindInt <= 2;
			final boolean isExtension = importKindInt == 1;

			for (final A_Tuple moduleImport : importSection.tupleAt(2))
			{
				// <importedModule, optionalVersions, optionalNamesPart>
				assert moduleImport.tupleSize() == 3;
				final A_Token importedModuleToken = moduleImport.tupleAt(1);
				final A_String importedModuleName =
					stringFromToken(importedModuleToken);

				final A_Tuple optionalImportVersions = moduleImport.tupleAt(2);
				assert optionalImportVersions.isTuple();
				A_Set importVersions = emptySet();
				if (optionalImportVersions.tupleSize() > 0)
				{
					assert optionalImportVersions.tupleSize() == 1;
					for (final A_Token importVersionToken
						: optionalImportVersions.tupleAt(1))
					{
						final A_String importVersionString =
							stringFromToken(importVersionToken);
						if (importVersions.hasElement(importVersionString))
						{
							importVersionToken
								.nextLexingStateIn(compilationContext)
								.expected(
									"module import versions to be unique");
							afterFail.value();
							return;
						}
						importVersions =
							importVersions.setWithElementCanDestroy(
								importVersionString, true);
					}
				}

				A_Set importedNames = emptySet();
				A_Map importedRenames = emptyMap();
				A_Set importedExcludes = emptySet();
				boolean wildcard = true;

				final A_Tuple optionalNamesPart = moduleImport.tupleAt(3);
				// <filterEntries, finalEllipsis>?
				if (optionalNamesPart.tupleSize() > 0)
				{
					assert optionalNamesPart.tupleSize() == 1;
					final A_Tuple namesPart = optionalNamesPart.tupleAt(1);
					assert namesPart.tupleSize() == 2;
					// <filterEntries, finalEllipsis>
					for (final A_Tuple filterEntry : namesPart.tupleAt(1))
					{
						// <negation, name, rename>
						assert filterEntry.tupleSize() == 3;
						final A_Token negationLiteralToken =
							filterEntry.tupleAt(1);
						final boolean negation =
							negationLiteralToken.literal().extractBoolean();
						final A_Token nameToken = filterEntry.tupleAt(2);
						final A_String name = stringFromToken(nameToken);
						final A_Tuple optionalRename = filterEntry.tupleAt(3);
						if (optionalRename.tupleSize() > 0)
						{
							// Process a renamed import
							assert optionalRename.tupleSize() == 1;
							final A_Token renameToken =
								optionalRename.tupleAt(1);
							if (negation)
							{
								renameToken
									.nextLexingStateIn(compilationContext)
									.expected(
										"negated or renaming import, but "
											+ "not both");
								afterFail.value();
								return;
							}
							final A_String rename =
								stringFromToken(renameToken);
							if (importedRenames.hasKey(rename))
							{
								renameToken
									.nextLexingStateIn(compilationContext)
									.expected(
										"renames to specify distinct "
											+ "target names");
								afterFail.value();
								return;
							}
							importedRenames =
								importedRenames.mapAtPuttingCanDestroy(
									rename, name, true);
						}
						else if (negation)
						{
							// Process an excluded import.
							if (importedExcludes.hasElement(name))
							{
								nameToken.nextLexingStateIn(compilationContext)
									.expected("import exclusions to be unique");
								afterFail.value();
								return;
							}
							importedExcludes =
								importedExcludes.setWithElementCanDestroy(
									name, true);
						}
						else
						{
							// Process a regular import (neither a negation
							// nor an exclusion).
							if (importedNames.hasElement(name))
							{
								nameToken.nextLexingStateIn(compilationContext)
									.expected("import names to be unique");
								afterFail.value();
								return;
							}
							importedNames =
								importedNames.setWithElementCanDestroy(
									name, true);
						}
					}

					// Check for the trailing ellipsis.
					final A_Token finalEllipsisLiteralToken =
						namesPart.tupleAt(2);
					final A_Atom finalEllipsis =
						finalEllipsisLiteralToken.literal();
					assert finalEllipsis.isBoolean();
					wildcard = finalEllipsis.extractBoolean();
				}

				try
				{
					moduleHeader().importedModules.add(
						new ModuleImport(
							importedModuleName,
							importVersions,
							isExtension,
							importedNames,
							importedRenames,
							importedExcludes,
							wildcard));
				}
				catch (final ImportValidationException e)
				{
					importedModuleToken.nextLexingStateIn(compilationContext)
						.expected(e.getMessage());
					afterFail.value();
					return;
				}
			}  // modules of an import subsection
		}  // imports section

		// Names section
		if (optionalNames.tupleSize() > 0)
		{
			assert optionalNames.tupleSize() == 1;
			for (final A_Token nameToken : optionalNames.tupleAt(1))
			{
				final A_String nameString = stringFromToken(nameToken);
				if (header.exportedNames.contains(nameString))
				{
					nameToken.nextLexingStateIn(compilationContext).expected(
						"declared names to be unique");
					afterFail.value();
					return;
				}
				header.exportedNames.add(nameString);
			}
		}

		// Entries section
		if (optionalEntries.tupleSize() > 0)
		{
			assert optionalEntries.tupleSize() == 1;
			for (final A_Token entryToken : optionalEntries.tupleAt(1))
			{
				header.entryPoints.add(stringFromToken(entryToken));
			}
		}

		// Pragmas section
		if (optionalPragmas.tupleSize() > 0)
		{
			assert optionalPragmas.tupleSize() == 1;
			for (final A_Token pragmaToken : optionalPragmas.tupleAt(1))
			{
				final A_Token innerToken = pragmaToken.literal();
				header.pragmas.add(innerToken);
			}
		}
		header.startOfBodyPosition = stateAfterHeader.position();
		header.startOfBodyLineNumber = stateAfterHeader.lineNumber();
	}

	/**
	 * Parse the header of the module from the token stream. If successful,
	 * return the {@link ParserState} just after the header, otherwise return
	 * {@code null}.
	 *
	 * <p>If the {@code dependenciesOnly} parameter is true, only parse the bare
	 * minimum needed to determine information about which modules are used by
	 * this one.</p>
	 *
	 * @param onSuccess
	 *        What to do after successfully parsing the header.  The compilation
	 *        context's header will have been updated, and the {@link
	 *        Continuation1NotNull} will be passed the {@link ParserState} after
	 *        the header.
	 * @param onFail
	 *        What to do if the module header could not be parsed.
	 */
	public void parseModuleHeader (
		final Continuation1NotNull<ParserState> onSuccess,
		final Continuation0 onFail)
	{
		// Create the initial parser state: no tokens have been seen, and no
		// names are in scope.
		final A_Map clientData = mapFromPairs(
			COMPILER_SCOPE_MAP_KEY.atom,
			emptyMap(),
			ALL_TOKENS_KEY.atom,
			emptyTuple());
		final ParserState state = new ParserState(
			new LexingState(compilationContext, 1, 1), clientData, emptyList());

		recordExpectationsRelativeTo(1);

		// Parse an invocation of the special module header macro.
		parseOutermostStatement(
			state,
			Con(
				null,
				solution ->
				{
					final A_Phrase headerPhrase = solution.phrase();
					assert headerPhrase.phraseKindIsUnder(
						EXPRESSION_AS_STATEMENT_PHRASE);
					assert headerPhrase.apparentSendName().equals(
						MODULE_HEADER_METHOD.atom);
					processHeaderMacro(
						headerPhrase.expression(),
						solution.endState(),
						onFail);
					onSuccess.value(solution.endState());
				}),
			onFail);
	}

	/**
	 * Parse an expression. Backtracking will find all valid interpretations.
	 * This method is a key optimization point, so the fragmentCache is used to
	 * keep track of parsing solutions at this point, simply replaying them on
	 * subsequent parses, as long as the variable declarations up to that point
	 * were identical.
	 *
	 * <p>
	 * Additionally, the fragmentCache also keeps track of actions to perform
	 * when another solution is found at this position, so the solutions and
	 * actions can be added in arbitrary order while ensuring that each action
	 * gets a chance to try each solution.
	 * </p>
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param originalContinuation
	 *        What to do with the expression.
	 */
	private void parseExpressionThen (
		final ParserState start,
		final Con originalContinuation)
	{
		// The first time we parse at this position the fragmentCache will
		// have no knowledge about it.
		final AvailCompilerBipartiteRendezvous rendezvous =
			fragmentCache.getRendezvous(start);
		if (!rendezvous.getAndSetStartedParsing())
		{
			// We're the (only) cause of the transition from hasn't-started to
			// has-started.
			start.expected(withDescription ->
			{
				final StringBuilder builder = new StringBuilder();
				builder.append("an expression for (at least) this reason:");
				describeOn(originalContinuation.superexpressions, builder);
				withDescription.value(builder.toString());
			});
			start.workUnitDo(
				a -> parseExpressionUncachedThen(start, a),
				Con(
					originalContinuation.superexpressions,
					rendezvous::addSolution));
		}
		start.workUnitDo(rendezvous::addAction, originalContinuation);
	}

	/**
	 * Parse a top-level statement.  This is the <em>only</em> boundary for the
	 * backtracking grammar (it used to be that <em>all</em> statements had to
	 * be unambiguous, even those in blocks).  The passed continuation will be
	 * invoked at most once, and only if the top-level statement had a single
	 * interpretation.
	 *
	 * @param start
	 *        Where to start parsing a top-level statement.
	 * @param continuation
	 *        What to do with the (unambiguous) top-level statement.
	 * @param afterFail
	 *        What to run after a failure has been reported.
	 */
	private void parseOutermostStatement (
		final ParserState start,
		final Con continuation,
		final Continuation0 afterFail)
	{
		// If a parsing error happens during parsing of this outermost
		// statement, only show the section of the file starting here.
		recordExpectationsRelativeTo(start.position());
		tryIfUnambiguousThen(
			start,
			whenFoundStatement -> parseExpressionThen(
				start,
				Con(
					null,
					solution ->
					{
						final A_Phrase expression = solution.phrase();
						final ParserState afterExpression = solution.endState();
						if (expression.phraseKindIsUnder(STATEMENT_PHRASE))
						{
							whenFoundStatement.value(
								new CompilerSolution(
									afterExpression, expression));
							return;
						}
						afterExpression.expected(
							new FormattingDescriber(
								"an outer level statement, not %s (%s)",
								expression.phraseKind(),
								expression));
					})),
			continuation,
			afterFail);
	}

	/**
	 * Parse an expression, without directly using the
	 * {@linkplain #fragmentCache}.
	 *
	 * @param start
	 *        Where to start parsing.
	 * @param continuation
	 *        What to do with the expression.
	 */
	private void parseExpressionUncachedThen (
		final ParserState start,
		final Con continuation)
	{
		parseLeadingKeywordSendThen(
			start,
			Con(
				continuation.superexpressions,
				solution -> parseOptionalLeadingArgumentSendAfterThen(
					start,
					solution.endState(),
					solution.phrase(),
					continuation)));
	}

	/**
	 * A helper method to queue a parsing activity for continuing to parse a
	 * {@linkplain SendPhraseDescriptor send phrase}.
	 *
	 * @param start
	 *        The current {@link ParserState}.
	 * @param bundleTree
	 *        The current {@link A_BundleTree} being applied.
	 * @param firstArgOrNull
	 *        Either null or a pre-parsed first argument phrase.
	 * @param initialTokenPosition
	 *        The position at which parsing of this message started. If it was
	 *        parsed as a leading argument send (i.e., firstArgOrNull started
	 *        out non-null) then the position is of the token following the
	 *        first argument.
	 * @param consumedAnything
	 *        Whether any tokens have been consumed yet.
	 * @param argsSoFar
	 *        The arguments stack.
	 * @param marksSoFar
	 *        The marks stack.
	 * @param continuation
	 *        What to do with a completed phrase.
	 */
	@InnerAccess void eventuallyParseRestOfSendNode (
		final ParserState start,
		final A_BundleTree bundleTree,
		final @Nullable A_Phrase firstArgOrNull,
		final ParserState initialTokenPosition,
		final boolean consumedAnything,
		final List<A_Token> consumedTokens,
		final List<A_Phrase> argsSoFar,
		final List<Integer> marksSoFar,
		final Con continuation)
	{
		start.workUnitDo(
			ignored -> parseRestOfSendNode(
				start,
				bundleTree,
				firstArgOrNull,
				initialTokenPosition,
				consumedAnything,
				consumedTokens,
				argsSoFar,
				marksSoFar,
				continuation),
			"999");
	}

	/**
	 * Answer the {@linkplain SetDescriptor set} of {@linkplain
	 * DeclarationPhraseDescriptor declaration phrases} which are used by this
	 * parse tree but are locally declared (i.e., not at global module scope).
	 *
	 * @param phrase
	 *        The phrase to recursively examine.
	 * @return The set of the local declarations that were used in the phrase.
	 */
	private static A_Set usesWhichLocalVariables (
		final A_Phrase phrase)
	{
		final Mutable<A_Set> usedDeclarations = new Mutable<>(emptySet());
		phrase.childrenDo(
			childPhrase ->
			{
				if (childPhrase.isInstanceOfKind(
					VARIABLE_USE_PHRASE.mostGeneralType()))
				{
					final A_Phrase declaration = childPhrase.declaration();
					if (!declaration.declarationKind().isModuleScoped())
					{
						usedDeclarations.value =
							usedDeclarations.value.setWithElementCanDestroy(
								declaration, true);
					}
				}
			});
		return usedDeclarations.value;
	}

	/**
	 * Serialize a function that will publish all atoms that are currently
	 * public in the module.
	 *
	 * @param isPublic
	 *        {@code true} if the atoms are public, {@code false} if they are
	 *        private.
	 */
	private void serializePublicationFunction (final boolean isPublic)
	{
		// Output a function that publishes the initial public set of atoms.
		final A_Map sourceNames =
			isPublic
				? compilationContext.module().importedNames()
				: compilationContext.module().privateNames();
		A_Set names = emptySet();
		for (final Entry entry : sourceNames.mapIterable())
		{
			names = names.setUnionCanDestroy(
				entry.value().makeImmutable(), true);
		}
		final A_Phrase send = newSendNode(
			emptyTuple(),
			PUBLISH_ATOMS.bundle,
			newListNode(
				tuple(
					syntheticLiteralNodeFor(names),
					syntheticLiteralNodeFor(objectFromBoolean(isPublic)))),
			TOP.o());
		final A_Function function = createFunctionForPhrase(
			send, compilationContext.module(), 0);
		function.makeImmutable();
		synchronized (this)
		{
			compilationContext.serializer.serialize(function);
		}
	}
}
