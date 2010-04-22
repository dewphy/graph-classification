package utilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import utilities.Pair;
import dataset.Dataset;

public class GraphTools {
	
	private Dataset dataset;
	
	public GraphTools(Dataset dataset) {
		this.dataset = dataset;
	}
	
	public void construct(int lowerIndex, int upperIndex) throws IOException, ParseException {
		int[] nbNeighbors = {1, 2, 3, 5, 8, 10, 20, 30, 40, 50, 60, 70 ,80, 90, 100};
		construct(lowerIndex, upperIndex, nbNeighbors);
	}
	
	public void construct(int lowerIndex, int upperIndex, int[] nbNeighbors) throws IOException, ParseException {
		// FileHandler.emptyDirectory(new File(dataset.getGraphPath() + "Raw/"));
		
		// Create label vector.
		StringBuilder labelVector = new StringBuilder();
		for (int docNb=lowerIndex; docNb<upperIndex; docNb++) {
			int label = 1 + dataset.getLabel(docNb);
			labelVector.append(label + "\n");
		}
		BufferedWriter labelWriter = new BufferedWriter(new FileWriter(dataset.getGraphPath() + "Raw/Labels" + "_" + lowerIndex + "_" + upperIndex));
		labelWriter.write(labelVector.toString());
		labelWriter.close();
		
		// Create graph matrixes.
		List<List<Pair<Integer, Float>>> graph = construct(lowerIndex, upperIndex, nbNeighbors[nbNeighbors.length-1]);
		for (int nbNeighbor : nbNeighbors) {
			System.out.print("Inferring graph with " + nbNeighbor + " neighbors... ");
			
			StringBuilder graphMatrix = new StringBuilder();
			for (int docNb=lowerIndex; docNb<upperIndex; docNb++) {
				for (int neighbor=0; neighbor<nbNeighbor; neighbor++) {
					Pair<Integer, Float> edge = graph.get(docNb-lowerIndex).get(neighbor);
					graphMatrix.append((1+docNb) + " " + (1+edge.getT1()) + " " + edge.getT2() + "\n");
				}
			}
			BufferedWriter graphWriter = new BufferedWriter(new FileWriter(dataset.getGraphPath() + "Raw/Graph_" + lowerIndex + "_" + upperIndex + "_" + nbNeighbor));
			graphWriter.write(graphMatrix.toString());
			graphWriter.close();
			System.out.println("done!");
		}
	}
	
	private List<List<Pair<Integer, Float>>> construct(int lowerIndex, int upperIndex, int nbNeighbor) throws IOException, ParseException {
		System.out.println("Inferring graphs [" + lowerIndex+ "|" + upperIndex + "]... ");
		
		// Initialize array to store the graph.
		List<List<Pair<Integer, Float>>> graph = new ArrayList<List<Pair<Integer, Float>>>(upperIndex-lowerIndex);
		
		// Load index for research.
		Directory directory =  FSDirectory.open(new File(dataset.getIndexPath()));
		IndexReader reader = new FilterIndexReader(IndexReader.open(directory, true));
		IndexSearcher searcher = new IndexSearcher(directory, true);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		QueryParser parser = new QueryParser(Dataset.FieldName.CONTENT, analyzer);
		
		// Find closest neighbors for every document in range [lowerIndex, upperIndex[
		for (int docNb=lowerIndex; docNb<upperIndex; docNb++) {
			
			// Construct query and search for it.
			TermFreqVector termFreqVector = reader.getTermFreqVector(docNb, Dataset.FieldName.CONTENT);
			int[] freqs = termFreqVector.getTermFrequencies();
			String[] terms = termFreqVector.getTerms();
			int maxTermCount = 0;
			StringBuilder queryBuilder = new StringBuilder();
			for (int i=0; i<terms.length && maxTermCount<1023; i++) {
				for (int j=0; j<freqs[i] && maxTermCount<1023; j++) {
					queryBuilder.append(terms[terms.length-1-i]+" ");
					maxTermCount++;
				}
			}
			String content = queryBuilder.toString();
			Query query = parser.parse(content);
			ScoreDoc[] hits = searcher.search(query, null, nbNeighbor).scoreDocs;
			
			// Store results.
			List<Pair<Integer, Float>> links = new ArrayList<Pair<Integer, Float>>(nbNeighbor);
			for (ScoreDoc hit : hits) {
				links.add(new Pair<Integer, Float>(hit.doc, hit.score));
			}
			graph.add(links);
			
			// Warn user if the query return an insufficient number of hits.
			if(links.size()<nbNeighbor) {
				System.out.println("Warning: Not enough neighbors!");
				System.out.println("docNb: " + docNb + " id: " + dataset.getId(docNb));
				System.out.println("nbNeighbors obtained: " + links.size() + ", nbNeighbors expected: " + nbNeighbor);
			}
			
			// Show progress.
			if (docNb%1000 == 0) {
				System.out.println("   " + docNb);
			}
		}
		searcher.close();
		reader.close();
		System.out.println("done!\n");
		return graph;
	}
}