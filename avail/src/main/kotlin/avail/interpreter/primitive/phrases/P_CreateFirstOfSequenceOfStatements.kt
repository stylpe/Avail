/*
 * P_CreateFirstOfSequenceOfStatements.kt
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

package avail.interpreter.primitive.phrases

import avail.descriptor.phrases.A_Phrase
import avail.descriptor.phrases.A_Phrase.Companion.flattenStatementsInto
import avail.descriptor.phrases.FirstOfSequencePhraseDescriptor
import avail.descriptor.phrases.FirstOfSequencePhraseDescriptor.Companion.newFirstOfSequenceNode
import avail.descriptor.phrases.PhraseDescriptor.Companion.containsOnlyStatements
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.TupleDescriptor
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.FIRST_OF_SEQUENCE_PHRASE
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.PARSE_PHRASE
import avail.descriptor.types.TupleTypeDescriptor.Companion.zeroOrMoreOf
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AvailErrorCode.E_SEQUENCE_CONTAINS_INVALID_STATEMENTS
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanFold
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.execution.Interpreter

/**
 * **Primitive:** Create a [first-of-sequence][FirstOfSequencePhraseDescriptor]
 * phrase from the specified [tuple][TupleDescriptor] of statements.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
@Suppress("unused")
object P_CreateFirstOfSequenceOfStatements : Primitive(1, CanFold, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val statements = interpreter.argument(0)
		val statementsSize = statements.tupleSize
		val flat = mutableListOf<A_Phrase>()
		for (i in 2 .. statementsSize)
		{
			statements.tupleAt(i).flattenStatementsInto(flat)
		}
		if (!containsOnlyStatements(flat, TOP.o))
		{
			return interpreter.primitiveFailure(
				E_SEQUENCE_CONTAINS_INVALID_STATEMENTS)
		}
		flat.add(0, statements.tupleAt(1))
		return interpreter.primitiveSuccess(newFirstOfSequenceNode(statements))
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				zeroOrMoreOf(PARSE_PHRASE.mostGeneralType)),
			FIRST_OF_SEQUENCE_PHRASE.mostGeneralType)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(E_SEQUENCE_CONTAINS_INVALID_STATEMENTS))
}
