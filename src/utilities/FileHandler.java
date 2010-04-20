package utilities;

import java.io.File;

public class FileHandler {
	
	public static boolean deleteDirectory(File path) {
		emptyDirectory(path);
		return(path.delete());
	}
	
	public static void emptyDirectory(File path) {
		if(path.exists()) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
	}
}