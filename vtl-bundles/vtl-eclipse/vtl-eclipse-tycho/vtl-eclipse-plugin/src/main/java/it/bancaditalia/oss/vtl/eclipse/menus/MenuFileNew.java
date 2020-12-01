package it.bancaditalia.oss.vtl.eclipse.menus;

import static it.bancaditalia.oss.vtl.eclipse.parts.NavigatorPart.NAVIGATOR_PART_ID;
import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import it.bancaditalia.oss.vtl.eclipse.impl.projects.VTLFolder;
import it.bancaditalia.oss.vtl.eclipse.impl.projects.VTLProject;
import it.bancaditalia.oss.vtl.eclipse.impl.projects.VTLScript;
import it.bancaditalia.oss.vtl.eclipse.parts.NavigatorPart;
import it.bancaditalia.oss.vtl.eclipse.wizards.NewVTLScriptSelectorPage;

public class MenuFileNew
{
	@Inject
	private EPartService partService;

	@Execute
	public void execute(Shell shell)
	{
		NavigatorPart navigator = ((NavigatorPart) partService.findPart(NAVIGATOR_PART_ID).getObject());
		Wizard wizard = new Wizard() {
			@Override
			public boolean performFinish()
			{
				return true;
			}
		};
		wizard.addPage(new NewVTLScriptSelectorPage(navigator.getProjects(), navigator.getSelection()));
		if (new WizardDialog(shell, wizard).open() == WizardDialog.OK)
		{
			navigator.addItemToSelection(new VTLScript("New VTL Script"));
			MPart editorPart = partService.createPart("vtl-eclipse-app.editor.template");
			partService.showPart(editorPart, ACTIVATE);	
		}
	}
	
	@CanExecute
	public boolean canExecute()
	{
		NavigatorPart navigator = ((NavigatorPart) partService.findPart(NAVIGATOR_PART_ID).getObject());
		Object selected = navigator.getSelection().getPaths()[0].getLastSegment();
		return selected instanceof VTLProject || selected instanceof VTLFolder;
	}
}
