package bullet.ui;

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

    private final String title = "Robo-Rock";

    private final String[] menuItems = {
        "Play Game",
        //"Select Map", not in use | it works, but we decided to go a different direction.
        "MultiPlayer",
        //"Options", not implemented yet 
        "Quit"
    };

    private final String[] mapNames = {
        "Cliffs",
        "Airbase"
    };

    private final String[] multiplayerItems = {
        "Host Game",
        "Join Game"
    };

    private int selectedIndex = 0;
    private int selectedMapIndex = 0;
    private int selectedMultiplayerIndex = 0;

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
          //  if (i == 1) sb.append(": ").append(mapNames[selectedMapIndex]); //uncomment if using select_map

            if (i == selectedIndex) sb.append(" <");
        }
        return sb.toString();
    }

    public String getFooterText()
    {
        return "Use UP/DOWN to move, LEFT/RIGHT to change map, ENTER to select";
    }

    public String getMultiplayerText()
    {
        StringBuilder sb = new StringBuilder("Multiplayer: ");
        for (int i = 0; i < multiplayerItems.length; i++)
        {
            if (i > 0) sb.append("   ");
            if (i == selectedMultiplayerIndex) sb.append("> ");

            sb.append(multiplayerItems[i]);

            if (i == selectedMultiplayerIndex) sb.append(" <");
        }
        return sb.toString();
    }

    public String getMultiplayerFooterText()
    {
        return "Use UP/DOWN to choose, ENTER to select, ESC to go back";
    }

    public int getSelectedIndex()
    {
        return selectedIndex;
    }

    public int getSelectedMapIndex()
    {
        return selectedMapIndex;
    }

    public int getSelectedMultiplayerIndex()
    {
        return selectedMultiplayerIndex;
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

    public void moveMultiplayerUp()
    {
        selectedMultiplayerIndex = (selectedMultiplayerIndex - 1 + multiplayerItems.length) % multiplayerItems.length;
    }

    public void moveMultiplayerDown()
    {
        selectedMultiplayerIndex = (selectedMultiplayerIndex + 1) % multiplayerItems.length;
    }

    public MenuAction activateSelection()
    {
        return switch (selectedIndex)
        {
            case 0 -> MenuAction.START_GAME;
            //case 1 -> MenuAction.SELECT_MAP;
            case 1 -> MenuAction.MULTIPLAYER; //change to 2 if using select_map
            //case 3 -> MenuAction.OPTIONS;
            case 2 -> MenuAction.QUIT; //change to 4 if using select_map and options
            default -> MenuAction.NONE;
        };
    }
}
