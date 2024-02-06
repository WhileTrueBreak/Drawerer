package application;
import java.util.concurrent.TimeUnit;
 
import javax.inject.Inject;
import javax.inject.Named;
 
//import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.geometry.Vector2D;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.capabilities.honk.IHonkCapability;
import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.ICallbackAction;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.ITriggerAction;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.kmp.SunriseOmniMoveMobilePlatform;
import com.kuka.roboticsAPI.executionModel.IFiredTriggerInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.CartPlane;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.RelativeLIN;
import com.kuka.roboticsAPI.motionModel.SplineMotionCP;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.sensorModel.TorqueSensorData;
import com.kuka.task.ITaskLogger;
import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.vividsolutions.jts.math.Vector3D;
 
 
import static com.kuka.roboticsAPI.motionModel.HRCMotions.*;
 

/**
* Implementation of a robot application.
* <p>
* The application provides a {@link RoboticsAPITask#initialize()} and a 
* {@link RoboticsAPITask#run()} method, which will be called successively in 
* the application lifecycle. The application will terminate automatically after 
* the {@link RoboticsAPITask#run()} method has finished or after stopping the 
* task. The {@link RoboticsAPITask#dispose()} method will be called, even if an 
* exception is thrown during initialization or run. 
* <p>
* <b>It is imperative to call <code>super.dispose()</code> when overriding the 
* {@link RoboticsAPITask#dispose()} method.</b> 
* 
* @see UseRoboticsAPIContext
* @see #initialize()
* @see #run()
* @see #dispose()
*/
public class hand_over extends RoboticsAPIApplication {
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
	@Inject 
	private SunriseOmniMoveMobilePlatform kmp;
	CartesianImpedanceControlMode springRobot;
	IMotionContainer m1;
	private double PosX, PosY, PosZ;
	@Override
	public void initialize() {
		gripper.attachTo(robot.getFlange());
		gripper2F1.initalise();
		gripper2F1.setForce(10);
		gripper2F1.setSpeed(150);
		gripper2F1.setPos(100);
		mF.setLEDBlue(true);
		ThreadUtil.milliSleep(200);
		gripper2F1.close();
		mF.setLEDBlue(false);
		ThreadUtil.milliSleep(200);
		//Spring motion initialisation
		springRobot = new CartesianImpedanceControlMode(); 
		springRobot.parametrize(CartDOF.X).setStiffness(200); 
		springRobot.parametrize(CartDOF.C).setStiffness(100);
		springRobot.parametrize(CartDOF.B).setStiffness(100);
		springRobot.parametrize(CartDOF.A).setStiffness(100);
		springRobot.setReferenceSystem(World.Current.getRootFrame());
		springRobot.parametrize(CartDOF.ALL).setDamping(0.7);
		//USAGE, will move to next line when triggered
		//LOOK at pipecutting.java for examples on analysing the break condition. 
		//gripper.move(linRel(0, 0, -30, World.Current.getRootFrame()).setCartVelocity(50).breakWhen(touch10)); 
		
	

	}
	private Vector3D dist(Frame pose){
		Frame newPosition = robot.getCurrentCartesianPosition(robot.getFlange());
		Vector3D distance = new Vector3D((pose.getX()-newPosition.getX()), (pose.getY()-newPosition.getY()), (pose.getZ()-newPosition.getZ()));
		return distance;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		IHonkCapability honkCapability = kmp.getCapability(IHonkCapability.class);

		honkCapability.honk();
		for (int i = 0; i < 5; i++) {
			gripper2F1.setPos(100);
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(180);
			robot.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.5).setMode(springRobot));//frame1
			//robot.move(ptp(getApplicationData().getFrame("/P2/P1")).setJointVelocityRel(0.4));//frame1
			double offset = -i*50;
			robot.move(linRel(0,offset,0,0,0,0).setJointVelocityRel(0.3).setMode(springRobot));
			robot.move(linRel(0,0,60,0,0,0).setJointVelocityRel(0.3).setMode(springRobot));
			ThreadUtil.milliSleep(1000);
			gripper2F1.close();
			ThreadUtil.milliSleep(200);
			while (gripper2F1.readObjectDetection() == 3){
				logger.info("No objects detected");
				honkCapability.honk();
				gripper2F1.setPos(100);
				mF.setLEDBlue(true);
				ThreadUtil.milliSleep(1000);
				mF.setLEDBlue(false);
				gripper2F1.close();
				//robot.move(ptp(getApplicationData().getFrame("/P2/P1")).setJointVelocityRel(0.4));//frame1
			}
			
			gripper2F1.close();
			logger.info("Object detected");
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			robot.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.4));//frame1
			robot.move(lin(getApplicationData().getFrame("/P3")).setJointVelocityRel(0.4));
			
//			CartesianSineImpedanceControlMode sineMode;
//			sineMode = CartesianSineImpedanceControlMode.createSinePattern(CartDOF.Y, 5, 12.0, 700.0);
//			//robot.move(linRel(0.0,0.0,0.0).setCartVelocity(100).setMode(sineMode));
//			
			CartesianSineImpedanceControlMode lissajousMode;
			lissajousMode = CartesianSineImpedanceControlMode.createLissajousPattern(CartPlane.YZ, 0.7, 4.0, 400.0);
			lissajousMode.parametrize(CartDOF.A).setStiffness(100);
			lissajousMode.parametrize(CartDOF.B).setStiffness(100);
			lissajousMode.parametrize(CartDOF.C).setStiffness(100);
			lissajousMode.parametrize(CartDOF.X).setStiffness(400);
			
			IMotionContainer m1 = robot.moveAsync(positionHold(lissajousMode, 20, TimeUnit.SECONDS));
			Frame pose = robot.getCurrentCartesianPosition(robot.getFlange());
			logger.info("Please take the object!");
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			while (true) {
				Vector3D v1 = dist(pose);
				if (v1.length() > 50) {
					mF.setLEDBlue(true);
					gripper2F1.open();
					logger.info("yaaaaayyyyyyyyy :)");
					mF.setLEDBlue(false);
					m1.cancel();
					break;
				} else if (m1.isFinished()) {
					logger.info("Sorry, Time out!");
					break;
				}
			}
			m1.cancel();
			double vel;
			IMotionContainer m3 = robot.move(lin(getApplicationData().getFrame("/P3")).setJointVelocityRel(0.4).setMode(springRobot));
			vel = ((SplineMotionCP<RelativeLIN>) m3).getCartVelocity();
			logger.info("vel is" + vel);
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			IMotionContainer m2 = robot.moveAsync(positionHold(lissajousMode, 20, TimeUnit.SECONDS));
			logger.info("hit me to grab or go back");
			/////
			//robot.move(linRel(Transformation.ofDeg(0,0,0,0,0,90)).setJointVelocityRel(0.6).setMode(springRobot));
			/////
			gripper2F1.open();
			pose = robot.getCurrentCartesianPosition(robot.getFlange());
			while (true) {
				Vector3D v2 = dist(pose);
				if (v2.length() > 50) {
					mF.setLEDBlue(true);
					gripper2F1.close();
					logger.info("yaaaaayyyyyyyy :)");
					mF.setLEDBlue(false);
					break;
				} else if (m2.isFinished()) {
					logger.info("Sorry, Time out!");
					break;
				}
			}
			m2.cancel();
			
			robot.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.4).setMode(springRobot));//frame1
			//robot.move(ptp(getApplicationData().getFrame("/P2/P1")).setJointVelocityRel(0.4));//frame1
		}
	}
}