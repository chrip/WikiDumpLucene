import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class GabToLucene {
	static int inlinkThreshold = 0;
	static int outlinkThreshold = 0;
	static int numberOfUniqueNonStopwordsThreshold = 100;
	static int minWordLengthThreshold = 0;
	static int titleWeight = 4;
	static boolean filterTitle = false;
	static boolean filterCategories = false;
	static boolean filterStopWords = false;
	
	public static void main(String[] args) throws IOException {
		Date start = new Date();
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
//		Analyzer analyzer = new WikipediaAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);

//		iwc.setSimilarity(new ESASimilarity());
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(2000.0);

		Directory dir = FSDirectory.open(new File("/home/chrisschaefer/Downloads/wikipedia-051105-preprocessed-all"));
		IndexWriter writer = new IndexWriter(dir, iwc);

		String dumpFile = "/home/chrisschaefer/Arbeitsfläche/github/wikiprep-esa-original/data/20051105_pages_articles.hgw.xml";
//		String dumpFile = "/home/chrisschaefer/Arbeitsfläche/github/wikiprep-esa-original/data/sample.hgw.xml";
		BufferedReader br = new BufferedReader(new FileReader(dumpFile));
		
		
		String line = br.readLine();
		ArrayList<String> pageIds = new ArrayList<String>();
		Map<String, Integer> inLinks = new HashMap<String, Integer>();
		while ( line != null )
		{
			int endPageId = line.indexOf("\" orglength=\"");
			if(endPageId != -1) {
				pageIds.add(line.substring(10, endPageId));
			}
			else if (line.startsWith("<links>")){
				String[] links = line.substring(7, line.length()-8).split(" ");
				for(int i = 0; i < links.length; i++) {         		 
					//            		  artikelTextWriter.println(link);
					if(inLinks.containsKey(links[i])) {
						int tmp = inLinks.get(links[i]);
						tmp++;
						inLinks.put(links[i], tmp);
					}
					else {
						inLinks.put(links[i], 1);
					}
				}
			}
			line = br.readLine();
		}
//		for(String id: pageIds){
//			linkWriter.println(id  + "\t" + (inLinks.containsKey(id)?inLinks.get(id):"0"));
//		}
		br.close();
//		linkWriter.close();
		Date end = new Date();
		int totalArticles = pageIds.size();
		double articlesDone = 0.0;

		String endStatement = end.getTime() - start.getTime() + " total milliseconds (" + (end.getTime() - start.getTime())/3600000.0 + " hours), " + totalArticles + " Articles.";
		System.out.println(endStatement);
		
		br = new BufferedReader(new FileReader(dumpFile));
		int iArticleCount = 0;
		line = br.readLine();
		while ( line != null )
		{
			int endPageId = line.indexOf("\" orglength=\"");
			String title;
			if(endPageId != -1) {
				String id = line.substring(10, endPageId);
				articlesDone++;
				if (inlinkThreshold != 0 || !inLinks.containsKey(id) || inLinks.get(id) < inlinkThreshold) {
					line = br.readLine();
					continue;
				}
				int outLinksPos = line.indexOf("outlinks");
				if(Integer.valueOf(line.substring(outLinksPos+10, line.indexOf("\"", outLinksPos+10))) < outlinkThreshold){
					line = br.readLine();
					continue;	
				}
				line = br.readLine();
				title = line.substring(7,line.length()-8);
				
		         if (!filterTitle &&   
		        		  (  title.startsWith("Media:")
		        	      || title.startsWith("Special:")
		        	      || title.startsWith("Talk:")
		        	      || title.startsWith("User:")
		        	      || title.startsWith("User talk:")
		        	      || title.startsWith("Wikipedia:")
		        	      || title.startsWith("Wikipedia talk:")
		        	      || title.startsWith("Image:")
		        	      || title.startsWith("Image talk:")
		        	      || title.startsWith("MediaWiki:")
		        	      || title.startsWith("MediaWiki talk:")
		        	      || title.startsWith("Template:")
		        	      || title.startsWith("Template talk:")
		        	      || title.startsWith("Help:")
		        	      || title.startsWith("Help talk:")
		        	      || title.startsWith("Category:")
		        	      || title.startsWith("Category talk:")
		        	      || title.startsWith("Portal:")
		        	      || title.startsWith("Portal talk:")
		            		
		            		// discard articles in month_year (e.g. January 2002) format
		            		|| title.matches("^(?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{4}$")
		            		
		            		// discard articles in year_in… (e.g. 2002 in literature, 1996 in the Olympics) format
		            		|| title.matches("^\\d{4} in .*")
		            		
		            		// discard articles in only digit format (e.g. 1996, 819382, 42)
		            		|| title.matches("^\\d+$")
		            		
		            		// discard articles in list format (e.g. List of ... )
		            		|| title.startsWith("List of"))) {
		        	 line = br.readLine();
		            			continue;
		            		}
				

				String text = new String();
				line = br.readLine();
				String[] categories = line.substring(12, line.length()-13).split(" ");
				boolean stop = false;
				for(int i = 1; filterCategories && i < categories.length-1; i++) {         		 
					if(stopCategoriesSet.contains(categories[i])) {
						stop = true;
						break;
					}
				}
				if(stop) {
					line = br.readLine();
					continue;
				}
					while ( !br.readLine().equals("<text>") ) {
						continue;
					}
					line = br.readLine();
					while ( !line.equals("</text>") ) {
						text += " ";
						text += line;						
						line = br.readLine();
					}
					
					text = text.replaceAll("[^\\w]", " ");
					String[] words = text.split(" ");
			        text = "";
			        String space = " ";
			        Set<String> wordSet = new HashSet<String>();
			        for(String w : words) {
			        	w = w.toLowerCase();
			        	if(w.length() < minWordLengthThreshold || (filterStopWords && stopWordSet.contains(w))) {
			        		continue;
			        	}
			        	wordSet.add(w);
			        	text += space;
			        	text += w;
			        }        
			        if(wordSet.size() < numberOfUniqueNonStopwordsThreshold) {
			        	line = br.readLine();
			        	continue;
			        }
			        iArticleCount++;
			        Document doc = new Document();
			         title += " ";
			         for(int i = 0; i < titleWeight-1; i++) {
			        	 title += title; 
			         }
					doc.add(new TextField("contents", title + text, Field.Store.NO ));
					writer.addDocument( doc );
					if ( iArticleCount % 1000 == 0 )
					{
						System.out.println(iArticleCount + "\t" + (articlesDone/(double)totalArticles)*100);
						writer.commit();
					} 
				}		
			line = br.readLine();
		}

		writer.commit();
		writer.forceMerge(1);
		writer.close();
		br.close();
		endStatement = end.getTime() - start.getTime() + " total milliseconds (" + (end.getTime() - start.getTime())/3600000.0 + " hours), " + iArticleCount + " Articles.";
		System.out.println(endStatement);
		
	}
	  public static final String[] stopCategories = new String[] {
		  "694492" //	Category:Star name disambiguations
		  ,"696996" //	Category:America
		  ,"706360" //	Category:Disambiguation
		  ,"707272" //	Category:Georgia
		  ,"708635" //	Category:Lists of political parties by generic name
		  ,"720975" //	Category:Galaxy name disambiguations
		  ,"722675" //	Category:Lists of two-letter combinations
		  ,"787611" //	Category:Disambiguation categories
		  ,"1039940" //	Category:Towns in Italy (disambiguation)
		  ,"1125125" //	Category:Redirects to disambiguation pages
		  ,"1169671" //	Category:Birmingham
		  ,"1756037" //	Category:Mathematical disambiguation
		  ,"1935906" //	Category:Public schools in Montgomery County
		  ,"2031328" //	Category:Structured lists
		  ,"2133730" //	Category:Identical titles for unrelated songs
		  ,"2391391" //	Category:Signpost articles
		  ,"2453533" //	Category:Township disambiguation
		  ,"2495113" //	Category:County disambiguation
		  ,"2620466" //	Category:Disambiguation pages in need of cleanup
		  ,"2634660" //	Category:Human name disambiguation
		  ,"2645680" //	Category:Number disambiguations
		  ,"2645816" //	Category:Letter and number combinations
		  ,"2649076" //	Category:,"4" //-letter acronyms
		  ,"2655288" //	Category:Acronyms that may need to be disambiguated
		  ,"2664682" //	Category:Lists of roads sharing the same title
		  ,"2803431" //	Category:List disambiguations
		  ,"2803858" //	Category:,"3" //-digit Interstate disambiguations
		  ,"2826432" //	Category:Geographical locations sharing the same title
		  ,"2866961" //	Category:Tropical cyclone disambiguation
		  ,"2891248" //	Category:Repeat-word disambiguations
		  ,"2900842" //	Category:Song disambiguations
		  ,"2906246" //	Category:Disambiguated phrases
		  ,"2907532" //	Category:Subway station disambiguations
		  ,"2907812" //	Category:Lists of identical but unrelated album titles
		  ,"2909071" //	Category:,"5" //-letter acronyms
		  ,"2911539" //	Category:Three-letter acronym disambiguations
		  ,"2929221" //	Category:Miscellaneous disambiguations
		  ,"3055424" //	Category:Two-letter acronym disambiguations
		  ,"952890" //	Category:Days
		  ,"1712083" //	Category:Eastern Orthodox liturgical days
		    };
		    public static final Set<String> stopCategoriesSet = new HashSet<String>(Arrays.asList(stopCategories));
		    public static final String[] stopWords = new String[] { 
		       "able","about","above","according","accordingly","across","actually","after","afterwards","again"
				,"against","ain","albeit","all","allow","allows","almost","alone","along","already"
				,"also","although","always","among","amongst","and","another","any","anybody","anyhow"
				,"anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate","appropriate","are"
				,"aren","around","aside","ask","asking","associated","available","away","awfully","became"
				,"because","become","becomes","becoming","been","before","beforehand","behind","being","believe"
				,"below","beside","besides","best","better","between","beyond","both","brief","but"
				,"mon","came","can","can","cannot","cant","cause","causes","certain","certainly"
				,"changes","clearly","com","come","comes","concerning","consequently","consider","considering","contain"
				,"containing","contains","corresponding","could","couldn","course","currently","definitely","described","despite"
				,"did","didn","different","does","doesn","doing","don","done","down","downwards"
				,"during","each","edu","eight","either","else","elsewhere","enough","entirely","especially"
				,"etc","even","ever","every","everybody","everyone","everything","everywhere","exactly","example"
				,"except","far","few","fifth","first","five","followed","following","follows","for"
				,"former","formerly","forth","four","from","further","furthermore","get","gets","getting"
				,"given","gives","goes","going","gone","got","gotten","greetings","had","hadn"
				,"happens","hardly","has","hasn","have","haven","having","hello","help","hence"
				,"her","here","here","hereafter","hereby","herein","hereupon","hers","herself","him"
				,"himself","his","hither","hopefully","how","howbeit","however","ignored","immediate","inasmuch"
				,"inc","indeed","indicate","indicated","indicates","inner","insofar","instead","into","inward"
				,"isn","its","itself","just","keep","keeps","kept","know","known","knows"
				,"last","lately","later","latter","latterly","least","less","lest","let","like"
				,"liked","likely","little","look","looking","looks","ltd","mainly","many","may"
				,"maybe","mean","meanwhile","merely","might","more","moreover","most","mostly","much"
				,"must","myself","name","namely","near","nearly","necessary","need","needs","neither"
				,"never","nevertheless","new","next","nine","nobody","non","none","noone","nor"
				,"normally","not","nothing","novel","now","nowhere","obviously","off","often","okay"
				,"old","once","one","ones","only","onto","other","others","otherwise","ought"
				,"our","ours","ourselves","out","outside","over","overall","own","particular","particularly"
				,"per","perhaps","placed","please","plus","possible","presumably","probably","provides","que"
				,"quite","rather","really","reasonably","regarding","regardless","regards","relatively","respectively","right"
				,"said","same","saw","say","saying","says","second","secondly","see","seeing"
				,"seem","seemed","seeming","seems","seen","self","selves","sensible","sent","serious"
				,"seriously","seven","several","shall","she","should","shouldn","since","six","some"
				,"somebody","somehow","someone","something","sometime","sometimes","somewhat","somewhere","soon","sorry"
				,"specified","specify","specifying","still","sub","such","sup","sure","take","taken"
				,"tell","tends","than","thank","thanks","thanx","that","that","thats","the"
				,"their","theirs","them","themselves","then","thence","there","there","thereafter","thereby"
				,"therefore","therein","theres","thereupon","these","they","think","third","this","thorough"
				,"thoroughly","those","though","three","through","throughout","thru","thus","together","too"
				,"took","toward","towards","tried","tries","truly","try","trying","twice","two"
				,"under","unfortunately","unless","unlikely","until","unto","upon","use","used","useful"
				,"uses","using","usually","uucp","value","various","very","via","viz","want"
				,"wants","was","wasn","way","welcome","well","went","were","weren","what"
				,"whatever","whatsoever","when","whence","whenever","whensoever","where","where","whereafter","whereas"
				,"whereat","whereby","wherefrom","wherein","whereinto","whereof","whereon","whereto","whereunto","whereupon"
				,"wherever","wherewith","whether","which","whichever","whichsoever","while","whilst","whither","who"
				,"whoever","whole","whom","whomever","whomsoever","whose","whosoever","why","will","willing"
				,"wish","with","within","without","won","wonder","would","wouldn","yes","yet"
				,"you","your","yours","yourself","yourselves","zero", "ref"
			};
		    public static final Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopWords));
}
