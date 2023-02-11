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
    final int TORPEDO_RADIUS = 400;
    final int AVOID_TORPEDO_RADIUS = 200;
    final int GASCLOUD_RADIUS = 10;
    final int BOUNDRY_RADIUS = 10;
    final int ASTEROID_RADIUS = 5;
    final int SUPERNOVABOMB_RADIUS = 200;
    final int TORPEDODODGE_R = 400;
    final int TELEPORTDODGE_R = 100;
    final int playerDODGE_R = 80;
    final int TELEPORT_SPEED = 20;
    final int MAX_TELEPORTER = 10;
    final int TORPEDO_COST = 5;

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

    public boolean computeNextPlayerAction(PlayerAction playerAction) {
        playerAction.setAction(PlayerActions.FORWARD);
        playerAction.setHeading(new Random().nextInt(360));        
        
        if (!gameState.getGameObjects().isEmpty()) {
            System.out.printf("=====================TICK %d\n", gameState.getWorld().currentTick);
            if (getDistanceBoundary() <= 0  ) {
                System.out.println("WARNING: OUT OF BOUNDS\n");
                moveToCenter();
                return true;
            }
            
            System.out.printf("SIZE: %d\n", bot.size);
            System.out.printf("TELEPORT COUNT: %d\n", bot.teleporterCount);

            var superbombList = getSupernovaBomb();
            if (this.firedTeleporter == null) {
                getFiredTeleporter();
            }

            if (this.firedTeleporter !=null) {
                System.out.printf("FIRED TELEPORTER: %s\n", firedTeleporter.getId());
            }
            // TELEPORT
            if (this.firedTeleporter != null && isTeleporterStillAvailable()) {
                var nearestTeleporter = getNearestTeleporter();
                if (nearestTeleporter != null && isObjHeadingUs(nearestTeleporter))  {
                    var runTeleport = dodgeObj(nearestTeleporter, TELEPORTDODGE_R);
                    if (!runTeleport) {
                        System.out.println("ADA TELEPORT TAPI GA BAHAYA\n");
                    }
                } else {
                    if(isTeleporterNearSmallerEnemy()) {
                        playerAction.setAction(PlayerActions.TELEPORT);
                        this.firedTeleporter = null;
                        return true;
                    } else {
                        goToFood(); 
                    }
                }
            } else {
                List<GameObject> torpedoes = getObjectsWithin(AVOID_TORPEDO_RADIUS, ObjectTypes.TORPEDOSALVO)
                                            .stream().filter(item->isObjHeadingUs(item))
                                            .sorted(Comparator.comparing(item->getDistanceBetween(item)))
                                            .toList();
                if (!torpedoes.isEmpty()) {
                    var runTorpedos = dodgeTorpedos(torpedoes.get(0), TELEPORTDODGE_R);
                    if (!runTorpedos) {
                        System.out.println("ADA TORPEDOS TAPI AMAN\n");
                    }
                    return true;
                }

                var isOffensePossible = computeOffense();
                if (!isOffensePossible) {
                    if (!superbombList.isEmpty()) {
                        this.superbomb = superbombList.get(0);
                        var runSuperBomb = dodgeObj(superbomb, SUPERNOVABOMB_RADIUS);
                        if (!runSuperBomb) {
                            goToFood(); 
                        } else {
                            System.out.println("ADA SUPERNOVA BAHAYA");
                        }
                    } else {
                        //lain
                        var isAbleToFireTorpedoes = fireTorpedoSalvo();
                        if (!isAbleToFireTorpedoes) {
                                goToFood();
                        }
                    }
                }
            } 
        } 

        this.playerAction = playerAction;
        // System.out.printf("ACTION: %d\n", playerAction.action.value);
        return true;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void moveToCenter() {
        Position position = new Position(0, 0);
        GameObject worldCenter = new GameObject(bot.getId(), 0, 0, 0, position, ObjectTypes.FOOD, 0, 0, 0, 0, 0);

        playerAction.setAction(PlayerActions.FORWARD);
        playerAction.setHeading(getHeadingBetween(worldCenter));
    }

    // TELEPORT MECHANISM
    private GameObject getNearestTeleporter() {
        var teleporter =  gameState.getGameObjects()
                            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
                            .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                            .collect(Collectors.toList());

        if (!teleporter.isEmpty()) {
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
            // System.out.println("No teleport fired.");
        } else {
            this.firedTeleporter = teleporterList.get(0);
        }
    }

    private boolean isTeleporterStillAvailable() {
        var teleporterList = gameState.getGameObjects()
                            .stream().filter(item -> item.getId().equals(firedTeleporter.getId()))
                            .collect(Collectors.toList());
        
        if (teleporterList.isEmpty()) {
            System.out.println("TELEPORTER EXISTED");
            this.firedTeleporter = null;
        }
        
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
        if (this.firedTeleporter != null) {
            return false;
        }
        var playerList = gameState.getPlayerGameObjects()
                        .stream().sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                        .toList();
        if (playerList.size() > 0 && bot.teleporterCount > 0) {
            for (int i =1; i < playerList.size();i++) {
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

    // ATTACK MECHANISM

    private boolean fireTorpedoSalvo() {
        if (bot.getSize() < TORPEDO_COST * 5) {
            return false;
        }

        var playerList = gameState.getPlayerGameObjects()
                            .stream().sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                            .collect(Collectors.toList());

        if (playerList.isEmpty()) {
            return false;
        }

        int heading = getHeadingBetween(playerList.get(1));
        playerAction.action = PlayerActions.FIRETORPEDOES;
        playerAction.heading = heading;
        System.out.printf("Fired torpedoeeeees...\n");
        return true;
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
        // System.out.println("Compute Food Safe...\n");
        var listGas = getGasCloudWithin(getObjectsWithin(GASCLOUD_RADIUS));
        // System.out.println("list gas...\n");
        System.out.println(listGas);
        var listAst = getAsteroidWithin(getObjectsWithin(ASTEROID_RADIUS));
        // System.out.println("list asteroid...\n");
        System.out.println(listAst);
        if (listAst.isEmpty() && listGas.isEmpty() && getDistanceBoundary() > BOUNDRY_RADIUS) {   
            // System.out.println("Food is safe..\n");
            var foodList = gameState.getGameObjects()
            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
            .sorted(Comparator
            .comparing(item -> getDistanceBetween(bot, item)))
            .collect(Collectors.toList());
            
            playerAction.heading = getHeadingBetween(foodList.get(0));
            return true;
        } else if (listAst.isEmpty() || listGas.isEmpty()) { // MASIH BELOM FIX AMAN
            // System.out.println("Food is safe but the distance is longer...\n");
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
        // System.out.println("Cek Superfood...\n");
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
        // System.out.println("Get Gas Within...\n");
        var hasil = objList.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
        .collect(Collectors.toList());
        return hasil;
    }

    private List<GameObject> getAsteroidWithin(List<GameObject> objList) {
        // System.out.println("Get Asteroid Within...\n");
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
        // System.out.println("GET OBJECT WITHIN\n");
        var objList = gameState.getGameObjects()
        .stream().filter(item -> getOuterDistanceBetween(bot, item) <= radius)
        .collect(Collectors.toList());

        return objList;
    }

    private List<GameObject> getObjectsWithin(int radius, ObjectTypes type) {
        return getObjectsWithin(radius).stream().filter(item->item.getGameObjectType() == type).collect(Collectors.toList());
    }

    /**
     * 
     * @param radius
     * @return List dari player yang ada di dalam radius
     */
    private List<GameObject> getPlayersWithin(int radius) {
        // System.out.println("GET OBJECT WITHIN\n");
        var objList = gameState.getPlayerGameObjects()
        .stream().filter(item -> getOuterDistanceBetween(bot, item) <= radius)
        .collect(Collectors.toList());

        return objList;
    }

    private List<GameObject> cekSupernovaAvailable(List<GameObject> objList) {
        // System.out.println("Cek Supernova...\n");
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
        var direction =toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
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

    private int runFromAtt(GameObject atkr) 
    {
        var distAtkr = getDistanceBetween(atkr);
        var headingAtkr = atkr.currentHeading;
        if (distAtkr <= atkr.speed && headingAtkr == getOppositeDirection(bot, atkr)) {
            return getOppositeDirection(bot, atkr);
        } else {
            return -1;
        }
        
    }

    private boolean dodgeObj(GameObject obj, int radius)
    {
        var headingObj = obj.currentHeading;

        if ((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)) {
            if (bot.getSize() > 20 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m STARTAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STARTAFTERBURNER;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360; 
                return true;
            } else {
                System.out.println((char)27+"[01;32m STOPAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                return true;
            }
        } else if ((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)) {
            if (bot.getSize() > 20 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m STARTAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STARTAFTERBURNER;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) %360; 
                return true;
            } else {
                System.out.println((char)27+"[01;32m STOPAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean dodgeTorpedos(GameObject obj, int radius)
    {
        var headingObj = obj.currentHeading;

        if ((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)) {
            if (bot.getSize() > 30 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m USESHIELD\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.USESHIELD;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360; 
                return true;
            } else if (bot.getSize() > 20 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m STARTAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STARTAFTERBURNER;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360; 
                return true;
            } else {
                System.out.println((char)27+"[01;32m STOPAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                return true;
            }
        } else if ((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)) {
            if (bot.getSize() > 30 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m USESHIELD\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.USESHIELD;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) %360; 
                return true;
            } else if (bot.getSize() > 20 && getDistanceBetween(obj) < radius) {
                System.out.println((char)27+"[01;31m STARTAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STARTAFTERBURNER;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) %360; 
                return true;
            } else {
                System.out.println((char)27+"[01;32m STOPAFTERBURNER\n"+(char)27+"[00;00m");
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isObjHeadingUs(GameObject obj) {
        var headingObj = obj.currentHeading;
        return (((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)))||(((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)));
    }
    
}
