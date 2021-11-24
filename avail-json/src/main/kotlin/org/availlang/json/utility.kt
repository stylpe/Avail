/*
 * utility.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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

package org.availlang.json

import java.io.Reader

/**
 * Answer a [JSONWriter] that has had the provided [writerAction] applied to it.
 *
 * @param writerAction
 *   A lambda that accepts the created and returned [JSONWriter].
 * @return
 *   A [JSONWriter].
 */
fun jsonWriter (writerAction: JSONWriter.() -> Unit): JSONWriter =
	JSONWriter.newWriter().apply(writerAction)

/**
 * Answer a [JSONData] that has had the provided [dataAction] applied to it.
 *
 * @param dataAction
 *   A lambda that accepts the created and returned [JSONData].
 * @return
 *   A [JSONData].
 */
fun jsonData (reader: Reader, dataAction: JSONData.() -> Unit): JSONData =
	JSONReader(reader).read().apply(dataAction)

/**
 * Answer a [JSONObject] that has had the provided [objectAction] applied to it.
 *
 * @param objectAction
 *   A lambda that accepts the created and returned [JSONObject].
 * @return
 *   A [JSONObject].
 */
fun jsonObject (reader: Reader, objectAction: JSONObject.() -> Unit): JSONObject =
	(JSONReader(reader).read() as JSONObject).apply(objectAction)

/**
 * Answer a [JSONArray] that has had the provided [arrayAction] applied to it.
 *
 * @param arrayAction
 *   A lambda that accepts the created and returned [JSONArray].
 * @return
 *   A [JSONArray].
 */
fun jsonArray (reader: Reader, arrayAction: JSONArray.() -> Unit): JSONArray =
	(JSONReader(reader).read() as JSONArray).apply(arrayAction)
