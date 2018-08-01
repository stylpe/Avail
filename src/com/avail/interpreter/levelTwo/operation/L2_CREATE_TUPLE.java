/*
 * L2_CREATE_TUPLE.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
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
package com.avail.interpreter.levelTwo.operation;

import com.avail.descriptor.A_BasicObject;
import com.avail.descriptor.A_Tuple;
import com.avail.descriptor.A_Type;
import com.avail.descriptor.AvailObject;
import com.avail.descriptor.ObjectTupleDescriptor;
import com.avail.descriptor.TupleDescriptor;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.L2Operand;
import com.avail.interpreter.levelTwo.operand.L2ReadPointerOperand;
import com.avail.interpreter.levelTwo.operand.L2WritePointerOperand;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.L2Translator;
import com.avail.optimizer.RegisterSet;
import com.avail.optimizer.jvm.JVMTranslator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.avail.descriptor.BottomTypeDescriptor.bottom;
import static com.avail.descriptor.InstanceTypeDescriptor.instanceType;
import static com.avail.descriptor.IntegerDescriptor.fromInt;
import static com.avail.descriptor.ObjectTupleDescriptor.tupleFromList;
import static com.avail.descriptor.TupleDescriptor.emptyTuple;
import static com.avail.descriptor.TupleTypeDescriptor
	.tupleTypeForSizesTypesDefaultType;
import static com.avail.descriptor.TypeDescriptor.Types.ANY;
import static com.avail.interpreter.levelTwo.L2OperandType.READ_VECTOR;
import static com.avail.interpreter.levelTwo.L2OperandType.WRITE_POINTER;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.*;

/**
 * Create a {@link TupleDescriptor tuple} from the {@linkplain AvailObject
 * objects} in the specified registers.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class L2_CREATE_TUPLE
extends L2Operation
{
	/**
	 * Construct an {@code L2_CREATE_TUPLE}.
	 */
	private L2_CREATE_TUPLE ()
	{
		super(
			READ_VECTOR.is("elements"),
			WRITE_POINTER.is("tuple"));
	}

	/**
	 * Initialize the sole instance.
	 */
	public static final L2_CREATE_TUPLE instance = new L2_CREATE_TUPLE();

	@Override
	protected void propagateTypes (
		final L2Instruction instruction,
		final RegisterSet registerSet,
		final L2Translator translator)
	{
		final List<L2ReadPointerOperand> elements =
			instruction.readVectorRegisterAt(0);
		final L2WritePointerOperand destinationReg =
			instruction.writeObjectRegisterAt(1);

		final int size = elements.size();
		final A_Type sizeRange = fromInt(size).kind();
		final List<A_Type> types = new ArrayList<>(size);
		for (final L2ReadPointerOperand element: elements)
		{
			if (registerSet.hasTypeAt(element.register()))
			{
				types.add(registerSet.typeAt(element.register()));
			}
			else
			{
				types.add(ANY.o());
			}
		}
		final A_Type tupleType =
			tupleTypeForSizesTypesDefaultType(sizeRange,
				tupleFromList(types), bottom());
		tupleType.makeImmutable();
		registerSet.removeConstantAt(destinationReg.register());
		registerSet.typeAtPut(
			destinationReg.register(),
			tupleType,
			instruction);
		if (registerSet.allRegistersAreConstant(elements))
		{
			final List<AvailObject> constants = new ArrayList<>(size);
			for (final L2ReadPointerOperand element : elements)
			{
				constants.add(registerSet.constantAt(element.register()));
			}
			final A_Tuple tuple = tupleFromList(constants);
			tuple.makeImmutable();
			assert tuple.isInstanceOf(tupleType);
			registerSet.typeAtPut(
				destinationReg.register(),
				instanceType(tuple),
				instruction);
			registerSet.constantAtPut(
				destinationReg.register(), tuple, instruction);
		}
	}

	/**
	 * Given an {@link L2Instruction} using this operation, extract the list of
	 * registers that supply the elements of the tuple.
	 *
	 * @param instruction
	 *        The tuple creation instruction to examine.
	 * @return The instruction's {@link List} of {@link L2ReadPointerOperand}s
	 *         that supply the tuple elements.
	 */
	public static List<L2ReadPointerOperand> tupleSourceRegistersOf (
		final L2Instruction instruction)
	{
		assert instruction.operation() == instance;
		return instruction.readVectorRegisterAt(0);
	}

	@Override
	public void toString (
		final L2Instruction instruction,
		final Set<L2OperandType> desiredTypes,
		final StringBuilder builder)
	{
		assert this == instruction.operation();
		final L2Operand elements = instruction.operand(0);
		final L2ObjectRegister destinationReg =
			instruction.writeObjectRegisterAt(1).register();

		renderPreamble(instruction, builder);
		builder.append(' ');
		builder.append(destinationReg);
		builder.append(" ← ");
		builder.append(elements);
	}

	/**
	 * Generated code uses:
	 * <ul>
	 *     <li>{@link ObjectTupleDescriptor#tupleFromArray(
	 *         A_BasicObject...)}</li>
	 *     <li>{@link TupleDescriptor#emptyTuple()}</li>
	 *     <li>{@link ObjectTupleDescriptor#tuple(A_BasicObject)}</li>
	 *     <li>{@link ObjectTupleDescriptor#tuple(
	 *         A_BasicObject, A_BasicObject)}</li>
	 *     <li>{@link ObjectTupleDescriptor#tuple(
	 *         A_BasicObject, A_BasicObject, A_BasicObject)}</li>
	 *     <li>{@link ObjectTupleDescriptor#tuple(
	 *         A_BasicObject, A_BasicObject, A_BasicObject, A_BasicObject)}</li>
	 *     <li>{@link ObjectTupleDescriptor#tuple(
	 *         A_BasicObject,
	 *         A_BasicObject,
	 *         A_BasicObject,
	 *         A_BasicObject,
	 *         A_BasicObject)}</li>
	 * </ul>
	 */
	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final List<L2ReadPointerOperand> elements =
			instruction.readVectorRegisterAt(0);
		final L2ObjectRegister destinationReg =
			instruction.writeObjectRegisterAt(1).register();

		final int size = elements.size();
		if (size <= 5)
		{
			// Special cases for small tuples
			if (size == 0)
			{
				// :: destination = theEmptyTupleLiteral;
				translator.literal(method, emptyTuple());
				translator.store(method, destinationReg);
				return;
			}
			// :: destination = TupleDescriptor.tuple(element1...elementN);
			for (int i = 0; i < size; i++)
			{
				translator.load(method, elements.get(i).register());
			}
			final Type[] callSignature = new Type[size];
			Arrays.fill(callSignature, getType(A_BasicObject.class));
			method.visitMethodInsn(
				INVOKESTATIC,
				getInternalName(ObjectTupleDescriptor.class),
				"tuple",
				getMethodDescriptor(
					getType(A_Tuple.class),
					callSignature),
				false);
			method.visitTypeInsn(CHECKCAST, getInternalName(AvailObject.class));
			translator.store(method, destinationReg);
			return;
		}
		// :: destination = TupleDescriptor.tupleFromArray(elements);
		translator.objectArray(method, elements, A_BasicObject.class);
		method.visitMethodInsn(
			INVOKESTATIC,
			getInternalName(ObjectTupleDescriptor.class),
			"tupleFromArray",
			getMethodDescriptor(
				getType(A_Tuple.class),
				getType(A_BasicObject[].class)),
			false);
		method.visitTypeInsn(CHECKCAST, getInternalName(AvailObject.class));
		translator.store(method, destinationReg);
	}
}
