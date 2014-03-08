/**
 * StacksGenerator.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
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

package com.avail.stacks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.HashMap;
import com.avail.builder.ModuleName;
import com.avail.compiler.AbstractAvailCompiler.ModuleHeader;
import com.avail.descriptor.A_Set;
import com.avail.descriptor.A_String;
import com.avail.descriptor.A_Tuple;
import com.avail.descriptor.CommentTokenDescriptor;
import com.avail.descriptor.ModuleDescriptor;
import com.avail.descriptor.TupleDescriptor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An Avail documentation generator.  It takes tokenized method/class comments
 * in .avail files and creates navigable documentation from them.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class StacksGenerator
{
	/**
	 * The default path to the output directory for module-oriented
	 * documentation and data files.
	 */
	public static final Path defaultDocumentationPath = Paths.get("stacks");

	/**
	 * The path for documentation storage as provided by the user.
	 */
	Path providedDocumentPath;

	/**
	 *  The location useds for storing any log files such as error-logs.
	 */
	public static final Path defaultLogPath = Paths.get("logs");

	/**
	 * The error log file for the malformed comments.
	 */
	AsynchronousFileChannel errorLog;

	/**
	 * Create a new error log path.
	 * @return the path for the newly created error log.
	 */
	private Path createErrorLogPath ()
	{
		return defaultLogPath
			.resolve("error log "
				+ Calendar.getInstance().getTime().toString());
	}

	/**
	 * File position tracker for error log
	 */
	private long errorFilePosition;

	/**
	 * Set errorFilePosition to a new postion.
	 * @param newPosition
	 */
	private void errorFilePosition(final long newPosition)
	{
		errorFilePosition = newPosition;
	}

	/**
	 * A map of {@linkplain ModuleName module names} to a list of all the method
	 * names exported from said module
	 */
	HashMap<A_String,A_Set> moduleToExportedMethodsMap;

	/**
	 * A map of {@linkplain ModuleName module names} to a list of all the method
	 * names exported from said module
	 */
	HashMap<A_String,StacksCommentsModule> moduleToComments;

	/**
	 * Construct a new {@link StacksGenerator}.
	 *
	 * @param outputPath
	 *        The {@linkplain Path path} to the output {@linkplain
	 *        BasicFileAttributes#isDirectory() directory} for documentation and
	 *        data files.
	 * @throws IllegalArgumentException
	 *         If the output path exists but does not specify a directory.
	 */
	public StacksGenerator(final Path outputPath)
		throws IllegalArgumentException
	{

		if (Files.exists(outputPath) && !Files.isDirectory(outputPath))
		{
			throw new IllegalArgumentException(
				outputPath + " exists and is not a directory");
		}
		try
		{
			this.providedDocumentPath = Files.createDirectories(outputPath);
			this.errorLog =
				AsynchronousFileChannel
					.open(createErrorLogPath(),
						StandardOpenOption.CREATE,
						StandardOpenOption.WRITE);
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}

		this.moduleToComments =
			new HashMap<A_String,StacksCommentsModule>();

		this.moduleToExportedMethodsMap =
			new HashMap<A_String,A_Set>();

		this.errorFilePosition = 0;
	}

	/**
	 * Inform the {@linkplain StacksGenerator generator} about the documentation
	 * and linkage of a {@linkplain ModuleDescriptor module}.
	 *
	 * @param header
	 *        The {@linkplain ModuleHeader header} of the module.
	 * @param commentTokens
	 *        The complete {@linkplain TupleDescriptor collection} of
	 *        {@linkplain CommentTokenDescriptor comments} produced for the
	 *        given module.
	 */
	public synchronized void add (
		final ModuleHeader header,
		final A_Tuple commentTokens)
	{
		StacksCommentsModule commentsModule = null;
		try
		{
			commentsModule = new StacksCommentsModule(
				header,commentTokens,moduleToExportedMethodsMap);
		}
		catch (StacksScannerException | StacksCommentBuilderException e)
		{
			try
			{
				final Future<Integer> newFilePostion = errorLog.write(
			    	  ByteBuffer.wrap(e.getMessage().getBytes()),
			    	  errorFilePosition);
				errorFilePosition(newFilePostion.get());
			}
			catch (InterruptedException | ExecutionException e1)
			{
				e1.printStackTrace();
			}
		}
		updateModuleToComments(commentsModule);
	}

	/**
	 * Update moduleToComments with a new {@linkplain StacksCommentsModule}.
	 * @param commentModule
	 * 		A new {@linkplain StacksCommentsModule} to add to moduleToComments;
	 */
	private void updateModuleToComments (
		final StacksCommentsModule commentModule)
	{
		moduleToComments.put(commentModule.moduleName(), commentModule);
	}

	/**
	 * Generate complete Stacks documentation.
	 *
	 * @param outermostModule
	 *        The outermost {@linkplain ModuleDescriptor module} for the
	 *        generation request.
	 */
	public synchronized void generate (final ModuleName outermostModule)
	{
		// TODO [RAA]: Implement everything else.
	}

	/**
	 * Clear all internal data structures and reinitialize the {@linkplain
	 * StacksGenerator generator} for subsequent usage.
	 */
	public synchronized void clear ()
	{
		this.moduleToComments.clear();

		this.moduleToExportedMethodsMap.clear();
	}
}
