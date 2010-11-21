/**
 * descriptor/Descriptor.java
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

import com.avail.annotations.NotNull;
import com.avail.compiler.Continuation1;
import com.avail.compiler.Generator;
import com.avail.descriptor.VoidDescriptor;
import com.avail.interpreter.AvailInterpreter;
import com.avail.visitor.AvailBeImmutableSubobjectVisitor;
import com.avail.visitor.AvailSubobjectVisitor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static com.avail.descriptor.AvailObject.*;

public abstract class Descriptor extends AbstractDescriptor
{
	/**
	 * Construct a new {@link Descriptor}.
	 *
	 * @param isMutable
	 */
	public Descriptor (boolean isMutable)
	{
		super(isMutable);
	}

	
	@Override
	public boolean ObjectAcceptsArgTypesFromClosureType (
			final AvailObject object, 
			final AvailObject closureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsArgTypesFromClosureType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param continuation
	 * @param stackp
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsArgumentsFromContinuationStackp (
			final AvailObject object, 
			final AvailObject continuation, 
			final int stackp)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsArgumentsFromContinuation:stackp: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param continuation
	 * @param stackp
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsArgumentTypesFromContinuationStackp (
			final AvailObject object, 
			final AvailObject continuation, 
			final int stackp)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsArgumentTypesFromContinuation:stackp: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsArrayOfArgTypes (
			final AvailObject object, 
			final List<AvailObject> argTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsArrayOfArgTypes: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param argValues
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsArrayOfArgValues (
			final AvailObject object, 
			final List<AvailObject> argValues)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsArrayOfArgValues: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsTupleOfArgTypes (
			final AvailObject object, 
			final AvailObject argTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsTupleOfArgTypes: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param arguments
	 * @return
	 */
	@Override
	public boolean ObjectAcceptsTupleOfArguments (
			final AvailObject object, 
			final AvailObject arguments)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:acceptsTupleOfArguments: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aChunkIndex
	 */
	@Override
	public void ObjectAddDependentChunkId (
			final AvailObject object, 
			final int aChunkIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:addDependentChunkId: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param implementation
	 */
	@Override
	public void ObjectAddImplementation (
			final AvailObject object, 
			final AvailObject implementation)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:addImplementation: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param restrictions
	 */
	@Override
	public void ObjectAddRestrictions (
			final AvailObject object, 
			final AvailObject restrictions)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:addRestrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param anInfinity
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectAddToInfinityCanDestroy (
			final AvailObject object, 
			final AvailObject anInfinity, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:addToInfinity:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInteger
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectAddToIntegerCanDestroy (
			final AvailObject object, 
			final AvailObject anInteger, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:addToInteger:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param args
	 * @param locals
	 * @param stack
	 * @param outers
	 * @param primitive
	 */
	@Override
	public void ObjectArgsLocalsStackOutersPrimitive (
			final AvailObject object, 
			final int args, 
			final int locals, 
			final int stack, 
			final int outers, 
			final int primitive)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:args:locals:stack:outers:primitive: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectArgTypeAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:argTypeAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectArgTypeAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:argTypeAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param methodName
	 * @param illegalArgMsgs
	 */
	@Override
	public void ObjectAtAddMessageRestrictions (
			final AvailObject object, 
			final AvailObject methodName, 
			final AvailObject illegalArgMsgs)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:at:addMessageRestrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param methodName
	 * @param implementation
	 */
	@Override
	public void ObjectAtAddMethodImplementation (
			final AvailObject object, 
			final AvailObject methodName, 
			final AvailObject implementation)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:at:addMethodImplementation: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param message
	 * @param bundle
	 */
	@Override
	public void ObjectAtMessageAddBundle (
			final AvailObject object, 
			final AvailObject message, 
			final AvailObject bundle)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:atMessage:addBundle: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param stringName
	 * @param trueName
	 */
	@Override
	public void ObjectAtNameAdd (
			final AvailObject object, 
			final AvailObject stringName, 
			final AvailObject trueName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:atName:add: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param stringName
	 * @param trueName
	 */
	@Override
	public void ObjectAtNewNamePut (
			final AvailObject object, 
			final AvailObject stringName, 
			final AvailObject trueName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:atNewName:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param stringName
	 * @param trueName
	 */
	@Override
	public void ObjectAtPrivateNameAdd (
			final AvailObject object, 
			final AvailObject stringName, 
			final AvailObject trueName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:atPrivateName:add: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectBinElementAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:binElementAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectBinElementAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:binElementAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBinHash (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:binHash: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBinSize (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:binSize: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBinUnionType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:binUnionType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBitVector (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bitVector: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBodyBlock (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bodyBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param bb
	 * @param rqb
	 * @param rtb
	 */
	@Override
	public void ObjectBodyBlockRequiresBlockReturnsBlock (
			final AvailObject object, 
			final AvailObject bb, 
			final AvailObject rqb, 
			final AvailObject rtb)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bodyBlock:requiresBlock:returnsBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param signature
	 */
	@Override
	public void ObjectBodySignature (
			final AvailObject object, 
			final AvailObject signature)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bodySignature: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param bs
	 * @param rqb
	 * @param rtb
	 */
	@Override
	public void ObjectBodySignatureRequiresBlockReturnsBlock (
			final AvailObject object, 
			final AvailObject bs, 
			final AvailObject rqb, 
			final AvailObject rtb)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bodySignature:requiresBlock:returnsBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectBreakpointBlock (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:breakpointBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param bundleTree
	 */
	@Override
	public void ObjectBuildFilteredBundleTreeFrom (
			final AvailObject object, 
			final AvailObject bundleTree)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:buildFilteredBundleTreeFrom: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param message
	 * @param parts
	 * @return
	 */
	@Override
	public AvailObject ObjectBundleAtMessageParts (
			final AvailObject object, 
			final AvailObject message, 
			final AvailObject parts)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:bundleAtMessage:parts: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectCaller (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:caller: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectClosure (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:closure: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectClosureType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:closureType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectCode (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:code: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectCodePoint (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:codePoint: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param anotherObject
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject anotherObject, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:with:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param aTuple
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithAnyTupleStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aTuple, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withAnyTuple:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param aByteString
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithByteStringStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aByteString, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withByteString:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param aByteTuple
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithByteTupleStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aByteTuple, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withByteTuple:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param aNybbleTuple
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithNybbleTupleStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aNybbleTuple, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withNybbleTuple:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param anObjectTuple
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithObjectTupleStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject anObjectTuple, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withObjectTuple:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param startIndex1
	 * @param endIndex1
	 * @param aTwoByteString
	 * @param startIndex2
	 * @return
	 */
	@Override
	public boolean ObjectCompareFromToWithTwoByteStringStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aTwoByteString, 
			final int startIndex2)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:compareFrom:to:withTwoByteString:startingAt: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectComplete (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:complete: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param start
	 * @param end
	 * @return
	 */
	@Override
	public int ObjectComputeHashFromTo (
			final AvailObject object, 
			final int start, 
			final int end)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:computeHashFrom:to: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @param anAvailInterpreter
	 * @return
	 */
	@Override
	public AvailObject ObjectComputeReturnTypeFromArgumentTypesInterpreter (
			final AvailObject object, 
			final List<AvailObject> argTypes, 
			final AvailInterpreter anAvailInterpreter)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:computeReturnTypeFromArgumentTypes:interpreter: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectConcatenateTuplesCanDestroy (
			final AvailObject object, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:concatenateTuplesCanDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectConstantBindings (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:constantBindings: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectContentType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:contentType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectContingentImpSets (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:contingentImpSets: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectContinuation (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:continuation: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param filteredBundleTree
	 * @param visibleNames
	 */
	@Override
	public void ObjectCopyToRestrictedTo (
			final AvailObject object, 
			final AvailObject filteredBundleTree, 
			final AvailObject visibleNames)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:copyTo:restrictedTo: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param start
	 * @param end
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectCopyTupleFromToCanDestroy (
			final AvailObject object, 
			final int start, 
			final int end, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:copyTupleFrom:to:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param argTypes
	 * @return
	 */
	@Override
	public boolean ObjectCouldEverBeInvokedWith (
			final AvailObject object, 
			final ArrayList<AvailObject> argTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:couldEverBeInvokedWith: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param positiveTuple
	 * @param possibilities
	 * @return
	 */
	@Override
	public AvailObject ObjectCreateTestingTreeWithPositiveMatchesRemainingPossibilities (
			final AvailObject object, 
			final AvailObject positiveTuple, 
			final AvailObject possibilities)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:createTestingTreeWithPositiveMatches:remainingPossibilities: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectDataAtIndex (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:dataAtIndex: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectDataAtIndexPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:dataAtIndex:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectDefaultType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:defaultType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectDependentChunks (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:dependentChunks: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectDepth (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:depth: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param aNumber
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectDivideCanDestroy (
			final AvailObject object, 
			final AvailObject aNumber, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:divide:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInfinity
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectDivideIntoInfinityCanDestroy (
			final AvailObject object, 
			final AvailObject anInfinity, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:divideIntoInfinity:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInteger
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectDivideIntoIntegerCanDestroy (
			final AvailObject object, 
			final AvailObject anInteger, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:divideIntoInteger:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectElementAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:elementAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectElementAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:elementAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public int ObjectEndOfZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:endOfZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public int ObjectEndSubtupleIndexInZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:endSubtupleIndexInZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectExecutionMode (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:executionMode: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectExecutionState (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:executionState: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public byte ObjectExtractNybbleFromTupleAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:extractNybbleFromTupleAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectFieldMap (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:fieldMap: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectFieldTypeMap (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:fieldTypeMap: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @return
	 */
	@Override
	public List<AvailObject> ObjectFilterByTypes (
			final AvailObject object, 
			final List<AvailObject> argTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:filterByTypes: in Avail.Descriptor", object);
		return null;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectFilteredBundleTree (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:filteredBundleTree: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectFirstTupleType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:firstTupleType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param zone
	 * @param newSubtuple
	 * @param startSubtupleIndex
	 * @param endOfZone
	 * @return
	 */
	@Override
	public AvailObject ObjectForZoneSetSubtupleStartSubtupleIndexEndOfZone (
			final AvailObject object, 
			final int zone, 
			final AvailObject newSubtuple, 
			final int startSubtupleIndex, 
			final int endOfZone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:forZone:setSubtuple:startSubtupleIndex:endOfZone: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectGreaterThanInteger (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:greaterThanInteger: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectGreaterThanSignedInfinity (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:greaterThanSignedInfinity: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param elementObject
	 * @return
	 */
	@Override
	public boolean ObjectHasElement (
			final AvailObject object, 
			final AvailObject elementObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hasElement: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHash (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hash: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	@Override
	public int ObjectHashFromTo (
			final AvailObject object, 
			final int startIndex, 
			final int endIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hashFrom:to: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHashOrZero (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hashOrZero: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param keyObject
	 * @return
	 */
	@Override
	public boolean ObjectHasKey (
			final AvailObject object, 
			final AvailObject keyObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hasKey: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHiLevelTwoChunkLowOffset (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hiLevelTwoChunkLowOffset: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHiNumLocalsLowNumArgs (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hiNumLocalsLowNumArgs: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHiPrimitiveLowNumArgsAndLocalsAndStack (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hiPrimitiveLowNumArgsAndLocalsAndStack: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectHiStartingChunkIndexLowNumOuters (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:hiStartingChunkIndexLowNumOuters: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @return
	 */
	@Override
	public ArrayList<AvailObject> ObjectImplementationsAtOrBelow (
			final AvailObject object, 
			final ArrayList<AvailObject> argTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:implementationsAtOrBelow: in Avail.Descriptor", object);
		return null;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectImplementationsTuple (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:implementationsTuple: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param message
	 * @param parts
	 * @return
	 */
	@Override
	public AvailObject ObjectIncludeBundleAtMessageParts (
			final AvailObject object, 
			final AvailObject message, 
			final AvailObject parts)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:includeBundleAtMessage:parts: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param imp
	 * @return
	 */
	@Override
	public boolean ObjectIncludes (
			final AvailObject object, 
			final AvailObject imp)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:includes: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInclusiveFlags (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:inclusiveFlags: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectIncomplete (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:incomplete: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectIndex (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:index: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInnerType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:innerType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInstance (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:instance: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInternalHash (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:internalHash: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInterruptRequestFlag (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:interruptRequestFlag: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectInvocationCount (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:invocationCount: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param aBoolean
	 */
	@Override
	public void ObjectIsSaved (
			final AvailObject object, 
			final boolean aBoolean)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSaved: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectIsSubsetOf (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSubsetOf: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectIsSubtypeOf (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSubtypeOf: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfClosureType (
			final AvailObject object, 
			final AvailObject aClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfClosureType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aContainerType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfContainerType (
			final AvailObject object, 
			final AvailObject aContainerType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfContainerType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aContinuationType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfContinuationType (
			final AvailObject object, 
			final AvailObject aContinuationType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfContinuationType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aCyclicType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfCyclicType (
			final AvailObject object, 
			final AvailObject aCyclicType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfCyclicType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aGeneralizedClosureType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfGeneralizedClosureType (
			final AvailObject object, 
			final AvailObject aGeneralizedClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfGeneralizedClosureType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param anIntegerRangeType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject anIntegerRangeType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfIntegerRangeType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aListType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfListType (
			final AvailObject object, 
			final AvailObject aListType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfListType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aMapType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfMapType (
			final AvailObject object, 
			final AvailObject aMapType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfMapType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param anObjectMeta
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfObjectMeta (
			final AvailObject object, 
			final AvailObject anObjectMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfObjectMeta: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param anObjectMetaMeta
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfObjectMetaMeta (
			final AvailObject object, 
			final AvailObject anObjectMetaMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfObjectMetaMeta: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param anObjectType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfObjectType (
			final AvailObject object, 
			final AvailObject anObjectType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfObjectType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aPrimitiveType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfPrimitiveType (
			final AvailObject object, 
			final AvailObject aPrimitiveType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfPrimitiveType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aSetType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfSetType (
			final AvailObject object, 
			final AvailObject aSetType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfSetType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aTupleType
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfTupleType (
			final AvailObject object, 
			final AvailObject aTupleType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isSupertypeOfTupleType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aBoolean
	 */
	@Override
	public void ObjectIsValid (
			final AvailObject object, 
			final boolean aBoolean)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isValid: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @param interpreter
	 * @return
	 */
	@Override
	public boolean ObjectIsValidForArgumentTypesInterpreter (
			final AvailObject object, 
			final List<AvailObject> argTypes, 
			final AvailInterpreter interpreter)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:isValidForArgumentTypes:interpreter: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectKeyAtIndex (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:keyAtIndex: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param keyObject
	 */
	@Override
	public void ObjectKeyAtIndexPut (
			final AvailObject object, 
			final int index, 
			final AvailObject keyObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:keyAtIndex:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectKeyType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:keyType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectLessOrEqual (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lessOrEqual: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectLessThan (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lessThan: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param index
	 * @param offset
	 */
	@Override
	public void ObjectLevelTwoChunkIndexOffset (
			final AvailObject object, 
			final int index, 
			final int offset)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:levelTwoChunkIndex:offset: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectLiteralAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:literalAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectLiteralAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:literalAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectLocalOrArgOrStackAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:localOrArgOrStackAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectLocalOrArgOrStackAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:localOrArgOrStackAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectLocalTypeAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:localTypeAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param argumentTypeArray
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByTypesFromArray (
			final AvailObject object, 
			final List<AvailObject> argumentTypeArray)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByTypesFromArray: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param continuation
	 * @param stackp
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByTypesFromContinuationStackp (
			final AvailObject object, 
			final AvailObject continuation, 
			final int stackp)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByTypesFromContinuation:stackp: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param argumentTypeTuple
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByTypesFromTuple (
			final AvailObject object, 
			final AvailObject argumentTypeTuple)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByTypesFromTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param argumentArray
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByValuesFromArray (
			final AvailObject object, 
			final List<AvailObject> argumentArray)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByValuesFromArray: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param continuation
	 * @param stackp
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByValuesFromContinuationStackp (
			final AvailObject object, 
			final AvailObject continuation, 
			final int stackp)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByValuesFromContinuation:stackp: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param argumentTuple
	 * @return
	 */
	@Override
	public AvailObject ObjectLookupByValuesFromTuple (
			final AvailObject object, 
			final AvailObject argumentTuple)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lookupByValuesFromTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectLowerBound (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lowerBound: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param lowInc
	 * @param highInc
	 */
	@Override
	public void ObjectLowerInclusiveUpperInclusive (
			final AvailObject object, 
			final boolean lowInc, 
			final boolean highInc)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:lowerInclusive:upperInclusive: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param keyObject
	 * @return
	 */
	@Override
	public AvailObject ObjectMapAt (
			final AvailObject object, 
			final AvailObject keyObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:mapAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param keyObject
	 * @param newValueObject
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectMapAtPuttingCanDestroy (
			final AvailObject object, 
			final AvailObject keyObject, 
			final AvailObject newValueObject, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:mapAt:putting:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMapSize (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:mapSize: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param keyObject
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectMapWithoutKeyCanDestroy (
			final AvailObject object, 
			final AvailObject keyObject, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:mapWithoutKey:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMessage (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:message: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMessageParts (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:messageParts: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMethods (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:methods: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param aNumber
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectMinusCanDestroy (
			final AvailObject object, 
			final AvailObject aNumber, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:minus:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInfinity
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectMultiplyByInfinityCanDestroy (
			final AvailObject object, 
			final AvailObject anInfinity, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:multiplyByInfinity:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInteger
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectMultiplyByIntegerCanDestroy (
			final AvailObject object, 
			final AvailObject anInteger, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:multiplyByInteger:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMyObjectMeta (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:myObjectMeta: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMyObjectType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:myObjectType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMyRestrictions (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:myRestrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectMyType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:myType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectName (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:name: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNames (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:names: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param trueName
	 * @return
	 */
	@Override
	public boolean ObjectNameVisible (
			final AvailObject object, 
			final AvailObject trueName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:nameVisible: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param anImplementationSet
	 */
	@Override
	public void ObjectNecessaryImplementationSetChanged (
			final AvailObject object, 
			final AvailObject anImplementationSet)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:necessaryImplementationSetChanged: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNewNames (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:newNames: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param nextChunk
	 */
	@Override
	public void ObjectNext (
			final AvailObject object, 
			final AvailObject nextChunk)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:next: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNextIndex (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:nextIndex: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNumBlanks (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:numBlanks: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNumFloats (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:numFloats: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNumIntegers (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:numIntegers: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNumObjects (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:numObjects: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectNybbles (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:nybbles: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public boolean ObjectOptionallyNilOuterVar (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:optionallyNilOuterVar: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectOuterTypeAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:outerTypeAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param tupleOfOuterTypes
	 * @param tupleOfLocalContainerTypes
	 */
	@Override
	public void ObjectOuterTypesLocalTypes (
			final AvailObject object, 
			final AvailObject tupleOfOuterTypes, 
			final AvailObject tupleOfLocalContainerTypes)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:outerTypes:localTypes: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectOuterVarAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:outerVarAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectOuterVarAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:outerVarAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPad (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:pad: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectParent (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:parent: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPc (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:pc: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param aNumber
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectPlusCanDestroy (
			final AvailObject object, 
			final AvailObject aNumber, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:plus:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param previousChunk
	 */
	@Override
	public void ObjectPrevious (
			final AvailObject object, 
			final AvailObject previousChunk)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:previous: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPreviousIndex (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:previousIndex: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPriority (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:priority: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param element
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateAddElement (
			final AvailObject object, 
			final AvailObject element)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateAddElement: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param element
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateExcludeElement (
			final AvailObject object, 
			final AvailObject element)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateExcludeElement: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param element
	 * @param knownIndex
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateExcludeElementKnownIndex (
			final AvailObject object, 
			final AvailObject element, 
			final int knownIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateExcludeElement:knownIndex: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param keyObject
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateExcludeKey (
			final AvailObject object, 
			final AvailObject keyObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateExcludeKey: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param keyObject
	 * @param valueObject
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateMapAtPut (
			final AvailObject object, 
			final AvailObject keyObject, 
			final AvailObject valueObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateMapAt:put: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPrivateNames (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateNames: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectPrivateTestingTree (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:privateTestingTree: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectProcessGlobals (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:processGlobals: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public short ObjectRawByteAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawByteAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param anInteger
	 */
	@Override
	public void ObjectRawByteAtPut (
			final AvailObject object, 
			final int index, 
			final short anInteger)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawByteAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public short ObjectRawByteForCharacterAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawByteForCharacterAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param anInteger
	 */
	@Override
	public void ObjectRawByteForCharacterAtPut (
			final AvailObject object, 
			final int index, 
			final short anInteger)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawByteForCharacterAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public byte ObjectRawNybbleAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawNybbleAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param aNybble
	 */
	@Override
	public void ObjectRawNybbleAtPut (
			final AvailObject object, 
			final int index, 
			final byte aNybble)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawNybbleAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectRawQuad1 (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawQuad1: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectRawQuad2 (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawQuad2: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public int ObjectRawQuadAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawQuadAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectRawQuadAtPut (
			final AvailObject object, 
			final int index, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawQuadAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public short ObjectRawShortForCharacterAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawShortForCharacterAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param anInteger
	 */
	@Override
	public void ObjectRawShortForCharacterAtPut (
			final AvailObject object, 
			final int index, 
			final short anInteger)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawShortForCharacterAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public int ObjectRawSignedIntegerAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawSignedIntegerAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectRawSignedIntegerAtPut (
			final AvailObject object, 
			final int index, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawSignedIntegerAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public long ObjectRawUnsignedIntegerAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawUnsignedIntegerAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectRawUnsignedIntegerAtPut (
			final AvailObject object, 
			final int index, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rawUnsignedIntegerAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param aChunkIndex
	 */
	@Override
	public void ObjectRemoveDependentChunkId (
			final AvailObject object, 
			final int aChunkIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:removeDependentChunkId: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param anInterpreter
	 */
	@Override
	public void ObjectRemoveFrom (
		final AvailObject object, 
		final AvailInterpreter anInterpreter)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:removeFrom: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param implementation
	 */
	@Override
	public void ObjectRemoveImplementation (
			final AvailObject object, 
			final AvailObject implementation)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:removeImplementation: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param message
	 * @param parts
	 * @return
	 */
	@Override
	public boolean ObjectRemoveMessageParts (
		final AvailObject object, 
		final AvailObject message, 
		final AvailObject parts)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:removeMessage:parts: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param obsoleteRestrictions
	 */
	@Override
	public void ObjectRemoveRestrictions (
		final AvailObject object, 
		final AvailObject obsoleteRestrictions)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:removeRestrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectRequiresBlock (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:requiresBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param forwardImplementation
	 * @param methodName
	 */
	@Override
	public void ObjectResolvedForwardWithName (
			final AvailObject object, 
			final AvailObject forwardImplementation, 
			final AvailObject methodName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:resolvedForward:withName: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectRestrictions (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:restrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectReturnsBlock (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:returnsBlock: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectReturnType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:returnType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectRootBin (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:rootBin: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectSecondTupleType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:secondTupleType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param otherSet
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSetIntersectionCanDestroy (
			final AvailObject object, 
			final AvailObject otherSet, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setIntersection:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param otherSet
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSetMinusCanDestroy (
			final AvailObject object, 
			final AvailObject otherSet, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setMinus:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectSetSize (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setSize: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param zoneIndex
	 * @param newTuple
	 */
	@Override
	public void ObjectSetSubtupleForZoneTo (
			final AvailObject object, 
			final int zoneIndex, 
			final AvailObject newTuple)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setSubtupleForZone:to: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param otherSet
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSetUnionCanDestroy (
			final AvailObject object, 
			final AvailObject otherSet, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setUnion:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param newValue
	 */
	@Override
	public void ObjectSetValue (
			final AvailObject object, 
			final AvailObject newValue)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setValue: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param newElementObject
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSetWithElementCanDestroy (
			final AvailObject object, 
			final AvailObject newElementObject, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setWithElement:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param elementObjectToExclude
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSetWithoutElementCanDestroy (
			final AvailObject object, 
			final AvailObject elementObjectToExclude, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:setWithoutElement:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectSignature (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:signature: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectSize (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:size: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public int ObjectSizeOfZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:sizeOfZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectSizeRange (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:sizeRange: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param slotIndex
	 * @return
	 */
	@Override
	public AvailObject ObjectStackAt (
			final AvailObject object, 
			final int slotIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:stackAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param slotIndex
	 * @param anObject
	 */
	@Override
	public void ObjectStackAtPut (
			final AvailObject object, 
			final int slotIndex, 
			final AvailObject anObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:stackAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectStackp (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:stackp: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectStartingChunkIndex (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:startingChunkIndex: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public int ObjectStartOfZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:startOfZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public int ObjectStartSubtupleIndexInZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:startSubtupleIndexInZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param anInfinity
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSubtractFromInfinityCanDestroy (
			final AvailObject object, 
			final AvailObject anInfinity, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:subtractFromInfinity:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anInteger
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectSubtractFromIntegerCanDestroy (
			final AvailObject object, 
			final AvailObject anInteger, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:subtractFromInteger:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param zone
	 * @return
	 */
	@Override
	public AvailObject ObjectSubtupleForZone (
			final AvailObject object, 
			final int zone)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:subtupleForZone: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aNumber
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectTimesCanDestroy (
			final AvailObject object, 
			final AvailObject aNumber, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:times:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param tupleIndex
	 * @param zoneIndex
	 * @return
	 */
	@Override
	public int ObjectTranslateToZone (
			final AvailObject object, 
			final int tupleIndex, 
			final int zoneIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:translate:toZone: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param stringName
	 * @return
	 */
	@Override
	public AvailObject ObjectTrueNamesForStringName (
			final AvailObject object, 
			final AvailObject stringName)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:trueNamesForStringName: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param newTupleSize
	 * @return
	 */
	@Override
	public AvailObject ObjectTruncateTo (
			final AvailObject object, 
			final int newTupleSize)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:truncateTo: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectTuple (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tuple: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectTupleAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tupleAt: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param aNybbleObject
	 */
	@Override
	public void ObjectTupleAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject aNybbleObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tupleAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @param newValueObject
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectTupleAtPuttingCanDestroy (
			final AvailObject object, 
			final int index, 
			final AvailObject newValueObject, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tupleAt:putting:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public int ObjectTupleIntAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tupleIntAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectTupleType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:tupleType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:type: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeAtIndex (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeAtIndex: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersection (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersection: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfClosureType (
			final AvailObject object, 
			final AvailObject aClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfClosureType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfClosureTypeCanDestroy (
			final AvailObject object, 
			final AvailObject aClosureType, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfClosureType:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aContainerType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfContainerType (
			final AvailObject object, 
			final AvailObject aContainerType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfContainerType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aContinuationType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfContinuationType (
			final AvailObject object, 
			final AvailObject aContinuationType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfContinuationType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aCyclicType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfCyclicType (
			final AvailObject object, 
			final AvailObject aCyclicType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfCyclicType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aGeneralizedClosureType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfGeneralizedClosureType (
			final AvailObject object, 
			final AvailObject aGeneralizedClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfGeneralizedClosureType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aGeneralizedClosureType
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfGeneralizedClosureTypeCanDestroy (
			final AvailObject object, 
			final AvailObject aGeneralizedClosureType, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfGeneralizedClosureType:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anIntegerRangeType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject anIntegerRangeType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfIntegerRangeType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aListType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfListType (
			final AvailObject object, 
			final AvailObject aListType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfListType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aMapType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfMapType (
			final AvailObject object, 
			final AvailObject aMapType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfMapType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param someMeta
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfMeta (
			final AvailObject object, 
			final AvailObject someMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectMeta
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfObjectMeta (
			final AvailObject object, 
			final AvailObject anObjectMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfObjectMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectMetaMeta
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfObjectMetaMeta (
			final AvailObject object, 
			final AvailObject anObjectMetaMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfObjectMetaMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfObjectType (
			final AvailObject object, 
			final AvailObject anObjectType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfObjectType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aSetType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfSetType (
			final AvailObject object, 
			final AvailObject aSetType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfSetType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aTupleType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeIntersectionOfTupleType (
			final AvailObject object, 
			final AvailObject aTupleType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeIntersectionOfTupleType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectTypeTuple (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeTuple: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnion (
			final AvailObject object, 
			final AvailObject another)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnion: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfClosureType (
			final AvailObject object, 
			final AvailObject aClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfClosureType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfClosureTypeCanDestroy (
			final AvailObject object, 
			final AvailObject aClosureType, 
			final boolean canDestroy)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfClosureType:canDestroy: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aContainerType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfContainerType (
			final AvailObject object, 
			final AvailObject aContainerType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfContainerType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aContinuationType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfContinuationType (
			final AvailObject object, 
			final AvailObject aContinuationType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfContinuationType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aCyclicType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfCyclicType (
			final AvailObject object, 
			final AvailObject aCyclicType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfCyclicType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aGeneralizedClosureType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfGeneralizedClosureType (
			final AvailObject object, 
			final AvailObject aGeneralizedClosureType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfGeneralizedClosureType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anIntegerRangeType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject anIntegerRangeType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfIntegerRangeType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aListType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfListType (
			final AvailObject object, 
			final AvailObject aListType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfListType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aMapType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfMapType (
			final AvailObject object, 
			final AvailObject aMapType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfMapType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectMeta
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfObjectMeta (
			final AvailObject object, 
			final AvailObject anObjectMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfObjectMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectMetaMeta
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfObjectMetaMeta (
			final AvailObject object, 
			final AvailObject anObjectMetaMeta)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfObjectMetaMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param anObjectType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfObjectType (
			final AvailObject object, 
			final AvailObject anObjectType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfObjectType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aSetType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfSetType (
			final AvailObject object, 
			final AvailObject aSetType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfSetType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param aTupleType
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeUnionOfTupleType (
			final AvailObject object, 
			final AvailObject aTupleType)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:typeUnionOfTupleType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectUnclassified (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:unclassified: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	@Override
	public AvailObject ObjectUnionOfTypesAtThrough (
			final AvailObject object, 
			final int startIndex, 
			final int endIndex)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:unionOfTypesAt:through: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public int ObjectUntranslatedDataAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:untranslatedDataAt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @param index
	 * @param value
	 */
	@Override
	public void ObjectUntranslatedDataAtPut (
			final AvailObject object, 
			final int index, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:untranslatedDataAt:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectUpperBound (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:upperBound: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param argTypes
	 * @param anAvailInterpreter
	 * @param failBlock
	 * @return
	 */
	@Override
	public AvailObject ObjectValidateArgumentTypesInterpreterIfFail (
			final AvailObject object, 
			final List<AvailObject> argTypes, 
			final AvailInterpreter anAvailInterpreter, 
			final Continuation1<Generator<String>> failBlock)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:validateArgumentTypes:interpreter:ifFail: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectValidity (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:validity: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectValue (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:value: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public AvailObject ObjectValueAtIndex (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:valueAtIndex: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @param index
	 * @param valueObject
	 */
	@Override
	public void ObjectValueAtIndexPut (
			final AvailObject object, 
			final int index, 
			final AvailObject valueObject)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:valueAtIndex:put: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectValueType (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:valueType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectVariableBindings (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:variableBindings: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectVectors (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:vectors: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectVisibleNames (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:visibleNames: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectWhichOne (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:whichOne: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectWordcodes (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:wordcodes: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @param index
	 * @return
	 */
	@Override
	public int ObjectZoneForIndex (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: Object:zoneForIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public String ObjectAsNativeString (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectAsNativeString: in Avail.Descriptor", object);
		return "";
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectAsObject (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectAsObject: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectAsSet (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectAsSet: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectAsTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectAsTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectBecomeExactType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBecomeExactType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectBecomeRealTupleType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBecomeRealTupleType: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectBitsPerEntry (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBitsPerEntry: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectBitVector (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBitVector: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectBodyBlock (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBodyBlock: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectBodySignature (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBodySignature: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectBreakpointBlock (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectBreakpointBlock: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCaller (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCaller: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectCapacity (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCapacity: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectCleanUpAfterCompile (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCleanUpAfterCompile: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectClearModule (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectClearModule: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectClearValue (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectClearValue: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectClosure (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectClosure: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectClosureType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectClosureType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCode (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCode: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectCodePoint (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCodePoint: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectComplete (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectComplete: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectConstantBindings (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectConstantBindings: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectContentType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectContentType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectContingentImpSets (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectContingentImpSets: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectContinuation (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectContinuation: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCopyAsMutableContinuation (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCopyAsMutableContinuation: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCopyAsMutableObjectTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCopyAsMutableObjectTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCopyAsMutableSpliceTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCopyAsMutableSpliceTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectCopyMutable (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectCopyMutable: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectDefaultType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectDefaultType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectDependentChunks (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectDependentChunks: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectDepth (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectDepth: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectDisplayTestingTree (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectDisplayTestingTree: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectEnsureMetacovariant (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectEnsureMetacovariant: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectEnsureMutable (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectEnsureMutable: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectEvictedByGarbageCollector (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectEvictedByGarbageCollector: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectExecutionMode (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExecutionMode: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectExecutionState (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExecutionState: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectExpand (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExpand: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectExtractBoolean (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractBoolean: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectExtractByte (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractByte: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public double ObjectExtractDouble (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractDouble: in Avail.Descriptor", object);
		return 0.0d;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public float ObjectExtractFloat (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractFloat: in Avail.Descriptor", object);
		return 0.0f;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectExtractInt (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractInt: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * Extract a 64-bit signed Java {@code long} from the specified Avail
	 * {@linkplain IntegerDescriptor integer}.
	 * 
	 * @param object An {@link AvailObject}.
	 * @return A 64-bit signed Java {@code long}
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	/**
	 * @param object
	 * @return
	 */
	@Override
	public long ObjectExtractLong (final @NotNull AvailObject object)
	{
		error(
			"Subclass responsiblity: ObjectExtractLong() in "
			+ getClass().getCanonicalName());
		return 0L;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public byte ObjectExtractNybble (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectExtractNybble: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectFieldMap (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectFieldMap: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectFieldTypeMap (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectFieldTypeMap: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectFilteredBundleTree (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectFilteredBundleTree: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectFirstTupleType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectFirstTupleType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectGetInteger (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectGetInteger: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectGetValue (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectGetValue: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHashOrZero (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHashOrZero: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectHasRestrictions (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHasRestrictions: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHiLevelTwoChunkLowOffset (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHiLevelTwoChunkLowOffset: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHiNumLocalsLowNumArgs (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHiNumLocalsLowNumArgs: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHiPrimitiveLowNumArgsAndLocalsAndStack (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHiPrimitiveLowNumArgsAndLocalsAndStack: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHiStartingChunkIndexLowNumOuters (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectHiStartingChunkIndexLowNumOuters: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectImplementationsTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectImplementationsTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectInclusiveFlags (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInclusiveFlags: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectIncomplete (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIncomplete: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectIndex (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectInnerType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInnerType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectInstance (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInstance: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectInternalHash (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInternalHash: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectInterruptRequestFlag (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInterruptRequestFlag: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectInvocationCount (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectInvocationCount: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsAbstract (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsAbstract: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsFinite (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsFinite: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsForward (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsForward: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsImplementation (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsImplementation: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsPositive (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsPositive: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSaved (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsSaved: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSplice (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsSplice: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfTerminates (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsSupertypeOfTerminates: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSupertypeOfVoid (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsSupertypeOfVoid: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsValid (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectIsValid: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public List<AvailObject> ObjectKeysAsArray (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectKeysAsArray: in Avail.Descriptor", object);
		return null;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectKeysAsSet (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectKeysAsSet: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectKeyType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectKeyType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectLevelTwoChunkIndex (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectLevelTwoChunkIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectLevelTwoOffset (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectLevelTwoOffset: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectLowerBound (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectLowerBound: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectLowerInclusive (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectLowerInclusive: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectMapSize (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMapSize: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectMaxStackDepth (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMaxStackDepth: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMessage (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMessage: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMessageParts (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMessageParts: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMethods (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMethods: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectMoveToHead (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMoveToHead: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMyObjectMeta (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMyObjectMeta: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMyObjectType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMyObjectType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMyRestrictions (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMyRestrictions: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMyType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectMyType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectName (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectName: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectNames (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNames: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectNewNames (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNewNames: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectNext (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNext: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNextIndex (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNextIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectNumArgs (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumArgs: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectNumArgsAndLocalsAndStack (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumArgsAndLocalsAndStack: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumberOfZones (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumberOfZones: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumBlanks (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumBlanks: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumFloats (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumFloats: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumIntegers (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumIntegers: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectNumLiterals (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumLiterals: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectNumLocals (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumLocals: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumLocalsOrArgsOrStack (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumLocalsOrArgsOrStack: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumObjects (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumObjects: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectNumOuters (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumOuters: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectNumOuterVars (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNumOuterVars: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectNybbles (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectNybbles: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectPad (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPad: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectParent (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectParent: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectPc (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPc: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectPrevious (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPrevious: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectPreviousIndex (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPreviousIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public short ObjectPrimitiveNumber (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPrimitiveNumber: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectPriority (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPriority: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateNames (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPrivateNames: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectPrivateTestingTree (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectPrivateTestingTree: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectProcessGlobals (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectProcessGlobals: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectRawQuad1 (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRawQuad1: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectRawQuad2 (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRawQuad2: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectReleaseVariableOrMakeContentsImmutable (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectReleaseVariableOrMakeContentsImmutable: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectRemoveFromQueue (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRemoveFromQueue: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectRemoveRestrictions (
		final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRemoveRestrictions: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectRequiresBlock (
		final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRequiresBlock: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectRestrictions (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRestrictions: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectReturnsBlock (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectReturnsBlock: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectReturnType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectReturnType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectRootBin (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectRootBin: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectSecondTupleType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectSecondTupleType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectSetSize (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectSetSize: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectSignature (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectSignature: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectSize (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectSize: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectSizeRange (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectSizeRange: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectStackp (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectStackp: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectStartingChunkIndex (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectStartingChunkIndex: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectStep (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectStep: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTestingTree (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTestingTree: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectTrimExcessLongs (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTrimExcessLongs: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectTupleSize (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTupleSize: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTupleType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTupleType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTypeTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectTypeTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectUnclassified (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectUnclassified: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectUpperBound (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectUpperBound: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectUpperInclusive (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectUpperInclusive: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectValidity (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectValidity: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectValue (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectValue: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectValuesAsTuple (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectValuesAsTuple: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectValueType (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectValueType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectVariableBindings (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectVariableBindings: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectVectors (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectVectors: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectVerify (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectVerify: in Avail.Descriptor", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectVisibleNames (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectVisibleNames: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectWhichOne (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectWhichOne: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectWordcodes (
			final AvailObject object)
	{
		//  GENERATED pure (abstract) method.

		error("Subclass responsibility: ObjectWordcodes: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}



	// GENERATED special mutable slots

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectEquals (
			final AvailObject object, 
			final AvailObject another)
	{
		error("Subclass responsibility: Object:equals: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aTuple
	 * @return
	 */
	@Override
	public boolean ObjectEqualsAnyTuple (
			final AvailObject object, 
			final AvailObject aTuple)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aString
	 * @return
	 */
	@Override
	public boolean ObjectEqualsByteString (
			final AvailObject object, 
			final AvailObject aString)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aTuple
	 * @return
	 */
	@Override
	public boolean ObjectEqualsByteTuple (
			final AvailObject object, 
			final AvailObject aTuple)
	{
		return false;
	}

	/**
	 * @param object
	 * @param otherCodePoint
	 * @return
	 */
	@Override
	public boolean ObjectEqualsCharacterWithCodePoint (
			final AvailObject object, 
			final int otherCodePoint)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aClosure
	 * @return
	 */
	@Override
	public boolean ObjectEqualsClosure (
			final AvailObject object, 
			final AvailObject aClosure)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aClosureType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsClosureType (
			final AvailObject object, 
			final AvailObject aClosureType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aCompiledCode
	 * @return
	 */
	@Override
	public boolean ObjectEqualsCompiledCode (
			final AvailObject object, 
			final AvailObject aCompiledCode)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aContainer
	 * @return
	 */
	@Override
	public boolean ObjectEqualsContainer (
			final AvailObject object, 
			final AvailObject aContainer)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsContainerType (
			final AvailObject object, 
			final AvailObject aType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aContinuation
	 * @return
	 */
	@Override
	public boolean ObjectEqualsContinuation (
			final AvailObject object, 
			final AvailObject aContinuation)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsContinuationType (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  GENERATED pure (abstract) method.

		return false;
	}

	/**
	 * @param object
	 * @param aDoubleObject
	 * @return
	 */
	@Override
	public boolean ObjectEqualsDouble (
			final AvailObject object, 
			final AvailObject aDoubleObject)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aFloatObject
	 * @return
	 */
	@Override
	public boolean ObjectEqualsFloat (
			final AvailObject object, 
			final AvailObject aFloatObject)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aGeneralizedClosureType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsGeneralizedClosureType (
			final AvailObject object, 
			final AvailObject aGeneralizedClosureType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param anInfinity
	 * @return
	 */
	@Override
	public boolean ObjectEqualsInfinity (
			final AvailObject object, 
			final AvailObject anInfinity)
	{
		return false;
	}

	/**
	 * @param object
	 * @param anAvailInteger
	 * @return
	 */
	@Override
	public boolean ObjectEqualsInteger (
			final AvailObject object, 
			final AvailObject anAvailInteger)
	{
		return false;
	}

	/**
	 * @param object
	 * @param another
	 * @return
	 */
	@Override
	public boolean ObjectEqualsIntegerRangeType (
			final AvailObject object, 
			final AvailObject another)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aList
	 * @return
	 */
	@Override
	public boolean ObjectEqualsList (
			final AvailObject object, 
			final AvailObject aList)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aListType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsListType (
			final AvailObject object, 
			final AvailObject aListType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aMap
	 * @return
	 */
	@Override
	public boolean ObjectEqualsMap (
			final AvailObject object, 
			final AvailObject aMap)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aMapType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsMapType (
			final AvailObject object, 
			final AvailObject aMapType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aTuple
	 * @return
	 */
	@Override
	public boolean ObjectEqualsNybbleTuple (
			final AvailObject object, 
			final AvailObject aTuple)
	{
		return false;
	}

	/**
	 * @param object
	 * @param anObject
	 * @return
	 */
	@Override
	public boolean ObjectEqualsObject (
			final AvailObject object, 
			final AvailObject anObject)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aTuple
	 * @return
	 */
	@Override
	public boolean ObjectEqualsObjectTuple (
			final AvailObject object, 
			final AvailObject aTuple)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsPrimitiveType (
			final AvailObject object, 
			final AvailObject aType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aSet
	 * @return
	 */
	@Override
	public boolean ObjectEqualsSet (
			final AvailObject object, 
			final AvailObject aSet)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aSetType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsSetType (
			final AvailObject object, 
			final AvailObject aSetType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aTupleType
	 * @return
	 */
	@Override
	public boolean ObjectEqualsTupleType (
			final AvailObject object, 
			final AvailObject aTupleType)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aString
	 * @return
	 */
	@Override
	public boolean ObjectEqualsTwoByteString (
			final AvailObject object, 
			final AvailObject aString)
	{
		return false;
	}

	/**
	 * @param object
	 * @param potentialInstance
	 * @return
	 */
	@Override
	public boolean ObjectHasObjectInstance (
			final AvailObject object, 
			final AvailObject potentialInstance)
	{
		//  The potentialInstance is a user-defined object.  See if it is an instance of me.

		return false;
	}

	/**
	 * @param object
	 * @param anotherObject
	 * @return
	 */
	@Override
	public boolean ObjectIsBetterRepresentationThan (
			final AvailObject object, 
			final AvailObject anotherObject)
	{
		//  Given two objects that are known to be equal, is the first one in a better form (more
		//  compact, more efficient, older generation) than the second one?

		return ((object.objectSlotsCount() + object.integerSlotsCount()) < (anotherObject.objectSlotsCount() + anotherObject.integerSlotsCount()));
	}

	/**
	 * @param object
	 * @param aTupleType
	 * @return
	 */
	@Override
	public boolean ObjectIsBetterRepresentationThanTupleType (
			final AvailObject object, 
			final AvailObject aTupleType)
	{
		//  Given two objects that are known to be equal, the second of which is in the form of
		//  a tuple type, is the first one in a better form than the second one?

		//  Explanation: This must be called with a tuple type as the second argument, but
		//  the two arguments must also be equal.  All alternative implementations of tuple
		//  types should reimplement this method.
		error("Subclass responsibility: Object:isBetterRepresentationThanTupleType: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectIsInstanceOfSubtypeOf (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  Answer whether object is an instance of a subtype of aType.  Don't generate
		//  an approximate type and do the comparison, because the approximate type
		//  will just send this message recursively.

		return object.exactType().isSubtypeOf(aType);
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectCanComputeHashOfType (
			final AvailObject object)
	{
		//  Answer whether object supports the #hashOfType protocol.

		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectEqualsBlank (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectEqualsFalse (
			final AvailObject object)
	{
		//  Answer true if this is the Avail false object, which it isn't.

		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectEqualsTrue (
			final AvailObject object)
	{
		//  Answer true if this is the Avail true object, which it isn't.

		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectEqualsVoid (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectEqualsVoidOrBlank (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectExactType (
			final AvailObject object)
	{
		//  Answer the object's type.  Don't answer an ApproximateType.

		error("Subclass responsibility: ObjectExactType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHash (
			final AvailObject object)
	{
		//  Answer a 32-bit long that is always the same for equal objects, but
		//  statistically different for different objects.

		error("Subclass responsibility: ObjectHash: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsClosure (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsHashAvailable (
			final AvailObject object)
	{
		//  Answer whether this object's hash value can be computed without creating
		//  new objects.  This method is used by the garbage collector to decide which
		//  objects to attempt to coalesce.  The garbage collector uses the hash values
		//  to find objects that it is likely can be coalesced together.

		return true;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectMakeImmutable (
			final AvailObject object)
	{
		//  Make the object immutable so it can be shared safely.  If I was mutable I have to scan
		//  my children and make them immutable as well (recursively down to immutable descendants).

		if (isMutable)
		{
			object.descriptorId((short)(object.descriptorId() | 1));
			object.makeSubobjectsImmutable();
		}
		return object;
	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectMakeSubobjectsImmutable (
			final AvailObject object)
	{
		//  Make my subobjects be immutable.  Don't change my own mutability state.
		//  Also, ignore my mutability state, as it should be tested (and sometimes set
		//  preemptively to immutable) prior to invoking this method.

		object.scanSubobjects(new AvailBeImmutableSubobjectVisitor());
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectType (
			final AvailObject object)
	{
		//  Answer the object's type.

		error("Subclass responsibility: ObjectType: in Avail.Descriptor", object);
		return VoidDescriptor.voidObject();
	}



	// operations-booleans

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsBoolean (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * Is the specified {@link AvailObject} an Avail byte tuple?
	 * 
	 * @param object An {@link AvailObject}.
	 * @return {@code true} if the argument is a byte tuple, {@code false}
	 *         otherwise.
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsByteTuple (final @NotNull AvailObject object)
	{
		return false;
	}

	// operations-characters

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsCharacter (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * Is the specified {@link AvailObject} an Avail string?
	 * 
	 * @param object An {@link AvailObject}.
	 * @return {@code true} if the argument is an Avail string, {@code false}
	 *         otherwise.
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsString (final @NotNull AvailObject object)
	{
		return false;
	}


	// operations-closure

	/**
	 * @param object
	 * @param aClosure
	 * @return
	 */
	@Override
	public boolean ObjectContainsBlock (
			final AvailObject object, 
			final AvailObject aClosure)
	{
		//  Answer true if either I am aClosure or I contain aClosure.  I only follow
		//  the trail of literal compiledCode and closure objects, so this is a dead end.

		return false;
	}



	// operations-faulting

	/**
	 * @param object
	 */
	@Override
	public void ObjectPostFault (
			final AvailObject object)
	{
		//  The object was just scanned, and its pointers converted into valid ToSpace pointers.
		//  Do any follow-up activities specific to the kind of object it is.
		//
		//  do nothing


	}

	/**
	 * @param object
	 */
	@Override
	public void ObjectReadBarrierFault (
			final AvailObject object)
	{
		//  The object is in ToSpace, and its fields already refer to ToSpace objects.  Do nothing,
		//  as there is no read barrier.  See also implementation in GCReadBarrierDescriptor.
		//
		//  do nothing


	}



	// operations-indirections

	/**
	 * @param object
	 * @param value
	 */
	@Override
	public void ObjectTarget (
			final AvailObject object, 
			final AvailObject value)
	{
		//  From IndirectionObjectDescriptor.  Fail if we're not an indirection object.

		error("This isn't an indirection object", object);
		return;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTarget (
			final AvailObject object)
	{
		//  From IndirectionObjectDescriptor.  Fail if we're not an indirection object.

		error("This isn't an indirection object", object);
		return VoidDescriptor.voidObject();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectTraversed (
			final AvailObject object)
	{
		//  Overidden in IndirectionDescriptor to skip over indirections.

		return object;
	}



	// operations-lists

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsList (
			final AvailObject object)
	{
		return false;
	}



	// operations-maps

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsMap (
			final AvailObject object)
	{
		return false;
	}



	// operations-numbers

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsByte (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsNybble (
			final AvailObject object)
	{
		return false;
	}



	// operations-set

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSet (
			final AvailObject object)
	{
		return false;
	}



	// operations-set bins

	/**
	 * @param object
	 * @param elementObject
	 * @param elementObjectHash
	 * @param myLevel
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectBinAddingElementHashLevelCanDestroy (
			final AvailObject object, 
			final AvailObject elementObject, 
			final int elementObjectHash, 
			final byte myLevel, 
			final boolean canDestroy)
	{
		//  Add the given element to this bin, potentially modifying it if canDestroy and it's
		//  mutable.  Answer the new bin.  Note that the client is responsible for marking
		//  elementObject as immutable if another reference exists.  In particular, the
		//  object is masquerading as a bin of size one.

		if (object.equals(elementObject))
		{
			return object;
		}
		//  Create a linear bin with two slots.
		final AvailObject result = AvailObject.newIndexedDescriptor(2, LinearSetBinDescriptor.isMutableLevel(true, myLevel));
		result.binHash(object.hash() + elementObject.hash());
		result.binElementAtPut(1, object);
		result.binElementAtPut(2, elementObject);
		if (!canDestroy)
		{
			result.makeImmutable();
		}
		return result;
	}

	/**
	 * @param object
	 * @param elementObject
	 * @param elementObjectHash
	 * @return
	 */
	@Override
	public boolean ObjectBinHasElementHash (
			final AvailObject object, 
			final AvailObject elementObject, 
			final int elementObjectHash)
	{
		//  Elements are treated as bins to save space, since bins are not
		//  entirely first-class objects (i.e., they can't be added to sets.

		return object.equals(elementObject);
	}

	/**
	 * @param object
	 * @param elementObject
	 * @param elementObjectHash
	 * @param canDestroy
	 * @return
	 */
	@Override
	public AvailObject ObjectBinRemoveElementHashCanDestroy (
			final AvailObject object, 
			final AvailObject elementObject, 
			final int elementObjectHash, 
			final boolean canDestroy)
	{
		//  Remove elementObject from the bin object, if present.  Answer the resulting bin.  The bin
		//  may be modified if it's mutable and canDestroy.  In particular, an element is masquerading
		//  as a bin of size one, so the answer must be either the object or voidObject (to indicate a size
		//  zero bin).

		if (object.equals(elementObject))
		{
			return VoidDescriptor.voidObject();
		}
		if (!canDestroy)
		{
			object.makeImmutable();
		}
		return object;
	}

	/**
	 * @param object
	 * @param potentialSuperset
	 * @return
	 */
	@Override
	public boolean ObjectIsBinSubsetOf (
			final AvailObject object, 
			final AvailObject potentialSuperset)
	{
		//  Sets only use explicit bins for collisions, otherwise they store the element
		//  itself.  This works because a bin can't be an element of a set.  Likewise,
		//  the voidObject can't be a member of a set and is treated like an empty bin.

		return potentialSuperset.hasElement(object);
	}

	/**
	 * @param object
	 * @param mutableTuple
	 * @param startingIndex
	 * @return
	 */
	@Override
	public int ObjectPopulateTupleStartingAt (
			final AvailObject object, 
			final AvailObject mutableTuple, 
			final int startingIndex)
	{
		//  Write set bin elements into the tuple, starting at the given startingIndex.  Answer
		//  the next available index in which to write.  Regular objects act as set bins
		//  of size 1, so treat them that way.

		assert mutableTuple.descriptor().isMutable();
		mutableTuple.tupleAtPut(startingIndex, object);
		return (startingIndex + 1);
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectBinHash (
			final AvailObject object)
	{
		//  An object masquerading as a size one bin has a bin hash which is the sum of
		//  the elements' hashes, which in this case is just the object's hash.

		return object.hash();
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectBinSize (
			final AvailObject object)
	{
		//  Answer how many elements this bin contains.  I act as a bin of size one.

		return 1;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public AvailObject ObjectBinUnionType (
			final AvailObject object)
	{
		//  Answer the union of the types of this bin's elements.  I act as a bin of size one.

		return object.type();
	}



	// operations-tuples

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsTuple (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @param aType
	 * @return
	 */
	@Override
	public boolean ObjectTypeEquals (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  Answer whether object's type is equal to aType (known to be a type).
		//  The current message may only be sent if the subclass receiving it has
		//  overidden ObjectCanComputeHashOfType to answer true.

		//  only provide if subclass canComputeHashOfType.
		error("Subclass responsibility: Object:typeEquals: in Avail.Descriptor", object);
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public int ObjectHashOfType (
			final AvailObject object)
	{
		//  We are computing the hash value of some ApproximateType, and it has
		//  delegated responsibility back to this descriptor, the one that created the
		//  ApproximateType that we're now trying to hash.  Only subclasses that
		//  answer true to the query canComputeHashOfType need to implement
		//  this method.

		//  only provide if subclass canComputeHashOfType.
		error("Subclass responsibility: ObjectHashOfType: in Avail.Descriptor", object);
		return 0;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsCyclicType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsExtendedInteger (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsIntegerRangeType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsListType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsMapType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsSetType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsTupleType (
			final AvailObject object)
	{
		return false;
	}

	/**
	 * @param object
	 * @return
	 */
	@Override
	public boolean ObjectIsType (
			final AvailObject object)
	{
		return false;
	}



	// scanning

	/**
	 * @param object
	 * @param visitor
	 */
	@Override
	public void ObjectScanSubobjects (
			final AvailObject object, 
			final AvailSubobjectVisitor visitor)
	{
		for (int byteIndex = -4, _end1 = object.objectSlotsCount() * -4; byteIndex >= _end1; byteIndex -= 4)
		{
			visitor.invokeWithParentIndex(object, byteIndex);
		}
	}

	/**
	 * Answer an {@linkplain Iterator iterator} suitable for traversing the
	 * elements of the {@linkplain AvailObject object} with a Java
	 * <em>foreach</em> construct.
	 * 
	 * @param object An {@link AvailObject}.
	 * @return An {@linkplain Iterator iterator}.
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	/**
	 * @param object
	 * @return
	 */
	@Override
	public @NotNull Iterator<AvailObject> ObjectIterator (
		final @NotNull AvailObject object)
	{
		error(
			"Subclass responsibility: ObjectIterator() in "
			+ getClass().getCanonicalName(),
			object);
		return null;
	}
}
