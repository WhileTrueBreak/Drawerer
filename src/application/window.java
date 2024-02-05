package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.circ;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.spl;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.common.Pair;
import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.geometry.Vector2D;
import com.kuka.roboticsAPI.applicationModel.IApplicationData;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.AbstractFrame;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ITransformationProvider;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.ITransformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.SPL;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.persistenceModel.IPersistenceEngine;
import com.kuka.roboticsAPI.persistenceModel.XmlApplicationDataSource;
import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.task.ITaskLogger;

public class window extends RoboticsAPIApplication{
	@Inject
	private LBR robot;

	@Inject 
	private Gripper2F gripper2F1;

	@Inject
	private MediaFlangeIOGroup mF;

	@Inject
	@Named("RobotiqGripper")
	private Tool gripper;
	
	@Inject
	private ITaskLogger logger;
	
	private CartesianImpedanceControlMode springRobot;
	
	@Override
	public void initialize() {
		// Initialize
		springRobot = new CartesianImpedanceControlMode(); 
		
		// Set stiffness

		// TODO: Stiff in every direction except plane perpendicular to flange
		springRobot.parametrize(CartDOF.X).setStiffness(1500);
		springRobot.parametrize(CartDOF.Y).setStiffness(1500);
		springRobot.parametrize(CartDOF.Z).setStiffness(1500);

		// Stiff rotation
		springRobot.parametrize(CartDOF.C).setStiffness(200);
		springRobot.parametrize(CartDOF.B).setStiffness(200);
		springRobot.parametrize(CartDOF.A).setStiffness(200);
		springRobot.setReferenceSystem(World.Current.getRootFrame());
		springRobot.parametrize(CartDOF.ALL).setDamping(0.2);
		
		// Inits the Robot
		gripper.attachTo(robot.getFlange());
		gripper2F1.initalise();
		gripper2F1.setSpeed(120);
		gripper2F1.setForce(10);
		mF.setLEDBlue(false);
		gripper2F1.close();
		ThreadUtil.milliSleep(200);
	}
	
	private Frame calibrateFrame(Tool grip, double force){
		ForceCondition touch = ForceCondition.createSpatialForceCondition(gripper.getFrame("/TCP"), force);
		IMotionContainer motion1 = gripper.move(linRel(0, 0, 100, gripper.getFrame("/TCP")).setCartVelocity(30).breakWhen(touch));
		if (motion1.getFiredBreakConditionInfo() == null){
			logger.info("No Collision Detected");
			return null;
		}
		else{
			logger.info("Collision Detected");
			return robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"));
		}

	}
	
	private Pair<Vector3D, Vector3D> getCanvasPlane(Vector3D origin, Vector3D up, Vector3D right){
		Vector3D ver = up.subtract(origin).normalize();
		Vector3D hor = right.subtract(origin).normalize();

		return new Pair<Vector3D, Vector3D>(hor, ver);
	}
	
	private Vector3D frameToVector(Frame frame){
		return Vector3D.of(frame.getX(), frame.getY(), frame.getZ());
	}
	
	public void run() {
		// Calibration sequence
		mF.setLEDBlue(true);
		
		robot.move(ptp(getApplicationData().getFrame("/P1")).setJointVelocityRel(0.5));
//		
		//getting the vector
		robot.move(ptp(getApplicationData().getFrame("/windowHandle/P1")).setJointVelocityRel(0.5));
		logger.info("Calibrating vector point 1");
		Vector3D origin = frameToVector(calibrateFrame(gripper,30));
		logger.info(String.format("Origin: %s", origin.toString()));

		logger.info("Moving to left");
		robot.move(ptp(getApplicationData().getFrame("/windowHandle/P2")).setJointVelocityRel(0.5));
		logger.info("Calibrating vector point 2");
		ThreadUtil.milliSleep(1000);
		Vector3D right = frameToVector(calibrateFrame(gripper,30));
		logger.info(String.format("Right: %s", right.toString()));
		
		logger.info("Moving to up");
		robot.move(ptp(getApplicationData().getFrame("/windowHandle/P4")).setJointVelocityRel(0.5));
		logger.info("Calibrating vector point 2");
		ThreadUtil.milliSleep(1000);
		Vector3D up = frameToVector(calibrateFrame(gripper,35));
		logger.info(String.format("Right: %s", up.toString()));
			
		robot.move(linRel(0, 0, -20).setJointVelocityRel(0.2));
		// get world unit vectors
		Pair<Vector3D,Vector3D> openLine = getCanvasPlane(origin, up, right);
		logger.info(String.format("Canvas X, Y: (%s), (%s)", openLine.getA().toString(), openLine.getB().toString()));

		Spline mySpline = new Spline(
				spl(getApplicationData().getFrame("/windowHandle/lockUp")),
				spl(getApplicationData().getFrame("/windowHandle/P5")),
				spl(getApplicationData().getFrame("/windowHandle/P6")),
				spl(getApplicationData().getFrame("/windowHandle/P7")),
				spl(getApplicationData().getFrame("/windowHandle/lockDown")),
				spl(getApplicationData().getFrame("/windowHandle/P3")));
				// ...
		robot.move(ptp(getApplicationData().getFrame("/windowHandle/lockUp")));
		robot.move(mySpline.setJointVelocityRel(0.4));			
				
				
		Boolean con1 = true;
		while (con1) {
			ForceSensorData data = robot.getExternalForceTorque(robot.getFlange(),World.Current.getRootFrame());
			Vector vForce = data.getForce();
			double forceInY = vForce.getY();
			forceInY = Math.abs(forceInY);
			if (forceInY < 25){
				robot.move(linRel(0, 0, 1).setJointVelocityRel(0.3));
			} else {
				con1 = false;
				break;
			}
		}
		
		robot.move(linRel(0, 0, -10).setJointVelocityRel(0.3));
		gripper2F1.setPos(20);
		robot.move(linRel(0, 0, 20).setJointVelocityRel(0.3));
		ThreadUtil.milliSleep(100);
		gripper2F1.close();
		
		springRobot.parametrize(CartDOF.X).setStiffness(3000);
		springRobot.parametrize(CartDOF.Y).setStiffness(3000);
		springRobot.parametrize(CartDOF.Z).setStiffness(3000);

		// Stiff rotation
		springRobot.parametrize(CartDOF.C).setStiffness(500);
		springRobot.parametrize(CartDOF.B).setStiffness(500);
		springRobot.parametrize(CartDOF.A).setStiffness(500);
		springRobot.setReferenceSystem(World.Current.getRootFrame());
		springRobot.parametrize(CartDOF.ALL).setDamping(1);
		
		Vector3D diag = openLine.getA().multiply(600);
		logger.info("moving on a line");
		double acc = 20;
		robot.move(linRel(diag.getZ(), diag.getX(), diag.getY()).setCartVelocity(10).setCartAcceleration(acc));

	}
}

