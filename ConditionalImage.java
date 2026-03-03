package main;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an image with conditions that determine when it should be displayed.
 */
public class ConditionalImage {
    private String imagePath;
    private String name;
    private Map<String, Boolean> conditions; // condition name -> required value
    private boolean showIfTrue; // true = show when conditions match, false = show when conditions don't match
    private boolean flipHorizontally; // true = flip image horizontally
    private boolean flipVertically; // true = flip image vertically

    public ConditionalImage() {
        this.imagePath = "";
        this.name = "Image";
        this.conditions = new LinkedHashMap<>();
        this.showIfTrue = true;
        this.flipHorizontally = false;
        this.flipVertically = false;
    }

    public ConditionalImage(String imagePath, String name) {
        this.imagePath = imagePath;
        this.name = name;
        this.conditions = new LinkedHashMap<>();
        this.showIfTrue = true;
        this.flipHorizontally = false;
        this.flipVertically = false;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Boolean> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(String conditionName, boolean requiredValue) {
        this.conditions.put(conditionName, requiredValue);
    }

    public void removeCondition(String conditionName) {
        this.conditions.remove(conditionName);
    }

    public boolean isShowIfTrue() {
        return showIfTrue;
    }

    public void setShowIfTrue(boolean showIfTrue) {
        this.showIfTrue = showIfTrue;
    }

    public boolean isFlipHorizontally() {
        return flipHorizontally;
    }

    public void setFlipHorizontally(boolean flipHorizontally) {
        this.flipHorizontally = flipHorizontally;
    }

    public boolean isFlipVertically() {
        return flipVertically;
    }

    public void setFlipVertically(boolean flipVertically) {
        this.flipVertically = flipVertically;
    }

    /**
     * Evaluates whether this image should be displayed based on current game conditions.
     * @param gameProgress The current game progress containing condition states
     * @return true if this image should be displayed, false otherwise
     */
    public boolean shouldDisplay(GameProgress gameProgress) {
        System.out.println("      ConditionalImage.shouldDisplay() for: " + name);
        System.out.println("        showIfTrue: " + showIfTrue);
        System.out.println("        conditions.size: " + conditions.size());

        // If no conditions are set, always display (unless it's a showIfFalse with no conditions)
        if (conditions.isEmpty()) {
            System.out.println("        No conditions, returning showIfTrue: " + showIfTrue);
            return showIfTrue; // Show only if showIfTrue mode
        }

        // Check all conditions
        boolean allConditionsMet = true;
        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            String conditionName = entry.getKey();
            boolean requiredValue = entry.getValue();

            boolean currentValue = Conditions.getCondition(conditionName);

            System.out.println("        Checking: " + conditionName + " (required=" + requiredValue + ", current=" + currentValue + ")");

            if (currentValue != requiredValue) {
                allConditionsMet = false;
                break;
            }
        }

        System.out.println("        allConditionsMet: " + allConditionsMet);

        // Apply showIfTrue/showIfFalse logic
        if (showIfTrue) {
            System.out.println("        Returning allConditionsMet: " + allConditionsMet);
            return allConditionsMet;
        } else {
            System.out.println("        Returning !allConditionsMet: " + !allConditionsMet);
            return !allConditionsMet;
        }
    }

    /**
     * Creates a deep copy of this ConditionalImage.
     * @return A new ConditionalImage with the same properties
     */
    public ConditionalImage copy() {
        ConditionalImage copy = new ConditionalImage(this.imagePath, this.name);
        copy.setShowIfTrue(this.showIfTrue);
        copy.setFlipHorizontally(this.flipHorizontally);
        copy.setFlipVertically(this.flipVertically);
        copy.setConditions(new LinkedHashMap<>(this.conditions));
        return copy;
    }

    @Override
    public String toString() {
        return name + " (" + imagePath + ")";
    }
}
