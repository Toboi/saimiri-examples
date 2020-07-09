package de.toboidev.saimiri.examples.tiled;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioListenerState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.state.GameSystemsState;
import de.toboidev.saimiri.es.DefaultEntityDataProvider;
import de.toboidev.saimiri.es.components.Position;
import de.toboidev.saimiri.es.components.Rotation;
import de.toboidev.saimiri.es.components.Size;
import de.toboidev.saimiri.game.collision.World;
import de.toboidev.saimiri.game.collision.staticbodies.TileMap;
import de.toboidev.saimiri.game.components.*;
import de.toboidev.saimiri.game.systems.CollisionSystem;
import de.toboidev.saimiri.game.systems.PlatformerCharacterSystem;
import de.toboidev.saimiri.game.systems.TopDownCharacterSystem;
import de.toboidev.saimiri.gfx.components.CameraController;
import de.toboidev.saimiri.gfx.components.PointLightComponent;
import de.toboidev.saimiri.gfx.components.RenderComponent;
import de.toboidev.saimiri.gfx.components.SpriteComponent;
import de.toboidev.saimiri.gfx.deferred.DeferredLightingProcessor;
import de.toboidev.saimiri.gfx.deferred.LightSystem;
import de.toboidev.saimiri.gfx.render.CameraControllerState;
import de.toboidev.saimiri.gfx.render.SpriteRenderState;
import de.toboidev.saimiri.tmx.MapCollision;
import de.toboidev.saimiri.tmx.MapRenderer;
import de.toboidev.saimiri.tmx.TiledLoader;
import org.tiledreader.TiledMap;
import org.tiledreader.TiledReader;

/**
 * @author Eike Foede <toboi@toboidev.de>
 */
public class Main extends SimpleApplication implements ActionListener {
    TiledMap map;
    private World world = new World();
    private EntityData ed;
    private EntityId playerId;
    private int playerSizeX = 127;
    private int playerSizeY = 127;
    private boolean platformerControls = true;
    private boolean deferredLighting = false;
    private DeferredLightingProcessor deferredLightingProcessor;

    public Main() {
        super(new AudioListenerState(), new DebugKeysAppState());
    }

    public static void main(String[] args) {

        Main m = new Main();
        m.start();
    }

    @Override public void simpleInitApp() {
        renderer.setLinearizeSrgbImages(false);
        CameraControllerState.setupCamera(cam, 0, 500);
        viewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new com.jme3.renderer.queue.GuiComparator());
        setPauseOnLostFocus(false);

        assetManager.registerLoader(TiledLoader.class, "tmx");
//        map = (TiledMap) assetManager.loadAsset("testMap.tmx");
        map = (TiledMap) assetManager.loadAsset("Maps/desert.tmx");

        MapRenderer mr = new MapRenderer();
        mr.assetManager = assetManager;

        Spatial s = mr.getSpatial((TiledMap)assetManager.loadAsset("Maps/desert.tmx"));
        rootNode.attachChild(s);


        ed = new DefaultEntityData();
        stateManager.attach(new DefaultEntityDataProvider(ed));

        GameSystemsState gss = new GameSystemsState(false);
        gss.register(EntityData.class, ed);
        gss.register(CollisionSystem.class, new CollisionSystem(world));
        gss.addSystem(new PlatformerCharacterSystem());
        gss.addSystem(new TopDownCharacterSystem());
        stateManager.attach(gss);


        stateManager.attach(new SpriteRenderState(rootNode));
        stateManager.attach(new CameraControllerState());


        setupInput();
        setupWorld();
        setupPlayerEntity();
        setupGroundEntity();

        deferredLightingProcessor = new DeferredLightingProcessor(assetManager);
        deferredLightingProcessor.setAmbientLight(ColorRGBA.Black);
        stateManager.attach(new LightSystem(deferredLightingProcessor));

    }

    private void setupWorld() {
        TileMap tmCollider = MapCollision.getTileMapCollider(map);
        world.addBody(tmCollider);
    }

    @Override public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("toggleInput") && isPressed) {
            togglePlatformer();
        } else if (name.equals("toggleLight") && isPressed) {
            toggleLight();
        } else if (platformerControls) {
            handlePlatformerInput(name, isPressed);
        } else {
            handleTopDownInput(name, isPressed);
        }
    }

    private void setupInput() {

        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("toggleInput", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("toggleLight", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addListener(this, "left", "right", "up", "down", "toggleInput", "toggleLight");
    }

    private void setupPlayerEntity() {
        playerId = ed.createEntity();
        ed.setComponents(playerId,
                new Position(100, 100),
                new Rotation(),
                new Size(playerSizeX, playerSizeY),
                new RenderComponent(0),
                new CameraController(600),
                new SpriteComponent("Common/Textures/MissingTexture.png"),
                new DynamicBodyComponent(),
                new PlatformerCharacterComponent(),
                new PlatformerInput(),
                new PointLightComponent(100, ColorRGBA.White, 0.1f));
    }

    private void setupGroundEntity() {
        EntityId ground = ed.createEntity();
        ed.setComponents(ground,
                new Position(500, -20),
                new Rotation(),
                new Size(5000, 40),
                new RenderComponent(-3),
                new SpriteComponent("Common/Textures/MissingModel.png"),
                new StaticCollider()
        );
    }

    private void handlePlatformerInput(String name, boolean isPressed) {
        PlatformerInput input = ed.getComponent(playerId, PlatformerInput.class);
        switch (name) {
            case "right":
                input = input.withRight(isPressed);
                break;
            case "left":
                input = input.withLeft(isPressed);
                break;
            case "up":
                if (isPressed) {
                    input = input.withJump(true);
                }
                break;
        }
        ed.setComponent(playerId, input);
    }

    private void handleTopDownInput(String name, boolean isPressed) {
        TopDownInput input = ed.getComponent(playerId, TopDownInput.class);
        if (input == null) {
            input = new TopDownInput(false, false, false, false);
        }
        switch (name) {
            case "right":
                input = input.withRight(isPressed);
                break;
            case "left":
                input = input.withLeft(isPressed);
                break;
            case "up":
                input = input.withUp(isPressed);
                break;
            case "down":
                input = input.withDown(isPressed);
                break;
        }
        ed.setComponent(playerId, input);
    }

    private void togglePlatformer() {
        if (platformerControls) {
            ed.removeComponent(playerId, PlatformerCharacterComponent.class);
            ed.setComponent(playerId, new TopDownCharacterComponent(400));
            ed.setComponent(playerId, new TopDownInput());
        } else {
            ed.removeComponent(playerId, TopDownCharacterComponent.class);
            ed.setComponent(playerId, new PlatformerCharacterComponent());
            ed.setComponent(playerId, new PlatformerInput());
        }
        platformerControls = !platformerControls;
    }

    private void toggleLight() {
        if (deferredLighting) {
            viewPort.removeProcessor(deferredLightingProcessor);
        } else {
            viewPort.addProcessor(deferredLightingProcessor);
        }
        deferredLighting = !deferredLighting;
    }
}