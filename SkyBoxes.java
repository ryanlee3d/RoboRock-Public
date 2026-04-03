package bullet;

import java.util.HashMap;
import java.util.Map;
import tage.Engine;
import tage.SceneGraph;

public class SkyBoxes
{
    private final Engine engine;
    private final Map<String, Integer> boxes = new HashMap<>();

    public SkyBoxes(Engine engine)
    {
        this.engine = engine;
    }

    public void load(String name, String folderName)
    {
        SceneGraph sg = engine.getSceneGraph();
        int textureID = sg.loadCubeMap(folderName);
        boxes.put(name, textureID);
    }

    public void activate(String name)
    {
        Integer tex = boxes.get(name);
        if (tex == null)
        {
            System.out.println("Skybox not found: " + name);
            return;
        }

        SceneGraph sg = engine.getSceneGraph();
        sg.setActiveSkyBoxTexture(tex);
        sg.setSkyBoxEnabled(true);
    }

    public boolean contains(String name)
    {
        return boxes.containsKey(name);
    }

    public int get(String name)
    {
        Integer tex = boxes.get(name);
        return (tex == null) ? -1 : tex;
    }
}