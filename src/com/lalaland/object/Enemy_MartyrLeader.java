package com.lalaland.object;

import processing.core.*;
import com.lalaland.environment.*;
import com.lalaland.steering.*;

import java.util.*;

public class Enemy_MartyrLeader extends Enemy {

  private static final float LEADER_RADIUS = 12;
  private static final PVector LEADER_COLOR = new PVector(112, 71, 231);
  private static final int NUM_FOLLOWERS = 8;
  private static final boolean DYNAMIC_FORMATION = true;
  private static final int PURSUE_FORESIGHT = 5;
  private static final int[] RANK_PRIORITIES = {1, 0, 2, 7, 3, 4, 5, 6};

  private enum States {
    WAIT_FOR_FORMATION, PURSUE_PLAYER, ALERT_MARTYRS, LEADER_DEAD
  }

  private States state;
  List<Enemy_MartyrFollower> followers;

  private static int spawnCount = 0;
  private Set<Integer> deadFollowers;
  public static int SPAWN_OFFSET, SPAWN_INTERVAL, SPAWN_MAX;

  private float damageTakenByLeader, totalDamage;

  public Enemy_MartyrLeader(float positionX, float positionY, PApplet parent, Environment environment) {
    super(positionX, positionY, parent, environment, LEADER_RADIUS, LEADER_COLOR.copy());
    followers = new ArrayList<>();
    deadFollowers = new HashSet<>();
    DRAW_BREADCRUMBS = false;
    TIME_TARGET_ROT = 10;
    RADIUS_SATISFACTION = 10;
    MAX_VELOCITY = 0.2f;
    MAX_ACCELERATION = 0.2f;
    DAMAGE_RADIUS = 40f;
    PLAYER_DAMAGE = 5f;
    targetPosition = new PVector(position.x, position.y);
    lifeReductionRate = 1;
    state = States.WAIT_FOR_FORMATION;
    spawnCount++;
    damageTakenByLeader = 0;
    totalDamage = 0;

    for (int i = 0; i < NUM_FOLLOWERS; i++)
      followers.add(new Enemy_MartyrFollower(positionX, positionY, parent, environment, this, i, DYNAMIC_FORMATION));
  }

  public static int getSpawnCount() {
    return spawnCount;
  }

  public static void initializeSpawnDetails(int frameRate) {
    SPAWN_OFFSET = frameRate * 10;
    SPAWN_INTERVAL = frameRate * 2000;
    SPAWN_MAX = 1;
  }

  @Override
  public void move() {
    updateLife();

    switch (state) {
      case WAIT_FOR_FORMATION:
        if (formationSuccessful())
          updateState(States.PURSUE_PLAYER);
        break;
      case PURSUE_PLAYER:
        targetPosition.x = environment.getPlayer().getPosition().x + environment.getPlayer().getVelocity().x * PURSUE_FORESIGHT;
        targetPosition.y = environment.getPlayer().getPosition().y + environment.getPlayer().getVelocity().y * PURSUE_FORESIGHT;
        break;
      case ALERT_MARTYRS:
        adjustFollowerRanks();
        updateState(States.WAIT_FOR_FORMATION);
        break;
      case LEADER_DEAD:
        if (allFollowersDead()) {
          killYourself(false);
          //printMetrics();
          spawnCount--;
        }
        else
          followers.forEach(follower -> follower.setState(Enemy_MartyrFollower.States.GO_BERSERK));
    }

    if (state == States.PURSUE_PLAYER)
      updatePosition();

    updateFollowers();
  }

  private void printMetrics() {
    System.out.println("Damage taken By Leader: " + damageTakenByLeader);
    System.out.println("Total Damage: " + (totalDamage + damageCount));
    System.out.println("Efficiency: " + damageTakenByLeader / (survivalTime / 1000));
  }

  public int getNumFollowers() {
    return followers.size();
  }

