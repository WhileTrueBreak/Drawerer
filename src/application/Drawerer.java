package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.common.Pair;
import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.geometry.Vector2D;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.RobotMotion;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.SplineMotionCP;
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

	private ForceCondition touch10;
	private ForceCondition touch15;
	
	
	@Override
	public void initialize() {
		
		//init force touch condition
		touch10 = ForceCondition.createSpatialForceCondition(gripper.getFrame("/TCP"), 10);
		touch15 = ForceCondition.createSpatialForceCondition(gripper.getFrame("/TCP"), 15);
		
		// Initializes the boing boing
		springRobot = new CartesianImpedanceControlMode(); 
		
		// Set stiffness

		// TODO: Stiff in every direction except plane perpendicular to flange
		springRobot.parametrize(CartDOF.X).setStiffness(1000);
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

	public static List<List<Vector2D>> getPathsFromString(String pathString){
		String[] pathStrings = pathString.split("\\|");
		List<String[]> coordStrings = new ArrayList<String[]>();
		for(String string:pathStrings) {
			coordStrings.add(string.split("(?<=\\d)-"));
		}
		
		List<List<Vector2D>> paths = new ArrayList<List<Vector2D>>();
		for(String[] e:coordStrings) {
			List<Vector2D> path = new ArrayList<Vector2D>();
			for(String coordString:e) {
				if(coordString.length() == 0) continue;
				String[] c = coordString.split(",");
				Vector2D coord = new Vector2D(MathHelper.clamp(Double.parseDouble(c[0]),0,1), MathHelper.clamp(Double.parseDouble(c[1]),0,1));
				path.add(coord);
			}
			path.add(path.get(0));
			paths.add(path);
		}
		return paths;
	}

	private void penUp(){
		logger.info("Moving Pen Up");
		gripper.move(linRel(0,0, -20).setJointVelocityRel(0.2));
	}
	
	private void penDown(){
		logger.info("Moving Pen Down");
		gripper.move(linRel(0, 0, 15).setMode(springRobot).setCartVelocity(20));
	}
	
	private Frame calibrateFrame(Tool grip){
		IMotionContainer motion1 = gripper.move(linRel(0, 0, 150, gripper.getFrame("/TCP")).setCartVelocity(10).breakWhen(touch10));
		if (motion1.getFiredBreakConditionInfo() == null){
			logger.info("No Collision Detected");
			return null;
		}
		else{
			logger.info("Collision Detected");
			return robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"));
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

	private Vector3D canvasToWorld(Vector2D point, Pair<Vector3D, Vector3D> canvas, double size){
		return canvas.getA().multiply(point.getX()*size).add(canvas.getB().multiply(point.getY()*size));
	}
	
	private Frame vectorToFrame(Vector3D vector, Frame baseFrame){
		return new Frame(vector.getX(), vector.getY(), vector.getZ(), baseFrame.getAlphaRad(), baseFrame.getBetaRad(), baseFrame.getGammaRad());
	}

	private Spline framesToSpline(Frame[] frames){
		SplineMotionCP<?>[] motions = new SplineMotionCP[frames.length];
		for (int i=0;i<frames.length;i++){
			motions[i] = lin(frames[i]);
		}

		return new Spline(motions).setBlendingCart(0);
		// return new Spline((SPL[])Arrays.asList(frames).stream().map(x->spl(x)).collect(Collectors.toList()).toArray());
	}

	private void springyMove(Spline path){
		int vel = 40;
		gripper.move(path.setMode(springRobot).setCartVelocity(vel));
	}
	
	private double maxMove(Vector3D dir) {
		Vector3D normDir = dir.normalize();
		double moveThresh = 10;
		double moveDist = 1000;
		double totalDist = 0;
		Vector3D moveVector = normDir.multiply(moveDist);
		while(true) {
			if(moveDist <= moveThresh) break;
			try {
				moveVector = normDir.multiply(moveDist);
				gripper.move(linRel(moveVector.getY(), moveVector.getZ(), moveVector.getX()).setCartVelocity(100));
				totalDist += moveDist;
			} catch (Exception e) {
				moveDist /= 2;
			}
		}
		logger.info("Moved: " + totalDist + "mm");
		return totalDist;
	}
	
	private void safeMove(RobotMotion<?> motion) throws Exception {
		IMotionContainer motionContainer = gripper.move(motion.breakWhen(touch15));
		if(motionContainer.getFiredBreakConditionInfo() != null) {
			logger.error("Touched something on safe move");
			logger.error(motionContainer.getFiredBreakConditionInfo().toString());
			logger.error(motionContainer.getErrorMessage());

			throw new Exception("Safe move tiggered");
		}
	}
	
	@Override
	public void run() throws Exception {
		// Calibration sequence
		mF.setLEDBlue(true);
		logger.info("Moving to bottom left");
		try {
			gripper.move(lin(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		} catch (Exception e) {
			gripper.move(ptp(getApplicationData().getFrame("/bottom_left")).setJointVelocityRel(0.2));
		}
		logger.info("Calibrating point 1");
		Frame originFrame = calibrateFrame(gripper);
		penUp();
		Frame originUpFrame = robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"));
		Vector3D origin = frameToVector(originFrame);
		logger.info(String.format("Origin: %s", origin.toString()));

		logger.info("Moving to Origin up");
		safeMove(lin(originUpFrame).setJointVelocityRel(0.2));
		gripper.move(linRel(0, 40, 0).setJointVelocityRel(0.2));
		logger.info("Calibrating point 2");
		Vector3D up = frameToVector(calibrateFrame(gripper));
		penUp();
		logger.info(String.format("Up: %s", up.toString()));

		logger.info("Moving to Origin up");
		safeMove(lin(originUpFrame).setJointVelocityRel(0.2));
		gripper.move(linRel(-40, 0,0).setJointVelocityRel(0.2));
		logger.info("Calibrating point 3");
		Vector3D right = frameToVector(calibrateFrame(gripper));
		penUp();
		logger.info(String.format("Right: %s", right.toString()));
		

		// get world unit vectors
		Pair<Vector3D,Vector3D> canvas = getCanvasPlane(origin, up, right);
		logger.info(String.format("Canvas X, Y: (%s), (%s)", canvas.getA().toString(), canvas.getB().toString()));

		// check upper right bound
		gripper.move(lin(originUpFrame).setJointVelocityRel(0.2));
		Vector3D diag = canvas.getA().add(canvas.getB());
		logger.info("Diagonal vector: " + diag.toString());
		logger.info("Moving to top right");
		double dist = maxMove(diag);
		logger.info(String.format("Found max at top right: %s", diag.toString()));
		
		// gets top right frame
		Vector3D top_right = frameToVector(robot.getCurrentCartesianPosition(gripper.getFrame("/TCP")));
		double diag_mag = top_right.subtract(origin).length();
		double size = Math.min(diag_mag/Math.sqrt(2), Math.sqrt(dist*dist/2))*0.9;
		logger.info(String.format("Canvas size: %f", size));
		mF.setLEDBlue(false);
		logger.info("Calibration completed.");
		
		logger.info("Reading Path File");
		String resPath = FileReader.findUniqueFolder("res", "..");
		List<String> file = FileReader.readFile(resPath+"/mai_c.txt");
		if(file == null || file.size() != 1) {
			logger.info("File is invalid");
			return;
		}
		List<List<Vector2D>> paths = getPathsFromString(file.get(0));
		Spline[] splines = new Spline[paths.size()];
		
		logger.info("Creating Spline");
		Vector3D v = Vector3D.of(10,0,0);
		for (int i=0;i<paths.size();i++){
			Frame[] tempFrames = new Frame[paths.get(i).size()];
			for (int j=0;j<paths.get(i).size();j++) {
				Vector3D path3D = canvasToWorld(paths.get(i).get(j), canvas, size).add(origin).add(v);
				tempFrames[j] = vectorToFrame(path3D, originFrame);
			}

			splines[i] = framesToSpline(tempFrames);
		}

		logger.info("Start Drawing");
		ListIterator<Spline> splineIterator = Arrays.asList(splines).listIterator();
		while(splineIterator.hasNext()){
			int index = splineIterator.nextIndex();
			logger.info("Start path "+index);
			gripper.move(lin(originUpFrame).setCartVelocity(300));
			Vector3D first = canvasToWorld(paths.get(index).get(0), canvas, size);
			logger.info("Moving to first frame");
			gripper.move(linRel(first.getY(), first.getZ(), first.getX()).setCartVelocity(300));
			penDown();
			logger.info("Start spline path");
			springyMove(splineIterator.next());
			logger.info("Finished path");
			penUp();
		}
		
		logger.info("Moving to base");
		gripper.move(lin(originUpFrame).setJointVelocityRel(0.2));
		penUp();
		mF.setLEDBlue(true);
	}
}
