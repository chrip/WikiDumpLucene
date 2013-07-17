
import org.apache.lucene.search.similarities.DefaultSimilarity;

public class ESASimilarity extends DefaultSimilarity {

	public float idf(int docFreq, int numDocs) {
		return (float) Math.log(numDocs / (double) docFreq);
	}
	
	@Override
	public float tf(float freq) {
		return (float) (1.0 + Math.log(freq));
	}

}
