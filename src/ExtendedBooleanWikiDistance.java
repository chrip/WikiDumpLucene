import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
//import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;




public class ExtendedBooleanWikiDistance {

	static String field = "contents";

	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;

	static ESASimilarity esaSimilarity;
 //	static DefaultSimilarity esaSimilarity;
	static Statistics _stats;

	public static void evaluate(Statistics stats) throws IOException, ParseException {
		_stats = stats;
		
		reader = DirectoryReader.open(FSDirectory.open(new File(_stats.indexPath)));
		searcher = new IndexSearcher(reader);
//		esaSimilarity = new DefaultSimilarity();
		esaSimilarity = new ESASimilarity();
		searcher.setSimilarity(esaSimilarity);


		Analyzer analyzer = new WikipediaAnalyzer(_stats);
//		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
		parser = new QueryParser(Version.LUCENE_43, field, analyzer);

		
		for(String datasetName: _stats.datasets) {
			for(String algotithm : _stats.algorithms) {
				String line;
				FileReader is = new FileReader(_stats.getInputPath(datasetName));
				BufferedReader br = new BufferedReader(is);

				
				LineNumberReader  lnr = new LineNumberReader(new FileReader(_stats.getInputPath(datasetName)));
				lnr.skip(Long.MAX_VALUE);
				int n = lnr.getLineNumber();
				lnr.close();
				double[] xArray = new double[n];
				double[] yArray = new double[n];
				
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_stats.getOutputPath(algotithm, datasetName)),"UTF-8"));
				
				int i = 0;
				while((line = br.readLine()) != null){
					final String [] parts = line.split(";");
					if(parts.length != 3) {
						break;
					}
					if(datasetName.equals("mc") || datasetName.equals("rg") || datasetName.equals("wordsim")) {
						xArray[i] = Double.valueOf(parts[2]);
					}
					if(algotithm.equals("NWD")) {
						yArray[i] = wikipediaDistance(parts[0], parts[1]);
						
					}
					else if(algotithm.equals("SNWD")) {
//						yArray[i] = cosineDistance(parts[0], parts[1]);
//						yArray[i] = GabrilovichEtAl(parts[0], parts[1]);
						yArray[i] = ExtendedBooleanGabrilovichEtAl(parts[0], parts[1]);
					}
					else if(algotithm.equals("ESA")) {
						yArray[i] = GabrilovichEtAl(parts[0], parts[1]);
					}
//					else if(useExtendedWikiDistance) {
//						yArray[i] = extendedBooleanWikipediaDistance(parts[0], parts[1]);
//					}
					else if(algotithm.equals("luceneScoreWikiDistance")) {
						yArray[i] = scoredWikipediaDistance(parts[0], parts[1]);
					}
					if(yArray[i] < 0 || yArray[i] > 1) {
						System.out.println("correlation: " + algotithm + " " + yArray[i] + " " + datasetName + " " + parts[0] + " " + parts[1]);
					}
					bw.write(line + ";" + _stats.myFormatter.format(yArray[i]) + "\n");
					i++;
				}
				br.close();
				bw.close();
				
				if(datasetName.equals("mc") || datasetName.equals("rg") || datasetName.equals("wordsim")) {
					SpearmansCorrelation sc = new SpearmansCorrelation();
					double co1 = sc.correlation(xArray, yArray);					
					_stats.setSpearmansCorrelation(co1, datasetName, algotithm);
					
					PearsonsCorrelation pc = new PearsonsCorrelation();
					double co2 = pc.correlation(xArray, yArray);
					_stats.setPearsonsCorrelation(co2, datasetName, algotithm);
				}
				
