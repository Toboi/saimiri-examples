package de.toboidev.saimiri.examples.tiled;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioListenerState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import de.toboidev.saimiri.gfx.render.CameraControllerState;
import de.toboidev.saimiri.tmx.MapRenderer;
import de.toboidev.saimiri.tmx.TiledLoader;
import org.tiledreader.TiledMap;

/**
 * @author Eike Foede <toboi@toboidev.de>
 */
public class Main extends SimpleApplication
{

    public Main()
    {
        super(new AudioListenerState(), new DebugKeysAppState());
    }

    public static void main(String[] args)
    {
        Main m = new Main();
        m.start();
    }

    @Override public void simpleInitApp()
    {
        CameraControllerState.setupCamera(cam, 0, 500);
        viewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new com.jme3.renderer.queue.GuiComparator());
        setPauseOnLostFocus(false);
        assetManager.registerLoader(TiledLoader.class, "tmx", "tsx");
        TiledMap map = (TiledMap) assetManager.loadAsset("Maps/desert.tmx");

        MapRenderer mr = new MapRenderer();
        mr.assetManager = assetManager;
        Spatial s = mr.getSpatial(map);
        rootNode.attachChild(s);
//viewPort.addProcessor(new DeferredLightingProcessor(assetManager));

    }

    @Override public void simpleUpdate(float tpf)
    {
    }
}