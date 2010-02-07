package lejos.robotics.proposal;






import java.util.ArrayList;
import lejos.robotics.proposal.*;
import lejos.nxt.*;
import lejos.robotics.*;

/*
 * WARNING: THIS CLASS IS SHARED BETWEEN THE classes AND pccomms PROJECTS.
 * DO NOT EDIT THE VERSION IN pccomms AS IT WILL BE OVERWRITTEN WHEN THE PROJECT IS BUILT.
 */

/**
 * The DifferentialPilot class is a software abstraction of the Pilot mechanism of a
 * NXT robot. It contains methods to control robot movements: travel forward or
 * backward in a straight line or a circular path or rotate to a new direction.<br>
 * This  class will only work with two independently controlled motors to
 * steer differentially, so it can rotate within its own footprint (i.e. turn on
 * one spot). It registers as a TachoMotorListener with each of its motors.
 * An object of this class assumes that it has exclusive control of
 * its motors.  If any other object makes calls to its motors, the resultes are
 * unpredictable. <br>
 * This class  can be used with robots that have reversed motor design: the robot moves
 * in the direction opposite to the the direction of motor rotation. .<br>
 * It automatically updates a DeadReckonerMoveProvider which has called the
 * addMoveListener() method on object class.
 * Some methods optionally return immediately so the thread that called the
 * method can monitor sensors, get current pose, and call stop() if necessary.<br>
 * Handling stalls: If a stall is detected,   <code>isStalled()</code> returns <code>
 * true </code>,  <code>isMoving()</code>  returns <code>false</code>, <code>moveStopped()
 * </code> is called, and, if a blocking method is executing, that method exits.
 *  Example:
 * <p>
 * <code><pre>
 * DifferentialPilot pilot = new DifferentialPilot(2.1f, 4.4f, Motor.A, Motor.C, true);  // parameters in inches
 * pilot.setRobotSpeed(10);                                           // inches per second
 * pilot.travel(12);                                                  // inches
 * pilot.rotate(-90);                                                 // degree clockwise
 * pilot.travel(-12,true);
 * while(pilot.isMoving())Thread.yield();
 * pilot.rotate(-90);
 * pilot.rotateTo(270);
 * pilot.steer(-50,180,true);
 * while(pilot.isMoving())Thread.yield();
 * pilot.steer(100);
 * try{Thread.sleep(1000);}
 * catch(InterruptedException e){}
 * pilot.stop();
 * </pre></code>
 * </p>
 *
 **/
