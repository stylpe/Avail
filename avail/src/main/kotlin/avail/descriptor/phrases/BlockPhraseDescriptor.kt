/*
 * BlockPhraseDescriptor.kt
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
package avail.descriptor.phrases

import avail.compiler.AvailCodeGenerator
import avail.compiler.AvailCodeGenerator.Companion.generateFunction
import avail.descriptor.functions.A_RawFunction
import avail.descriptor.functions.FunctionDescriptor.Companion.createFunction
import avail.descriptor.module.A_Module
import avail.descriptor.module.ModuleDescriptor
import avail.descriptor.phrases.A_Phrase.Companion.argumentsTuple
import avail.descriptor.phrases.A_Phrase.Companion.childrenDo
import avail.descriptor.phrases.A_Phrase.Companion.declaration
import avail.descriptor.phrases.A_Phrase.Companion.declaredExceptions
import avail.descriptor.phrases.A_Phrase.Companion.declaredType
import avail.descriptor.phrases.A_Phrase.Companion.flattenStatementsInto
import avail.descriptor.phrases.A_Phrase.Companion.generateInModule
import avail.descriptor.phrases.A_Phrase.Companion.isMacroSubstitutionNode
import avail.descriptor.phrases.A_Phrase.Companion.neededVariables
import avail.descriptor.phrases.A_Phrase.Companion.phraseExpressionType
import avail.descriptor.phrases.A_Phrase.Companion.phraseKind
import avail.descriptor.phrases.A_Phrase.Companion.phraseKindIsUnder
import avail.descriptor.phrases.A_Phrase.Companion.primitive
import avail.descriptor.phrases.A_Phrase.Companion.statementsTuple
import avail.descriptor.phrases.A_Phrase.Companion.tokens
import avail.descriptor.phrases.A_Phrase.Companion.validateLocally
import avail.descriptor.phrases.BlockPhraseDescriptor.IntegerSlots.Companion.STARTING_LINE_NUMBER
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.ARGUMENTS_TUPLE
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.DECLARED_EXCEPTIONS
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.NEEDED_VARIABLES
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.PRIMITIVE_POJO
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.RESULT_TYPE
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.STATEMENTS_TUPLE
import avail.descriptor.phrases.BlockPhraseDescriptor.ObjectSlots.TOKENS
import avail.descriptor.phrases.DeclarationPhraseDescriptor.DeclarationKind
import avail.descriptor.phrases.DeclarationPhraseDescriptor.DeclarationKind.LOCAL_CONSTANT
import avail.descriptor.phrases.DeclarationPhraseDescriptor.DeclarationKind.LOCAL_VARIABLE
import avail.descriptor.phrases.DeclarationPhraseDescriptor.DeclarationKind.PRIMITIVE_FAILURE_REASON
import avail.descriptor.pojos.RawPojoDescriptor
import avail.descriptor.pojos.RawPojoDescriptor.Companion.identityPojo
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AbstractSlotsEnum
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObject.Companion.combine5
import avail.descriptor.representation.BitField
import avail.descriptor.representation.IntegerSlotsEnum
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.sets.A_Set
import avail.descriptor.sets.A_Set.Companion.setSize
import avail.descriptor.tokens.A_Token
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.generateObjectTupleFrom
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.TupleDescriptor
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.BLOCK_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.DECLARATION_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.LABEL_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.LITERAL_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.VARIABLE_USE_PHRASE
import avail.descriptor.types.TypeTag
import avail.exceptions.AvailErrorCode.E_BLOCK_MUST_NOT_CONTAIN_OUTERS
import avail.exceptions.AvailRuntimeException
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag
import avail.serialization.SerializerOperation
import avail.utility.Strings.newlineTab
import avail.utility.evaluation.Combinator.recurse
import org.availlang.json.JSONWriter
import java.util.IdentityHashMap

/**
 * My instances represent occurrences of blocks (functions) encountered in code.
 *
 * @constructor
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
class BlockPhraseDescriptor
private constructor(mutability: Mutability) : PhraseDescriptor(
	mutability,
	TypeTag.BLOCK_PHRASE_TAG,
	ObjectSlots::class.java,
	IntegerSlots::class.java
) {
	/**
	 * My slots of type [int][Integer].
	 */
	enum class IntegerSlots : IntegerSlotsEnum {
		/**
		 * A slot containing multiple [BitField]s, potentially.
		 */
		STARTING_LINE_NUMBER_AND_MORE;

		companion object {
			/**
			 * The line number on which this block starts.
			 */
			val STARTING_LINE_NUMBER = BitField(
				STARTING_LINE_NUMBER_AND_MORE, 0, 32, Int::toString)
		}
	}

	/**
	 * My slots of type [AvailObject].
	 */
	enum class ObjectSlots : ObjectSlotsEnum {
		/**
		 * The block's tuple of argument declarations.
		 */
		ARGUMENTS_TUPLE,

		/**
		 * The tuple of statements contained in this block.
		 */
		STATEMENTS_TUPLE,

		/**
		 * The type this block is expected to return an instance of.
		 */
		RESULT_TYPE,

		/**
		 * A tuple of variables needed by this block.  This is set after the
		 * [block][BlockPhraseDescriptor] phrase has already been created.
		 */
		NEEDED_VARIABLES,

		/**
		 * The block's set of exception types that may be raised.  This set *has
		 * not yet been normalized* (e.g., removing types that are subtypes of
		 * types that are also present in the set).
		 */
		DECLARED_EXCEPTIONS,

		/**
		 * The tuple of tokens forming this block phrase, if any.
		 */
		TOKENS,

		/**
		 * Either [nil] or a raw [pojo][RawPojoDescriptor] holding the
		 * [Primitive] to invoke for this block.
		 */
		PRIMITIVE_POJO
	}

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	) : Unit = with(builder)
	{
		// Optimize for one-liners...
		val argumentsTuple = self.argumentsTuple
		val argCount = argumentsTuple.tupleSize
		val primitive = self.primitive
		val statementsTuple = self.statementsTuple
		val statementsSize = statementsTuple.tupleSize
		var explicitResultType: A_Type? = self.resultType()
		if (statementsSize >= 1
			&& statementsTuple.tupleAt(statementsSize).phraseExpressionType
				.equals(explicitResultType!!))
		{
			explicitResultType = null
		}
		val declaredExceptions: A_Set? = self.declaredExceptions.let {
			if (it.setSize == 0) null else it
		}
		val endsWithStatement = (statementsSize < 1
			|| statementsTuple.tupleAt(statementsSize)
				.phraseExpressionType.isTop)
		if (argCount == 0
			&& primitive === null
			&& statementsSize == 1
			&& explicitResultType === null
			&& declaredExceptions === null)
		{
			// See if the lone statement fits on a line.
			val tempBuilder = StringBuilder()
			statementsTuple.tupleAt(1).printOnAvoidingIndent(
				tempBuilder,
				recursionMap,
				indent)
			if (!tempBuilder.contains('\n') && tempBuilder.length < 100)
			{
				append('[')
				append(tempBuilder)
				if (endsWithStatement)
				{
					append(';')
				}
				append(']')
				return
			}
		}

		// Use multiple lines instead...
		append('[')
		var wroteAnything = false
		if (argCount > 0)
		{
			wroteAnything = true
			for (argIndex in 1 .. argCount)
			{
				if (argIndex > 1)
				{
					append(',')
				}
				newlineTab(indent)
				argumentsTuple.tupleAt(argIndex).printOnAvoidingIndent(
					builder, recursionMap, indent)
			}
			newlineTab(indent - 1)
			append('|')
		}
		var skipFailureDeclaration = false
		if (primitive !== null && !primitive.hasFlag(Flag.SpecialForm))
		{
			wroteAnything = true
			newlineTab(indent)
			append("Primitive ")
			append(primitive.name)
			if (!primitive.hasFlag(Flag.CannotFail))
			{
				append(" (")
				statementsTuple.tupleAt(1).printOnAvoidingIndent(
					builder, recursionMap, indent)
				append(')')
				skipFailureDeclaration = true
			}
			append(';')
		}
		for (index in 1 .. statementsSize)
		{
			val statement: A_Phrase = statementsTuple.tupleAt(index)
			if (skipFailureDeclaration)
			{
				assert(
					statement.isInstanceOf(
						DECLARATION_PHRASE.mostGeneralType))
				skipFailureDeclaration = false
			}
			else
			{
				wroteAnything = true
				newlineTab(indent)
				statement.printOnAvoidingIndent(
					builder, recursionMap, indent)
				if (index < statementsSize || endsWithStatement)
				{
					append(';')
				}
			}
		}
		if (wroteAnything)
		{
			newlineTab(indent - 1)
		}
		append(']')
		if (explicitResultType !== null)
		{
			append(" : ")
			append(explicitResultType)
		}
		if (declaredExceptions !== null)
		{
			append(" ^ ")
			append(declaredExceptions)
		}
	}

	override fun allowsImmutableToMutableReferenceInField(
		e: AbstractSlotsEnum
	) = e === NEEDED_VARIABLES

	override fun o_ArgumentsTuple(self: AvailObject): A_Tuple =
		self.slot(ARGUMENTS_TUPLE)

	override fun o_ChildrenDo(
		self: AvailObject,
		action: (A_Phrase) -> Unit
	) {
		self.argumentsTuple.forEach(action)
		self.statementsTuple.forEach(action)
	}

	override fun o_ChildrenMap(
		self: AvailObject,
		transformer: (A_Phrase) -> A_Phrase
	) {
		var arguments: A_Tuple = self.slot(ARGUMENTS_TUPLE)
		arguments = generateObjectTupleFrom(arguments.tupleSize) {
			transformer(arguments.tupleAt(it))
		}
		self.setSlot(ARGUMENTS_TUPLE, arguments)
		var statements = self.slot(STATEMENTS_TUPLE)
		statements = generateObjectTupleFrom(statements.tupleSize) {
			transformer(statements.tupleAt(it))
		}
		self.setSlot(STATEMENTS_TUPLE, statements)
	}

	override fun o_DeclaredExceptions(self: AvailObject): A_Set =
		self.slot(DECLARED_EXCEPTIONS)

	/**
	 * The expression `[`someExpression`]` has no effect, only a value (the
	 * function itself).
	 */
	override fun o_EmitEffectOn(
		self: AvailObject,
		codeGenerator: AvailCodeGenerator
	) {
		// No effect.
	}

	override fun o_EmitValueOn(
		self: AvailObject,
		codeGenerator: AvailCodeGenerator
	) {
		val compiledBlock = self.generateInModule(codeGenerator.module)
		val neededVariables = self.neededVariables
		when (neededVariables.tupleSize)
		{
			0 -> codeGenerator.emitPushLiteral(
				self.tokens,
				createFunction(compiledBlock, emptyTuple).makeImmutable())
			else -> codeGenerator.emitCloseCode(
				self.tokens, compiledBlock, neededVariables)
		}
	}

	override fun o_EqualsPhrase(
		self: AvailObject,
		aPhrase: A_Phrase
	): Boolean {
		return (!aPhrase.isMacroSubstitutionNode
			&& self.phraseKind == aPhrase.phraseKind
			&& self.argumentsTuple.equals(aPhrase.argumentsTuple)
			&& self.statementsTuple.equals(aPhrase.statementsTuple)
			&& self.resultType().equals(aPhrase.resultType())
			&& self.primitive === aPhrase.primitive)
	}

	override fun o_PhraseExpressionType(self: AvailObject): A_Type =
		functionType(
			tupleFromList(self.argumentsTuple.map { it.declaredType }),
			self.resultType())

	/**
	 * Answer an Avail compiled block compiled from the given block phrase,
	 * using the given [AvailCodeGenerator].
	 *
	 * @param self
	 *   The block phrase.
	 * @param module
	 *   The [module][ModuleDescriptor] which is intended to hold the resulting
	 *   code.
	 * @return
	 *   An [A_RawFunction].
	 */
	override fun o_GenerateInModule(
		self: AvailObject,
		module: A_Module
	): A_RawFunction = generateFunction(module, self)

	override fun o_Hash(self: AvailObject): Int = combine5(
		self.argumentsTuple.hash(),
		self.statementsTuple.hash(),
		self.resultType().hash(),
		self.primitive?.name?.hashCode() ?: 0,
		0x05E6A04A)

	override fun o_NeededVariables(self: AvailObject): A_Tuple =
		self.mutableSlot(NEEDED_VARIABLES)

	override fun o_NeededVariables(
		self: AvailObject,
		neededVariables: A_Tuple
	) = self.setMutableSlot(NEEDED_VARIABLES, neededVariables)

	override fun o_PhraseKind(self: AvailObject): PhraseKind =
		BLOCK_PHRASE

	override fun o_Primitive(self: AvailObject): Primitive? =
		self.slot(PRIMITIVE_POJO).run {
			if (isNil) null else javaObject<Primitive>()
		}

	override fun o_ResultType(self: AvailObject): A_Type =
		self.slot(RESULT_TYPE)

	override fun o_SerializerOperation(self: AvailObject): SerializerOperation =
		SerializerOperation.BLOCK_PHRASE

	override fun o_StartingLineNumber(self: AvailObject): Int =
		self.slot(STARTING_LINE_NUMBER)

	override fun o_StatementsTuple(self: AvailObject): A_Tuple =
		self.slot(STATEMENTS_TUPLE)

	override fun o_StatementsDo(
		self: AvailObject,
		continuation: (A_Phrase) -> Unit
	) = unsupported

	override fun o_Tokens(self: AvailObject): A_Tuple = self.slot(TOKENS)

	override fun o_ValidateLocally(
		self: AvailObject,
		parent: A_Phrase?
	) {
		// Make sure our neededVariables list has up-to-date information about
		// the outer variables that are accessed in me, because they have to be
		// captured when a function is made for me.
		collectNeededVariablesOfOuterBlocks(self)
	}

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("block phrase") }
			at("primitive") { write(self.primitive?.name ?: "") }
			at("starting line") { write(self.slot(STARTING_LINE_NUMBER)) }
			at("arguments") { self.slot(ARGUMENTS_TUPLE).writeTo(writer) }
			at("statements") { self.slot(STATEMENTS_TUPLE).writeTo(writer) }
			at("result type") { self.slot(RESULT_TYPE).writeTo(writer) }
			at("needed variables") {
				self.slot(NEEDED_VARIABLES).writeTo(writer)
			}
			at("declared exceptions") {
				self.slot(DECLARED_EXCEPTIONS).writeTo(writer)
			}
			at("tokens") { self.slot(TOKENS).writeTo(writer) }
		}

	override fun o_WriteSummaryTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("block phrase") }
			at("primitive") { write(self.primitive?.name ?: "") }
			at("starting line") { write(self.slot(STARTING_LINE_NUMBER)) }
			at("arguments") {
				self.slot(ARGUMENTS_TUPLE).writeSummaryTo(writer)
			}
			at("statements") {
				self.slot(STATEMENTS_TUPLE).writeSummaryTo(writer)
			}
			at("result type") { self.slot(RESULT_TYPE).writeSummaryTo(writer) }
			at("needed variables") {
				self.slot(NEEDED_VARIABLES).writeSummaryTo(writer)
			}
			at("declared exceptions") {
				self.slot(DECLARED_EXCEPTIONS).writeSummaryTo(writer)
			}
		}

	override fun mutable() = mutable

	override fun shared() = shared

	companion object
	{
		/**
		 * Return a [List] of all [declaration][DeclarationPhraseDescriptor]
		 * phrases defined by this block. This includes arguments, locals,
		 * constants, and labels.
		 *
		 * Note that we also have to look inside
		 * [sequence-as-expression][SequenceAsExpressionPhraseDescriptor]
		 * phrases, since they're allowed to contain declarations as well.
		 *
		 * @param self
		 *   The Avail block phrase to scan.
		 * @return
		 *   The list of declarations.
		 */
		private fun allLocalDeclarations(
			self: A_Phrase
		): List<A_Phrase>
		{
			val declarations = mutableListOf<A_Phrase>()
			recurse(self) { parent: A_Phrase, again: (A_Phrase)->Unit ->
				parent.childrenDo { child: A_Phrase ->
					when
					{
						// Don't recurse into blocks.
						child.phraseKindIsUnder(BLOCK_PHRASE) -> { }
						// Don't look in a variable use phrases, because the
						// declaration it refers to might be in an outer scope.
						child.phraseKindIsUnder(VARIABLE_USE_PHRASE) -> { }
						child.phraseKindIsUnder(DECLARATION_PHRASE) ->
						{
							declarations.add(child)
							// The initialization expression of a declaration is
							// allowed to be a sequence-as-expression that has
							// additional declarations.
							again(child)
						}
						else -> again(child)
					}
				}
			}
			return declarations
		}

		/**
		 * Answer the labels present in this block's list of statements. There
		 * is either zero or one label, and it must be the first statement.
		 *
		 * @param self
		 *   The block phrase to examine.
		 * @return
		 *   A list of between zero and one labels.
		 */
		fun labels(self: A_Phrase): List<A_Phrase> =
			allLocalDeclarations(self).filter {
				it.isInstanceOfKind(LABEL_PHRASE.mostGeneralType) }

		/**
		 * Answer the declarations of this block's local variables.  Do not
		 * include the label declaration if present, nor argument declarations,
		 * nor local constants.
		 *
		 * Include the primitive failure reason variable, if present.
		 *
		 * @param self
		 *   The block phrase to examine.
		 * @return
		 *   This block's local variable declarations.
		 */
		fun locals(self: A_Phrase): List<A_Phrase> =
			allLocalDeclarations(self).filter {
				it.declarationKind() === PRIMITIVE_FAILURE_REASON
					|| it.declarationKind() === LOCAL_VARIABLE }

		/**
		 * Answer the declarations of this block's local constants.  Do not
		 * include the label declaration if present, nor argument declarations,
		 * nor local variables.
		 *
		 * @param self
		 *   The block phrase to examine.
		 * @return
		 *   This block's local constant declarations.
		 */
		fun constants(self: A_Phrase): List<A_Phrase> =
			allLocalDeclarations(self).filter {
				it.declarationKind() === LOCAL_CONSTANT }

		/**
		 * Construct a block phrase.
		 *
		 * @param arguments
		 *   The [tuple][TupleDescriptor] of argument
		 *   [declarations][DeclarationPhraseDescriptor].
		 * @param primitive
		 *   The [Primitive] that the resulting block will invoke, or `null` if
		 *   this is not a primitive.
		 * @param statements
		 *   The [tuple][A_Tuple] of statement [phrases][PhraseDescriptor].
		 * @param resultType
		 *   The [type][A_Type] that will be returned by the block.
		 * @param declaredExceptions
		 *   The [set][A_Set] of exception types that may be raised by this
		 *   block.  *This is not yet normalized.*
		 * @param lineNumber
		 *   The line number in the current module at which this block begins.
		 * @param tokens
		 *   The [A_Tuple] of [A_Token]s contributing to this block phrase.
		 * @return
		 *   A block phrase.
		 */
		fun newBlockNode(
			arguments: A_Tuple,
			primitive: Primitive?,
			statements: A_Tuple,
			resultType: A_Type,
			declaredExceptions: A_Set,
			lineNumber: Int,
			tokens: A_Tuple
		): AvailObject
		{
			val flattenedStatements = mutableListOf<A_Phrase>()
			statements.forEach {
				it.flattenStatementsInto(flattenedStatements)
			}
			// Remove useless statements that are just literals, other than the
			// final statement.
			for (index in flattenedStatements.size - 2 downTo 0)
			{
				val statement = flattenedStatements[index]
				if (statement.isInstanceOfKind(
						LITERAL_PHRASE.mostGeneralType))
				{
					flattenedStatements.removeAt(index)
				}
			}
			return mutable.createShared {
				setSlot(ARGUMENTS_TUPLE, arguments)
				setSlot(STATEMENTS_TUPLE, tupleFromList(flattenedStatements))
				setSlot(RESULT_TYPE, resultType)
				setSlot(NEEDED_VARIABLES, nil)
				setSlot(DECLARED_EXCEPTIONS, declaredExceptions)
				setSlot(TOKENS, tokens)
				setSlot(STARTING_LINE_NUMBER, lineNumber)
				setSlot(PRIMITIVE_POJO, primitive?.let(::identityPojo) ?: nil)
			}
		}

		/**
		 * Ensure that the block phrase is valid.  Throw an appropriate
		 * exception if it is not.
		 *
		 * @param blockNode
		 *   The block phrase to validate.
		 */
		fun recursivelyValidate(blockNode: A_Phrase)
		{
			treeDoWithParent(
				blockNode,
				{ obj: A_Phrase, parent: A_Phrase? ->
					obj.validateLocally(parent)
				},
				null)
			if (blockNode.neededVariables.tupleSize != 0)
			{
				throw AvailRuntimeException(E_BLOCK_MUST_NOT_CONTAIN_OUTERS)
			}
		}

		/**
		 * Figure out what outer variables will need to be captured when a
		 * function for me is built.
		 *
		 * @param self
		 *   The block phrase to analyze.
		 */
		private fun collectNeededVariablesOfOuterBlocks(self: A_Phrase)
		{
			val providedByMe = allLocalDeclarations(self).toSet()
			val neededDeclarationsSet = mutableSetOf<A_Phrase>()
			val neededDeclarations = mutableListOf<A_Phrase>()
			recurse(self) { parent: A_Phrase, again: (A_Phrase)->Unit ->
				parent.childrenDo { child: A_Phrase ->
					when
					{
						child.phraseKindIsUnder(BLOCK_PHRASE) ->
							for (declaration in child.neededVariables)
							{
								if (!providedByMe.contains(declaration)
									&& !neededDeclarationsSet.contains(
										declaration))
								{
									neededDeclarationsSet.add(declaration)
									neededDeclarations.add(declaration)
								}
							}
						child.phraseKindIsUnder(VARIABLE_USE_PHRASE) ->
						{
							val declaration = child.declaration
							if (!providedByMe.contains(declaration)
								&& declaration.declarationKind()
									!== DeclarationKind.MODULE_VARIABLE
								&& declaration.declarationKind()
									!== DeclarationKind.MODULE_CONSTANT
								&& !neededDeclarationsSet.contains(declaration))
							{
								neededDeclarationsSet.add(declaration)
								neededDeclarations.add(declaration)
							}
							// Avoid visiting the declaration explicitly,
							// otherwise uses of declarations that have
							// initializations will cause variables used in
							// those initializations to accidentally be captured
							// as well.
						}
						else -> again(child)
					}
				}
			}
			self.neededVariables = tupleFromList(neededDeclarations)
		}

		/** The mutable [BlockPhraseDescriptor]. */
		private val mutable = BlockPhraseDescriptor(Mutability.MUTABLE)

		/** The shared [BlockPhraseDescriptor]. */
		private val shared = BlockPhraseDescriptor(Mutability.SHARED)
	}
}
