package application;

import java.io.IOException;

import com.kuka.math.geometry.Vector3D;

import application.utils.MathHelper;

public class test {

	public static void main(String[] args) throws InterruptedException, IOException {
		Vector3D a = Vector3D.of(0, 1, 0);
		Vector3D b = Vector3D.of(1, 0, 0);
		double angle = a.angleRad(b);
		System.out.println(MathHelper.lerp(0,1,MathHelper.clamp(angle/(Math.PI/2),0,1)));
	}

}
