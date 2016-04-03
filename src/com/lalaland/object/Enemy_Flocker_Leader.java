package com.lalaland.object;

import com.lalaland.utility.Logger;
import processing.core.*;
import com.lalaland.environment.*;
import com.lalaland.steering.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Enemy_Flocker_Leader extends Enemy {

	private static final float LEADER_RADIUS = 7;
	private static final PVector LEADER_COLOR = new PVector(200, 45, 200);
	private static final int NUM_FOLLOWERS = 10;
	private static final int MAX_FOLLOW_NODE_COUNT = 10;
	private static final float SIGHT_RADIUS = 200f;

	private enum States {
		SEEK_PLAYER, PATH_FIND_PLAYER, PATH_FOLLOW_PLAYER, KILL_PLAYER, LEADER_DEAD_KILL_PLAYER
	}

	private States state;
	List<Enemy_Flocker_Follower> followers;
	public int followedNodes;

	private static int spawnCount = 0;
	public static int SPAWN_OFFSET, SPAWN_INTERVAL, SPAWN_MAX;

	public Enemy_Flocker_Leader(float positionX, float positionY, PApplet parent, Environment environment) {
		super(positionX, positionY, parent, environment, LEADER_RADIUS, LEADER_COLOR.copy());
		followers = new ArrayList<>();
		POSITION_MATCHING = true;
		DRAW_BREADCRUMBS = false;
		TIME_TARGET_ROT = 10;
		RADIUS_SATISFACTION = 10;
		MAX_VELOCITY = 0.5f;
		MAX_ACCELERATION = 0.2f;
		targetPosition = new PVector(position.x, position.y);
		lifeReductionRate = 3;
		state = States.SEEK_PLAYER;
		spawnCount++;
		
		for (int i = 0; i < NUM_FOLLOWERS; i++)
			followers.add(new Enemy_Flocker_Follower(positionX, positionY, parent, environment, this));
	}

	public static int getSpawnCount() {
		return spawnCount;
	}

	public static void initializeSpawnDetails(int frameRate) {
		SPAWN_OFFSET = frameRate * 20;
		SPAWN_INTERVAL = frameRate * 20;
		SPAWN_MAX = 1;
	}

	@Override
	public void move() {
		updateLife();

		switch (state) {
		case SEEK_PLAYER:
			targetPosition.x = environment.getPlayer().getPosition().x;
			targetPosition.y = environment.getPlayer().getPosition().y;
			if(playerWithinSight())
				updateState(States.KILL_PLAYER);
			if (checkForObstacleAvoidance(velocity))
				updateState(States.PATH_FIND_PLAYER);
			break;
		case PATH_FIND_PLAYER:
			findPlayer();
			break;
		case PATH_FOLLOW_PLAYER:
			followPathForSometime();
			break;
		case KILL_PLAYER:
			targetPosition.x = environment.getPlayer().getPosition().x;
			targetPosition.y = environment.getPlayer().getPosition().y;
			if(!playerWithinSight())
				updateState(States.SEEK_PLAYER);
			if (checkForObstacleAvoidance(velocity))
				updateState(States.PATH_FIND_PLAYER);
			break;
		case LEADER_DEAD_KILL_PLAYER:
			if (allFollowersDead()) {
				alive = false;
				spawnCount--;
			}
			break;
		}

		if (state != States.LEADER_DEAD_KILL_PLAYER)
			updatePosition();

		updateFollowers();
	}

	private void updateFollowers() {
		Iterator<Enemy_Flocker_Follower> i = followers.iterator();
		while (i.hasNext()) {
			Enemy_Flocker_Follower follower = i.next();
			if (follower.isAlive())
				follower.move();
			else
				i.remove();
		}
	}

	private void findPlayer() {
		PVector pointToFleeTo = targetPosition.copy();
		pathFind(pointToFleeTo);
		updateState(States.PATH_FOLLOW_PLAYER);
	}

	private void followPathForSometime() {
		if (solutionPath != null && solutionPath.size() != 0 && followedNodes == 0) {
			int node = solutionPath.poll();
			int gridY = (int) (node / environment.getNumTiles().x);
			int gridX = (int) (node % environment.getNumTiles().x);
			targetPosition.x = gridX * environment.getTileSize().x + environment.getTileSize().x / 2;
			targetPosition.y = gridY * environment.getTileSize().y + environment.getTileSize().y / 2;
			followedNodes++;
		} else if (solutionPath != null && solutionPath.size() != 0 && reached && followedNodes <= MAX_FOLLOW_NODE_COUNT) {
			int node = solutionPath.poll();
			int gridY = (int) (node / environment.getNumTiles().x);
			int gridX = (int) (node % environment.getNumTiles().x);
			targetPosition.x = gridX * environment.getTileSize().x + environment.getTileSize().x / 2;
			targetPosition.y = gridY * environment.getTileSize().y + environment.getTileSize().y / 2;
			followedNodes++;
		} else if (solutionPath == null || solutionPath.size() == 0 || followedNodes > MAX_FOLLOW_NODE_COUNT) {
			updateState(States.SEEK_PLAYER);
			followedNodes = 0;
		}
	}

	private void updateState(States state) {
		this.state = state;
		if(state == States.KILL_PLAYER || state == States.LEADER_DEAD_KILL_PLAYER)
			followers.forEach(Enemy_Flocker_Follower::updateStateToKill);
		else
			followers.forEach(Enemy_Flocker_Follower::updateStateToFollow);
	}

	private void updateLife() {
		List<Bullet> bullets = environment.getPlayer().getBullets();
		synchronized (bullets) {
			Iterator<Bullet> i = bullets.iterator();
			while (i.hasNext()) {
				Bullet bullet = i.next();
				if (environment.inSameGrid(bullet.getPosition(), position)) {
					life -= lifeReductionRate;
					super.incrementTotalHPDamage((int)lifeReductionRate);
					i.remove();
				}
			}
		}
		if (life <= LIFE_THRESHOLD) {
			updateState(States.LEADER_DEAD_KILL_PLAYER);
		}
	}

	private void pathFind(PVector pointToFleeTo) {
		int originX = (int) (position.x / environment.getTileSize().x);
		int originY = (int) (position.y / environment.getTileSize().y);
		int originNode = originY * (int) environment.getNumTiles().x + originX;

		int destinationX = (int) (pointToFleeTo.x / environment.getTileSize().x);
		int destinationY = (int) (pointToFleeTo.y / environment.getTileSize().y);
		int destinationNode = destinationY * (int) environment.getNumTiles().x + destinationX;

		if (graphSearch.search(originNode, destinationNode, searchType)) {
			solutionPath = graphSearch.getSolutionPath();
			Logger.log("Path cost is " + Double.toString(graphSearch.getPathCost()) + ".");
			Logger.log("Solution path is " + solutionPath.toString());
		}
		graphSearch.reset();
	}

	private void updatePosition() {
		position.add(velocity);

		Kinematic target = new Kinematic(targetPosition, null, 0, 0);
		SteeringOutput steering;

		steering = Seek.getSteering(this, target, MAX_ACCELERATION, RADIUS_SATISFACTION);
		if (steering.linear.mag() == 0) {
			velocity.set(0, 0);
			acceleration.set(0, 0);
			reached = true;
			return;
		}
		reached = false;
		velocity.add(steering.linear);
		if (velocity.mag() >= MAX_VELOCITY)
			velocity.setMag(MAX_VELOCITY);

		steering.angular = LookWhereYoureGoing.getSteering(this, target, TIME_TARGET_ROT).angular;
		orientation += steering.angular;

		if (DRAW_BREADCRUMBS)
			storeHistory();
	}
	
	@Override
	public void display() {
		followers.forEach(Enemy_Flocker_Follower::display);
		if(state != States.LEADER_DEAD_KILL_PLAYER)
			super.display();
	}
	
	private boolean playerWithinSight() {
		return position.dist(environment.getPlayer().getPosition()) <= SIGHT_RADIUS;
	}
	
	private boolean allFollowersDead() {
		if (followers.size() == 0)
				return true;
		return false;
	}
}