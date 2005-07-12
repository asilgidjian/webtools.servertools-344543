/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.core.internal;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.*;
/**
 * 
 */
public class ServerType implements IServerType {
	protected IConfigurationElement element;

	/**
	 * ServerType constructor comment.
	 * 
	 * @param element a configuration element
	 */
	public ServerType(IConfigurationElement element) {
		super();
		this.element = element;
	}
	
	protected IConfigurationElement getElement() {
		return element;
	}

	/**
	 * Returns the id of this factory.
	 *
	 * @return java.lang.String
	 */
	public String getId() {
		return element.getAttribute("id");
	}
	
	public String getName() {
		return element.getAttribute("name");
	}

	public boolean startBeforePublish() {
		return "true".equals(element.getAttribute("startBeforePublish"));
	}

	public String getDescription() {
		return element.getAttribute("description");
	}
	
	public IRuntimeType getRuntimeType() {
		String typeId = element.getAttribute("runtimeTypeId");
		if (typeId == null)
			return null;
		return ServerCore.findRuntimeType(typeId);
	}

	public boolean hasRuntime() {
		String s = element.getAttribute("runtime");
		return "true".equals(s);
	}
	
	public ILaunchConfigurationType getLaunchConfigurationType() {
		String launchConfigId = element.getAttribute("launchConfigId");
		if (launchConfigId == null)
			return null;
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		return launchManager.getLaunchConfigurationType(launchConfigId);
	}
	
	/**
	 * Returns true if this server can start or may already be started
	 * in the given mode, and false if not. Uses the launchMode attribute,
	 * which may contain the strings "run", "debug", and/or "profile".
	 * 
	 * @param launchMode String
	 * @return boolean
	 */
	public boolean supportsLaunchMode(String launchMode) {
		ILaunchConfigurationType configType = getLaunchConfigurationType();
		if (configType == null) {
			String mode = element.getAttribute("launchModes");
			if (mode == null)
				return false;
			return mode.indexOf(launchMode) >= 0;
		}
		return configType.supportsMode(launchMode);
	}

	/*public IServerConfigurationType getServerConfigurationType() {
		String configurationTypeId = element.getAttribute("configurationTypeId");
		return ServerCore.findServerConfigurationType(configurationTypeId);
	}*/
	
	public boolean supportsRemoteHosts() {
		String hosts = element.getAttribute("supportsRemoteHosts");
		return (hosts != null && hosts.toLowerCase().equals("true"));
	}

	public byte getInitialState() {
		String stateString = element.getAttribute("initialState");
		if (stateString != null)
			stateString = stateString.toLowerCase();
		if ("stopped".equals(stateString))
			return IServer.STATE_STOPPED;
		else if ("started".equals(stateString))
			return IServer.STATE_STARTED;
		return IServer.STATE_UNKNOWN;
	}

	public boolean hasServerConfiguration() {
		return ("true".equalsIgnoreCase(element.getAttribute("hasConfiguration")));
	}

	public IServerWorkingCopy createServer(String id, IFile file, IRuntime runtime, IProgressMonitor monitor) throws CoreException {
		if (id == null || id.length() == 0)
			id = ServerPlugin.generateId();
		ServerWorkingCopy swc = new ServerWorkingCopy(id, file, runtime, this);
		swc.setDefaults(monitor);
		swc.setRuntime(runtime);
		
		// TODO
		if (swc.getServerType().hasServerConfiguration())
			((Server)swc).importConfiguration(runtime, null);
		
		return swc;
	}
	
	/**
	 * Returns an array of all known runtime instances of
	 * the given runtime type. This convenience method filters the list of known
	 * runtime ({@link ServerCore#getRuntimes()}) for ones with a matching
	 * runtime type ({@link IRuntime#getRuntimeType()}). The array will not
	 * contain any working copies.
	 * <p>
	 * A new array is returned on each call, so clients may store or modify the result.
	 * </p>
	 * 
	 * @param runtimeType the runtime type
	 * @return a possibly-empty list of runtime instances {@link IRuntime}
	 * of the given runtime type
	 */
	protected static IRuntime[] getRuntimes(IRuntimeType runtimeType) {
		List list = new ArrayList();
		IRuntime[] runtimes = ServerCore.getRuntimes();
		if (runtimes != null) {
			int size = runtimes.length;
			for (int i = 0; i < size; i++) {
				if (runtimes[i].getRuntimeType() != null && runtimes[i].getRuntimeType().equals(runtimeType))
					list.add(runtimes[i]);
			}
		}
		
		IRuntime[] r = new IRuntime[list.size()];
		list.toArray(r);
		return r;
	}

	public IServerWorkingCopy createServer(String id, IFile file, IProgressMonitor monitor) throws CoreException {
		if (id == null || id.length() == 0)
			id = ServerPlugin.generateId();
		
		IRuntime runtime = null;
		if (hasRuntime()) {
			// look for existing runtime
			IRuntimeType runtimeType = getRuntimeType();
			IRuntime[] runtimes = getRuntimes(runtimeType);
			if (runtimes != null && runtimes.length > 0)
				runtime = runtimes[0];
			else {
				// create runtime
				try {
					IRuntimeWorkingCopy runtimeWC = runtimeType.createRuntime(id + "-runtime", monitor);
					ServerUtil.setRuntimeDefaultName(runtimeWC);
					runtime = runtimeWC;
				} catch (Exception e) {
					Trace.trace(Trace.SEVERE, "Couldn't create runtime", e);
				}
			}
		}

		ServerWorkingCopy swc = new ServerWorkingCopy(id, file, runtime, this);
		ServerUtil.setServerDefaultName(swc);
		if (runtime != null)
			swc.setRuntime(runtime);
		
		if (swc.getServerType().hasServerConfiguration())
			((Server)swc).importConfiguration(runtime, null);
		
		swc.setDefaults(monitor);
		
		return swc;
	}
	
	public static IProject getServerProject() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null) {
			int size = projects.length;
			for (int i = 0; i < size; i++) {
				if (((ProjectProperties)ServerCore.getProjectProperties(projects[i])).isServerProject())
					return projects[i];
			}
		}
		
		String s = findUnusedServerProjectName();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(s);
		project.create(null);
		project.open(null);
		((ProjectProperties)ServerCore.getProjectProperties(project)).setServerProject(true, null);
		return project;
	}

	/**
	 * Finds an unused project name to use as a server project.
	 * 
	 * @return java.lang.String
	 */
	protected static String findUnusedServerProjectName() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String name = NLS.bind(Messages.defaultServerProjectName, "");
		int count = 1;
		while (root.getProject(name).exists()) {
			name = NLS.bind(Messages.defaultServerProjectName, ++count + "");
		}
		return name;
	}

	/**
	 * Return the timeout (in ms) that should be used to wait for the server to start.
	 * Returns -1 if there is no timeout.
	 * 
	 * @return the server startup timeout
	 */
	public int getStartTimeout() {
		try {
			int i = Integer.parseInt(element.getAttribute("startTimeout"));
			int s = ServerPreferences.getInstance().getMachineSpeed();
			i = i * (10 - s) / 5;
			return i;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Return the timeout (in ms) to wait before assuming that the server
	 * has failed to stop. Returns -1 if there is no timeout.
	 * 
	 * @return the server shutdown timeout
	 */
	public int getStopTimeout() {
		try {
			return Integer.parseInt(element.getAttribute("stopTimeout"));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Return a string representation of this object.
	 * 
	 * @return java.lang.String
	 */
	public String toString() {
		return "ServerType[" + getId() + "]";
	}
}