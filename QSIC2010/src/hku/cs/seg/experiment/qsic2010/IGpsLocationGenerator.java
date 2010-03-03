package hku.cs.seg.experiment.qsic2010;
import java.sql.Timestamp;

public interface IGpsLocationGenerator {
	void init();
	void generate();
	double getLongitude();
	double getLatitude();
	int getContextDiversity();
	Timestamp getTimestamp();
}
