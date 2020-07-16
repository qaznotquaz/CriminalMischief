package jason.storyteller;

import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Script implements Iterable<JSONObject>{
    private JSONObject sceneScript;
    private final String scene = "0";
    private static final ArrayList<MiniActor> cast = new ArrayList<>();

    public Script(String scriptName) {
        InputStream inputStream = getClass().getResourceAsStream(String.format("scripts/%s", scriptName));
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contentBuilder = new StringBuilder();

        reader.lines().forEach(s -> contentBuilder.append(s).append("\n"));

        JSONObject json = new JSONObject(contentBuilder.toString());

        JSONObject header = json.getJSONObject("header");
        /*if (header.getInt("episode") != episode || header.getInt("act") != act){
            throw new IllegalStateException("Playscript at " + path + " has invalid header.");
        }*/

        sceneScript = json.getJSONObject("scenes").getJSONObject(scene);
        JSONObject actors = sceneScript.getJSONObject("actors");
        actors.keySet().forEach(a -> cast.add(new MiniActor(a, actors.getJSONObject(a))));
    }

    /**
     * Returns an iterator over elements of type {@code String[]}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<JSONObject> iterator() {
        return new SceneIterator();
    }

    public static MiniActor getActor(String name){
        for (MiniActor actor:cast) {
            if(actor.getName().equals(name)){
                return actor;
            }
        }
        return null;
    }

    public enum LineType{
        dm, header, diagnostic
    }

    class MiniActor{
        /**
         * The actor's name.
         */
        private String name;
        /**
         * The actor's speaking color.
         */
        private String color;
        /**
         * The side of the screen the actor's messages show up on. {@code false} is left, {@code true} is right
         */
        private boolean side;

        public MiniActor(String name, JSONObject json){
            this.name = name;
            color = ANSI.dynamicColor.get(json.getString("color"));
            side = json.getString("side").equals("Left");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public boolean isSide() {
            return side;
        }

        public void setSide(boolean side) {
            this.side = side;
        }
    }

    class SceneIterator implements Iterator<JSONObject>{
        private final int totalLines;
        private int currentLine;

        public SceneIterator(){
            totalLines = sceneScript.length();
            currentLine = 1;
        }

        /**
         * Returns {@code true} if the iteration has more elements.
         * (In other words, returns {@code true} if {@link #next} would
         * return an element rather than throwing an exception.)
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            return currentLine < totalLines;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public JSONObject next() {
            JSONObject line = sceneScript.getJSONObject(String.valueOf(currentLine));
            currentLine++;
            return line;
        }
    }
}
