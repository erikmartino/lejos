package org.lejos.nxt.ldt.wizard;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.lejos.nxt.ldt.container.LeJOSLibContainer;
import org.lejos.nxt.ldt.util.LeJOSNXJUtil;

public class NewPCProjectPageOne extends NewJavaProjectWizardPageOne {
	
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] oldEntries = super.getDefaultClasspathEntries();
		Path lcp = new Path(LeJOSLibContainer.ID+"/"+LeJOSNXJUtil.LIBSUBDIR_PC);
		
		int len = oldEntries.length;
		IClasspathEntry[] newEntries = new IClasspathEntry[len + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, len);
		newEntries[len] = JavaCore.newContainerEntry(lcp);
		
		return newEntries;
	}
}
