/*
 * InfinityDescriptor.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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
package com.avail.descriptor.numbers

import com.avail.descriptor.numbers.AbstractNumberDescriptor.Sign.POSITIVE
import com.avail.descriptor.numbers.DoubleDescriptor.Companion.compareDoubles
import com.avail.descriptor.numbers.DoubleDescriptor.Companion.fromDoubleRecycling
import com.avail.descriptor.numbers.FloatDescriptor.Companion.fromFloatRecycling
import com.avail.descriptor.numbers.IntegerDescriptor.Companion.zero
import com.avail.descriptor.representation.A_BasicObject
import com.avail.descriptor.representation.AvailObject
import com.avail.descriptor.representation.Mutability
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.IntegerRangeTypeDescriptor
import com.avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.singleInteger
import com.avail.descriptor.types.TypeDescriptor.Types
import com.avail.descriptor.types.TypeTag
import com.avail.exceptions.ArithmeticException
import com.avail.exceptions.AvailErrorCode.E_CANNOT_ADD_UNLIKE_INFINITIES
import com.avail.exceptions.AvailErrorCode.E_CANNOT_DIVIDE_INFINITIES
import com.avail.exceptions.AvailErrorCode.E_CANNOT_MULTIPLY_ZERO_AND_INFINITY
import com.avail.exceptions.AvailErrorCode.E_CANNOT_SUBTRACT_LIKE_INFINITIES
import com.avail.utility.json.JSONWriter
import java.util.IdentityHashMap

/**
 * I represent the [extended&#32;integers][ExtendedIntegerDescriptor] positive
 * infinity and negative infinity.  By supporting these as first-class values in
 * Avail we eliminate arbitrary limits, awkward duplication of effort, and a
 * host of other dangling singularities.  For example, it makes sense to talk
 * about iterating from 1 to infinity.  Infinities also play a key role in
 * [integer&#32;range&#32;types][IntegerRangeTypeDescriptor], specifically by
 * their appearance as inclusive or exclusive bounds.
 *
 * @property sign
 *   The [Sign][AbstractNumberDescriptor.Sign] of this infinity.
 *
 * @constructor
 *
 * @param mutability
 *   The [Mutability] of the new descriptor.
 * @param sign
 *   The [Sign][AbstractNumberDescriptor.Sign] of the infinity for this
 *   descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
class InfinityDescriptor private constructor(
	mutability: Mutability,
	val sign: Sign
) : ExtendedIntegerDescriptor(
	mutability,
	when (sign) {
		POSITIVE -> TypeTag.POSITIVE_INFINITY_TAG
		else -> TypeTag.NEGATIVE_INFINITY_TAG
	},
	null,
	null
) {
	init {
		assert(sign == POSITIVE || sign == Sign.NEGATIVE)
	}

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	) {
		builder.append(if (self.isPositive()) "\u221E" else "-\u221E")
	}

	override fun o_Equals(
		self: AvailObject,
		another: A_BasicObject
	): Boolean = another.equalsInfinity(sign)

	override fun o_EqualsInfinity(
		self: AvailObject,
		sign: Sign
	): Boolean = this.sign == sign

	/*
	 * Infinities are either above or below all integers, depending on sign.
	 */
	override fun o_NumericCompareToInteger(
		self: AvailObject,
		anInteger: AvailObject
	): Order = when (sign) {
		POSITIVE -> Order.MORE
		else -> Order.LESS
	}

	override fun o_NumericCompareToInfinity(
		self: AvailObject,
		sign: Sign
	): Order = compareDoubles(this.sign.limitDouble(), sign.limitDouble())

	override fun o_IsInstanceOfKind(
		self: AvailObject,
		aType: A_Type
	): Boolean = when {
		aType.isSupertypeOfPrimitiveTypeEnum(Types.NUMBER) -> true
		!aType.isIntegerRangeType -> false
		sign == POSITIVE ->
			aType.upperBound().equals(self) && aType.upperInclusive()
		else -> aType.lowerBound().equals(self) && aType.lowerInclusive()
	}

	override fun o_Hash(self: AvailObject): Int =
		if (sign == POSITIVE) 0x14B326DA else 0xBF9302D

	override fun o_IsFinite(self: AvailObject): Boolean = false

	override fun o_Kind(self: AvailObject): A_Type = singleInteger(self)

	override fun o_ExtractDouble(self: AvailObject): Double =
		when {
			self.isPositive() -> Double.POSITIVE_INFINITY
			else -> Double.NEGATIVE_INFINITY
		}

	override fun o_ExtractFloat(
		self: AvailObject
	): Float =
		when {
			self.isPositive() -> Float.POSITIVE_INFINITY
			else -> Float.NEGATIVE_INFINITY
		}

	override fun o_DivideCanDestroy(
		self: AvailObject,
		aNumber: A_Number,
		canDestroy: Boolean
	): A_Number = aNumber.divideIntoInfinityCanDestroy(sign, canDestroy)

	override fun o_MinusCanDestroy(
		self: AvailObject,
		aNumber: A_Number,
		canDestroy: Boolean
	): A_Number = aNumber.subtractFromInfinityCanDestroy(sign, canDestroy)

	override fun o_PlusCanDestroy(
		self: AvailObject,
		aNumber: A_Number,
		canDestroy: Boolean
	): A_Number = aNumber.addToInfinityCanDestroy(sign, canDestroy)

	override fun o_TimesCanDestroy(
		self: AvailObject,
		aNumber: A_Number,
		canDestroy: Boolean
	): A_Number = aNumber.multiplyByInfinityCanDestroy(sign, canDestroy)

	override fun o_IsPositive(self: AvailObject): Boolean = sign === POSITIVE

	override fun o_AddToInfinityCanDestroy(
		self: AvailObject,
		sign: Sign,
		canDestroy: Boolean
	): A_Number = when (sign) {
		this.sign -> self
		else -> throw ArithmeticException(E_CANNOT_ADD_UNLIKE_INFINITIES)
	}

	override fun o_AddToIntegerCanDestroy(
		self: AvailObject,
		anInteger: AvailObject,
		canDestroy: Boolean
	): A_Number = self

	override fun o_AddToDoubleCanDestroy(
		self: AvailObject,
		doubleObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromDoubleRecycling(
		doubleObject.extractDouble() + sign.limitDouble(),
		doubleObject,
		canDestroy)

	override fun o_AddToFloatCanDestroy(
		self: AvailObject,
		floatObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromFloatRecycling(
		floatObject.extractFloat() + sign.limitFloat(),
		floatObject,
		canDestroy)

	override fun o_DivideIntoInfinityCanDestroy(
		self: AvailObject,
		sign: Sign,
		canDestroy: Boolean
	): A_Number = throw ArithmeticException(E_CANNOT_DIVIDE_INFINITIES)

	override fun o_DivideIntoIntegerCanDestroy(
		self: AvailObject,
		anInteger: AvailObject,
		canDestroy: Boolean
	): A_Number = zero

	override fun o_DivideIntoDoubleCanDestroy(
		self: AvailObject,
		doubleObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromDoubleRecycling(
		doubleObject.extractDouble() / sign.limitDouble(),
		doubleObject,
		canDestroy)

	override fun o_DivideIntoFloatCanDestroy(
		self: AvailObject,
		floatObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromFloatRecycling(
		floatObject.extractFloat() / sign.limitFloat(),
		floatObject,
		canDestroy)

	override fun o_MultiplyByInfinityCanDestroy(
		self: AvailObject,
		sign: Sign,
		canDestroy: Boolean
	): A_Number = when {
		(sign == POSITIVE) == self.isPositive() -> positiveInfinity()
		else -> negativeInfinity()
	}

	override fun o_MultiplyByIntegerCanDestroy(
		self: AvailObject,
		anInteger: AvailObject,
		canDestroy: Boolean
	): A_Number = when {
		anInteger.equalsInt(0) ->
			throw ArithmeticException(E_CANNOT_MULTIPLY_ZERO_AND_INFINITY)
		anInteger.greaterThan(zero) xor self.isPositive() ->
			negativeInfinity()
		else -> positiveInfinity()
	}

	override fun o_MultiplyByDoubleCanDestroy(
		self: AvailObject,
		doubleObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromDoubleRecycling(
		doubleObject.extractDouble() * sign.limitDouble(),
		doubleObject,
		canDestroy)

	override fun o_MultiplyByFloatCanDestroy(
		self: AvailObject,
		floatObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromFloatRecycling(
		floatObject.extractFloat() * sign.limitFloat(),
		floatObject,
		canDestroy)

	override fun o_SubtractFromInfinityCanDestroy(
		self: AvailObject,
		sign: Sign,
		canDestroy: Boolean
	): A_Number = when (sign) {
		this.sign ->
			throw ArithmeticException(E_CANNOT_SUBTRACT_LIKE_INFINITIES)
		POSITIVE -> positiveInfinity()
		else -> negativeInfinity()
	}

	override fun o_SubtractFromIntegerCanDestroy(
		self: AvailObject,
		anInteger: AvailObject,
		canDestroy: Boolean
	): A_Number = when {
		self.isPositive() -> negativeInfinity()
		else -> positiveInfinity()
	}

	override fun o_SubtractFromDoubleCanDestroy(
		self: AvailObject,
		doubleObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromDoubleRecycling(
		doubleObject.extractDouble() - sign.limitDouble(),
		doubleObject,
		canDestroy)

	override fun o_SubtractFromFloatCanDestroy(
		self: AvailObject,
		floatObject: A_Number,
		canDestroy: Boolean
	): A_Number = fromFloatRecycling(
		floatObject.extractFloat() - sign.limitFloat(),
		floatObject,
		canDestroy)

	override fun o_NumericCompare(
		self: AvailObject,
		another: A_Number
	): Order = another.numericCompareToInfinity(sign).reverse()

	override fun o_NumericCompareToDouble(
		self: AvailObject,
		aDouble: Double
	): Order = compareDoubles(sign.limitDouble(), aDouble)

	override fun o_MakeImmutable(self: AvailObject): AvailObject {
		assert(isShared)
		return self
	}

	override fun o_MakeShared(self: AvailObject): AvailObject {
		if (!isShared) {
			self.setDescriptor(shared())
		}
		return self
	}

	/*
	 * Not finite, so not numerically equal to an integer.
	 */
	override fun o_IsNumericallyIntegral(self: AvailObject): Boolean = false

	/**
	 * Shift the given infinity to the left by the specified shift factor
	 * (number of bits).  The shift factor is a finite integer, and shifting
	 * left or right never changes the sign of a number, so we answer self.
	 *
	 * @param self
	 *   The infinity to shift.
	 * @param shiftFactor
	 *   How much to shift left (may be negative to indicate a right shift).
	 * @param canDestroy
	 *   Whether it is permitted to alter the original object if it happens to
	 *   be mutable.
	 * @return
	 *   ⌊self &times; 2^shiftFactor⌋
	 */
	override fun o_BitShift(
		self: AvailObject,
		shiftFactor: A_Number,
		canDestroy: Boolean
	): A_Number = self

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter) =
		writer.write(
			if (sign == POSITIVE) Double.POSITIVE_INFINITY
			else Double.NEGATIVE_INFINITY)

	override fun mutable() =
		if (sign == POSITIVE) mutablePositive else mutableNegative

	// There isn't an immutable variant; answer a shared one.
	override fun immutable() = shared()

	override fun shared() =
		if (sign == POSITIVE) sharedPositive else sharedNegative

	companion object {
		/** The mutable [InfinityDescriptor] for positive infinity.  */
		private val mutablePositive =
			InfinityDescriptor(Mutability.MUTABLE, POSITIVE)

		/** The mutable [InfinityDescriptor] for negative infinity.  */
		private val mutableNegative =
			InfinityDescriptor(Mutability.MUTABLE, Sign.NEGATIVE)

		/** The shared [InfinityDescriptor] for positive infinity.  */
		private val sharedPositive =
			InfinityDescriptor(Mutability.SHARED, POSITIVE)

		/** The shared [InfinityDescriptor] for negative infinity.  */
		private val sharedNegative =
			InfinityDescriptor(Mutability.SHARED, Sign.NEGATIVE)

		/**
		 * The Avail [extended&#32;integer][ExtendedIntegerDescriptor]
		 * representing positive infinity.
		 */
		private val positiveInfinity: A_Number =
			mutablePositive.createShared { }

		/**
		 * Answer the positive infinity object.
		 *
		 * @return
		 *   Positive infinity.
		 */
		@JvmStatic
		fun
			positiveInfinity(): A_Number = positiveInfinity

		/**
		 * The Avail [extended&#32;integer][ExtendedIntegerDescriptor]
		 * representing negative infinity.
		 */
		private val negativeInfinity: A_Number =
			mutableNegative.createShared { }

		/**
		 * Answer the negative infinity object.
		 *
		 * @return
		 *   Negative infinity.
		 */
		@JvmStatic
		fun negativeInfinity(): A_Number = negativeInfinity
	}
}
