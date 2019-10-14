package frc.robot;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
    private final String c_autoDefault = "Default";
    private final String c_autoPaths   = "Paths";
    private String m_autoSelected;
    private final SendableChooser<String> m_chooser = new SendableChooser<>();

    // This is the main joystick
    Joystick m_flightStick = new Joystick(0);

    // Dogtail controllers
    Victor m_dogtailLeft  = new Victor(4);
    Victor m_dogtailRight = new Victor(5);
    Spark  m_dogtailPos   = new Spark(7); // Motor that controls the dogtail's position

    // Drivetrain speed controllers
    Spark m_leftFront  = new Spark(0); // Green motor
    Spark m_leftBack   = new Spark(1); // Yellow motor
    Spark m_rightFront = new Spark(2); // Blue motor
    Spark m_rightBack  = new Spark(3); // White motor

    // Group motors into sides
    SpeedControllerGroup m_motorsLeft = new SpeedControllerGroup(m_leftFront, m_leftBack);
    SpeedControllerGroup m_motorsRight = new SpeedControllerGroup(m_rightFront, m_rightBack);

    // Group sides together together
    DifferentialDrive m_drive = new DifferentialDrive(m_motorsLeft, m_motorsRight);

    // Compressor
    Compressor m_compressor = new Compressor(0);

    // Solenoids for manipulating the claw
    DoubleSolenoid m_solenoidClawPos   = new DoubleSolenoid(0, 1);
    DoubleSolenoid m_solenoidClawEject = new DoubleSolenoid(2, 3);

    // Solenoids for raising the robot
    DoubleSolenoid m_solenoidFrontRaise = new DoubleSolenoid(4, 5);
    DoubleSolenoid m_solenoidBackRaise  = new DoubleSolenoid(6, 7);

    /**
     * This function is run when the robot is first started up and should be used
     * for any initialization code.
     */
    @Override
    public void robotInit() {
        CameraServer.getInstance().startAutomaticCapture();
        m_chooser.addOption("Path Auto", c_autoPaths);
        m_chooser.setDefaultOption("Default Auto", c_autoDefault);
        SmartDashboard.putData("Auto choices", m_chooser);
        System.out.println("** // Robot Ready // **");
        m_compressor.start();
        m_compressor.setClosedLoopControl(true);
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different autonomous modes using the dashboard. The sendable chooser
     * code works with the Java SmartDashboard. If you prefer the LabVIEW Dashboard,
     * remove all of the chooser code and uncomment the getString line to get the
     * auto name from the text box below the Gyro
     *
     * <p>
     * You can add additional auto modes by adding additional comparisons to the
     * switch structure below with additional strings. If using the SendableChooser
     * make sure to add them to the chooser code above as well.
     */
    @Override
    public void autonomousInit() {
        m_autoSelected = m_chooser.getSelected();
    }

    /**
     * CUSTOM This function is a custom one that is used for raising or lowering a
     * certain solenoid based up a pair of buttons This function abstracts this away
     * so that the same code is not written 4 times for each solenoid
     */
    private void activateSolenoid(boolean extend, boolean retract, DoubleSolenoid solenoid) {
        if (extend == retract) {
            solenoid.set(DoubleSolenoid.Value.kOff);
        } else if (extend) {
            solenoid.set(DoubleSolenoid.Value.kForward);
        } else if (retract) {
            m_solenoidClawPos.set(DoubleSolenoid.Value.kReverse);
        }
    }

    /**
     * CUSTOM This function is a custom one that contain all the code for
     * controlling the robot This is to prevent rewriting the exact same code within
     * "autonomousPeriodic()" and "teleopPeriodic()"
     */
    private void defaultControl() { 
        // Main robot movement controls
        double robotForward = m_flightStick.getRawAxis(0); // X axis is rotation
        double robotRotate  = m_flightStick.getRawAxis(1); // Y axis is throttle
        m_drive.arcadeDrive(robotForward, robotRotate);

        // Dogtail movement controls
        boolean dogtailForward = m_flightStick.getRawButton(5);
        boolean dogtailBackward = m_flightStick.getRawButton(3);

        final double dogtailMoveSpeed = 1.0;
        if (dogtailForward == dogtailBackward) {
            m_dogtailLeft.set(0);
            m_dogtailRight.set(0);
        } else if (dogtailForward) {
            m_dogtailLeft.set(-dogtailMoveSpeed);
            m_dogtailRight.set(dogtailMoveSpeed);
        } else if (dogtailBackward) {
            m_dogtailLeft.set(dogtailMoveSpeed);
            m_dogtailRight.set(-dogtailMoveSpeed);
        }

        // Dogtail raising and lowering controls
        boolean dogtailLower = m_flightStick.getRawButton(7);
        boolean dogtailRaise = m_flightStick.getRawButton(9);

        final double dogtailPosSpeed = .60;
        if (dogtailLower == dogtailRaise) {
            m_dogtailPos.set(0);
        } else if (dogtailLower) {
            m_dogtailPos.set(-dogtailPosSpeed);
        } else if (dogtailRaise) {
            m_dogtailPos.set(dogtailPosSpeed);
        }

        // Claw ejector - This pops out the claw so it is now able to be used
        boolean clawUp = m_flightStick.getRawButton(2);
        boolean clawDown = m_flightStick.getRawButton(1);
        activateSolenoid(clawUp, clawDown, m_solenoidClawPos);

        // Claw extension
        boolean clawEject = m_flightStick.getRawButton(4);
        boolean clawRetract = m_flightStick.getRawButton(6);
        activateSolenoid(clawEject, clawRetract, m_solenoidClawEject);

        // Robot lifting controls
        // boolean robotUp = m_flightStick.getRawButton(12);
        // boolean frontDown = m_flightStick.getRawButton(8);
        // boolean backDown = m_flightStick.getRawButton(10);
        // activateSolenoid(robotUp, frontDown, m_solenoidFrontRaise);
        // activateSolenoid(robotUp, backDown, m_solenoidBackRaise);

    }

    /**
     * This function is called periodically during autonomous.
     */
    @Override
    public void autonomousPeriodic() {
        switch (m_autoSelected)
        {
            case c_autoDefault:
            default:
                defaultControl();
                break;
        }
   }

    /**
     * This function is called periodically during operator control.
     */
    @Override
    public void teleopPeriodic() {
        defaultControl();
    }
}
