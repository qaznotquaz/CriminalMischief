package jason.storyteller;

import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Script implements Iterable<JSONObject>{
    private static JSONObject sceneScript;
    private static final String scene = "0";
    private static final ArrayList<MiniActor> cast = new ArrayList<>();

    public Script(String date, ArrayList<String> tags) {
        InputStream inputStream;
        String[] sceneTags;
        String sceneSelected = "none";

        try {
            inputStream = getClass().getResourceAsStream(String.format("snippets/%s.json", date));
        } catch (Exception e){
            throw new IllegalArgumentException(String.format("Date %s does not exist.", date));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contentBuilder = new StringBuilder();

        reader.lines().forEach(s -> contentBuilder.append(s).append("\n"));

        JSONObject json = new JSONObject(contentBuilder.toString());

        /*JSONObject header = json.getJSONObject("header");
        if (header.getInt("episode") != episode || header.getInt("act") != act){
            throw new IllegalArgumentException(String.format("Scene %s is not ep %s, act %s", date, episode, act));
        }*/

        JSONObject scenes = json.getJSONObject("scenes");

        for (String key:scenes.keySet()) {
             sceneTags = scenes.getJSONObject(key).optJSONObject("header").getString("tags").split(", ");

             if(Arrays.stream(sceneTags).anyMatch(tags::contains)){
                 sceneSelected = key;
                 break;
             }
        }

        sceneScript = scenes.getJSONObject(sceneSelected);
        JSONObject actors = sceneScript.getJSONObject("header").optJSONObject("actors");
        if(actors!=null){
            actors.keySet().forEach(a -> cast.add(new MiniActor(a, actors.getJSONObject(a))));
        }
    }

    public static int getSceneLength(){
        return sceneScript.getJSONObject("header").getInt("length");
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
        dm, diagnostic, animation
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
         * The side of the screen the actor's messages show up on. {@code false} is left, {@code true} is right.
         */
        private boolean backwards;

        public MiniActor(String name, JSONObject json){
            this.name = name;
            color = ANSI.dynamicColor.get(json.getString("color"));
            backwards = json.getString("side").equals("Left");
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

        public boolean isBackwards() {
            return backwards;
        }

        public void setBackwards(boolean backwards) {
            this.backwards = backwards;
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
