package com.lalaland.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.lalaland.environment.Environment;
import com.lalaland.object.BonusItem;
import com.lalaland.object.Enemy;
import com.lalaland.object.Enemy_Blender;
import com.lalaland.object.Enemy_FlockerLeader;
import com.lalaland.object.Enemy_Grunt;
import com.lalaland.object.Enemy_Hermit;
import com.lalaland.object.Enemy_MartyrLeader;
import com.lalaland.object.Enemy_Soldier;
import com.lalaland.object.Player;
import com.lalaland.utility.Utility;

import processing.core.PApplet;
import processing.core.PVector;

public class Engine extends PApplet {

  private static final PVector RESOLUTION = new PVector(1000, 800);
  private static final int FRAME_RATE = 55;
  private static final int SMOOTH_FACTOR = 4;
  private static final int BONUS_DROP_INTERVAL = 800;
  private static final PVector BACKGROUND_RGB = new PVector(60, 60, 60);
  private static final PVector PLAYER_INITIAL_POSITION = new PVector(RESOLUTION.x / 2, RESOLUTION.y / 2);
  private static final PVector NUM_TILES = new PVector(100, 80);
  
  private Environment environment;
  private Player player;

  private List<Enemy> enemies;
  private List<BonusItem> bonusItems;
  private boolean inDEVMode = false;

  private static float time = 0f;

  public static float getTime() {
    return time;
  }

  public static PVector getResolution() {
    return RESOLUTION;
  }

  public void settings() {
    size((int)RESOLUTION.x, (int)RESOLUTION.y, P3D);
    smooth(SMOOTH_FACTOR);
    initializeEnemySpawnDetails();
  }

  public List<Enemy> getEnemies() {
    return enemies;
  }

  private void initializeEnemySpawnDetails() {
    Enemy_Soldier.initializeSpawnDetails(FRAME_RATE);
    Enemy_Hermit.initializeSpawnDetails(FRAME_RATE);
    Enemy_Grunt.initializeSpawnDetails(FRAME_RATE);
    Enemy_FlockerLeader.initializeSpawnDetails(FRAME_RATE);
    Enemy_MartyrLeader.initializeSpawnDetails(FRAME_RATE);
    Enemy_Blender.initializeSpawnDetails(FRAME_RATE);
  }
  
  public void setup() {
    noStroke();    
    frameRate(FRAME_RATE);
    environment = new Environment(this, RESOLUTION, NUM_TILES);
    
    player = new Player(PLAYER_INITIAL_POSITION.x, PLAYER_INITIAL_POSITION.y, this, environment);
    environment.setPlayer(player);
    enemies = new LinkedList<>();
    environment.setEnemies(enemies);

    bonusItems = new LinkedList<>();
    environment.setBonusItems(bonusItems);
  }
  
  public static void main(String args[]) {  
    PApplet.main(new String[] { "com.lalaland.engine.Engine" });
  }
  
  public void draw() {
    time = millis();

    background(BACKGROUND_RGB.x, BACKGROUND_RGB.y, BACKGROUND_RGB.z);
    
    environment.drawObstacles();
    controlItems();
    if (player.isAlive())
      spawnBonusItems();
    controlEnemies();
    controlPlayer();
    controlHUD();
  }

  private void controlPlayer() {
    if (player.isAlive())
      player.move();
    else
      Utility.drawText("HE'S DEAD, JIM!", RESOLUTION.x / 2f - 15f, RESOLUTION.y / 2f - 15f, this);
    player.display();
    controlPlayerGun();    
  }

  private void controlPlayerGun(){
  	if(mousePressed && frameCount % Player.getGUN_FIRE_INTERVAL() == 0){
      if(mousePressed && mouseButton == RIGHT)
        player.shootRadialBullets();
      else{
        player.shootBullet();
      }
    }

  }
  
  private void controlEnemies() {
    if (!inDEVMode)
      spawnEnemies();
    Iterator<Enemy> i = enemies.iterator();
    while (i.hasNext()) {
      Enemy enemy = i.next();
      if (enemy.isAlive()) {
        if (player.isAlive())
          enemy.move();
        enemy.display();
      }
      else{
      	i.remove();
      }
    }
  }

