package application;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.task.ITaskLogger;
import com.kuka.common.ThreadUtil;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;

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
public class RobotPickandPlace extends RoboticsAPIApplication {
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
	
	@Override
	public void initialize() {
		gripper.attachTo(robot.getFlange());
		gripper2F1.initalise();
		gripper2F1.setSpeed(189);
		gripper2F1.open();

//		mF.setLEDBlue(true);
//		ThreadUtil.milliSleep(200);
//		mF.setLEDBlue(false);
//		mF.setLEDRed(true);
//		ThreadUtil.milliSleep(200);
//		mF.setLEDRed(false);
//		mF.setLEDGreen(true);
//		ThreadUtil.milliSleep(200);
//		mF.setLEDGreen(false);
	}

	@Override
	public void run() {
		int Hspeed = 400;//movement speed
		int AV = 5;// angular velocity
		int Descendspeed = 100;//pick and place speed
		int cs = 30; //cube size
		int clearance = 140;//distance to avoid collision
	

		
		gripper.move(ptp(getApplicationData().getFrame("/P3")).setJointVelocityRel(0.5)); //home position
	    
		gripper.move(ptp(getApplicationData().getFrame("/P1")).setJointVelocityRel(0.5));//frame1
	    gripper.move(linRel(0, 0, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, ((-3)*cs), World.Current.getRootFrame()).setCartVelocity(Descendspeed));//going down to pick
		gripper2F1.close();//close gripper
		gripper.move(lin(getApplicationData().getFrame("/P1")).setCartVelocity(Hspeed));//get back to frame1
		gripper.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.5));// go to frame2
	    gripper.move(linRel(0, 0, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, (-3)*cs, World.Current.getRootFrame()).setCartVelocity(Descendspeed));// going down to place
		gripper2F1.open();//open gripper
	    gripper.move(lin(getApplicationData().getFrame("/P2")).setCartVelocity(Hspeed));

		  //second cube
	    gripper.move(ptp(getApplicationData().getFrame("/P1")).setJointVelocityRel(0.5));//frame1
	    gripper.move(linRel(0, 35, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, ((-3)*cs), World.Current.getRootFrame()).setCartVelocity(Descendspeed));//going down to pick
		gripper2F1.close();
		gripper.move(lin(getApplicationData().getFrame("/P1")).setCartVelocity(Hspeed));//get back to frame1
		gripper.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.5));// go to frame2
	    gripper.move(linRel(0, 0, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, ((-2)*cs), World.Current.getRootFrame()).setCartVelocity(Descendspeed));// going down to place
		gripper2F1.open();
	    gripper.move(lin(getApplicationData().getFrame("/P2")).setCartVelocity(Hspeed)); //home position

	    //3rd
	    gripper.move(ptp(getApplicationData().getFrame("/P1")).setJointVelocityRel(0.5));//frame1
	    gripper.move(linRel(0, 70, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, ((-3)*cs), World.Current.getRootFrame()).setCartVelocity(Descendspeed));//going down to pick
		gripper2F1.close();
		gripper.move(lin(getApplicationData().getFrame("/P1")).setCartVelocity(Hspeed));//get back to frame1
		gripper.move(ptp(getApplicationData().getFrame("/P2")).setJointVelocityRel(0.5));// go to frame2
	    gripper.move(linRel(0, 0, (-clearance), World.Current.getRootFrame()).setCartVelocity(Hspeed));//going down
	    gripper.move(linRel(0, 0, ((-1)*cs), World.Current.getRootFrame()).setCartVelocity(Descendspeed));// going down to place
		gripper2F1.open();
	    gripper.move(lin(getApplicationData().getFrame("/P2")).setCartVelocity(Hspeed)); //home position
	    gripper.move(ptp(getApplicationData().getFrame("/P3")).setJointVelocityRel(0.5)); //home position
	    

	   
	    
	    


	}
}