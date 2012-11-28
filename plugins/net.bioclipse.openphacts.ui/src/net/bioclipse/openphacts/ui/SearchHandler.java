package net.bioclipse.openphacts.ui;

import net.bioclipse.openphacts.ui.dialogs.SearchDialog;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * @author ola
 */
public class SearchHandler extends AbstractHandler {

	private static final Logger logger = Logger.getLogger(SearchHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SearchDialog sDialog = 
				new SearchDialog( PlatformUI
						.getWorkbench()
						.getActiveWorkbenchWindow()
						.getShell() );

		sDialog.open();
		if(sDialog .getReturnCode() == Window.OK) {
			logger.debug("SearchAction succeeded");
		}

		return null;
	}


}
