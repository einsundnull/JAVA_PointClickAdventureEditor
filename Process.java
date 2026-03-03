package main;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a process that executes when an action is performed on an item.
 * Contains conditions to check and actions to execute.
 */
public class Process {
    private String processName;
    private String description;
    private Map<String, Boolean> conditions; // IF conditions
    private List<ProcessAction> actions; // THEN actions

    public Process(String processName) {
        this.processName = processName;
        this.description = "";
        this.conditions = new HashMap<>();
        this.actions = new ArrayList<>();
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Boolean> getConditions() {
        return conditions;
    }

    public void addCondition(String conditionName, boolean expectedValue) {
        this.conditions.put(conditionName, expectedValue);
    }

    public List<ProcessAction> getActions() {
        return actions;
    }

    public void addAction(ProcessAction action) {
        this.actions.add(action);
    }

    /**
     * Check if all conditions are met
     */
    public boolean checkConditions(GameProgress gameProgress) {
        if (conditions.isEmpty()) {
            return true; // No conditions = always execute
        }

        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            String conditionName = entry.getKey();
            boolean expectedValue = entry.getValue();

            // Check condition via Conditions class
            boolean actualValue = Conditions.getCondition(conditionName);

            if (actualValue != expectedValue) {
                System.out.println("Process '" + processName + "' condition not met: " +
                                 conditionName + " (expected: " + expectedValue + ", actual: " + actualValue + ")");
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "Process{name='" + processName + "', conditions=" + conditions.size() +
               ", actions=" + actions.size() + "}";
    }

    /**
     * Base class for all process actions
     */
    public static abstract class ProcessAction {
        protected String actionType;
        protected Map<String, String> parameters;

        public ProcessAction(String actionType) {
            this.actionType = actionType;
            this.parameters = new HashMap<>();
        }

        public String getActionType() {
            return actionType;
        }

        public void setParameter(String key, String value) {
            parameters.put(key, value);
        }

        public String getParameter(String key) {
            return parameters.get(key);
        }

        public String getParameter(String key, String defaultValue) {
            return parameters.getOrDefault(key, defaultValue);
        }

        public boolean getBooleanParameter(String key, boolean defaultValue) {
            String value = parameters.get(key);
            if (value == null) return defaultValue;
            return Boolean.parseBoolean(value);
        }

        public int getIntParameter(String key, int defaultValue) {
            String value = parameters.get(key);
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public double getDoubleParameter(String key, double defaultValue) {
            String value = parameters.get(key);
            if (value == null) return defaultValue;
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        /**
         * Get all parameters
         */
        public Map<String, String> getParameters() {
            return parameters;
        }

        /**
         * Execute this action. Returns true if execution should block (wait for completion).
         */
        public abstract boolean execute(AdventureGame game);

        /**
         * Check if this action has completed (for blocking actions)
         */
        public boolean isComplete() {
            return true; // Most actions complete instantly
        }
    }

    /**
     * Movement action - moves character to position
     */
    public static class MovementAction extends ProcessAction {
        private AdventureGame gameRef;

        public MovementAction() {
            super("Movement");
        }

        @Override
        public boolean execute(AdventureGame game) {
            this.gameRef = game;
            String moveType = getParameter("type", "GoTo");
            boolean waitForCompletion = getBooleanParameter("waitForCompletion", true);

            if (moveType.equals("GoTo")) {
                int x = getIntParameter("x", 0);
                int y = getIntParameter("y", 0);
                Point target = new Point(x, y);

                // Find character item
                Item characterItem = findCharacterItem(game);
                if (characterItem != null) {
                    Point targetPosition = CharacterMovement.calculateTargetPosition(
                        characterItem, target, game.getGameProgress()
                    );
                    game.startCharacterMovementProcess(characterItem, targetPosition);
                    System.out.println("Process: Moving character to (" + x + ", " + y + ")");
                }
            } else if (moveType.equals("GoToItem")) {
                String itemName = getParameter("itemName");
                int offsetX = getIntParameter("offset_x", 0);
                int offsetY = getIntParameter("offset_y", 0);

                // Find target item
                Item targetItem = game.getCurrentScene().getItemByName(itemName);
                if (targetItem != null) {
                    Point itemPos = targetItem.getPosition();
                    Point target = new Point(itemPos.x + offsetX, itemPos.y + offsetY);

                    Item characterItem = findCharacterItem(game);
                    if (characterItem != null) {
                        Point targetPosition = CharacterMovement.calculateTargetPosition(
                            characterItem, target, game.getGameProgress()
                        );
                        game.startCharacterMovementProcess(characterItem, targetPosition);
                        System.out.println("Process: Moving character to item '" + itemName + "'");
                    }
                }
            }

            return waitForCompletion;
        }

        private Item findCharacterItem(AdventureGame game) {
            Scene currentScene = game.getCurrentScene();
            if (currentScene == null) return null;

            for (Item item : currentScene.getItems()) {
                if (item.isFollowingOnMouseClick()) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public boolean isComplete() {
            if (gameRef == null) return true;
            // Check if character is still moving
            return !gameRef.isCharacterMoving();
        }
    }

    /**
     * Conditions action - sets condition values
     */
    public static class ConditionsAction extends ProcessAction {
        public ConditionsAction() {
            super("Conditions");
        }

        @Override
        public boolean execute(AdventureGame game) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String conditionName = entry.getKey();
                boolean value = Boolean.parseBoolean(entry.getValue());

                Conditions.setCondition(conditionName, value);
                System.out.println("Process: Set condition '" + conditionName + "' = " + value);
            }
            return false; // Non-blocking
        }
    }

    /**
     * Dialog action - shows dialog window
     */
    public static class DialogAction extends ProcessAction {
        public DialogAction() {
            super("Dialog");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String fileName = getParameter("file");
            String position = getParameter("position", "center");
            boolean blockInput = getBooleanParameter("blockInput", true);

            System.out.println("Process: Showing dialog '" + fileName + "'");
            // TODO: Implement dialog system
            // game.showDialog(fileName, position, blockInput);

            return true; // Blocking - waits for user to close dialog
        }
    }

    /**
     * Text action - displays text on screen
     */
    public static class TextAction extends ProcessAction {
        public TextAction() {
            super("Text");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String message = getParameter("message", "");
            int duration = getIntParameter("duration", 2000);
            String position = getParameter("position", "center");
            String colorStr = getParameter("color", "255,255,255");

            System.out.println("Process: Displaying text '" + message + "' for " + duration + "ms");
            // TODO: Implement text display system
            // game.displayText(message, duration, position, parseColor(colorStr));

            return false; // Non-blocking
        }
    }

    /**
     * SceneChange action - switches to different scene
     */
    public static class SceneChangeAction extends ProcessAction {
        public SceneChangeAction() {
            super("SceneChange");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String sceneName = getParameter("sceneName");
            String subSceneName = getParameter("subSceneName");
            String spawnPoint = getParameter("spawnPoint", "");
            String transition = getParameter("transition", "fade");
            int transitionDuration = getIntParameter("transitionDuration", 500);

            System.out.println("Process: Changing scene to '" + sceneName + "/" + subSceneName + "'");
            // TODO: Implement scene change
            // game.changeScene(sceneName, subSceneName, spawnPoint, transition, transitionDuration);

            return true; // Blocking
        }
    }

    /**
     * Sound action - plays sound effect
     */
    public static class SoundAction extends ProcessAction {
        public SoundAction() {
            super("Sound");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String fileName = getParameter("file");
            String type = getParameter("type", "sfx");
            double volume = getDoubleParameter("volume", 1.0);
            boolean loop = getBooleanParameter("loop", false);
            boolean waitForCompletion = getBooleanParameter("waitForCompletion", false);

            System.out.println("Process: Playing sound '" + fileName + "'");
            // TODO: Implement sound system
            // game.playSound(fileName, type, volume, loop);

            return waitForCompletion;
        }
    }

    /**
     * AddToInventory action - adds item to inventory
     */
    public static class AddToInventoryAction extends ProcessAction {
        public AddToInventoryAction() {
            super("AddToInventory");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String itemName = getParameter("itemName");
            boolean showNotification = getBooleanParameter("showNotification", true);
            String notificationText = getParameter("notificationText", itemName + " added to inventory");

            System.out.println("Process: Adding '" + itemName + "' to inventory");

            // Set inventory condition
            String conditionName = "isInInventory_" + itemName;
            Conditions.setCondition(conditionName, true);

            // TODO: Show notification
            // if (showNotification) {
            //     game.showNotification(notificationText);
            // }

            return false; // Non-blocking
        }
    }

    /**
     * RemoveFromInventory action - removes item from inventory
     */
    public static class RemoveFromInventoryAction extends ProcessAction {
        public RemoveFromInventoryAction() {
            super("RemoveFromInventory");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String itemName = getParameter("itemName");
            boolean showNotification = getBooleanParameter("showNotification", false);

            System.out.println("Process: Removing '" + itemName + "' from inventory");

            // Set inventory condition
            String conditionName = "isInInventory_" + itemName;
            Conditions.setCondition(conditionName, false);

            return false; // Non-blocking
        }
    }

    /**
     * Wait action - pauses execution
     */
    public static class WaitAction extends ProcessAction {
        private long startTime;
        private long duration;

        public WaitAction() {
            super("Wait");
        }

        @Override
        public boolean execute(AdventureGame game) {
            duration = getIntParameter("duration", 1000);
            startTime = System.currentTimeMillis();

            System.out.println("Process: Waiting for " + duration + "ms");
            return true; // Blocking
        }

        @Override
        public boolean isComplete() {
            return System.currentTimeMillis() - startTime >= duration;
        }
    }

    /**
     * ItemVisibility action - shows/hides items
     */
    public static class ItemVisibilityAction extends ProcessAction {
        public ItemVisibilityAction() {
            super("ItemVisibility");
        }

        @Override
        public boolean execute(AdventureGame game) {
            String itemName = getParameter("itemName");
            boolean visible = getBooleanParameter("visible", true);
            boolean fade = getBooleanParameter("fade", false);
            int fadeDuration = getIntParameter("fadeDuration", 500);

            System.out.println("Process: Setting item '" + itemName + "' visibility to " + visible);

            Scene currentScene = game.getCurrentScene();
            if (currentScene != null) {
                Item item = currentScene.getItemByName(itemName);
                if (item != null) {
                    item.setVisible(visible);
                    game.repaint();
                }
            }

            return false; // Non-blocking (unless fade is implemented)
        }
    }
}
