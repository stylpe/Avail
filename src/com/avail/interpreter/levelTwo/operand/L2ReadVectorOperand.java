/**
 * L2ReadVectorOperand.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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
 *   may be used to endorse or promote products derived set this software
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

package com.avail.interpreter.levelTwo.operand;

import com.avail.descriptor.A_Type;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandDispatcher;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.interpreter.levelTwo.register.L2Register;
import com.avail.interpreter.levelTwo.register.RegisterTransformer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * An {@code L2ReadVectorOperand} is an operand of type {@link
 * L2OperandType#READ_VECTOR}.  It holds a {@link List} of {@link
 * L2ReadPointerOperand}s.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class L2ReadVectorOperand extends L2Operand
{
	/**
	 * The {@link List} of {@link L2ReadPointerOperand}s.
	 */
	private final List<L2ReadPointerOperand> elements;

	/** A lazily computed list of the type constraints of my elements. */
	private final List<A_Type> types;

	/**
	 * Construct a new {@code L2ReadVectorOperand} with the specified {@link
	 * List} of {@link L2ReadPointerOperand}s.
	 *
	 * @param elements The list of {@link L2ReadPointerOperand}s.
	 */
	public L2ReadVectorOperand (
		final List<L2ReadPointerOperand> elements)
	{
		this.elements = unmodifiableList(elements);
		this.types = unmodifiableList(
			elements.stream()
				.map(L2ReadPointerOperand::type)
				.collect(toList()));
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public L2Operand clone ()
	{
		final List<L2ReadPointerOperand> clonedElements = elements.stream()
			.map(r -> (L2ReadPointerOperand) r.clone())
			.collect(toList());
		return new L2ReadVectorOperand(clonedElements);
	}

	@Override
	public L2OperandType operandType ()
	{
		return L2OperandType.READ_VECTOR;
	}

	/**
	 * Answer my {@link List} of {@link L2ReadPointerOperand}s.
	 */
	public List<L2ReadPointerOperand> elements ()
	{
		return elements;
	}

	/**
	 * Answer a {@link List} of the {@link A_Type}s bounding each corresponding
	 * element.
	 *
	 * @return The list of types.
	 */
	public List<A_Type> types ()
	{
		return types;
	}

	@Override
	public void dispatchOperand (final L2OperandDispatcher dispatcher)
	{
		dispatcher.doOperand(this);
	}

	@Override
	public L2ReadVectorOperand transformRegisters (
		final RegisterTransformer<L2OperandType> transformer)
	{
		return new L2ReadVectorOperand(
			elements.stream()
				.map(element -> element.transformRegisters(transformer))
				.collect(toList()));
	}

	@Override
	public void instructionWasAdded (final L2Instruction instruction)
	{
		for (final L2ReadPointerOperand element : elements)
		{
			element.instructionWasAdded(instruction);
		}
	}

	@Override
	public void instructionWasRemoved (final L2Instruction instruction)
	{
		for (final L2ReadPointerOperand element : elements)
		{
			element.instructionWasRemoved(instruction);
		}
	}

	@Override
	public void replaceRegisters (
		final Map<L2Register, L2Register> registerRemap,
		final L2Instruction instruction)
	{
		elements.forEach(
			read -> read.replaceRegisters(registerRemap, instruction));
	}

	@Override
	public String toString ()
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("ReadVector(");
		boolean first = true;
		for (final L2ReadPointerOperand register : elements)
		{
			if (!first)
			{
				builder.append(",");
			}
			builder.append(register);
			first = false;
		}
		builder.append(")");
		return builder.toString();
	}
}
