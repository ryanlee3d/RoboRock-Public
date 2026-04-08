package bullet;

public class MainMenu
{
    public enum MenuAction
    {
        NONE,
        START_GAME,
        MULTIPLAYER,
        OPTIONS,
        QUIT
    }

    private final String title = "TITLE";
    private final String[] menuItems = {
        "Play Game",
        "MultiPlayer",
        "Options",
        "Quit"
    };

    private int selectedIndex = 0;

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
            if (i == selectedIndex) sb.append(" <");
        }
        return sb.toString();
    }

    public String getFooterText()
    {
        return "Use UP/DOWN to move, ENTER to select";
    }

    public int getSelectedIndex()
    {
        return selectedIndex;
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

    public MenuAction activateSelection()
    {
        return switch (selectedIndex)
        {
            case 0 -> MenuAction.START_GAME;
            case 2 -> MenuAction.MULTIPLAYER;
            case 3 -> MenuAction.OPTIONS;
            case 4 -> MenuAction.QUIT;
            default -> MenuAction.NONE;
        };
    }
}
