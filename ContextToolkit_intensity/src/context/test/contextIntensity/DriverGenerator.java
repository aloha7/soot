package context.test.contextIntensity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import context.test.util.Constant;

public class DriverGenerator {
	private Vector cfgs;
	private static DriverGenerator gen= null;
	
	public static DriverGenerator getInstance(){
		if(gen == null){
			gen = new DriverGenerator();
		}
		return gen;
	}
	
	private DriverGenerator() {
		this(Constant.baseFolder + "/ContextIntensity/handlerCFGs.txt");
	}

	private DriverGenerator(String fileName) {
		cfgs = new Vector();

		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = br.readLine()) != null) { // construct cfg from
				// files
				int index = line.indexOf("\t");
				if (index > -1) {// extract id of the cfg
					String id_cfg = line.substring(0, index);
					CFG cfg = new CFG(id_cfg);
					line = line.substring(index + "\t".length());
					index = line.indexOf(":");
					while (index > -1) { // extract nodes and edges
						String srcID = line.substring(0, line.indexOf(":"));
						Node src = new Node(srcID);
						cfg.N.add(src);
						String destID;

						if (line.indexOf("\t") == -1) { // there maybe no "\t"
							// in the last edge
							destID = line.substring(line.indexOf(":")
									+ ":".length(), line.length());
						} else {
							destID = line.substring(line.indexOf(":")
									+ ":".length(), line.indexOf("\t"));
						}
						if (destID.indexOf(",") > -1) { // several successores
							// for one source
							String[] destIDs = destID.split(",");
							for (int i = 0; i < destIDs.length; i++) {
								Node dest = new Node(destIDs[i]);
								cfg.N.add(dest);
								Edge edge = new Edge(src, dest);
								cfg.E.add(edge);
							}
						} else { // only one successor for one source
							Node dest = new Node(destID);
							cfg.N.add(dest);
							Edge edge = new Edge(src, dest);
							cfg.E.add(edge);
						}
						if (line.indexOf("\t") > -1) {
							line = line.substring(line.indexOf("\t")
									+ "\t".length());
							index = line.indexOf(":");
						} else { // the end of the CFG
							cfgs.add(cfg);
							break;
						}
					}

				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CFG getCFG(String id) {
		for (int i = 0; i < cfgs.size(); i++) {
			CFG cfg = (CFG) cfgs.get(i);
			if (cfg.ID.equals(id)) {
				return cfg;
			}
		}
		return null;
	}

	public Vector getAllkSwitchSequence(int k) {

		Vector switches = new Vector();
		String[] choices_cfg = new String[cfgs.size()];
		for (int i = 0; i < choices_cfg.length; i++) {
			choices_cfg[i] = ((CFG) cfgs.get(i)).ID;
		}

		if (k == 1) {
			for (int i = 0; i < choices_cfg.length; i++) {
				for (int j = 0; j < choices_cfg.length; j++) {
					Switches seqs = new Switches(k);
					seqs.addSwitch(choices_cfg[i]);
					seqs.addSwitch(choices_cfg[j]);
					switches.add(seqs);
				}
			}
		}

		if (k == 2) {
			for (int i = 0; i < choices_cfg.length; i++) {
				for (int j = 0; j < choices_cfg.length; j++) {
					for (int p = 0; p < choices_cfg.length; p++) {
						Switches seqs = new Switches(k);
						seqs.addSwitch(choices_cfg[i]);
						seqs.addSwitch(choices_cfg[j]);
						seqs.addSwitch(choices_cfg[p]);
						switches.add(seqs);
					}
				}
			}
		}
		return switches;
	}
	
//	public DriverSet StockFromCappDGenerator(int k){
//		DriverSet k_drivers = new DriverSet();
//		Vector kswitches = this.getAllkSwitchSequence(k);
//		
//	}
	
	public DriverSet StockDGenerator(int k) {
		DriverSet k_drivers = new DriverSet();
		Vector kswitches = this.getAllkSwitchSequence(k); // get all k-switch
		// sequences

		while (kswitches.size() != 0) {
			Switches kSwitchSeq = (Switches) kswitches.remove(0);
			Vector duplicate = kSwitchSeq.findDuplicate(); // if current switch
			// contains switches
			// among the same
			// type
			Driver driver = new Driver();
			CFG currCFG;
			int index_Switch = -1;
			do {
				index_Switch ++;
				
				currCFG = this.getCFG(kSwitchSeq.getSwitch(index_Switch)); //get CFG according to its id 
				Node caff = currCFG.BFSToGetCapp("start"); //find the context-aware program point in CFG

				while (!caff.index.equals("end")) {//not the end of the CFG
					driver.add(currCFG.ID + caff.index);//each item in the driver is "cfg id"+"statement id"
					if (this.timeToSwitch()) {
						index_Switch++;
						currCFG = this.getCFG(kSwitchSeq
								.getSwitch(index_Switch));
					}
					caff = currCFG.BFSToGetCapp("start");//must start at tree root in case of missing some nodes 
				}
			} while (index_Switch != (kSwitchSeq.getLength() - 1));
			this.refreshCFGs();
			k_drivers.addDriver(driver);
		}

		return k_drivers;
	}

	public void refreshCFGs(){
		for(int i = 0; i < cfgs.size(); i ++){
			CFG cfg = (CFG)cfgs.get(i);
			cfg.refresh();
		}
	}
	
	
	public boolean timeToSwitch() {//time to switch to another CFG
		Random rand = new Random();
		int num = rand.nextInt(1);
		if (num == 0)
			return false;
		else
			return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DriverGenerator gen = new DriverGenerator();
		int k = 1;
		Vector switchSeq = gen.getAllkSwitchSequence(k);
		DriverSet drivers = gen.StockDGenerator(k);
		drivers.writeToFile(Constant.baseFolder + "/ContextIntensity/Drivers_Stoc1.txt");
//		drivers.writeToFile(Constant.baseFolder + "/ContextIntensity/Drivers_Stoc2.txt");
		System.out.println("Hi");
	}

	// public

}
