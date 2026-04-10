package bullet;

import bullet.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class RestartGame extends AbstractInputAction {
    private final MyGame game;

    public RestartGame(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event event) {
		game.getEngine().getSceneGraph().removeAllGameObjects();
		game.buildObjects();
		game.initializeGame();
    game.setGameState("MENU");
		}
    }
