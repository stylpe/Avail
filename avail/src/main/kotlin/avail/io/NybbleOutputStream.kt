/*
 * NybbleOutputStream.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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

package avail.io

import java.io.ByteArrayOutputStream

/**
 * `NybbleOutputStream` is a [ByteArrayOutputStream] that enforces that only
 * nybbles — values in `[0..15]` — are written.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
class NybbleOutputStream
@JvmOverloads constructor(size: Int = 0): ByteArrayOutputStream(size)
{
	override fun write(b: Int)
	{
		assert(b < 16) { "b must be in [0..15]" }
		super.write(b)
	}

	override fun write(b: ByteArray)
	{
		assert(b.all { it < 16 }) { "all bytes must be in [0..15]" }
		super.write(b)
	}

	override fun write(b: ByteArray, off: Int, len: Int)
	{
		assert((off..len).all { b[it] < 16 }) { "all bytes must be in [0..15]" }
		super.write(b, off, len)
	}
}
