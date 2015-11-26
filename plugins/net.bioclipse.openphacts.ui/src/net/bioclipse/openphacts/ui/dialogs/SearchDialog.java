package net.bioclipse.openphacts.ui.dialogs;

import java.util.List;

import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.openphacts.business.IOpenphactsManager;
import net.bioclipse.openphacts.model.Resource;
import net.bioclipse.openphacts.ui.Activator;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * @author ola
 *
 */
public class SearchDialog extends TitleAreaDialog {

	private Text searchText;
	private TableViewer resultViewer;

	public SearchDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		setTitle("Search Open PHACTS");
		setMessage("\n\nSearch the Open PHACTS infrastructure");
		setTitleImage(Activator.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "icons/OPS_logo_small.jpg").createImage());
		
		final IOpenphactsManager openphacts = 
				net.bioclipse.openphacts.Activator.getDefault().getJavaOpenphactsManager();
		
		// create the top level composite for the dialog area
		Composite compositeTOP = new Composite(parent, SWT.RESIZE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		compositeTOP.setLayout(layout);
		compositeTOP.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		compositeTOP.setFont(parent.getFont());
		// Build the separator line
		Label titleBarSeparator2 = new Label(compositeTOP, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		titleBarSeparator2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layoutC = new GridLayout();
		layoutC.numColumns=3;
		composite.setLayout(layoutC);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		
		//Label is fixed minimum size
		Label label1 = new Label(composite, SWT.NONE);
		label1.setText("Query:");
		GridData gd2 = new GridData();
		label1.setLayoutData(gd2);

		
		// The text fields will grow with the size of the dialog
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;


		searchText = new Text(composite, SWT.BORDER);
		searchText.setLayoutData(gridData);

		Button btnSearch = new Button(composite, SWT.NONE);
		btnSearch.setText("Search");
		btnSearch.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<Resource> res;
				try {
					res = openphacts.lookUpCompounds(searchText.getText());
					
					System.out.println(res);
				} catch (BioclipseException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		
		//Viewer
		//======
		resultViewer = new TableViewer(composite);
		GridData gridData2 = new GridData(GridData.FILL_BOTH);
		gridData2.horizontalSpan=3;
		resultViewer.getTable().setLayoutData(gridData2);
		resultViewer.setContentProvider(new ArrayContentProvider());

        TableLayout tableLayout = new TableLayout();
        resultViewer.getTable().setLayout(tableLayout);

        //Add columns
        TableViewerColumn molCol=new TableViewerColumn(resultViewer, SWT.NONE);
        molCol.getColumn().setText("Name");
        tableLayout.addColumnData(new ColumnPixelData(250));
        molCol.setLabelProvider(new ColumnLabelProvider());
        
		return searchText;

	} 
	
	
	
	@Override
	protected void okPressed() {
		validate();
		
		//Do search
		super.okPressed();
	}
	
	
	private void validate(){
		setErrorMessage(null);
		if (resultViewer.getSelection()==null)
			setErrorMessage("Nothing selected in results viewer");
		getButtonBar().update();
	}

}
