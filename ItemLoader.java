package main;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Loads items from .txt files in resources/items/
 */
public class ItemLoader {

    public static Item loadItem(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Item file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        Item item = null;
        String currentSection = "";
        String currentSubSection = "";
        String pendingCondition = null;
        List<String> pendingConditions = new ArrayList<>(); // Collect multiple --- lines
        String currentAction = null;
        KeyArea.ActionHandler currentActionHandler = null;
        int clickAreaX = 0;
        int clickAreaY = 0;
        int movingRangeX = 0;
        int movingRangeY= 0;
        int pathX = 0;
        int pathY = 0;
        // For ConditionalImages
        ConditionalImage currentConditionalImage = null;
        boolean inConditionalImageSection = false;
        boolean inConditionsSubSection = false;

        // For CustomClickAreas
        CustomClickArea currentCustomClickArea = null;
        boolean inCustomClickAreaSection = false;
        boolean customClickAreaPointsLoaded = false; // MIGRATION FIX: Track if points were loaded
        int customClickPointX = 0;
        int customClickPointY = 0;

        // For MovingRanges
        MovingRange currentMovingRange = null;
        boolean inMovingRangeSection = false;
        boolean movingRangePointsLoaded = false; // MIGRATION FIX: Track if points were loaded

        Path currentPath = null;
        boolean inPathSection = false;
        boolean pathPointsLoaded = false; // MIGRATION FIX: Track if points were loaded

        // For NEW Actions Format (IF/THEN/Processes)
        List<String> conditionsIF = new ArrayList<>();
        List<String> conditionsTHEN = new ArrayList<>();
        List<String> processes = new ArrayList<>();
        String actionSubSection = ""; // "IF", "THEN", or "PROCESSES"
        boolean legacyCustomAreaInitialized = false;
        boolean legacyMovingRangeInitialized = false;
        boolean legacyPathInitialized = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("//")) {
                continue; // Skip empty lines and comments
            }

            // Finalize pending objects when hitting a new section (starts with #)
            if (line.startsWith("#")) {
                // Finalize CustomClickArea if in progress
                if (inCustomClickAreaSection && currentCustomClickArea != null) {
                    currentCustomClickArea.updatePolygon();
                    System.out.println("ItemLoader: Finalized CustomClickArea with " + currentCustomClickArea.getPoints().size() + " points");
                    currentCustomClickArea = null;
                    inCustomClickAreaSection = false;
                    legacyCustomAreaInitialized = false;
                }
                // Finalize MovingRange if in progress
                if (inMovingRangeSection && currentMovingRange != null) {
                    System.out.println("ItemLoader: Finalized MovingRange with " + currentMovingRange.getPoints().size() + " points");
                    currentMovingRange = null;
                    inMovingRangeSection = false;
                    legacyMovingRangeInitialized = false;
                }
                // Finalize Path if in progress
                if (inPathSection && currentPath != null) {
                    System.out.println("ItemLoader: Finalized Path with " + currentPath.getPoints().size() + " points");
                    currentPath = null;
                    inPathSection = false;
                    legacyPathInitialized = false;
                }
            }