  private void adjustFollowerRanks() {
    if (!DYNAMIC_FORMATION) {
      for (int x = 0; x <= RANK_PRIORITIES.length - 1; x++) {
        if (deadFollowers.contains(RANK_PRIORITIES[x])) {
          for (int y = RANK_PRIORITIES.length - 1; y > x; y--) {
            if (!deadFollowers.contains(RANK_PRIORITIES[y])) {
              int index = followers.indexOf(new Enemy_MartyrFollower(0, 0, null, null, null, RANK_PRIORITIES[y], false));
              Enemy_MartyrFollower follower = followers.get(index);
              follower.setRank(RANK_PRIORITIES[x]);
              deadFollowers.remove(RANK_PRIORITIES[x]);
              deadFollowers.add(RANK_PRIORITIES[y]);
              break;
            }
          }
        }
      }
    }
    else
      for (int i = 0; i <= followers.size() - 1; i++) followers.get(i).setRank(i);

    followers.forEach(follower -> follower.setState(Enemy_MartyrFollower.States.UPDATE_FORMATION));
  }

  private boolean formationSuccessful() {
    Iterator<Enemy_MartyrFollower> i = followers.iterator();
    int currentNumFollowers = followers.size();
    while (i.hasNext()) {
      Enemy_MartyrFollower follower = i.next();
      if (follower.getState() == Enemy_MartyrFollower.States.FORMATION_READY) {
        currentNumFollowers--;
      }
    }
    if (currentNumFollowers <= 0) {
      followers.forEach(follower -> follower.setState(Enemy_MartyrFollower.States.FOLLOW_FORMATION));
      return true;
    }
    return false;
  }

  private void updateFollowers() {
    int previousDeadFollowerCount = deadFollowers.size();

    Iterator<Enemy_MartyrFollower> i = followers.iterator();
    while (i.hasNext()) {
      Enemy_MartyrFollower follower = i.next();
      if (follower.isAlive())
        follower.move();
      else {
        deadFollowers.add(follower.getRank());
        totalDamage += follower.damageCount;
        i.remove();
      }
    }

    if (deadFollowers.size() > previousDeadFollowerCount) {
      updateState(States.ALERT_MARTYRS);
    }
  }

  private void updateState(States state) {
    this.state = state;
  }

  private void updateLife() {
    List<Bullet> bullets = environment.getPlayer().getBullets();
    synchronized (bullets) {
      Iterator<Bullet> i = bullets.iterator();
      while (i.hasNext()) {
        Bullet bullet = i.next();
        if (environment.inSameGrid(bullet.getPosition(), position)) {
          life -= lifeReductionRate;
          if (!allFollowersDead())
            damageTakenByLeader += lifeReductionRate;
          super.incrementTotalHPDamage((int)lifeReductionRate);
          i.remove();
        }
      }
    }
    if (life <= LIFE_THRESHOLD) {
      updateState(States.LEADER_DEAD);
    }
    checkAndReducePlayerLife();
  }

  private List<PVector> buildAvoidanceRays() {
    List<PVector> futureRays = new ArrayList<>();

    futureRays.add(PVector.add(position, PVector.fromAngle(orientation).setMag(70f)));
    futureRays.add(PVector.add(position, PVector.fromAngle(orientation - PConstants.PI / 4f).setMag(70f)));
    futureRays.add(PVector.add(position, PVector.fromAngle(orientation + PConstants.PI / 4f).setMag(70f)));
    futureRays.add(PVector.add(position, PVector.fromAngle(orientation).setMag(35f)));
    futureRays.add(PVector.add(position, PVector.fromAngle(orientation - PConstants.PI / 4f).setMag(35f)));
    futureRays.add(PVector.add(position, PVector.fromAngle(orientation + PConstants.PI / 4f).setMag(35f)));

    futureRays.forEach(futureRay -> parent.ellipse(futureRay.x, futureRay.y, 5, 5));

    return futureRays;
  }

  private void updatePosition() {
    position.add(velocity);

    boolean onObstacle = ObstacleSteering.checkForObstacleAvoidance(this, parent, environment, 5);
    if (onObstacle) {
      targetPosition.set(ObstacleSteering.avoidObstacleOnSeek(this, environment, 5));
    }

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
    followers.forEach(follower -> follower.display());
    if(state != States.LEADER_DEAD)
      super.display();
  }

  private boolean allFollowersDead() {
    if (followers.size() == 0)
      return true;
    return false;
  }

}
