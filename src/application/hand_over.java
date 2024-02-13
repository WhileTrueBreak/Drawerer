package application;
 
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
//import com.kuka.math.geometry.Vector3D;
import com.kuka.nav.command.CommandContainer;
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
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
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
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.SplineMotionCP;
import com.kuka.roboticsAPI.motionModel.SplineOrientationType;
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
	private timed cartData;
	CartesianImpedanceControlMode springRobot;
	IMotionContainer m1;
 
	@Override
	public void initialize() {
		gripper.attachTo(robot.getFlange());
		gripper2F1.initalise();
		gripper2F1.setForce(60);
		gripper2F1.setSpeed(100);
		gripper2F1.setPos(150);
		mF.setLEDBlue(true);
		ThreadUtil.milliSleep(200);
		gripper2F1.close();
		mF.setLEDBlue(false);
		ThreadUtil.milliSleep(200);
		cartData.initialize();
		cartData.run();
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
		Frame newPosition = robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"),World.Current.getRootFrame());
		
		Vector3D distance = new Vector3D((newPosition.getX()-pose.getX()), (newPosition.getY()-pose.getY()), (newPosition.getZ()-pose.getZ()));
		return distance;
	}
	
	@Override
	public void run() {
//		IHonkCapability honkCapability = kmp.getCapability(IHonkCapability.class);
//		honkCapability.honk();
		double velocity = cartData.vel;
		double acceleration = cartData.acc;
		logger.info("arm velocity :" + velocity);
		logger.info("arm acceleration :" + acceleration);
		for (int i = 0; i < 1; i++) {
			gripper2F1.setPos(100);
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(180);
			gripper.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.5).setMode(springRobot));
			double offset = -i*70;
			robot.move(linRel(0,offset,0,0,0,0).setJointVelocityRel(0.3).setMode(springRobot));
			gripper.move(linRel(0,0,-60,0,0,0, World.Current.getRootFrame()).setJointVelocityRel(0.3).setMode(springRobot));
			ThreadUtil.milliSleep(1000);
			gripper2F1.close();
			ThreadUtil.milliSleep(200);
			logger.info("arm velocity :" + velocity);
			logger.info("arm acceleration :" + acceleration);
			while (gripper2F1.readObjectDetection() == 3){
				logger.info("No objects detected");
				gripper2F1.setPos(100);
				mF.setLEDBlue(true);
				ThreadUtil.milliSleep(1000);
				mF.setLEDBlue(false);
				gripper2F1.close();
			}
			gripper2F1.close();
			logger.info("Object detected");
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			gripper.move(linRel(0,0,60,0,0,0, World.Current.getRootFrame()).setJointVelocityRel(0.3).setMode(springRobot));
			Spline mySpline = new Spline(
					spl(getApplicationData().getFrame("/handOver/P1")),
					spl(getApplicationData().getFrame("/handOver"))
			);
			
			gripper.move(mySpline.setJointVelocityRel(0.4).setMode(springRobot));
			
			CartesianSineImpedanceControlMode lissajousMode;
			lissajousMode = CartesianSineImpedanceControlMode.createLissajousPattern(CartPlane.YZ, 0.7, 4.0, 300.0);
			lissajousMode.parametrize(CartDOF.A).setStiffness(80);
			lissajousMode.parametrize(CartDOF.B).setStiffness(80);
			lissajousMode.parametrize(CartDOF.C).setStiffness(80);
			lissajousMode.parametrize(CartDOF.X).setStiffness(300);
			IMotionContainer m1 = robot.moveAsync(positionHold(lissajousMode, 60, TimeUnit.SECONDS));
			
			Frame pose = robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"),World.Current.getRootFrame());
			logger.info("Please take the object!");
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			
			while (true) {
				Vector3D v1 = dist(pose);
				velocity = cartData.vel;
				if ((v1.length() > 80 && v1.getY() < -10) || (velocity > 250)) {
					mF.setLEDBlue(true);
					gripper2F1.open();
					logger.info("velocity : " + velocity);
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
			
			mF.setLEDBlue(true);
			ThreadUtil.milliSleep(200);
			mF.setLEDBlue(false);
			IMotionContainer m2 = robot.moveAsync(positionHold(lissajousMode, 40, TimeUnit.SECONDS));
			logger.info("hit me to grab or go back");
			gripper2F1.open();
			
			pose = robot.getCurrentCartesianPosition(gripper.getFrame("/TCP"),World.Current.getRootFrame());
			while (true) {
				Vector3D v2 = dist(pose);
				velocity = cartData.vel;
				if (v2.length() > 25 || velocity > 170) {
					mF.setLEDBlue(true);
					logger.info("velocity : " + velocity);
					logger.info("yaaaaayyyyyyyyy :)");
					gripper2F1.close();
					mF.setLEDBlue(false);
					break;
				} else if (m2.isFinished()) {
					logger.info("Sorry, Time out!");
					break;
				}
			}
			m2.cancel();
			robot.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.4).setMode(springRobot));
		}
		cartData.stop();
	}
}