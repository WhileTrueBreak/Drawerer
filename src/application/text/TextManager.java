package application.text;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.parser.FileReader;
import application.path.PointPath;
import application.robotControl.Canvas;
import application.utils.Handler;

public class TextManager {

	private static Map<Integer, PointPath> charTable = new HashMap<Integer, PointPath>();
	private static String fontPath = null;
	private static double baseScale = 1;
	
	public static void setFontPath(String path) {
		TextManager.fontPath = path;
	}
	
	public static void loadChar(char c, Canvas canvas) {
		if(fontPath == null) return;
		List<String> file = null;
		try {
			file = FileReader.readFile(TextManager.fontPath+"/"+((int) c)+".txt");
		} catch (FileNotFoundException e) {
			Handler.getLogger().info("Failed to load char: \"" + c + "\"");
			System.out.println("Failed to load char: \"" + c + "\"");
		}
		if(file == null) return;
		charTable.put((int) c, PointPath.createPointPathsV2(file, canvas, baseScale));
	}
	
	public static PointPath getCharPath(char c) {
		if(!charTable.containsKey((int) c)) return null;
		return charTable.get((int) c);
	}
	
	public static void setBaseScale(double scale) {
		baseScale = scale;
	}
	
}
