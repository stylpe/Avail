/**
 * NewModuleTask.java
 * Copyright © 1993-2017, The Avail Foundation, LLC. All rights reserved.
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

package com.avail.environment.tasks;
import com.avail.builder.ModuleName;
import com.avail.builder.ResolvedModuleName;
import com.avail.builder.UnresolvedDependencyException;
import com.avail.environment.AvailWorkbench;
import com.avail.environment.AvailWorkbench.AbstractWorkbenchTask;
import com.avail.environment.viewer.NewModuleWindow;
import com.avail.environment.viewer.NewPackageWindow;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;

/**
 * A {@code NewPackageTask} is a {@link AbstractWorkbenchTask} used to create
 * a new package.
 *
 * @author Rich Arriaga &lt;rich@availlang.org&gt;
 */
public class NewPackageTask
extends FXWindowTask
{
	/**
	 * The {@link File} directory this new package will be placed in.
	 */
	private final @NotNull File directory;

	/**
	 * The base portion of the qualified name.
	 */
	private final @NotNull String baseQualifiedName;

	/**
	 * The {@link ModuleName#qualifiedName} of the new module.
	 */
	private String qualifiedName;

	/**
	 * Create the {@link ModuleName#qualifiedName} of the new module.
	 *
	 * @param leaf
	 *        The name of the module.
	 */
	public void setQualifiedName (final @NotNull String leaf)
	{
		qualifiedName = baseQualifiedName + leaf;
	}

	@Override
	public @NotNull Scene newScene ()
	{
		return new NewPackageWindow(
			310,
			135,
			directory,
			workbench,
			this);
	}

	@Override
	public void positionFrame (final @NotNull JFrame frame)
	{
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(
			dim.width/2-frame.getSize().width/2,
			dim.height/2-frame.getSize().height/2);
	}

	@Override
	public void cleanCloseTask ()
	{
		final ResolvedModuleName selection = workbench.selectedModule();
		final TreeNode modules = workbench.newModuleTree();
		workbench.moduleTree.setModel(new DefaultTreeModel(modules));
		for (int i = workbench.moduleTree.getRowCount() - 1; i >= 0; i--)
		{
			workbench.moduleTree.expandRow(i);
		}
		if (selection != null)
		{
			final TreePath path = workbench.modulePath(
				selection.qualifiedName());
			if (path != null)
			{
				workbench.moduleTree.setSelectionPath(path);
			}
		}

		final TreeNode entryPoints = workbench.newEntryPointsTree();
		workbench.entryPointsTree.setModel(new DefaultTreeModel(entryPoints));
		for (int i = workbench.entryPointsTree.getRowCount() - 1; i >= 0; i--)
		{
			workbench.entryPointsTree.expandRow(i);
		}

		EventQueue.invokeLater(() ->
		{
			try
			{
				ResolvedModuleName resolvedModuleName =
					workbench.availBuilder.runtime.moduleNameResolver()
						.resolve(new ModuleName(qualifiedName), null);
				try
				{
					new EditModuleTask(workbench, resolvedModuleName)
						.executeTask();
				}
				catch (Exception e)
				{
					//We tried...
				}
			}
			catch (UnresolvedDependencyException e)
			{
				//Don't bother opening it.
			}
		});
	}

	/**
	 * Construct a new {@link NewPackageTask}.
	 *
	 * @param workbench
	 *        The owning {@link AvailWorkbench}.
	 * @param directory
	 *        The {@link File} directory this new module will be placed in.
	 * @param baseQualifiedName
	 *        The base portion of the qualified name.
	 * @param width
	 *        The width of the JFrame.
	 * @param height
	 *        The height of the JFrame.
	 */
	public NewPackageTask (
		final AvailWorkbench workbench,
		final @NotNull File directory,
		final @NotNull String baseQualifiedName,
		final int width,
		final int height)
	{
		super(workbench, "Create New Module", false, width, height);
		this.directory = directory;
		this.baseQualifiedName = baseQualifiedName;

	}
}
