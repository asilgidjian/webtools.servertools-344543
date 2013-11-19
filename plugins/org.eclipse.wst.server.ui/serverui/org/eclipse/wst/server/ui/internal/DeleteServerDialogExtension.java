package org.eclipse.wst.server.ui.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;

/**
 * A modifier class that allow adopter to modify the behaviour of the delete server dialog.
 * It also has places where adopter can inject custom GUI into the delete server dialog.
 */
public abstract class DeleteServerDialogExtension {

	protected IServer[] servers = null;


	/**
	 * Create the custom UI on the delete server dialog.
	 * 
	 * @param parent - parent composite.
	 */
	public abstract void createControl(Composite parent);
	
	
	/**
	* Sets servers for which the delete dialog is triggered.
	* 
	* @param servers - array of servers that will be deleted.
	*/
	public void setServers(IServer[] servers) {
		this.servers = servers;
	}
	
	/**
	 * Enables the extender to perform custom cleanup logic upon servers deletion.
	 * 
	 * @param monitor - progress monitor
	 */
	public abstract void performCustomCleanup(IProgressMonitor monitor);

}