            // Section headers
            if (line.startsWith("#Name:")) {
                currentSection = "NAME";
            } else if (line.startsWith("#ImageFile:")) {
                currentSection = "IMAGEFILE";
            } else if (line.startsWith("#ImagePath:")) {
                currentSection = "IMAGEPATH";
            } else if (line.startsWith("#ImagePathTopLeft:")) {
                currentSection = "IMAGEPATHTOPLEFT";
            } else if (line.startsWith("#ImagePathTop:")) {
                currentSection = "IMAGEPATHTOP";
            } else if (line.startsWith("#ImagePathTopRight:")) {
                currentSection = "IMAGEPATHTOPRIGHT";
            } else if (line.startsWith("#ImagePathLeft:")) {
                currentSection = "IMAGEPATHLEFT";
            } else if (line.startsWith("#ImagePathMiddle:")) {
                currentSection = "IMAGEPATHMIDDLE";
            } else if (line.startsWith("#ImagePathRight:")) {
                currentSection = "IMAGEPATHRIGHT";
            } else if (line.startsWith("#ImagePathBottomLeft:")) {
                currentSection = "IMAGEPATHBOTTOMLEFT";
            } else if (line.startsWith("#ImagePathBottom:")) {
                currentSection = "IMAGEPATHBOTTOM";
            } else if (line.startsWith("#ImagePathBottomRight:")) {
                currentSection = "IMAGEPATHBOTTOMRIGHT";
            } else if (line.startsWith("#Position:")) {
                currentSection = "POSITION";
            } else if (line.startsWith("#Size:")) {
                currentSection = "SIZE";
            } else if (line.startsWith("#IsInInventory:")) {
                currentSection = "INVENTORY";
            } else if (line.startsWith("#IsFollowingMouse:")) {
                currentSection = "FOLLOWINGMOUSE";
            } else if (line.startsWith("#IsFollowingOnMouseClick:")) {
                currentSection = "FOLLOWINGONMOUSECLICK";
            } else if (line.startsWith("#MouseHover:")) {
                System.out.println("=====> ItemLoader: Found #MouseHover: section!");
                currentSection = "MOUSEHOVER";
            } else if (line.startsWith("#CustomClickArea:")) {
                currentSection = "CUSTOMCLICKAREA";
                System.out.println("ItemLoader: Found #CustomClickArea: section");
                // Use primary CustomClickArea
                if (item != null) {
                    currentCustomClickArea = item.ensurePrimaryCustomClickArea();
                    // MIGRATION FIX: Don't clear points immediately - only clear if we find new points
                    // currentCustomClickArea.getPoints().clear(); // REMOVED - was causing data loss
                    // But DO reset conditions and hover text since they're section-specific
                    currentCustomClickArea.setConditions(new LinkedHashMap<>());
                    currentCustomClickArea.setHoverText("");
                    item.setHasCustomClickArea(true);
                    System.out.println("ItemLoader: Prepared primary CustomClickArea for loading");
                } else {
                    currentCustomClickArea = new CustomClickArea();
                }
                inCustomClickAreaSection = true;
            } else if (line.startsWith("#MovingRange:")) {
                currentSection = "MOVINGRANGE";
                System.out.println("ItemLoader: Found #MovingRange: section");
                if (item != null) {
                    currentMovingRange = item.ensurePrimaryMovingRange();
                    // MIGRATION FIX: Don't clear points immediately - only clear if we find new points
                    // currentMovingRange.getPoints().clear(); // REMOVED - was causing data loss
                } else {
                    currentMovingRange = new MovingRange();
                }
                inMovingRangeSection = true;
            } else if (line.startsWith("#Path:")) {
                currentSection = "PATH";
                System.out.println("ItemLoader: Found #Path: section");
                if (item != null) {
                    currentPath = item.ensurePrimaryPath();
                    // MIGRATION FIX: Don't clear points immediately - only clear if we find new points
                    // currentPath.getPoints().clear(); // REMOVED - was causing data loss
                } else {
                    currentPath = new Path();
                }
                inPathSection = true;
            } else if (line.startsWith("#Actions:")) {
                currentSection = "ACTIONS";
            } else if (line.startsWith("#CustomClickAreas:")) {
                currentSection = "CUSTOMCLICKAREAS";
                System.out.println("ItemLoader: Found #CustomClickAreas: section");
            } else if (line.startsWith("#MovingRanges:")) {
                currentSection = "MOVINGRANGES";
                System.out.println("ItemLoader: Found #MovingRanges: section");
            } else if (line.startsWith("#Paths:")) {
                currentSection = "PATHS";
                System.out.println("ItemLoader: Found #Paths: section");
            }

