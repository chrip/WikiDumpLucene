import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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


public class ExtendedBooleanWikiDistance {

	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/enwiki-20130604-lucene-no-stubs-5-inlinks";
	static String wordsim353Path = "/home/chrisschaefer/Arbeitsfläche/github/wikiprep-esa/esa-lucene/src/config/wordsim353-combined.tab";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	static QueryParser parserTitle;
	static boolean useWikiDistance = false;
	static boolean useExtendedWikiDistance = false;
	static boolean useCosineDistance = true;
	static boolean useScoredDistance = false;
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
		analyzer = new StandardAnalyzer(Version.LUCENE_43);
		parser = new QueryParser(Version.LUCENE_43, field, analyzer);
		parserTitle = new QueryParser(Version.LUCENE_43, "title", analyzer);
		String line;
		FileReader is = new FileReader(wordsim353Path);
		BufferedReader br = new BufferedReader(is);
		br.readLine(); //skip first line
		System.out.println("Word 1\tWord 2\tHuman (mean)\tScore");
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 3) {
				break;
			}

			if(useWikiDistance) {
				System.out.println(line + "\t" + wikipediaDistance(parts[0], parts[1]));
			}
			else if(useCosineDistance) {
				System.out.println(line + "\t" + cosineDistance(parts[0], parts[1]));
			}
			else if(useExtendedWikiDistance) {
				System.out.println(line + "\t" + (1.0 - extendedBooleanWikipediaDistance(parts[0], parts[1])));
			}
			else if(useScoredDistance) {
				System.out.println(line + "\t" + (1.0 - scoredWikipediaDistance(parts[0], parts[1])));
			}
		}
		br.close();
	}

	public static double wikipediaDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query queryTitle0 = parserTitle.parse(term0);
		Query query1 = parser.parse(term1);
		Query queryTitle1 = parserTitle.parse(term1);
		
		BooleanQuery combiQuery0 = new BooleanQuery();
		combiQuery0.add(query0, BooleanClause.Occur.SHOULD);
		combiQuery0.add(queryTitle0, BooleanClause.Occur.SHOULD);		
		TopDocs results0 = searcher.search(combiQuery0, 1);
		
		BooleanQuery combiQuery1 = new BooleanQuery();
		combiQuery1.add(query1, BooleanClause.Occur.SHOULD);
		combiQuery1.add(queryTitle1, BooleanClause.Occur.SHOULD);		
		TopDocs results1 = searcher.search(combiQuery1, 1);

		BooleanQuery query0AND1 = new BooleanQuery();
		query0AND1.add(combiQuery0, BooleanClause.Occur.MUST);
		query0AND1.add(combiQuery1, BooleanClause.Occur.MUST);

		TopDocs results0AND1 = searcher.search(query0AND1, 1);


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
	
		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
	        if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
	        	r0Norm += Math.pow(results0.scoreDocs[i].score, 2);
	            i++;
	        }
	        else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
	        	scalar += results0.scoreDocs[i].score * results1.scoreDocs[j].score;
	        	r0Norm += Math.pow(results0.scoreDocs[i].score, 2);
	        	r1Norm += Math.pow(results1.scoreDocs[j].score, 2);
	            i++;
	            j++;
	        }
	        else {
	        	r1Norm += Math.pow(results1.scoreDocs[j].score, 2);
	            j++;
	        }
	    }
	    while (i < results0.scoreDocs.length) {
	    	r0Norm += Math.pow(results0.scoreDocs[i].score, 2);
	        i++;

	    }
	    while (j < results1.scoreDocs.length) {
	    	r1Norm += Math.pow(results1.scoreDocs[j].score, 2);
	        j++;
	    }
		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
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
}

