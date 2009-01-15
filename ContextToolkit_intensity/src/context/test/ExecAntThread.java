package context.test;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class ExecAntThread extends Thread{
	private final String buildFile ;
	
	public ExecAntThread(String fileName){
		this.buildFile = fileName;
	}
	
	public void run(){
		Project p = new Project();
		p.init();
		File file = new File(buildFile);
		p.setUserProperty("ant.file", file.getAbsolutePath());
		ProjectHelper.configureProject(p, file);
		p.executeTarget(p.getDefaultTarget());
	}

}
