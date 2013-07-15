

import java.io.Reader;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;


public class WikipediaAnalyzer extends Analyzer {
	
	  @Override
	  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		final StandardTokenizer src = new StandardTokenizer(Version.LUCENE_43, reader);
        TokenStream tok = new StandardFilter(Version.LUCENE_43, src);
        tok = new LengthFilter(false, tok, 3, 100);
        tok = new LowerCaseFilter(Version.LUCENE_43, tok);	    
	    tok = new StopFilter(Version.LUCENE_43, tok, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
	    tok = new PorterStemFilter(tok);
	    tok = new PorterStemFilter(tok);
	    tok = new PorterStemFilter(tok);
	    return new TokenStreamComponents(src, tok);
	  }
}