package classifier;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import dataset.Dataset;

public class PrTfIdf extends Classifier {
	
	public static final String CLASSIFIER_NAME = "prtfidf";
	
	private double[] aprioriProba;
	private Map<String, Double> term2idf;
	private List<Map<String, Double>> prototypeVectors;
	
	public PrTfIdf(Dataset dataset) {
		super(dataset, CLASSIFIER_NAME);
	}
	
	@Override
	public void learn(int[] lowerIndex, int[] upperIndex) throws IOException {
		System.out.println(new StringBuffer("PrTfIdf classifier learning ")
				.append("[" + new DecimalFormat("0.00").format(lowerIndex[0]/(float)dataset.getNbDocs(0)))
				.append("|" + new DecimalFormat("0.00").format(upperIndex[0]/(float)dataset.getNbDocs(0)))
				.append("]...").toString());
		
		Directory directory =  FSDirectory.open(new File(dataset.getIndexPath()));
		IndexReader reader = new FilterIndexReader(IndexReader.open(directory, true));
		
		// Compute apriori probability.
		aprioriProba = new double[dataset.getNbLabels()];
		for (int label=0; label<dataset.getNbLabels(); label++) {
			aprioriProba[label] = dataset.getNbDocs(label)/(double)dataset.getNbDocs();
		}
		// Compute Idf.
		term2idf = new HashMap<String, Double>();
		for (TermEnum terms = reader.terms(); terms.next();) {
			Term term = terms.term();
			TermDocs termdoc = reader.termDocs(term);
			double df = 0;
			while (termdoc.next()) {
				df += termdoc.freq()/(double)dataset.getDocLenght(termdoc.doc());
			}
			double idf = Math.sqrt(dataset.getNbDocs()/df);
			term2idf.put(term.text(), idf);
		}
		// Compute prototype vectors.
		prototypeVectors = new ArrayList<Map<String, Double>>();
		for (int label=0; label<dataset.getNbLabels(); label++) {
			prototypeVectors.add(new HashMap<String,Double>());
		}
		for (int label=0; label<dataset.getNbLabels(); label++) {
			System.out.print("\t" + label);
			for (int index=0; index<lowerIndex[label]; index++) {
				computePrototypeVectors(reader, label, dataset.getDocNb(label, index));
			}
			for (int index=upperIndex[label]; index<dataset.getNbDocs(label); index++) {
				computePrototypeVectors(reader, label, dataset.getDocNb(label, index));
			}
		}
		// Remove negative components.
		for (int label=0; label<dataset.getNbLabels(); label++) {
			Map<String, Double> prototypeVector = prototypeVectors.get(label);
			List<String> negativeComponents = new ArrayList<String>();
			for (String term : prototypeVector.keySet()) {
				if (prototypeVector.get(term) <= 0) {
					negativeComponents.add(term);
				}
			}
			for (String term : negativeComponents) {
				prototypeVector.remove(term);
			}
		}
		reader.close();
		System.out.println("\n...done!\n");
	}
	
	private void computePrototypeVectors(IndexReader reader, int docLabel, int docNb) throws IOException {
		TermFreqVector termFreqVector = reader.getTermFreqVector(docNb, Dataset.FieldName.CONTENT);
		String[] terms = termFreqVector.getTerms();
		int[] freqs = termFreqVector.getTermFrequencies();
		
		// Compute the norm of d: ||d||.
		double sum2 = 0;
		for (int i=0; i<terms.length; i++) {
			double tfidf = freqs[i]*term2idf.get(terms[i]);
			sum2 += tfidf*tfidf;
		}
		double norm = Math.sqrt(sum2);
		
		// Increase prototype vectors component if document is in label. Decrease otherwise.
		for (int i=0; i<terms.length; i++) {
			
			double tfidf = freqs[i]*term2idf.get(terms[i]);
			for (int label=0; label<dataset.getNbLabels(); label++) {
				Map<String, Double> prototypeVector = prototypeVectors.get(label);
				
				double alpha = aprioriProba[label];
				double beta = 0;
				double delta;
				
				if (label == docLabel) {
					delta = alpha*tfidf/norm/((double)dataset.getNbDocs(label));
				} else {
					delta = -beta*tfidf/norm/((double)(dataset.getNbDocs() - dataset.getNbDocs(label)));
				}
				if (prototypeVector.containsKey(terms[i])) {
					delta += prototypeVector.get(terms[i]);
				}
				prototypeVector.put(terms[i], delta);
			}
		}
	}
	
	@Override
	protected void computeConfusionMatrix(IndexReader reader, int[][] confusionMatrix, int docLabel, int docNb) throws IOException {
		
		TermFreqVector termFreqVector = reader.getTermFreqVector(docNb,Dataset.FieldName.CONTENT);
		String[] terms = termFreqVector.getTerms();
		
		int[] freqs = termFreqVector.getTermFrequencies();
		
		// Compute max similarity measure.
		int bestlabel = 0;
		double[] measures = new double[dataset.getNbLabels()];
		for (int label=0; label<dataset.getNbLabels(); label++) {
			measures[label] = 0;
			for (int i=0; i<terms.length; i++) {
				Map<String, Double> prototypeVector = prototypeVectors.get(label);
				if (prototypeVector.containsKey(terms[i])) {
					measures[label] += freqs[i]*term2idf.get(terms[i])*prototypeVector.get(terms[i]);
				}
			}
			if (measures[label] > measures[bestlabel]) {
				bestlabel = label;
			}
		}
		confusionMatrix[docLabel][bestlabel]++;
	}
}