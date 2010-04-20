package dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class NewsGroups extends Dataset {
	
	public static final String DATASET_NAME = "newsGroups/";
	
	public NewsGroups() {
		super(DATASET_NAME);
		nbLabels = 20;
	}
	
	@Override
	protected void createIndex(String indexPath) throws IOException {
		Directory directory = FSDirectory.open(new File(indexPath));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		IndexWriter writer = new IndexWriter(directory, analyzer, true, MaxFieldLength.LIMITED);
		writer.setMergeFactor(10000);
		
		int label = 0;
		label2NbDocs = new int[nbLabels];
		for (String newsGroup : new File(getDatasetPath()).list()) {
			System.out.print("   " + (label) + ": " + newsGroup);
			
			label2NbDocs[label] = 0;
			String newsGroupPath = getDatasetPath() + newsGroup;
			for (String news : new File(newsGroupPath).list()) {
				String newsPath = newsGroupPath + "/" + news;
				
				FileInputStream stream = new FileInputStream(newsPath);
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf8"));
				
				String line;
				StringBuffer buffer = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					buffer.append(line).append(" ");
				}
				String content = buffer.toString().trim();
				if (!content.equals("")) {
					Document doc = new Document();
					doc.add(new Field(FieldName.ID, news, Field.Store.YES, Field.Index.NO));
					doc.add(new Field(FieldName.LABEL, String.valueOf(label), Field.Store.YES, Field.Index.NO));
					doc.add(new Field(FieldName.CONTENT, content, Field.Store.YES, Field.Index.ANALYZED,
							Field.TermVector.WITH_POSITIONS_OFFSETS));
					writer.addDocument(doc);
					
					label2NbDocs[label]++;
				}
				reader.close();
			}
			System.out.println(" (" + label2NbDocs[label] + " news)");
			label++;
		}
		writer.close();
	}
	
	@Override
	protected void loadIndex(String indexPath) throws IOException {
		Directory directory =  FSDirectory.open(new File(getIndexPath()));
		IndexReader reader = new FilterIndexReader(IndexReader.open(directory, true));
		
		nbDocs = reader.numDocs();
		System.out.println("   " + nbDocs + " docs");
		
		nbTerms = 0;
		for (TermEnum terms = reader.terms(); terms.next();) {
			nbTerms++;
		}
		System.out.println("   " + nbTerms + " terms");
		
		docNbs = new ArrayList<List<Integer>>(nbLabels);
		for (int label = 0; label<nbLabels; label++) {
			docNbs.add(new ArrayList<Integer>());
		}
		
		docNb2Id = new int[nbDocs];
		docNb2Label = new int[nbDocs];
		docNb2Lenght = new int[nbDocs];
		for (int doc=0; doc<nbDocs; doc++) {
			TermFreqVector termFreqVector = reader.getTermFreqVector(doc, FieldName.CONTENT);
			int[] freqs = termFreqVector.getTermFrequencies();
			
			docNb2Lenght[doc] = 0;
			for (int i=0; i<freqs.length; i++) {
				docNb2Lenght[doc] += freqs[i];
			}
			int id = Integer.valueOf(reader.document(doc).getField(FieldName.ID).stringValue());
			int label = Integer.valueOf(reader.document(doc).getField(FieldName.LABEL).stringValue());
			docNb2Id[doc] = id;
			docNb2Label[doc] = label;
			docNbs.get(label).add(doc);
		}
		
		label2NbDocs = new int[nbLabels];
		for (int label = 0; label<nbLabels; label++) {
			label2NbDocs[label] = docNbs.get(label).size();
			System.out.println("   " + label + ": (" + label2NbDocs[label] + " news)");
		}
		reader.close();
	}
}