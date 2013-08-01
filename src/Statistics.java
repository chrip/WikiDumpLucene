import java.lang.reflect.Field;


public class Statistics {
	static int inlinkThreshold = 0;
	static int outlinkThreshold = 0;
	static int numberOfUniqueNonStopwordsThreshold = 0;
	static int minWordLengthThreshold = 0;
	static int titleWeight = 4;
	static boolean filterTitle = false;
	static boolean filterCategories = false;
	static boolean filterStopWords = false;
	static String indexPath = "/home/chrisschaefer/Downloads/wikipedia-gab-2013-07-29";
	static String anchorText = "NONE"; //, UNIQUE, ALL };
	static long runtimeInMilliSec = 0;
	static int numberOfWords = 0;
	static int numberOfDocs = 0;

	static String similarity = "ESA"; //, DEFAULT_LUCENE }
	static int stemmerCalls = 0;
	
	static double tfidfThreshold = 9.28;
	static int freqThreshold = 0;
	static int WINDOW_SIZE = 100;
	static double WINDOW_THRES = 0.005f;

	static double wikiDistance = 0;
	static double cosineDistance = 0;
	static double scoredWikiDistance = 0;




public String getHeader() {
  StringBuilder result = new StringBuilder();

  //determine fields declared in this class only (no fields of superclass)
  Field[] fields = this.getClass().getDeclaredFields();
  
  boolean isFirstLine = true;
  //print field names

  for ( Field field : fields  ) {
      result.append(isFirstLine?"":"\t");
      isFirstLine = false;
      result.append( field.getName() );
  }
  return result.toString();
}



public String getValues() {
  StringBuilder result = new StringBuilder();

  //determine fields declared in this class only (no fields of superclass)
  Field[] fields = this.getClass().getDeclaredFields();

  boolean isFirstLine = true;
  //print field values
  for ( Field field : fields  ) {
    try {
    	result.append(isFirstLine?"":"\t");
        isFirstLine = false;
      //requires access to private field:
      result.append( field.get(this) );
    } catch ( IllegalAccessException ex ) {
      System.out.println(ex);
    }
  }

  return result.toString();
}

public static void main(String[] args) {
	Statistics s = new Statistics();
	System.out.println(s.getHeader());
	System.out.println(s.getValues());
}

}
