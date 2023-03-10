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
    private boolean isTeleporterFired = false;
    private boolean firedSuperbomb;
    private HashSet<Effects> currentEffect = new HashSet<Effects>();
    // ADD MORE CONST IF ANY
    final int SAFETY_NUM = 40;
    final int TELEPORT_COST = 20;
    final int SUPERFOOD_RADIUS = 50;
    final int TORPEDO_RADIUS = 600;
    final int AVOID_TORPEDO_RADIUS = 150;
    final int GASCLOUD_RADIUS = 10;
    final int SUPERNOVABOMB_RADIUS = 200;
    final int TELEPORT_SPEED = 20;
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
    
    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
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

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private double getDistanceBetween(GameObject go) {
        return getDistanceBetween(bot, go);
    }

    /**
        Compute next player action
        @return true jika kalkulasi untuk menentukan aksi selanjutnya 
        telah selesai
    */    
    public boolean computeNextPlayerAction(PlayerAction playerAction) {
        if (playerAction.action == null) {
            playerAction.setAction(PlayerActions.FORWARD);
            playerAction.setHeading(new Random().nextInt(360));
        }        
        
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
            System.out.printf("SHIELD: %d\n", bot.shieldCount);

            if (supernovaList.size() > 0) {
                System.out.printf("DISTANCE FROM SN: %f\n", getDistanceBetween(supernovaList.get(0)));
            }    

            if (gameState.world.centerPoint != null) {
                System.out.printf("X: %d Y: %d\n", gameState.world.centerPoint.x, gameState.world.centerPoint.y);
            }    

            if (this.firedTeleporter == null && isTeleporterFired) {
                getFiredTeleporter();
            }    

            if (this.firedSuperbomb && isSupernovaNearOtherPlayer()) {
                System.out.println("Denotate Supernova Bomb");
                playerAction.setAction(PlayerActions.DETONATESUPERNOVA);
                firedSuperbomb = false;
                return true;
            }    
            
            if (this.firedTeleporter != null && isTeleporterStillAvailable() && isTeleporterNearSmallerEnemy()) {
                playerAction.setAction(PlayerActions.TELEPORT);
                this.firedTeleporter = null;
                this.isTeleporterFired = false;
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
                var isAbleToFireSuperbomb = fireSuperbomb();
                if (isAbleToFireSuperbomb) {
                    return true;
                }    
            }    

            var isOffensePossible = fireTeleporter();
            if (isOffensePossible) {
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
            
            if (distanceFromBoundary <= 20) {
                System.out.println("TOO CLOSE TO BOUNDARY...MOVING TO CENTER...\n");
                moveToCenter();
                return true;
            }    

            var nearestTeleporter = getNearestTeleporter();
            if (nearestTeleporter != null) {
                System.out.println("Avoiding teleporter");
                dodgeObj(nearestTeleporter);
                return true;
            }

            GameObject gasCloud = getGasCloudInPath();
            if (gasCloud != null && distanceFromBoundary <= bot.speed + 20) {
                System.out.println("Avoid Gas Cloud");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = getOppositeDirection(gasCloud);
                return true;
            }    

            List<GameObject> torpedoes = getObjectsWithin(AVOID_TORPEDO_RADIUS, ObjectTypes.TORPEDOSALVO)
                    .stream().filter(item -> isObjHeadingUs(item))
                    .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                    .toList();
            if (!torpedoes.isEmpty()) {
                System.out.println("Avoiding torpedoes");
                dodgeTorpedos(torpedoes.get(0));
                return true;
            }    
            
            if (!superbombList.isEmpty()) {
                dodgeObj(superbomb);
                System.out.println("Avoiding superbomb");
                return true;
            }
            
            var isAbleToFireTorpedoes = fireTorpedoSalvo();
            if (isAbleToFireTorpedoes) {
                return true;
            }    

            GameObject vulnerablePlayer = getVulnerableNearPlayer();
            if (vulnerablePlayer != null) {
                System.out.println("CHASING VULNERABLE PLAYER");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = getHeadingBetween(vulnerablePlayer);
                return true;
            } 
            
            goToFood();
        }    
        currentEffect.clear();
        this.playerAction = playerAction;
        return true;
    }    

    /**
     * Mengeset heading menuju world center game
     */
    private void moveToCenter() {
        Position position = new Position(0, 0);
        GameObject worldCenter = new GameObject(bot.getId(), 0, 0, 0, position, 
            ObjectTypes.FOOD, 0, 0, 0, 0, 0);
        var heading = getHeadingBetween(worldCenter);
        for (int i = 20; i <= 340; i += 20) {
            if (!isHeadingNearDangerousObject(heading)) {
                break;
            }
            System.out.println("Heading is dangerous, changing heading");
            heading += i;
        }
        playerAction.setAction(PlayerActions.FORWARD);
        playerAction.setHeading(heading % 360);
    }

    /**
     * TELEPORT MECHANISM
     * @return Mendapatkan object teleporter terdekat
     */
    private GameObject getNearestTeleporter() {
        var teleporter = getObjectsWithin(400)
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && isObjHeadingUs(item))
                .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .collect(Collectors.toList());
        if (!teleporter.isEmpty()) {
            return teleporter.get(0);
        }
        return null;
    }

    /**
     * Mendapatkan teleporter yang baru saja ditembak
     */
    private void getFiredTeleporter() {
        var teleporterList = gameState.getGameObjects()
                .stream()
                .filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER
                        && Math.abs(getHeadingBetween(item) - item.currentHeading) <= 10)
                .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .collect(Collectors.toList());
        if (teleporterList.size() != 0) {
            this.firedTeleporter = teleporterList.get(0);
        }
    }

    /**
     * @return True jika teleporter yang sudah ditembak masih ada di dunia
     */
    private boolean isTeleporterStillAvailable() {
        var teleporterList = gameState.getGameObjects()
                .stream().filter(item -> item.getId().equals(firedTeleporter.getId()))
                .collect(Collectors.toList());

        if (teleporterList.isEmpty()) {
            System.out.println((char) 27 + "[01;32m TELEPORTER EXIST MAP\n" + (char) 27 + "[00;00m");
            this.firedTeleporter = null;
            this.isTeleporterFired = false;
        } else {
            System.out.println("UPDATE TELEPORTER");
            firedTeleporter = teleporterList.get(0);
        }
        return !teleporterList.isEmpty();
    }

    /**
     * @return True jika teleporter yang ditembak sedang berada
     *         di dekat musuh yang lebih kecil
     */
    private boolean     isTeleporterNearSmallerEnemy() {
        if (this.firedTeleporter == null) {
            System.out.println("TELEPORTER IS NULL");
            this.isTeleporterFired = false;
            return false;
        } else {
            System.out.println("IS TELEPORT NEAR SMALLER ENEMY?");
            var playerList = gameState.getPlayerGameObjects()
                    .stream()
                    .sorted(Comparator.comparing(item -> item.getSize()))
                    .collect(Collectors.toList());
            for (int i = 0; i < playerList.size(); i++) {
                var dist = getOuterDistanceBetween(playerList.get(i), firedTeleporter);
                System.out.printf("%s SIZE: %d\n", playerList.get(i).getId(), playerList.get(i).getSize());
                System.out.printf("%s DISTANCE: %f\n", playerList.get(i).getId(), dist);
                if (dist <= TELEPORT_SPEED * 3 + (bot.getSize() - 20)
                        && playerList.get(i).getSize() + 10 < bot.getSize()) {
                    System.out.println("IT IS");
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @return true jika terdapat bot lain yang bisa dimakan
     *         dengan menggunakan teleporter
     */
    private boolean fireTeleporter() {
        if (this.isTeleporterFired || this.firedTeleporter != null) {
            return false;
        }
        var playerList = gameState.getPlayerGameObjects()
                .stream().filter(item -> !isObjectNearDangerousObject(item))
                .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .toList();
        if (playerList.size() > 0 && bot.teleporterCount > 0) {
            for (int i = 0; i < playerList.size(); i++) {
                var currTarget = playerList.get(i);
                if (!bot.getId().equals(playerList.get(i).getId())
                        && currTarget.getSize() < bot.getSize() - TELEPORT_COST - SAFETY_NUM) {
                    System.out.printf("Target is %s\n", currTarget.getId());
                    playerAction.action = PlayerActions.FIRETELEPORT;
                    playerAction.heading = getHeadingBetween(currTarget);
                    this.isTeleporterFired = true;
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * ATTACK MECHANISM
     * @return true jika Supernova berhasil ditembakkan
     */
    private boolean fireSuperbomb() {
        List<GameObject> playerList = gameState.getPlayerGameObjects()
                .stream().filter(item -> !item.getId().equals(bot.getId()) &&
                        getDistanceBetween(item) >= 250)
                .sorted(Comparator.comparing(item -> item.getSize()))
                .toList();

        if (playerList.isEmpty()) {
            return false;
        }
        playerAction.setAction(PlayerActions.FIRESUPERNOVA);
        playerAction.setHeading(getHeadingBetween(playerList.get(playerList.size() - 1)));
        this.firedSuperbomb = true;
        return true;
    }

    /**
     * @return true jika supernova berhasil diledakkan
    */
    private boolean isSupernovaNearOtherPlayer() {
        var supernovaBombList = getSupernovaBomb();
        if (supernovaBombList.isEmpty()) {
            System.out.println("Supernova Exist Map");
            return false;
        }
        List<GameObject> playerList = gameState.getPlayerGameObjects()
                .stream().filter(item -> !item.getId().equals(bot.getId()) &&
                        getDistanceBetween(item) >= 250 &&
                        getDistanceBetween(item, supernovaBombList.get(0)) <= 100)
                .toList();

        if (playerList.isEmpty()) {
            System.out.println("No Player Near Supernova, getting alternate action");
            if (getDistanceBoundary(supernovaBombList.get(0)) <= 150) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Menembakkan torpedo salvo
     * @return true jika bot dapat menembak torpedo salvo
     */
    private boolean fireTorpedoSalvo() {
        int minSize, radius;
        if (bot.torpedoSalvoCount <= 0) {
            return false;
        }

        if (gameState.getPlayerGameObjects().size() == 2) {
            minSize = TORPEDO_COST * 4;
            radius = 200;
        } else {
            minSize = TORPEDO_COST * 6;
            radius = 400;
        }

        if (bot.getSize() < minSize) {
            return false;
        }

        var playerList = getPlayersWithin(radius)
                .stream().filter(item -> item.effects < 16 && !item.getId().equals(bot.getId()) && item.getSize() > 10)
                .sorted(Comparator.comparing(item -> getDistanceBetween(item)))
                .collect(Collectors.toList());

        if (playerList.size() == 0) {
            return false;
        }

        var objectList = getObjectsWithin(radius)
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD ||
                        item.getGameObjectType() == ObjectTypes.ASTEROIDFIELD ||
                        item.getGameObjectType() == ObjectTypes.WORMHOLE)
                .collect(Collectors.toList());

        if (!isObjInBetween(objectList, playerList.get(0))) {
            System.out.println("\n\nTERHALANG OBJEK GAGAL TORPEDO\n\n");
            return false;
        }

        int heading = getHeadingBetween(playerList.get(0));
        playerAction.action = PlayerActions.FIRETORPEDOES;
        playerAction.heading = heading;
        System.out.printf("Fired torpedoeeeees...\n");
        return true;
    }

    /**
     * @param obj Object yang akan dicek
     * @return true jika obj dekat dengan object yang berbahaya
     */
    private boolean isObjectNearDangerousObject(GameObject obj) {
        if (gameState.getPlayerGameObjects().size() == 2) {
            return false;
        }
        List<GameObject> dangerousObj = gameState.getGameObjects()
                .stream().filter(item -> getOuterDistanceBetween(obj, item) <= 25 &&
                        item.getGameObjectType() != ObjectTypes.FOOD &&
                        item.getGameObjectType() != ObjectTypes.ASTEROIDFIELD &&
                        item.getGameObjectType() != ObjectTypes.SUPERFOOD &&
                        item.getGameObjectType() != ObjectTypes.SUPERNOVAPICKUP)
                .toList();
        return !dangerousObj.isEmpty() || getDistanceBoundary(obj) <= 25;
    }

    /** 
     * @param heading Heading yang akan dicek
     * @return true jika heading menuju object yang berbahaya
     */
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

    /**
     * @return player terdekat yang dapat kita makan,
     *         null jika tidak ada
     */
    private GameObject getVulnerableNearPlayer() {
        List<GameObject> playerNearBot = getPlayersWithin(bot.speed * 8)
                .stream().sorted(Comparator.comparing(item -> getDistanceBetween(item)))
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

    /**
     * Bergerak menuju superfood dan food terdekat
     */
    private void goToFood() {
        var cekSuperfood = getSuperfood(getObjectsWithin(SUPERFOOD_RADIUS));
        if (!cekSuperfood) {
            var status = computeFoodTarget();
            if (!status) {
                System.out.println("Can't do anything");
                playerAction.setAction(PlayerActions.STOP);
            } else {
                System.out.println("Going for nearest food");
            }
        }
    }

    /**
     * Mendapatkan makanan terdekat
     * @return true jika terdapat makanan terdekat
     *         yang dapat dimakan
     */
    private boolean computeFoodTarget() {
        var foodList = gameState.getGameObjects()
                .stream()
                .filter(item -> item.getGameObjectType() == ObjectTypes.FOOD && !isObjectNearDangerousObject(item))
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());
        if (foodList.isEmpty()) {
            return false;
        }
        var shortedDistance = (int) getDistanceBetween(foodList.get(0));

        var shortedFoodList = foodList.stream().filter(item -> ((int) getDistanceBetween(item)) == shortedDistance)
                .sorted(Comparator
                        .comparing(item -> item.getId()))
                .toList();

        if (shortedFoodList.isEmpty()) {
            return false;
        }
        System.out.printf("Chasing %s with distance %d\n", shortedFoodList.get(0).getId(),
                (int) getDistanceBetween(shortedFoodList.get(0)));
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = getHeadingBetween(shortedFoodList.get(0));
        return true;
    }

    /**
     * Mendapatkan super food terdekat
     * @param object
     * @return true jika terdapat superfood pada object
     */
    private boolean getSuperfood(List<GameObject> object) {
        var hasil = object.stream()
                .filter(item -> item.getGameObjectType() == ObjectTypes.SUPERFOOD && !isObjectNearDangerousObject(item))
                .collect(Collectors.toList());
        if (!hasil.isEmpty()) {
            System.out.println("ADA SUPERFOOD...\n");
            playerAction.action = PlayerActions.FORWARD;
            playerAction.heading = getHeadingBetween(hasil.get(0));
            return true;
        }
        return false;
    }

    /**
     * @param radius
     * @return List dari game objects yang ada di dalam radius
     */
    private List<GameObject> getObjectsWithin(int radius) {
        var objList = gameState.getGameObjects()
                .stream().filter(item -> getOuterDistanceBetween(bot, item) <= radius)
                .collect(Collectors.toList());
        return objList;
    }

    /**
     * @param radius
     * @param type
     * @return List dari game objects dalam radius dengan tipe object type
     */
    private List<GameObject> getObjectsWithin(int radius, ObjectTypes type) {
        return getObjectsWithin(radius).stream().filter(item -> item.getGameObjectType() == type)
                .collect(Collectors.toList());
    }

    /**
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

    /**
     * @return Supernova pickup, null jika tidak ada
     */
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

    /**
     * @return List of supernova bomb
     */
    private List<GameObject> getSupernovaBomb() {
        var superbomb = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVABOMB)
                .collect(Collectors.toList());
        if (!superbomb.isEmpty()) {
            return superbomb;
        }
        return superbomb;
    }

    /** 
     * @param object1
     * @param object2
     * @return Jarak object1 dan object2 dari sisi terluar
     */
    private double getOuterDistanceBetween(GameObject object1, GameObject object2) {
        return getDistanceBetween(object1, object2) - object1.getSize() - object2.getSize();
    }

    /**
     * @param otherObject
     * @return arah player/bot menuju otherobject
     */
    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    /**
     * @param obj
     * @return jarak dari obj ke boundary (batas)
     */
    private int getDistanceBoundary(GameObject obj) {
        var distanceFromOrigin = (int) Math
                .ceil(Math.sqrt(obj.getPosition().x * obj.getPosition().x + obj.getPosition().y * obj.getPosition().y));

        if (gameState.world.getRadius() != null) {
            return gameState.world.getRadius() - distanceFromOrigin - obj.getSize();
        }
        return 123;
    }

    /**
     * @param gameObject1
     * @param gameObject2
     * @return lawan arah player/bot menuju object
     */
    private int getOppositeDirection(GameObject obj) {
        return (180 + getHeadingBetween(obj)) % 360;
    }

    // AVOIDING OTHER PLAYER
    /**
     * @return player lain yang berpotensi menimbulkan
     *         masalah untuk kita, yaitu memiliki size lebih besar
     *         dan jaraknya dekat
     */
    private GameObject getDangerousNearPlayer() {
        int radius = 160;
        if (gameState.getPlayerGameObjects().size() == 2) {
            radius = 80;
        }
        List<GameObject> playerNearBot = getPlayersWithin(radius)
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

    /**
     * Berlari dari musuh atkr
     * Pengecekan apakah musuh berbahaya ada di fungsi utama
     * @param atkr musuh yang menyerang kita,
     */
    private void runFromAtt(GameObject atkr) {
        playerAction.action = PlayerActions.FORWARD;
        Position position = new Position(0, 0);
        GameObject worldCenter = new GameObject(bot.getId(), 
            0, 0, 0, position, ObjectTypes.FOOD, 
                0, 0, 0, 0, 0);
        var distanceFromBoundary = getDistanceBoundary(bot);
        if (distanceFromBoundary <= 20) {
            if (getHeadingBetween(atkr) < 90 || getHeadingBetween(atkr) > 270) {
                playerAction.heading = (getHeadingBetween(worldCenter) - 90) % 360;
            } else {
                playerAction.heading = (getHeadingBetween(worldCenter) + 90) % 360;
            }
        } else {
            playerAction.heading = getOppositeDirection(atkr);
        }
    }

    /**
     * @param obj
     * @return Menghindar dari obj
     */
    private void dodgeObj(GameObject obj) {
        System.out.println((char) 27 + "[01;32m LARI-LARI-LARI-LARI\n" + (char) 27 + "[00;00m");
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = (getOppositeDirection(obj) - 90) % 360;
    }

    /**
     * @param obj
     * @return
     */
    private boolean dodgeTorpedos(GameObject obj) {
        var headingObj = obj.currentHeading;
        if ((headingObj - getOppositeDirection(obj) % 360 > 5
                && headingObj - getOppositeDirection(obj) % 360 < 30)) {
            if (bot.getSize() > 40 && bot.shieldCount > 0) {
                System.out.println((char) 27 + "[01;32m USESHIELD\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.ACTIVATESHIELD;
                playerAction.heading = (getOppositeDirection(obj) - 90 + headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(obj) - 90 + headingObj) % 360;
                return true;
            }
        } else if ((headingObj - getOppositeDirection(obj) % 360 < -5
                && headingObj - getOppositeDirection(obj) % 360 > -30)) {
            if (bot.getSize() > 40 && bot.shieldCount > 0) {
                System.out.println((char) 27 + "[01;32m USESHIELD\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.ACTIVATESHIELD;
                playerAction.heading = (getOppositeDirection(obj) + 90 - headingObj) % 360;
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(obj) + 90 - headingObj) % 360;
                return true;
            }
        } else if ((headingObj - getOppositeDirection(obj) % 360 <= 5
                && headingObj - getOppositeDirection(obj) % 360 >= -5)) {
            if (bot.getSize() > TORPEDO_COST * 4) {
                System.out.println((char) 27 + "[01;32m TEMBAK BALIK TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FIRETORPEDOES;
                playerAction.heading = getHeadingBetween(obj);
                return true;
            } else {
                System.out.println((char) 27 + "[01;31m LARI DARI TORPEDOS\n" + (char) 27 + "[00;00m");
                playerAction.action = PlayerActions.FORWARD;
                playerAction.heading = (getOppositeDirection(obj) + 90 - headingObj) % 360;
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * @param obj
     * @return true jika object mengarah ke kita
     */
    private boolean isObjHeadingUs(GameObject obj) {
        var headingObj = obj.currentHeading;
        return (((headingObj - getOppositeDirection(obj) % 360 >= 0
                && headingObj - getOppositeDirection(obj) % 360 < 60)))
                || (((headingObj - getOppositeDirection(obj) % 360 < 0
                        && headingObj - getOppositeDirection(obj) % 360 > -60)));
    }

    /**
     * ESCAPING FROM GAS CLOUD
     * @return mengembalikan object gas cloud terdekat
     *         null jika tidak ada
     */
    private GameObject getGasCloudInPath() {
        List<GameObject> gcList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD &&
                        getOuterDistanceBetween(item, bot) <= 80)
                .toList();
        if (gcList.size() > 0) {
            return gcList.get(0);
        } else {
            return null;
        }
    }

    /**
     * Mengambil efek yang dimiliki bot kita
     * @param hashCode kode hash dari efek
     */
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

    /**
     * @param LG List GameObject
     * @param Player OtherPlayer
     * @return true jika ada object lain diantara bot dengan otherPlayer
     */
    private boolean isObjInBetween(List<GameObject> LG, GameObject Player){
        boolean ret = true;
        for (int i = 0; i < LG.size(); i++) {
            double a = getDistanceBetween(bot, LG.get(i));
            double b = getDistanceBetween(bot, Player);
            double c = getDistanceBetween(LG.get(i), Player);
            if (c > b) {
                continue;
            } else {
                double d = a * (Math.sin(Math.acos((a * a + b * b - c * c) / (2 * a * b))));
                if (d < LG.get(i).getSize()) {
                    return false;
                }
            }
        }
        return ret;
    }
}
