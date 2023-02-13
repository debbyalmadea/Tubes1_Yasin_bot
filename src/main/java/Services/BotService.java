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
    private HashSet<Effects> currentEffect = new HashSet<Effects>();
    // ADD MORE CONST IF ANY
    final int SAFETY_NUM = 40;
    final int TELEPORT_COST = 20;
    final int PLAYER_RADIUS = 200;
    final int SUPERNOVA_RADIUS = 50;
    final int SUPERFOOD_RADIUS = 50;
    final int TORPEDO_RADIUS = 600;
    final int AVOID_TORPEDO_RADIUS = 100;
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
            var distanceFromBoundary = getDistanceBoundary(bot);
            var supernovaList = gameState.getGameObjects().stream()
                    .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVAPICKUP)
                    .toList();
            var superbombList = getSupernovaBomb();
            GameObject dangerousNearPlayer = getDangerousNearPlayer();
            getEffects(bot.effects);

            System.out.printf("SIZE: %d\n", bot.size);
            System.out.printf("EFFECTS HASH CODE: %d\n", bot.effects);
            System.out.printf("TELEPORT COUNT: %d\n", bot.teleporterCount);
            System.out.printf("WORLD RADIUS: %d\n", gameState.getWorld().radius);
            System.out.printf("DISTANCE FROM BOUNDARY: %d\n", distanceFromBoundary);
            System.out.printf("SUPERNOVA: %d\n", supernovaList.size());

            if (supernovaList.size() > 0) {
                System.out.printf("DISTANCE FROM SN: %f\n", getDistanceBetween(supernovaList.get(0)));
            }
            System.out.printf("SHIELD: %d\n", bot.shieldCount);
            if (gameState.world.centerPoint != null) {
                System.out.printf("X: %d Y: %d\n", gameState.world.centerPoint.x, gameState.world.centerPoint.y);
            }

            if (this.firedTeleporter == null) {
                getFiredTeleporter();
            }

            if (this.firedTeleporter != null && isTeleporterStillAvailable() && isTeleporterNearSmallerEnemy()) {
                playerAction.setAction(PlayerActions.TELEPORT);
                this.firedTeleporter = null;
                return true;
            }

            var supernova = getSupernova();
            if (supernova != null) {
                System.out.println("GOING AFTER SUPERNOVA PICKUP");
                playerAction.setAction(PlayerActions.FORWARD);
                playerAction.setHeading(getHeadingBetween(supernova));
                return true;
            }

            if (bot.supernovaAvailable > 0) {
                System.out.println("WE HAVE SUPERNOVA");
            }

            if (currentEffect.contains(Effects.GASCLOUD)) {
                System.out.println("GAS CLOUD EFFECTS");
                moveToCenter();
                return true;
            }

            if (distanceFromBoundary <= 100) {
                System.out.println("TOO CLOSE TO BOUNDARY...MOVING TO CENTER...\n");
                moveToCenter();
                return true;
            }

            if (dangerousNearPlayer != null) {
                System.out.println("WARNING BIGGER BOT");
                var isAbleToFireTorpedoes = fireTorpedoSalvo();
                if (!isAbleToFireTorpedoes) {
                    runFromAtt(dangerousNearPlayer);
                }
                return true;
            }

            var nearestTeleporter = getNearestTeleporter();
            if (nearestTeleporter != null
                    && Math.abs(getHeadingBetween(nearestTeleporter) - nearestTeleporter.currentHeading) >= 90) {
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = getOppositeDirection(bot, nearestTeleporter);
                return true;
            }

            GameObject gasCloud = getGasCloudInPath();
            if (gasCloud != null && distanceFromBoundary <= bot.speed + bot.getSize() + 40) {
                System.out.println("Avoid Gas Cloud");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = getOppositeDirection(bot, gasCloud);
                return true;
            }

            GameObject vulnerablePlayer = getVunerableNearPlayer();
            if (vulnerablePlayer != null) {
                System.out.println("CHASING VULNERABLE PLAYER");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = getHeadingBetween(vulnerablePlayer);
                return true;
            }

            List<GameObject> torpedoes = getObjectsWithin(AVOID_TORPEDO_RADIUS, ObjectTypes.TORPEDOSALVO)
                    .stream().filter(item -> isObjHeadingUs(item))
                    .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                    .toList();
            if (!torpedoes.isEmpty()) {
                var runTorpedos = dodgeTorpedos(torpedoes.get(0));
                if (!runTorpedos) {
                    System.out.println("ADA TORPEDOS TAPI AMAN\n");
                }
                return true;
            }

            var isOffensePossible = fireTeleporter();
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
                    // lain
                    var isAbleToFireTorpedoes = fireTorpedoSalvo();
                    if (!isAbleToFireTorpedoes) {
                        goToFood();
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
        var heading = getHeadingBetween(worldCenter);
        for (int i = 20; i <= 180; i += 20) {
            if (!isHeadingNearDangerousObject(heading)) {
                break;
            }
            heading += i;
        }
        playerAction.setAction(PlayerActions.FORWARD);
        playerAction.setHeading(getHeadingBetween(worldCenter));
    }

    // TELEPORT MECHANISM
    private GameObject getNearestTeleporter() {
        var teleporter = getObjectsWithin(200)
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
                .stream()
                .filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER
                        && Math.abs(getHeadingBetween(item) - item.currentHeading) <= 10)
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
            System.out.println((char) 27 + "[01;32m TELEPORTER EXIST MAP\n" + (char) 27 + "[00;00m");
            this.firedTeleporter = null;
        } else {
            System.out.println("UPDATE TELEPORTER");
            firedTeleporter = teleporterList.get(0);
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
                    .stream()
                    .sorted(Comparator.comparing(item -> item.getSize()))
                    .collect(Collectors.toList());

            for (int i = 0; i < playerList.size(); i++) {
                var dist = getDistanceBetween(playerList.get(i), firedTeleporter);
                System.out.printf("%s SIZE: %d\n", playerList.get(i).getId(), playerList.get(i).getSize());
                System.out.printf("%s DISTANCE: %f\n", playerList.get(i).getId(), dist);
                if (dist <= TELEPORT_SPEED * 3 + (bot.getSize() / 2) && playerList.get(i).getSize() < bot.getSize()) {
                    System.out.println("IT IS");
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 
     * @return true jika terdapat bot lain yang bisa dimakan
     */
    private boolean fireTeleporter() {
        if (this.firedTeleporter != null || this.playerAction.action == PlayerActions.FIRETELEPORT) {
            return false;
        }
        var playerList = gameState.getPlayerGameObjects()
                .stream().sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .toList();
        if (playerList.size() > 0 && bot.teleporterCount > 0) {
            for (int i = 0; i < playerList.size(); i++) {
                var currTarget = playerList.get(i);
                if (!bot.getId().equals(playerList.get(i).getId())
                        && currTarget.getSize() < bot.getSize() - TELEPORT_COST - SAFETY_NUM) {
                    System.out.printf("Target is %s\n", currTarget.getId());
                    playerAction.action = PlayerActions.FIRETELEPORT;
                    playerAction.heading = getHeadingBetween(currTarget);
                    return true;
                }
            }

            return false;
        }
        return false;
    }

    // ATTACK MECHANISM
    private boolean fireTorpedoSalvo() {
        if (bot.torpedoSalvoCount <= 0) {
            return false;
        }

        if (bot.getSize() < TORPEDO_COST * 6) {
            return false;
        }

        var playerList = getPlayersWithin(TORPEDO_RADIUS)
                .stream().filter(item -> item.effects < 16)
                .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .collect(Collectors.toList());

        if (playerList.size() <= 1) {
            return false;
        }

        int heading = getHeadingBetween(playerList.get(1));
        playerAction.action = PlayerActions.FIRETORPEDOES;
        playerAction.heading = heading;
        System.out.printf("Fired torpedoeeeees...\n");
        return true;
    }

    private boolean isObjectNearDangerousObject(GameObject obj) {
        List<GameObject> dangerousObj = gameState.getGameObjects()
                .stream().filter(item -> getOuterDistanceBetween(obj, item) <= bot.speed * 2 &&
                        item.getGameObjectType() != ObjectTypes.FOOD &&
                        item.getGameObjectType() != ObjectTypes.ASTEROIDFIELD &&
                        item.getGameObjectType() != ObjectTypes.SUPERFOOD &&
                        item.getGameObjectType() != ObjectTypes.SUPERNOVAPICKUP)
                .toList();
        return !dangerousObj.isEmpty() || getDistanceBoundary(obj) <= bot.speed + bot.size;
    }

    private boolean isHeadingNearDangerousObject(int heading) {
        List<GameObject> dangerousObj = gameState.getGameObjects()
                .stream().filter(item -> getDistanceBetween(item) <= bot.speed + bot.size &&
                        Math.abs(getHeadingBetween(item) - heading) <= 40 &&
                        item.getGameObjectType() != ObjectTypes.FOOD &&
                        item.getGameObjectType() != ObjectTypes.ASTEROIDFIELD &&
                        item.getGameObjectType() != ObjectTypes.SUPERFOOD &&
                        item.getGameObjectType() != ObjectTypes.SUPERNOVAPICKUP)
                .toList();

        List<GameObject> dangerousPlayer = gameState.getGameObjects()
                .stream().filter(item -> getDistanceBetween(item) <= bot.speed + bot.size &&
                        Math.abs(getHeadingBetween(item) - heading) <= 60 &&
                        item.size > bot.size)
                .toList();
        return !dangerousObj.isEmpty() || !dangerousPlayer.isEmpty();
    }

    private GameObject getVunerableNearPlayer() {
        List<GameObject> playerNearBot = getPlayersWithin(bot.speed * 2)
                .stream().sorted(Comparator.comparing(item -> item.getSize()))
                .toList();
        if (playerNearBot.size() <= 1) {
            return null;
        }

        GameObject vulnerablePlayer = playerNearBot.get(0);
        if (vulnerablePlayer.getSize() < 0.6 * bot.getSize()) {
            return vulnerablePlayer;
        }

        return null;
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
        // System.out.println("Compute Food Safe...\n");
        var listGas = getGasCloudWithin(getObjectsWithin(GASCLOUD_RADIUS));
        // System.out.println("list gas...\n");
        System.out.println(listGas);
        var listAst = getAsteroidWithin(getObjectsWithin(ASTEROID_RADIUS));
        // System.out.println("list asteroid...\n");
        System.out.println(listAst);
        if (listAst.isEmpty() && listGas.isEmpty() && getDistanceBoundary(bot) > BOUNDRY_RADIUS) {
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
                    .stream()
                    .filter(item -> item.getGameObjectType() == ObjectTypes.FOOD
                            && getOuterDistanceBetween(bot, item) > 10) // MASIH KIRA2 ALIAS BLM AMAN
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

        if (!hasil.isEmpty()) {
            System.out.println("ADA SUPERFOOD...\n");
            playerAction.action = PlayerActions.FORWARD;
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
        return getObjectsWithin(radius).stream().filter(item -> item.getGameObjectType() == type)
                .collect(Collectors.toList());
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

    private GameObject getSupernova() {
        // System.out.println("Cek Supernova...\n");
        var supernova = getObjectsWithin(bot.speed * 4).stream()
                .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVAPICKUP
                        && !isObjectNearDangerousObject(item))
                .collect(Collectors.toList());

        if (supernova.isEmpty()) {
            return null;
        }
        return supernova.get(0);
    }

    private List<GameObject> getSupernovaBomb() {
        var superbomb = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVABOMB)
                .collect(Collectors.toList());

        if (!superbomb.isEmpty()) {
            return superbomb;
        }

        return superbomb;
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream()
                .filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
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

    private int getDistanceBoundary(GameObject obj) {
        var distanceFromOrigin = (int) Math
                .ceil(Math.sqrt(obj.getPosition().x * obj.getPosition().x + obj.getPosition().y * obj.getPosition().y));

        if (gameState.world.getRadius() != null) {
            return gameState.world.getRadius() - distanceFromOrigin - obj.getSize();
        }

        return 123;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private double getDistanceBetween(GameObject go) {
        return getDistanceBetween(bot, go);
    }

    private int getOppositeDirection(GameObject gameObject1, GameObject gameObject2) {
        return toDegrees(Math.atan2(gameObject2.position.y - gameObject1.position.y,
                gameObject2.position.x - gameObject1.position.y));
    }

    // AVOIDING OTHER PLAYER
    private GameObject getDangerousNearPlayer() {
        List<GameObject> playerNearBot = getPlayersWithin(100)
                .stream().filter(item -> !bot.getId().equals(item.getId()))
                .sorted(Comparator.comparing(item -> item.getSize()))
                .toList();

        if (playerNearBot.size() == 0) {
            return null;
        }

        GameObject biggestPlayer = playerNearBot.get(playerNearBot.size() - 1);
        if (biggestPlayer.getSize() > bot.getSize()) {
            return biggestPlayer;
        }

        return null;
    }

    private void runFromAtt(GameObject atkr) {
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = getOppositeDirection(bot, atkr);
    }

    private boolean dodgeObj(GameObject obj, int radius) {
        var headingObj = obj.currentHeading;

        if ((headingObj - getOppositeDirection(bot, obj) % 360 >= 0
                && headingObj - getOppositeDirection(bot, obj) % 360 < 60)) {
            if (getDistanceBetween(obj) < radius) {
                System.out.println((char) 27 + "[01;32m LARI-LARI-LARI-LARI\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m AMAN DARI SERANGAN\n" + (char) 27 + "[00;00m");
                return true;
            }
        } else if ((headingObj - getOppositeDirection(bot, obj) % 360 < 0
                && headingObj - getOppositeDirection(bot, obj) % 360 > -60)) {
            if (getDistanceBetween(obj) < radius) {
                System.out.println((char) 27 + "[01;31m LARI-LARI-LARI-LARI\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;32m AMAN DARI\n" + (char) 27 + "[00;00m");
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean dodgeTorpedos(GameObject obj) {
        var headingObj = obj.currentHeading;

        if ((headingObj - getOppositeDirection(bot, obj) % 360 > 5
                && headingObj - getOppositeDirection(bot, obj) % 360 < 30)) {
            if (bot.getSize() > 40 && getDistanceBetween(obj) < TORPEDODODGE_R) {
                System.out.println((char) 27 + "[01;32m USESHIELD\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.ACTIVATESHIELD;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(bot, obj) - 90 + headingObj) % 360;
                return true;
            }
        } else if ((headingObj - getOppositeDirection(bot, obj) % 360 < -5
                && headingObj - getOppositeDirection(bot, obj) % 360 > -30)) {
            if (bot.getSize() > 40 && getDistanceBetween(obj) < TORPEDODODGE_R) {
                System.out.println((char) 27 + "[01;32m USESHIELD\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.ACTIVATESHIELD;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) % 360;
                return true;
            }
        } else if ((headingObj - getOppositeDirection(bot, obj) % 360 <= 5
                && headingObj - getOppositeDirection(bot, obj) % 360 >= -5)) {
            if (bot.getSize() > TORPEDO_COST * 5) {
                System.out.println((char) 27 + "[01;32m TEMBAK BALIK TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FIRETORPEDOES;
                playerAction.heading = (360 - getOppositeDirection(bot, obj)) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(bot, obj) + 90 - headingObj) % 360;
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isObjHeadingUs(GameObject obj) {
        var headingObj = obj.currentHeading;
        return (((headingObj - getOppositeDirection(bot, obj) % 360 >= 0
                && headingObj - getOppositeDirection(bot, obj) % 360 < 60)))
                || (((headingObj - getOppositeDirection(bot, obj) % 360 < 0
                        && headingObj - getOppositeDirection(bot, obj) % 360 > -60)));
    }

    // ESCAPING FROM GAS CLOUD
    private GameObject getGasCloudInPath() {
        List<GameObject> gcList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD &&
                        getOuterDistanceBetween(item, bot) <= bot.speed * 3)
                .toList();
        if (gcList.size() > 0) {
            return gcList.get(0);
        } else {
            return null;
        }
    }

    private void getEffects(int hashCode) {
        if (hashCode == 0) {
            /* DO NOTHING */
        } else {
            if (hashCode % 2 == 1) {
                currentEffect.add(Effects.AFTERBURNER);
                getEffects(hashCode -= 1);
            }

            if (hashCode >= 16) {
                currentEffect.add(Effects.SHIELD);
                getEffects(hashCode -= 16);
            }

            if (hashCode >= 8) {
                currentEffect.add(Effects.SUPERFOOD);
                getEffects(hashCode -= 8);
            }

            if (hashCode >= 4) {
                currentEffect.add(Effects.GASCLOUD);
                getEffects(hashCode -= 4);
            }

            if (hashCode >= 2) {
                currentEffect.add(Effects.ASTEROIDFIELD);
                getEffects(hashCode -= 2);
            }
        }
    }

}
