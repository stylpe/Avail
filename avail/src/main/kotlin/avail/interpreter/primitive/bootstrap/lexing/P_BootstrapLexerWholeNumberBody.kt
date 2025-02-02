/*
 * P_BootstrapLexerWholeNumberBody.kt
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

package avail.interpreter.primitive.bootstrap.lexing

import avail.descriptor.numbers.A_Number
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.numbers.A_Number.Companion.plusCanDestroy
import avail.descriptor.numbers.A_Number.Companion.timesCanDestroy
import avail.descriptor.numbers.IntegerDescriptor
import avail.descriptor.numbers.IntegerDescriptor.Companion.cachedSquareOfQuintillion
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromLong
import avail.descriptor.parsing.LexerDescriptor.Companion.lexerBodyFunctionType
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tokens.LiteralTokenDescriptor.Companion.literalToken
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.A_Tuple.Companion.tupleCodePointAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.types.A_Type
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.Bootstrap
import avail.interpreter.Primitive.Flag.CanFold
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.Primitive.Flag.CannotFail
import avail.interpreter.execution.Interpreter

/**
 * The `P_BootstrapLexerWholeNumberBody` primitive is used for parsing
 * non-negative integer literal tokens.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
@Suppress("unused")
object P_BootstrapLexerWholeNumberBody
	: Primitive(3, CannotFail, CanFold, CanInline, Bootstrap)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(3)
		val source = interpreter.argument(0)
		val sourcePositionInteger = interpreter.argument(1)
		val lineNumberInteger = interpreter.argument(2)

		val startPosition = sourcePositionInteger.extractInt
		val digitCount = countDigits(source, startPosition)

		val string = source.copyStringFromToCanDestroy(
			startPosition, startPosition + digitCount - 1, false)
		val number = readInteger(string, 1, digitCount)
		val token = literalToken(
			string, startPosition, lineNumberInteger.extractInt, number)
		return interpreter.primitiveSuccess(set(tuple(token)))
	}

	override fun privateBlockTypeRestriction(): A_Type = lexerBodyFunctionType()

	/**
	 * Determine how many consecutive decimal digits occur in the string,
	 * starting at the given startPosition.
	 *
	 * @param string
	 *   The string to examine.
	 * @param startPosition
	 *   The start of the run of digits in the string.
	 * @return The number of consecutive digits that were found.
	 */
	private fun countDigits(string: A_String, startPosition: Int): Int
	{
		val stringSize = string.tupleSize
		var position = startPosition
		while (position <= stringSize
				&& Character.isDigit(string.tupleCodePointAt(position)))
		{
			position++
		}
		assert(position > startPosition)
		return position - startPosition
	}

	/**
	 * Read an integer from a range of the string.
	 *
	 * @param string
	 *   The string to read digits from.
	 * @param startPosition
	 *   The position of the first digit to read.
	 * @param digitCount
	 *   The number of digits to read.
	 * @return A positive Avail [integer][IntegerDescriptor].
	 */
	private fun readInteger(
		string: A_String, startPosition: Int, digitCount: Int): A_Number
	{
		assert(digitCount > 0)
		if (digitCount <= 18)
		{
			// Process a (potentially short) group.
			var value: Long = 0
			var position = startPosition
			for (i in digitCount downTo 1)
			{
				value *= 10L
				value += Character.digit(
					string.tupleCodePointAt(position++), 10).toLong()
			}
			return fromLong(value)
		}

		// Find N, the largest power of two times 18 that's less than the
		// digitCount.  Recurse on that many of the right digits to get R, then
		// recurse on the remaining left digits to get L, then compute
		// (10^18)^N * L + R.
		val groupCount = (digitCount + 17) / 18
		val logGroupCount = 31 - Integer.numberOfLeadingZeros(groupCount - 1)
		val rightGroupCount = 1 shl logGroupCount
		assert(rightGroupCount < groupCount)
		assert(rightGroupCount shl 1 >= groupCount)
		assert(logGroupCount >= 0)

		val rightCount = 18 shl logGroupCount
		val leftPart = readInteger(
			string, startPosition, digitCount - rightCount)
		val rightPart = readInteger(
			string, startPosition + digitCount - rightCount, rightCount)
		val squareOfQuintillion = cachedSquareOfQuintillion(logGroupCount)
		val shiftedLeft = leftPart.timesCanDestroy(squareOfQuintillion, true)
		return shiftedLeft.plusCanDestroy(rightPart, true)
	}
}
