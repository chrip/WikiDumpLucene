
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class LuceneTest {
	static Map<String, Integer> _inLinks = new HashMap<String, Integer>();
	private LuceneTest() {}

	/** Index all text files under a directory. 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException */
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String baseDir = "/home/chrisschaefer/";
		String luceneIndexName = "lucene-test";


		try {

			Directory dir = FSDirectory.open(new File(baseDir + luceneIndexName));


			Analyzer analyzer = new WikipediaAnalyzer("/home/chrisschaefer/Arbeitsfläche/github/esalib/res/stopwords.en.txt");
//			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);


			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
			iwc.setSimilarity(new ESASimilarity());

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer.  But if you do this, increase the max heap
			// size to the JVM (eg add -Xmxm or -Xmx1g):
			//
			iwc.setRAMBufferSizeMB(2000.0);

			IndexWriter writer = new IndexWriter(dir, iwc);

	
			{
			Document doc = new Document();
			doc.add(new TextField("contents", "cat dog sex sex", Field.Store.NO ));
			writer.addDocument( doc );     
			}			
			{
			Document doc = new Document();
			doc.add(new TextField("contents", "battery staple horse foobar", Field.Store.NO ));
			writer.addDocument( doc );     
			}
			{
			Document doc = new Document();
			doc.add(new TextField("contents", "I am a nonsense sentence tiger", Field.Store.NO ));
			writer.addDocument( doc );     
			}	
			{
			Document doc = new Document();
			doc.add(new TextField("contents", "cat dog horse love", Field.Store.NO ));
			writer.addDocument( doc );     
			}
	
			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here.  This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			writer.commit();
			writer.forceMerge(1);
			writer.close();

		} catch (Exception e) {
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
}




