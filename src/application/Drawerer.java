package application;

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
import com.kuka.core.geometry.Vector;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.geometry.Vector2D;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.AbstractFrame;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.SPL;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.task.ITaskLogger;

public class Drawerer extends RoboticsAPIApplication{
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
		
		// Initializes the boing boing
		springRobot = new CartesianImpedanceControlMode(); 
		
		// Set stiffness

		// TODO: Stiff in every direction except plane perpendicular to flange
		springRobot.parametrize(CartDOF.X).setStiffness(500);
		springRobot.parametrize(CartDOF.Y).setStiffness(5000);
		springRobot.parametrize(CartDOF.Z).setStiffness(5000);

		// Stiff rotation
		springRobot.parametrize(CartDOF.C).setStiffness(300);
		springRobot.parametrize(CartDOF.B).setStiffness(300);
		springRobot.parametrize(CartDOF.A).setStiffness(300);
		springRobot.setReferenceSystem(World.Current.getRootFrame());
		springRobot.parametrize(CartDOF.ALL).setDamping(0.4);
		
		// Inits the Robot
		gripper.attachTo(robot.getFlange());
		gripper2F1.initalise();
		gripper2F1.setSpeed(189);
//		gripper2F1.open();
//		mF.setLEDBlue(true);
//		ThreadUtil.milliSleep(10000);
		mF.setLEDBlue(false);
		gripper2F1.close();
		ThreadUtil.milliSleep(200);
	}

	private Vector2D[][] getPaths(){
		Vector2D[][] path = {{new Vector2D(0.7, 0.6), new Vector2D(0.5,0.5)}};
		return path;
	}

	private void penUp(){
		gripper.move(linRel(0,0, -20).setJointVelocityRel(0.2));
	}
	private void penDown(){
		gripper.move(linRel(0,0, 20).setMode(springRobot).setJointVelocityRel(0.2));
	}
	
	private Frame calibrateFrame(Tool grip){
		ForceCondition touch = ForceCondition.createSpatialForceCondition(gripper.getFrame("/bottom_left"), 10);
		IMotionContainer motion1 = gripper.move(linRel(0, 0, 150, gripper.getFrame("/bottom_left")).setCartVelocity(20).breakWhen(touch));
		if (motion1.getFiredBreakConditionInfo() == null){
			logger.info("No Collision Detected");
			return null;
		}
		else{
			logger.info("Collision Detected");
			return robot.getCurrentCartesianPosition(gripper.getFrame("/bottom_left"));
		}

	}

	private Vector3D frameToVector(Frame frame){
		return Vector3D.of(frame.getX(), frame.getY(), frame.getZ());
	}

	private Pair<Vector3D, Vector3D> getCanvasPlane(Vector3D origin, Vector3D up, Vector3D right){
		Vector3D ver = up.subtract(origin).normalize();
		Vector3D hor = right.subtract(origin).normalize();

		return new Pair<Vector3D, Vector3D>(hor, ver);
	}

	private Vector3D canvasToWorld(Vector2D point, Pair<Vector3D, Vector3D> canvas, Vector3D origin){
		return canvas.getA().multiply(point.getX()).add(canvas.getB().multiply(point.getY())).add(origin);
	}
	
	private Frame vectorToFrame(Vector3D vector, Frame baseFrame){
		return new Frame(vector.getX(), vector.getY(), vector.getZ(), baseFrame.getAlphaRad(), baseFrame.getBetaRad(), baseFrame.getGammaRad());
	}

	private Spline framesToSpline(Frame[] frames){
		SPL[] splines = new SPL[frames.length];
		for (int i=0;i<frames.length;i++){
			splines[i] = spl(frames[i]);
		}

		return new Spline(splines);
		// return new Spline((SPL[])Arrays.asList(frames).stream().map(x->spl(x)).collect(Collectors.toList()).toArray());
	}

	private void springyMove(Spline path){
		double vel = 0.2;
		gripper.move(path.setMode(springRobot).setJointVelocityRel(vel));
	}
	
	IMotionContainer m1;
	@Override
	public void run() {
		// Calibration sequence
		mF.setLEDBlue(true);
		gripper.move(ptp(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		Frame originFrame = calibrateFrame(gripper);
		Vector3D origin = frameToVector(originFrame);

		gripper.move(ptp(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		gripper.move(linRel(0, 20, 0).setJointVelocityRel(0.2));
		Vector3D up = frameToVector(calibrateFrame(gripper));

		gripper.move(ptp(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		gripper.move(linRel(20, 0,0).setJointVelocityRel(0.2));
		Vector3D right = frameToVector(calibrateFrame(gripper));

		// get world unit vectors
		Pair<Vector3D,Vector3D> canvas = getCanvasPlane(origin, up, right);

		// check upper right bound
		double diag_size = 10;
		gripper.move(ptp(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		Vector3D diag = canvas.getA().add(canvas.getB()).multiply(diag_size);
		for(int i =0;i<10;i++){
			gripper.move(linRel(diag.getX(), diag.getY(), diag.getZ()).setCartVelocity(10));
		}

		// gets top right fraem
		Vector3D top_right = frameToVector(robot.getCurrentCartesianPosition(gripper.getFrame("/bottom_left")));
		double diag_mag = top_right.subtract(origin).length();
		double size = diag_mag/diag.length()*diag_size;
		mF.setLEDBlue(false);

		Vector2D[][] paths = getPaths();
		Spline[] splines = new Spline[paths.length];

		for (int i=0;i<paths.length;i++){
			Frame[] tempFrames = new Frame[paths[i].length];
			for (int j=0;j<paths[i].length;j++) {
				tempFrames[j] = vectorToFrame(canvasToWorld(paths[i][j], canvas, origin), originFrame);
			}

			splines[i] = framesToSpline(tempFrames);
			// Frame[] frames = (Frame[])Arrays.asList(path).stream().map(x->vectorToFrame(canvasToWorld(x, canvas, origin), originFrame)).collect(Collectors.toList()).toArray();
			// Spline spline = framesToSpline(frames);
		}

		// Spline[] splines = (Spline[])Arrays.asList(paths).stream().map(y-> framesToSpline((Frame[])Arrays.asList(y).stream().map(x->vectorToFrame(canvasToWorld(x, canvas, origin), originFrame)).collect(Collectors.toList()).toArray())).collect(Collectors.toList()).toArray();
		ListIterator<Spline> splineIterator = Arrays.asList(splines).listIterator();
		penUp();
		while(splineIterator.hasNext()){
			int index = splineIterator.nextIndex();
			gripper.move(lin(vectorToFrame(canvasToWorld(paths[index][0], canvas, origin), originFrame)).setCartVelocity(10));
			penDown();
			springyMove(splineIterator.next());
			penUp();
		}

		mF.setLEDBlue(true);
		//		ThreadUtil.milliSleep(120000);
	}
}
