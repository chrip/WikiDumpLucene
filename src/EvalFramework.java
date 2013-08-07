import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.lucene.queryparser.classic.ParseException;


public class EvalFramework {

	public static void main(String[] args) throws IOException, ParseException {

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/chrisschaefer/robustness_evaluation/index.csv"),"UTF-8"));
		
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			bw.write(stats.getHeader());
			bw.flush();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-001";
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-002";
			stats.outlinkThreshold = 5;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-003";
			stats.inlinkThreshold = 5;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-004";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-005";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 1;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-006";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 2;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-007";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();		
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-008";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
			stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-009";
			stats.stemmerCalls = 3;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();

			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-010";
			stats.stemmerCalls = 3;
			stats.titleWeight = 0;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-011";
			stats.stemmerCalls = 3;
			stats.titleWeight = 0;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-012";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-013";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			stats.filterTitle = true;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-014";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			stats.filterTitle = true;
			stats.minWordLengthThreshold = 3;
			
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-015";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			stats.filterTitle = true;
			stats.minWordLengthThreshold = 3;
			stats.filterStopWords = true;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-016";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			stats.filterTitle = true;
			stats.minWordLengthThreshold = 3;
			stats.filterStopWords = true;
			stats.numberOfUniqueNonStopwordsThreshold = 100;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		{ 
			long start = System.currentTimeMillis();
			Statistics stats = new Statistics();
			
			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-017";
			stats.inlinkThreshold = 5;
			stats.outlinkThreshold = 5;
			stats.stemmerCalls = 3;
			stats.anchorText = "ALL";
			stats.filterCategories = true;
			stats.filterTitle = true;
			stats.minWordLengthThreshold = 3;
			stats.filterStopWords = true;
			stats.numberOfUniqueNonStopwordsThreshold = 100;
			stats.indexPruning = true;
			
			GabToLucene.buildIndex(stats);
			ExtendedBooleanWikiDistance.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
		}
		bw.close();
	}
	
}
