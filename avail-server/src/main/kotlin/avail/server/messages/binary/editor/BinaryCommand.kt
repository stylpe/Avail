/*
 * BinaryCommand.kt
 * Copyright © 1993-2019, The Avail Foundation, LLC.
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

package avail.server.messages.binary.editor

import avail.builder.ModuleName
import avail.builder.ModuleNameResolver.Companion.availExtension
import avail.builder.ModuleRoot
import avail.builder.ModuleRootErrorCode.BAD_MODULE_ROOT
import avail.error.ErrorCode
import avail.files.EditRange
import avail.files.FileErrorCode.BAD_FILE_ID
import avail.files.FileManager
import avail.files.RedoAction
import avail.files.ReplaceContents
import avail.files.SaveAction
import avail.files.UndoAction
import avail.server.AvailServer.Companion.logger
import avail.server.error.ServerErrorCode
import avail.server.error.ServerErrorCode.NO_SESSION
import avail.server.io.AvailServerChannel
import avail.server.messages.Message
import avail.server.session.Session
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.logging.Level

/**
 * `BinaryCommand` enumerates the set of possible commands available for use
 * over a [binary][AvailServerChannel.ProtocolState.BINARY]
 * [channel][AvailServerChannel].
 *
 * All command [Message]s have the same 12 prefix bytes:
 *
 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
 *    identifies the transaction the message is part of.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property id
 *   The integer that uniquely identifies this [BinaryCommand].
 *
 * @constructor
 * Construct a [BinaryCommand].
 *
 * @param id
 *   The integer that uniquely identifies this [BinaryCommand].
 */
