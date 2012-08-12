/**
 * P_161_FileOpenWrite.java
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
package com.avail.interpreter.primitive;

import static com.avail.descriptor.TypeDescriptor.Types.ATOM;
import static com.avail.exceptions.AvailErrorCode.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.io.*;
import java.util.List;
import com.avail.annotations.NotNull;
import com.avail.descriptor.*;
import com.avail.interpreter.*;

/**
 * <strong>Primitive 161:</strong> Open a {@linkplain RandomAccessFile file}
 * for writing. Answer a {@linkplain AtomDescriptor handle} that uniquely
 * identifies the file.
 *
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
public class P_161_FileOpenWrite
extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public final static Primitive instance = new P_161_FileOpenWrite().init(
		2, CanInline, HasSideEffect);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter)
	{
		assert args.size() == 2;
		final AvailObject filename = args.get(0);
		final AvailObject append = args.get(1);

		final AvailObject handle = interpreter.createAtom(filename);
		try
		{
			final RandomAccessFile file = new RandomAccessFile(
				filename.asNativeString(),
				"rw");
			if (append.extractBoolean())
			{
				file.seek(file.length());
			}
			else
			{
				file.setLength(0);
			}
			interpreter.runtime().putWritableFile(handle, file);
		}
		catch (final IOException e)
		{
			return interpreter.primitiveFailure(E_IO_ERROR);
		}
		catch (final SecurityException e)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED);
		}
		return interpreter.primitiveSuccess(handle);
	}

	@Override
	protected AvailObject privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				TupleTypeDescriptor.stringTupleType(),
				EnumerationTypeDescriptor.booleanObject()),
			ATOM.o());
	}
}