package IOOperation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteFile {

	public static void main(String[] args) {
		String saveFile = "C:\\Jack\\workspace\\QSIC2010\\run1-3.sh";
		
		StringBuilder sb = new StringBuilder();
		sb.append("echo Testing Mutant 1\n");
		sb.append("\\cp -f ../mutants/1/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		sb.append("java hku.cs.seg.experiment.qsic2010.TestLauncher 1 ../testpool/100.pool ../testresult/1.log\n");
		sb.append("\\cp -f ../goldenversion/c7302/CityAcitivyRecommendation/Accommodation/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		
		sb.append("echo Testing Mutant 2\n");
		sb.append("\\cp -f ../mutants/2/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		sb.append("java hku.cs.seg.experiment.qsic2010.TestLauncher 2 ../testpool/100.pool ../testresult/1.log\n");
		sb.append("\\cp -f ../goldenversion/c7302/CityAcitivyRecommendation/Accommodation/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		
		sb.append("echo Testing Mutant 3\n");
		sb.append("\\cp -f ../mutants/3/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		sb.append("java hku.cs.seg.experiment.qsic2010.TestLauncher 3 ../testpool/100.pool ../testresult/1.log\n");
		sb.append("\\cp -f ../goldenversion/c7302/CityAcitivyRecommendation/Accommodation/*.class c7302/CityAcitivyRecommendation/Accommodation\n");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
