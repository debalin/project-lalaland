package com.lalaland.object;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import com.lalaland.environment.*;

import java.util.LinkedList;

public abstract class Enemy extends GameObject {

	protected PVector acceleration;
	protected PVector targetPosition;
	protected boolean reached;
	protected boolean alive;
	protected GraphSearch graphSearch;
	protected LinkedList<Integer> solutionPath;
	protected float RADIUS_SATISFACTION;
	protected float MAX_ACCELERATION;
	protected int life;
	protected int lifeReductionRate;

	protected float TTA = 120;
	boolean USE_ACCEL = true;
	private int wa_counter = 0;
	private final int WA_LIMIT = 300;
	private float wa_angle = PConstants.PI;
	protected boolean rotationInProg = false;
	protected final int BORDER_PADDING = 12;

	protected static final GraphSearch.SearchType searchType = GraphSearch.SearchType.ASTAR;
	private static final int MAX_LIFE = 100;

	public boolean isAlive() {
		return alive;
	}

	public Enemy(float positionX, float positionY, PApplet parent, Environment environment,
			float IND_RADIUS, PVector IND_COLOR) {
		super(positionX, positionY, parent, environment, IND_RADIUS, IND_COLOR);
		acceleration = new PVector();
		reached = false;
		alive = true;
		graphSearch = environment.getNewGraphSearch();
		life = MAX_LIFE;
	}

	
	/*************methods*************/
	
	protected void rotateShapeDirection(float angle) {
		angle = scaleRotationAngle(angle);
		if (!USE_ACCEL) TTA = 1;
		angle = angle / TTA;
		orientation += angle;
		group.rotateZ(angle);		
	}

	float scaleRotationAngle(float angle) {
		angle = angle % PConstants.TWO_PI;
		if (Math.abs(angle) <= PConstants.PI) return angle;
		if (angle > PConstants.PI) {
			angle -= PConstants.TWO_PI;
		}
		else if (angle < -PConstants.PI) {
			angle += PConstants.TWO_PI;
		}
		return angle;
	}

	protected void updateVelocityPerOrientation() {
		velocity.x = MAX_VELOCITY * PApplet.cos(orientation);
		velocity.y = MAX_VELOCITY * PApplet.sin(orientation);
	}

	protected float RandomWallAvoidanceAngle() {
		wa_counter++;
		if (wa_counter == WA_LIMIT) {
			wa_angle = (parent.random(2) > 1) ? -1 * PConstants.PI : PConstants.PI;
			wa_counter = 0;
		}
		return wa_angle;

	}

	protected float RandomBinomial() {
		return parent.random(0, 1) - parent.random(0, 1);
		// return parent.random(-1,1);
	}
	
	protected boolean handleObstacleAvoidance(){
		PVector future_ray1 = PVector.add(position, PVector.mult(velocity, 1.5f));
		PVector future_ray2 = PVector.add(position, PVector.mult(velocity, 3f));
		if (
				environment.onObstacle(future_ray1) || 
				environment.onObstacle(future_ray2)
				)
		{
			avoidObstacle();			
			return true;
		}
		return false;		
	}
	
	void avoidObstacle(){
		float avoidance_orient = RandomWallAvoidanceAngle();
    rotateShapeDirection(avoidance_orient);
    if(USE_ACCEL)
      rotationInProg = true;    
    updateVelocityPerOrientation();
	}
	
	
	protected void avoidBoundary(){
		if(position.x < BORDER_PADDING  ){
    	position.x = BORDER_PADDING;
      rotateShapeDirection(RandomWallAvoidanceAngle());
      updateVelocityPerOrientation();
    }
    else if(position.x > parent.width - BORDER_PADDING){
    	position.x = parent.width - BORDER_PADDING; 
      rotateShapeDirection(RandomWallAvoidanceAngle());
      updateVelocityPerOrientation();
    }    
    else if(position.y < BORDER_PADDING){
    	position.y = BORDER_PADDING;
      rotateShapeDirection(RandomWallAvoidanceAngle());
      updateVelocityPerOrientation();
    }
    else if(position.y > parent.height - BORDER_PADDING){
    	position.y = parent.height - BORDER_PADDING; 
      rotateShapeDirection(RandomWallAvoidanceAngle());
      updateVelocityPerOrientation();
    }
	}
	
}
