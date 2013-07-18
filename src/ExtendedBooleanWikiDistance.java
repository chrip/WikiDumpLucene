import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;

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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;




public class ExtendedBooleanWikiDistance {

	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/2013-06-17-lucene-no-stemming";
	static String wordsim353Path = "/home/chrisschaefer/Arbeitsfläche/github/wikiprep-esa/esa-lucene/src/config/wordsim353-combined.tab";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	static QueryParser parserTitle;
	static boolean useWikiDistance = false;
	static boolean useExtendedWikiDistance = true;
	static boolean useCosineDistance = false;
	static boolean useScoredDistance = false;
	static ESASimilarity esaSimilarity;
	static double threshold = 0.1;
	static double tfidfThreshold = 9.28;
	static int freqThreshold = 1;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {

		for(int i = 0;i < args.length;i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i+1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i+1];
				i++;
			} else if ("-wordsim353".equals(args[i])) {
				wordsim353Path = args[i+1];
				i++;
			} 
			else if ("-wikiDistance".equals(args[i])) {
				useWikiDistance = true;
				i++;
			}
			else if ("-extendedWikiDistance".equals(args[i])) {
				useExtendedWikiDistance = true;
				i++;
			}
			else if ("-cosineDistance".equals(args[i])) {
				useCosineDistance = true;
				i++;
			}
			else if ("-scoredDistance".equals(args[i])) {
				useScoredDistance = true;
				i++;
			}
		}

		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
		esaSimilarity = new ESASimilarity();
		searcher.setSimilarity(esaSimilarity);


		analyzer = new WikipediaAnalyzer();
		parser = new QueryParser(Version.LUCENE_43, field, analyzer);


		double maxPc = 0, maxTfidfThres = 0, maxFreqThres = 0, maxCorrelation = 0;
		
		for(tfidfThreshold = 9; tfidfThreshold < 11; tfidfThreshold+=0.1) {
		//	for(freqThreshold = 1;freqThreshold < 10; freqThreshold++) {
				//parserTitle = new QueryParser(Version.LUCENE_43, "title", analyzer);
				String line;
				FileReader is = new FileReader(wordsim353Path);
				BufferedReader br = new BufferedReader(is);
				br.readLine(); //skip first line
				//System.out.println("Word 1\tWord 2\tHuman (mean)\tScore");
				double[] xArray = new double[353];
				double[] yArray = new double[353];
				int i = 0;
				while((line = br.readLine()) != null){
					final String [] parts = line.split("\t");
					if(parts.length != 3) {
						break;
					}
					xArray[i] = Double.valueOf(parts[2]);
					if(useWikiDistance) {
						yArray[i] = wikipediaDistance(parts[0], parts[1]);
					}
					else if(useCosineDistance) {
						//yArray[i] = cosineDistance(parts[0], parts[1]);
						yArray[i] = GabrilovichEtAl(parts[0], parts[1]);
						//yArray[i] = ExtendedBooleanGabrilovichEtAl(parts[0], parts[1]);
					}
					else if(useExtendedWikiDistance) {
						yArray[i] = extendedBooleanWikipediaDistance(parts[0], parts[1]);
					}
					else if(useScoredDistance) {
						yArray[i] = scoredWikipediaDistance(parts[0], parts[1]);
					}
					//System.out.println(line + "\t" + yArray[i]);
					i++;
				}
				br.close();
				SpearmansCorrelation sc = new SpearmansCorrelation();
				double co = sc.correlation(xArray, yArray);
				if(co > maxCorrelation) {
					maxFreqThres = freqThreshold;
					maxTfidfThres = tfidfThreshold;
					maxCorrelation = co;
				}
				System.out.println("correlation: " + co + " " + tfidfThreshold + " " + freqThreshold);
			}
			
			//PearsonsCorrelation pc = new PearsonsCorrelation();
			//System.out.println("Pearson's linear correlation coefficient: " + pc.correlation(xArray, yArray));
				
	//	}
		System.out.println("freqThreshold: " + maxFreqThres);
		System.out.println("tfidfThreshold: " + maxTfidfThres);
		System.out.println("Spearman rank-order correlation coefficient: " + maxCorrelation);
		
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
		
		if(results0.totalHits < 1 || results0.totalHits < 1|| results0AND1.totalHits < 1) {
			return 5;
		}

		double log0, log1 , logCommon, maxlog, minlog;
		log0 = Math.log(results0.totalHits);
		log1 = Math.log(results1.totalHits);
		logCommon = Math.log(results0AND1.totalHits);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 

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
			return 5;
		}
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(results0));
		log1 = Math.log(sumScores(results1));	
		logCommon = Math.log(sumExtendedBoolScores(results0, results1));
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 
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

			if(score0 < threshold) {
				score0 = 0.0;	        	
			}
			if(score1 < threshold) {
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
			if(score0 < threshold) {
				score0 = 0.0;	        	
			}
			r0Norm += Math.pow(score0, 2);
			i++;

		}
		while (j < results1.scoreDocs.length) {
			double score1 = results1.scoreDocs[j].score/maxScore1;
			if(score1 < threshold) {
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
			return 5;
		}

		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(results0));
		log1 = Math.log(sumScores(results1));	
		logCommon = Math.log(sumCommonScores(results0, results1));
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 
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
				double sim1AND2 = (results0.scoreDocs[i].score/maxScore0)  * (results1.scoreDocs[j].score/maxScore1);
				sum += sim1AND2;
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
		String s = new String();
		int o = 0;	

		String term0Parsed = query0.toString();
		String term1Parsed = query1.toString();
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed.substring(field.length()+1));
		Term t1 = new Term(field, term1Parsed.substring(field.length()+1));
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);

		if (docFreq0 < 3 || docFreq1 < 3){
			return 0.5;
		}


		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);
		int docid0 = docEnum0.nextDoc();
		int docid1 = docEnum1.nextDoc();

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0;
				
		while (docid0 != DocsEnum.NO_MORE_DOCS && docid1 != DocsEnum.NO_MORE_DOCS) {

			double tfidf0 = esaSimilarity.tf(docEnum0.freq()) * idf0;
			double tfidf1 = esaSimilarity.tf(docEnum1.freq()) * idf1;

			if (docFreq0 < freqThreshold || tfidf0 < tfidfThreshold){
				tfidf0 = 0;
			}
			if (docFreq1 < freqThreshold || tfidf1 < tfidfThreshold){
				tfidf1 = 0;
			}
			
			if (docid0 < docid1) {
				r0Norm += Math.pow(tfidf0, 2);
				docid0 = docEnum0.nextDoc();
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += Math.pow(tfidf0, 2);
				r1Norm += Math.pow(tfidf1, 2);
				docid0 = docEnum0.nextDoc();
				docid1 = docEnum1.nextDoc();
			}
			else {
				r1Norm += Math.pow(tfidf1, 2);
				docid1 = docEnum1.nextDoc();
			}
			//			 if(tfidf0 > 10) {
				//				 o++;
				//				 s += "\t" + docid0 + "\t" + tfidf0;
				//			 }
		}
		while (docid0 != DocsEnum.NO_MORE_DOCS) {
			double tfidf0 = esaSimilarity.tf(docEnum0.freq()) * idf0;

			if (docFreq0 < freqThreshold || tfidf0 < tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += Math.pow(tfidf0, 2);
			docid0 = docEnum0.nextDoc();

		}
		while (docid1 != DocsEnum.NO_MORE_DOCS) {

			double tfidf1 = esaSimilarity.tf(docEnum1.freq()) * idf1;
			if (docFreq1 < freqThreshold || tfidf1 < tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += Math.pow(tfidf1, 2);
			docid1 = docEnum1.nextDoc();
		}
		//		System.out.println(t0.text() + "\t" + o + "\t" + s);

		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
		if(r0Norm == 0 || r1Norm == 0){
			return 0;
		}
		return scalar / (r0Norm * r1Norm); 
	}
	
	public static double ExtendedBooleanGabrilovichEtAl(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);		

		AtomicReader ar = reader.leaves().get(0).reader();	

		int totalDocs = reader.numDocs();
		String s = new String();
		int o = 0;	

		String term0Parsed = query0.toString();
		String term1Parsed = query1.toString();
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed.substring(field.length()+1));
		Term t1 = new Term(field, term1Parsed.substring(field.length()+1));
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);

		if (docFreq0 < 3 || docFreq1 < 3){
			return 0.5;
		}


		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);
		int docid0 = docEnum0.nextDoc();
		int docid1 = docEnum1.nextDoc();

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0;
				
		while (docid0 != DocsEnum.NO_MORE_DOCS && docid1 != DocsEnum.NO_MORE_DOCS) {

			double tfidf0 = esaSimilarity.tf(docEnum0.freq()) * idf0;
			double tfidf1 = esaSimilarity.tf(docEnum1.freq()) * idf1;

			if (docFreq0 < freqThreshold || tfidf0 < tfidfThreshold){
				tfidf0 = 0;
			}
			if (docFreq1 < freqThreshold || tfidf1 < tfidfThreshold){
				tfidf1 = 0;
			}
			
			if (docid0 < docid1) {
				r0Norm += tfidf0;
				docid0 = docEnum0.nextDoc();
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += tfidf0;
				r1Norm += tfidf1;
				docid0 = docEnum0.nextDoc();
				docid1 = docEnum1.nextDoc();
			}
			else {
				r1Norm += tfidf1;
				docid1 = docEnum1.nextDoc();
			}
			//			 if(tfidf0 > 10) {
				//				 o++;
				//				 s += "\t" + docid0 + "\t" + tfidf0;
				//			 }
		}
		while (docid0 != DocsEnum.NO_MORE_DOCS) {
			double tfidf0 = esaSimilarity.tf(docEnum0.freq()) * idf0;

			if (docFreq0 < freqThreshold || tfidf0 < tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += tfidf0;
			docid0 = docEnum0.nextDoc();

		}
		while (docid1 != DocsEnum.NO_MORE_DOCS) {

			double tfidf1 = esaSimilarity.tf(docEnum1.freq()) * idf1;
			if (docFreq1 < freqThreshold || tfidf1 < tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += Math.pow(tfidf1, 2);
			docid1 = docEnum1.nextDoc();
		}
		//		System.out.println(t0.text() + "\t" + o + "\t" + s);
		//scalar=Math.sqrt(r0Norm);
//		r0Norm=Math.sqrt(r0Norm);
//		r1Norm=Math.sqrt(r1Norm);

		//return scalar / (r0Norm * r1Norm); 
	    if(r0Norm == 0 || r1Norm == 0){
			return 0.5;
		}
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(r0Norm);
		log1 = Math.log(r1Norm);	
		logCommon = Math.log(scalar);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog); 
	}
}

