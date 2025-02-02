/*
 * SuperCastPhraseDescriptor.kt
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
import avail.descriptor.phrases.A_Phrase.Companion.expression
import avail.descriptor.phrases.A_Phrase.Companion.isMacroSubstitutionNode
import avail.descriptor.phrases.A_Phrase.Companion.phraseKind
import avail.descriptor.phrases.A_Phrase.Companion.sequence
import avail.descriptor.phrases.A_Phrase.Companion.superUnionType
import avail.descriptor.phrases.A_Phrase.Companion.tokens
import avail.descriptor.phrases.SuperCastPhraseDescriptor.ObjectSlots.EXPRESSION
import avail.descriptor.phrases.SuperCastPhraseDescriptor.ObjectSlots.TYPE_FOR_LOOKUP
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObject.Companion.combine3
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.typeUnion
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind
import avail.descriptor.types.TypeTag
import avail.serialization.SerializerOperation
import org.availlang.json.JSONWriter
import java.util.IdentityHashMap

/**
 * My instances represent [phrases][PhraseDescriptor] which are elements of
 * recursive [list][ListPhraseDescriptor] phrases holding arguments to a (super)
 * [send][SendPhraseDescriptor] phrase.
 *
 * @constructor
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
class SuperCastPhraseDescriptor private constructor(
	mutability: Mutability
) : PhraseDescriptor(
	mutability,
	TypeTag.SUPER_CAST_PHRASE_TAG,
	ObjectSlots::class.java,
	null
) {
	/**
	 * My slots of type [AvailObject].
	 */
	enum class ObjectSlots : ObjectSlotsEnum {
		/**
		 * The expression producing the actual value.
		 */
		EXPRESSION,

		/**
		 * The static type used to look up this argument in the enclosing
		 * (super) [send][SendPhraseDescriptor] phrase.
		 */
		TYPE_FOR_LOOKUP
	}

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	): Unit = with(builder) {
		append("«(")
		append(self.expression)
		append(" :: ")
		append(self.superUnionType)
		append(")»")
	}

	override fun o_ChildrenDo(
		self: AvailObject,
		action: (A_Phrase) -> Unit
	) = action(self.slot(EXPRESSION))

	override fun o_ChildrenMap(
		self: AvailObject,
		transformer: (A_Phrase) -> A_Phrase
	) = self.setSlot(EXPRESSION, transformer(self.slot(EXPRESSION)))

	override fun o_EmitValueOn(
		self: AvailObject,
		codeGenerator: AvailCodeGenerator
	) = self.slot(EXPRESSION).emitValueOn(codeGenerator)

	override fun o_EqualsPhrase(
		self: AvailObject,
		aPhrase: A_Phrase
	): Boolean = (!aPhrase.isMacroSubstitutionNode
		&& self.phraseKind == aPhrase.phraseKind
		&& self.expression.equals(aPhrase.expression)
		&& self.superUnionType.equals(aPhrase.superUnionType))

	/**
	 * Answer the expression producing the actual value.
	 */
	override fun o_Expression(self: AvailObject): A_Phrase =
		self.slot(EXPRESSION)

	/**
	 * Answer the lookup type to ensure polymorphic macro substitutions happen
	 * the right way.
	 */
	override fun o_PhraseExpressionType(self: AvailObject): A_Type =
		self.slot(TYPE_FOR_LOOKUP)

	override fun o_Hash(self: AvailObject): Int = combine3(
		self.slot(EXPRESSION).hash(),
		self.slot(TYPE_FOR_LOOKUP).hash(),
		0x5035ebe5)

	override fun o_HasSuperCast(self: AvailObject): Boolean = true

	override fun o_PhraseKind(self: AvailObject): PhraseKind =
		PhraseKind.SUPER_CAST_PHRASE

	override fun o_Sequence (self: AvailObject): A_Phrase =
		self.slot(EXPRESSION).sequence

	override fun o_SerializerOperation(self: AvailObject): SerializerOperation =
		SerializerOperation.SUPER_CAST_PHRASE

	override fun o_StatementsDo(
		self: AvailObject,
		continuation: (A_Phrase) -> Unit
	): Unit = unsupported

	override fun o_SuperUnionType(self: AvailObject): A_Type =
		self.slot(TYPE_FOR_LOOKUP)

	override fun o_Tokens(self: AvailObject): A_Tuple =
		self.slot(EXPRESSION).tokens

	override fun o_ValidateLocally(
		self: AvailObject,
		parent: A_Phrase?
	) {
		// Do nothing.
	}

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("super cast phrase") }
			at("expression") { self.slot(EXPRESSION).writeTo(writer) }
			at("type to lookup") { self.slot(TYPE_FOR_LOOKUP).writeTo(writer) }
		}

	override fun o_WriteSummaryTo(self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("list phrase") }
			at("expression") { self.slot(EXPRESSION).writeSummaryTo(writer) }
			at("type to lookup") {
				self.slot(TYPE_FOR_LOOKUP).writeSummaryTo(writer)
			}
		}

	override fun mutable() = mutable

	override fun shared() = shared

	companion object {
		/**
		 * Create a new [super&#32;cast&#32;phrase][SuperCastPhraseDescriptor]
		 * from the given [phrase][PhraseDescriptor] and [type][A_Type] with
		 * which to perform a method lookup.
		 *
		 * @param expression
		 *   The base expression.
		 * @param superUnionType
		 *   The type to combine via a [type&#32;union][A_Type.typeUnion] with
		 *   the type of the actual runtime value produced by the expression, in
		 *   order to look up the method.
		 * @return
		 *   The resulting super cast phrase.
		 */
		fun newSuperCastNode(
			expression: A_Phrase,
			superUnionType: A_Type
		): A_Phrase = mutable.createShared {
			setSlot(EXPRESSION, expression)
			setSlot(TYPE_FOR_LOOKUP, superUnionType)
		}

		/** The mutable [SuperCastPhraseDescriptor]. */
		private val mutable = SuperCastPhraseDescriptor(Mutability.MUTABLE)

		/** The shared [SuperCastPhraseDescriptor]. */
		private val shared = SuperCastPhraseDescriptor(Mutability.SHARED)
	}
}
