
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
    
    import java.io.File;
    import java.io.IOException;
    import java.util.Date;
    
    /** Index all text files under a directory.
     * <p>
     * This is a command-line application demonstrating simple Lucene indexing.
     * Run it with no command-line arguments for usage information.
     */
    public class MakeLuceneIndex {
      
      private MakeLuceneIndex() {}
    
      /** Index all text files under a directory. */
      public static void main(String[] args) {
    	  String baseDir = "/home/chrisschaefer/";
          String wikiDumpFile = "Downloads/enwiki-20130604-pages-articles.xml.bz2";
          String luceneIndexName = "enwiki-20130604-lucene-no-stubs";
          System.currentTimeMillis();
          boolean bIgnoreStubs = true;

          for ( int i = 0; i < args.length; ++i )
          {
              if ( args[i].equals( "-luceneindex" ) )
                  luceneIndexName = args[++i];

              if ( args[i].equals( "-basedir" ) )
                  baseDir = args[++i];

              if ( args[i].equals( "-dumpfile" ) )
                  wikiDumpFile = args[++i];

              if ( args[i].equals( "-includestubs" ) )
                  bIgnoreStubs = false;
          }
        
          System.out.println("Indexing to directory '" + baseDir + luceneIndexName + "'");
          
        Date start = new Date();
        System.out.println(start.toString() + " iArticleCount: 0 iSkippedPageCount: 0");
        
        try {
              
          Directory dir = FSDirectory.open(new File(baseDir + luceneIndexName));
          
         
          Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
          IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);
    

        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);

    
          // Optional: for better indexing performance, if you
          // are indexing many documents, increase the RAM
          // buffer.  But if you do this, increase the max heap
          // size to the JVM (eg add -Xmxm or -Xmx1g):
          //
          iwc.setRAMBufferSizeMB(2000.0);
    
          IndexWriter writer = new IndexWriter(dir, iwc);
          
          Extractor wikidumpExtractor = new Extractor( baseDir + File.separator + wikiDumpFile );
          wikidumpExtractor.setLinkSeparator( "_" );
          wikidumpExtractor.setCategorySeparator( "_" );
          
          int iStubs = 0;
          int iArticleCount = 0;
          int iSkippedPageCount = 0;
          long iStartTime = java.lang.System.nanoTime();
          long iTime = iStartTime;

          while ( wikidumpExtractor.nextPage() )
          {
              if ( wikidumpExtractor.getPageType() != Extractor.PageType.ARTICLE )
              {
                  ++iSkippedPageCount;
                  continue;
              }

              if ( bIgnoreStubs && wikidumpExtractor.getStub() )
              {
                  ++iStubs;
                  continue;
              }

              Document doc = new Document();
              ++iArticleCount;


//              wikidumpExtractor.setTitleSeparator( "_" );
//              doc.add( new TextField( "url_title", wikidumpExtractor.getPageTitle( false ), Field.Store.YES) );

              wikidumpExtractor.setTitleSeparator( " " );
              // doc.add( new TextField( "title", wikidumpExtractor.getPageTitle( false ), Field.Store.YES) );
              //doc.add(new LongField("wiki_id", wikidumpExtractor.getPageId(), Field.Store.YES));
              doc.add(new TextField("contents", wikidumpExtractor.getPageTitle( false ) + " " + wikidumpExtractor.getPageText(), Field.Store.NO ));

              writer.addDocument( doc );              

              if ( iArticleCount % 10000 == 0 )
              {
            	  writer.commit();
            	  System.out.println(new Date().toString() + " iArticleCount: " + iArticleCount + " iSkippedPageCount: " + iSkippedPageCount);                          
              }
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
    
          Date end = new Date();
          System.out.println(end.getTime() - start.getTime() + " total milliseconds");
    
        } catch (IOException e) {
          System.out.println(" caught a " + e.getClass() +
           "\n with message: " + e.getMessage());
        }
      }
    }




