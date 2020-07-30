/*
 * CollectionExtensions.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
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
package com.avail.utility

import com.avail.utility.structures.EnumMap

/**
 * Project the receiver onto an {@link EnumMap}, applying the function to each
 * enum value of the array.
 *
 * @param K
 *   The key type, an [Enum].
 * @param V
 *   The value type produced by the function.
 * @param generator
 *   The function to map keys to values.
 * @return
 *   A fully populated [EnumMap].
 */
inline fun <K : Enum<K>, V: Any> Array<K>.toEnumMap (
	generator: (K) -> V
): EnumMap<K, V>
{
	val map = EnumMap<K, V>(this)
	this.forEach { key -> map[key] = generator(key) }
	return map
}

/**
 * Transform the receiver via the supplied function and collect the results into
 * an optionally provided set. Answer the result set.
 *
 * @param T
 *   The element type of the incoming [Iterable].
 * @param R
 *   The element type of the outgoing [Set].
 * @param destination
 *   The destination [MutableSet]. Defaults to [mutableSetOf].
 * @param transform
 *   The function to map keys to values.
 * @return
 *   The resultant [MutableSet].
 */
inline fun <T, R> Iterable<T>.mapToSet (
	destination: MutableSet<R> = mutableSetOf(),
	transform: (T) -> R
): MutableSet<R> = mapTo(destination, transform)
