/*
 * SetTypeDescriptor.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
package avail.descriptor.types

import avail.descriptor.numbers.A_Number.Companion.equalsInt
import avail.descriptor.numbers.A_Number.Companion.lessOrEqual
import avail.descriptor.numbers.A_Number.Companion.minusCanDestroy
import avail.descriptor.numbers.A_Number.Companion.plusCanDestroy
import avail.descriptor.numbers.IntegerDescriptor.Companion.one
import avail.descriptor.numbers.IntegerDescriptor.Companion.zero
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObject.Companion.combine3
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.sets.SetDescriptor
import avail.descriptor.types.A_Type.Companion.computeSuperkind
import avail.descriptor.types.A_Type.Companion.contentType
import avail.descriptor.types.A_Type.Companion.instanceCount
import avail.descriptor.types.A_Type.Companion.isSubtypeOf
import avail.descriptor.types.A_Type.Companion.isSupertypeOfSetType
import avail.descriptor.types.A_Type.Companion.lowerBound
import avail.descriptor.types.A_Type.Companion.sizeRange
import avail.descriptor.types.A_Type.Companion.trimType
import avail.descriptor.types.A_Type.Companion.typeIntersection
import avail.descriptor.types.A_Type.Companion.typeIntersectionOfSetType
import avail.descriptor.types.A_Type.Companion.typeUnion
import avail.descriptor.types.A_Type.Companion.typeUnionOfSetType
import avail.descriptor.types.A_Type.Companion.upperBound
import avail.descriptor.types.A_Type.Companion.upperInclusive
import avail.descriptor.types.BottomTypeDescriptor.Companion.bottom
import avail.descriptor.types.InstanceMetaDescriptor.Companion.instanceMeta
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.inclusive
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.int32
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.singleInteger
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.wholeNumbers
import avail.descriptor.types.SetTypeDescriptor.ObjectSlots
import avail.descriptor.types.SetTypeDescriptor.ObjectSlots.CONTENT_TYPE
import avail.descriptor.types.SetTypeDescriptor.ObjectSlots.SIZE_RANGE
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ANY
import avail.serialization.SerializerOperation
import org.availlang.json.JSONWriter
import java.util.IdentityHashMap

/**
 * A `SetTypeDescriptor` object instance is a type that some
 * [sets][SetDescriptor] may conform to. It is built up from a [range of
 * sizes][ObjectSlots.SIZE_RANGE] that the sets may be, and the [content
 * type][ObjectSlots.CONTENT_TYPE] that the set's elements would have to conform
 * to.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @constructor
 * Construct a new [SetTypeDescriptor].
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 */
class SetTypeDescriptor
private constructor(
	mutability: Mutability
) : TypeDescriptor(
	mutability,
	TypeTag.SET_TYPE_TAG,
	TypeTag.SET_TAG,
	ObjectSlots::class.java,
	null)
{
	/**
	 * The layout of object slots for my instances.
	 */
	enum class ObjectSlots : ObjectSlotsEnum
	{
		/**
		 * An [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] which
		 * limits the sizes of [set][SetDescriptor]s that may be instances of
		 * this type.
		 */
		SIZE_RANGE,

		/**
		 * A [type][TypeDescriptor] which limits the objects which may be
		 * members of [set][SetDescriptor]s if they purport to be of this set
		 * type.
		 */
		CONTENT_TYPE
	}

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int)
	{
		if (self.slot(CONTENT_TYPE).equals(ANY.o)
			&& self.slot(SIZE_RANGE).equals(wholeNumbers))
		{
			builder.append("set")
			return
		}
		builder.append('{')
		self.slot(CONTENT_TYPE).printOnAvoidingIndent(
			builder, recursionMap, indent + 1)
		builder.append('|')
		val sizeRange: A_Type = self.slot(SIZE_RANGE)
		if (sizeRange.equals(wholeNumbers))
		{
			builder.append('}')
			return
		}
		sizeRange.lowerBound.printOnAvoidingIndent(
			builder, recursionMap, indent + 1)
		if (!sizeRange.lowerBound.equals(sizeRange.upperBound))
		{
			builder.append("..")
			sizeRange.upperBound.printOnAvoidingIndent(
				builder, recursionMap, indent + 1)
		}
		builder.append('}')
	}

	override fun o_ContentType(self: AvailObject): A_Type =
		self.slot(CONTENT_TYPE)

	override fun o_SizeRange(self: AvailObject): A_Type =
		self.slot(SIZE_RANGE)

	override fun o_Equals(self: AvailObject, another: A_BasicObject): Boolean =
		another.equalsSetType(self)

	// Set types are equal iff both their sizeRange and contentType match.
	override fun o_EqualsSetType(self: AvailObject, aSetType: A_Type): Boolean =
		if (self.sameAddressAs(aSetType)) true
		else self.slot(SIZE_RANGE).equals(aSetType.sizeRange)
			&& self.slot(CONTENT_TYPE)
				.equals(aSetType.contentType)

	// Answer a 32-bit integer that is always the same for equal objects,
	// but statistically different for different objects.
	override fun o_Hash(self: AvailObject): Int =
		combine3(self.sizeRange.hash(), self.contentType.hash(), -0x0098ef2b)

	// Check if self (a set type) is a subtype of aType (should also be a type).
	override fun o_IsSubtypeOf(self: AvailObject, aType: A_Type): Boolean =
		aType.isSupertypeOfSetType(self)

	override fun o_IsSupertypeOfSetType(
		self: AvailObject,
		aSetType: A_Type): Boolean
	{
		// Set type A is a subtype of B if and only if their size ranges are
		// covariant and their content types are covariant.
		val otherType = aSetType as AvailObject
		return (otherType.slot(SIZE_RANGE).isSubtypeOf(self.slot(SIZE_RANGE))
			&& otherType.slot(CONTENT_TYPE).isSubtypeOf(
				self.slot(CONTENT_TYPE)))
	}

	override fun o_IsVacuousType(self: AvailObject): Boolean =
		(!self.slot(SIZE_RANGE).lowerBound.equalsInt(0)
			&& self.slot(CONTENT_TYPE).isVacuousType)

	override fun o_TrimType(self: AvailObject, typeToRemove: A_Type): A_Type
	{
		if (!self.sizeRange.isSubtypeOf(int32))
		{
			// Trim the type to only those that are physically possible.
			self.makeImmutable()
			return setTypeForSizesContentType(
				self.sizeRange.typeIntersection(int32),
				self.contentType
			).trimType(typeToRemove)
		}
		if (self.isSubtypeOf(typeToRemove)) return bottom
		if (!typeToRemove.isSetType) return self
		if (typeToRemove.isEnumeration) return self
		self.makeImmutable()
		typeToRemove.makeImmutable()
		if (!typeToRemove.sizeRange.isSubtypeOf(int32))
		{
			// Trim the type to only those that are physically possible.
			return self.trimType(
				setTypeForSizesContentType(
					typeToRemove.sizeRange.typeIntersection(int32),
					typeToRemove.contentType))
		}
		val contentType = self.contentType
		val removedContentType = typeToRemove.contentType
		val sizeRange = self.sizeRange
		val removedSizeRange = typeToRemove.sizeRange
		if (contentType.isSubtypeOf(removedContentType))
		{
			// The element types won't be enough to keep sets around, so we can
			// use the set sizes to determine what we can actually exclude.
			return setTypeForSizesContentType(
				sizeRange.trimType(removedSizeRange), contentType)
		}
		if (sizeRange.isSubtypeOf(removedSizeRange))
		{
			// The sizes won't be enough to keep sets around, so we can use the
			// content type to determine what we can actually exclude.
			return setTypeForSizesContentType(
				sizeRange, contentType.trimType(removedContentType))
		}
		return self
	}

	// Answer the most general type that is still at least as specific as
	// these.
	override fun o_TypeIntersection(
		self: AvailObject,
		another: A_Type): A_Type =
			when
			{
				self.isSubtypeOf(another) -> self
				another.isSubtypeOf(self) -> another
				else -> another.typeIntersectionOfSetType(self)
			}

	override fun o_TypeIntersectionOfSetType(
		self: AvailObject,
		aSetType: A_Type): A_Type
	{
		return setTypeForSizesContentType(
			self.slot(SIZE_RANGE).typeIntersection(aSetType.sizeRange),
			self.slot(CONTENT_TYPE).typeIntersection(aSetType.contentType))
	}

	override fun o_TypeUnion(self: AvailObject, another: A_Type): A_Type =
		when
		{
			self.equals(another) -> self
			self.isSubtypeOf(another) -> another
			another.isSubtypeOf(self) -> self
			else -> another.typeUnionOfSetType(self)
		}

	override fun o_TypeUnionOfSetType(
		self: AvailObject,
		aSetType: A_Type): A_Type =
			setTypeForSizesContentType(
				self.slot(SIZE_RANGE).typeUnion(aSetType.sizeRange),
				self.slot(CONTENT_TYPE).typeUnion(aSetType.contentType))

	override fun o_IsSetType(self: AvailObject): Boolean = true

	override fun o_SerializerOperation(self: AvailObject): SerializerOperation =
		SerializerOperation.SET_TYPE

	override fun o_MakeImmutable(self: AvailObject): AvailObject
	{
		if (isMutable)
		{
			// Make the object shared, since there isn't an immutable choice.
			self.makeShared()
		}
		return self
	}

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter)
	{
		writer.startObject()
		writer.write("kind")
		writer.write("set type")
		writer.write("content type")
		self.slot(CONTENT_TYPE).writeTo(writer)
		writer.write("cardinality")
		self.slot(SIZE_RANGE).writeTo(writer)
		writer.endObject()
	}

	override fun o_WriteSummaryTo(self: AvailObject, writer: JSONWriter)
	{
		writer.startObject()
		writer.write("kind")
		writer.write("set type")
		writer.write("content type")
		self.slot(CONTENT_TYPE).writeSummaryTo(writer)
		writer.write("cardinality")
		self.slot(SIZE_RANGE).writeTo(writer)
		writer.endObject()
	}

	override fun mutable(): SetTypeDescriptor = mutable

	// There isn't an immutable descriptor, just the shared one.
	override fun immutable(): SetTypeDescriptor = shared

	override fun shared(): SetTypeDescriptor = shared

	companion object
	{
		/**
		 * Create a set type with the given range of sizes and content type.
		 * Normalize it certain ways:
		 *
		 *
		 * * An enumeration for the size range is weakened to a kind.
		 * * A ⊥ content type implies exactly zero elements, or ⊥ as the
		 *   resulting set type if zero is not an allowed size.
		 * * At most zero elements implies a ⊥ content type.
		 * * A non-meta enumeration for the content bounds the maximum size of
		 *   the set (e.g., a set of booleans has at most 2 elements).
		 * * Similarly, an integral range for the content type bounds the
		 *   maximum size of the set (you can't have a 1000-element set of
		 *   bytes).
		 *
		 *
		 * @param sizeRange
		 *   The allowed sizes of my instances.
		 * @param contentType
		 *   The type that constrains my instances' elements.
		 * @return
		 *   An immutable set type as specified.
		 */
		fun setTypeForSizesContentType(
			sizeRange: A_Type,
			contentType: A_Type): A_Type
		{
			if (sizeRange.isBottom)
			{
				return bottom
			}
			assert(sizeRange.lowerBound.isFinite)
			assert(zero.lessOrEqual(sizeRange.lowerBound))
			assert(sizeRange.upperBound.isFinite
					|| !sizeRange.upperInclusive)
			val sizeRangeKind =
				if (sizeRange.isEnumeration) sizeRange.computeSuperkind()
				else sizeRange
			val newSizeRange: A_Type
			val newContentType: A_Type
			when
			{
				sizeRangeKind.upperBound.equalsInt(0) ->
				{
					newSizeRange = sizeRangeKind
					newContentType = bottom
				}
				contentType.isBottom ->
				{
					if (sizeRangeKind.lowerBound.equalsInt(0))
					{
						// sizeRange includes at least 0 and 1, but the content
						// type is bottom, so no contents exist.
						newSizeRange = singleInteger(zero)
						newContentType = bottom
					}
					else
					{
						// sizeRange does not include 0, and bottom is not the
						// content type, so the whole type is inconsistent.
						// Answer bottom.
						return bottom
					}
				}
				else ->
				{
					val contentRestrictedSizes: A_Type =
						when
						{
							contentType.isEnumeration
								&& !contentType.isInstanceMeta ->
							{
								// There can't ever be more elements in the set
								// than there are distinct possible values.
								inclusive(zero, contentType.instanceCount)
							}
							contentType.isIntegerRangeType
							&& (contentType.lowerBound.isFinite
								|| contentType.upperBound.isFinite
								|| contentType.lowerBound.equals(
								contentType.upperBound)) ->
							{
								// We had already ruled out ⊥, and the latest
								// test rules out [-∞..∞], [-∞..∞), (-∞..∞], and
								// (-∞..∞), allowing safe subtraction.
								inclusive(
									zero,
									contentType.upperBound.minusCanDestroy(
										contentType.lowerBound, false)
										.plusCanDestroy(one, false))
							}
							else ->
							{
								// Otherwise don't narrow the size range.
								wholeNumbers
							}
						}
					newSizeRange = sizeRangeKind.typeIntersection(
						contentRestrictedSizes)
					newContentType = contentType
				}
			}
			return mutable.createShared {
				setSlot(SIZE_RANGE, newSizeRange)
				setSlot(CONTENT_TYPE, newContentType)
			}
		}

		/** The mutable [SetTypeDescriptor]. */
		private val mutable = SetTypeDescriptor(Mutability.MUTABLE)

		/** The shared [SetTypeDescriptor]. */
		private val shared = SetTypeDescriptor(Mutability.SHARED)

		/** The most general set type. */
		private val mostGeneralType: A_Type =
			setTypeForSizesContentType(wholeNumbers, ANY.o)
				.makeShared()

		/**
		 * Answer the most general set type.
		 *
		 * @return T
		 *   The most general set type.
		 */
		fun mostGeneralSetType(): A_Type = mostGeneralType

		/**
		 * The metatype for all set types.
		 */
		private val meta = instanceMeta(mostGeneralType)

		/**
		 * Answer the metatype for all set types.
		 *
		 * @return
		 *   The statically referenced metatype.
		 */
		fun setMeta(): A_Type = meta
	}
}
