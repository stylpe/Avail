/**
 * compiler/AvailSendNode.java Copyright (c) 2010, Mark van Gulik. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
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

package com.avail.compiler;

import java.util.*;
import com.avail.descriptor.AvailObject;
import com.avail.interpreter.levelTwo.L2Interpreter;

public class AvailSendNode extends AvailParseNode
{
	AvailObject _message;
	AvailObject _bundle;
	AvailObject _implementationSet;
	List<AvailParseNode> _arguments;
	AvailObject _returnType;


	public List<AvailParseNode> arguments ()
	{
		return _arguments;
	}

	public void arguments (final List<AvailParseNode> anArray)
	{
		_arguments = anArray;
	}

	public AvailObject bundle ()
	{
		return _bundle;
	}

	public void bundle (final AvailObject aMessageBundle)
	{
		_bundle = aMessageBundle;
	}

	public AvailObject implementationSet ()
	{
		return _implementationSet;
	}

	public void implementationSet (final AvailObject anImplementationSet)
	{
		_implementationSet = anImplementationSet;
	}

	public AvailObject message ()
	{
		return _message;
	}

	public void message (final AvailObject cyclicType)
	{
		_message = cyclicType;
	}

	/**
	 * Answer this message send's expected return type.  Make it immutable so
	 * that multiple requests avoid accidental sharing.
	 *
	 * @return The type of this message send.
	 */
	public AvailObject returnType ()
	{
		return _returnType.makeImmutable();
	}

	/**
	 * Set this message send's expected return type.
	 *
	 * @param aType The type this message send should return.
	 */
	public void returnType (final AvailObject aType)
	{
		_returnType = aType;
	}

	@Override
	public AvailObject type ()
	{
		return returnType();
	}

	@Override
	public void emitValueOn (final AvailCodeGenerator codeGenerator)
	{
		boolean anyCasts;
		anyCasts = false;
		for (AvailParseNode arg : _arguments)
		{
			arg.emitValueOn(codeGenerator);
			if (arg.isSuperCast())
			{
				anyCasts = true;
			}
		}
		_message.makeImmutable();
		if (anyCasts)
		{
			for (AvailParseNode arg : _arguments)
			{
				if (arg.isSuperCast())
				{
					codeGenerator.emitPushLiteral(arg.type());
				}
				else
				{
					codeGenerator.emitGetType(_arguments.size() - 1);
				}
			}
			// We've pushed all argument values and all arguments types onto the
			// stack.
			codeGenerator.emitSuperCall(
				_arguments.size(),
				_implementationSet,
				returnType());
		}
		else
		{
			codeGenerator.emitCall(
				_arguments.size(),
				_implementationSet,
				returnType());
		}
	}

	@Override
	public void childrenMap (
		final Transformer1<AvailParseNode, AvailParseNode> aBlock)
	{
		_arguments = new ArrayList<AvailParseNode>(_arguments);
		for (int i = 0; i < _arguments.size(); i++)
		{
			_arguments.set(i, aBlock.value(_arguments.get(i)));
		}
	}

	@Override
	public void printOnIndent (final StringBuilder aStream, final int indent)
	{
		final boolean nicePrinting = true;
		if (nicePrinting)
		{
			MessageSplitter splitter = new MessageSplitter(message().name());
			splitter.printSendNodeOnIndent(
				this,
				aStream,
				indent);
		}
		else
		{
			aStream.append("SendNode[");
			aStream.append(message().name().asNativeString());
			aStream.append("](");
			boolean isFirst = true;
			for (AvailParseNode arg : arguments())
			{
				if (!isFirst)
				{
					aStream.append(",");
				}
				aStream.append("\n");
				for (int i = indent; i >= 0; i--)
				{
					aStream .append("\t");
				}
				arg.printOnIndentIn(aStream, indent + 1, this);
				isFirst = false;
			}
			aStream.append(")");
		}
	}

	@Override
	public void printOnIndentIn (
		final StringBuilder aStream,
		final int indent,
		final AvailParseNode outerNode)
	{
		// aStream.append('(');
		printOnIndent(aStream, indent);
		// aStream.append(')');
	}


	@Override
	public boolean isSend ()
	{
		return true;
	}


	@Override
	public AvailParseNode validateLocallyWithParentOuterBlocksInterpreter (
		final AvailParseNode parent,
		final List<AvailBlockNode> outerBlocks,
		final L2Interpreter anAvailInterpreter)
	{
		// Invoke the requires clauses in bottom-up order.

		List<AvailObject> argumentTypes;
		argumentTypes = new ArrayList<AvailObject>(_arguments.size());
		for (AvailParseNode arg : _arguments)
		{
			argumentTypes.add(arg.type());
		}
		anAvailInterpreter.validateRequiresClausesOfMessageSendArgumentTypes(
			message(),
			argumentTypes);
		return this;
	}

}
