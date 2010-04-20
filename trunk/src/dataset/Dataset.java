package dataset;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import utilities.FileHandler;


public abstract class Dataset {
	
	public static final String ROOT = "/fs1/pdewagter";
	
	public static final String DATASETS = ROOT + "/datasets/";
	public static final String INDEXES = ROOT + "/indexes/";
	public static final String GRAPHS = ROOT + "/graphs/";
	public static final String RESULTS = ROOT + "/results/";
	
	protected final String datasetName;
	
	protected int nbDocs;
	protected int nbTerms;
	protected int nbLabels;
	
	protected int[] docNb2Id;
	protected int[] docNb2Label;
	protected int[] docNb2Lenght;
	protected int[] label2NbDocs;
	
	protected List<List<Integer>> docNbs;
	
	public static class FieldName {
		public static final String ID = "ID";
		public static final String LABEL = "LABEL";
		public static final String CONTENT = "CONTENT";
	}
	
	public Dataset(String datasetName) {
		this.datasetName = datasetName;
		if (!new File(getDatasetPath()).isDirectory()) {
			throw new NullPointerException("Dataset not found at " + getDatasetPath());
		}
	}
	
	public void createIndex() throws IOException {
		System.out.println("\nCreating index...");
		FileHandler.deleteDirectory(new File(getIndexPath()));
		if (!new File(INDEXES).isDirectory()) {
			FileHandler.deleteDirectory(new File(INDEXES));
		}
		createIndex(getIndexPath());
		System.out.println("...done!\n");
	}
	
	protected abstract void createIndex(String indexPath) throws IOException;
	
	public void loadIndex(int seed) throws IOException {
		System.out.println("\nLoading index...");
		if (!new File(getIndexPath()).isDirectory()) {
			throw new NullPointerException("Dataset not found at " + getIndexPath());
		}
		loadIndex(getIndexPath());
		System.out.println("...done!\n");
		shuffle(seed);
	}
	
	protected abstract void loadIndex(String indexPath) throws IOException;
	
	protected void shuffle(int seed) {
		System.out.println("Shuffling index with seed: " + seed + "...");
		for (int label=0; label<nbLabels; label++) {
			Collections.shuffle(docNbs.get(label), new Random(seed));
		}
		System.out.println("...done!\n");
	}
	
	public String getDatasetPath() {
		return DATASETS + datasetName;
	}
	
	public String getIndexPath() {
		return INDEXES + datasetName;
	}
	
	public String getGraphPath() {
		return GRAPHS + datasetName;
	}
	
	public String getResultsPath() {
		return RESULTS + datasetName;
	}
	
	public int getNbDocs() {
		return nbDocs;
	}
	
	public int getNbDocs(int label) {
		return label2NbDocs[label];
	}
	
	public int getNbTerms() {
		return nbTerms;
	}
	
	public int getNbLabels() {
		return nbLabels;
	}
	
	public int getDocNb(int label, int index) {
		return docNbs.get(label).get(index);
	}
	
	public int getDocLenght(int docNb) {
		return docNb2Lenght[docNb];
	}
	
	public int getId(int docNb) {
		return docNb2Id[docNb];
	}
	
	public int getLabel(int docNb) {
		return docNb2Label[docNb];
	}
}