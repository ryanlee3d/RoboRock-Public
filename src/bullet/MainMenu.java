package bullet;

public class MainMenu
{
    public enum MenuAction
    {
        NONE,
        START_GAME,
        SELECT_MAP,
        MULTIPLAYER,
        OPTIONS,
        QUIT
    }

    private final String title = "TITLE";

    private final String[] menuItems = {
        "Play Game",
        "Select Map",
        "MultiPlayer",
        "Options",
        "Quit"
    };

    private final String[] mapNames = {
        "Cliffs",
        "Airbase"
    };

    private int selectedIndex = 0;
    private int selectedMapIndex = 0;

    public String getTitleText()
    {
        return title;
    }

    public String getMenuText()
    {
        StringBuilder sb = new StringBuilder("Menu: ");
        for (int i = 0; i < menuItems.length; i++)
        {
            if (i > 0) sb.append("   ");
            if (i == selectedIndex) sb.append("> ");

            sb.append(menuItems[i]);
            if (i == 1) sb.append(": ").append(mapNames[selectedMapIndex]);

            if (i == selectedIndex) sb.append(" <");
        }
        return sb.toString();
    }

    public String getFooterText()
    {
        return "Use UP/DOWN to move, LEFT/RIGHT to change map, ENTER to select";
    }

    public int getSelectedIndex()
    {
        return selectedIndex;
    }

    public int getSelectedMapIndex()
    {
        return selectedMapIndex;
    }

    public String getSelectedItem()
    {
        return menuItems[selectedIndex];
    }

    public void moveUp()
    {
        selectedIndex = (selectedIndex - 1 + menuItems.length) % menuItems.length;
    }

    public void moveDown()
    {
        selectedIndex = (selectedIndex + 1) % menuItems.length;
    }

    public void nextMap()
    {
        selectedMapIndex = (selectedMapIndex + 1) % mapNames.length;
    }

    public void previousMap()
    {
        selectedMapIndex = (selectedMapIndex - 1 + mapNames.length) % mapNames.length;
    }

    public MenuAction activateSelection()
    {
        return switch (selectedIndex)
        {
            case 0 -> MenuAction.START_GAME;
            case 1 -> MenuAction.SELECT_MAP;
            case 2 -> MenuAction.MULTIPLAYER;
            case 3 -> MenuAction.OPTIONS;
            case 4 -> MenuAction.QUIT;
            default -> MenuAction.NONE;
        };
    }
}
