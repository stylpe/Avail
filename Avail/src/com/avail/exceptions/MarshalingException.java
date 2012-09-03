/**
 * MarshalingException.java
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

package com.avail.exceptions;

import static com.avail.exceptions.AvailErrorCode.E_JAVA_MARSHALING_FAILED;
import com.avail.descriptor.AvailObject;

/**
 * {@code MarshalingException} indicates that an error occurred during the
 * marshaling of an {@linkplain AvailObject Avail object} to Java.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class MarshalingException
extends AvailRuntimeException
{
	/** The serial version identifier. */
	private static final long serialVersionUID = -5955389215116529L;

	/**
	 * Construct a new {@link MarshalingException}.
	 */
	public MarshalingException ()
	{
		super(E_JAVA_MARSHALING_FAILED);
	}

	/**
	 * Construct a new {@link MarshalingException}.
	 *
	 * @param cause
	 *        The proximal {@linkplain Throwable cause} of the {@code
	 *        MarshalingException exception}.
	 */
	public MarshalingException (final Throwable cause)
	{
		super(E_JAVA_MARSHALING_FAILED, cause);
	}
}
