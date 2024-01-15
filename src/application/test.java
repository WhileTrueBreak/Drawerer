package application;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.geometry.Vector2D;

import application.parser.FileReader;
import application.parser.PathParser;
import application.path.Node;
import application.path.Path;
import application.utils.Bezier;

public class test {

	public static void main(String[] args) throws InterruptedException, IOException {
		Vector3D v1 = Vector3D.of(0, 0, 0);
		Vector3D v2 = Vector3D.of(1, 0, 0);
		Vector3D v3 = Vector3D.of(1, 1, 0);
		
		List<Vector3D> controlPoints = new ArrayList<Vector3D>();
		controlPoints.add(v1);
		controlPoints.add(v2);
		controlPoints.add(v3);
		
		double length = Bezier.approxBezierLength(controlPoints, 100);
		System.out.println(length);
	}

}