public class DifferentialPilot implements
        TachoMotorListener, MoveProvider, ArcRotateMoveController
{

  /**
   * Allocates a DifferentialPilot object, and sets the physical parameters of the
   * NXT robot.<br>
   * Assumes Motor.forward() causes the robot to move forward.
   *
   * @param wheelDiameter
   *            Diameter of the tire, in any convenient units (diameter in mm
   *            is usually printed on the tire).
   * @param trackWidth
   *            Distance between center of right tire and center of left tire,
   *            in same units as wheelDiameter.
   * @param leftMotor
   *            The left Motor (e.g., Motor.C).
   * @param rightMotor
   *            The right Motor (e.g., Motor.A).
   */
  public DifferentialPilot(final float wheelDiameter, final float trackWidth,
          final TachoMotor leftMotor, final TachoMotor rightMotor)
  {
    this(wheelDiameter, trackWidth, leftMotor, rightMotor, false);
  }

  /**
   * Allocates a DifferentialPilot object, and sets the physical parameters of the
   * NXT robot.<br>
   *
   * @param wheelDiameter
   *            Diameter of the tire, in any convenient units (diameter in mm
   *            is usually printed on the tire).
   * @param trackWidth
   *            Distance between center of right tire and center of left tire,
   *            in same units as wheelDiameter.
   * @param leftMotor
   *            The left Motor (e.g., Motor.C).
   * @param rightMotor
   *            The right Motor (e.g., Motor.A).
   * @param reverse
   *            If true, the NXT robot moves forward when the motors are
   *            running backward.
   */
  public DifferentialPilot(final float wheelDiameter, final float trackWidth,
          final TachoMotor leftMotor, final TachoMotor rightMotor,
          final boolean reverse)
  {
    this(wheelDiameter, wheelDiameter, trackWidth, leftMotor, rightMotor,
            reverse);
  }

  /**
   * Allocates a DifferentialPilot object, and sets the physical parameters of the
   * NXT robot.<br>
   *
   * @param leftWheelDiameter
   *            Diameter of the left wheel, in any convenient units (diameter
   *            in mm is usually printed on the tire).
   * @param rightWheelDiameter
   *            Diameter of the right wheel. You can actually fit
   *            intentionally wheels with different size to your robot. If you
   *            fitted wheels with the same size, but your robot is not going
   *            straight, try swapping the wheels and see if it deviates into
   *            the other direction. That would indicate a small difference in
   *            wheel size. Adjust wheel size accordingly. The minimum change
   *            in wheel size which will actually have an effect is given by
   *            minChange = A*wheelDiameter*wheelDiameter/(1-(A*wheelDiameter)
   *            where A = PI/(moveSpeed*360). Thus for a moveSpeed of 25
   *            cm/second and a wheelDiameter of 5,5 cm the minChange is about
   *            0,01058 cm. The reason for this is, that different while sizes
   *            will result in different motor speed. And that is given as an
   *            integer in degree per second.
   * @param trackWidth
   *            Distance between center of right tire and center of left tire,
   *            in same units as wheelDiameter.
   * @param leftMotor
   *            The left Motor (e.g., Motor.C).
   * @param rightMotor
   *            The right Motor (e.g., Motor.A).
   * @param reverse
   *            If true, the NXT robot moves forward when the motors are
   *            running backward.
   */
  public DifferentialPilot(final float leftWheelDiameter,
          final float rightWheelDiameter, final float trackWidth,
          final TachoMotor leftMotor, final TachoMotor rightMotor,
          final boolean reverse)
  {
    _left = leftMotor;
   _left.addListener(this);
    _leftWheelDiameter = leftWheelDiameter;
    _leftTurnRatio = trackWidth / leftWheelDiameter;
    _leftDegPerDistance = 360 / ((float) Math.PI * leftWheelDiameter);
    // right
    _right = rightMotor;
    _right.addListener(this);
    _rightWheelDiameter = rightWheelDiameter;
    _rightTurnRatio = trackWidth / rightWheelDiameter;
    _rightDegPerDistance = 360 / ((float) Math.PI * rightWheelDiameter);
    // both
    _trackWidth = trackWidth;
    _parity = (byte) (reverse ? -1 : 1);
    setTravelSpeed(.8f * getMaxTravelSpeed());
    setRotateSpeed(.8f * getMaxRotateSpeed());
  }

  /**
   * Returns the left motor.
   * @return left motor.
   */
  public TachoMotor getLeft()
  {
    return _left;
  }

  /**
   * returns the right motor.
   * @return right motor.
   */
  public TachoMotor getRight()
  {
    return _right;
  }

  /**
   * Returnsthe tachoCount of the left motor
   * @return tachoCount of left motor. Positive value means motor has moved
   *         the robot forward.
   */
  public int getLeftCount()
  {
    return _parity * _left.getTachoCount();
  }

  /**
   * Returns the tachoCount of the right motor
   * @return tachoCount of the right motor. Positive value means motor has
   *         moved the robot forward.
   */
  public int getRightCount()
  {
    return _parity * _right.getTachoCount();
  }

  /**
   * Returns the actual speed of the left motor
   * @return actual speed of left motor in degrees per second. A negative
   *         value if motor is rotating backwards. Updated every 100 ms.
   **/
  public int getLeftActualSpeed()
  {
    return _left.getRotationSpeed();
  }

  /**
   * Returns the actual speed of right motor
   * @return actual speed of right motor in degrees per second. A negative
   *         value if motor is rotating backwards. Updated every 100 ms.
   **/
  public int getRightActualSpeed()
  {
    return _right.getRotationSpeed();
  }

  /**
   * Returns the ratio of motor revolutions per 360 degree rotation of the robot
   * @return ratio of motor revolutions per 360 degree rotation of the robot.
   *         If your robot has wheels with different size, it is the average.
   */
  public float getTurnRatio()
  {
    return (_leftTurnRatio + _rightTurnRatio) / 2.0f;
  }

  private void setSpeed(final int leftSpeed, final int rightSpeed)
  {
    _left.setSpeed(leftSpeed);
    _right.setSpeed(rightSpeed);
  }

  /**
   * also sets _motorSpeed
   *
   * @see lejos.robotics.navigation.Pilot#setTravelSpeed(float)
   */
  public void setTravelSpeed(final float travelSpeed)
  {
    _robotTravelSpeed = travelSpeed;
    _motorSpeed = Math.round(0.5f * travelSpeed * (_leftDegPerDistance + _rightDegPerDistance));
    setSpeed(Math.round(travelSpeed * _leftDegPerDistance), Math.round(travelSpeed * _rightDegPerDistance));
  }

  /**
   * @see lejos.robotics.navigation.Pilot#getTravelSpeed()
   */
  public float getTravelSpeed()
  {
    return _robotTravelSpeed;
  }

  public float getMoveMaxSpeed()
  {
    return getMaxTravelSpeed();
  }

  public float getMoveSpeed()
  {
    return getTravelSpeed();
  }

  public void setMoveSpeed(float s)
  {
    setTravelSpeed(s);
  }

  /**
   * @see lejos.robotics.navigation.Pilot#getTravelMaxSpeed()
   */
  public float getMaxTravelSpeed()
  {
    // it is generally assumed, that the maximum accurate speed of Motor is
    // 100 degree/second * Voltage
    return Battery.getVoltage() * 100.0f / Math.max(_leftDegPerDistance, _rightDegPerDistance);
    // max degree/second divided by degree/unit = unit/second
  }

  /**
   * sets the rotation speed of the vehicle, degrees per second
   * @param rotateSpeed
   */
  public void setRotateSpeed(float rotateSpeed)
  {
    _robotRotateSpeed = rotateSpeed;
    setSpeed(Math.round(rotateSpeed * _leftTurnRatio), Math.round(rotateSpeed * _rightTurnRatio));
  }

  /**
   * @see lejos.robotics.navigation.Pilot#getRotateSpeed()
   */
  public float getRotateSpeed()
  {
    return _robotRotateSpeed;
  }

  /**
   * @see lejos.robotics.navigation.Pilot#getRotateMaxSpeed()
   */
  public float getMaxRotateSpeed()
  {
    // it is generally assumed, that the maximum accurate speed of that can
//    be reliably maintained Motor is
    // 100 degree/second * Voltage
    return Battery.getVoltage() * 100.0f / Math.max(_leftTurnRatio, _rightTurnRatio);
    // max degree/second divided by degree/unit = unit/second
  }

  public void setTurnSpeed(float s)
  {
    setRotateSpeed(s);
  }

  public float getTurnSpeed()
  {
    return getRotateSpeed();
  }

  public float getTurnMaxSpeed()
  {
    return getMaxRotateSpeed();
  }

  /**
   * Moves the NXT robot forward until stop() is called.
   */
  public void forward()
  {
    _type = Move.MoveType.TRAVEL;
    movementStart(false);
    setSpeed(Math.round(_robotTravelSpeed * _leftDegPerDistance), Math.round(_robotTravelSpeed * _rightDegPerDistance));
    if (_parity == 1)
    {
      fwd();
    } else
    {
      bak();
    }
  }

  /**
   * Moves the NXT robot backward until stop() is called.
   */
  public void backward()
  {
    _type = Move.MoveType.TRAVEL;
    movementStart(false);
    setSpeed(Math.round(_robotTravelSpeed * _leftDegPerDistance), Math.round(_robotTravelSpeed * _rightDegPerDistance));

    if (_parity == 1)
    {
      bak();
    } else
    {
      fwd();
    }
  }

  /**
   * Motors backward. This is called by forward() and backward(), demending in parity.
   */
  private void bak()
  {
    _left.backward();
    _right.backward();
  }

  /**
   * Motors forward. This is called by forward() and backward() depending in parity.
   */
  private void fwd()
  {
    _left.forward();
    _right.forward();
  }

  public boolean rotateLeft()
  {
    _type = Move.MoveType.ROTATE;
    _right.forward();
    _left.backward();
    return true;
  }

  public boolean rotateRight()
  {
    _type = Move.MoveType.ROTATE;
    _left.forward();
    _right.backward();
    return true;
  }

  /**
   * Rotates the NXT robot through a specific angle. Returns when angle is
   * reached. Wheels turn in opposite directions producing a zero radius turn.<br>
   * Note: Requires correct values for wheel diameter and track width.
   * calls rotate(angle,false)
   * @param angle
   *            The wanted angle of rotation in degrees. Positive angle rotate
   *            left (clockwise), negative right.
   */
  public boolean rotate(final float angle)
  {
    return rotate(angle, false);
  }

  /**
   * Rotates the NXT robot through a specific angle. Returns when angle is
   * reached. Wheels turn in opposite directions producing a zero radius turn.<br>
   * Note: Requires correct values for wheel diameter and track width.
   * Side effect: inform listeners
   * @param angle
   *            The wanted angle of rotation in degrees. Positive angle rotate
   *            left (clockwise), negative right.
   * @param immediateReturn
   *            If true this method returns immediately.
   */
  public boolean rotate(final float angle, final boolean immediateReturn)
  {
    _type = Move.MoveType.ROTATE;
    movementStart(immediateReturn);
    setSpeed(Math.round(_robotRotateSpeed * _leftTurnRatio), Math.round(_robotRotateSpeed * _rightTurnRatio));
    int rotateAngleLeft = _parity * (int) (angle * _leftTurnRatio);
    int rotateAngleRight = _parity * (int) (angle * _rightTurnRatio);
    _left.rotate(-rotateAngleLeft, true);
    _right.rotate(rotateAngleRight, immediateReturn);

    if (!immediateReturn)  while (isMoving()) Thread.yield();
    return true;
  }

  /**
   * This method can be overridden by subclasses to stop the robot if a hazard
   * is detected
   *
   * @return true iff no hazard is detected
   */
  protected boolean continueMoving()
  {
    return true;
  }

  /**
   * Stops the NXT robot.
   *  side effect: inform listeners of end of movement
   */
  public boolean stop()
  {
    _left.stop();
    _right.stop();
    while (isMoving())
    {
      Thread.yield();
    }
//    movementStop();
    return true;
  }

  /**
   * Moves the NXT robot a specific distance in an (hopefully) straight line.<br>
   * A positive distance causes forward motion, a negative distance moves
   * backward. If a drift correction has been specified in the constructor it
   * will be applied to the left motor.
   * calls travel(distance, false)
   * @param distance
   *            The distance to move. Unit of measure for distance must be
   *            same as wheelDiameter and trackWidth.
   **/
  public boolean travel(final float distance)
  {
    return travel(distance, false);
  }

  /**
   * Moves the NXT robot a specific distance in an (hopefully) straight line.<br>
   * A positive distance causes forward motion, a negative distance moves
   * backward. If a drift correction has been specified in the constructor it
   * will be applied to the left motor.
   *
   * @param distance
   *            The distance to move. Unit of measure for distance must be
   *            same as wheelDiameter and trackWidth.
   * @param immediateReturn
   *            If true this method returns immediately.
   */
  public boolean travel(final float distance, final boolean immediateReturn)
  {
    _type = Move.MoveType.TRAVEL;
    if (distance == Float.POSITIVE_INFINITY)
    {
      forward();
      return true;
    }
    if ((distance == Float.NEGATIVE_INFINITY))
    {
      backward();
      return true;
    }
    movementStart(immediateReturn);
    setSpeed(Math.round(_robotTravelSpeed * _leftDegPerDistance), Math.round(_robotTravelSpeed * _rightDegPerDistance));
    _left.rotate((int) (_parity * distance * _leftDegPerDistance), true);
    _right.rotate((int) (_parity * distance * _rightDegPerDistance),
            immediateReturn);
    if (!immediateReturn) while (isMoving()) Thread.yield();
    return true;
  }

  public boolean arcForward(final float radius)
  {
    _type = Move.MoveType.ARC;
    movementStart(true);
    float turnRate = turnRate(radius);
    steerPrep(turnRate); // sets motor speeds
    _outside.forward();
    if (_parity * _steerRatio > 0)
    {
      _inside.forward();
    } else
    {
      _inside.backward();
    }
    return true;
  }

  public boolean arcBackward(final float radius)
  {
     _type = Move.MoveType.ARC;
    movementStart(true);
    float turnRate = turnRate(radius);
    steerPrep(turnRate);// sets motor speeds
    _outside.backward();
    if (_parity * _steerRatio > 0)
    {
      _inside.backward();
    } else
    {
      _inside.forward();
    }
    return true;
  }

  public boolean arc(final float radius, final float angle)
  {
    return arc(radius, angle, false);
  }

  public boolean arc(final float radius, final float angle,
          final boolean immediateReturn)
  {
    if (radius == Float.POSITIVE_INFINITY || radius == Float.NEGATIVE_INFINITY)
    {
      forward();
      return true;
    }
    steer(turnRate(radius), angle, immediateReturn);// type and move started called by steer()
    if (!immediateReturn) while(isMoving())Thread.yield();
    return true;
  }

  public boolean travelArc(float radius, float distance)
  {
    return travelArc(radius, distance, false);
  }

  public boolean travelArc(float radius, float distance, boolean immediateReturn)
  {
    if (radius == Float.POSITIVE_INFINITY || radius == Float.NEGATIVE_INFINITY)
    {
      travel(distance, immediateReturn);
      return true;
    }
//    _type = Move.MoveType.ARC;
//    movementStart(immediateReturn);
    if (radius == 0)
    {
      throw new IllegalArgumentException("Zero arc radius");
    }
    float angle = (distance * 180) / ((float) Math.PI * radius);
    return arc(radius, angle, immediateReturn);
  }

  /**
   * Calculates the turn rate corresponding to the turn radius; <br>
   * use as the parameter for steer() negative argument means center of turn
   * is on right, so angle of turn is negative
   * @param radius
   * @return turnRate to be used in steer()
   */
  private float turnRate(final float radius)
  {
    int direction;
    float radiusToUse;
    if (radius < 0)
    {
      direction = -1;
      radiusToUse = -radius;
    } else
    {
      direction = 1;
      radiusToUse = radius;
    }
    float ratio = (2 * radiusToUse - _trackWidth) / (2 * radiusToUse + _trackWidth);
    return (direction * 100 * (1 - ratio));
  }

/**
 * This method is for frequent adjustments of robot direction, for example
 * for line following and in CompassPilot to correctheading traveling.
 * It should NEVER be called when this classes is used as a Move Provider
 * for navigation purposes.
 * @param turnRate
 */
  public void steer(float turnRate)
  {
    if (turnRate == 0)
    {
      forward();
      return;
    }
    steerPrep(turnRate);
    _outside.forward();
    if (_parity * _steerRatio > 0) _inside.forward();
    else _inside.backward();
  }

  public void steer(final float turnRate, float angle)
  {
    steer(turnRate, angle, false);
  }

  public void steer(final float turnRate, final float angle,
          final boolean immediateReturn)
  {
    if (angle == 0)
    {
      return;
    }
    if (turnRate == 0)
    {
      forward();
      return;
    }
   _type = Move.MoveType.ARC;
   movementStart(immediateReturn);
    steerPrep(turnRate);
    int side = (int) Math.signum(turnRate);
    int rotAngle = (int) (angle * _trackWidth * 2 / (_leftWheelDiameter * (1 - _steerRatio)));
    _inside.rotate((int) (_parity * side * rotAngle * _steerRatio), true);
    _outside.rotate(_parity * side * rotAngle, immediateReturn);
    if (immediateReturn)
    {
      return;
    }
     while (isMoving()) Thread.yield();
    _inside.setSpeed(_outside.getSpeed());
  }

  /**
   * helper method used by steer(float) and steer(float,float,boolean)
   * sets _outsideSpeed, _insideSpeed, _steerRatio
   * @param turnRate
   */
  protected void steerPrep(final float turnRate)
  {

//    if (turnRate == 0)
//    {
//      forward();
//      return;
//    }
    float rate = turnRate;
    if (rate < -200) rate = -200;
    if (rate > 200) rate = 200;

    if (turnRate < 0)
    {
      _inside = _right;
      _outside = _left;
      rate = -rate;
    } else
    {
      _inside = _left;
      _outside = _right;
    }
    _outside.setSpeed(_motorSpeed);
    _steerRatio = 1 - rate / 100.0f;
    _inside.setSpeed((int) (_motorSpeed * _steerRatio));
  }

//  protected void checkStall()
//  {
//    while (isMoving()) Thread.yield();
//  }

  /**
   * called by Arc() ,travel(),rotate(),stop() rotationStopped()
   * calls moveStopped on listener
   */
  protected synchronized void movementStop()
  {
    for(MoveListener ml : _listeners)
      ml.moveStopped(new Move(_type,
            getMovementIncrement(), getAngleIncrement(), isMoving()), this);
  }

  /**
   * called by TachoMotor when a motor rotation is complete
   * calls movementStop() after both motors stop;
   * @param motor
   * @param count
   * @param ts
   */
  public synchronized void rotationStopped(TachoMotor m, int tachoCount, boolean stall,long ts)
  {
   if(m.isStalled())stop();
   else if (!isMoving())movementStop();// a motor has stopped
  }

  /**
   * called by TachoMotor when a motor rotation starts
   * not used.
   * @param motor
   * @param count
   * @param ts
   */
  public synchronized void rotationStarted(TachoMotor m, int tachoCount, boolean stall,long ts)
  {
  }

  /**
   * called at start of a movement to inform the listening pose  that a movement has started
   */
  protected void movementStart(boolean alert)
  {
    if (isMoving())  movementStop();
    reset();
    for(MoveListener ml : _listeners)
      ml.moveStarted(new Move(_type,
            getMovementIncrement(), getAngleIncrement(), isMoving()), this);
  }

  /**
   * @return true if the NXT robot is moving.
   **/
  public boolean isMoving()
  {
    return _left.isMoving() || _right.isMoving();
  }

  public boolean isStalled()
  {
    return _left.isStalled() || _right.isStalled();
  }
  /**
   * Resets tacho count for both motors.
   **/
  public void reset()
  { 
    _left.resetTachoCount();
    _right.resetTachoCount();
  }

  public void setMinRadius(float radius)
  {
    _turnRadius = radius;
  }

  public float getMinRadius()
  {
    return _turnRadius;
  }

  public float getMovementIncrement()
  {
    float left = _left.getTachoCount() / _leftDegPerDistance;
    float right = _right.getTachoCount() / _rightDegPerDistance;
    return _parity * (left + right) / 2.0f;
  }

  public float getAngleIncrement()
  {
    return _parity * ((_right.getTachoCount() / _rightTurnRatio) -
            (_left.getTachoCount() / _leftTurnRatio)) / 2.0f;
  }

  public void addMoveListener(MoveListener m)
  {
    _listeners.add(m);
  }

  public Move getMovement()
  {
    return  new Move(_type, getMovementIncrement(), getAngleIncrement(), isMoving());
  }

  private float _turnRadius = 0;
  /**
   * Left motor.
   */
  protected final TachoMotor _left;
  /**
   * Right motor.
   */
  protected final TachoMotor _right;
  /**
   * The motor at the inside of the turn. set by steer(turnRate)
   * used by other steer methodsl
   */
  protected TachoMotor _inside;
  /**
   * The motor at the outside of the turn. set by steer(turnRate)
   * used by other steer methodsl
   */
  protected TachoMotor _outside;
  /**
   * ratio of inside/outside motor speeds
   * set by steer(turnRate)
   * used by other steer methods;
   */
  protected float _steerRatio;
  /**
   * Left motor degrees per unit of travel.
   */
  protected final float _leftDegPerDistance;
  /**
   * Right motor degrees per unit of travel.
   */
  protected final float _rightDegPerDistance;
  /**
   * Left motor revolutions for 360 degree rotation of robot (motors running
   * in opposite directions). Calculated from wheel diameter and track width.
   * Used by rotate() and steer() methods.
   **/
  protected final float _leftTurnRatio;
  /**
   * Right motor revolutions for 360 degree rotation of robot (motors running
   * in opposite directions). Calculated from wheel diameter and track width.
   * Used by rotate() and steer() methods.
   **/
  protected final float _rightTurnRatio;
  /**
   * Speed of robot for moving in wheel diameter units per seconds. Set by
   * setSpeed(), setTravelSpeed()
   */
  protected float _robotTravelSpeed;
  /**
   * Speed of robot for turning in degree per seconds.
   */
  protected float _robotRotateSpeed;
  /**
   * Motor speed degrees per second. Used by forward(),backward() and steer().
   */
  protected int _motorSpeed;
  /**
   * Motor rotation forward makes robot move forward if parity == 1.
   */
  private byte _parity;
  /**
   * Distance between wheels. Used in steer() and rotate().
   */
  protected final float _trackWidth;
  /**
   * Diameter of left wheel.
   */
  protected final float _leftWheelDiameter;
  /**
   * Diameter of right wheel.
   */
  protected final float _rightWheelDiameter;

  protected ArrayList<MoveListener> _listeners= new ArrayList<MoveListener>();
  protected MoveListener _listener;
  protected Move.MoveType _type;
}