				_stats.numberOfDocs = reader.numDocs();
	
//				System.out.println("correlation: " + co + " " + tfidfThreshold + " " + freqThreshold + " " + WINDOW_THRES);
			}

		}
		
	}

	public static double wikipediaDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);

		BooleanQuery combiQuery0 = new BooleanQuery();
		combiQuery0.add(query0, BooleanClause.Occur.MUST);
		TopDocs results0 = searcher.search(combiQuery0, 1);

		BooleanQuery combiQuery1 = new BooleanQuery();
		combiQuery1.add(query1, BooleanClause.Occur.MUST);
		TopDocs results1 = searcher.search(combiQuery1, 1);

		BooleanQuery query0AND1 = new BooleanQuery();
		query0AND1.add(combiQuery0, BooleanClause.Occur.MUST);
		query0AND1.add(combiQuery1, BooleanClause.Occur.MUST);

		TopDocs results0AND1 = searcher.search(query0AND1, 1);
		
		if(results0.totalHits < 1 || results1.totalHits < 1|| results0AND1.totalHits < 1) {
			return 0;
		}

		double log0, log1 , logCommon, maxlog, minlog;
		log0 = Math.log(results0.totalHits);
		log1 = Math.log(results1.totalHits);
		logCommon = Math.log(results0AND1.totalHits);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return 1 - 0.5 * (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 

	}

	public static double extendedBooleanWikipediaDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);


		Sort sort0 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc0 = TopFieldCollector.create(sort0, reader.numDocs(), true, true, true, true); 
		Sort sort1 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc1 = TopFieldCollector.create(sort1, reader.numDocs(), true, true, true, true); 

		searcher.search(query0, tfc0);
		TopDocs results0 = tfc0.topDocs();
		searcher.search(query1, tfc1);
		TopDocs results1 = tfc1.topDocs();

		if(tfc0.getTotalHits() < 1 || tfc1.getTotalHits() < 1) {
			return 0;
		}
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(results0));
		log1 = Math.log(sumScores(results1));	
		logCommon = Math.log(sumExtendedBoolScores(results0, results1));
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return 1 - 0.5 * (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 
	}

	private static double sumScores(TopDocs results) {
		double sum = 0.0; 
		double maxScore = results.getMaxScore();

		for (ScoreDoc d: results.scoreDocs) {
			sum += (d.score/maxScore);
		}
		return sum;
	}

	private static double sumExtendedBoolScores(TopDocs results0, TopDocs results1){
		double sum = 0;
		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		int i = 0, j = 0;

		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
				double sim1AND2 = 1 - Math.sqrt((Math.pow(1 - results0.scoreDocs[i].score/maxScore0, 2) + 1) / 2.0);
				sum += sim1AND2;
				i++;
			}
			else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				double sim1AND2 = 1 - Math.sqrt((Math.pow(1 - results0.scoreDocs[i].score/maxScore0, 2) + Math.pow(1 - results1.scoreDocs[j].score/maxScore1, 2)) / 2.0);
				sum += sim1AND2;
				i++;
				j++;
			}
			else {
				double sim1AND2 = 1 - Math.sqrt((1 + Math.pow(1 - results1.scoreDocs[j].score/maxScore1, 2)) / 2.0);
				sum += sim1AND2;
				j++;
			}
		}

		while (i < results0.scoreDocs.length) {
			double sim1AND2 = 1 - Math.sqrt((Math.pow(1 - results0.scoreDocs[i].score/maxScore0, 2) + 1) / 2.0);
			sum += sim1AND2;
			i++;

		}

		while (j < results1.scoreDocs.length) {
			double sim1AND2 = 1 - Math.sqrt((1 + Math.pow(1 - results1.scoreDocs[j].score/maxScore1, 2)) / 2.0);
			sum += sim1AND2;
			j++;
		}
		return sum;
	}

	public static double cosineDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);

		Sort sort0 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc0 = TopFieldCollector.create(sort0, reader.numDocs(), true, true, true, true); 
		Sort sort1 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc1 = TopFieldCollector.create(sort1, reader.numDocs(), true, true, true, true); 

		searcher.search(query0, tfc0);
		TopDocs results0 = tfc0.topDocs();
		searcher.search(query1, tfc1);
		TopDocs results1 = tfc1.topDocs();

		return cosine(results0, results1);
	}

	private static double cosine(TopDocs results0, TopDocs results1){
		double scalar = 0.0d, r0Norm=0.0d, r1Norm=0.0d;

		int i = 0, j = 0;

		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			double score0 = results0.scoreDocs[i].score/maxScore0;
			double score1 = results1.scoreDocs[j].score/maxScore1;

			if(score0 < _stats.tfidfThreshold) {
				score0 = 0.0;	        	
			}
			if(score1 < _stats.tfidfThreshold) {
				score1 = 0.0;
			}
			if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
				r0Norm += Math.pow(score0, 2);
				i++;
			}
			else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				scalar += results0.scoreDocs[i].score * results1.scoreDocs[j].score;
				r0Norm += Math.pow(score0, 2);
				r1Norm += Math.pow(score1, 2);
				i++;
				j++;
			}
			else {
				r1Norm += Math.pow(score1, 2);
				j++;
			}
		}
		while (i < results0.scoreDocs.length) {
			double score0 = results0.scoreDocs[i].score/maxScore0;
			if(score0 < _stats.tfidfThreshold) {
				score0 = 0.0;	        	
			}
			r0Norm += Math.pow(score0, 2);
			i++;

		}
		while (j < results1.scoreDocs.length) {
			double score1 = results1.scoreDocs[j].score/maxScore1;
			if(score1 < _stats.tfidfThreshold) {
				score1 = 0.0;
			}
			r1Norm += Math.pow(score1, 2);
			j++;
		}
		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
		if(r0Norm == 0 || r1Norm == 0){
			return 0;
		}
		return scalar / (r0Norm * r1Norm);
	}


	public static double scoredWikipediaDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);

		Sort sort0 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc0 = TopFieldCollector.create(sort0, reader.numDocs(), true, true, true, true); 
		Sort sort1 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc1 = TopFieldCollector.create(sort1, reader.numDocs(), true, true, true, true); 

		searcher.search(query0, tfc0);
		TopDocs results0 = tfc0.topDocs();
		searcher.search(query1, tfc1);
		TopDocs results1 = tfc1.topDocs();

		if(tfc0.getTotalHits() < 1 || tfc1.getTotalHits() < 1) {
			return 0;
		}

		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(results0));
		log1 = Math.log(sumScores(results1));
        double commonScore = sumCommonScores(results0, results1);
		if(commonScore == 0) {
			return 0;
		}
		logCommon = Math.log(commonScore);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return 1 - 0.5 * (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 
	}


	private static double sumCommonScores(TopDocs results0, TopDocs results1){
		double sum = 0;
		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		int i = 0, j = 0;

		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
				i++;
			}
			else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				if(results0.scoreDocs[i].score > _stats.tfidfThreshold/maxScore0 && results1.scoreDocs[j].score/maxScore1 > _stats.tfidfThreshold) {
					double sim1AND2 = (results0.scoreDocs[i].score/maxScore0)  * (results1.scoreDocs[j].score/maxScore1);
					sum += sim1AND2;
				}
				i++;
				j++;
			}
			else {
				j++;
			}
		}
		return sum;
	}

	public static double GabrilovichEtAl(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);		

		AtomicReader ar = reader.leaves().get(0).reader();	

		int totalDocs = reader.numDocs();

		String term0Parsed = query0.toString();
		String term1Parsed = query1.toString();
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed.substring(field.length()+1));
		Term t1 = new Term(field, term1Parsed.substring(field.length()+1));
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);

		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0;
		List<IdScorePair> prunedVector0 = getIndexPruningThreshold(docEnum0, idf0);
		List<IdScorePair> prunedVector1 = getIndexPruningThreshold(docEnum1, idf1);
		
		int i = 0;
		int j = 0;
		int docid0 = 0;
		int docid1 = 0;
		while (i < prunedVector0.size() && j < prunedVector1.size()) {

			docid0 = prunedVector0.get(i).id;
			docid1 = prunedVector1.get(j).id;
			double tfidf0 = prunedVector0.get(i).score;
			double tfidf1 = prunedVector1.get(j).score;

			if (docFreq0 < _stats.freqThreshold || tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			if (docFreq1 < _stats.freqThreshold || tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			
			
			if (docid0 < docid1) {
				r0Norm += Math.pow(tfidf0, 2);
				docid0 = prunedVector0.get(i).id;
				i++;
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += Math.pow(tfidf0, 2);
				r1Norm += Math.pow(tfidf1, 2);
				docid0 = prunedVector0.get(i).id;
				docid1 = prunedVector1.get(j).id;
				i++;
				j++;
			}
			else {
				r1Norm += Math.pow(tfidf1, 2);
				docid1 = prunedVector1.get(j).id;
				j++;
			}

		}
		while (i < prunedVector0.size()) {
			double tfidf0 = prunedVector0.get(i).score;

			if (docFreq0 < _stats.freqThreshold || tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += Math.pow(tfidf0, 2);
			i++;
		}
		while (j < prunedVector1.size()) {
			double tfidf1 = prunedVector1.get(j).score;

			if (docFreq1 < _stats.freqThreshold || tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += Math.pow(tfidf1, 2);
			j++;
		}

		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
		if(r0Norm == 0 || r1Norm == 0){
			return 0;
		}
		return scalar / (r0Norm * r1Norm); 
	}
	
	private static List<IdScorePair> getIndexPruningThreshold(DocsEnum docEnum, double idf) throws IOException {
		List<IdScorePair> termVector = new ArrayList<IdScorePair>();
		if (docEnum == null) {
			return termVector;
		}
		int docid = docEnum.nextDoc();
		double sum = 0;
		while (docid != DocsEnum.NO_MORE_DOCS) {
			int termFreq = docEnum.freq();
			double tfidf = esaSimilarity.tf(termFreq) * idf;
			sum+= tfidf * tfidf;
			termVector.add(new IdScorePair(docid, tfidf));
			docid = docEnum.nextDoc();
		}
		sum = Math.sqrt(sum);
		
		// normalize vector
		for(IdScorePair p:termVector) {
			p.score = p.score/sum;
		}
		
		if(_stats.indexPruning) {
			Collections.sort(termVector, new ScoreComparator());
			
			int mark = 0;
			int windowMark = 0;
			double score = 0;
			double highest = 0;
			double first = 0;
			double last = 0;
			
			double[] window = new double[_stats.WINDOW_SIZE];
	
			for (int j = 0; j < termVector.size(); j++) {
				score = termVector.get(j).score;
	
				// sliding window
	
				window[windowMark] = score;
	
				if (mark == 0) {
					highest = score;
					first = score;
				}
	
				if (mark < _stats.WINDOW_SIZE) {
					// fill window
				} else if (highest * _stats.WINDOW_THRES < (first - last)) {
					// ok
	
					if (windowMark < _stats.WINDOW_SIZE - 1) {
						first = window[windowMark + 1];
					} else {
						first = window[0];
					}
				} else {
					// truncate
					termVector = termVector.subList(0, j);
					break;
				}
	
				last = score;
	
				mark++;
				windowMark++;
	
				windowMark = windowMark % _stats.WINDOW_SIZE;
	
			}
		}
		Collections.sort(termVector, new IdComparator());
		
		return termVector;
	}

	public static double ExtendedBooleanGabrilovichEtAl(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);		

		AtomicReader ar = reader.leaves().get(0).reader();	

		int totalDocs = reader.numDocs();

		String term0Parsed = query0.toString();
		String term1Parsed = query1.toString();
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed.substring(field.length()+1));
		Term t1 = new Term(field, term1Parsed.substring(field.length()+1));
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);

		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0;
		List<IdScorePair> prunedVector0 = getIndexPruningThreshold(docEnum0, idf0);
		List<IdScorePair> prunedVector1 = getIndexPruningThreshold(docEnum1, idf1);
		
		int i = 0;
		int j = 0;
		int docid0 = 0;
		int docid1 = 0;
		while (i < prunedVector0.size() && j < prunedVector1.size()) {

			docid0 = prunedVector0.get(i).id;
			docid1 = prunedVector1.get(j).id;
			double tfidf0 = prunedVector0.get(i).score;
			double tfidf1 = prunedVector1.get(j).score;

			if (docFreq0 < _stats.freqThreshold || tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			if (docFreq1 < _stats.freqThreshold || tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			
			
			if (docid0 < docid1) {
				r0Norm += tfidf0;
				i++;
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += tfidf0;
				r1Norm += tfidf1;
				i++;
				j++;
			}
			else {
				r1Norm +=tfidf1;
				j++;
			}

		}
		while (i < prunedVector0.size()) {
			double tfidf0 = prunedVector0.get(i).score;

			if (docFreq0 < _stats.freqThreshold || tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += tfidf0;
			i++;
		}
		while (j < prunedVector1.size()) {
			double tfidf1 = prunedVector1.get(j).score;

			if (docFreq1 < _stats.freqThreshold || tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += tfidf1;
			j++;
		}
		if(scalar == 0) {
			return 0;
		}
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(r0Norm);
		log1 = Math.log(r1Norm);	
		logCommon = Math.log(scalar);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);
		
		return 1 - 0.5 * (maxlog - logCommon) / (Math.log(totalDocs) - minlog); 
	}
	
}

class IdScorePair {
    public int id;
    public double score;

    public IdScorePair(int id, double score) {
        this.id = id;
        this.score = score;
    }
}
class IdComparator implements Comparator<IdScorePair> {
    public int compare(IdScorePair idScorePair1, IdScorePair idScorePair2) {
        return idScorePair1.id - idScorePair2.id;
    }
}
class ScoreComparator implements Comparator<IdScorePair> {
    public int compare(IdScorePair idScorePair1, IdScorePair idScorePair2) {
        return (idScorePair1.score < idScorePair2.score)?1:-1;
    }
}
