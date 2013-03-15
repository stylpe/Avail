/**
 * P_383_CreateSendExpression.java
 * Copyright © 1993-2013, Mark van Gulik and Todd L Smith.
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

package com.avail.interpreter.primitive;

import static com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind.*;
import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.exceptions.AvailErrorCode.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.*;
import com.avail.compiler.MessageSplitter;
import com.avail.descriptor.*;
import com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind;
import com.avail.exceptions.*;
import com.avail.interpreter.*;

/**
 * <strong>Primitive 383</strong>: Create a {@linkplain SendNodeDescriptor send
 * expression} from the specified {@linkplain MessageBundleDescriptor message
 * bundle}, {@linkplain ListNodeDescriptor list node} of {@linkplain
 * ParseNodeKind#EXPRESSION_NODE argument expressions}, and {@linkplain
 * TypeDescriptor return type}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class P_383_CreateSendExpression
extends Primitive
{
	/**
	 * The sole instance of this primitive class. Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_383_CreateSendExpression().init(3, CanFold);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter)
	{
		assert args.size() == 3;
		final A_Atom messageName = args.get(0);
		final A_Phrase argsListNode = args.get(1);
		final A_Type returnType = args.get(2);
		try
		{
			final MessageSplitter splitter =
				new MessageSplitter(messageName.name());
			if (splitter.numberOfArguments()
				!= argsListNode.expressionsTuple().tupleSize())
			{
				return interpreter.primitiveFailure(
					E_INCORRECT_NUMBER_OF_ARGUMENTS);
			}
		}
		catch (final SignatureException e)
		{
			assert false : "The method name was extracted from a real method!";
		}
		return interpreter.primitiveSuccess(
			SendNodeDescriptor.from(
				messageName.bundleOrCreate(),
				argsListNode,
				returnType));
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				ATOM.o(),
				LIST_NODE.mostGeneralType(),
				InstanceMetaDescriptor.topMeta()),
			SEND_NODE.mostGeneralType());
	}
}
