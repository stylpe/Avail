/*
 * AvailLoader.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
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
package avail.interpreter.execution

import avail.AvailRuntime
import avail.AvailThread
import avail.compiler.ModuleManifestEntry
import avail.compiler.SideEffectKind
import avail.compiler.scanning.LexingState
import avail.compiler.splitter.MessageSplitter
import avail.descriptor.atoms.A_Atom
import avail.descriptor.atoms.A_Atom.Companion.atomName
import avail.descriptor.atoms.A_Atom.Companion.bundleOrCreate
import avail.descriptor.atoms.A_Atom.Companion.extractBoolean
import avail.descriptor.atoms.A_Atom.Companion.issuingModule
import avail.descriptor.atoms.A_Atom.Companion.setAtomProperty
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.atoms.AtomDescriptor.Companion.createAtom
import avail.descriptor.atoms.AtomDescriptor.Companion.createSpecialAtom
import avail.descriptor.atoms.AtomDescriptor.Companion.trueObject
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom.EXPLICIT_SUBCLASSING_KEY
import avail.descriptor.bundles.A_Bundle
import avail.descriptor.bundles.A_Bundle.Companion.addGrammaticalRestriction
import avail.descriptor.bundles.A_Bundle.Companion.bundleAddMacro
import avail.descriptor.bundles.A_Bundle.Companion.bundleMethod
import avail.descriptor.bundles.A_Bundle.Companion.definitionParsingPlans
import avail.descriptor.bundles.A_Bundle.Companion.macrosTuple
import avail.descriptor.bundles.A_Bundle.Companion.message
import avail.descriptor.bundles.A_Bundle.Companion.messageSplitter
import avail.descriptor.bundles.A_BundleTree
import avail.descriptor.bundles.A_BundleTree.Companion.addPlanInProgress
import avail.descriptor.bundles.A_BundleTree.Companion.removePlanInProgress
import avail.descriptor.bundles.A_BundleTree.Companion.updateForNewGrammaticalRestriction
import avail.descriptor.bundles.MessageBundleTreeDescriptor
import avail.descriptor.bundles.MessageBundleTreeDescriptor.Companion.newBundleTree
import avail.descriptor.character.CharacterDescriptor.Companion.fromCodePoint
import avail.descriptor.fiber.A_Fiber
import avail.descriptor.fiber.A_Fiber.Companion.setSuccessAndFailure
import avail.descriptor.fiber.FiberDescriptor.Companion.loaderPriority
import avail.descriptor.fiber.FiberDescriptor.Companion.newFiber
import avail.descriptor.fiber.FiberDescriptor.Companion.newLoaderFiber
import avail.descriptor.functions.A_Function
import avail.descriptor.functions.A_RawFunction.Companion.codeStartingLineNumber
import avail.descriptor.functions.A_RawFunction.Companion.numArgs
import avail.descriptor.functions.FunctionDescriptor
import avail.descriptor.functions.FunctionDescriptor.Companion.createFunction
import avail.descriptor.functions.PrimitiveCompiledCodeDescriptor.Companion.newPrimitiveRawFunction
import avail.descriptor.maps.A_Map.Companion.forEach
import avail.descriptor.maps.A_Map.Companion.mapAt
import avail.descriptor.maps.A_Map.Companion.mapAtOrNull
import avail.descriptor.maps.A_Map.Companion.mapIterable
import avail.descriptor.methods.A_Definition
import avail.descriptor.methods.A_Definition.Companion.definitionMethod
import avail.descriptor.methods.A_GrammaticalRestriction
import avail.descriptor.methods.A_Macro
import avail.descriptor.methods.A_Method
import avail.descriptor.methods.A_Method.Companion.bundles
import avail.descriptor.methods.A_Method.Companion.chooseBundle
import avail.descriptor.methods.A_Method.Companion.definitionsTuple
import avail.descriptor.methods.A_Method.Companion.includesDefinition
import avail.descriptor.methods.A_Method.Companion.lexer
import avail.descriptor.methods.A_Method.Companion.methodAddDefinition
import avail.descriptor.methods.A_Method.Companion.numArgs
import avail.descriptor.methods.A_Method.Companion.removeDefinition
import avail.descriptor.methods.A_SemanticRestriction
import avail.descriptor.methods.A_Sendable.Companion.bodyBlock
import avail.descriptor.methods.A_Sendable.Companion.bodySignature
import avail.descriptor.methods.A_Sendable.Companion.isAbstractDefinition
import avail.descriptor.methods.A_Sendable.Companion.isForwardDefinition
import avail.descriptor.methods.A_Sendable.Companion.isMethodDefinition
import avail.descriptor.methods.AbstractDefinitionDescriptor
import avail.descriptor.methods.AbstractDefinitionDescriptor.Companion.newAbstractDefinition
import avail.descriptor.methods.DefinitionDescriptor
import avail.descriptor.methods.ForwardDefinitionDescriptor
import avail.descriptor.methods.ForwardDefinitionDescriptor.Companion.newForwardDefinition
import avail.descriptor.methods.GrammaticalRestrictionDescriptor.Companion.newGrammaticalRestriction
import avail.descriptor.methods.MacroDescriptor
import avail.descriptor.methods.MacroDescriptor.Companion.newMacroDefinition
import avail.descriptor.methods.MethodDefinitionDescriptor
import avail.descriptor.methods.MethodDefinitionDescriptor.Companion.newMethodDefinition
import avail.descriptor.methods.MethodDescriptor.SpecialMethodAtom
import avail.descriptor.methods.SemanticRestrictionDescriptor
import avail.descriptor.module.A_Module
import avail.descriptor.module.A_Module.Companion.addLexer
import avail.descriptor.module.A_Module.Companion.addPrivateName
import avail.descriptor.module.A_Module.Companion.addSeal
import avail.descriptor.module.A_Module.Companion.buildFilteredBundleTree
import avail.descriptor.module.A_Module.Companion.createLexicalScanner
import avail.descriptor.module.A_Module.Companion.hasAncestor
import avail.descriptor.module.A_Module.Companion.importedNames
import avail.descriptor.module.A_Module.Companion.moduleAddDefinition
import avail.descriptor.module.A_Module.Companion.moduleAddGrammaticalRestriction
import avail.descriptor.module.A_Module.Companion.moduleAddMacro
import avail.descriptor.module.A_Module.Companion.moduleAddSemanticRestriction
import avail.descriptor.module.A_Module.Companion.moduleState
import avail.descriptor.module.A_Module.Companion.newNames
import avail.descriptor.module.A_Module.Companion.privateNames
import avail.descriptor.module.A_Module.Companion.resolveForward
import avail.descriptor.module.A_Module.Companion.shortModuleNameNative
import avail.descriptor.module.A_Module.Companion.trueNamesForStringName
import avail.descriptor.module.ModuleDescriptor
import avail.descriptor.module.ModuleDescriptor.State.Loading
import avail.descriptor.numbers.A_Number.Companion.equalsInt
import avail.descriptor.parsing.A_DefinitionParsingPlan
import avail.descriptor.parsing.A_Lexer
import avail.descriptor.parsing.A_Lexer.Companion.definitionModule
import avail.descriptor.parsing.A_Lexer.Companion.lexerApplicability
import avail.descriptor.parsing.A_Lexer.Companion.lexerFilterFunction
import avail.descriptor.parsing.A_Lexer.Companion.lexerMethod
import avail.descriptor.parsing.A_Lexer.Companion.setLexerApplicability
import avail.descriptor.parsing.A_ParsingPlanInProgress
import avail.descriptor.parsing.LexerDescriptor.Companion.newLexer
import avail.descriptor.parsing.ParsingPlanInProgressDescriptor.Companion.newPlanInProgress
import avail.descriptor.phrases.A_Phrase
import avail.descriptor.phrases.A_Phrase.Companion.startingLineNumber
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObject.Companion.error
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.sets.A_Set
import avail.descriptor.sets.A_Set.Companion.asTuple
import avail.descriptor.sets.A_Set.Companion.hasElement
import avail.descriptor.sets.A_Set.Companion.setSize
import avail.descriptor.sets.A_Set.Companion.setUnionCanDestroy
import avail.descriptor.sets.A_Set.Companion.setWithElementCanDestroy
import avail.descriptor.sets.A_Set.Companion.setWithoutElementCanDestroy
import avail.descriptor.sets.SetDescriptor.Companion.emptySet
import avail.descriptor.sets.SetDescriptor.Companion.setFromCollection
import avail.descriptor.sets.SetDescriptor.Companion.singletonSet
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.tuples.StringDescriptor.Companion.formatString
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.acceptsArgTypesFromFunctionType
import avail.descriptor.types.A_Type.Companion.argsTupleType
import avail.descriptor.types.A_Type.Companion.isSubtypeOf
import avail.descriptor.types.A_Type.Companion.lowerBound
import avail.descriptor.types.A_Type.Companion.returnType
import avail.descriptor.types.A_Type.Companion.sizeRange
import avail.descriptor.types.A_Type.Companion.upperBound
import avail.descriptor.types.EnumerationTypeDescriptor.Companion.booleanType
import avail.descriptor.types.FunctionTypeDescriptor
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.PARSE_PHRASE
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AmbiguousNameException
import avail.exceptions.AvailErrorCode.E_INCORRECT_NUMBER_OF_ARGUMENTS
import avail.exceptions.AvailErrorCode.E_MACRO_MUST_RETURN_A_PHRASE
import avail.exceptions.AvailErrorCode.E_METHOD_RETURN_TYPE_NOT_AS_FORWARD_DECLARED
import avail.exceptions.AvailErrorCode.E_REDEFINED_WITH_SAME_ARGUMENT_TYPES
import avail.exceptions.AvailErrorCode.E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS
import avail.exceptions.MalformedMessageException
import avail.exceptions.SignatureException
import avail.interpreter.Primitive
import avail.interpreter.effects.LoadingEffect
import avail.interpreter.effects.LoadingEffectToAddDefinition
import avail.interpreter.effects.LoadingEffectToAddMacro
import avail.interpreter.effects.LoadingEffectToRunPrimitive
import avail.interpreter.execution.AvailLoader.Phase.COMPILING
import avail.interpreter.execution.AvailLoader.Phase.EXECUTING_FOR_COMPILE
import avail.interpreter.execution.AvailLoader.Phase.EXECUTING_FOR_LOAD
import avail.interpreter.execution.AvailLoader.Phase.INITIALIZING
import avail.interpreter.execution.AvailLoader.Phase.LOADING
import avail.interpreter.execution.AvailLoader.Phase.UNLOADING
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerKeywordBody
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerKeywordFilter
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerOperatorBody
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerOperatorFilter
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerSlashStarCommentBody
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerSlashStarCommentFilter
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerStringBody
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerStringFilter
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerWhitespaceBody
import avail.interpreter.primitive.bootstrap.lexing.P_BootstrapLexerWhitespaceFilter
import avail.interpreter.primitive.methods.P_Alias
import avail.io.TextInterface
import avail.utility.StackPrinter
import avail.utility.evaluation.Combinator.recurse
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An `AvailLoader` is responsible for orchestrating module-level side-effects,
 * such as those caused by adding [method][MethodDefinitionDescriptor],
 * [abstract][AbstractDefinitionDescriptor], and
 * [forward][ForwardDefinitionDescriptor] definitions.  Also
 * [macros][MacroDescriptor], [A_Lexer]s, [A_SemanticRestriction]s,
 * [A_GrammaticalRestriction]s, and method [seals][AvailRuntime.addSeal].
 *
 * @constructor
 *
 * @property runtime
 *   The current [AvailRuntime].
 * @property module
 *   The Avail [module][ModuleDescriptor] undergoing loading.
 * @property textInterface
 *   The [text&#32;interface][TextInterface] for any [fibers][A_Fiber] started by
 *   this [AvailLoader].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
class AvailLoader
constructor(
	val runtime: AvailRuntime,
	val module: A_Module,
	val textInterface: TextInterface)
{
	/**
	 * A class that tracks all visible [A_Lexer]s while compiling a module.
	 */
	class LexicalScanner
	{
		/**
		 * The [List] of all [lexers][A_Lexer] which are visible within the
		 * module being compiled.
		 */
		val allVisibleLexers = mutableListOf<A_Lexer>()

		/**
		 * When set, fail on attempts to change the lexical scanner.  This is
		 * a safety measure.
		 */
		@Volatile
		var frozen = false

		/**
		 * Ensure new [A_Lexer]s are not added after this point.  This is a
		 * safety measure.
		 */
		fun freezeFromChanges()
		{
			assert(!frozen)
			frozen = true
		}

		/**
		 * A 256-way dispatch table that takes a Latin-1 character's Unicode
		 * codepoint (which is in [0..255]) to a [tuple][A_Tuple] of
		 * [lexers][A_Lexer].  Non-Latin1 characters (i.e., with codepoints
		 * ≥ 256) are tracked separately in [nonLatin1Lexers].
		 *
		 * This array is populated lazily and incrementally, so if an entry is
		 * null, it should be constructed by testing the character against all
		 * visible lexers' filter functions.  Place the ones that pass into a
		 * set, then normalize it by looking up that set in a
		 * [map][canonicalLexerTuples] from such sets to tuples.  This causes
		 * two equal sets of lexers to be canonicalized to the same tuple,
		 * thereby reusing it.
		 *
		 * When a new lexer is defined, we null all entries of this dispatch
		 * table and clear the supplementary map, allowing the entries to be
		 * incrementally constructed.  We also clear the map from lexer sets to
		 * canonical tuples.
		 */
		private val latin1ApplicableLexers = AtomicReferenceArray<A_Tuple>(256)

		/**
		 * A [ConcurrentHashMap] from non-Latin-1 codepoint (i.e., ≥ 256) to the
		 * tuple of lexers that should run when that character is encountered at
		 * a lexing point.
		 */
		private val nonLatin1Lexers = ConcurrentHashMap<Int, A_Tuple>()

		/**
		 * The canonical mapping from each set of lexers to a tuple of lexers.
		 */
		private val canonicalLexerTuples = mutableMapOf<A_Set, A_Tuple>()

		/**
		 * Add an [A_Lexer].  Update not just the current lexing information for
		 * this loader, but also the specified atom's bundle's method and the
		 * current module.
		 *
		 * This must be called as an L1-safe task, which precludes execution of
		 * Avail code while it's running.  That does not preclude other
		 * non-Avail code from running (i.e., other L1-safe tasks), so this
		 * method is synchronized to achieve that safety.
		 *
		 * @param lexer
		 *   The [A_Lexer] to add.
		 */
		@Synchronized
		fun addLexer(lexer: A_Lexer)
		{
			assert(!frozen)
			lexer.lexerMethod.lexer = lexer
			val module: A_Module = lexer.definitionModule
			if (module.notNil)
			{
				if (module.moduleState == Loading)
				{
					module.addLexer(lexer)
				}
			}
			// Update the loader's lexing tables...
			allVisibleLexers.add(lexer)
			// Since the existence of at least one non-null entry in the Latin-1
			// or non-Latin-1 tables implies there must be at least one
			// canonical tuple of lexers, we can skip clearing the tables if the
			// canonical map is empty.
			if (canonicalLexerTuples.isNotEmpty())
			{
				for (i in 0 .. 255)
				{
					latin1ApplicableLexers[i] = null
				}
				nonLatin1Lexers.clear()
				canonicalLexerTuples.clear()
			}
		}

		/**
		 * Collect the [lexers][A_Lexer] that should run when we encounter a
		 * character with the given ([Int]) code point, then pass this tuple of
		 * lexers to the supplied Kotlin function.
		 *
		 * We pass it forward rather than return it, since sometimes this
		 * requires lexer filter functions to run, which we must not do
		 * synchronously.  However, if the lexer filters have already run for
		 * this code point, we *may* invoke the continuation synchronously for
		 * performance.
		 *
		 * @param lexingState
		 *   The [LexingState] at which the lexical scanning is happening.
		 * @param codePoint
		 *   The full Unicode code point in the range 0..1,114,111.
		 * @param continuation
		 *   What to invoke with the tuple of tokens at this position.
		 * @param onFailure
		 *   What to do if lexical scanning fails.
		 */
		fun getLexersForCodePointThen(
			lexingState: LexingState,
			codePoint: Int,
			continuation: (A_Tuple)->Unit,
			onFailure: (Map<A_Lexer, Throwable>)->Unit)
		{
			if (codePoint and 255.inv() == 0)
			{
				latin1ApplicableLexers[codePoint]?.let {
					continuation(it)
					return
				}
				// Run the filters to produce the set of applicable lexers, then
				// use the canonical map to make it a tuple, then invoke the
				// continuation with it.
				selectLexersPassingFilterThen(lexingState, codePoint)
				{ applicable, failures ->
					when
					{
						failures.isNotEmpty() ->
						{
							onFailure(failures)
							// Don't cache the successful lexer filter results,
							// because we shouldn't continue and neither should
							// lexing of any codePoint equal to the one that
							// caused this trouble.
						}
						else ->
						{
							val lexers = canonicalTupleOfLexers(applicable)
							// Just replace it, even if another thread beat us
							// to the punch, since it's semantically idempotent.
							latin1ApplicableLexers[codePoint] = lexers
							continuation(lexers)
						}
					}
				}
				return
			}

			// It's non-Latin1.
			val tuple = nonLatin1Lexers[codePoint]
			if (tuple !== null) return continuation(tuple)
			// Run the filters to produce the set of applicable lexers, then use
			// the canonical map to make it a tuple, then invoke the
			// continuation with it.
			selectLexersPassingFilterThen(lexingState, codePoint)
			{ applicable, failures ->
				if (failures.isNotEmpty())
				{
					onFailure(failures)
					// Don't cache the successful lexer filter results, because
					// we shouldn't continue and neither should lexing of any
					// codePoint equal to the one that caused this trouble.
					return@selectLexersPassingFilterThen
				}
				val lexers = canonicalTupleOfLexers(applicable)
				// Just replace it, even if another thread beat us to the punch,
				// since it's semantically idempotent.
				nonLatin1Lexers[codePoint] = lexers
				continuation(lexers)
			}
		}

		/**
		 * Given an [A_Set] of [A_Lexer]s applicable for some character, look up
		 * the corresponding canonical [A_Tuple], recording it if necessary.
		 */
		private fun canonicalTupleOfLexers(applicable: A_Set): A_Tuple =
			synchronized(canonicalLexerTuples) {
				canonicalLexerTuples[applicable] ?: run {
					val tuple = applicable.asTuple.makeShared()
					canonicalLexerTuples[applicable.makeShared()] = tuple
					tuple
				}
			}

		/**
		 * Collect the lexers that should run when we encounter a character with
		 * the given (int) code point, then pass this set of lexers to the
		 * supplied function.
		 *
		 * We pass it forward rather than return it, since sometimes this
		 * requires lexer filter functions to run, which we must not do
		 * synchronously.  However, if the lexer filters have already run for
		 * this code point, we *may* invoke the continuation synchronously for
		 * performance.
		 *
		 * @param lexingState
		 *   The [LexingState] at which scanning encountered this codePoint for
		 *   the first time.
		 * @param codePoint
		 *   The full Unicode code point in the range 0..1114111.
		 * @param continuation
		 *   What to invoke with the [set][A_Set] of [lexers][A_Lexer] and a
		 *   (normally empty) map from lexer to throwable, indicating lexer
		 *   filter invocations that raised exceptions.
		 */
		private fun selectLexersPassingFilterThen(
			lexingState: LexingState,
			codePoint: Int,
			continuation: (A_Set, Map<A_Lexer, Throwable>)->Unit)
		{
			val applicableLexers = mutableListOf<A_Lexer>()
			val undecidedLexers = mutableListOf<A_Lexer>()
			when (codePoint)
			{
				in 0 .. 255 -> allVisibleLexers.forEach {
					when (it.lexerApplicability(codePoint))
					{
						null -> undecidedLexers.add(it)
						true -> applicableLexers.add(it)
						false -> { }
					}
				}
				else -> undecidedLexers.addAll(allVisibleLexers)
			}
			var countdown = undecidedLexers.size
			if (countdown == 0)
			{
				continuation(setFromCollection(applicableLexers), emptyMap())
				return
			}
			// Initially use the immutable emptyMap for the failureMap, but
			// replace it if/when the first error happens.
			val argsList = listOf(fromCodePoint(codePoint))
			val compilationContext = lexingState.compilationContext
			val loader = compilationContext.loader
			val joinLock = ReentrantLock()
			val failureMap = mutableMapOf<A_Lexer, Throwable>()
			val fibers = undecidedLexers.map { lexer ->
				val fiber = newLoaderFiber(booleanType, loader)
				{
					formatString(
						"Check lexer filter %s for U+%06x",
						lexer.lexerMethod.chooseBundle(loader.module)
							.message.atomName,
						codePoint)
				}
				lexingState.setFiberContinuationsTrackingWork(
					fiber,
					{ boolObject: AvailObject ->
						val boolValue = boolObject.extractBoolean
						if (codePoint in 0 .. 255)
						{
							// Cache the filter result with the lexer
							// itself, so other modules can reuse it.
							lexer.setLexerApplicability(codePoint, boolValue)
						}
						val countdownHitZero = joinLock.withLock {
							if (boolValue)
							{
								applicableLexers.add(lexer)
							}
							countdown--
							assert(countdown >= 0)
							countdown == 0
						}
						if (countdownHitZero)
						{
							// This was the fiber reporting the last result.
							continuation(
								setFromCollection(applicableLexers),
								failureMap)
						}
					},
					{ throwable: Throwable ->
						val countdownHitZero = joinLock.withLock {
							failureMap[lexer] = throwable
							countdown--
							assert(countdown >= 0)
							countdown == 0
						}
						if (countdownHitZero)
						{
							// This was the fiber reporting the last result
							// (a fiber failure).
							continuation(
								setFromCollection(applicableLexers),
								failureMap)
						}
					})
				fiber
			}
			// Launch the fibers only after they've all been created.  That's
			// because we increment the queued count while setting the fibers'
			// success/failure continuations, with the corresponding increments
			// of the completed counts dealt with by wrapping the continuations.
			// If a fiber ran to completion before we could create them all, the
			// counters could collide, running the noMoreWorkUnits action before
			// all fibers got a chance to run.
			fibers.forEachIndexed { i, fiber ->
				loader.runtime.runOutermostFunction(
					fiber,
					undecidedLexers[i].lexerFilterFunction,
					argsList)
			}
		}
	}

	/**
	 * The macro-state of the loader.  During compilation from a file, a loader
	 * will ratchet between [COMPILING] while parsing a top-level statement, and
	 * [EXECUTING_FOR_COMPILE] while executing the compiled statement.
	 * Similarly, when loading from a file, the loader's [phase] alternates
	 * between [LOADING] and [EXECUTING_FOR_LOAD].
	 *
	 * @constructor
	 *
	 * @property isExecuting
	 *   Whether this phase represents a time when execution is happening.
	 */
	enum class Phase(val isExecuting: Boolean = false)
	{
		/** No statements have been loaded or compiled yet. */
		INITIALIZING,

		/** A top-level statement is being compiled. */
		COMPILING,

		/** A top-level statement is being loaded from a repository. */
		LOADING,

		/** A top-level parsed statement is being executed. */
		EXECUTING_FOR_COMPILE(true),

		/** A top-level deserialized statement is being executed. */
		EXECUTING_FOR_LOAD(true),

		/** The fully-loaded module is now being unloaded. */
		UNLOADING,

		/**
		 * The [AvailLoader] is parsing an expression within some anonymous
		 * module.  The current fiber is attempting to execute some Avail code
		 * as requested by the compilation.
		 *
		 * Note that this is permitted after loading has completed, but also
		 * during loading if the code being loaded explicitly creates an
		 * anonymous module and uses it to compile an expression.  In both cases
		 * the current loader will be tied to an anonymous module.
		 */
		// TODO: [MvG] Finish supporting eval.
		@Suppress("unused")
		COMPILING_FOR_EVAL(false);
	}

	/** The current loading setPhase. */
	@Volatile
	private var phase: Phase = INITIALIZING

	/**
	 * Get the current loading [Phase].
	 *
	 * @return
	 *   The loader's current [Phase].
	 */
	fun phase(): Phase = phase

	/**
	 * Set the current loading [Phase].
	 *
	 * @param newPhase
	 *   The new [Phase].
	 */
	fun setPhase(newPhase: Phase) {
		phase = newPhase
	}

	/**
	 * Used for extracting tokens from the source text. Start by using the
	 * module header lexical scanner, and replace it after the header has been
	 * fully parsed.
	 */
	private var lexicalScanner: LexicalScanner? = moduleHeaderLexicalScanner

	/**
	 * Answer the [LexicalScanner] used for creating tokens from source
	 * code for this [AvailLoader].
	 *
	 * @return
	 *   The [LexicalScanner], which must not be `null`.
	 */
	fun lexicalScanner(): LexicalScanner = lexicalScanner!!

	/**
	 * Answer the [message&#32;bundle&#32;tree][MessageBundleTreeDescriptor]
	 * that this [AvailLoader] is using to parse its [module][ModuleDescriptor].
	 * Start it out as the [moduleHeaderBundleRoot] for parsing the header, then
	 * switch it out to parse the body.
	 */
	private var rootBundleTree: A_BundleTree = moduleHeaderBundleRoot

	/**
	 * Answer the [message&#32;bundle&#32;tree][MessageBundleTreeDescriptor]
	 * that this [AvailLoader] is using to parse its [module][ModuleDescriptor].
	 * It must not be `null`.
	 *
	 * @return
	 *   A message bundle tree.
	 */
	fun rootBundleTree(): A_BundleTree = rootBundleTree

	/**
	 * During module compilation, this holds the top-level zero-argument block
	 * phrase that wraps the parsed statement, and is in the process of being
	 * evaluated.
	 */
	var topLevelStatementBeingCompiled: A_Phrase? = null

	/**
	 * A stream on which to serialize each [ModuleManifestEntry] when the
	 * definition actually occurs during compilation.  After compilation, the
	 * bytes of this stream are written to a record whose index is captured in
	 * the [A_Module]'s [ModuleDescriptor.ObjectSlots.ALL_MANIFEST_ENTRIES], and
	 * fetched from the repository and decoded into a pojo array when needed.
	 */
	var manifestEntries: MutableList<ModuleManifestEntry>? = null

	/**
	 * A flag that is cleared before executing each top-level statement of a
	 * module, and set whenever execution of the statement causes behavior that
	 * can't simply be summarized by a sequence of [LoadingEffect]s.
	 */
	@Volatile
	private var statementCanBeSummarized = true

	/**
	 * A flag that indicates whether we are attempting to determine whether an
	 * expression can be summarized into a series of [LoadingEffect]s.
	 */
	private var determiningSummarizability = false

	/**
	 * Replace the boolean that indicates whether the current statement can be
	 * summarized into a sequence of [LoadingEffect]s.  It is set to true before
	 * executing a top-level statement, and set to false if an activity is
	 * performed that cannot be summarized.
	 *
	 * @param summarizable
	 *   The new value of the flag.
	 */
	@Synchronized
	fun statementCanBeSummarized(summarizable: Boolean)
	{
		if (determiningSummarizability)
		{
			if (debugUnsummarizedStatements
				&& !summarizable
				&& statementCanBeSummarized)
			{
				// Here's a good place for a breakpoint, to see why an
				// expression couldn't be summarized.
				val e = Throwable().fillInStackTrace()
				println("Disabled summary:\n${StackPrinter.trace(e)}")
			}
			statementCanBeSummarized = summarizable
		}
	}

	/**
	 * Answer whether the current statement can be summarized into a sequence of
	 * [LoadingEffect]s.
	 *
	 * @return
	 *   The current value of the flag.
	 */
	fun statementCanBeSummarized() = statementCanBeSummarized

	/**
	 * The sequence of effects performed by the current top-level statement of a
	 * module being compiled.
	 */
	private val effectsAddedByTopStatement = mutableListOf<LoadingEffect>()

	/**
	 * Record a [LoadingEffect] to ensure it will be replayed when the module
	 * which is currently being compiled is later loaded.
	 *
	 * @param anEffect
	 *   The effect to record.
	 */
	@Synchronized
	fun recordEffect(anEffect: LoadingEffect)
	{
		if (determiningSummarizability)
		{
			effectsAddedByTopStatement.add(anEffect)
		}
	}

	/**
	 * Set a flag that indicates we are determining if the effects of running
	 * a function can be summarized, and if so into what [LoadingEffect]s.
	 */
	@Synchronized
	fun startRecordingEffects()
	{
		assert(!determiningSummarizability)
		determiningSummarizability = true
		statementCanBeSummarized = enableFastLoader
		effectsAddedByTopStatement.clear()
	}

	/**
	 * Clear the flag that indicates whether we are determining if the effects
	 * of running a function can be summarized into [LoadingEffect]s.
	 */
	@Synchronized
	fun stopRecordingEffects()
	{
		assert(determiningSummarizability)
		determiningSummarizability = false
	}

	/**
	 * Answer the list of [LoadingEffect]s.
	 *
	 * @return
	 *   Answer the recorded [LoadingEffect]s.
	 */
	@Synchronized
	fun recordedEffects(): List<LoadingEffect> =
		effectsAddedByTopStatement.toList()

	/**
	 * Set up the [rootBundleTree] and [lexicalScanner] for compiling the body
	 * of the module.
	 */
	fun prepareForCompilingModuleBody()
	{
		rootBundleTree = module.buildFilteredBundleTree()
		lexicalScanner = module.createLexicalScanner()
	}

	/**
	 * Clear the [rootBundleTree] and [lexicalScanner] in preparation for
	 * loading (not compiling) the body of the module.
	 */
	fun prepareForLoadingModuleBody()
	{
		rootBundleTree = nil
		lexicalScanner = null
	}

	/** The currently unresolved forward method declarations. */
	var pendingForwards: A_Set = emptySet

	/**
	 * The given forward is in the process of being resolved. A real definition
	 * is about to be added to the method tables, so remove the forward now.
	 *
	 * @param forwardDefinition
	 *   A [forward][ForwardDefinitionDescriptor] declaration.
	 */
	private fun removeForward(forwardDefinition: A_Definition)
	{
		val method = forwardDefinition.definitionMethod
		when
		{
			!pendingForwards.hasElement(forwardDefinition) ->
				error("Inconsistent forward declaration handling code")
			!method.includesDefinition(forwardDefinition) ->
				error("Inconsistent forward declaration handling code")
		}
		pendingForwards =
			pendingForwards.setWithoutElementCanDestroy(forwardDefinition, true)
				.makeShared()
		method.removeDefinition(forwardDefinition)
		module.resolveForward(forwardDefinition)
	}

	/**
	 * This is a forward declaration of a method. Insert an appropriately
	 * stubbed definition in the module's method dictionary, and add it to the
	 * list of methods needing to be declared later in this module.
	 *
	 * @param methodName
	 *   The [method&#32;name][AtomDescriptor].
	 * @param bodySignature
	 *   A function [type][MethodDefinitionDescriptor] at which to create a
	 *   forward definition.
	 * @throws MalformedMessageException
	 *   If the message name is malformed.
	 * @throws SignatureException
	 *   If there is a problem with the signature.
	 */
	@Throws(
		MalformedMessageException::class,
		SignatureException::class)
	fun addForwardStub(
		methodName: A_Atom,
		bodySignature: A_Type)
	{
		methodName.makeShared()
		bodySignature.makeShared()
		val bundle: A_Bundle = methodName.bundleOrCreate()
		val splitter: MessageSplitter = bundle.messageSplitter
		splitter.checkImplementationSignature(bodySignature)
		val bodyArgsTupleType = bodySignature.argsTupleType
		// Add the stubbed method definition.
		val method: A_Method = bundle.bundleMethod
		method.definitionsTuple.forEach { definition ->
			val existingType = definition.bodySignature()
			if (existingType.argsTupleType.equals(bodyArgsTupleType))
			{
				throw SignatureException(E_REDEFINED_WITH_SAME_ARGUMENT_TYPES)
			}
			if (existingType.acceptsArgTypesFromFunctionType(bodySignature))
			{
				if (!bodySignature.returnType.isSubtypeOf(
						existingType.returnType))
				{
					throw SignatureException(
						E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS)
				}
			}
			if (bodySignature.acceptsArgTypesFromFunctionType(existingType))
			{
				if (!existingType.returnType.isSubtypeOf(
						bodySignature.returnType))
				{
					throw SignatureException(
						E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS)
				}
			}
		}
		// Only bother with adding and resolving forwards during compilation.
		if (phase == EXECUTING_FOR_COMPILE)
		{
			val newForward: A_Definition = newForwardDefinition(
				method, module, bodySignature)
			method.methodAddDefinition(newForward)
			recordEffect(LoadingEffectToAddDefinition(bundle, newForward))
			val theModule = module
			val root = rootBundleTree()
			theModule.lock {
				theModule.moduleAddDefinition(newForward)
				pendingForwards =
					pendingForwards.setWithElementCanDestroy(newForward, true)
						.makeShared()
				val plan = bundle.definitionParsingPlans.mapAt(newForward)
				val planInProgress = newPlanInProgress(plan, 1)
				root.addPlanInProgress(planInProgress)
			}
		}
	}

	/**
	 * Add the method definition. The precedence rules can change at any time.
	 *
	 * @param methodName
	 *   The method's [name][AtomDescriptor].
	 * @param bodyBlock
	 *   The body [function][FunctionDescriptor].
	 * @throws MalformedMessageException
	 *   If the message name is malformed.
	 * @throws SignatureException
	 *   If the signature is invalid.
	 */
	@Throws(
		MalformedMessageException::class,
		SignatureException::class)
	fun addMethodBody(
		methodName: A_Atom,
		bodyBlock: A_Function)
	{
		assert(methodName.isAtom)
		assert(bodyBlock.isFunction)
		val bundle = methodName.bundleOrCreate()
		val splitter = bundle.messageSplitter
		splitter.checkImplementationSignature(bodyBlock.kind())
		val numArgs = splitter.numberOfArguments
		if (bodyBlock.code().numArgs() != numArgs)
		{
			throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
		}
		addDefinition(
			methodName.makeShared(),
			newMethodDefinition(
				bundle.bundleMethod,
				module,
				bodyBlock.makeShared()))
	}

	/**
	 * Add the [abstract][AbstractDefinitionDescriptor] method signature. A
	 * class is considered abstract if there are any abstract methods that
	 * haven't been overridden with definitions for it.
	 *
	 * @param methodName
	 *   A method [name][AtomDescriptor].
	 * @param bodySignature
	 *   The function [type][FunctionTypeDescriptor].
	 * @throws MalformedMessageException
	 *   If the message name is malformed.
	 * @throws SignatureException
	 *   If there is a problem with the signature.
	 */
	@Throws(MalformedMessageException::class, SignatureException::class)
	fun addAbstractSignature(
		methodName: A_Atom,
		bodySignature: A_Type)
	{
		val bundle: A_Bundle = methodName.bundleOrCreate()
		val splitter: MessageSplitter = bundle.messageSplitter
		val numArgs = splitter.numberOfArguments
		val bodyArgsSizes = bodySignature.argsTupleType.sizeRange
		if (!bodyArgsSizes.lowerBound.equalsInt(numArgs)
			|| !bodyArgsSizes.upperBound.equalsInt(numArgs))
		{
			throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
		}
		assert(bodyArgsSizes.upperBound.equalsInt(numArgs))
		{
			"Wrong number of arguments in abstract method signature"
		}
		addDefinition(
			methodName.makeShared(),
			newAbstractDefinition(
				bundle.bundleMethod,
				module,
				bodySignature.makeShared()))
	}

	/**
	 * Add the new [A_Definition] to its [A_Method], via the captured [A_Bundle]
	 * (otherwise using a random bundle from the method would break replay if a
	 * [P_Alias] happens after the definition in the same grouped step). Also
	 * update the loader's [rootBundleTree] as needed.
	 *
	 * @param newDefinition
	 *   The definition to add.
	 * @throws SignatureException
	 *   If the signature disagrees with existing definitions and forwards.
	 */
	@Throws(SignatureException::class)
	private fun addDefinition(
		methodName: A_Atom,
		newDefinition: A_Definition)
	{
		val method = newDefinition.definitionMethod
		val bodySignature = newDefinition.bodySignature()
		val argsTupleType = bodySignature.argsTupleType
		var forward: A_Definition? = null
		method.definitionsTuple.forEach { existingDefinition ->
			val existingType = existingDefinition.bodySignature()
			val same = existingType.argsTupleType.equals(argsTupleType)
			if (same)
			{
				when
				{
					!existingDefinition.isForwardDefinition() ->
						throw SignatureException(
							E_REDEFINED_WITH_SAME_ARGUMENT_TYPES)
					!existingType.returnType.equals(bodySignature.returnType) ->
						throw SignatureException(
							E_METHOD_RETURN_TYPE_NOT_AS_FORWARD_DECLARED)
				}
				forward = existingDefinition
			}
			if (existingType.acceptsArgTypesFromFunctionType(bodySignature))
			{
				if (!bodySignature.returnType.isSubtypeOf(
						existingType.returnType))
				{
					throw SignatureException(
						E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS)
				}
			}
			if (bodySignature.acceptsArgTypesFromFunctionType(existingType))
			{
				if (!existingType.returnType.isSubtypeOf(
						bodySignature.returnType))
				{
					throw SignatureException(
						E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS)
				}
			}
		}
		if (phase == EXECUTING_FOR_COMPILE)
		{
			module.lock {
				val root = rootBundleTree()
				forward?.let { forward ->
					method.bundles.forEach { bundle ->
						if (module.hasAncestor(bundle.message.issuingModule))
						{
							// Remove the appropriate forwarder plan from the
							// bundle tree.
							val plan: A_DefinitionParsingPlan =
								bundle.definitionParsingPlans.mapAt(forward)
							val planInProgress = newPlanInProgress(plan, 1)
							root.removePlanInProgress(planInProgress)
						}
					}
					removeForward(forward)
				}
				try
				{
					method.methodAddDefinition(newDefinition)
				}
				catch (e: SignatureException)
				{
					assert(false) { "Signature was already vetted" }
					return@lock
				}
				recordEffect(
					LoadingEffectToAddDefinition(
						methodName.bundleOrCreate(), newDefinition))
				method.bundles.forEach { bundle ->
					if (module.hasAncestor(bundle.message.issuingModule))
					{
						val plan: A_DefinitionParsingPlan =
							bundle.definitionParsingPlans.mapAt(newDefinition)
						val planInProgress = newPlanInProgress(plan, 1)
						root.addPlanInProgress(planInProgress)
					}
				}
				module.moduleAddDefinition(newDefinition)
				val topStart = topLevelStatementBeingCompiled!!
					.startingLineNumber
				manifestEntries!!.add(
					when
					{
						newDefinition.isMethodDefinition() ->
						{
							val body = newDefinition.bodyBlock()
							ModuleManifestEntry(
								SideEffectKind.METHOD_DEFINITION_KIND,
								methodName.atomName.asNativeString(),
								topStart,
								body.code().codeStartingLineNumber,
								body)
						}
						newDefinition.isForwardDefinition() ->
							ModuleManifestEntry(
								SideEffectKind.FORWARD_METHOD_DEFINITION_KIND,
								methodName.atomName.asNativeString(),
								topStart,
								topStart)
						newDefinition.isAbstractDefinition() ->
							ModuleManifestEntry(
								SideEffectKind.ABSTRACT_METHOD_DEFINITION_KIND,
								methodName.atomName.asNativeString(),
								topStart,
								topStart)
						else -> throw UnsupportedOperationException(
							"Unknown definition kind")
					})
			}
		}
		else
		{
			try
			{
				method.methodAddDefinition(newDefinition)
			}
			catch (e: SignatureException)
			{
				assert(false) { "Signature was already vetted" }
				return
			}
			module.moduleAddDefinition(newDefinition)
		}
	}

	/**
	 * Add the macro definition. The precedence rules can not change after the
	 * first definition is encountered, so set them to 'no restrictions' if
	 * they're not set already.
	 *
	 * @param methodName
	 *   The macro's name, an [atom][AtomDescriptor].
	 * @param macroBody
	 *   A [function][FunctionDescriptor] that transforms phrases.
	 * @param prefixFunctions
	 *   The tuple of functions to run during macro parsing, corresponding with
	 *   occurrences of section checkpoints ("§") in the macro name.
	 * @throws MalformedMessageException
	 *   If the macro signature is malformed.
	 * @throws SignatureException
	 *   If the macro signature is invalid.
	 */
	@Throws(
		MalformedMessageException::class,
		SignatureException::class)
	fun addMacroBody(
		methodName: A_Atom,
		macroBody: A_Function,
		prefixFunctions: A_Tuple,
		ignoreSeals: Boolean)
	{
		assert(methodName.isAtom)
		assert(macroBody.isFunction)
		val bundle = methodName.bundleOrCreate()
		val splitter = bundle.messageSplitter
		val numArgs = splitter.numberOfArguments
		val macroCode = macroBody.code()
		when
		{
			macroCode.numArgs() != numArgs ->
				throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
			!macroCode.functionType().returnType.isSubtypeOf(
					PARSE_PHRASE.mostGeneralType) ->
				throw SignatureException(E_MACRO_MUST_RETURN_A_PHRASE)
		}
		// Make it so we can safely hold onto these things in the VM.
		methodName.makeShared()
		macroBody.makeShared()
		// Add the macro definition.
		val macroDefinition = newMacroDefinition(
			bundle, module, macroBody, prefixFunctions)
		val macroBodyType = macroBody.kind()
		val argsType = macroBodyType.argsTupleType
		// Note: Macro definitions don't have to satisfy a covariance
		// relationship with their result types, since they're static.
		if (bundle.macrosTuple.any { existingDef ->
			argsType.equals(existingDef.bodySignature().argsTupleType)
		})
		{
			throw SignatureException(E_REDEFINED_WITH_SAME_ARGUMENT_TYPES)
		}
		// This may throw a SignatureException prior to making semantic changes
		// to the runtime.
		bundle.bundleAddMacro(macroDefinition, ignoreSeals)
		module.moduleAddMacro(macroDefinition)
		if (phase == EXECUTING_FOR_COMPILE)
		{
			recordEffect(LoadingEffectToAddMacro(bundle, macroDefinition))
			module.lock {
				manifestEntries!!.add(
					ModuleManifestEntry(
						SideEffectKind.MACRO_DEFINITION_KIND,
						methodName.atomName.asNativeString(),
						topLevelStatementBeingCompiled!!.startingLineNumber,
						macroCode.codeStartingLineNumber,
						macroBody))
				val plan: A_DefinitionParsingPlan =
					bundle.definitionParsingPlans.mapAt(macroDefinition)
				val planInProgress = newPlanInProgress(plan, 1)
				rootBundleTree().addPlanInProgress(planInProgress)
			}
		}
	}

	/**
	 * Add a semantic restriction to its associated method.
	 *
	 * @param restriction
	 *   A [semantic&#32;restriction][SemanticRestrictionDescriptor] that
	 *   validates the static types of arguments at call sites.
	 * @throws SignatureException
	 *   If the signature is invalid.
	 */
	@Throws(SignatureException::class)
	fun addSemanticRestriction(restriction: A_SemanticRestriction)
	{
		val method = restriction.definitionMethod()
		val function = restriction.function()
		if (function.code().numArgs() != method.numArgs)
		{
			throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
		}
		runtime.addSemanticRestriction(restriction)
		val atom = method.chooseBundle(module).message
		recordEffect(
			LoadingEffectToRunPrimitive(
				SpecialMethodAtom.SEMANTIC_RESTRICTION.bundle,
				atom,
				function))
		val theModule = module
		val code = function.code()
		theModule.lock {
			theModule.moduleAddSemanticRestriction(restriction)
			if (phase == EXECUTING_FOR_COMPILE)
			{
				manifestEntries!!.add(
					ModuleManifestEntry(
						SideEffectKind.SEMANTIC_RESTRICTION_KIND,
						atom.atomName.asNativeString(),
						topLevelStatementBeingCompiled!!.startingLineNumber,
						code.codeStartingLineNumber,
						function))
			}
		}
	}

	/**
	 * Add a seal to the method associated with the given method name.
	 *
	 * @param methodName
	 *   The method name, an [atom][AtomDescriptor].
	 * @param seal
	 *   The signature at which to seal the method.
	 * @throws MalformedMessageException
	 *   If the macro signature is malformed.
	 * @throws SignatureException
	 *   If the macro signature is invalid.
	 */
	@Throws(
		MalformedMessageException::class,
		SignatureException::class)
	fun addSeal(
		methodName: A_Atom,
		seal: A_Tuple)
	{
		assert(methodName.isAtom)
		assert(seal.isTuple)
		val bundle = methodName.bundleOrCreate()
		val splitter = bundle.messageSplitter
		if (seal.tupleSize != splitter.numberOfArguments)
		{
			throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
		}
		methodName.makeShared()
		seal.makeShared()
		runtime.addSeal(methodName, seal)
		module.addSeal(methodName, seal)
		recordEffect(
			LoadingEffectToRunPrimitive(
				SpecialMethodAtom.SEAL.bundle, methodName, seal))
		if (phase == EXECUTING_FOR_COMPILE)
		{
			manifestEntries!!.add(
				ModuleManifestEntry(
					SideEffectKind.SEAL_KIND,
					methodName.atomName.asNativeString(),
					topLevelStatementBeingCompiled!!.startingLineNumber,
					topLevelStatementBeingCompiled!!.startingLineNumber))
		}
	}

	/**
	 * The modularity scheme should prevent all inter-modular method conflicts.
	 * Precedence is specified as an array of message sets that are not allowed
	 * to be messages generating the arguments of this message.  For example,
	 * `<{"_+_"}, {"_+_", "_*_"}>` for the `"_*_"` operator makes `*` bind
	 * tighter than `+`, and also groups multiple `*`'s left-to-right.
	 *
	 * Note that we don't have to prevent L2 code from running, since the
	 * grammatical restrictions only affect parsing.  We still have to latch
	 * access to the grammatical restrictions to avoid read/write conflicts.
	 *
	 * @param parentAtoms
	 *   An [A_Set] of [A_Atom]s that name the message bundles that are to have
	 *   their arguments constrained.
	 * @param illegalArgumentMessages
	 *   The [A_Tuple] of [A_Set]s of [A_Atom]s that name methods.
	 * @throws MalformedMessageException
	 *   If one of the specified names is inappropriate as a method name.
	 * @throws SignatureException
	 *   If one of the specified names is inappropriate as a method name.
	 */
	@Throws(
		MalformedMessageException::class,
		SignatureException::class)
	fun addGrammaticalRestrictions(
		parentAtoms: A_Set,
		illegalArgumentMessages: A_Tuple)
	{
		parentAtoms.makeShared()
		illegalArgumentMessages.makeShared()
		val bundleSetList = illegalArgumentMessages.map { atomsSet ->
			var bundleSet = emptySet
			atomsSet.forEach { atom ->
				bundleSet = bundleSet.setWithElementCanDestroy(
					atom.bundleOrCreate(), true)
			}
			bundleSet.makeShared()
		}
		val bundleSetTuple = tupleFromList(bundleSetList)
		parentAtoms.forEach { parentAtom ->
			val bundle: A_Bundle = parentAtom.bundleOrCreate()
			val splitter: MessageSplitter = bundle.messageSplitter
			val numArgs = splitter.leafArgumentCount
			if (illegalArgumentMessages.tupleSize != numArgs) {
				throw SignatureException(E_INCORRECT_NUMBER_OF_ARGUMENTS)
			}
			val grammaticalRestriction =
				newGrammaticalRestriction(bundleSetTuple, bundle, module)
			val root = rootBundleTree()
			val theModule = module
			theModule.lock {
				bundle.addGrammaticalRestriction(grammaticalRestriction)
				theModule.moduleAddGrammaticalRestriction(
					grammaticalRestriction)
				if (phase != EXECUTING_FOR_COMPILE) return@lock
				// Update the message bundle tree to accommodate the new
				// grammatical restriction.
				val treesToVisit =
					ArrayDeque<Pair<A_BundleTree, A_ParsingPlanInProgress>>()
				bundle.definitionParsingPlans.forEach {
					_, plan: A_DefinitionParsingPlan ->
					treesToVisit.addLast(root to newPlanInProgress(plan, 1))
					while (treesToVisit.isNotEmpty()) {
						val (tree, planInProgress) = treesToVisit.removeLast()
						tree.updateForNewGrammaticalRestriction(
							planInProgress, treesToVisit)
					}
				}
				if (phase == EXECUTING_FOR_COMPILE)
				{
					manifestEntries!!.add(
						ModuleManifestEntry(
							SideEffectKind.GRAMMATICAL_RESTRICTION_KIND,
							parentAtom.atomName.asNativeString(),
							topLevelStatementBeingCompiled!!.startingLineNumber,
							topLevelStatementBeingCompiled!!.startingLineNumber))
				}
			}
		}
		recordEffect(
			LoadingEffectToRunPrimitive(
				SpecialMethodAtom.GRAMMATICAL_RESTRICTION.bundle,
				parentAtoms,
				illegalArgumentMessages))
	}

	/**
	 * Unbind the specified method definition from this loader and runtime.
	 *
	 * @param definition
	 *   A [definition][DefinitionDescriptor].
	 */
	fun removeDefinition(definition: A_Definition)
	{
		if (definition.isForwardDefinition())
		{
			pendingForwards =
				pendingForwards.setWithoutElementCanDestroy(definition, true)
		}
		runtime.removeDefinition(definition)
	}

	/**
	 * Unbind the specified macro definition from this loader and runtime.
	 *
	 * @param macro
	 *   A [definition][DefinitionDescriptor].
	 */
	fun removeMacro(macro: A_Macro)
	{
		runtime.removeMacro(macro)
	}

	/**
	 * Run the specified [tuple][A_Tuple] of [functions][A_Function]
	 * sequentially.
	 *
	 * @param unloadFunctions
	 *   A tuple of unload functions.
	 * @param afterRunning
	 *   What to do after every unload function has completed.
	 */
	fun runUnloadFunctions(
		unloadFunctions: A_Tuple,
		afterRunning: () -> Unit)
	{
		val size = unloadFunctions.tupleSize
		// The index into the tuple of unload functions.
		if (size == 0)
		{
			// When there's at least one unload function, the fact that it forks
			// a fiber ensures the stack doesn't grow arbitrarily deep from
			// running afterRunning functions with ever deeper recursion, due to
			// the Thread agnosticism of Graph.ParallelVisitor.  Here we have
			// no unload functions, so break the direct cyclic call explicitly
			// by queueing an action.
			runtime.execute(loaderPriority, afterRunning)
		}
		else
		{
			var index = 1
			recurse { again ->
				if (index <= size)
				{
					val currentIndex = index++
					val unloadFunction: A_Function =
						unloadFunctions.tupleAt(currentIndex)
					val fiber = newFiber(
						TOP.o, runtime, textInterface, loaderPriority)
					{
						formatString(
							"Unload function #%d/%d for module %s",
							currentIndex,
							size,
							module.shortModuleNameNative)
					}
					fiber.setSuccessAndFailure({ again() }, { again() })
					runtime.runOutermostFunction(
						fiber, unloadFunction, emptyList())
				}
				else
				{
					runtime.execute(loaderPriority, afterRunning)
				}
			}
		}
	}

	/**
	 * Look up the given [A_String] in the current [module][ModuleDescriptor]'s
	 * namespace. Answer the [atom][AtomDescriptor] associated with the string,
	 * creating the true name if necessary. A local true name always hides other
	 * true names.  If `isExplicitSubclassAtom` is true and we're creating a new
	 * atom, add the [SpecialAtom.EXPLICIT_SUBCLASSING_KEY] property.
	 *
	 * @param stringName
	 *   An Avail [string][A_String].
	 * @param isExplicitSubclassAtom
	 *   Whether to mark a new atom for creating an explicit subclass.
	 * @return
	 *   An [atom][A_Atom].
	 * @throws AmbiguousNameException
	 *   If the string could represent several different true names.
	 */
	@JvmOverloads
	@Throws(AmbiguousNameException::class)
	fun lookupName(
		stringName: A_String,
		isExplicitSubclassAtom: Boolean = false
	): A_Atom = module.lock {
		//  Check if it's already defined somewhere...
		val who = module.trueNamesForStringName(stringName)
		when (who.setSize)
		{
			1 -> who.single()
			0 ->
			{
				val trueName = createAtom(stringName, module)
				if (phase == EXECUTING_FOR_COMPILE)
				{
					val topStart = topLevelStatementBeingCompiled!!
						.startingLineNumber
					manifestEntries!!.add(
						ModuleManifestEntry(
							SideEffectKind.ATOM_DEFINITION_KIND,
							stringName.asNativeString(),
							topStart,
							topStart))
				}
				if (isExplicitSubclassAtom)
				{
					trueName.setAtomProperty(
						EXPLICIT_SUBCLASSING_KEY.atom, trueObject)
				}
				trueName.makeShared()
				module.addPrivateName(trueName)
				trueName
			}
			else -> throw AmbiguousNameException()
		}
	}

	/**
	 * Look up the given [string][StringDescriptor] in the current
	 * [module][ModuleDescriptor]'s namespace. Answer every
	 * [atom][AtomDescriptor] associated with the string. Never create a new
	 * atom.
	 *
	 * @param stringName
	 *   An Avail [string][A_String].
	 * @return
	 *   Every [atom][AtomDescriptor] associated with the name.
	 */
	fun lookupAtomsForName(stringName: A_String): A_Set = module.lock {
		val newNames = when (val name = module.newNames.mapAtOrNull(stringName))
		{
			null -> emptySet
			else -> singletonSet(name)
		}
		val publicNames =
			module.importedNames.mapAtOrNull(stringName) ?: emptySet
		val privateNames =
			module.privateNames.mapAtOrNull(stringName) ?: emptySet
		newNames
			.setUnionCanDestroy(publicNames, true)
			.setUnionCanDestroy(privateNames, true)
	}

	companion object {
		/**
		 * Allow investigation of why a top-level expression is being excluded
		 * from summarization.
		 */
		var debugUnsummarizedStatements = false

		/**
		 * Show the top-level statements that are executed during loading or
		 * compilation.
		 */
		var debugLoadedStatements = false

		/**
		 * A flag that controls whether compilation attempts to use the
		 * fast-loader to rewrite some top-level statements into a faster form.
		 */
		var enableFastLoader = true

		/**
		 * If the current [Thread] is an [AvailThread], extract its
		 * [AvailLoader], if any.  Otherwise answer `null`.
		 */
		fun currentLoaderOrNull(): AvailLoader?
		{
			val availThread =
				Thread.currentThread() as? AvailThread ?: return null
			return availThread.interpreter.availLoaderOrNull()
		}

		/**
		 * Create an `AvailLoader` suitable for unloading the specified
		 * [module][ModuleDescriptor].
		 *
		 * @param runtime
		 *   The current [AvailRuntime].
		 * @param module
		 *   The module that will be unloaded.
		 * @param textInterface
		 *   The [TextInterface] for any [fiber][A_Fiber] started by the new
		 *   builder. @return An AvailLoader suitable for unloading the module.
		 */
		fun forUnloading(
			runtime: AvailRuntime,
			module: A_Module,
			textInterface: TextInterface
		): AvailLoader
		{
			val loader = AvailLoader(runtime, module, textInterface)
			// We had better not be removing forward declarations from an
			// already fully-loaded module.
			loader.pendingForwards = nil
			loader.phase = UNLOADING
			return loader
		}

		/**
		 * Define a special root bundle tree that's *only* for parsing method
		 * headers.
		 */
		private val moduleHeaderBundleRoot = newBundleTree(nil).apply {
			// Add the method that allows the header to be parsed.
			val headerMethodBundle = try
			{
				SpecialMethodAtom.MODULE_HEADER.atom.bundleOrCreate()
			}
			catch (e: MalformedMessageException)
			{
				assert(false) { "Malformed module header method name" }
				throw RuntimeException(e)
			}
			val headerPlan: A_DefinitionParsingPlan =
				headerMethodBundle.definitionParsingPlans.mapIterable
					.next()
					.value()
			addPlanInProgress(newPlanInProgress(headerPlan, 1))
		}

		/**
		 * The [LexicalScanner] used only for parsing module headers.
		 */
		private val moduleHeaderLexicalScanner = LexicalScanner().apply {
			// Add the string literal lexer.
			createPrimitiveLexerForHeaderParsing(
				P_BootstrapLexerStringFilter,
				P_BootstrapLexerStringBody,
				"string token lexer")

			// The module header uses keywords, e.g. "Extends".
			createPrimitiveLexerForHeaderParsing(
				P_BootstrapLexerKeywordFilter,
				P_BootstrapLexerKeywordBody,
				"keyword token lexer")

			// There's also punctuation in there, like commas.
			createPrimitiveLexerForHeaderParsing(
				P_BootstrapLexerOperatorFilter,
				P_BootstrapLexerOperatorBody,
				"operator token lexer")

			// It would be tricky with no whitespace!
			createPrimitiveLexerForHeaderParsing(
				P_BootstrapLexerWhitespaceFilter,
				P_BootstrapLexerWhitespaceBody,
				"whitespace lexer")

			// Slash-star-star-slash comments are legal in the header.
			createPrimitiveLexerForHeaderParsing(
				P_BootstrapLexerSlashStarCommentFilter,
				P_BootstrapLexerSlashStarCommentBody,
				"comment lexer")

			freezeFromChanges()
		}

		/**
		 * Create an [A_Lexer] from the given filter and body primitives, and
		 * install it in the specified atom's bundle.  Add the lexer to the root
		 * [A_BundleTree] of the receiver, a [LexicalScanner] used for parsing
		 * module headers.
		 *
		 * @param filterPrimitive
		 *   A primitive for filtering the lexer by its first character.
		 * @param bodyPrimitive
		 *   A primitive for constructing a tuple of tokens at the current
		 *   position.  Typically the tuple has zero or one tokens, but more can
		 *   be produced to indicate ambiguity within the lexer.
		 * @param atomName
		 *   The [A_Atom] under which to record the new lexer.
		 */
		private fun LexicalScanner.createPrimitiveLexerForHeaderParsing(
			filterPrimitive: Primitive,
			bodyPrimitive: Primitive,
			atomName: String)
		{
			val stringLexerFilter = createFunction(
				newPrimitiveRawFunction(filterPrimitive, nil, 0),
				emptyTuple)
			val stringLexerBody = createFunction(
				newPrimitiveRawFunction(bodyPrimitive, nil, 0),
				emptyTuple)
			val bundle: A_Bundle = try
			{
				createSpecialAtom(atomName).bundleOrCreate()
			}
			catch (e: MalformedMessageException)
			{
				assert(false) { "Invalid special lexer name: $atomName" }
				throw RuntimeException(e)
			}
			val lexer = newLexer(
				stringLexerFilter, stringLexerBody, bundle.bundleMethod, nil)
			addLexer(lexer)
		}
	}
}
