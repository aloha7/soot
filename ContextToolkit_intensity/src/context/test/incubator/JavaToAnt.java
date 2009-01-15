package context.test.incubator;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import context.test.util.Constant;

public class JavaToAnt {
	public static void main(String[] args){
		Project p = new Project();
		p.init();
		File f = new File(Constant.baseFolder +"build.xml");
		p.setUserProperty("ant.file", f.getAbsolutePath());
		ProjectHelper.configureProject(p, f);
		p.executeTarget(p.getDefaultTarget());
	}
}
