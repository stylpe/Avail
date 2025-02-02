/*
 * ListPhraseDescriptor.kt
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
import avail.descriptor.phrases.A_Phrase.Companion.emitValueOn
import avail.descriptor.phrases.A_Phrase.Companion.expressionsTuple
import avail.descriptor.phrases.A_Phrase.Companion.hasSuperCast
import avail.descriptor.phrases.A_Phrase.Companion.isMacroSubstitutionNode
import avail.descriptor.phrases.A_Phrase.Companion.phraseExpressionType
import avail.descriptor.phrases.A_Phrase.Companion.phraseKind
import avail.descriptor.phrases.A_Phrase.Companion.stripMacro
import avail.descriptor.phrases.A_Phrase.Companion.superUnionType
import avail.descriptor.phrases.A_Phrase.Companion.tokens
import avail.descriptor.phrases.ListPhraseDescriptor.ObjectSlots.EXPRESSIONS_TUPLE
import avail.descriptor.phrases.ListPhraseDescriptor.ObjectSlots.TUPLE_TYPE
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.A_BasicObject.Companion.synchronizeIf
import avail.descriptor.representation.AbstractSlotsEnum
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObject.Companion.combine2
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.tokens.A_Token
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.appendCanDestroy
import avail.descriptor.tuples.A_Tuple.Companion.concatenateWith
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.TupleDescriptor
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.isSubtypeOf
import avail.descriptor.types.A_Type.Companion.subexpressionsTupleType
import avail.descriptor.types.BottomTypeDescriptor.Companion.bottom
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind
import avail.descriptor.types.TupleTypeDescriptor.Companion.tupleTypeForTypes
import avail.descriptor.types.TupleTypeDescriptor.Companion.tupleTypeForTypesList
import avail.descriptor.types.TypeTag
import avail.serialization.SerializerOperation
import avail.utility.Strings.newlineTab
import org.availlang.json.JSONWriter
import java.util.IdentityHashMap

/**
 * My instances represent [phrases][PhraseDescriptor] which will generate tuples
 * directly at runtime.
 *
 * @constructor
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
class ListPhraseDescriptor private constructor(
	mutability: Mutability
) : PhraseDescriptor(
	mutability,
	TypeTag.LIST_PHRASE_TAG,
	ObjectSlots::class.java,
	null
) {
	/**
	 * My slots of type [AvailObject].
	 */
	enum class ObjectSlots : ObjectSlotsEnum {
		/**
		 * The [tuple][TupleDescriptor] of [phrases][PhraseDescriptor] that
		 * produce the values that will be aggregated into a tuple at runtime.
		 */
		EXPRESSIONS_TUPLE,

		/**
		 * The static type of the tuple that will be generated.
		 */
		TUPLE_TYPE
	}

	override fun allowsImmutableToMutableReferenceInField(
		e: AbstractSlotsEnum
	) = e === TUPLE_TYPE

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	) {
		builder.append("List(")
		val strings = self.expressionsTuple.map {
			val tempBuilder = StringBuilder()
			it.printOnAvoidingIndent(tempBuilder, recursionMap, indent + 1)
			tempBuilder.toString()
		}
		if (strings.any { it.contains('\n') }
			|| strings.sumOf { it.length } > 60)
		{
			// Print on multiple lines.
			val indenter = buildString { newlineTab(indent) }
			strings.joinTo(builder, ",$indenter", indenter)
		}
		strings.joinTo(builder, ", ")
		builder.append(")")
	}

	override fun o_ChildrenDo(
		self: AvailObject,
		action: (A_Phrase) -> Unit
	) = self.expressionsTuple.forEach(action)

	override fun o_ChildrenMap(
		self: AvailObject,
		transformer: (A_Phrase) -> A_Phrase
	) {
		self.setSlot(
			EXPRESSIONS_TUPLE,
			tupleFromList(self.expressionsTuple.map(transformer)))
	}

	/**
	 * Create a new [list&#32;phrase]][ListPhraseDescriptor] with one more
	 * phrase added to the end of the list.
	 *
	 * @param self
	 *   The list phrase to extend.
	 * @param newPhrase
	 *   The phrase to append.
	 * @return
	 *   A new list phrase with the phrase appended.
	 */
	override fun o_CopyWith(
		self: AvailObject,
		newPhrase: A_Phrase
	): A_Phrase = newListNode(
		self.slot(EXPRESSIONS_TUPLE).appendCanDestroy(newPhrase, true))

	/**
	 * Create a new [list&#32;phrase][ListPhraseDescriptor] with phrases from a
	 * given list phrase appended to the end of the list.
	 *
	 * @param self
	 *   The list phrase to extend.
	 * @param newListPhrase
	 *   The list phrase containing subphrases to append.
	 * @return
	 *   A new list phrase with the given list phrase's subphrases appended.
	 */
	override fun o_CopyConcatenating(
		self: AvailObject,
		newListPhrase: A_Phrase
	): A_Phrase = newListNode(
		self.slot(EXPRESSIONS_TUPLE).concatenateWith(
			newListPhrase.expressionsTuple, false))

	override fun o_EmitAllValuesOn(
		self: AvailObject,
		codeGenerator: AvailCodeGenerator
	) = self.expressionsTuple.forEach {
		it.emitValueOn(codeGenerator)
	}

	override fun o_EmitValueOn(
		self: AvailObject,
		codeGenerator: AvailCodeGenerator
	) {
		val childNodes = self.expressionsTuple
		childNodes.forEach {
			it.emitValueOn(codeGenerator)
		}
		codeGenerator.emitMakeTuple(emptyTuple, childNodes.tupleSize)
	}

	override fun o_EqualsPhrase(
		self: AvailObject,
		aPhrase: A_Phrase
	): Boolean = (!aPhrase.isMacroSubstitutionNode
		&& self.phraseKind == aPhrase.phraseKind
		&& self.expressionsTuple.equals(aPhrase.expressionsTuple))

	override fun o_ExpressionAt(self: AvailObject, index: Int): A_Phrase =
		self.slot(EXPRESSIONS_TUPLE).tupleAt(index)

	override fun o_ExpressionsSize(self: AvailObject): Int =
		self.slot(EXPRESSIONS_TUPLE).tupleSize

	override fun o_ExpressionsTuple(self: AvailObject): A_Tuple =
		self.slot(EXPRESSIONS_TUPLE)

	override fun o_PhraseExpressionType(self: AvailObject): A_Type =
		self.synchronizeIf(isShared) { expressionType(self) }

	override fun o_Hash(self: AvailObject): Int =
		combine2(self.expressionsTuple.hash(), -0x3ebc1689)

	override fun o_HasSuperCast(self: AvailObject): Boolean =
		self.slot(EXPRESSIONS_TUPLE).any { it.hasSuperCast }

	override fun o_IsInstanceOfKind(
		self: AvailObject,
		aType: A_Type
	) = when {
		!super.o_IsInstanceOfKind(self, aType) -> false
		!aType.isSubtypeOf(PhraseKind.LIST_PHRASE.mostGeneralType) -> true
		else -> self.slot(EXPRESSIONS_TUPLE).isInstanceOf(
			aType.subexpressionsTupleType)
	}

	override fun o_LastExpression(self: AvailObject): A_Phrase
	{
		val tuple: A_Tuple = self.slot(EXPRESSIONS_TUPLE)
		return tuple.tupleAt(tuple.tupleSize)
	}

	override fun o_StatementsDo(
		self: AvailObject,
		continuation: (A_Phrase) -> Unit
	): Unit = unsupported

	override fun o_PhraseKind(self: AvailObject): PhraseKind =
		PhraseKind.LIST_PHRASE

	override fun o_StripMacro(self: AvailObject): A_Phrase
	{
		// Strip away macro substitution phrases inside my recursive list
		// structure.  This has to be done recursively over list phrases because
		// of the way the "leaf" phrases are checked for grammatical
		// restrictions, but the "root" phrases are what get passed into
		// functions.
		val expressionsTuple: A_Tuple =
			self.slot(EXPRESSIONS_TUPLE).makeImmutable()
		var anyStripped = false
		val newExpressions = expressionsTuple.map {
			val strippedElement = it.stripMacro
			if (!it.equals(strippedElement)) {
				anyStripped = true
			}
			strippedElement
		}
		return when {
			anyStripped -> newListNode(tupleFromList(newExpressions))
			else -> self
		}
	}

	override fun o_SuperUnionType(self: AvailObject): A_Type
	{
		val expressions: A_Tuple = self.slot(EXPRESSIONS_TUPLE)
		var anyNotBottom = false
		val types = Array(expressions.tupleSize) { i ->
			val lookupType = expressions.tupleAt(i + 1).superUnionType
			if (!lookupType.isBottom) anyNotBottom = true
			lookupType
		}
		return when {
			anyNotBottom -> tupleTypeForTypes(*types)
			// The elements' superunion types were all bottom, so answer bottom.
			else -> bottom
		}
	}

	override fun o_ValidateLocally(self: AvailObject, parent: A_Phrase?
	) {
		// Do nothing.
	}

	override fun o_SerializerOperation(self: AvailObject): SerializerOperation =
		SerializerOperation.LIST_PHRASE

	override fun o_Tokens(self: AvailObject): A_Tuple
	{
		val tokens = mutableListOf<A_Token>()
		self.slot(EXPRESSIONS_TUPLE).forEach { tokens.addAll(it.tokens) }
		return tupleFromList(tokens)
	}

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("list phrase") }
			at("expressions") { self.slot(EXPRESSIONS_TUPLE).writeTo(writer) }
		}

	override fun o_WriteSummaryTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("list phrase") }
			at("expressions") {
				self.slot(EXPRESSIONS_TUPLE).writeSummaryTo(writer)
			}
		}

	override fun mutable() = mutable

	override fun shared() = shared

	companion object {
		/**
		 * Lazily compute and install the expression type of the specified list
		 * phrase.
		 *
		 * @param self
		 *   A list phrase.
		 * @return
		 *   A type.
		 */
		private fun expressionType(self: AvailObject): A_Type {
			var tupleType: A_Type = self.mutableSlot(TUPLE_TYPE)
			if (tupleType.notNil) return tupleType
			val types = self.expressionsTuple.map {
				val expressionType = it.phraseExpressionType
				if (expressionType.isBottom) return bottom
				expressionType
			}
			tupleType = tupleTypeForTypesList(types).makeShared()
			self.setMutableSlot(TUPLE_TYPE, tupleType)
			return tupleType
		}

		/**
		 * Create a new list phrase from the given [tuple][TupleDescriptor] of
		 * [expressions][PhraseDescriptor].
		 *
		 * @param expressions
		 *   The expressions to assemble into a list phrase.
		 * @return
		 *   The resulting list phrase.
		 */
		fun newListNode(expressions: A_Tuple): AvailObject =
			mutable.createShared {
				setSlot(EXPRESSIONS_TUPLE, expressions)
				setSlot(TUPLE_TYPE, nil)
			}

		/** The mutable [ListPhraseDescriptor]. */
		private val mutable = ListPhraseDescriptor(Mutability.MUTABLE)

		/** The shared [ListPhraseDescriptor]. */
		private val shared = ListPhraseDescriptor(Mutability.SHARED)

		/** The empty [list phrase][ListPhraseDescriptor]. */
		private val empty = newListNode(emptyTuple).makeShared()

		/**
		 * Answer the empty list phrase.
		 *
		 * @return
		 *   The empty list phrase.
		 */
		fun emptyListNode(): AvailObject = empty
	}
}
