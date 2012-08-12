/**
 * MacroSubstitutionNodeDescriptor.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

import static com.avail.descriptor.AvailObject.multiplier;
import static com.avail.descriptor.MacroSubstitutionNodeDescriptor.ObjectSlots.*;
import java.util.List;
import com.avail.annotations.*;
import com.avail.compiler.AvailCodeGenerator;
import com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind;
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
	 * Getter for field macroName.
	 */
	@Override @AvailMethod
	AvailObject o_MacroName (
		final AvailObject object)
	{
		return object.slot(MACRO_NAME);
	}

	/**
	 * Getter for field outputParseNode.
	 */
	@Override @AvailMethod
	AvailObject o_OutputParseNode (
		final AvailObject object)
	{
		return object.slot(OUTPUT_PARSE_NODE);
	}


	@Override @AvailMethod
	AvailObject o_ExpressionType (final AvailObject object)
	{
		return object.outputParseNode().expressionType();
	}


	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		return
			object.macroName().hash() * multiplier
				+ object.outputParseNode().hash()
			^ 0x1d50d7f9;
	}


	@Override @AvailMethod
	boolean o_Equals (
		final AvailObject object,
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
		final AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		object.outputParseNode().emitEffectOn(codeGenerator);
	}


	@Override @AvailMethod
	void o_EmitValueOn (
		final AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		object.outputParseNode().emitValueOn(codeGenerator);
	}


	@Override @AvailMethod
	void o_ChildrenMap (
		final AvailObject object,
		final Transformer1<AvailObject, AvailObject> aBlock)
	{
		object.setSlot(
			OUTPUT_PARSE_NODE,
			aBlock.value(object.slot(OUTPUT_PARSE_NODE)));
	}


	@Override @AvailMethod
	void o_ChildrenDo (
		final AvailObject object,
		final Continuation1<AvailObject> aBlock)
	{
		aBlock.value(object.outputParseNode());
	}


	@Override @AvailMethod
	void o_ValidateLocally (
		final AvailObject object,
		final @Nullable AvailObject parent)
	{
		// Do nothing.
	}


	@Override @AvailMethod
	void o_FlattenStatementsInto (
		final AvailObject object,
		final List<AvailObject> accumulatedStatements)
	{
		object.outputParseNode().flattenStatementsInto(accumulatedStatements);
	}

	@Override
	ParseNodeKind o_ParseNodeKind (final AvailObject object)
	{
		return object.slot(OUTPUT_PARSE_NODE).parseNodeKind();
	}


	@Override
	public void printObjectOnAvoidingIndent (
		final AvailObject object,
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
	 * Construct a new {@linkplain MacroSubstitutionNodeDescriptor macro
	 * substitution node}.
	 *
	 * @param macroName
	 *            The name of the macro that produced this node.
	 * @param outputParseNode
	 *            The expression produced by the macro body.
	 * @return The new macro substitution node.
	 */
	public static AvailObject fromNameAndNode(
		final AvailObject macroName,
		final AvailObject outputParseNode)
	{
		final AvailObject newNode = mutable().create();
		newNode.setSlot(MACRO_NAME, macroName);
		newNode.setSlot(OUTPUT_PARSE_NODE, outputParseNode);
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
	private static final MacroSubstitutionNodeDescriptor mutable =
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
	private static final MacroSubstitutionNodeDescriptor immutable =
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
