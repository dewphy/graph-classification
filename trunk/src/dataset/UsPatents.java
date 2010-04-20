package dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


public class UsPatents extends Dataset {
	
	public static final String DATASET_NAME = "usPatents/";
	public static final String PATENTS = "us_patents_abstracts.csv";
	private static final String MAPPING_2_LABEL = "Ypatents.mat.txt";
	private static final String MAPPING_2_ID = "links_mapping.mat.txt";
	
	public static class FieldName extends Dataset.FieldName{
		public static final String APPLN_ID = "APPLN_ID";
		public static final String MAPPING = "MAPPING";
	}
	
	public UsPatents() {
		super(DATASET_NAME);
		nbLabels = 6;
	}
	
	@Override
	protected void createIndex(String indexPath) throws IOException {
		Map<Integer, Integer> id2mapping = computeId2Mapping();
		int[] mapping2label = computeMapping2label();
		
		Directory directory = FSDirectory.open(new File(indexPath));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		IndexWriter writer = new IndexWriter(directory, analyzer, true, MaxFieldLength.LIMITED);
		writer.setMergeFactor(10000);
		
		FileInputStream stream = new FileInputStream(getDatasetPath() + PATENTS);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf16"));
		
		int lineNumber = 0;
		label2NbDocs = new int[nbLabels];
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			line = line.trim();
			if (line.equals("")) {
				System.out.println("   Warning (line " + lineNumber + ") empty line.");
			} else if (line.charAt(0) == '#') {
				System.out.println("   Warning (line " + lineNumber + ") commented line.");
			} else {
				String[] blocs = line.split(",", 3);
				if (blocs.length != 3) {
					System.out.println("   Warning (line " + lineNumber + ") incorrect arguments count.");
				} else {
					try {
						int applnId = Integer.parseInt(blocs[0]);
						int id = Integer.parseInt(blocs[1]);
						String content = blocs[2];
						if (content.length() <= 2) {
							System.out.println("   Warning (line " + lineNumber + ") empty abstract.");
						} else if (content.length() <= 100) {
//							System.out.println("   Warning (line " + lineNumber + ") abstract too short.");
						} else {
							if (content.charAt(0) != '\"' || content.charAt(content.length()-1) != '\"') {
								System.out.println("   Warning (line " + lineNumber + ") bad abstract quotation.");
							} else {
								if (!id2mapping.containsKey(id)) {
//									System.out.println("   Warning (line " + lineNumber + ") unknown patent " + patent);
								} else {
									content = content.substring(1,content.length()-1);
									int mapping = id2mapping.get(id);
									int label = mapping2label[mapping];
									
									Document doc = new Document();
									
									doc.add(new Field(FieldName.CONTENT, content, Field.Store.YES, Field.Index.ANALYZED,
											Field.TermVector.WITH_POSITIONS_OFFSETS));
									doc.add(new Field(FieldName.LABEL, String.valueOf(label), Field.Store.YES, Field.Index.NO));
									doc.add(new Field(FieldName.ID, String.valueOf(id), Field.Store.YES, Field.Index.NO));
									doc.add(new Field(FieldName.MAPPING, String.valueOf(mapping), Field.Store.YES, Field.Index.NO));
									doc.add(new Field(FieldName.APPLN_ID, String.valueOf(applnId), Field.Store.YES, Field.Index.NO));
									
									writer.addDocument(doc);
								}
							}
						}
					} catch (NumberFormatException e) {
						System.out.println("   Warning (line " + lineNumber + ") invalid numbers.");
					}
				}
			}
			if (lineNumber++%100000 == 0) {
				System.out.println("   " + (lineNumber-1));
			}
		}
		reader.close();
		writer.close();
	}
	
	private int[] computeMapping2label() throws IOException {
		System.out.println("\nMapping to labels...");
		
		BufferedReader reader  = new BufferedReader(new FileReader(getDatasetPath() + MAPPING_2_LABEL));
		int[] mapping2label = new int[3245005];
		
		int mapping = 0;
		int lineNumber = 0;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			line = line.trim();
			if (line.equals("")) {
				System.out.println("   Warning (line " + lineNumber + ") empty line.");
			} else if (line.charAt(0) == '#') {
				System.out.println("   Warning (line " + lineNumber + ") commented line.");
			} else {
				String[] blocs = line.split(" ");
				if (blocs.length != 1) {
					System.out.println("   Warning (line " + lineNumber + ") incorrect arguments count.");
				} else {
					try {
						mapping2label[mapping++] = Integer.parseInt(blocs[0]) - 1;
					} catch (NumberFormatException e) {
						System.out.println("   Warning (line " + lineNumber + ") invalid numbers.");
					}
				}
			}
			if (lineNumber++%100000 == 0) {
				System.out.println("   " + (lineNumber-1));
			}
		}
		reader.close();
		System.out.println("   " + mapping2label.length + " mapping 2 label over " + lineNumber + " lines.");
		System.out.println("..done!\n");
		return mapping2label;
	}
	
	private Map<Integer, Integer> computeId2Mapping() throws IOException {
		System.out.println("\nId to mapping...");
		
		BufferedReader reader  = new BufferedReader(new FileReader(getDatasetPath() + MAPPING_2_ID));
		Map<Integer, Integer> id2mapping = new HashMap<Integer,Integer>();
		
		int lineNumber = 0;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			line = line.trim();
			if (line.equals("")) {
				System.out.println("   Warning (line " + lineNumber + ") empty line.");
			} else if (line.charAt(0) == '#') {
				System.out.println("   Warning (line " + lineNumber + ") commented line.");
			} else {
				String[] blocs = line.split(" ");
				if (blocs.length != 2) {
					System.out.println("   Warning (line " + lineNumber + ") incorrect arguments count.");
				} else {
					try {
						id2mapping.put(Integer.parseInt(blocs[1]), Integer.parseInt(blocs[0]) - 1);
					} catch (NumberFormatException e) {
						System.out.println("   Warning (line " + lineNumber + ") invalid numbers.");
					}
				}
			}
			if (lineNumber++%100000 == 0) {
				System.out.println("   " + (lineNumber-1));
			}
		}
		reader.close();
		System.out.println("   " + id2mapping.size() + " id 2 mapping over " + lineNumber + " lines.");
		System.out.println("...done!\n");
		return id2mapping;
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
			
			if (doc%100000 == 0) {
				System.out.println("   " + doc);
			}
		}
		label2NbDocs = new int[nbLabels];
		for (int label = 0; label<nbLabels; label++) {
			label2NbDocs[label] = docNbs.get(label).size();
			System.out.println("   " + label + ": (" + label2NbDocs[label] + " patents)");
		}
		reader.close();
	}
}