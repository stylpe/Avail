/**
 * MacroSubstitutionNodeDescriptor.java
 * Copyright (c) 2011, Mark van Gulik.
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
import java.util.List;
import com.avail.annotations.*;
import com.avail.compiler.AvailCodeGenerator;
import com.avail.interpreter.levelTwo.L2Interpreter;
import com.avail.utility.*;

/**
 * A {@linkplain MacroSubstitutionNodeDescriptor macro substitution node}
 * represents the result of applying a {@linkplain MacroImplementationDescriptor
 * macro} to its argument {@linkplain ParseNodeDescriptor expressions} to
 * produce an {@linkplain ObjectSlots#OUTPUT_PARSE_NODE output parse node}.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class MacroSubstitutionNodeDescriptor extends ParseNodeDescriptor
{
	/**
	 * My slots of type {@link AvailObject}.
	 *
	 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
	 */
	public enum ObjectSlots implements ObjectSlotsEnum
	{
		/**
		 * The {@linkplain AtomDescriptor true name} of the macro that was
		 * invoked to produce this {@linkplain MacroSubstitutionNodeDescriptor
		 * macro substitution node}.
		 */
		MACRO_NAME,

		/**
		 * The {@linkplain ParseNodeDescriptor parse node} that is the result of
		 * transforming the input parse node through a {@linkplain
		 * MacroImplementationDescriptor macro substitution}.
		 */
		OUTPUT_PARSE_NODE
	}

	/**
	 * Setter for field macroName.
	 */
	@Override @AvailMethod
	void o_MacroName (
		final @NotNull AvailObject object,
		final AvailObject value)
	{
		object.setSlot(ObjectSlots.MACRO_NAME, value);
	}

	/**
	 * Getter for field macroName.
	 */
	@Override @AvailMethod
	AvailObject o_MacroName (
		final @NotNull AvailObject object)
	{
		return object.slot(ObjectSlots.MACRO_NAME);
	}

	/**
	 * Setter for field outputParseNode.
	 */
	@Override @AvailMethod
	void o_OutputParseNode (
		final @NotNull AvailObject object,
		final AvailObject value)
	{
		object.setSlot(ObjectSlots.OUTPUT_PARSE_NODE, value);
	}

	/**
	 * Getter for field outputParseNode.
	 */
	@Override @AvailMethod
	AvailObject o_OutputParseNode (
		final @NotNull AvailObject object)
	{
		return object.slot(ObjectSlots.OUTPUT_PARSE_NODE);
	}


	@Override @AvailMethod
	AvailObject o_ExpressionType (final AvailObject object)
	{
		return object.outputParseNode().expressionType();
	}


	@Override @AvailMethod
	AvailObject o_Kind (final AvailObject object)
	{
		return object.outputParseNode().kind();
	}


	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		return
			object.macroName().hash() * Multiplier
				+ object.outputParseNode().hash()
			^ 0x1d50d7f9;
	}


	@Override @AvailMethod
	boolean o_Equals (
		final @NotNull AvailObject object,
		final AvailObject another)
	{
		return object.macroName().equals(another.macroName())
			&& object.outputParseNode().equals(another.outputParseNode());
	}


	@Override @AvailMethod
	AvailObject o_ApparentSendName (final AvailObject object)
	{
		return object.macroName();
	}


	@Override @AvailMethod
	void o_EmitEffectOn (
		final @NotNull AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		object.outputParseNode().emitEffectOn(codeGenerator);
	}


	@Override @AvailMethod
	void o_EmitValueOn (
		final @NotNull AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		object.outputParseNode().emitValueOn(codeGenerator);
	}


	@Override @AvailMethod
	void o_ChildrenMap (
		final @NotNull AvailObject object,
		final Transformer1<AvailObject, AvailObject> aBlock)
	{
		object.outputParseNode(aBlock.value(object.outputParseNode()));
	}


	@Override @AvailMethod
	void o_ChildrenDo (
		final @NotNull AvailObject object,
		final Continuation1<AvailObject> aBlock)
	{
		aBlock.value(object.outputParseNode());
	}


	@Override @AvailMethod
	void o_ValidateLocally (
		final @NotNull AvailObject object,
		final AvailObject parent,
		final List<AvailObject> outerBlocks,
		final L2Interpreter anAvailInterpreter)
	{
		// Do nothing.
	}


	@Override @AvailMethod
	void o_FlattenStatementsInto (
		final @NotNull AvailObject object,
		final List<AvailObject> accumulatedStatements)
	{
		object.outputParseNode().flattenStatementsInto(accumulatedStatements);
	}


	@Override
	public void printObjectOnAvoidingIndent (
		final @NotNull AvailObject object,
		final StringBuilder builder,
		final List<AvailObject> recursionList,
		final int indent)
	{
		builder.append("MACRO TRANSFORMATION (");
		builder.append(object.macroName());
		builder.append(") = ");
		object.outputParseNode().printOnAvoidingIndent(
			builder,
			recursionList,
			indent);
	}


	/**
	 * Construct a new {@linkplain MacroSubstitutionNodeDescriptor macro substitution
	 * node}.
	 *
	 * @param macroName
	 *            The name of the macro that produced this node.
	 * @param outputParseNode
	 *            The expression produced by the macro body.
	 * @return The new macro substitution node.
	 */
	public AvailObject fromNameAndNode(
		final @NotNull AvailObject macroName,
		final @NotNull AvailObject outputParseNode)
	{
		final AvailObject newNode = mutable().create();
		newNode.macroName(macroName);
		newNode.outputParseNode(outputParseNode);
		newNode.makeImmutable();
		return newNode;
	}

	/**
	 * Construct a new {@link MacroSubstitutionNodeDescriptor}.
	 *
	 * @param isMutable Whether my {@linkplain AvailObject instances} can
	 *                  change.
	 */
	public MacroSubstitutionNodeDescriptor (final boolean isMutable)
	{
		super(isMutable);
	}

	/**
	 * The mutable {@link MacroSubstitutionNodeDescriptor}.
	 */
	private final static MacroSubstitutionNodeDescriptor mutable =
		new MacroSubstitutionNodeDescriptor(true);

	/**
	 * Answer the mutable {@link MacroSubstitutionNodeDescriptor}.
	 *
	 * @return The mutable {@link MacroSubstitutionNodeDescriptor}.
	 */
	public static MacroSubstitutionNodeDescriptor mutable ()
	{
		return mutable;
	}

	/**
	 * The immutable {@link MacroSubstitutionNodeDescriptor}.
	 */
	private final static MacroSubstitutionNodeDescriptor immutable =
		new MacroSubstitutionNodeDescriptor(false);

	/**
	 * Answer the immutable {@link MacroSubstitutionNodeDescriptor}.
	 *
	 * @return The immutable {@link MacroSubstitutionNodeDescriptor}.
	 */
	public static MacroSubstitutionNodeDescriptor immutable ()
	{
		return immutable;
	}
}