  private void spawnEnemies() {
    for (Enemy.EnemyTypes enemyType : Enemy.EnemyTypes.values()) {
      switch (enemyType) {
        case SOLDIER:
          if ((Enemy_Soldier.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_Soldier.SPAWN_OFFSET) % Enemy_Soldier.SPAWN_INTERVAL == 0) && (Enemy_Soldier.getSpawnCount() < Enemy_Soldier.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.SOLDIER);
          }
          break;
        case HERMIT:
          if ((Enemy_Hermit.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_Hermit.SPAWN_OFFSET) % Enemy_Hermit.SPAWN_INTERVAL == 0) && (Enemy_Hermit.getSpawnCount() < Enemy_Hermit.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.HERMIT);
          }
          break;
        case GRUNT:
          if ((Enemy_Grunt.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_Grunt.SPAWN_OFFSET) % Enemy_Grunt.SPAWN_INTERVAL == 0) && (Enemy_Grunt.getSpawnCount() < Enemy_Grunt.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.GRUNT);
          }
          break;
        case FLOCKER:
          if ((Enemy_FlockerLeader.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_FlockerLeader.SPAWN_OFFSET) % Enemy_FlockerLeader.SPAWN_INTERVAL == 0) && (Enemy_FlockerLeader.getSpawnCount() < Enemy_FlockerLeader.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.FLOCKER);
          }
          break;
        case MARTYR:
          if ((Enemy_MartyrLeader.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_MartyrLeader.SPAWN_OFFSET) % Enemy_MartyrLeader.SPAWN_INTERVAL == 0) && (Enemy_MartyrLeader.getSpawnCount() < Enemy_FlockerLeader.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.MARTYR);
          }
          break;
        case BLENDER:
          if ((Enemy_Blender.SPAWN_OFFSET <= frameCount) && ((frameCount - Enemy_Blender.SPAWN_OFFSET) % Enemy_Blender.SPAWN_INTERVAL == 0) && (Enemy_Blender.getSpawnCount() < Enemy_Blender.SPAWN_MAX)) {
            spawnEnemyNow(Enemy.EnemyTypes.BLENDER);
          }
          break;
      }
    }
  }

  private void spawnEnemyNow(Enemy.EnemyTypes enemytype){
    PVector spawnSpot;
    switch (enemytype){
      case SOLDIER:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_Soldier(spawnSpot.x, spawnSpot.y, this, environment));
          break;
      case HERMIT:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_Hermit(spawnSpot.x, spawnSpot.y, this, environment));
          break;
      case GRUNT:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_Grunt(spawnSpot.x, spawnSpot.y, this, environment));
          break;
      case FLOCKER:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_FlockerLeader(spawnSpot.x, spawnSpot.y, this, environment));
          break;
      case MARTYR:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_MartyrLeader(spawnSpot.x, spawnSpot.y, this, environment));
          break;
      case BLENDER:
          spawnSpot = getRandomSpawnSpot();
          enemies.add(new Enemy_Blender(spawnSpot.x, spawnSpot.y, this, environment));
          break;
    }
  }

  private PVector getRandomSpawnSpot() {
    PVector randomSpawnSpot = new PVector();

    float random = random(1, 100);
    if (random < 50)
      randomSpawnSpot.x = random + 3 * Enemy.BORDER_PADDING;
    else
      randomSpawnSpot.x = RESOLUTION.x - 3 * Enemy.BORDER_PADDING - (random - 50);

    random = random(1, 100);
    if (random < 50)
      randomSpawnSpot.y = random + 3 * Enemy.BORDER_PADDING;
    else
      randomSpawnSpot.y = RESOLUTION.y - 3 * Enemy.BORDER_PADDING - (random - 50);

    return randomSpawnSpot;
  }

  private void controlItems(){
  	Iterator<BonusItem> i = bonusItems.iterator();
  	while(i.hasNext()){
  		BonusItem item = i.next();
  		if(!item.isConsumed())
  			item.display();
  		else
  			i.remove();
  	}
  }

  private void controlHUD(){
    Utility.drawText("HP DAMAGE: " + Enemy.getTotalHPDamage(), width - 120, 30, this);
  }

  private void spawnBonusItems(){
  	if(frameCount % BONUS_DROP_INTERVAL == 0){
  		PVector position = environment.getRandomValidPosition();
      double rand = Math.random();
      boolean radialBullets = false;
      if(rand > 0.5)
        radialBullets = true;
  		bonusItems.add(new BonusItem(position.x, position.y, this, environment, radialBullets));
  	}
  }

  public void keyPressed() {
    player.setDirection(key, true);
  }
  
  public void keyReleased() {
    player.setDirection(key, false);
    if (inDEVMode)
      handleEnemySpawn();
  }

  private void handleEnemySpawn(){
    switch (key){
      case '1':
        spawnEnemyNow(Enemy.EnemyTypes.SOLDIER);
        break;
      case '2':
        spawnEnemyNow(Enemy.EnemyTypes.HERMIT);
        break;
      case '3':
        spawnEnemyNow(Enemy.EnemyTypes.GRUNT);
        break;
      case '4':
        spawnEnemyNow(Enemy.EnemyTypes.MARTYR);
        break;
      case '5':
        spawnEnemyNow(Enemy.EnemyTypes.FLOCKER);
        break;
      case '6':
        spawnEnemyNow(Enemy.EnemyTypes.BLENDER);
        break;
    }
  }

}
