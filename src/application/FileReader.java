package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.grpc.binarylog.GrpcLogEntry.Logger;

public class FileReader{
    public static List<String> readFile(String filename) throws FileNotFoundException {
		try {
			List<String> output = new ArrayList<String>();
			File file = new File(filename);
			Scanner reader = new Scanner(file);
			while (reader.hasNextLine()) 
				output.add(reader.nextLine());
			reader.close();
			return output;
	    } catch (FileNotFoundException e) {
	    	throw e;
	    }
	}
}