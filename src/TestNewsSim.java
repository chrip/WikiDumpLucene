

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class TestNewsSim {


	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/enwiki-20130604-lucene-no-stubs";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	public static void main(String[] args) throws IOException, ParseException {

		String line;
		double val;
		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
		analyzer = new StandardAnalyzer(Version.LUCENE_43);
		parser = new QueryParser(Version.LUCENE_43, field, analyzer);
		
		// read Lee's newspaper articles
		FileReader is = new FileReader("/home/chrisschaefer/Dokumente/gesa/lee.cor");
		BufferedReader br = new BufferedReader(is);
		String[] leesCorpus = new String[50];
		int i = 0;
		while((line = br.readLine()) != null){
			leesCorpus[i] = line;
			//System.out.println(leesCorpus[i]);
			i++;
		}
		br.close();
		double[][] result = new double[50][50];
		for(i=0; i < 50; i++) {
			for(int j = 0; j< 50; j++) {
				if(i > j) {
					result[i][j] = 0.0;
				}
				else if (i == j) {
					result[i][j] = 1.0;
				}
				else {
					val = 1 - getRelatedness(leesCorpus[i].replace("/", "\\/"), leesCorpus[j].replace("/", "\\/"));
					if (val == -1) {
						val = 0;
					}
					result[i][j] = val;
				}
				System.out.print(result[i][j] + "\t");
			}
			System.out.print("\n");
		}		
	}
	public static double getRelatedness(String term0, String term1) throws ParseException, IOException {

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
		double sum = 0;

		int i = 0, j = 0;
	
		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
	        if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
	            i++;
	        }
	        else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				sum += results0.scoreDocs[i].score * results1.scoreDocs[j].score;
	            i++;
	            j++;
	        }
	        else {
	            j++;
	        }
	    }
		return sum / (lengthScoreVector(results0) * lengthScoreVector(results1));
	}
	
	private static double lengthScoreVector(TopDocs results) {
		double sum = 0.0; 
		for (ScoreDoc d: results.scoreDocs) {
			sum += (d.score * d.score);
		}
		return Math.sqrt(sum);
	}

}
