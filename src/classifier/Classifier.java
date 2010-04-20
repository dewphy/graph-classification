package classifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import dataset.Dataset;


public abstract class Classifier {
	
	protected final String classifierName;
	protected final Dataset dataset;
	
	public Classifier(Dataset dataset, String classifierName) {
		this.dataset = dataset;
		this.classifierName = classifierName;
	}
	
	public void learn(double lowerPercent, double upperPercent) throws IOException {
		if (lowerPercent>1 || lowerPercent<0 || upperPercent>1 || upperPercent<0) {
			throw new NullPointerException("Percent must be between 0 and 1."
					+ " lowerPercent: " + lowerPercent + " upperPercent: " + upperPercent);
		}
		int[] lowerIndex = new int[dataset.getNbLabels()];
		int[] upperIndex = new int[dataset.getNbLabels()];
		for (int label=0; label<dataset.getNbLabels(); label++) {
			lowerIndex[label] = (int) (lowerPercent*dataset.getNbDocs(label));
			upperIndex[label] = (int) (upperPercent*dataset.getNbDocs(label));
		}
		learn(lowerIndex, upperIndex);
	}
	
	public abstract void learn(int[] lowerIndex, int[] upperIndex) throws IOException;
	
	public double test(double lowerPercent, double upperPercent) throws IOException {
		if (lowerPercent>1 || lowerPercent<0 || upperPercent>1 || upperPercent<0) {
			throw new NullPointerException("Percent must be between 0 and 1."
					+ " lowerPercent: " + lowerPercent + " upperPercent: " + upperPercent);
		}
		int[] lowerIndex = new int[dataset.getNbLabels()];
		int[] upperIndex = new int[dataset.getNbLabels()];
		for (int label=0; label<dataset.getNbLabels(); label++) {
			lowerIndex[label] = (int) (lowerPercent*dataset.getNbDocs(label));
			upperIndex[label] = (int) (upperPercent*dataset.getNbDocs(label));
		}
		return test(lowerIndex, upperIndex);
	}
	
	protected double test(int[] lowerIndex, int[] upperIndex) throws IOException {
		String lowerPercent = new DecimalFormat("0.00").format(lowerIndex[0]/(float)dataset.getNbDocs(0));
		String upperPercent = new DecimalFormat("0.00").format(upperIndex[0]/(float)dataset.getNbDocs(0));
		System.out.println("Classifier testing [" + lowerPercent + "|" + upperPercent + "]...");
		
		Directory directory =  FSDirectory.open(new File(dataset.getIndexPath()));
		IndexReader reader = new FilterIndexReader(IndexReader.open(directory, true));
		
		// Initialize confusion matrix with 0.
		int[][] confusionMatrix = new int[dataset.getNbLabels()][dataset.getNbLabels()];
		for (int iLabel=0; iLabel<dataset.getNbLabels(); iLabel++) {
			for (int jLabel=0; jLabel<dataset.getNbLabels(); jLabel++) {
				confusionMatrix[iLabel][jLabel] = 0;
			}
		}
		
		// Compute confusion matrix.
		for (int docLabel=0; docLabel<dataset.getNbLabels(); docLabel++) {
			System.out.print("\t" + docLabel);
			for (int index=lowerIndex[docLabel]; index<upperIndex[docLabel]; index++) {
				computeConfusionMatrix(reader, confusionMatrix, docLabel, dataset.getDocNb(docLabel, index));
			}
		}
		System.out.println();
		
		int nbLabeling = 0;
		int nbCorrectLabeling = 0;
		int[] lineSum = new int[dataset.getNbLabels()];
		StringBuilder builder = new StringBuilder();
		for (int iLabel=0; iLabel<dataset.getNbLabels(); iLabel++) {
			lineSum[iLabel] = 0;
			if (iLabel!=0) {
				builder.append("\n");
			}
			for (int jLabel=0; jLabel<dataset.getNbLabels(); jLabel++) {
				lineSum[iLabel] += confusionMatrix[iLabel][jLabel];
				System.out.print("\t" + confusionMatrix[iLabel][jLabel]);
				builder.append("\t" + confusionMatrix[iLabel][jLabel]);
			}
			nbCorrectLabeling += confusionMatrix[iLabel][iLabel];
			System.out.println("\trate: " + (int)(100*confusionMatrix[iLabel][iLabel]/(double)lineSum[iLabel]) + "%");
			builder.append("\t" + (confusionMatrix[iLabel][iLabel]/(double)lineSum[iLabel]));
			nbLabeling += lineSum[iLabel];
		}
		double rate = nbCorrectLabeling/(double)nbLabeling;
		System.out.println("\n\tAverage rate(%): " + (100*rate));
		builder.append("\t" + rate);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(dataset.getResultsPath() + classifierName + "_" + lowerPercent + "_" + upperPercent));
		writer.write(builder.toString());
		writer.close();
		System.out.println("...done!\n");
		return rate;
	}
	
	protected abstract void computeConfusionMatrix(IndexReader reader, int[][] confusionMatrix, int docLabel, int docNb)  throws IOException;
	
	public double test(double percents) throws IOException {
		double rateSum = 0;
		int nbTests = (int)(1/percents);
		
		for (int i=0; i<nbTests; i++) {
			double lowerPercent = i*percents;
			double upperPercent = (i+1)*percents;
			learn(lowerPercent, upperPercent);
			
			double rate = test(lowerPercent, upperPercent);
			rateSum += rate;
		}
		double rate = rateSum/nbTests;
		System.out.println("\n OVERWHOLE AVERAGE RATE: " + rate);
		return rate;
	}
}