enum class BinaryCommand constructor(val id: Int)
{
	/** The canonical representation of an invalid [BinaryCommand]. */
	INVALID(-1)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			buffer.rewind()
			val content = ByteArray(buffer.limit())
			buffer.get(content)
			InvalidBinaryMessage(commandId, id, content).processThen(channel)
		}
	},

	/**
	 * Confirmation of successful completion of a request/command.
	 *
	 * The message is only expected to have the standard 12-byte header with
	 * no body:
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 */
	OK(0)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			continuation()
		}
	},

	/**
	 * Indicates an [error][ServerErrorCode] has occurred.
	 *
	 * The message expects the standard 12-byte header with an optional error
	 * code.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * [ServerErrorCode.ordinal] (4-bytes): The int value that identifies the
	 *    error code the describes the problem.
	 */
	ERROR(1)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			if (buffer.remaining() >= 4)
			{
				val errorCode = ServerErrorCode.code(buffer.int)
				val remaining = buffer.remaining()
				val description =
					if (remaining > 0)
					{
						String(ByteArray(remaining), Charsets.UTF_8)
					}
					else null
				// TODO any special error handling?
				ErrorBinaryMessage(commandId, errorCode, false, description)
					.processThen(channel, continuation)
			}
			else
			{
				continuation()
			}
		}
	},

	/**
	 * [Create][FileManager.createFile] a new file in the file hierarchy of a
	 * loaded [Avail root][ModuleRoot]. Write access for the Avail root must
	 * be held by the client for this to be allowed. (TODO RAA - do this)
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `Path` (n-bytes): The path relative to the Avail root where the file
	 *    is to be created. ex: `/avail/Avail.avail/Some New File`
	 */
	CREATE_FILE(2)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			// TODO support creating a package
			val mimeSize = buffer.int
			val mimeBytes = ByteArray(mimeSize)
			buffer.get(mimeBytes)
			val mimeType = String(mimeBytes, Charsets.UTF_8)
			val raw = ByteArray(buffer.remaining())
			buffer.get(raw)
			val qualifiedName = String(raw, Charsets.UTF_8)
			assert(qualifiedName.isNotEmpty())
			val target = ModuleName(qualifiedName)
			val moduleRoot = channel.server.runtime.moduleRoots()
				.moduleRootFor(target.rootName)
			moduleRoot ?: return channel.enqueueMessageThen(
				ErrorBinaryMessage(
					commandId,
					BAD_MODULE_ROOT,
					false,
					"${target.rootName} not found"
				).message,
				continuation)
			// Module root was found.
			val resolver = moduleRoot.resolver
			channel.server.fileManager.createFile(
				qualifiedName,
				mimeType, // TODO adjust for MIME type
				resolver,
				completion = {
					channel.server.fileManager.readFile(
						qualifiedName,
						resolver,
						withFile = { uuid, mime, file ->
							channel.sessionId?.let { sessionId ->
								channel.server.sessions[sessionId]?.let {
									val fileId = it.addFileCacheId(uuid)
									FileOpenedMessage(
										commandId,
										fileId,
										file.rawContent.size.toLong(),
										mime
									).processThen(channel) {
										FileStreamMessage(
											commandId,
											fileId,
											file.rawContent
										).processThen(channel)
									}
								}
							}
						},
						failureHandler = { code, ex ->
							ex?.let { e ->
								logger.log(Level.SEVERE, e) {
									"Could not read file: $qualifiedName"
								}
							}
							channel.enqueueMessageThen(
								ErrorBinaryMessage(commandId, code).message
							) {
								// TODO [RAA] handle session not found
							}
						})
				},
				failureHandler = { code, throwable ->
					throwable?.let { e ->
						logger.log(Level.SEVERE, e) {
							"Could not create file: " +
								resolver.fullResourceURI(qualifiedName)
						}
					}
					channel.enqueueMessageThen(
						ErrorBinaryMessage(commandId, code).message) {}
				})
			// Request is asynchronous, so continue
			continuation()
		}
	},

	/**
	 * Request to open a file in the [FileManager]. Read access for the Avail
	 * root must be held by the client for this to be allowed. (TODO RAA - do this)
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `Path` (n-bytes): The path relative to the Avail root where the file
	 *    is located. ex: `/avail/Avail.avail/Some New File`
	 */
	OPEN_FILE(3)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val raw = ByteArray(buffer.remaining())
			buffer.get(raw)
			val relativePath = String(raw, Charsets.UTF_8)
			assert(relativePath.isNotEmpty())
			val qname = relativePath.replace(availExtension, "")
			val target = ModuleName(qname)
			val fileManager = channel.server.fileManager
			val moduleRoot = channel.server.runtime.moduleRoots()
				.moduleRootFor(target.rootName)
			moduleRoot ?: return channel.enqueueMessageThen(
				ErrorBinaryMessage(
					commandId,
					BAD_MODULE_ROOT,
					false,
					"${target.rootName} not found"
				).message,
				continuation)
			// The module root was found.
			val moduleRootResolver = moduleRoot.resolver
			moduleRootResolver.provideResolverReference(
				target.qualifiedName,
				withReference = { ref ->
					fileManager.readFile(
						target.qualifiedName,
						moduleRootResolver,
						withFile = { uuid, _, file ->
							channel.session?.let { session ->
								val fileId = session.addFileCacheId(uuid)
								FileOpenedMessage(
									commandId, fileId, ref.size, ref.mimeType
								).processThen(channel) {
									FileStreamMessage(
										commandId,
										fileId,
										file.rawContent
									).processThen(channel)
								}
							}
						},
						failureHandler = { code, throwable ->
							throwable?.let { e ->
								logger.log(Level.SEVERE, e) {
									"Could not read file: ${ref.qualifiedName}"
								}
								e.printStackTrace()
							}
							channel.enqueueMessageThen(
								ErrorBinaryMessage(
									commandId,
									code).message) {}
						})
					// Request is asynchronous, so continue
					continuation()
				},
				failureHandler = { code, ex ->
					ex?.let { e ->
						logger.log(Level.SEVERE, e) {
							"Could not read file: " + target.qualifiedName
						}
						e.printStackTrace()
					}
					channel.enqueueMessageThen(
						ErrorBinaryMessage(commandId, code).message) {}
				})
		}
	},

	/**
	 * Request to close a file in the [FileManager]. This indicates the
	 * [Session] no longer wishes cached access to the file. If number of
	 * sessions interested in the file is 0 as a result of this close, the file
	 * will be removed from the [FileManager].
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File id` (4-bytes): The `Session` specific cache id of the file to
	 *    close.
	 */
	CLOSE_FILE(4)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val fileId = buffer.int
			val removedId = session.removeFileCacheId(fileId)
			if (removedId == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			OkMessage(commandId).processThen(channel, continuation)
		}
	},

	/**
	 * Request to save a file in the [FileManager] to disk. This writes the file
	 * as it exists in the `FileManager` cache to disk.
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File id` (4-bytes): The `Session` specific cache id of the file to
	 *    save.
	 */
	SAVE_FILE(5)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val fileId = buffer.int
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val uuid = session.getFile(fileId)
			if (uuid == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			val fail: (ErrorCode, Throwable?) -> Unit =
				{ code, e ->
					logger.log(Level.SEVERE, "Save file error: $code", e)
					ErrorBinaryMessage(commandId, code, false)
						.processThen(channel)
				}
			channel.server.fileManager.executeAction(
				uuid,
				SaveAction(channel.server.fileManager, fail),
				channel.sessionId!!,
				continuation,
				fail)
		}
	},

	/**
	 * Response to the client to [OPEN_FILE] or [CREATE_FILE].
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File id` (4-bytes): The `Session` specific cache id of the file being
	 *    opened.
	 * * `mime size` (4-bytes) - The number of bytes that makes up the string `mime`
	 * * `file size` (4-bytes) - The total number of bytes that makes up the file.
	 * * `mime` (n-bytes) - The UTF-8 encoded mime. ex: "text/avail"
	 *
	 * See [FileOpenedMessage].
	 */
	FILE_OPENED(6),

	/**
	 * Stream the contents of a file to the client. This follows the
	 * [FILE_OPENED] message in response to a [OPEN_FILE] request or
	 * [CREATE_FILE] request.
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File id` (4-bytes): The `Session` specific cache id of the file being
	 *    streamed.
	 * * `file` (n-bytes) - The UTF-16BE encoded file contents.
	 */
	FILE_STREAM(7),

	/**
	 * An [EditRange] request. Write access for the Avail root must be held by
	 * the client for this to be allowed. (TODO RAA - do this)
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File Id` (4-bytes): The `Session` specific cache id of the file to
	 *    edit.
	 * * `start index` (4-bytes): The index into the target file to start
	 *    replacing text or bytes for a binary file. This marks the place where
	 *    new text (or bytes) will be entered.
	 * * `end index` (4-bytes): The index, exclusive, that marks the end of
	 *    where text (or bytes) is being replaced. Effectively [start, end)
	 *    marks the range of content to delete.
	 * * `replacement content` (n-bytes) - The bytes used to replace in the file.
	 *    If a text file is being edited, the encoding should be UTF-16BE.
	 */
	EDIT_FILE_RANGE(8)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val fileId = buffer.int
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val uuid = session.getFile(fileId)
			if (uuid == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			val start = buffer.int
			val end = buffer.int
			val data = ByteArray(buffer.remaining())
			buffer.get(data)
			val edit = EditRange(data, start, end)
			channel.server.fileManager.executeAction(
				uuid,
				edit,
				channel.sessionId!!,
				continuation)
			{ code, e ->
				logger.log(Level.SEVERE, "Edit file range error: $code", e)
				ErrorBinaryMessage(commandId, code, false)
					.processThen(channel)
			}
		}
	},

	/**
	 * An [UndoAction] request. This will undo the last [EditRange] action.
	 * Write access for the Avail root must be held by the client for this to be
	 * allowed. (TODO RAA - do this)
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File Id` (4-bytes): The `Session` specific cache id of the file to
	 *    undo edit.
	 */
	UNDO_FILE_EDIT(9)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val fileId = buffer.int
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val uuid = session.getFile(fileId)
			if (uuid == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			channel.server.fileManager.executeAction(
				uuid, UndoAction, channel.sessionId!!, continuation) { code, e ->
					logger.log(Level.SEVERE, "Undo edit error: $code", e)
					ErrorBinaryMessage(commandId, code, false)
						.processThen(channel)
				}
		}
	},

	/**
	 * A [RedoAction] request. This will redo the last [UNDO_FILE_EDIT] action.
	 * Write access for the Avail root must be held by the client for this to
	 * be allowed. (TODO RAA - do this)
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File Id` (4-bytes): The `Session` specific cache id of the file to
	 *    redo edit.
	 */
	REDO_FILE_EDIT(10)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val fileId = buffer.int
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val uuid = session.getFile(fileId)
			if (uuid == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			channel.server.fileManager.executeAction(
				uuid, RedoAction, channel.sessionId!!, continuation) { code, e ->
					logger.log(Level.SEVERE, "Redo file error: $code", e)
					ErrorBinaryMessage(commandId, code, false)
						.processThen(channel)
				}
		}
	},

	/**
	 * A request to delete a file from the file system. Write access for the
	 * Avail root must be held by the client for this to be allowed.
	 * (TODO RAA - do this)
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File Id` (4-bytes): The `Session` specific cache id of the file to
	 *    delete.
	 */
	DELETE_FILE(11)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val raw = ByteArray(buffer.remaining())
			buffer.get(raw)
			val relativePath = String(raw, Charsets.UTF_8)
			assert(relativePath.isNotEmpty())
			val target = ModuleName(relativePath)
			channel.server.runtime.moduleRoots()
				.moduleRootFor(target.rootName)?.let { mr ->
					mr.resolver.let {
						val path = Paths.get(
								it.uri.toString(), target.rootRelativeName)
							.toString()
						it.provideResolverReference(target.qualifiedName,
						{ reference ->
							channel.server.fileManager.delete(
								reference,
								{ fileId ->
									if (fileId != null)
									{
										channel.server.sessions.values
											.forEach { session ->
												session.removeFile(fileId)
												// TODO RAA notify interested parties?

											}
									}
									OkMessage(commandId)
										.processThen(channel, continuation)
								}) { code, throwable ->
								throwable?.let { e ->
									logger.log(Level.SEVERE, e) {
										"Could not delete file: $path"
									}
								}
								channel.enqueueMessageThen(
									ErrorBinaryMessage(
										commandId,
										code).message) {}
							}
						})
						{ code, ex ->
								ex?.printStackTrace()
								channel.enqueueMessageThen(
									ErrorBinaryMessage(
										commandId,
										code,
										false,
										"Could not delete " +
											target.qualifiedName
									).message,
									continuation)
						}
					}
				} ?: channel.enqueueMessageThen(
					ErrorBinaryMessage(
						commandId,
						BAD_MODULE_ROOT,
						false,
						"${target.rootName} not found"
					).message,
					continuation)
		}
	},

	/**
	 * A [ReplaceContents] request. Write access for the Avail root must be held
	 * by the client for this to be allowed. (TODO RAA - do this)
	 *
	 * The message expects the standard 12-byte header with additional content.
	 *
	 * **Message Format**
	 * * [BinaryCommand.id] (4-bytes): The int id that identifies the command
	 * * [BinaryMessage.commandId] (8-bytes): The long transaction id that
	 *    identifies the transaction the message is part of.
	 * * `File Id` (4-bytes): The `Session` specific cache id of the file to
	 *    edit.
	 * * `replacement content` (n-bytes) - The bytes used to replace in the file.
	 *    If a text file is being edited, the encoding should be UTF-16BE.
	 */
	REPLACE_CONTENTS(12)
	{
		override fun receiveThen(
			id: Int,
			commandId: Long,
			buffer: ByteBuffer,
			channel: AvailServerChannel,
			continuation: () -> Unit)
		{
			val fileId = buffer.int
			val session = channel.session
			if (session == null)
			{
				ErrorBinaryMessage(commandId, NO_SESSION, false)
					.processThen(channel)
				return
			}
			val uuid = session.getFile(fileId)
			if (uuid == null)
			{
				ErrorBinaryMessage(commandId, BAD_FILE_ID, false)
					.processThen(channel)
				return
			}
			val data = ByteArray(buffer.remaining())
			buffer.get(data)
			channel.server.fileManager.executeAction(
				uuid, ReplaceContents(data),
				channel.sessionId!!,
				continuation)
				{ code, e ->
					logger.log(Level.SEVERE, "Edit file range error: $code", e)
					ErrorBinaryMessage(commandId, code, false)
						.processThen(channel)
				}
		}
	};

	/**
	 * Process this [binary message][BinaryMessage] on behalf of the specified
	 * [channel][AvailServerChannel].
	 *
	 * @param id
	 *   The [BinaryCommand.id].
	 * @param commandId
	 *   The identifier of the [message][BinaryMessage]. This identifier should
	 *   appear in any responses to this message.
	 * @param buffer
	 *   The [ByteBuffer] that contains the [Message].
	 * @param channel
	 *   The channel that is associated with this message.
	 * @param continuation
	 *   What to do when sufficient processing has occurred.
	 */
	open fun receiveThen (
		id: Int,
		commandId: Long,
		buffer: ByteBuffer,
		channel: AvailServerChannel,
		continuation: ()->Unit)
	{
		throw UnsupportedOperationException("$name does not support receiveThen")
	}

	companion object
	{
		/**
		 * Answer the [BinaryCommand] for the provided [BinaryCommand.id].
		 *
		 * @param id
		 *   The integer value used to identify the `BinaryCommand`.
		 * @return
		 *   The associated `BinaryCommand` or [BinaryCommand.INVALID] if the
		 *   id is not found.
		 */
		fun command (id: Int): BinaryCommand =
			when(id)
			{
				0 -> OK
				1 -> ERROR
				2 -> CREATE_FILE
				3 -> OPEN_FILE
				4 -> CLOSE_FILE
				5 -> SAVE_FILE
				6 -> FILE_OPENED
				7 -> FILE_STREAM
				8 -> EDIT_FILE_RANGE
				9 -> UNDO_FILE_EDIT
				10 -> REDO_FILE_EDIT
				11 -> DELETE_FILE
				12 -> REPLACE_CONTENTS
				else -> INVALID
			}
	}
}
