package classifier;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import dataset.Dataset;
import utilities.Pair;


public class Bayes extends Classifier {
	
	public static final String CLASSIFIER_NAME = "bayes";
	
	private int[] label2Tf;
	private double[] aprioriProba;
	private Map<Pair<String, Integer>, Integer> termLabel2Freq;
	
	public Bayes(Dataset dataset) {
		super(dataset, CLASSIFIER_NAME);
	}
	
	@Override
	public void learn(int[] lowerIndex, int[] upperIndex) throws IOException {
		System.out.println(new StringBuffer("Bayes classifier learning ")
				.append("[" + new DecimalFormat("0.00").format(lowerIndex[0]/(float)dataset.getNbDocs(0)))
				.append("|" + new DecimalFormat("0.00").format(upperIndex[0]/(float)dataset.getNbDocs(0)))
				.append("]...").toString());
		
		Directory directory =  FSDirectory.open(new File(dataset.getIndexPath()));
		IndexReader reader = new FilterIndexReader(IndexReader.open(directory, true));
		
		label2Tf =  new int[dataset.getNbLabels()];
		aprioriProba = new double[dataset.getNbLabels()];
		termLabel2Freq = new HashMap<Pair<String, Integer>, Integer>();
		for (int label=0; label<dataset.getNbLabels(); label++) {
			System.out.print("\t" + label);
			
			label2Tf[label] = 0;
			aprioriProba[label] = dataset.getNbDocs(label)/(double)dataset.getNbDocs();
			
			for (int index=0; index<lowerIndex[label]; index++) {
				computeTermLabelFreq(reader, label, dataset.getDocNb(label, index));
			}
			for (int index=upperIndex[label]; index<dataset.getNbDocs(label); index++) {
				computeTermLabelFreq(reader, label, dataset.getDocNb(label, index));
			}
		}
		reader.close();
		System.out.println("\n...done!\n");
	}
	
	private void computeTermLabelFreq(IndexReader reader, int label, int docNb) throws IOException {
		TermFreqVector termFreqVector = reader.getTermFreqVector(docNb, Dataset.FieldName.CONTENT);
		String[] terms = termFreqVector.getTerms();
		int[] freqs = termFreqVector.getTermFrequencies();
		
		for (int i=0; i<terms.length; i++) {
			Pair<String, Integer> termLabel = new Pair<String, Integer>(terms[i], label);
			if (termLabel2Freq.containsKey(termLabel)) {
				termLabel2Freq.put(termLabel, termLabel2Freq.get(termLabel) + freqs[i]);
			} else {
				termLabel2Freq.put(termLabel, freqs[i]);
			}
			label2Tf[label] += freqs[i];
		}
	}
	
	protected void computeConfusionMatrix(IndexReader reader, int[][] confusionMatrix, int docLabel,
			int docNb) throws IOException {
		
		TermFreqVector termFreqVector = reader.getTermFreqVector(docNb,Dataset.FieldName.CONTENT);
		String[] terms = termFreqVector.getTerms();
		
		int[] freqs = termFreqVector.getTermFrequencies();
		
		// Compute max similarity measure.
		int bestlabel = 0;
		double[] measures = new double[dataset.getNbLabels()];
		for (int label=0; label<dataset.getNbLabels(); label++) {
			measures[label] = 0;
			for (int i=0; i<terms.length; i++) {
				Pair<String, Integer> termLabel = new Pair<String, Integer>(terms[i], label);
				int freq = termLabel2Freq.containsKey(termLabel) ? termLabel2Freq.get(termLabel) : 0;
				double numerator = 1+freq;
				double denominator = 1*dataset.getNbTerms() + label2Tf[label];
				measures[label] += freqs[i]*Math.log(numerator/denominator);
			}
			measures[label] += Math.log(aprioriProba[label]);
			
			if (measures[label] > measures[bestlabel]) {
				bestlabel = label;
			}
		}
		confusionMatrix[docLabel][bestlabel]++;
	}
}