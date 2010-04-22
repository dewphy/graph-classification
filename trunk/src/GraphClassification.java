import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;

import utilities.GraphTools;

import classifier.Bayes;
import classifier.Classifier;
import classifier.PrTfIdf;
import classifier.TfIdf;
import dataset.Dataset;
import dataset.NewsGroups;
import dataset.UsPatents;


public class GraphClassification {
	
	public static void main(String[] args) throws IOException, ParseException {
		
		UsPatents usPatents = new UsPatents();
		usPatents.loadIndex(0);
		
		StringBuilder builder = new StringBuilder();
		for (int docNb = 0; docNb < usPatents.getNbDocs(); docNb++) {
			builder.append(docNb + " " + usPatents.getMapping(docNb) + "\t");
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(usPatents.getDatasetPath() + "docNb2mapping.txt"));
		writer.write(builder.toString());
		writer.close();
		
//		Dataset dataset = null;
//		Classifier classifier = null;
//		
//		for (int i=0; i<args.length; i++) {
//			
//			if (args[i].equalsIgnoreCase("-Dataset")) {
//				if (args[++i].equalsIgnoreCase("NewsGroups")) {
//					dataset = new NewsGroups();
//				} else if (args[i].equalsIgnoreCase("UsPatents")) {
//					dataset = new UsPatents();
//				} else {
//					throw new NullPointerException("Unknows dataset " + args[i]);
//				}
//			}
//			
//			else if (args[i].equalsIgnoreCase("-Classifier")) {
//				if (args[++i].equalsIgnoreCase("TfIdf")) {
//					classifier = new TfIdf(dataset);
//				} else if (args[i].equalsIgnoreCase("Bayes")) {
//					classifier = new Bayes(dataset);
//				} else if (args[i].equalsIgnoreCase("PrTfIdf")) {
//					classifier = new PrTfIdf(dataset);
//				} else {
//					throw new NullPointerException("Unknows classifier " + args[i]);
//				}
//			}
//			
////			else if (args[i].equalsIgnoreCase("-learn")) {
////				double lowerPercent = Double.valueOf(args[++i]);
////				double upperPercent = Double.valueOf(args[++i]);
////				classifier.learn(lowerPercent, upperPercent);
////			}
//			
//			else if (args[i].equalsIgnoreCase("-test")) {
//				double lowerPercent = Double.valueOf(args[++i]);
//				double upperPercent = lowerPercent + Double.valueOf(args[++i]);;
//				classifier.learn(lowerPercent, upperPercent);
//				classifier.test(lowerPercent, upperPercent);
//			}
//			
//			else if (args[i].equalsIgnoreCase("-createIndex")) {
//				dataset.createIndex();
//			} else if (args[i].equalsIgnoreCase("-loadIndex")) {
//				dataset.loadIndex(0);
//			}
//			
//			else if (args[i].equalsIgnoreCase("-constructGraph")) {
//				int lowerIndex = Integer.valueOf(args[++i]);
//				int upperIndex = lowerIndex + Integer.valueOf(args[++i]);
//				GraphTools graphConstructor = new GraphTools(dataset);
//				graphConstructor.construct(lowerIndex, upperIndex);
//			}
//			
//			else {
//				throw new NullPointerException("Unknows parameter " + args[i]);
//			}
//		}
	}
}