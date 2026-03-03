package main;

/**
 * Represents an item in the Scenes ListView - can be either a Scene or SubScene
 */
public class SceneListItem {
    public enum Type {
        SCENE,
        SUBSCENE
    }

    private Type type;
    private String name;
    private Scene sceneObject; // The actual Scene object
    private boolean isExpanded; // Only for Scene type
    private String parentSceneName; // Only for SubScene type

    // Scene constructor
    public SceneListItem(String name, Scene sceneObject) {
        this.type = Type.SCENE;
        this.name = name;
        this.sceneObject = sceneObject;
        this.isExpanded = true; // Default expanded
        this.parentSceneName = null;
    }

    // SubScene constructor
    public SceneListItem(String name, Scene sceneObject, String parentSceneName) {
        this.type = Type.SUBSCENE;
        this.name = name;
        this.sceneObject = sceneObject;
        this.isExpanded = false; // SubScenes don't have expand state
        this.parentSceneName = parentSceneName;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Scene getSceneObject() {
        return sceneObject;
    }

    public void setSceneObject(Scene sceneObject) {
        this.sceneObject = sceneObject;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public String getParentSceneName() {
        return parentSceneName;
    }

    public void setParentSceneName(String parentSceneName) {
        this.parentSceneName = parentSceneName;
    }

    public boolean isScene() {
        return type == Type.SCENE;
    }

    public boolean isSubScene() {
        return type == Type.SUBSCENE;
    }

    /**
     * Gets the thumbnail path for this scene/subscene
     */
    public String getThumbnailPath() {
        if (sceneObject == null) {
            return null;
        }

        // Get background image path from scene
        return getBackgroundImagePath(sceneObject);
    }

    private String getBackgroundImagePath(Scene scene) {
        if (scene.getBackgroundImages() != null && !scene.getBackgroundImages().isEmpty()) {
            return scene.getBackgroundImages().get(0).getImagePath();
        }
        return scene.getBackgroundImagePath();
    }

    @Override
    public String toString() {
        return name;
    }
}