            // Parse new point format: -point1: (uniform format for all sections)
            else if ((currentSection.equals("CUSTOMCLICKAREA") ||
                      currentSection.equals("MOVINGRANGE") ||
                      currentSection.equals("PATH")) &&
                     line.startsWith("-point") && line.contains(":")) {
                // Parse format: -point1: x=590, y=429, z=1 (unified format with single dash)
                try {
                    String coordPart = line.substring(line.indexOf(":") + 1).trim();
                    String[] coords = coordPart.split(",");
                    int x = 0, y = 0;

                    for (String coord : coords) {
                        coord = coord.trim();
                        if (coord.startsWith("x=")) {
                            x = Integer.parseInt(coord.substring(2).trim());
                        } else if (coord.startsWith("y=")) {
                            y = Integer.parseInt(coord.substring(2).trim());
                        }
                        // z wird ignoriert, da Point nur x,y hat
                    }

                    if (item != null) {
                        if (currentSection.equals("CUSTOMCLICKAREA") && currentCustomClickArea != null) {
                            // MIGRATION FIX: Clear old points only when first new point is loaded
                            if (!customClickAreaPointsLoaded) {
                                currentCustomClickArea.getPoints().clear();
                                customClickAreaPointsLoaded = true;
                            }
                            currentCustomClickArea.addPoint(new Point(x, y));
                            System.out.println("ItemLoader: Added CustomClickArea point (" + x + ", " + y + ")");
                        } else if (currentSection.equals("MOVINGRANGE") && currentMovingRange != null) {
                            // MIGRATION FIX: Clear old points only when first new point is loaded
                            if (!movingRangePointsLoaded) {
                                currentMovingRange.getPoints().clear();
                                movingRangePointsLoaded = true;
                            }
                            currentMovingRange.addPoint(new Point(x, y));
                            System.out.println("ItemLoader: Added MovingRange point (" + x + ", " + y + ")");
                        } else if (currentSection.equals("PATH") && currentPath != null) {
                            // MIGRATION FIX: Clear old points only when first new point is loaded
                            if (!pathPointsLoaded) {
                                currentPath.getPoints().clear();
                                pathPointsLoaded = true;
                            }
                            currentPath.addPoint(new Point(x, y));
                            System.out.println("ItemLoader: Added Path point (" + x + ", " + y + ")");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ItemLoader: Error parsing point: " + line + " - " + e.getMessage());
                }
            }
            // OLD FORMAT - ClickArea coordinates - MIGRATION: Convert to CustomClickArea
            else if (currentSection.equals("CLICKAREA") && line.startsWith("--x")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    clickAreaX = Integer.parseInt(parts[1].trim().replace(";", ""));
                    System.out.println("ItemLoader: Read clickAreaX = " + clickAreaX);
                }
            }
            else if (currentSection.equals("CLICKAREA") && line.startsWith("--y")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    clickAreaY = Integer.parseInt(parts[1].trim().replace(";", ""));
                    // MIGRATION: Add to CustomClickArea instead of legacy clickAreaPoints
                    if (item != null) {
                        CustomClickArea primaryArea = item.ensurePrimaryCustomClickArea();
                        primaryArea.addPoint(clickAreaX, clickAreaY);
                        item.setHasCustomClickArea(true);
                        System.out.println("ItemLoader: Added point (" + clickAreaX + ", " + clickAreaY + ") to CustomClickArea");
                    }
                }
            }
            
