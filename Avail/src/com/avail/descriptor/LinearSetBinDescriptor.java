/**
 * LinearSetBinDescriptor.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
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

import static com.avail.descriptor.LinearSetBinDescriptor.ObjectSlots.*;
import static com.avail.descriptor.LinearSetBinDescriptor.IntegerSlots.*;
import static com.avail.descriptor.Mutability.*;
import static java.lang.Integer.bitCount;
import com.avail.annotations.*;
import com.avail.descriptor.SetDescriptor.SetIterator;

/**
 * A {@code LinearSetBinDescriptor} is a leaf bin in a {@link SetDescriptor
 * set}'s hierarchy of bins.  It consists of a small number of distinct elements
 * in no particular order.  If more elements need to be stored, a {@linkplain
 * HashedSetBinDescriptor hashed bin} will be used instead.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class LinearSetBinDescriptor
extends SetBinDescriptor
{
	/**
	 * The layout of integer slots for my instances.
	 */
	public enum IntegerSlots
	implements IntegerSlotsEnum
	{
		/**
		 * The sum of the hashes of the elements within this bin.
		 */
		BIN_HASH;

		static
		{
			assert SetBinDescriptor.IntegerSlots.BIN_HASH.ordinal()
				== BIN_HASH.ordinal();
		}
	}

	/**
	 * The layout of object slots for my instances.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/**
		 * The elements of this bin.  The elements are never sub-bins, since
		 * this is a {@linkplain LinearSetBinDescriptor linear bin}, a leaf bin.
		 */
		BIN_ELEMENT_AT_
	}

	@Override @AvailMethod
	AvailObject o_BinElementAt (final AvailObject object, final int subscript)
	{
		return object.slot(BIN_ELEMENT_AT_, subscript);
	}

	@Override @AvailMethod
	void o_BinElementAtPut (
		final AvailObject object,
		final int subscript,
		final A_BasicObject value)
	{
		object.setSlot(BIN_ELEMENT_AT_, subscript, value);
	}

	@Override @AvailMethod
	A_BasicObject o_SetBinAddingElementHashLevelCanDestroy (
		final AvailObject object,
		final A_BasicObject elementObject,
		final int elementObjectHash,
		final byte myLevel,
		final boolean canDestroy)
	{
		// Add the given element to this bin, potentially modifying it if
		// canDestroy and it's mutable.  Answer the new bin.  Note that the
		// client is responsible for marking elementObject as immutable if
		// another reference exists.
		assert myLevel == level;
		if (object.binHasElementWithHash(elementObject, elementObjectHash))
		{
			if (!canDestroy)
			{
				object.makeImmutable();
			}
			return object;
		}
		// It's not present, so grow the list.  Keep it simple for now by always
		// replacing the list.
		final int oldSize = object.variableObjectSlotsCount();
		AvailObject result;
		if (myLevel < numberOfLevels - 1 && oldSize >= 10)
		{
			int bitPosition = bitShift(elementObjectHash, -5 * myLevel) & 31;
			int bitVector = bitShift(1, bitPosition);
			for (int i = 1; i <= oldSize; i++)
			{
				final A_BasicObject element = object.slot(BIN_ELEMENT_AT_, i);
				bitPosition = bitShift(element.hash(), -5 * myLevel) & 31;
				bitVector |= bitShift(1, bitPosition);
			}
			final int newSize = bitCount(bitVector);
			result = HashedSetBinDescriptor.createBin(
				myLevel,
				newSize,
				0,
				0,
				bitVector,
				NilDescriptor.nil());
			final AvailObject emptySubBin =
				emptyBinForLevel((byte) (myLevel + 1));
			for (int i = 1; i <= newSize; i++)
			{
				result.setSlot(
					HashedSetBinDescriptor.ObjectSlots.BIN_ELEMENT_AT_,
					i,
					emptySubBin);
			}
			A_BasicObject localAddResult;
			for (int i = 0; i <= oldSize; i++)
			{
				final A_BasicObject eachElement;
				final int eachHash;
				if (i == 0)
				{
					eachElement = elementObject;
					eachHash = elementObjectHash;
				}
				else
				{
					eachElement = object.slot(BIN_ELEMENT_AT_, i);
					eachHash = eachElement.hash();
				}
				assert result.descriptor().isMutable();
				localAddResult = result.setBinAddingElementHashLevelCanDestroy(
					eachElement,
					eachHash,
					myLevel,
					true);
				assert localAddResult.sameAddressAs(result)
				: "The element should have been added without reallocation";
			}
			final int newHash = object.binHash() + elementObjectHash;
			assert result.binHash() == newHash;
			assert result.binSize() == oldSize + 1;
			return result;
		}
		// Make a slightly larger linear bin and populate it.
		result = LinearSetBinDescriptor.createBin(
			myLevel,
			oldSize + 1,
			object.binHash() + elementObjectHash);
		result.setSlot(BIN_ELEMENT_AT_, oldSize + 1, elementObject);
		if (canDestroy && isMutable())
		{
			for (int i = 1; i <= oldSize; i++)
			{
				result.setSlot(
					BIN_ELEMENT_AT_,
					i,
					object.slot(BIN_ELEMENT_AT_, i));
				// Clear old slot for safety.
				object.setSlot(BIN_ELEMENT_AT_, i, NilDescriptor.nil());
			}
		}
		else if (isMutable())
		{
			for (int i = 1; i <= oldSize; i++)
			{
				result.setSlot(
					BIN_ELEMENT_AT_,
					i,
					object.slot(BIN_ELEMENT_AT_, i).makeImmutable());
			}
		}
		else
		{
			for (int i = 1; i <= oldSize; i++)
			{
				result.setSlot(
					BIN_ELEMENT_AT_,
					i,
					object.slot(BIN_ELEMENT_AT_, i));
			}
		}
		return result;
	}

	@Override @AvailMethod
	boolean o_BinHasElementWithHash (
		final AvailObject object,
		final A_BasicObject elementObject,
		final int elementObjectHash)
	{
		final int limit = object.variableObjectSlotsCount();
		for (int i = 1; i <= limit; i++)
		{
			if (elementObject.equals(object.slot(BIN_ELEMENT_AT_, i)))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove elementObject from the bin object, if present. Answer the
	 * resulting bin. The bin may be modified if it's mutable and canDestroy.
	 */
	@Override @AvailMethod
	AvailObject o_BinRemoveElementHashLevelCanDestroy (
		final AvailObject object,
		final A_BasicObject elementObject,
		final int elementObjectHash,
		final byte myLevel,
		final boolean canDestroy)
	{
		assert level == myLevel;
		final int oldSize = object.variableObjectSlotsCount();
		for (int searchIndex = 1; searchIndex <= oldSize; searchIndex++)
		{
			if (object.slot(BIN_ELEMENT_AT_, searchIndex).equals(elementObject))
			{
				if (oldSize == 2)
				{
					final AvailObject survivor =
						object.slot(BIN_ELEMENT_AT_, 3 - searchIndex);
					if (!canDestroy)
					{
						survivor.makeImmutable();
					}
					return survivor;
				}
				final AvailObject result = LinearSetBinDescriptor.createBin(
					level,
					oldSize - 1,
					object.binHash() - elementObjectHash);
				for (int copyIndex = 1; copyIndex < searchIndex; copyIndex++)
				{
					result.setSlot(
						BIN_ELEMENT_AT_,
						copyIndex,
						object.slot(BIN_ELEMENT_AT_, copyIndex));
				}
				for (
					int copyIndex = searchIndex + 1;
					copyIndex <= oldSize;
					copyIndex++)
				{
					result.setSlot(
						BIN_ELEMENT_AT_,
						copyIndex - 1,
						object.slot(BIN_ELEMENT_AT_, copyIndex));
				}
				if (!canDestroy)
				{
					result.makeSubobjectsImmutable();
				}
				return result;
			}
		}
		if (!canDestroy)
		{
			object.makeImmutable();
		}
		return object;
	}

	@Override @AvailMethod
	boolean o_IsBinSubsetOf (
		final AvailObject object,
		final A_Set potentialSuperset)
	{
		// Check if object, a bin, holds a subset of aSet's elements.
		for (
			int physicalIndex = object.variableObjectSlotsCount();
			physicalIndex >= 1;
			physicalIndex--)
		{
			if (!object.slot(BIN_ELEMENT_AT_, physicalIndex).isBinSubsetOf(
				potentialSuperset))
			{
				return false;
			}
		}
		return true;
	}

	@Override @AvailMethod
	int o_BinSize (final AvailObject object)
	{
		// Answer how many elements this bin contains.
		return object.variableObjectSlotsCount();
	}

	@Override @AvailMethod
	A_Type o_BinUnionKind (final AvailObject object)
	{
		// Answer the nearest kind of the union of the types of this bin's
		// elements. I'm supposed to be small, so recalculate it per request.
		A_Type unionKind = object.slot(BIN_ELEMENT_AT_, 1).kind();
		final int limit = object.variableObjectSlotsCount();
		for (int index = 2; index <= limit; index++)
		{
			unionKind = unionKind.typeUnion(
				object.slot(BIN_ELEMENT_AT_, index).kind());
		}
		return unionKind;
	}

	@Override @AvailMethod
	boolean o_BinElementsAreAllInstancesOfKind (
		final AvailObject object,
		final A_Type kind)
	{
		final int limit = object.variableObjectSlotsCount();
		for (int index = 1; index <= limit; index++)
		{
			if (!object.slot(BIN_ELEMENT_AT_, index).isInstanceOfKind(kind))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	SetIterator o_SetBinIterator (final AvailObject object)
	{
		return new SetIterator()
		{
			final int limit = object.variableObjectSlotsCount();

			int index = 1;

			@Override
			public AvailObject next ()
			{
				assert index <= limit;
				return object.binElementAt(index++);
			}

			@Override
			public boolean hasNext ()
			{
				return index <= limit;
			}
		};
	}

	/**
	 * Create a mutable linear bin at the specified level with the given size
	 * and bin hash. The caller is responsible for initializing the elements
	 * and making them immutable if necessary.
	 *
	 * @param level The level of the new bin.
	 * @param size The number of elements in the new bin.
	 * @param hash The hash of the bin's elements, or zero if unknown.
	 * @return A new linear set bin with uninitialized element slots.
	 */
	public static AvailObject createBin (
		final byte level,
		final int size,
		final int hash)
	{
		final AvailObject instance = descriptorFor(MUTABLE, level).create(size);
		instance.setSlot(BIN_HASH, hash);
		return instance;
	}

	/**
	 * Create a mutable 2-element linear bin at the specified level and with the
	 * specified elements. The caller is responsible for making the elements
	 * immutable if necessary.
	 *
	 * @param level The level of the new bin.
	 * @param firstElement The first element of the new bin.
	 * @param secondElement The second element of the new bin.
	 * @return A 2-element set bin.
	 */
	public static AvailObject createPair (
		final byte level,
		final A_BasicObject firstElement,
		final A_BasicObject secondElement)
	{
		final AvailObject instance = descriptorFor(MUTABLE, level).create(2);
		instance.setSlot(BIN_ELEMENT_AT_, 1, firstElement);
		instance.setSlot(BIN_ELEMENT_AT_, 2, secondElement);
		instance.setSlot(BIN_HASH, firstElement.hash() + secondElement.hash());
		return instance;
	}

	/**
	 * The number of distinct levels at which {@linkplain LinearSetBinDescriptor
	 * linear bins} may occur.
	 */
	static final byte numberOfLevels = 7;

	/**
	 * Answer a suitable descriptor for a linear bin with the specified
	 * mutability and at the specified level.
	 *
	 * @param flag The desired {@linkplain Mutability mutability}.
	 * @param level The level for the bins using the descriptor.
	 * @return The descriptor with the requested properties.
	 */
	private static LinearSetBinDescriptor descriptorFor (
		final Mutability flag,
		final byte level)
	{
		assert 0 <= level && level < numberOfLevels;
		return descriptors[level * 3 + flag.ordinal()];
	}

	/**
	 * Construct a new {@link LinearSetBinDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 * @param level
	 *        The depth of the bin in the hash tree.
	 */
	private LinearSetBinDescriptor (
		final Mutability mutability,
		final int level)
	{
		super(mutability, ObjectSlots.class, IntegerSlots.class, level);
	}

	/**
	 * {@link LinearSetBinDescriptor}s clustered by mutability and level.
	 */
	static final LinearSetBinDescriptor[] descriptors;

	static
	{
		descriptors = new LinearSetBinDescriptor[numberOfLevels * 3];
		int target = 0;
		for (int level = 0; level < numberOfLevels; level++)
		{
			descriptors[target++] =
				new LinearSetBinDescriptor(MUTABLE, level);
			descriptors[target++] =
				new LinearSetBinDescriptor(IMMUTABLE, level);
			descriptors[target++] =
				new LinearSetBinDescriptor(SHARED, level);
		}
	}

	@Override
	LinearSetBinDescriptor mutable ()
	{
		return descriptorFor(MUTABLE, level);
	}

	@Override
	LinearSetBinDescriptor immutable ()
	{
		return descriptorFor(IMMUTABLE, level);
	}

	@Override
	LinearSetBinDescriptor shared ()
	{
		return descriptorFor(SHARED, level);
	}

	/**
	 * The canonical array of empty bins, one for each level.
	 */
	private final static AvailObject [] emptyBins =
		new AvailObject [numberOfLevels];

	static
	{
		for (int i = 0; i < numberOfLevels; i++)
		{
			final AvailObject bin = createBin((byte) i, 0, 0);
			bin.makeShared();
			emptyBins[i] = bin;
		}
	}

	/**
	 * Answer an empty bin for the specified level.
	 *
	 * @param level The level at which this bin occurs.
	 * @return An empty bin.
	 */
	final static AvailObject emptyBinForLevel (final byte level)
	{
		return emptyBins[level];
	}
}
