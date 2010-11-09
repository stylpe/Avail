/**
 * interpreter/levelTwo/L2InstructionWriter.java
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

package com.avail.interpreter.levelTwo;

import java.io.ByteArrayOutputStream;
import com.avail.descriptor.AvailObject;
import com.avail.descriptor.ByteTupleDescriptor;
import com.avail.descriptor.IntegerDescriptor;
import com.avail.descriptor.ObjectTupleDescriptor;
import com.avail.interpreter.levelTwo.L2RawInstruction;

public class L2InstructionWriter
{

	final ByteArrayOutputStream stream = new ByteArrayOutputStream();

	private void writeOperand(int operand)
	{
		assert operand == (operand & 0xFFFF);
		stream.write(operand >>> 8);
		stream.write(operand & 0xFF);
	};

	public void write(L2RawInstruction instruction)
	{
		int opcode = instruction.operation().ordinal();
		assert opcode == (opcode & 0xFFFF);
		stream.write(opcode >>> 8);
		stream.write(opcode & 0xFF);
		int [] operands = instruction.operands();
		for (int i = 0; i < operands.length; i++)
		{
			writeOperand(operands[i]);
		}
	};

	public AvailObject words()
	{
		byte [] byteArray = stream.toByteArray();
		int wordCount = byteArray.length >> 1;
		// If all the high bytes are zero we can use a ByteTuple.
		boolean allBytes = true;
		for (int i = 0; i < byteArray.length; i += 2)
		{
			if (byteArray[i] != 0)
			{
				allBytes = false;
			}
		}
		AvailObject words;
		if (allBytes)
		{
			words = AvailObject.newIndexedDescriptor(
				(wordCount + 3) / 4,
				ByteTupleDescriptor.isMutableSize(true, wordCount));
			int dest = 1;
			for (int source = 1; source < byteArray.length; source += 2)
			{
				words.rawByteAtPut(dest++, byteArray[source]);
			}
		}
		else
		{
			words = AvailObject.newIndexedDescriptor(
				wordCount,
				ObjectTupleDescriptor.mutableDescriptor());
			int dest = 1;
			for (int source = 0; source < byteArray.length; source += 2)
			{
				int value = (byteArray[source] & 0xFF) << 8 + (byteArray[source + 1] & 0xFF);
				words.tupleAtPut(dest++, IntegerDescriptor.objectFromInt(value));
			}
		}
		words.hashOrZero(0);
		words.makeImmutable();
		return words;
	};
}