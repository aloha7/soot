package context.arch.generator;

import java.util.Vector;

import context.arch.widget.Widget;

public class TestCaseFeedThread implements Runnable{
	
	private Vector widgetSet;
	private String info;
	private IButtonData data;
	
	public TestCaseFeedThread(Vector widgets, String information, IButtonData datum){
		this.widgetSet = widgets;
		this.info = information;
		this.data = datum;
	}
	
	public void run(){
		for(int i = 0; i < widgetSet.size(); i ++){			
			Widget widget = (Widget) widgetSet.get(i);
			widget.notify(info, data);			
		}
	}

}
