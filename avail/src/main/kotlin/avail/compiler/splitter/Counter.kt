/*
 * Counter.kt
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
package avail.compiler.splitter

import avail.compiler.ParsingConversionRule.LIST_TO_SIZE
import avail.compiler.ParsingOperation.CONVERT
import avail.compiler.splitter.MessageSplitter.Companion.throwSignatureException
import avail.compiler.splitter.MessageSplitter.Metacharacter
import avail.compiler.splitter.WrapState.SHOULD_NOT_PUSH_LIST
import avail.descriptor.numbers.A_Number.Companion.equalsInt
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.phrases.A_Phrase
import avail.descriptor.phrases.A_Phrase.Companion.token
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.isSubtypeOf
import avail.descriptor.types.A_Type.Companion.lowerBound
import avail.descriptor.types.A_Type.Companion.phraseTypeExpressionType
import avail.descriptor.types.InstanceTypeDescriptor.Companion.instanceType
import avail.descriptor.types.IntegerRangeTypeDescriptor
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.wholeNumbers
import avail.descriptor.types.ListPhraseTypeDescriptor
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.LIST_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.PARSE_PHRASE
import avail.descriptor.types.TupleTypeDescriptor.Companion.tupleTypeForSizesTypesDefaultType
import avail.exceptions.AvailErrorCode.E_INCORRECT_TYPE_FOR_COUNTING_GROUP
import avail.exceptions.SignatureException
import java.util.Collections

/**
 * A `Counter` is a special subgroup (i.e., not a root group)
 * indicated by an [octothorp][Metacharacter.OCTOTHORP] following a
 * [group][Group]. It may not contain [arguments][Argument] or subgroups, though
 * it may contain a [double&#32;dagger][Metacharacter.DOUBLE_DAGGER].
 *
 * When a double dagger appears in a counter, the counter produces a
 * [whole&#32;number][IntegerRangeTypeDescriptor.wholeNumbers] that indicates
 * the number of occurrences of the subexpression to the left of the double
 * dagger. The message "«very‡,»#good" accepts a single argument: the count of
 * occurrences of "very".
 *
 * When no double dagger appears in a counter, then the counter produces
 * a whole number that indicates the number of occurrences of the entire
 * group. The message "«very»#good" accepts a single argument: the count of
 * occurrences of "very".
 *
 * @property group
 *   The [group][Group] whose occurrences should be counted.
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 *
 * @constructor
 *
 * Construct a new `Counter`.
 *
 * @param positionInName
 *   The position of the start of the group in the message name.
 * @param group
 *   The [group][Group] whose occurrences should be counted.
 */
internal class Counter(
	positionInName: Int,
	private val group: Group
) : Expression(positionInName)
{
	init
	{
		assert(group.beforeDagger.yielders.isEmpty())
		assert(group.afterDagger.yielders.isEmpty())
	}

	override val recursivelyContainsReorders: Boolean
		get() = group.recursivelyContainsReorders

	override val yieldsValue
		get() = true

	override val isLowerCase
		get() = group.isLowerCase

	override fun applyCaseInsensitive() =
		Counter(positionInName, group.applyCaseInsensitive())

	override val underscoreCount: Int
		get()
		{
			assert(group.underscoreCount == 0)
			return 0
		}

	override fun extractSectionCheckpointsInto(
			sectionCheckpoints: MutableList<SectionCheckpoint>)
		= group.extractSectionCheckpointsInto(sectionCheckpoints)

	@Throws(SignatureException::class)
	override fun checkType(argumentType: A_Type, sectionNumber: Int)
	{
		// The declared type for the subexpression must be a subtype of whole
		// number.
		if (!argumentType.isSubtypeOf(wholeNumbers))
		{
			throwSignatureException(E_INCORRECT_TYPE_FOR_COUNTING_GROUP)
		}
	}

	@Suppress("LocalVariableName")
	override fun emitOn(
		phraseType: A_Type,
		generator: InstructionGenerator,
		wrapState: WrapState): WrapState
	{
		/* push current parse position
		 * push empty list
		 * branch to $loopSkip
		 * $loopStart:
		 * ...Stuff before dagger.  Must not have arguments or subgroups.
		 * push empty list (represents group presence)
		 * append (add solution)
		 * branch to $loopExit (even if no dagger)
		 * ...Stuff after dagger, nothing if dagger is omitted.  Must not have
		 * ...arguments or subgroups.
		 * check progress and update saved position, or abort.
		 * jump to $loopStart
		 * $loopExit:
		 * check progress and update saved position, or abort.
		 * $loopSkip:
		 * under-pop parse position (remove 2nd from top of stack)
		 */
		generator.flushDelayed()
		val phraseCountRange = phraseType.phraseTypeExpressionType
		val emptyTupleType = instanceType(emptyTuple)
		val tupleOfEmptyTuplesType = tupleTypeForSizesTypesDefaultType(
			phraseCountRange, emptyTuple, emptyTupleType)
		val tupleOfEmptyTuplePhrasesType = tupleTypeForSizesTypesDefaultType(
			phraseCountRange, emptyTuple, PARSE_PHRASE.create(emptyTupleType))
		val listPhraseType = ListPhraseTypeDescriptor.createListPhraseType(
			LIST_PHRASE, tupleOfEmptyTuplesType, tupleOfEmptyTuplePhrasesType)
		val newWrapState = group.emitOn(listPhraseType, generator, wrapState)
		assert(newWrapState == SHOULD_NOT_PUSH_LIST)
		generator.emit(this, CONVERT, LIST_TO_SIZE.number)
		return wrapState.processAfterPushedArgument(this, generator)
	}

	override fun toString(): String =
		"${this@Counter.javaClass.simpleName}($group)"

	override fun printWithArguments(
		arguments: Iterator<A_Phrase>?,
		builder: StringBuilder,
		indent: Int)
	{
		val countLiteral = arguments!!.next()
		assert(
			countLiteral.isInstanceOf(
				PhraseKind.LITERAL_PHRASE.mostGeneralType))
		val count = countLiteral.token.literal().extractInt
		for (i in 1..count)
		{
			if (i > 1)
			{
				builder.append(' ')
			}
			group.printGroupOccurrence(
				Collections.emptyIterator(),
				builder,
				indent,
				yieldsValue)
		}
		builder.append('#')
	}

	override val shouldBeSeparatedOnLeft: Boolean
		// This Counter node should be separated on the left if the
		// contained group should be.
		get() = group.shouldBeSeparatedOnLeft

	override val shouldBeSeparatedOnRight: Boolean
		// This Counter node should be separated on the right to emphasize
		// the trailing "#".
		get() = true

	override fun mightBeEmpty(phraseType: A_Type): Boolean
	{
		val integerRangeType = phraseType.phraseTypeExpressionType
		assert(integerRangeType.isIntegerRangeType)
		return integerRangeType.lowerBound.equalsInt(0)
	}

	override fun checkListStructure(phrase: A_Phrase): Boolean = true
}
