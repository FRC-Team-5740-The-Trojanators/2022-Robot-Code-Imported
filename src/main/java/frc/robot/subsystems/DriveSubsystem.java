// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.PigeonIMU;
import com.ctre.phoenix.sensors.PigeonIMU_StatusFrame;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.ADIS16448_IMU;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.CANBusIDs;
import frc.robot.Constants.RioInputs;
import frc.robot.Constants.SteerModulePIDValues;
import frc.robot.Constants.SwerveDriveModuleConstants;
import lib.LazyTalonFX;

public class DriveSubsystem extends SubsystemBase
{
  public PigeonIMU m_imu = new PigeonIMU(CANBusIDs.k_pigeonID);
 // public ADIS16448_IMU m_imu = new ADIS16448_IMU(RioInputs.k_gyroID);
  //private Pose2d m_robotPose = new Pose2d();

  private UsbCamera m_UsbCamera = CameraServer.startAutomaticCapture();
  NetworkTableEntry cameraEntry = NetworkTableInstance.getDefault().getTable("").getEntry("Camera");
  //NetworkTableEntry limelight = NetworkTableInstance.getDefault().getTable("").getEntry("limelight"); 
  boolean cam = NetworkTableInstance.getDefault().getTable("limelight").getEntry("camMode").setNumber(1);

  private SwerveModuleState[] m_states = new SwerveModuleState[]
  {
      new SwerveModuleState(0.0, new Rotation2d(0)),
      new SwerveModuleState(0.0, new Rotation2d(0)),
      new SwerveModuleState(0.0, new Rotation2d(0)),
      new SwerveModuleState(0.0, new Rotation2d(0))
  };

  private SwerveModulePosition[] swerveModulePosition = new SwerveModulePosition[]
  {
    new SwerveModulePosition(),
    new SwerveModulePosition(),
    new SwerveModulePosition(),
    new SwerveModulePosition()
  };

  SwerveDriveOdometry m_odometry =
        new SwerveDriveOdometry(SwerveDriveModuleConstants.kinematics, Rotation2d.fromDegrees(m_imu.getYaw()), swerveModulePosition); 

  public SwerveModule[] modules = new SwerveModule[]
  {
      new SwerveModule(new LazyTalonFX(CANBusIDs.k_LeftFront_DriveMotor), new LazyTalonFX(CANBusIDs.k_LeftFront_SteeringMotor), new CANCoder(CANBusIDs.leftFrontCANCoderId), Rotation2d.fromDegrees(SwerveDriveModuleConstants.leftFrontOffset)), // Left Front
      new SwerveModule(new LazyTalonFX(CANBusIDs.k_RightFront_DriveMotor), new LazyTalonFX(CANBusIDs.k_RightFront_SteeringMotor), new CANCoder(CANBusIDs.rightFrontCANCoderId), Rotation2d.fromDegrees(SwerveDriveModuleConstants.rightFrontOffset)), // Right Front
      new SwerveModule(new LazyTalonFX(CANBusIDs.k_LeftRear_DriveMotor), new LazyTalonFX(CANBusIDs.k_LeftRear_SteeringMotor), new CANCoder(CANBusIDs.leftRearCANCoderId), Rotation2d.fromDegrees(SwerveDriveModuleConstants.leftRearOffset)), // Left Rear
      new SwerveModule(new LazyTalonFX(CANBusIDs.k_RightRear_DriveMotor), new LazyTalonFX(CANBusIDs.k_RightRear_SteeringMotor), new CANCoder(CANBusIDs.rightRearCANCoderId), Rotation2d.fromDegrees(SwerveDriveModuleConstants.rightRearOffset)), // Right Rear
  };

 public DriveSubsystem(boolean calibrateGyro) 
  {
    if(calibrateGyro) 
    {
      m_imu.setYaw(0); //recalibrates gyro offset
    }
        
    for(int i = 0; i < 4; i++)
    {
       modules[i].resetDriveEncoder();
    }

    m_UsbCamera.setResolution(320, 240);
    m_UsbCamera.setFPS(10);

    m_imu.setStatusFramePeriod(PigeonIMU_StatusFrame.CondStatus_1_General, 20);
  }

