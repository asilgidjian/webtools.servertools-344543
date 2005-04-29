/**********************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.jst.server.tomcat.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IWebModule;
/**
 * Tomcat 32 handler.
 */
public class Tomcat32Handler implements ITomcatVersionHandler {
	/**
	 * @see ITomcatVersionHandler#verifyInstallPath(IPath)
	 */
	public boolean verifyInstallPath(IPath installPath) {
		return TomcatPlugin.verifyInstallPath(installPath, TomcatPlugin.TOMCAT_32);
	}
	
	/**
	 * @see ITomcatVersionHandler#getRuntimeClass()
	 */
	public String getRuntimeClass() {
		return "org.apache.tomcat.startup.Tomcat";
	}

	/**
	 * @see ITomcatVersionHandler#getRuntimeClasspath(IPath)
	 */
	public List getRuntimeClasspath(IPath installPath) {
		List cp = new ArrayList();
		// add all jars from the Tomcat lib directory
		File libDir = installPath.append("lib").toFile();
		if (libDir.exists()) {
			String[] libs = libDir.list();
			for (int i = 0; i < libs.length; i++) {
				if (libs[i].endsWith("jar")) {
					IPath path = installPath.append("lib").append(libs[i]);
					cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path));
				}
			}
		}

		return cp;
	}

	/**
	 * @see ITomcatVersionHandler#getRuntimeProgramArguments(IPath, boolean, boolean)
	 */
	public String[] getRuntimeProgramArguments(IPath configPath, boolean debug, boolean starting) {
		List list = new ArrayList();
		if (configPath != null) {
			list.add("-f \"" + configPath.append("conf").append("server.xml").toOSString() + "\"");
		}
		
		if (!starting)
			list.add("-stop");
		
		String[] s = new String[list.size()];
		list.toArray(s);
		return s;
	}

	/**
	 * @see ITomcatVersionHandler#getRuntimeVMArguments(IPath, IPath, boolean, boolean)
	 */
	public String[] getRuntimeVMArguments(IPath installPath, IPath configPath, boolean isTestEnv, boolean isSecure) {
		List list = new ArrayList();
		list.add("-Dtomcat.home=\"" + installPath.toOSString() + "\"");
		
		// run in secure mode
		if (isSecure) {
			list.add("-Djava.security.manager");
			IPath dir = configPath.append("conf").append("tomcat.policy");
			list.add("-Djava.security.policy=\"" + dir.toOSString() + "\"");
		}
		
		String[] s = new String[list.size()];
		list.toArray(s);
		return s;
	}
	
	/**
	 * @see ITomcatVersionHandler#canAddModule(IWebModule)
	 */
	public IStatus canAddModule(IWebModule module) {
		if ("1.2".equals(module.getJ2EESpecificationVersion()))
			return new Status(IStatus.OK, TomcatPlugin.PLUGIN_ID, 0, Messages.canAddModule, null);
		
		return new Status(IStatus.ERROR, TomcatPlugin.PLUGIN_ID, 0, Messages.errorSpec32, null);
	}
}