            else if (currentSection.equals("MOVINGRANGE") && line.startsWith("--x")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    movingRangeX = Integer.parseInt(parts[1].trim().replace(";", ""));
                    System.out.println("ItemLoader: Read movingRangeX = " + movingRangeX);
                }
            }
            else if (currentSection.equals("MOVINGRANGE") && line.startsWith("--y")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                	movingRangeY = Integer.parseInt(parts[1].trim().replace(";", ""));
                    if (currentMovingRange != null) {
                        currentMovingRange.addPoint(new Point(movingRangeX, movingRangeY));
                        System.out.println("ItemLoader: Added legacy MovingRange point (" + movingRangeX + ", " + movingRangeY + ")");
                    }
                }
            }
            
            else if (currentSection.equals("PATH") && line.startsWith("--x")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    pathX = Integer.parseInt(parts[1].trim().replace(";", ""));
                    System.out.println("ItemLoader: Read movingRangeX = " + pathX);
                }
            }
            else if (currentSection.equals("PATH") && line.startsWith("--y")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                	pathY = Integer.parseInt(parts[1].trim().replace(";", ""));
                    if (currentPath != null) {
                        currentPath.addPoint(new Point(pathX, pathY));
                        System.out.println("ItemLoader: Added legacy Path point (" + pathX + ", " + pathY + ")");
                    }
                }
            }
            
            // Parse ConditionalImages (new format)
            else if (currentSection.equals("CONDITIONALIMAGES")) {
                if (line.equals("-Image:")) {
                    // Save previous image if exists
                    if (currentConditionalImage != null && item != null) {
                        item.addConditionalImage(currentConditionalImage);
                    }
                    // Start new image
                    currentConditionalImage = new ConditionalImage();
                    inConditionalImageSection = true;
                    inConditionsSubSection = false;
                } else if (inConditionalImageSection && currentConditionalImage != null) {
                    if (line.startsWith("--Name:")) {
                        String name = line.substring(7).trim();
                        currentConditionalImage.setName(name);
                    } else if (line.startsWith("--Path:")) {
                        String path = line.substring(7).trim();

                        // Store only filename, not full path
                        // Strip "resources/images/items/" prefix if present
                        if (path.startsWith("resources/images/items/")) {
                            path = path.substring("resources/images/items/".length());
                        } else if (path.startsWith("resources/images/")) {
                            path = path.substring("resources/images/".length());
                        } else if (path.startsWith("resources/images/cursors/")) {
                            path = path.substring("resources/images/cursors/".length());
                        }

                        // Extract just the filename if it contains path separators
                        if (path.contains("/")) {
                            path = path.substring(path.lastIndexOf("/") + 1);
                        } else if (path.contains("\\")) {
                            path = path.substring(path.lastIndexOf("\\") + 1);
                        }

                        currentConditionalImage.setImagePath(path);
                        System.out.println("ItemLoader: Set Image path to: " + path);
                    } else if (line.startsWith("--ShowIfTrue:")) {
                        String showIfStr = line.substring(13).trim();
                        currentConditionalImage.setShowIfTrue(Boolean.parseBoolean(showIfStr));
                    } else if (line.equals("--Conditions:")) {
                        inConditionsSubSection = true;
                    } else if (inConditionsSubSection && line.startsWith("---")) {
                        // Parse condition: "---hasLighter = true"
                        String conditionLine = line.substring(3).trim();
                        if (conditionLine.contains("=")) {
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String condName = parts[0].trim();
                                boolean condValue = Boolean.parseBoolean(parts[1].trim());
                                currentConditionalImage.addCondition(condName, condValue);
                            }
                        }
                    }
                }
            }
            // Parse CustomClickAreas
            else if (currentSection.equals("CUSTOMCLICKAREAS")) {
                if (line.equals("-CustomClickArea:")) {
                    if (item != null) {
                        currentCustomClickArea = item.ensurePrimaryCustomClickArea();
                        if (!legacyCustomAreaInitialized) {
                            currentCustomClickArea.getPoints().clear();
                            currentCustomClickArea.setConditions(new LinkedHashMap<>());
                            currentCustomClickArea.setHoverText("");
                            legacyCustomAreaInitialized = true;
                        }
                        item.setHasCustomClickArea(true);
                    } else {
                        currentCustomClickArea = new CustomClickArea();
                    }
                    inCustomClickAreaSection = true;
                    System.out.println("ItemLoader: Started legacy CustomClickArea block");
                } else if (inCustomClickAreaSection && currentCustomClickArea != null) {
                    if (line.equals("--Points:")) {
                        // Points section
                    } else if (line.startsWith("---point") && line.contains(":")) {
                        // Parse point: "---point1: x=100, y=200, z=0"
                        String pointData = line.substring(line.indexOf(":") + 1).trim();
                        String[] coords = pointData.split(",");
                        if (coords.length >= 2) {
                            try {
                                int x = Integer.parseInt(coords[0].split("=")[1].trim());
                                int y = Integer.parseInt(coords[1].split("=")[1].trim());
                                currentCustomClickArea.addPoint(x, y);
                                System.out.println("ItemLoader: Added point (" + x + ", " + y + ") to CustomClickArea");
                            } catch (Exception e) {
                                System.err.println("Error parsing point: " + line);
                            }
                        }
                    } else if (line.equals("--Conditions:")) {
                        inConditionsSubSection = true;
                    } else if (inConditionsSubSection && line.startsWith("---")) {
                        // Parse condition: "---lookedAtCupAtBeach = false"
                        String conditionLine = line.substring(3).trim();
                        if (conditionLine.contains("=")) {
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String condName = parts[0].trim();
                                boolean condValue = Boolean.parseBoolean(parts[1].trim());
                                currentCustomClickArea.addCondition(condName, condValue);
                                System.out.println("ItemLoader: Added condition to CustomClickArea: " + condName + " = " + condValue);
                            }
                        }
                    } else if (line.startsWith("--HoverText:")) {
                        // Parse hover text: "--HoverText: "white cup""
                        String hoverText = line.substring(12).trim();
                        if (hoverText.startsWith("\"") && hoverText.endsWith("\"")) {
                            hoverText = hoverText.substring(1, hoverText.length() - 1);
                        }
                        currentCustomClickArea.setHoverText(hoverText);
                        System.out.println("ItemLoader: Set hover text: " + hoverText);
                        inConditionsSubSection = false;
                    }
                }
            }
            // Parse MovingRanges (NEW FORMAT: simple name references)
            // Format: #MovingRanges: followed by -name entries
            else if (currentSection.equals("MOVINGRANGES")) {
                if (line.startsWith("-") && !line.startsWith("--")) {
                    // NEW FORMAT: Parse name reference: "-cup_range"
                    String rangeName = line.substring(1).trim();
                    if (item != null && !rangeName.isEmpty()) {
                        item.addMovingRangeName(rangeName);
                        System.out.println("ItemLoader: Added MovingRange reference: " + rangeName);
                    }
                } else if (line.equals("-MovingRange:")) {
                    // LEGACY FORMAT: Old format with embedded points (for backward compatibility)
                    if (item != null) {
                        currentMovingRange = item.ensurePrimaryMovingRange();
                        if (!legacyMovingRangeInitialized) {
                            currentMovingRange.getPoints().clear();
                            legacyMovingRangeInitialized = true;
                        }
                    } else {
                        currentMovingRange = new MovingRange();
                    }
                    inMovingRangeSection = true;
                    System.out.println("ItemLoader: Started legacy MovingRange block (deprecated)");
                } else if (inMovingRangeSection && currentMovingRange != null) {
                    // Legacy format continuation
                    if (line.startsWith("--KeyAreaName:")) {
                        String keyAreaName = line.substring(14).trim();
                        currentMovingRange.setKeyAreaName(keyAreaName);
                        System.out.println("ItemLoader: Set KeyAreaName: " + keyAreaName);
                    } else if (line.equals("--Conditions:")) {
                        inConditionsSubSection = true;
                    } else if (inConditionsSubSection && line.startsWith("---")) {
                        // Parse condition: "---someCondition = true"
                        String conditionLine = line.substring(3).trim();
                        if (conditionLine.contains("=")) {
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String condName = parts[0].trim();
                                boolean condValue = Boolean.parseBoolean(parts[1].trim());
                                currentMovingRange.addCondition(condName, condValue);
                                System.out.println("ItemLoader: Added condition to MovingRange: " + condName + " = " + condValue);
                            }
                        }
                    }
                }
            }
            
            else if (currentSection.equals("PATHS")) {
                if (line.equals("-Path:")) {
                    if (item != null) {
                        currentPath = item.ensurePrimaryPath();
                        if (!legacyPathInitialized) {
                            currentPath.getPoints().clear();
                            legacyPathInitialized = true;
                        }
                    } else {
                        currentPath = new Path();
                    }
                    inPathSection = true;
                    System.out.println("ItemLoader: Started legacy Path block");
                } else if (inPathSection && currentPath != null) {
                    if (line.startsWith("--KeyAreaName:")) {
                        String keyAreaName = line.substring(14).trim();
                        currentPath.setKeyAreaName(keyAreaName);
                        System.out.println("ItemLoader: Set KeyAreaName: " + keyAreaName);
                    } else if (line.equals("--Conditions:")) {
                        inConditionsSubSection = true;
                    } else if (inConditionsSubSection && line.startsWith("---")) {
                        // Parse condition: "---someCondition = true"
                        String conditionLine = line.substring(3).trim();
                        if (conditionLine.contains("=")) {
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String condName = parts[0].trim();
                                boolean condValue = Boolean.parseBoolean(parts[1].trim());
//                                currentPath.addCondition(condName, condValue);
                                System.out.println("ItemLoader: Added condition to Path: " + condName + " = " + condValue);
                            }
                        }
                    }
                }
            }
            // Data lines (only single dash, not --, ---, etc.)
            else if (line.startsWith("-") && !line.startsWith("--")) {
                String value = line.substring(1).trim();

                switch (currentSection) {
                    case "NAME":
                        item = new Item(value);
                        break;

                    case "IMAGEFILE":
                        if (item != null) {
                            item.setImageFileName(value);
                        }
                        break;

                    case "IMAGEPATH":
                        if (item != null) {
                            // MIGRATION: Convert legacy ImagePath to ConditionalImage
                            String imagePath = value;
                            if (!imagePath.isEmpty()) {
                                ConditionalImage defaultImage = new ConditionalImage(imagePath, "Default");
                                item.addConditionalImage(defaultImage);
                                // Also set legacy field for backward compatibility during transition
                                item.setImageFilePath(imagePath);
                            }
                        }
                        break;

                    case "IMAGEPATHTOPLEFT":
                        if (item != null) {
                            item.setImagePathTopLeft(value);
                        }
                        break;

                    case "IMAGEPATHTOP":
                        if (item != null) {
                            item.setImagePathTop(value);
                        }
                        break;

                    case "IMAGEPATHTOPRIGHT":
                        if (item != null) {
                            item.setImagePathTopRight(value);
                        }
                        break;

                    case "IMAGEPATHLEFT":
                        if (item != null) {
                            item.setImagePathLeft(value);
                        }
                        break;

                    case "IMAGEPATHMIDDLE":
                        if (item != null) {
                            item.setImagePathMiddle(value);
                        }
                        break;

                    case "IMAGEPATHRIGHT":
                        if (item != null) {
                            item.setImagePathRight(value);
                        }
                        break;

                    case "IMAGEPATHBOTTOMLEFT":
                        if (item != null) {
                            item.setImagePathBottomLeft(value);
                        }
                        break;

                    case "IMAGEPATHBOTTOM":
                        if (item != null) {
                            item.setImagePathBottom(value);
                        }
                        break;

                    case "IMAGEPATHBOTTOMRIGHT":
                        if (item != null) {
                            item.setImagePathBottomRight(value);
                        }
                        break;

                    case "POSITION":
                        if (item != null && value.contains("=")) {
                            String[] parts = value.split("=");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                String val = parts[1].trim().replace(";", "");

                                if (key.equals("x")) {
                                    Point pos = item.getPosition();
                                    item.setPosition(Integer.parseInt(val), pos.y);
                                } else if (key.equals("y")) {
                                    Point pos = item.getPosition();
                                    item.setPosition(pos.x, Integer.parseInt(val));
                                }
                            }
                        }
                        break;

                    case "SIZE":
                        if (item != null && value.contains("=")) {
                            String[] parts = value.split("=");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                String val = parts[1].trim().replace(";", "");

                                if (key.equals("width")) {
                                    item.setWidth(Integer.parseInt(val));
                                } else if (key.equals("height")) {
                                    item.setHeight(Integer.parseInt(val));
                                }
                            }
                        }
                        break;

                    case "CONDITIONS":
                        if (item != null && value.contains("=")) {
                            String conditionLine = value.replace(";", "");
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String conditionName = parts[0].trim();
                                boolean conditionValue = Boolean.parseBoolean(parts[1].trim());
                                item.addCondition(conditionName, conditionValue);
                            }
                        }
                        break;

                    case "INVENTORY":
                        if (item != null) {
                            item.setInInventory(Boolean.parseBoolean(value));
                        }
                        break;

                    case "FOLLOWINGMOUSE":
                        if (item != null) {
                            item.setFollowingMouse(Boolean.parseBoolean(value));
                        }
                        break;

                    case "FOLLOWINGONMOUSECLICK":
                        if (item != null) {
                            item.setFollowingOnMouseClick(Boolean.parseBoolean(value));
                        }
                        break;

                    case "MOUSEHOVER":
                        // NEW SCHEMA: Simple format -"text"
                        // MIGRATION: Store hover text in CustomClickArea instead of hoverDisplayConditions
                        if (item != null) {
                            String hoverText = value;
                            // Remove quotes if present
                            if (hoverText.startsWith("\"") && hoverText.endsWith("\"")) {
                                hoverText = hoverText.substring(1, hoverText.length() - 1);
                            }
                            // Store in primary CustomClickArea (NEW SYSTEM)
                            CustomClickArea primaryArea = item.ensurePrimaryCustomClickArea();
                            primaryArea.setHoverText(hoverText);
                            // Also store in legacy format for backward compatibility
                            item.addHoverDisplayCondition("none", hoverText);
                            System.out.println("ItemLoader: Set hover text: " + hoverText);
                        }
                        break;

                    case "ACTIONS":
                        // Finalize previous action if exists (NEW SCHEMA)
                        if (currentActionHandler != null && !conditionsIF.isEmpty()) {
                            // Convert IF/THEN/Processes to old format: condition -> result
                            String ifCondition = conditionsIF.isEmpty() || conditionsIF.contains("none") ? "none" : String.join(" AND ", conditionsIF);
                            String process = processes.isEmpty() ? "processDefault" : processes.get(0);
                            currentActionHandler.addConditionalResult(ifCondition, "#Dialog:" + process);
                            System.out.println("ItemLoader: Finalized action with IF=" + ifCondition + ", Process=" + process);
                        }

                        // Start new action
                        pendingConditions.clear();
                        conditionsIF.clear();
                        conditionsTHEN.clear();
                        processes.clear();
                        actionSubSection = "";

                        currentAction = value;
                        currentActionHandler = new KeyArea.ActionHandler();
                        if (item != null) {
                            item.addAction(currentAction, currentActionHandler);
                        }
                        break;
                }
            }
            // ClickArea marker ### - reset coordinates
            else if (currentSection.equals("CLICKAREA") && line.startsWith("###")) {
                // Marker for new click area point - reset coordinates
                clickAreaX = 0;
                clickAreaY = 0;
                System.out.println("ItemLoader: Found ### marker, reset coordinates");
                continue;
            }
            // Sub-sections for MouseHover, ImageConditions, Actions
            // NOTE: This must come AFTER --x and --y checks!
            // NEW SCHEMA: Support --Conditions IF, --Conditions THEN, --Processes:
            else if (line.startsWith("--Conditions IF") || line.startsWith("--conditions IF")) {
                actionSubSection = "IF";
                conditionsIF.clear();
                System.out.println("ItemLoader: Found --Conditions IF");
            }
            else if (line.startsWith("--Conditions THEN") || line.startsWith("--conditions THEN")) {
                actionSubSection = "THEN";
                conditionsTHEN.clear();
                System.out.println("ItemLoader: Found --Conditions THEN");
            }
            else if (line.startsWith("--Processes:") || line.startsWith("--processes:")) {
                actionSubSection = "PROCESSES";
                processes.clear();
                System.out.println("ItemLoader: Found --Processes:");
            }
            else if (line.startsWith("--conditions") || line.startsWith("--conditions:")) {
                // OLD SCHEMA compatibility
                currentSubSection = "CONDITIONS";
                pendingConditions.clear();
            }
            else if (line.startsWith("---") && !line.startsWith("----") && !line.startsWith("---point")) {
                // Condition/Process line - collect based on actionSubSection
                // NOTE: Exclude "---point" lines as they are point definitions for MovingRange/Path, not conditions
                String content = line.substring(3).trim().replace(";", "");

                if (actionSubSection.equals("IF")) {
                    conditionsIF.add(content);
                    System.out.println("ItemLoader: Added IF condition: '" + content + "'");
                } else if (actionSubSection.equals("THEN")) {
                    conditionsTHEN.add(content);
                    System.out.println("ItemLoader: Added THEN condition: '" + content + "'");
                } else if (actionSubSection.equals("PROCESSES")) {
                    processes.add(content);
                    System.out.println("ItemLoader: Added Process: '" + content + "'");
                } else {
                    // OLD SCHEMA compatibility
                    pendingConditions.add(content);
                    System.out.println("ItemLoader: Added condition to list: '" + content + "' (Total: " + pendingConditions.size() + ")");
                }
            }
            else if (line.startsWith("----Display:")) {
                System.out.println("ItemLoader: Found ----Display: (Setting currentSubSection=DISPLAY)");
                currentSubSection = "DISPLAY";
            }
            else if (line.startsWith("----Image:")) {
                String imagePath = line.substring(10).trim().replace(";", "");
                // MIGRATION: Convert legacy ImageConditions to ConditionalImage
                if (item != null && !pendingConditions.isEmpty() && currentSection.equals("IMAGECONDITIONS")) {
                    // Create ConditionalImage from legacy format
                    ConditionalImage conditionalImage = new ConditionalImage(imagePath, "Condition_" + item.getConditionalImages().size());
                    // Parse conditions like "hasLighter = true" into name=value pairs
                    for (String conditionStr : pendingConditions) {
                        String[] parts = conditionStr.split("=");
                        if (parts.length == 2) {
                            String condName = parts[0].trim();
                            boolean condValue = Boolean.parseBoolean(parts[1].trim());
                            conditionalImage.addCondition(condName, condValue);
                        }
                    }
                    item.addConditionalImage(conditionalImage);
                    // Also store in legacy format for backward compatibility
                    pendingCondition = String.join(" AND ", pendingConditions);
                    item.addImageCondition(pendingCondition, imagePath);
                }
                pendingCondition = null;
                // Don't clear pendingConditions here - may have multiple results for same conditions
            }
            else if (line.startsWith("----#Dialog:")) {
                currentSubSection = "DIALOG";
            }
            else if (line.startsWith("----##load")) {
                String result = line.substring(4).trim();
                // Combine all pending conditions with AND
                if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                    pendingCondition = String.join(" AND ", pendingConditions);
                    currentActionHandler.addConditionalResult(pendingCondition, result);
                }
                pendingCondition = null;
                // Don't clear pendingConditions here - may have multiple results for same conditions
            }
            else if (line.startsWith("----#SetBoolean:")) {
                String result = line.substring(4).trim();
                // Combine all pending conditions with AND
                if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                    pendingCondition = String.join(" AND ", pendingConditions);
                    currentActionHandler.addConditionalResult(pendingCondition, result);
                    System.out.println("ItemLoader: Added SetBoolean result: " + result + " for condition: " + pendingCondition);
                }
                pendingCondition = null;
                // Don't clear pendingConditions here - may have multiple results for same conditions
            }
            else if (line.startsWith("------")) {
                String value = line.substring(6).trim();

                if (currentSection.equals("MOUSEHOVER") && currentSubSection.equals("DISPLAY")) {
                    // Hover text - combine all pending conditions with AND
                    // MIGRATION: Convert to CustomClickArea with conditions
                    String displayText = value.replace("\"", "");
                    System.out.println("DEBUG ItemLoader: Loading hover text for item");
                    System.out.println("  item: " + (item != null ? item.getName() : "null"));
                    System.out.println("  pendingConditions size: " + pendingConditions.size());
                    System.out.println("  displayText: '" + displayText + "'");

                    if (item != null) {
                        CustomClickArea area = item.ensurePrimaryCustomClickArea();
                        area.setHoverText(displayText);

                        // Add conditions to CustomClickArea
                        for (String condStr : pendingConditions) {
                            String[] parts = condStr.split("=");
                            if (parts.length == 2) {
                                String condName = parts[0].trim();
                                boolean condValue = Boolean.parseBoolean(parts[1].trim());
                                area.addCondition(condName, condValue);
                            }
                        }

                        // Also store in legacy format for backward compatibility
                        if (!pendingConditions.isEmpty()) {
                            pendingCondition = String.join(" AND ", pendingConditions);
                            item.addHoverDisplayCondition(pendingCondition, displayText);
                            System.out.println("  -> Added hover condition: " + pendingCondition);
                        }
                    } else {
                        System.out.println("  -> NOT added (item is null)");
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                } else if (currentSection.equals("ACTIONS") && currentSubSection.equals("DIALOG")) {
                    // Action dialog result - combine all pending conditions with AND
                    if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentActionHandler.addConditionalResult(pendingCondition, "#Dialog:" + value);
                        System.out.println("ItemLoader: Added Dialog result for condition: " + pendingCondition);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
            }
        }

        // Finalize last action if exists (NEW SCHEMA)
        if (currentActionHandler != null && !conditionsIF.isEmpty()) {
            String ifCondition = conditionsIF.isEmpty() || conditionsIF.contains("none") ? "none" : String.join(" AND ", conditionsIF);
            String process = processes.isEmpty() ? "processDefault" : processes.get(0);
            currentActionHandler.addConditionalResult(ifCondition, "#Dialog:" + process);
            System.out.println("ItemLoader: Finalized LAST action with IF=" + ifCondition + ", Process=" + process);
        }

        // Add last conditional image if exists
        if (currentConditionalImage != null && item != null) {
            item.addConditionalImage(currentConditionalImage);
        }

        // Add last custom click area if exists
        if (currentCustomClickArea != null && item != null) {
            currentCustomClickArea.updatePolygon(); // Ensure polygon is updated
            System.out.println("ItemLoader: Finalized last CustomClickArea with " + currentCustomClickArea.getPoints().size() + " points");
        }

        // Add last moving range if exists
        if (currentMovingRange != null && item != null) {
            System.out.println("ItemLoader: Finalized last MovingRange with " + currentMovingRange.getPoints().size() + " points");
        }

        // Add last path if exists
        if (currentPath != null && item != null) {
            System.out.println("ItemLoader: Finalized last Path with " + currentPath.getPoints().size() + " points");
        }

        reader.close();

        if (item == null) {
            throw new IOException("Invalid item file format - no name found");
        }

        // Ensure only one point container per type
        item.consolidatePointContainers();

        // Create or update isInInventory condition for this item (runtime only, not saved to conditions.txt)
        String inventoryConditionName = "isInInventory_" + item.getName();
        if (!Conditions.conditionExists(inventoryConditionName)) {
            Conditions.addConditionRuntimeOnly(inventoryConditionName, item.isInInventory());
            System.out.println("Created runtime condition: " + inventoryConditionName + " = " + item.isInInventory());
        } else {
            // Update condition with loaded value
            Conditions.setCondition(inventoryConditionName, item.isInInventory());
            System.out.println("Updated condition: " + inventoryConditionName + " = " + item.isInInventory());
        }

        // Update polygon if custom points were loaded (NEW SYSTEM: CustomClickArea)
        if (item.hasCustomClickArea() || !item.getCustomClickAreas().isEmpty()) {
            System.out.println("Loaded Custom Click Area for Item: " + item.getName());
            for (CustomClickArea area : item.getCustomClickAreas()) {
                area.updatePolygon();
                System.out.println("  CustomClickArea with " + area.getPoints().size() + " points");
            }
        } else {
            System.out.println("Loaded item: " + item.getName());
        }

        System.out.println("Final item hover display conditions size: " + item.getHoverDisplayConditions().size());
        for (java.util.Map.Entry<String, String> entry : item.getHoverDisplayConditions().entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }

        return item;
    }

    /**
     * Load item from DEFAULT file: resources/items/[itemName].txt
     * Use this in EDITOR MODE and as fallback in GAMING MODE
     * This is the DEFAULT version of the item
     */
    public static Item loadItemByName(String itemName) throws IOException {
        String filename = "resources/items/" + itemName + ".txt";
        System.out.println("✓ Loading item from DEFAULT: " + filename);
        return loadItem(filename);
    }

    /**
     * @deprecated Use loadItemByName() instead - <name>.txt is now the DEFAULT
     */
    @Deprecated
    public static Item loadItemFromDefault(String itemName) throws IOException {
        return loadItemByName(itemName);
    }

    /**
     * @deprecated Progress is now managed differently - _progress.txt files are no longer used
     */
    @Deprecated
    public static Item loadItemFromProgress(String itemName) throws IOException {
        System.out.println("⚠️  WARNING: loadItemFromProgress() is deprecated - Progress is managed differently now");
        // Fall back to loading from default file
        return loadItemByName(itemName);
    }
}