  public void autoDrive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) 
  {       
    m_states =
      SwerveDriveModuleConstants.kinematics.toSwerveModuleStates(
          fieldRelative
              ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, Rotation2d.fromDegrees(-m_imu.getYaw()))
              : new ChassisSpeeds(xSpeed, ySpeed, rot));
      SwerveDriveKinematics.desaturateWheelSpeeds(m_states, SwerveDriveModuleConstants.k_MaxAutoSpeed);
      for (int i = 0; i < m_states.length; i++) 
      {
          SwerveModule module = modules[i];
          module.setDesiredState(m_states[i]);
          SmartDashboard.putNumber("Drive Actual", module.getState().speedMetersPerSecond);
          SmartDashboard.putNumber("Drive Expected", module.getDriveVelocity());
      } 
  }

  public void setSwerveModuleStates(SwerveModuleState[] desiredStates) 
  {
    SwerveDriveKinematics.desaturateWheelSpeeds(
        desiredStates, SwerveDriveModuleConstants.k_MaxTeleSpeed);
        modules[0].setDesiredState(desiredStates[0]);
        modules[1].setDesiredState(desiredStates[1]);
        modules[2].setDesiredState(desiredStates[2]);
        modules[3].setDesiredState(desiredStates[3]);
  }
  public void teleDrive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) 
  {       
    m_states =
      SwerveDriveModuleConstants.kinematics.toSwerveModuleStates(
          fieldRelative
              ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, Rotation2d.fromDegrees(-m_imu.getYaw()))
              : new ChassisSpeeds(xSpeed, ySpeed, rot));
      //SwerveDriveKinematics.desaturateWheelSpeeds(m_states, SwerveDriveModuleConstants.k_MaxTeleSpeed);
      setSwerveModuleStates(m_states);

      for (int i = 0; i < 4; i++) 
      {
          SwerveModule module = modules[i];
          module.setDesiredState(m_states[i]);
          SmartDashboard.putNumber("Drive Expected", module.getState().speedMetersPerSecond);
          SmartDashboard.putNumber("Drive Actual", module.getDriveVelocity());


      } 
  }
    
  public void resetEncoders()
  {
    modules[0].resetDriveEncoder();
    modules[1].resetDriveEncoder();
    modules[2].resetDriveEncoder();
    modules[3].resetDriveEncoder();
  }

  public void resetIMU()
  {
    m_imu.setYaw(0);
  }

  @Override
  public void periodic() 
  {

  }
    // This method will be called once per scheduler run
    //var gyroAngle = Rotation2d.fromDegrees(m_imu.getYaw());
   // m_robotPose = m_odometry.update(gyroAngle, modules[0].getState(), modules[1].getState(), modules[2].getState(), modules[3].getState());
    //SmartDashboard.putNumber("Pigeon IMU", m_imu.getYaw());
    //checkTemperature();


    // SmartDashboard.putNumber("Drive Actual",modules[0].getDriveVelocity());

  public SwerveDriveOdometry getOdometry()
  {
      return m_odometry;
  }

  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose)
  {
      m_odometry.resetPosition(Rotation2d.fromDegrees(-m_imu.getYaw()), swerveModulePosition, pose);
  }

  public Pose2d getPoseMeters() 
  {
    return m_odometry.getPoseMeters();
  }
  // public boolean checkTemperature()
  // {
  //   boolean temperature = false;
  //   for (int i = 0; i < 4; i++) 
  //   {
  //     if (modules[i].getTemperature()) 
  //     {
  //       temperature = true;
  //       teleDrive(0, 0, 0, false);
  //       SmartDashboard.putBoolean("Temperature Good", false);
  //     } else 
  //      {
  //       SmartDashboard.putBoolean("Temperature Good", true);

  //      }
  //   }
  //   return temperature;
  // }
  
}
