package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private GameObject target;
    private GameObject teleporter;
    // ADD MORE CONST IF ANY
    final int SAFETY_NUM = 20;
    final int TELEPORT_COST = 20;
    final int PLAYER_RADIUS = 800;
    final int SUPERNOVA_RADIUS = 50;
    final int SUPERFOOD_RADIUS = 50;

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
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = new Random().nextInt(360);
        
        
        if (!gameState.getGameObjects().isEmpty()) {

            if (getDistanceBoundary() <= 0) {
                System.out.println("WARNING: OUT OF BOUNDS\n");
            }
            System.out.printf("SIZE: %d\n", bot.size);
            System.out.printf("TELEPORT COUNT: %d\n", bot.teleporterCount);

            var teleporterList = getTeleporter();
            // TELEPORT
            if (!teleporterList.isEmpty()) {
                this.teleporter = teleporterList.get(0);
                if (computeTeleport()) {
                    /* NOTHING */
                } else {
                    computeFoodTarget();
                }
            } else {

                // TODO: PLAYER LIST USE GIVEN RADIUS
                var playerList = gameState.getPlayerGameObjects()
                                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.PLAYER)
                                .sorted(Comparator
                                        .comparing(item -> getDistanceBetween(bot, item)))
                                .collect(Collectors.toList());

                // System.out.println(playerList.size());
                
                if (!playerList.isEmpty()) {
                    System.out.println("Detected players inside range\n");
                    System.out.println("COMPUTE OFFENSE\n");
                    var i = 0;
                    while (i < playerList.size()) {
                        target = playerList.get(i);
                        if (computeOffense()) {
                            // FOUND NEW TARGET FOR OFFENSE
                            break;
                        } 
    
                        if (i == playerList.size() - 1) {
                            System.out.println("No target available...setting to null...\n");
                            target = null;
                        }
                        i++;
                    }
                }
                
                // NO TARGET
                if (target == null) {
                    // System.out.printf("eat eat size: %d\n",bot.getSize());
                   computeFoodTarget();
                }
    
                }
            }

        this.playerAction = playerAction;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    /**
     * 
     * @return List dari object teleporter di dalam map
     */
    private List<GameObject> getTeleporter() {
        var teleporter =  gameState.getGameObjects()
        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
        .collect(Collectors.toList());

        if (!teleporter.isEmpty()) {
            System.out.println("Found teleporter\n");
            for (int i =0; i < teleporter.size();i++) {
                System.out.printf("%s\n", teleporter.get(i).getId());
            }

            return teleporter;
        }

        return teleporter;
    }

    /**
     * 
     * @return true jika sudah waktunya untuk melakukan teleport
     */
    private boolean computeTeleport() {
        if (this.target == null) {
            System.out.println("TARGET IS NULL\n");
        }
        if (this.target != null && this.teleporter != null) {
            // TODO: mengecek kembali size dari target apakah masih memungkinkan atau tidak
            System.out.printf("DISTANCE TELEPORTER FROM TARGET: %d\n", (int) getDistanceBetween(target, teleporter));
            if ((int) getDistanceBetween(target, teleporter) <= 20) {
                System.out.println("Teleporting...\n");
                playerAction.action = PlayerActions.TELEPORT;
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * @return true jika terdapat bot lain yang bisa dimakan
     */
    private boolean computeOffense() { 
        // TODO: Manual Offense (chasing)       
        if (bot.teleporterCount > 0 && target.getSize() < bot.getSize() - TELEPORT_COST - SAFETY_NUM) {
            System.out.printf("Target is %s\n", target.getId());
            playerAction.action = PlayerActions.FIRETELEPORT;
            playerAction.heading = getHeadingBetween(target);
            return true;
        }
        System.out.printf("ID: %s TOO BIG SIZE: %d\n",  target.getId(), target.getSize());
        return false;
    }

    /*
     * mencari target makanan selanjutnya
     */
    private void computeFoodTarget() {
        // TODO: confirm the target is safe from gas cloud or other player
        System.out.println("Waiting for teleporter...\n");
        var foodList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        playerAction.heading = getHeadingBetween(foodList.get(0));
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

    private List<GameObject> getSuperfood(List<GameObject> object) {
        System.out.println("Cek Superfood...\n");
        var hasil = object.stream()
        .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERFOOD)
        .collect(Collectors.toList());
        return hasil;
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
        var distanceFromOrigin = (int) Math.ceil(Math.sqrt(Math.pow(bot.getPosition().x, 2) + Math.pow(bot.getPosition().y, 2)));

        if (gameState.world.getRadius() != null) {

            return gameState.world.getRadius() - distanceFromOrigin;
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

    private double runFromAtt(GameObject bot, GameObject atkr) 
    {
        var distAtkr = getDistanceBetween(atkr);
        var headingAtkr = atkr.currentHeading;
        if (distAtkr <= atkr.speed && headingAtkr == getOppositeDirection(bot, atkr)) {
            return getOppositeDirection(bot, atkr);
        } else {
            return bot.currentHeading;
        }
        
    }

    private double dodgeObj(GameObject bot, GameObject obj)
    {
        var headingObj = obj.currentHeading;
        if ((headingObj-getOppositeDirection(bot, obj)%360>=0 && headingObj-getOppositeDirection(bot, obj)%360<60)) {
            return (getOppositeDirection(bot, obj) - 90) % 360;
        } else if ((headingObj-getOppositeDirection(bot, obj)%360<0 && headingObj-getOppositeDirection(bot, obj)%360>-60)) {
            return (getOppositeDirection(bot, obj)+90) %360;
        } else {
            return bot.currentHeading;
        }
    }

    
}
