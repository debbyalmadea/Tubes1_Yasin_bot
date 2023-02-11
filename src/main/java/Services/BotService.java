package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private GameObject superbomb;
    private GameObject firedTeleporter;
    // ADD MORE CONST IF ANY
    final int SAFETY_NUM = 20;
    final int TELEPORT_COST = 20;
    final int PLAYER_RADIUS = 800;
    final int SUPERNOVA_RADIUS = 50;
    final int SUPERFOOD_RADIUS = 50;
    final int SUPERNOVABOMB_RADIUS = 200;
    final int GASCLOUD_RADIUS = 10;
    final int BOUNDRY_RADIUS = 10;
    final int ASTEROID_RADIUS = 5;
    final int TELEPORT_SPEED = 20;
    final int MAX_TELEPORTER = 10;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        playerAction.setAction(PlayerActions.FORWARD);
        playerAction.setHeading(new Random().nextInt(360));        
        
        if (!gameState.getGameObjects().isEmpty()) {

            System.out.printf("=====================TICK %d\n", gameState.getWorld().currentTick);
            if (getDistanceBoundary() <= 0  ) {
                System.out.println("WARNING: OUT OF BOUNDS\n");
            }
            
            System.out.printf("SIZE: %d\n", bot.size);
            System.out.printf("TELEPORT COUNT: %d\n", bot.teleporterCount);

            var superbombList = getSupernovaBomb();
            getFiredTeleporter();
            // TELEPORT
            if (this.firedTeleporter != null && isTeleporterStillAvailable()) {
                var nearestTeleporter = getNearestTeleporter();
                if (nearestTeleporter != null && isObjHeadingUs(bot, nearestTeleporter))  {
                    playerAction.action = PlayerActions.FORWARD;
                    playerAction.heading = dodgeObj(bot, nearestTeleporter);
                } else {
                    if( isTeleporterNearSmallerEnemy()) {
                        playerAction.setAction(PlayerActions.TELEPORT);
                        this.firedTeleporter = null;
                    } else {
                        goToFood();
                    }
                }
            } else {
                var isOffensePossible = computeOffense();
                if (!isOffensePossible) {
                    System.out.printf("eat eat size: %d\n",bot.getSize());
                    if (!superbombList.isEmpty()) {
                        this.superbomb = superbombList.get(0);
                        if (isObjHeadingUs(bot, superbomb)) {
                            dodgeObj(bot, superbomb);
                        } else {
                            goToFood();
                        }
                    }
                    else {
                        goToFood();
                    }
                }
            } 
        } 

        this.playerAction = playerAction;
        System.out.printf("ACTION: %d\n", playerAction.action.value);
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private GameObject getNearestTeleporter() {
        var teleporter =  gameState.getGameObjects()
        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
        .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
        .collect(Collectors.toList());

        if (!teleporter.isEmpty()) {
            System.out.println("Found teleporter\n");

            return teleporter.get(0);
        }

        return null;
    }

    private void getFiredTeleporter() {
        var teleporterList = gameState.getGameObjects()
                            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && Math.abs(getHeadingBetween(item) - item.currentHeading) <= 10)
                            .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                            .collect(Collectors.toList());  

                            
        if (teleporterList.size() == 0) {
            System.out.println("No teleport fired.");
        } else {
            for (int i = 0; i < teleporterList.size(); i++) {
                System.out.printf("TELEPORTER %d HEADING %d HEADING BETWEEN %d\n", i, teleporterList.get(i).currentHeading, getHeadingBetween(teleporterList.get(i)));
            }
            this.firedTeleporter = teleporterList.get(0);
            System.out.printf("Teleport fired...\n");
        }
    }

    private boolean isTeleporterStillAvailable() {
        var teleporterList = gameState.getGameObjects()
                            .stream().filter(item -> item.getId() == firedTeleporter.getId())
                            .toList();
        
        return !teleporterList.isEmpty();
    }       

    private boolean isTeleporterNearSmallerEnemy() {
        if (this.firedTeleporter == null) {
            System.out.println("TELEPORTER IS NULL");
            return false;
        } else {
            System.out.println("IS TELEPORT NEAR SMALLER ENEMY?");
            var playerList = gameState.getPlayerGameObjects()
                            .stream().filter(item ->  item.getSize() < bot.getSize())
                            .sorted(Comparator.comparing(item -> item.getSize()))
                            .collect(Collectors.toList());

            for (int i = 0; i < playerList.size();i++) {
                var dist = getDistanceBetween(playerList.get(i), firedTeleporter);
                System.out.printf("%s DISTANCE: %f\n", playerList.get(i).getId(), dist);
                if (dist <= TELEPORT_SPEED + (bot.getSize() / 2)) {
                    System.out.println("IT IS");
                    return true;
                }
            }
            // if (playerList.isEmpty()) {
            //     System.out.println("FALSE");
            //     return false;
            // }
                return false;
        
            // System.out.println("IT IS");
            // return true;
        }
    }

    /**
     * 
     * @return true jika terdapat bot lain yang bisa dimakan
     */
    private boolean computeOffense() { 
        // TODO: Manual Offense (chasing) 
        var playerList = gameState.getPlayerGameObjects()
                        .stream().sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                        .toList();
        if (playerList.size() > 0 && bot.teleporterCount > 0) {
            for (int i =0; i < playerList.size();i++) {
                var currTarget = playerList.get(i);
                if (currTarget.getSize() < bot.getSize() - TELEPORT_COST - SAFETY_NUM) {
                    System.out.printf("Target is %s\n", currTarget.getId());
                    playerAction.action = PlayerActions.FIRETELEPORT;
                    playerAction.heading = getHeadingBetween(currTarget);
                    return true;
                }
            }

            return false;
        }
        // System.out.printf("ID: %s TOO BIG SIZE: %d\n",  target.getId(), target.getSize());
        return false;
    }

    private void goToFood() {
        var cekSuperfood = getSuperfood(getObjectsWithin(SUPERFOOD_RADIUS));
        if (!cekSuperfood) {
            var status = computeFoodTarget();
            if (!status) {
                // NANTI ISI ACTION PALING TERAKHIR
            }
        }
    }

    /*
     * mencari target makanan selanjutnya
     */
    private boolean computeFoodTarget() {
        // TODO: confirm the target is safe from gas cloud or other player
        System.out.println("Compute Food Safe...\n");
        var listGas = getGasCloudWithin(getObjectsWithin(GASCLOUD_RADIUS));
        System.out.println("list gas...\n");
        System.out.println(listGas);
        var listAst = getAsteroidWithin(getObjectsWithin(ASTEROID_RADIUS));
        System.out.println("list asteroid...\n");
        System.out.println(listAst);
        if (listAst.isEmpty() && listGas.isEmpty() && getDistanceBoundary() > BOUNDRY_RADIUS) {   
            System.out.println("Food is safe..\n");
            var foodList = gameState.getGameObjects()
            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
            .sorted(Comparator
            .comparing(item -> getDistanceBetween(bot, item)))
            .collect(Collectors.toList());
            
            playerAction.heading = getHeadingBetween(foodList.get(0));
            return true;
        } else if (listAst.isEmpty() || listGas.isEmpty()) { // MASIH BELOM FIX AMAN
            System.out.println("Food is safe but the distance is longer...\n");
            var foodList = gameState.getGameObjects()
            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD && getOuterDistanceBetween(bot, item) > 10) // MASIH KIRA2 ALIAS BLM AMAN
            .sorted(Comparator
            .comparing(item -> getDistanceBetween(bot, item)))
            .collect(Collectors.toList());
            
            playerAction.heading = getHeadingBetween(foodList.get(0));
            return true;
        }
        return false; 
    }

    private boolean getSuperfood(List<GameObject> object) {
        System.out.println("Cek Superfood...\n");
        var hasil = object.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERFOOD)
        .collect(Collectors.toList());
        
        if (!hasil.isEmpty()){
            System.out.println("ADA SUPERFOOD...\n");
            playerAction.heading = getHeadingBetween(hasil.get(0));
            return true;
        }
        return false;
    }

    private List<GameObject> getGasCloudWithin(List<GameObject> objList) {
        System.out.println("Get Gas Within...\n");
        var hasil = objList.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
        .collect(Collectors.toList());
        return hasil;
    }

    private List<GameObject> getAsteroidWithin(List<GameObject> objList) {
        System.out.println("Get Asteroid Within...\n");
        var hasil = objList.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.ASTEROIDFIELD)
        .collect(Collectors.toList());
        return hasil;
    }

    /**
     * 
     * @param radius
     * @return List dari game objects yang ada di dalam radius
     */
    private List<GameObject> getObjectsWithin(int radius) {
        // TODO: Use this in main func
        System.out.println("GET OBJECT WITHIN\n");
        var objList = gameState.getGameObjects()
        .stream().filter(item -> getOuterDistanceBetween(bot, item) <= radius)
        .collect(Collectors.toList());

        return objList;
    }

    /**
     * 
     * @param radius
     * @return List dari player yang ada di dalam radius
     */
    private List<GameObject> getPlayersWithin(int radius) {
        System.out.println("GET OBJECT WITHIN\n");
        var objList = gameState.getPlayerGameObjects()
        .stream().filter(item -> getOuterDistanceBetween(bot, item) <= radius)
        .collect(Collectors.toList());

        return objList;
    }

    private List<GameObject> cekSupernovaAvailable(List<GameObject> objList) {
        System.out.println("Cek Supernova...\n");
        var hasil = objList.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVAPICKUP)
        .collect(Collectors.toList());
        return hasil;
    } 

    private List<GameObject> getSupernovaBomb() {
        var superbomb =  gameState.getGameObjects()
        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVABOMB)
        .collect(Collectors.toList());

        if (!superbomb.isEmpty()) {
            return superbomb;
        }

        return superbomb;
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    /**
     * 
     * @param object1
     * @param object2
     * @return Jarak object1 dan object2 dari sisi terluar
     */
    private double getOuterDistanceBetween(GameObject object1, GameObject object2) {
        return getDistanceBetween(object1, object2) - object1.getSize() - object2.getSize();
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }
 
    private int getDistanceBoundary() {
        var distanceFromOrigin = (int) Math.ceil(Math.sqrt(Math.pow(bot.getPosition().x , 2) + Math.pow(bot.getPosition().y, 2)));

        if (gameState.world.getRadius() != null) {
            System.out.println(gameState.world.getRadius() - distanceFromOrigin - bot.getSize());
            return gameState.world.getRadius() - distanceFromOrigin - bot.getSize();
        } 

        return 123;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }
    
    private double getDistanceBetween(GameObject go)
    {
        return getDistanceBetween(bot, go);
    }

    private int getOppositeDirection(GameObject gameObject1, GameObject gameObject2)
    {
        return toDegrees(Math.atan2(gameObject2.position.y - gameObject1.position.y, gameObject2.position.x - gameObject1.position.y));
    }

    private int runFromAtt(GameObject bot, GameObject atkr) 
    {
        var distAtkr = getDistanceBetween(atkr);
        var headingAtkr = atkr.currentHeading;
        if (distAtkr <= atkr.speed && headingAtkr == getOppositeDirection(bot, atkr)) {
            return getOppositeDirection(bot, atkr);
        } else {
            return -1;
        }
        
    }

    private int dodgeObj(GameObject bot, GameObject obj)
    {
        var headingObj = obj.currentHeading;

        if ((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)) {
            return (getOppositeDirection(bot, obj) - 180 + headingObj) % 360;
        } else if ((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)) {
            return (getOppositeDirection(bot, obj)+ 180 - headingObj) %360;
        } else {
            return -1;
        }
    }

    private boolean isObjHeadingUs(GameObject bot, GameObject obj) {
        var headingObj = obj.currentHeading;
        return (((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)))||(((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)));
    }
    
}
