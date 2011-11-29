/**
 * descriptor/ByteTupleDescriptor.java
 * Copyright (c) 2010, Mark van Gulik.
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

package com.avail.descriptor;

import static com.avail.descriptor.AvailObject.Multiplier;
import static com.avail.descriptor.TypeDescriptor.Types.*;
import static java.lang.Math.*;
import java.util.List;
import com.avail.annotations.*;

/**
 * {@code ByteTupleDescriptor} represents a tuple of integers that happen to
 * fall in the range 0..255.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
public class ByteTupleDescriptor
extends TupleDescriptor
{
	/**
	 * The layout of integer slots for my instances.
	 */
	public enum IntegerSlots implements IntegerSlotsEnum
	{
		/**
		 * The hash, or zero ({@code 0}) if the hash has not yet been computed.
		 */
		HASH_OR_ZERO,

		/**
		 * The raw 32-bit machine words that constitute the representation of
		 * the {@linkplain ByteTupleDescriptor byte tuple}.
		 */
		RAW_QUAD_AT_
	}

	/**
	 * The number of bytes of the last {@code int} that do not participate in
	 * the representation of the {@linkplain ByteTupleDescriptor byte tuple}.
	 * Must be between 0 and 3.
	 */
	private final int unusedBytesOfLastWord;

	@Override
	public void printObjectOnAvoidingIndent (
		final @NotNull AvailObject object,
		final @NotNull StringBuilder aStream,
		final @NotNull List<AvailObject> recursionList,
		final int indent)
	{
		if (isMutable())
		{
			aStream.append("(mut) ");
		}
		aStream.append("ByteTuple with: #[");
		int rowSize = max(30 - indent * 3 / 2, 8);
		rowSize = rowSize + 3 & ~3;
		//  How many equal (shorter by at least 1 on the last) rows are needed?
		final int rows = (object.tupleSize() + rowSize) / rowSize;
		//  How many on each row for that layout?
		rowSize = (object.tupleSize() + rows) / rows;
		//  Round up to a multiple of four per row.
		rowSize = rowSize + 3 & ~3;
		int rowStart = 1;
		while (rowStart <= object.tupleSize())
		{
			aStream.append('\n');
			for (int count = 1; count <= indent; count++)
			{
				aStream.append('\t');
			}
			for (
					int
						i = rowStart,
						end = min(rowStart + rowSize - 1, object.tupleSize());
					i <= end;
					i++)
			{
				final short val = object.tupleAt(i).extractByte();
				aStream.append(Integer.toHexString(val >> 4));
				aStream.append(Integer.toHexString(val & 15));
				if (i % 4 == 0)
				{
					aStream.append(' ');
				}
				aStream.append(' ');
			}
			rowStart += rowSize;
		}
		aStream.append(']');
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final @NotNull AvailObject anotherObject,
		final int startIndex2)
	{
		//  Compare sections of two tuples.

		return anotherObject.compareFromToWithByteTupleStartingAt(
			startIndex2,
			startIndex2 + endIndex1 - startIndex1,
			object,
			startIndex1);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithByteTupleStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final @NotNull AvailObject aByteTuple,
		final int startIndex2)
	{
		//  Compare sections of two byte tuples.

		if (object.sameAddressAs(aByteTuple) && startIndex1 == startIndex2)
		{
			return true;
		}
		//  Compare actual bytes.
		int index2 = startIndex2;
		for (int index1 = startIndex1; index1 <= endIndex1; index1++)
		{
			if (object.rawByteAt(index1) != aByteTuple.rawByteAt(index2))
			{
				return false;
			}
			index2++;
		}
		return true;
	}

	@Override @AvailMethod
	boolean o_Equals (
		final @NotNull AvailObject object,
		final @NotNull AvailObject another)
	{
		return another.equalsByteTuple(object);
	}

	@Override @AvailMethod
	boolean o_EqualsByteTuple (
		final @NotNull AvailObject object,
		final @NotNull AvailObject aByteTuple)
	{
		// First, check for object-structure (address) identity.
		if (object.sameAddressAs(aByteTuple))
		{
			return true;
		}
		if (object.tupleSize() != aByteTuple.tupleSize())
		{
			return false;
		}
		if (object.hash() != aByteTuple.hash())
		{
			return false;
		}
		if (!object.compareFromToWithByteTupleStartingAt(
			1,
			object.tupleSize(),
			aByteTuple,
			1))
		{
			return false;
		}
		// They're equal (but occupy disjoint storage).  Replace one with an
		// indirection to the other to keep down the frequency of byte-wise
		// comparisons.
		object.becomeIndirectionTo(aByteTuple);
		// There are now at least two references to it.
		aByteTuple.makeImmutable();
		return true;
	}

	@Override @AvailMethod
	boolean o_IsByteTuple (final @NotNull AvailObject object)
	{
		return true;
	}

	@Override @AvailMethod
	boolean o_IsInstanceOfKind (
		final @NotNull AvailObject object,
		final @NotNull AvailObject aType)
	{
		if (aType.equals(TOP.o()))
		{
			return true;
		}
		if (aType.equals(ANY.o()))
		{
			return true;
		}
		if (!aType.isTupleType())
		{
			return false;
		}
		//  See if it's an acceptable size...
		final AvailObject size = IntegerDescriptor.fromInt(object.tupleSize());
		if (!size.isInstanceOf(aType.sizeRange()))
		{
			return false;
		}
		//  tuple's size is out of range.
		final AvailObject typeTuple = aType.typeTuple();
		final int breakIndex = min(object.tupleSize(), typeTuple.tupleSize());
		for (int i = 1; i <= breakIndex; i++)
		{
			if (!object.tupleAt(i).isInstanceOf(aType.typeAtIndex(i)))
			{
				return false;
			}
		}
		final AvailObject defaultTypeObject = aType.defaultType();
		if (IntegerRangeTypeDescriptor.bytes().isSubtypeOf(defaultTypeObject))
		{
			return true;
		}
		for (int i = breakIndex + 1, end = object.tupleSize(); i <= end; i++)
		{
			if (!object.tupleAt(i).isInstanceOf(defaultTypeObject))
			{
				return false;
			}
		}
		return true;
	}

	@Override @AvailMethod
	@NotNull AvailObject o_MakeImmutable (
		final @NotNull AvailObject object)
	{
		//  Make the object immutable so it can be shared safely.

		if (isMutable)
		{
			object.descriptor = isMutableSize(false, object.tupleSize());
		}
		return object;
	}

	@Override @AvailMethod
	short o_RawByteAt (
		final @NotNull AvailObject object,
		final int index)
	{
		//  Answer the byte at the given index.
		assert index >= 1 && index <= object.tupleSize();
		return object.byteSlotAt(IntegerSlots.RAW_QUAD_AT_, index);
	}

	@Override @AvailMethod
	void o_RawByteAtPut (
		final @NotNull AvailObject object,
		final int index,
		final short anInteger)
	{
		//  Set the byte at the given index.
		assert index >= 1 && index <= object.tupleSize();
		object.byteSlotAtPut(IntegerSlots.RAW_QUAD_AT_, index, anInteger);
	}

	@Override @AvailMethod
	@NotNull AvailObject o_TupleAt (
		final @NotNull AvailObject object,
		final int index)
	{
		//  Answer the element at the given index in the tuple object.
		assert index >= 1 && index <= object.tupleSize();
		return IntegerDescriptor.fromUnsignedByte(
			object.byteSlotAt(IntegerSlots.RAW_QUAD_AT_, index));
	}

	@Override @AvailMethod
	void o_TupleAtPut (
		final @NotNull AvailObject object,
		final int index,
		final @NotNull AvailObject aByteObject)
	{
		// Set the byte at the given index to the given object (which should be
		// an AvailObject that's an integer 0<=n<=255.
		assert index >= 1 && index <= object.tupleSize();
		final short theByte = aByteObject.extractByte();
		object.byteSlotAtPut(IntegerSlots.RAW_QUAD_AT_, index, theByte);
	}

	@Override @AvailMethod
	@NotNull AvailObject o_TupleAtPuttingCanDestroy (
		final @NotNull AvailObject object,
		final int index,
		final @NotNull AvailObject newValueObject,
		final boolean canDestroy)
	{
		// Answer a tuple with all the elements of object except at the given
		// index we should have newValueObject.  This may destroy the original
		// tuple if canDestroy is true.

		assert index >= 1 && index <= object.tupleSize();
		if (!newValueObject.isByte())
		{
			return object.copyAsMutableObjectTuple().tupleAtPuttingCanDestroy(
				index,
				newValueObject,
				true);
		}
		if (!canDestroy || !isMutable)
		{
			return copyAsMutableByteTuple(object).tupleAtPuttingCanDestroy(
				index,
				newValueObject,
				true);
		}
		// Clobber the object in place...
		object.rawByteAtPut(index, newValueObject.extractByte());
		object.hashOrZero(0);
		//  ...invalidate the hash value.
		return object;
	}

	@Override @AvailMethod
	int o_TupleIntAt (
		final @NotNull AvailObject object,
		final int index)
	{
		//  Answer the integer element at the given index in the tuple object.

		return object.byteSlotAt(IntegerSlots.RAW_QUAD_AT_, index);
	}

	@Override @AvailMethod
	int o_BitsPerEntry (
		final @NotNull AvailObject object)
	{
		//  Answer approximately how many bits per entry are taken up by this object.

		return 8;
	}

	@Override @AvailMethod
	int o_ComputeHashFromTo (
		final @NotNull AvailObject object,
		final int start,
		final int end)
	{
		//  See comment in superclass.  This method must produce the same value.

		int hash = 0;
		for (int index = end; index >= start; index--)
		{
			final int itemHash = IntegerDescriptor.hashOfUnsignedByte(
				object.rawByteAt(index)) ^ PreToggle;
			hash = hash * Multiplier + itemHash;
		}
		return hash * Multiplier;
	}

	/**
	 * Answer a mutable copy of object that also only holds bytes.
	 *
	 * @param object The byte tuple to copy.
	 * @return The new mutable byte tuple.
	 */
	private @NotNull AvailObject copyAsMutableByteTuple (
		final @NotNull AvailObject object)
	{
		final AvailObject result = mutableObjectOfSize(object.tupleSize());
		assert result.integerSlotsCount() == object.integerSlotsCount();
		result.hashOrZero(object.hashOrZero());
		// Copy four bytes at a time.
		for (int i = 1, end = object.tupleSize() + 3 >> 2; i <= end; i++)
		{
			result.integerSlotAtPut(
				IntegerSlots.RAW_QUAD_AT_,
				i,
				object.integerSlotAt(IntegerSlots.RAW_QUAD_AT_, i));
		}
		return result;
	}

	@Override @AvailMethod
	int o_TupleSize (
		final @NotNull AvailObject object)
	{
		return object.variableIntegerSlotsCount() * 4 - unusedBytesOfLastWord;
	}

	/**
	 * Build a mutable {@link ByteTupleDescriptor byte tuple} with room for the
	 * specified number of elements.
	 *
	 * @param size The number of bytes in the resulting tuple.
	 * @return A byte tuple with the specified number of bytes (initially zero).
	 */
	public static @NotNull AvailObject mutableObjectOfSize (
		final int size)
	{
		final ByteTupleDescriptor descriptor = isMutableSize(true, size);
		assert (size + descriptor.unusedBytesOfLastWord & 3) == 0;
		return descriptor.create(size + 3 >> 2);
	}

	/**
	 * Obtain the {@link ByteTupleDescriptor} that corresponds to the specified
	 * mutability flag and size.
	 *
	 * @param flag {@code true} if the desired descriptor is mutable.
	 * @param size The number of bytes in conformant tuples.
	 * @return A {@code ByteTupleDescriptor}.
	 */
	private static @NotNull ByteTupleDescriptor isMutableSize (
		final boolean flag,
		final int size)
	{
		final int delta = flag ? 0 : 1;
		return descriptors[delta + (size & 3) * 2];
	}

	/**
	 * Construct a new {@link ByteTupleDescriptor}.
	 *
	 * @param isMutable
	 *        Does the {@linkplain Descriptor descriptor} represent a mutable
	 *        object?
	 * @param unusedBytes The number of unused bytes of the last word.
	 */
	protected ByteTupleDescriptor (
		final boolean isMutable,
		final int unusedBytes)
	{
		super(isMutable);
		unusedBytesOfLastWord = unusedBytes;
	}

	/** {@link ByteTupleDescriptor}s corresponding to different sizes. */
	private final static ByteTupleDescriptor[] descriptors =
	{
		new ByteTupleDescriptor(true, 0),
		new ByteTupleDescriptor(false, 0),
		new ByteTupleDescriptor(true, 3),
		new ByteTupleDescriptor(false, 3),
		new ByteTupleDescriptor(true, 2),
		new ByteTupleDescriptor(false, 2),
		new ByteTupleDescriptor(true, 1),
		new ByteTupleDescriptor(false, 1)
	};

}
